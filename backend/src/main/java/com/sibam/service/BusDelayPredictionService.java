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

    /**
     * Ob zagonu aplikacije naloži ONNX model zamud in lokalno preslikavo smeri.
     *
     * @throws OrtException če ONNX seje ni mogoče ustvariti
     * @throws IOException če preslikave stop_direction_mapping.json ni mogoče prebrati
     */
    @PostConstruct
    public void load() throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();

        byte[] bytes = supabaseStorageClient.download("gold", "models/model_bus_delay.onnx");
        model = env.createSession(bytes, new OrtSession.SessionOptions());

        InputStream is = getClass().getResourceAsStream("/stop_direction_mapping.json");
        directionMapping = new ObjectMapper().readValue(is, new TypeReference<>() {});
    }

    /**
     * Sinhronizirano prenese nov model zamud iz Supabase Storage in zamenja sejo.
     *
     * @throws OrtException če prenos ali ustvarjanje nove seje spodleti
     */
    public synchronized void reloadModel() throws OrtException {
        OrtSession newModel = env.createSession(
                supabaseStorageClient.download("gold", "models/model_bus_delay.onnx"),
                new OrtSession.SessionOptions()
        );
        try { model.close(); } catch (OrtException e) { /* best-effort close before reload */ }
        model = newModel;
    }

    /**
     * Napove zamudo avtobusa za linijo, postajo, čas in vremenske pogoje.
     *
     * Če kombinacije routeId in stopId ni v preslikavi smeri, vrne nevtralno
     * zamudo 0 brez klica ONNX modela.
     *
     * @param routeId identifikator Marprom linije
     * @param stopSequence zaporedna številka postaje v vožnji
     * @param hour ura odhoda oziroma opazovanja
     * @param dayOfWeek dan v tednu, 1-7
     * @param isWeekend 1 za vikend, sicer 0
     * @param temperature temperatura v stopinjah Celzija
     * @param rain količina padavin
     * @param windSpeed hitrost vetra
     * @param stopId identifikator vstopne postaje
     * @return napovedana zamuda v sekundah
     */
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
