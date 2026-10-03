package ca.en.solution.crm;

import ca.en.solution.portfolio.PortfolioMetadata;

public interface CrmClient {
    PortfolioMetadata fetchPortfolio(String portfolioId);
}
