import { startPortfolioApi } from '../support/portfolio-api.mjs';
import { options } from '../support/http.mjs';
startPortfolioApi({ ...options(4000), name: 'Frontend mock API' });
