package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ConnectSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ResponseBoolean;
import searchengine.dto.ResponseWithMsg;
import searchengine.dto.indexing.IndexParser;
import searchengine.dto.lemmas.LemmaParser;
import searchengine.dto.parsingURL.SiteIndexing;

import searchengine.model.SiteDB;
import searchengine.model.SiteStatusEnum;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private ExecutorService executorService;
    private static final int coreCount = Runtime.getRuntime().availableProcessors();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaParser lemmaParser;
    private final IndexRepository indexRepository;
    private final IndexParser indexParser;
    private final SitesList sitesList;
    private final ConnectSettings connectSettings;
    public static volatile boolean isStopped = false;
    public static long startTime;

    @Override
    public ResponseBoolean startIndexing() {
        if (isIndexing()) {
            return new ResponseWithMsg(false, "Индексация уже запущена");
        }
        isStopped = false;
        startTime = System.currentTimeMillis();
        List<Site> siteList = sites.getSites();
        executorService = Executors.newFixedThreadPool(coreCount);
        for (Site site : siteList) {
            String url = site.getUrl();
            SiteDB siteDB = siteRepository.findByUrl(url);
            if (siteDB != null) {
                System.out.println("Delete from Database site " + url);
                siteRepository.delete(siteDB);
            }
            executorService.submit(new SiteIndexing(siteRepository, pageRepository, url, sitesList, false,
                    lemmaRepository, lemmaParser, indexRepository, indexParser, connectSettings));
        }
        executorService.shutdown();
        return new ResponseBoolean(true);
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexing()) {
            // Останавливаем индексацию
            executorService.shutdownNow();
            isStopped = true;
            stopAllSitesDB();
            return true;
        } else {
            // Индексация не может быть остановлена т.к. не была запущена
            return false;
        }
    }

    @Override
    public boolean pageIndexing(String url) {
        if (checkUrl(url)) {
            System.out.println("Start indexing page url - " + url);
            startTime = System.currentTimeMillis();
            executorService = Executors.newFixedThreadPool(coreCount);
            executorService.submit(new SiteIndexing(siteRepository, pageRepository, url, sitesList, true,
                    lemmaRepository, lemmaParser, indexRepository, indexParser, connectSettings));
            executorService.shutdown();
            return true;
        }
        return false;
    }

    private boolean checkUrl(String url) {
        List<Site> siteList = sitesList.getSites();
        for (Site site : siteList) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

    private boolean isIndexing() {
        siteRepository.flush();
        List<SiteDB> siteList = siteRepository.findAll();
        for (SiteDB siteDB : siteList) {
            if (siteDB.getStatus() == SiteStatusEnum.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private void stopAllSitesDB() {
        siteRepository.flush();
        List<SiteDB> siteList = siteRepository.findAll();
        for (SiteDB siteDB : siteList) {
            if (siteDB.getStatus() == SiteStatusEnum.INDEXING) {
                siteDB.setLastError("Индексация остановлена пользователем");
                siteDB.setStatus(SiteStatusEnum.FAILED);
                siteDB.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteDB);
                siteRepository.flush();
            }
        }
    }
}
