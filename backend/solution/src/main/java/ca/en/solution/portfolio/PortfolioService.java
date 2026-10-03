package ca.en.solution.portfolio;

import ca.en.solution.crm.CrmClient;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
    private final CrmClient crmClient;

    public PortfolioService(CrmClient crmClient) {
        this.crmClient = crmClient;
    }

    public PortfolioMetadata getPortfolio(String portfolioId) {
        return crmClient.fetchPortfolio(portfolioId);
    }
}
