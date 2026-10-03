package ca.en.solution.ledger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;

/**
 * Derives a holding's quantity and average cost from its transactions (average-cost accounting).
 */
public final class LedgerReplay {

    // Working precision for the average; rounding for display happens at output.
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private LedgerReplay() {
    }

    public static Position replay(List<Transaction> transactions) {
        transactions.forEach(LedgerReplay::validate);
        // The sort is stable, so transactions on the same date keep their input order.
        List<Transaction> ordered = transactions.stream()
                .sorted(Comparator.comparing(Transaction::date))
                .toList();

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal averageCost = null;
        for (Transaction transaction : ordered) {
            if (transaction.type() == TransactionType.BUY) {
                BigDecimal heldCost = averageCost == null ? BigDecimal.ZERO : quantity.multiply(averageCost);
                BigDecimal boughtCost = transaction.quantity().multiply(transaction.price());
                quantity = quantity.add(transaction.quantity());
                averageCost = heldCost.add(boughtCost).divide(quantity, PRECISION);
            } else {
                if (transaction.quantity().compareTo(quantity) > 0) {
                    throw new InvalidLedgerException("SELL of " + transaction.quantity() + " on "
                            + transaction.date() + " is more than the " + quantity + " held");
                }
                quantity = quantity.subtract(transaction.quantity());
                if (quantity.signum() == 0) {
                    averageCost = null;
                }
            }
        }
        return new Position(quantity, averageCost);
    }

    private static void validate(Transaction transaction) {
        if (transaction.type() == null) {
            throw new InvalidLedgerException("A transaction needs a type of BUY or SELL");
        }
        if (transaction.date() == null) {
            throw new InvalidLedgerException("A transaction needs a date");
        }
        if (transaction.quantity() == null || transaction.quantity().signum() <= 0) {
            throw new InvalidLedgerException("A transaction quantity must be greater than 0");
        }
        if (transaction.price() == null || transaction.price().signum() < 0) {
            throw new InvalidLedgerException("A transaction price must not be negative");
        }
    }

}
