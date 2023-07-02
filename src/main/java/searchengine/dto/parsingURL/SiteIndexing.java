package searchengine.dto.parsingURL;

import lombok.RequiredArgsConstructor;
import searchengine.config.ConnectSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;

import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.IndexParser;
import searchengine.dto.lemmas.LemmaDto;
import searchengine.dto.lemmas.LemmaParser;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingServiceImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;


@RequiredArgsConstructor
public class SiteIndexing implements Runnable {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final String url;
    private final SitesList sitesList;
    private final boolean onlyPage;
    private final LemmaRepository lemmaRepository;
    private final LemmaParser lemmaParser;
    private final IndexRepository indexRepository;
    private final IndexParser indexParser;
    private final ConnectSettings connectSettings;
    private String mainUrl;
    private static final int coreCount = Runtime.getRuntime().availableProcessors();

    @Override
    public void run() {
        if (IndexingServiceImpl.isStopped) {
            return;
        }
        SiteDB siteDB = null;
        String path = "", comment;
        if (onlyPage) {
            comment = "Page " + url;
            char ch = '/';
            int index = url.indexOf(ch);
            int i = 0;
            while (index != -1 && i < 2) {
                i++;
                index = url.indexOf(ch, index + 1);
            }
            mainUrl = url.substring(0, index);
            path = url.substring(index);
            cleanOnePage(mainUrl, path);
        } else {
            comment = "Site " + url;
            mainUrl = url;
        }
        if (IndexingServiceImpl.isStopped) {
            return;
        }
        siteDB = initSiteDB(mainUrl, siteDB);
        List<PageDto> pageDtoList;
        try {
            pageDtoList = getPageDtoList();
            saveToBase(pageDtoList, siteDB, SiteStatusEnum.INDEXED);
        } catch (InterruptedException e) {
            //       e.printStackTrace();
            if (!IndexingServiceImpl.isStopped) {
                errorSite("InterruptedException: " + e.getMessage(), siteDB);
            }
            Thread.currentThread().interrupt();
            // throw new RuntimeException(e);
        }
        List<LemmaDB> lemmaDBList;
        if (onlyPage) {
            lemmaDBList = parsingLemmasOfPage(siteDB, getPageFromDB(siteDB, path));
        } else {
            lemmaDBList = parsingLemmasOfSite(siteDB);
        }
        if (lemmaDBList != null) {
            indexingLemmas(siteDB, lemmaDBList);
        }
        System.out.println(comment + " was totaly parsed in " + (System.currentTimeMillis() - IndexingServiceImpl.startTime) / 1000 + " сек");
    }

    private void cleanOnePage(String mainUrl, String path) {
        PageDB pageDB = null;
        SiteDB siteDBPage = siteRepository.findByUrl(mainUrl);
        if (path.isEmpty()) {
            return;
        }
        if (siteDBPage != null) {
            pageDB = getPageFromDB(siteDBPage, path);
        }
        if (pageDB != null) {
            System.out.println("Deleting lemmas and indexes of page " + path);
            cleanLemmasOfPage(siteDBPage, pageDB);
            pageRepository.delete(pageDB);
            updateStatusSite(siteDBPage, SiteStatusEnum.INDEXING);
        }
    }

    private String getName() {
        List<Site> sites = sitesList.getSites();
        for (Site map : sites) {
            if (map.getUrl().equals(mainUrl)) {
                return map.getName();
            }
        }
        return "";
    }

    private PageDB getPageFromDB(SiteDB siteDB, String path) {
        path = path.toLowerCase(Locale.ROOT);
        Iterator<PageDB> pageDBIterator = pageRepository.findByPathAndSite(path, siteDB).iterator();
        if (pageDBIterator.hasNext()) {
            return pageDBIterator.next();
        } else {
            System.out.println(siteDB.getName() + " - page " + path + " not found in DB!");
            return null;
        }
    }

