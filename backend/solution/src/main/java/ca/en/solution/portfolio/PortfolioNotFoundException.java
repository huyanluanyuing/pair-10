package ca.en.solution.portfolio;

import ca.en.solution.common.NotFoundException;

public class PortfolioNotFoundException extends NotFoundException {
    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio '" + portfolioId + "' was not found.");
    }
}
