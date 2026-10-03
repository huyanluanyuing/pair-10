package ca.en.solution.history;

import java.util.List;

public interface HistoryRepository {
    List<PerformanceSnapshot> historyOf(String portfolioId);
}
