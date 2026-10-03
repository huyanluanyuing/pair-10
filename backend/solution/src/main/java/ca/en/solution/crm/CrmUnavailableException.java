package ca.en.solution.crm;

public class CrmUnavailableException extends RuntimeException {
    public CrmUnavailableException() {
        super("Portfolio metadata is temporarily unavailable. Please try again later.");
    }

    public CrmUnavailableException(Throwable cause) {
        super("Portfolio metadata is temporarily unavailable. Please try again later.", cause);
    }
}
