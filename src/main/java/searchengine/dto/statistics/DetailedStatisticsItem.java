package searchengine.dto.statistics;

import lombok.Value;
import searchengine.model.SiteStatusEnum;


@Value
public class DetailedStatisticsItem {
    String url;
    String name;
    SiteStatusEnum status;
    long statusTime;
    String error;
    int pages;
    int lemmas;
}
