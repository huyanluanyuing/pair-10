package ca.en.solution.holding;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HoldingController {

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping("/portfolios/{id}/holdings")
    public List<HoldingResponse> holdings(@PathVariable String id) {
        return holdingService.holdingsOf(id);
    }

}
