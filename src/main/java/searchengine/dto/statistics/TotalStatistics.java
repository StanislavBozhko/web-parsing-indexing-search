package searchengine.dto.statistics;

import lombok.Value;

@Value
public class TotalStatistics {
    int sites;
    int pages;
    int lemmas;
    boolean indexing;
}
