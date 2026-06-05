package com.sibam.api;
import ai.onnxruntime.OrtException;
import com.sibam.dto.prediction.BusDelayPredictionRequest;
import com.sibam.dto.prediction.BusDelayPredictionResponse;
import com.sibam.service.BusDelayPredictionService;
import org.springframework.web.bind.annotation.*;

/**
 * REST krmilnik za napoved zamude avtobusa.
 * Endpoint: POST /api/bus-delay/predict
 * Sprejme podatke o liniji, postaji, času in vremenu, vrne napovedano zamudo v sekundah.
 */

@RestController
@RequestMapping("/api/bus-delay")
public class BusDelayPredictionController {
    private final BusDelayPredictionService service;

    public BusDelayPredictionController(BusDelayPredictionService service) {
        this.service = service;
    }

    /**
     * Izvede ONNX inferenco zamude za eno vstopno postajo.
     *
     * @param req vhodne značilke vožnje, postaje in vremena
     * @return napovedana zamuda v sekundah
     */
    @PostMapping("/predict")
    public BusDelayPredictionResponse predict(@RequestBody BusDelayPredictionRequest req) throws OrtException {
        int delay = service.predictDelay(
                req.routeId(), req.stopSequence(), req.hour(), req.dayOfWeek(),
                req.isWeekend(), req.temperature(), req.rain(), req.windSpeed(),
                req.stopId()
        );
        return new BusDelayPredictionResponse(delay);
    }
}
