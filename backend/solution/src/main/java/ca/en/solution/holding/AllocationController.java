package ca.en.solution.holding;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/portfolios/{id}/allocation")
    public List<AllocationResponse> allocation(@PathVariable String id) {
        return allocationService.allocationOf(id);
    }
}
