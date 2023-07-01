package searchengine.dto.lemmas;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LemmaData implements LemmaParser {
    private final PageRepository pageRepository;

    private Set<LemmaDto> lemmaDtoList;

    private Map<PageDB, Map<String, Integer>> indexes;

    @Override
    public void run(SiteDB siteDB, PageDB pageDB) {
        TreeMap<String, Integer> lemmas = new TreeMap<>();
        indexes = new HashMap<>();
        lemmaDtoList = ConcurrentHashMap.newKeySet();
        ArrayList<PageDB> pageDBList = new ArrayList<>();
        if (pageDB != null) {
            pageDBList.add(pageDB);
        } else {
            pageDBList = pageRepository.findBySite(siteDB);
        }
        LemmaAnalyzer lemmaAnalyzer = null;
        try {
            lemmaAnalyzer = LemmaAnalyzer.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i = 0;
        long time1 = System.currentTimeMillis();
        for (PageDB page : pageDBList) {
            if (page.getCode() >= 400) {
                continue;
            }
            Map<String, Integer> lemmasPage = lemmaAnalyzer.collectLemmasPage(page);
            indexes.put(page, lemmasPage);
            for (String word : lemmasPage.keySet()) {
                int freq = lemmas.getOrDefault(word, 0) + 1;
                lemmas.put(word, freq);
            }
            i++;
            if (i % 500 == 0) {
                System.out.println(siteDB.getName() + " - Collected lemmas from " + i + " pages");
            }
        }
        lemmas.forEach((k, v) -> lemmaDtoList.add(new LemmaDto(k, v)));
        long time2 = System.currentTimeMillis();
        System.out.println(siteDB.getName() + " - lemmas " + lemmaDtoList.size() + " was parsed from pages (size = " + pageDBList.size() + ") in " + (time2 - time1));
    }

    @Override
    public List<LemmaDto> getLemmaDtoList() {
        return lemmaDtoList.stream().toList();
    }

    @Override
    public Map<PageDB, Map<String, Integer>> getIndexMap() {
        return indexes;
    }

}
