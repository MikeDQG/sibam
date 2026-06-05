package com.sibam.service;
import ai.onnxruntime.*;
import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.sibam.integration.supabase.SupabaseStorageClient;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

@Service
public class BikePredictionService {
    private OrtSession modelBikes;
    private OrtSession modelStands;
    private OrtSession modelBikeAvailable;
    private OrtSession modelStandAvailable;

    private OrtEnvironment env;

    private final SupabaseStorageClient supabaseStorageClient;

    public BikePredictionService(SupabaseStorageClient supabaseStorageClient) {
        this.supabaseStorageClient = supabaseStorageClient;
    }

    @PostConstruct
    public void loadModels() throws OrtException {
        env = OrtEnvironment.getEnvironment();
        modelBikes          = loadFromGold("model_bikes.onnx");
        modelStands         = loadFromGold("model_stands.onnx");
        modelBikeAvailable  = loadFromGold("model_available_bike.onnx");
        modelStandAvailable = loadFromGold("model_available_stand.onnx");
    }

    private OrtSession loadFromGold(String filename) throws OrtException {
        byte[] bytes = supabaseStorageClient.download("gold", "models/" + filename);
        return env.createSession(bytes, new OrtSession.SessionOptions());
    }


    public synchronized void reloadModels() throws OrtException {
        OrtSession newBikes          = loadFromGold("model_bikes.onnx");
        OrtSession newStands         = loadFromGold("model_stands.onnx");
        OrtSession newBikeAvailable  = loadFromGold("model_available_bike.onnx");
        OrtSession newStandAvailable = loadFromGold("model_available_stand.onnx");

        closeQuietly(modelBikes);
        closeQuietly(modelStands);
        closeQuietly(modelBikeAvailable);
        closeQuietly(modelStandAvailable);

        modelBikes          = newBikes;
        modelStands         = newStands;
        modelBikeAvailable  = newBikeAvailable;
        modelStandAvailable = newStandAvailable;
    }

    private void closeQuietly(OrtSession session) {
        try { session.close(); } catch (OrtException e) { /* best-effort close before reload */ }
    }

    public synchronized BikePredictionResponse predict(BikePredictionRequest req) throws OrtException {
        float[] features = {
                req.stationNumber(),
                req.hour(),
                req.dayOfWeek(),
                req.isWeekend(),
                req.temperature(),
                req.rain(),
                req.windSpeed()
        };

        int predictedBikes  = Math.round(runRegressor(modelBikes, features));
        int predictedStands = Math.round(runRegressor(modelStands, features));
        double bikeProb     = runClassifier(modelBikeAvailable, features);
        double standProb    = runClassifier(modelStandAvailable, features);

        return new BikePredictionResponse(
                Math.max(0, predictedBikes),
                Math.max(0, predictedStands),
                bikeProb,
                standProb
        );
    }

    private float runRegressor(OrtSession session, float[] features) throws OrtException {
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), new long[]{1, 7});
        try (var result = session.run(Map.of("float_input", tensor))) {
            float[][] output = (float[][]) result.get(0).getValue();
            return output[0][0];
        }
    }
    
    @SuppressWarnings("unchecked")
    private double runClassifier(OrtSession session, float[] features) throws OrtException {
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), new long[]{1, 7});
        try (var result = session.run(Map.of("float_input", tensor))) {
            List<OnnxMap> probList = (List<OnnxMap>) result.get(1).getValue();
            Map<Long, Float> probMap = (Map<Long, Float>) probList.get(0).getValue();
            return probMap.get(1L);
        }
    }


}