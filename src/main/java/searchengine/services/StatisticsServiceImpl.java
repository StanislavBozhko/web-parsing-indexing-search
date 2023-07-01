package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteDB;
import searchengine.model.SiteStatusEnum;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> list = getDetailedListStatistics();
        return new StatisticsResponse(true, new StatisticsData(total, list));
    }

    private TotalStatistics getTotalStatistics() {
        int sites = (int) siteRepository.count();
        int pages = (int) pageRepository.count();
        int lemmas = (int) lemmaRepository.count();
        return new TotalStatistics(sites, pages, lemmas, true);
    }

    private List<DetailedStatisticsItem> getDetailedListStatistics() {
        List<SiteDB> siteList = siteRepository.findAll();
        List<DetailedStatisticsItem> result = new ArrayList<>();
        for (SiteDB site : siteList) {
            DetailedStatisticsItem item = getDetailed(site);
            result.add(item);
        }
        return result;
    }

    private DetailedStatisticsItem getDetailed(SiteDB siteDB) {
        String url = siteDB.getUrl();
        String name = siteDB.getName();
        SiteStatusEnum status = siteDB.getStatus();
        long statusTime = siteDB.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String error = siteDB.getLastError();
        int pages = (int) pageRepository.countBySite(siteDB);
        int lemmas = (int) lemmaRepository.countBySiteEntityId(siteDB);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }


}