    private void cleanLemmasOfPage(SiteDB siteDB, PageDB pageDB) {
        if (siteDB == null || pageDB == null || IndexingServiceImpl.isStopped) {
            return;
        }
        List<LemmaDB> lemmaDBList = siteDB.getLemmas();
        if (lemmaDBList.size() == 0) {
            System.out.println("Site " + siteDB.getName() + " - not found lemmas!");
            return;
        }
        List<IndexDB> indexDBList = pageDB.getIndexDBList();
        if (indexDBList.size() == 0) {
            return;
        }
        HashMap<String, LemmaDB> lemmaDBmap = new HashMap<>(lemmaDBList.size());
        lemmaDBList.forEach(lemDB -> lemmaDBmap.put(lemDB.getLemma(), lemDB));
        for (IndexDB indexDB : indexDBList) {
            LemmaDB lemmaDBIndex = indexDB.getLemma();
            LemmaDB lemmaDBSite = lemmaDBmap.get(lemmaDBIndex.getLemma());
            if (lemmaDBSite.getFrequency() > 1) {
                lemmaDBSite.setFrequency(lemmaDBSite.getFrequency() - 1);
            } else {
                lemmaDBList.remove(lemmaDBSite);
            }
        }
    }

    private List<PageDto> getPageDtoList() {
        System.out.println("Start indexing site url - " + url);
        if (IndexingServiceImpl.isStopped) {
            return null;
        }
        if (!Thread.interrupted()) {
            String urlFormat = url;
            Vector<PageDto> pageDtoVector = new Vector<>();
            Set<String> urlList = ConcurrentHashMap.newKeySet();
            urlList.add(urlFormat);
            ForkJoinPool forkJoinPool = new ForkJoinPool(coreCount);
            List<PageDto> parsedPages = forkJoinPool.invoke(new PageUrlParser(urlFormat, urlList, pageDtoVector, mainUrl, onlyPage, this, connectSettings));
            return new CopyOnWriteArrayList<>(parsedPages);
        } else {
            errorSite("Thread interrupted", siteRepository.findByUrl(mainUrl));
            //       throw new InterruptedException();
            return null;
        }
    }

    private SiteDB initSiteDB(String urlSite, SiteDB siteDB) {
        if (siteDB == null) {
            siteDB = siteRepository.findByUrl(urlSite);
        }
        if (siteDB == null) {
            siteDB = new SiteDB();
            siteDB.setUrl(urlSite);
            siteDB.setName(getName());
        }
        updateStatusSite(siteDB, SiteStatusEnum.INDEXING);
        System.out.println("Save new site url - " + urlSite);
        return siteDB;
    }

    protected void saveToBase(List<PageDto> pages, SiteDB siteDB, SiteStatusEnum newStatus) throws InterruptedException {
        if (!Thread.interrupted()) {
            if (IndexingServiceImpl.isStopped) {
                return;
            }
            if (siteDB == null) {
                siteDB = siteRepository.findByUrl(mainUrl);
            }
            List<PageDB> pageList = new ArrayList<>(pages.size());
            for (PageDto page : pages) {
                int start = page.getUrl().indexOf(mainUrl) + mainUrl.length();
                String pageFormat = page.getUrl().substring(start);
                pageList.add(new PageDB(siteDB, page.getCode(), page.getContent(), pageFormat));
            }
            pageRepository.saveAll(pageList);
            pageRepository.flush();
            updateStatusSite(siteDB, newStatus);
            if (newStatus == SiteStatusEnum.INDEXED) {
                System.out.println(siteDB.getName() + " - Site & Pages saved in DB in  " + (System.currentTimeMillis() - IndexingServiceImpl.startTime) / 1000 + " s");
            }
        } else {
            throw new InterruptedException();
        }
    }

