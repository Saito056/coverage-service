package co.com.andesexpress.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.com.andesexpress.model.CoverageResult;
import co.com.andesexpress.model.Location;
import co.com.andesexpress.model.Weight;
import co.com.andesexpress.usecase.validatecoverage.ValidateCoverageUseCase;

@RestController
@RequestMapping("/coverage")
public class CoverageController {

    private final ValidateCoverageUseCase useCase;

    public CoverageController(ValidateCoverageUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/validate")
    public CoverageResponse validate(@RequestBody CoverageRequest request) {
        if (request == null || request.origin() == null || request.destination() == null) {
            throw new IllegalArgumentException("origin, destination y totalWeightKg son obligatorios");
        }

        CoverageResult result = useCase.execute(
                new Location(request.origin().departmentId(), request.origin().cityId()),
                new Location(request.destination().departmentId(), request.destination().cityId()),
                new Weight(request.totalWeightKg()));

        if (result instanceof CoverageResult.Valid valid) {
            return new CoverageResponse.Valid(true, valid.zone().name(),
                    new CoverageResponse.FareBody(valid.fare().amount(), valid.fare().currency()));
        }
        CoverageResult.Invalid invalid = (CoverageResult.Invalid) result;
        return new CoverageResponse.Invalid(false, invalid.reason().name(), invalid.message());
    }
}
