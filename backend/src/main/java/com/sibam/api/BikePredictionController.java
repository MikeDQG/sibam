package com.sibam.api;

import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import com.sibam.service.BikePredictionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/predict")
public class BikePredictionController {

    private final BikePredictionService predictionService;

    public BikePredictionController(BikePredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/bikes")
    public BikePredictionResponse predictBikes(@RequestBody BikePredictionRequest request) throws Exception {
        return predictionService.predict(request);
    }
}
