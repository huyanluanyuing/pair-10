package ca.en.solution.history;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.en.solution.currency.CurrencyContext;
import ca.en.solution.currency.CurrencyConverter;

@RestController
public class PerformanceHistoryController {

    private final PerformanceHistoryService performanceHistoryService;
    private final CurrencyConverter currencyConverter;

    public PerformanceHistoryController(PerformanceHistoryService performanceHistoryService, CurrencyConverter currencyConverter) {
        this.performanceHistoryService = performanceHistoryService;
        this.currencyConverter = currencyConverter;
    }

    @GetMapping("/portfolios/{id}/performance-history")
    public List<CurrencyPerformanceSnapshot> performanceHistory(@PathVariable String id,
            @RequestParam(required = false) String range, @RequestParam(required = false) String currency) {
        CurrencyContext context = currencyConverter.contextFor(currency);
        return performanceHistoryService.historyOf(id, range).stream()
                .map(snapshot -> CurrencyPerformanceSnapshot.from(snapshot, context)).toList();
    }
}
