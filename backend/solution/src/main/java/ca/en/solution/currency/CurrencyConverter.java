package ca.en.solution.currency;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class CurrencyConverter {

    private final CurrencyRateClient currencyRateClient;

    public CurrencyConverter(CurrencyRateClient currencyRateClient) {
        this.currencyRateClient = currencyRateClient;
    }

    public CurrencyContext contextFor(String requestedCurrency) {
        return switch (DisplayCurrency.parse(requestedCurrency)) {
            case CAD -> new CurrencyContext("CAD", BigDecimal.ONE);
            case USD -> new CurrencyContext("USD", currencyRateClient.cadToUsdRate());
        };
    }
}
