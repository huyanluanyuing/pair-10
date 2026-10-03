package ca.en.solution.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(TransactionType type, BigDecimal quantity, BigDecimal price, LocalDate date) {
}
