package ca.en.solution.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RoundingTest {

    @Test
    void moneyIsRoundedToTwoDecimalsHalfUp() {
        assertThat(Rounding.money(new BigDecimal("166.075"))).isEqualTo(new BigDecimal("166.08"));
        assertThat(Rounding.money(new BigDecimal("52.633"))).isEqualTo(new BigDecimal("52.63"));
        assertThat(Rounding.money(new BigDecimal("-0.005"))).isEqualTo(new BigDecimal("-0.01"));
    }

    @Test
    void wholeMoneyAmountsGetTwoDecimals() {
        assertThat(Rounding.money(new BigDecimal("200"))).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    void ratiosAreRoundedToSixDecimalsHalfUp() {
        assertThat(Rounding.ratio(new BigDecimal("0.5579399"))).isEqualTo(new BigDecimal("0.557940"));
        assertThat(Rounding.ratio(new BigDecimal("-0.01232877"))).isEqualTo(new BigDecimal("-0.012329"));
        assertThat(Rounding.ratio(new BigDecimal("0.0000005"))).isEqualTo(new BigDecimal("0.000001"));
    }

    @Test
    void anUnknownValueStaysNull() {
        assertThat(Rounding.money(null)).isNull();
        assertThat(Rounding.ratio(null)).isNull();
    }

}
