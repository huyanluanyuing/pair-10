package ca.en.solution.history;

import java.math.BigDecimal;
import java.time.LocalDate;

import ca.en.solution.currency.CurrencyContext;

public record CurrencyPerformanceSnapshot(LocalDate date, BigDecimal marketValue, String currency,
        BigDecimal exchangeRate) {

    public static CurrencyPerformanceSnapshot from(PerformanceSnapshot snapshot, CurrencyContext context) {
        return new CurrencyPerformanceSnapshot(snapshot.date(), context.convertMoney(snapshot.marketValue()),
                context.currency(), context.exchangeRate());
    }
}
