package ca.en.solution.history;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.en.solution.common.BadRequestException;
import ca.en.solution.common.SeedData;
import ca.en.solution.portfolio.PortfolioNotFoundException;

@Service
public class PerformanceHistoryService {

    private final SeedData seedData;
    private final HistoryRepository historyRepository;
    private final Clock clock;

    public PerformanceHistoryService(SeedData seedData, HistoryRepository historyRepository, Clock clock) {
        this.seedData = seedData;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    public List<PerformanceSnapshot> historyOf(String portfolioId, String requestedRange) {
        Range range = Range.parse(requestedRange);
        if (!seedData.hasPortfolio(portfolioId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate start = range.start(today);
        return historyRepository.historyOf(portfolioId).stream()
                .filter(snapshot -> !snapshot.date().isAfter(today))
                .filter(snapshot -> start == null || !snapshot.date().isBefore(start))
                .sorted(Comparator.comparing(PerformanceSnapshot::date))
                .map(PerformanceSnapshot::rounded)
                .toList();
    }

    private enum Range {
        ONE_DAY("1D"), ONE_MONTH("1M"), YEAR_TO_DATE("YTD"), ONE_YEAR("1Y"), ALL("All");

        private final String parameter;

        Range(String parameter) {
            this.parameter = parameter;
        }

        static Range parse(String value) {
            if (value == null) {
                return ALL;
            }
            for (Range range : values()) {
                if (range.parameter.equals(value)) {
                    return range;
                }
            }
            throw new BadRequestException("range must be one of: 1D, 1M, YTD, 1Y, All.");
        }

        LocalDate start(LocalDate today) {
            return switch (this) {
                case ONE_DAY -> today.minusDays(1);
                case ONE_MONTH -> today.minusMonths(1);
                case YEAR_TO_DATE -> today.withDayOfYear(1);
                case ONE_YEAR -> today.minusYears(1);
                case ALL -> null;
            };
        }
    }
}
