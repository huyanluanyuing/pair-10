package ca.en.solution.holding;

import java.math.BigDecimal;

import ca.en.solution.common.Rounding;

public record AllocationResponse(String assetClass, BigDecimal value, BigDecimal percent) {

    static AllocationResponse from(String assetClass, BigDecimal value, BigDecimal percent) {
        return new AllocationResponse(assetClass, Rounding.money(value), Rounding.ratio(percent));
    }
}
