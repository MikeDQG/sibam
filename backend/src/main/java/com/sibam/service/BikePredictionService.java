package com.sibam.service;
import ai.onnxruntime.*;
import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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

    @PostConstruct
    public void loadModels() throws Exception {
        env = OrtEnvironment.getEnvironment();
        modelBikes         = loadModel("/models/model_bikes.onnx");
        modelStands        = loadModel("/models/model_stands.onnx");
        modelBikeAvailable  = loadModel("/models/model_available_bike.onnx");
        modelStandAvailable = loadModel("/models/model_available_stand.onnx");

    }

    private OrtSession loadModel(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("ONNX model not found: " + resourcePath);
        }
        byte[] bytes = is.readAllBytes();
        is.close();
        return env.createSession(bytes, new OrtSession.SessionOptions());
    }

    public BikePredictionResponse predict(BikePredictionRequest req) throws OrtException {
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

//    private double runClassifier(OrtSession session, float[] features) throws OrtException {
//        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), new long[]{1, 7});
//        try (var result = session.run(Map.of("float_input", tensor))) {
//            // classifier returns probabilities for [class0, class1]
//            float[][][] output = (float[][][]) result.get(1).getValue();
//            return output[0][0][1]; // probability that bikes/stands ARE available
//        }
//    }

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