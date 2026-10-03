package ca.en.solution.holding;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AllocationService {

    private final HoldingService holdingService;

    public AllocationService(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    public List<AllocationResponse> allocationOf(String portfolioId) {
        return AllocationCalculator.allocate(holdingService.valuationsOf(portfolioId));
    }
}
