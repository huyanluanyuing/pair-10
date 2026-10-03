package ca.en.solution.history;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PerformanceHistoryController {

    private final PerformanceHistoryService performanceHistoryService;

    public PerformanceHistoryController(PerformanceHistoryService performanceHistoryService) {
        this.performanceHistoryService = performanceHistoryService;
    }

    @GetMapping("/portfolios/{id}/performance-history")
    public List<PerformanceSnapshot> performanceHistory(@PathVariable String id,
            @RequestParam(required = false) String range) {
        return performanceHistoryService.historyOf(id, range);
    }
}
