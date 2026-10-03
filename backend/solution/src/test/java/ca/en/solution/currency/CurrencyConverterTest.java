package ca.en.solution.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ca.en.solution.common.BadRequestException;

class CurrencyConverterTest {

    private final CurrencyConverter converter = new CurrencyConverter(() -> new BigDecimal("0.73"));

    @Test
    void defaultsToCadWithoutCallingTheRateClient() {
        CurrencyContext context = converter.contextFor(null);

        assertThat(context.currency()).isEqualTo("CAD");
        assertThat(context.exchangeRate()).isEqualByComparingTo("1");
        assertThat(context.convertMoney(new BigDecimal("227.5"))).isEqualByComparingTo("227.50");
    }

    @Test
    void convertsUsdMoneyAtFullPrecisionBeforeRounding() {
        CurrencyContext context = converter.contextFor("USD");

        assertThat(context.currency()).isEqualTo("USD");
        assertThat(context.exchangeRate()).isEqualByComparingTo("0.73");
        assertThat(context.convertMoney(new BigDecimal("227.5"))).isEqualByComparingTo("166.08");
        assertThat(context.convertMoney(null)).isNull();
    }

    @Test
    void rejectsUnsupportedAndNonExactCurrencyValues() {
        assertThatThrownBy(() -> converter.contextFor("usd")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> converter.contextFor("EUR")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> converter.contextFor("")).isInstanceOf(BadRequestException.class);
    }
}
