package ca.en.solution.holding;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.en.solution.currency.CurrencyContext;
import ca.en.solution.currency.CurrencyConverter;

@RestController
public class HoldingController {

    private final HoldingService holdingService;
    private final CurrencyConverter currencyConverter;

    public HoldingController(HoldingService holdingService, CurrencyConverter currencyConverter) {
        this.holdingService = holdingService;
        this.currencyConverter = currencyConverter;
    }

    @GetMapping("/portfolios/{id}/holdings")
    public List<CurrencyHoldingResponse> holdings(@PathVariable String id,
            @RequestParam(required = false) String currency) {
        CurrencyContext context = currencyConverter.contextFor(currency);
        return holdingService.valuationsOf(id).stream().map(valuation -> CurrencyHoldingResponse.from(valuation, context))
                .toList();
    }

}
