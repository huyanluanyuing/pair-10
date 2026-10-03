package ca.en.solution.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The one place output values are rounded: money to 2 decimals, ratios to 6, half-up.
 * An unknown value stays null.
 */
public final class Rounding {

    private Rounding() {
    }

    public static BigDecimal money(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratio(BigDecimal ratio) {
        return ratio == null ? null : ratio.setScale(6, RoundingMode.HALF_UP);
    }

}
