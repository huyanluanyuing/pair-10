package ca.en.solution.ledger;

import java.math.BigDecimal;

/**
 * What a holding amounts to after its transactions are replayed.
 * The average cost is null when nothing is held.
 */
public record Position(BigDecimal currentQuantity, BigDecimal averageCostBasisPerShare) {
}
