package ca.en.solution.holding;

import java.math.BigDecimal;

/**
 * One holding before valuation. The cost basis is null when nothing is held.
 */
public record HoldingPosition(String ticker, String name, String assetClass, BigDecimal quantity,
        BigDecimal costBasisPerShare, BigDecimal price, BigDecimal previousClosePrice) {
}
