package ca.en.solution.currency;

import java.math.BigDecimal;

import ca.en.solution.common.Rounding;

public record CurrencyContext(String currency, BigDecimal exchangeRate) {

    public BigDecimal convertMoney(BigDecimal amount) {
        return amount == null ? null : Rounding.money(amount.multiply(exchangeRate));
    }
}
