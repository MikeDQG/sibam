package com.sibam.api;

import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.dto.prediction.BikePredictionResponse;
import com.sibam.service.BikePredictionService;
import org.springframework.web.bind.annotation.*;

/**
 * Controller za neposredno testiranje MBajk ML napovedi.
 *
 * Endpoint sprejme značilke postaje, časa in vremena ter vrne napoved prostih
 * koles, stojal in verjetnosti razpoložljivosti.
 */
@RestController
@RequestMapping("/predict")
public class BikePredictionController {

    private final BikePredictionService predictionService;

    public BikePredictionController(BikePredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /**
     * Izvede ONNX inferenco za MBajk postajo.
     *
     * @param request vhodne značilke za modele koles in stojal
     * @return napoved števila in verjetnosti razpoložljivosti
     */
    @PostMapping("/bikes")
    public BikePredictionResponse predictBikes(@RequestBody BikePredictionRequest request) throws Exception {
        return predictionService.predict(request);
    }
}