    private void errorSite(String message, SiteDB siteDB) {
        if (siteDB == null) {
            siteDB = siteRepository.findByUrl(mainUrl);
        }
        siteDB.setLastError(message);
        siteDB.setStatus(SiteStatusEnum.FAILED);
        siteDB.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteDB);
    }

    private List<LemmaDB> parsingLemmasOfSite(SiteDB siteDB) {
        if (!Thread.interrupted()) {
            if (IndexingServiceImpl.isStopped) {
                return null;
            }
            if (siteDB == null) {
                siteDB = siteRepository.findByUrl(mainUrl);
            }
            updateStatusSite(siteDB, null);
            lemmaParser.run(siteDB, null);
            return putLemmasDtoDB(siteDB, lemmaParser.getLemmaDtoList());

        } else {
            throw new RuntimeException();
        }
    }

    private List<LemmaDB> parsingLemmasOfPage(SiteDB siteDB, PageDB pageDB) {
        if (!Thread.interrupted()) {
            if (IndexingServiceImpl.isStopped || pageDB == null || pageDB.getCode() >= 400) {
                return null;
            }
            updateStatusSite(siteDB, null);
            lemmaParser.run(siteDB, pageDB);
            return putLemmasDtoDB(siteDB, lemmaParser.getLemmaDtoList());
        } else {
            throw new RuntimeException();
        }

    }


    private List<LemmaDB> putLemmasDtoDB(SiteDB siteDB, List<LemmaDto> lemmaDtoList) {
        if (IndexingServiceImpl.isStopped) {
            return null;
        }
        List<LemmaDB> lemmaDBList = siteDB.getLemmas();
        if (lemmaDBList.size() == 0) {
            lemmaDtoList.forEach(lemmaDto -> lemmaDBList.add(new LemmaDB(siteDB, lemmaDto.getLemma(), lemmaDto.getFrequency())));
        } else {
            int i = 0;
            for (LemmaDto lemmaDto : lemmaDtoList) {
                String lemma = lemmaDto.getLemma();
                LemmaDB lemmaDB = lemmaDBList.stream()
                        .filter(lem -> (lem.getLemma().equals(lemma)))
                        .findFirst()
                        .orElse(null);
                if (lemmaDB != null) {
                    lemmaDB.setFrequency(lemmaDB.getFrequency() + lemmaDto.getFrequency());
                } else {
                    lemmaDBList.add(new LemmaDB(siteDB, lemmaDto.getLemma(), lemmaDto.getFrequency()));
                }
                i++;
                if (i % 1000 == 0) {
                    System.out.println(siteDB.getName() + " - Collected lemmas from " + i + " pages");
                }
            }
        }
        lemmaRepository.saveAll(lemmaDBList);
        lemmaRepository.flush();
        System.out.println(siteDB.getName() + " - " + lemmaDBList.size() + " lemmas was saved to DB");
        return lemmaDBList;
    }

    private void indexingLemmas(SiteDB siteDB, List<LemmaDB> lemmaDBList) {
        if (IndexingServiceImpl.isStopped) {
            return;
        }
        HashMap<String, LemmaDB> lemmaDBmap = new HashMap<>(lemmaDBList.size());
        lemmaDBList.forEach(lemDB -> lemmaDBmap.put(lemDB.getLemma(), lemDB));
        indexParser.run(siteDB, lemmaDBmap, lemmaParser.getIndexMap());
        putIndexesToDB(siteDB, indexParser.getIndexList());
    }

    private void putIndexesToDB(SiteDB siteDB, List<IndexDto> indexDtoList) {
        if (IndexingServiceImpl.isStopped) {
            return;
        }
        List<IndexDB> indexDBList = new ArrayList<>(indexDtoList.size());
        indexDtoList.forEach(indexDto -> indexDBList.add(new IndexDB(indexDto.getPage(), indexDto.getLemma(), indexDto.getRank())));
        indexRepository.saveAllAndFlush(indexDBList);
        System.out.println(siteDB.getName() + " - " + indexDBList.size() + " indexes was saved to DB");
        updateStatusSite(siteDB, SiteStatusEnum.INDEXED);
    }

    private void updateStatusSite(SiteDB siteDB, SiteStatusEnum newStatus) {
        if (newStatus != null && !IndexingServiceImpl.isStopped) {
            siteDB.setStatus(newStatus);
        }
        siteDB.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteDB);
    }
}
