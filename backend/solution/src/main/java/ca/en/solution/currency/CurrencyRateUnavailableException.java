package ca.en.solution.currency;

public class CurrencyRateUnavailableException extends RuntimeException {

    public CurrencyRateUnavailableException() {
        super("Currency rate service is unavailable.");
    }

    public CurrencyRateUnavailableException(Throwable cause) {
        super("Currency rate service is unavailable.", cause);
    }
}
