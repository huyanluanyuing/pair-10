package ca.en.solution.portfolio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.en.solution.currency.CurrencyConverter;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {
    private final PortfolioService service;
    private final CurrencyConverter currencyConverter;

    public PortfolioController(PortfolioService service, CurrencyConverter currencyConverter) {
        this.service = service;
        this.currencyConverter = currencyConverter;
    }

    @GetMapping("/{id}")
    public CurrencyPortfolioResponse getPortfolio(@PathVariable String id,
            @RequestParam(required = false) String currency) {
        return CurrencyPortfolioResponse.from(service.getPortfolio(id), currencyConverter.contextFor(currency));
    }
}
