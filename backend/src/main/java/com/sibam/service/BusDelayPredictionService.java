package com.sibam.service;
import ai.onnxruntime.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sibam.integration.supabase.SupabaseStorageClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Map;

/**
 * Service za napovedovanje zamude avtobusa na podlagi ONNX modela.
 * Model se ob zagonu prenese iz Supabase Storage (gold/models/model_bus_delay.onnx).Preslikava postaj v smeri se naloži iz stop_direction_mapping.json.
 * Vhodne značilke (v tem vrstnem redu): route_id, stop_sequence, hour, day_of_week, is_weekend, temperature, rain, wind_speed, direction
 */

@Service
public class BusDelayPredictionService {
    private final SupabaseStorageClient supabaseStorageClient;

    private OrtEnvironment env;
    private OrtSession model;
    private Map<String, Integer> directionMapping;

    public BusDelayPredictionService(SupabaseStorageClient supabaseStorageClient) {
        this.supabaseStorageClient = supabaseStorageClient;
    }

    @PostConstruct
    public void load() throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();

        byte[] bytes = supabaseStorageClient.download("gold", "models/model_bus_delay.onnx");
        model = env.createSession(bytes, new OrtSession.SessionOptions());

        InputStream is = getClass().getResourceAsStream("/stop_direction_mapping.json");
        directionMapping = new ObjectMapper().readValue(is, new TypeReference<>() {});
    }

    public synchronized void reloadModel() throws OrtException {
        OrtSession newModel = env.createSession(
                supabaseStorageClient.download("gold", "models/model_bus_delay.onnx"),
                new OrtSession.SessionOptions()
        );
        try { model.close(); } catch (OrtException e) { /* best-effort close before reload */ }
        model = newModel;
    }

    public synchronized int predictDelay(int routeId, int stopSequence, int hour, int dayOfWeek,
                            int isWeekend, float temperature, float rain, float windSpeed,
                            int stopId) throws OrtException {

        String key = routeId + "_" + stopId;
        int direction = directionMapping.getOrDefault(key, -1);
        if (direction == -1) return 0;

        float[] features = {
                routeId,
                stopSequence,
                hour,
                dayOfWeek,
                isWeekend,
                temperature,
                rain,
                windSpeed,
                direction
        };

        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), new long[]{1, 9});
        try (var result = model.run(Map.of("float_input", tensor))) {
            float[][] output = (float[][]) result.get(0).getValue();
            return Math.round(output[0][0]);
        }
    }
}
