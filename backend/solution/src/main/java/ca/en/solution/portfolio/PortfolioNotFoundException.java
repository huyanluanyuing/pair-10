package ca.en.solution.portfolio;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio '" + portfolioId + "' was not found.");
    }
}
