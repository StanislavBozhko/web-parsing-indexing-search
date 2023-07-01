package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SearchProperties;
import searchengine.dto.lemmas.LemmaAnalyzer;
import searchengine.dto.search.SearchDto;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SearchProperties searchProperties;

    private LemmaAnalyzer lemmaAnalyzer;


    @Override
    public SearchResponse allSiteSearch(String text, int offset, int limit) {
        System.out.println("Searching \"" + text + "\" in all sites");
        List<SearchDto> searchResults = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse(true, 0, searchResults);
        List<SearchDto> searchDtoList = new ArrayList<>();
        List<SiteDB> siteDBList = siteRepository.findAll();
        if (siteDBList.size() == 0) {
            return searchResponse;
        }
        try {
            lemmaAnalyzer = LemmaAnalyzer.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<SearchDto> searchData;
        for (SiteDB siteDB : siteDBList) {
            if (!siteDB.getStatus().equals(SiteStatusEnum.INDEXED)) {
                continue;
            }
            searchData = collectDataForSite(siteDB, text);
            searchDtoList.addAll(searchData);
        }
        if (searchDtoList.size() == 0) {
            return searchResponse;
        }

        searchDtoList.sort((s1, s2) -> Float.compare(s2.getRelevance(), s1.getRelevance()));
        searchResults = prepareDataForWeb(searchDtoList, offset, limit);

        System.out.println("Finished searching!");
        searchResponse.setCount(searchDtoList.size());
        searchResponse.setData(searchResults);
        return searchResponse;
    }

    @Override
    public SearchResponse siteSearch(String searchText, String url, int offset, int limit) {
        System.out.println("Searching \"" + searchText + "\" on site - " + url);
        List<SearchDto> searchResults = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse(true, 0, searchResults);
        SiteDB siteDB = siteRepository.findByUrl(url);
        if (siteDB == null) {
            return searchResponse;
        }
        try {
            lemmaAnalyzer = LemmaAnalyzer.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<SearchDto> searchData = collectDataForSite(siteDB, searchText); // getSearchDtoList(filteredLemmaDBList, lemmaTextList, offset, limit);
        searchResults = prepareDataForWeb(searchData, offset, limit);
        System.out.println("Finished searching!");
        searchResponse.setCount(searchData.size());
        searchResponse.setData(searchResults);
        return searchResponse;
    }

    private List<SearchDto> collectDataForSite(SiteDB siteDB, String searchText) {
        List<SearchDto> searchData = new ArrayList<>();

        List<String> lemmaTextList = lemmaAnalyzer.getLemmaSet(searchText, searchProperties.isOnlyFirstNormalWord()).stream().toList();
        List<LemmaDB> foundLemmaDBList = getLemmaListFromSite(lemmaTextList, siteDB);
        float totalPagesOk = (float) siteDB.getPages().stream().filter(p -> p.getCode() < 400).count(); // pageRepository.countBySite(siteDB);
        List<LemmaDB> filteredLemmaDBList = new ArrayList<>(foundLemmaDBList.stream()
                .filter(lemmaDB -> lemmaDB.getFrequency() * 100 / totalPagesOk < searchProperties.getPercentFilteredLemmas())
                .toList());
        if (filteredLemmaDBList.size() == 0) {
            return searchData;
        }
        searchData = getSearchDtoList(filteredLemmaDBList, lemmaTextList); // lemmaTextList
        return searchData;
    }

    private List<SearchDto> prepareDataForWeb(List<SearchDto> searchData, int offset, int limit) {
        List<SearchDto> searchDtoList = new ArrayList<>();
        if (offset > searchData.size()) {
            return new ArrayList<>();
        }
        int end = offset + Math.min(searchData.size() - offset, limit);
        for (int i = offset; i < end; i++) {
            searchDtoList.add(searchData.get(i));
        }
        return searchDtoList;
    }

    private List<LemmaDB> getLemmaListFromSite(List<String> lemmaStringList, SiteDB siteDB) {
        lemmaRepository.flush();
        List<LemmaDB> lemmaDBList = lemmaRepository.findLemmaListBySite(lemmaStringList, siteDB);
        lemmaDBList.sort(Comparator.comparingInt(LemmaDB::getFrequency));
        return lemmaDBList;
    }

    private List<SearchDto> getSearchDtoList(List<LemmaDB> lemmaDBList, List<String> lemmaStringList) { //
        pageRepository.flush();
        Set<PageDB> pageDBSet = pageRepository.findByLemma(lemmaDBList.get(0));
        for (int i = 1; i < lemmaDBList.size(); i++) {
            if (pageDBSet.size() == 0) {
                return new ArrayList<>();
            }
            pageDBSet = pageRepository.findByLemmaAndPageList(pageDBSet, lemmaDBList.get(i));
        }
        if (pageDBSet.size() == 0) {
            return new ArrayList<>();
        }
        List<PageDB> pageDBList = new ArrayList<>(pageDBSet);
        indexRepository.flush();
        List<IndexDB> indexDBList = indexRepository.findByLemmasAndPages(lemmaDBList, pageDBList);
        List<Map.Entry<PageDB, Float>> pageDBSortByRelatRelevance = getPagesRelatRelevance(pageDBList, indexDBList);
        List<SearchDto> searchData = getSearchData(pageDBSortByRelatRelevance, lemmaStringList);
        return searchData;
    }

    private List<SearchDto> getSearchData(List<Map.Entry<PageDB, Float>> sortedMapEntry, List<String> lemmaStringList) { // List<String> lemmaStringList
        List<SearchDto> resultList = new ArrayList<>();
        for (Map.Entry<PageDB, Float> entry : sortedMapEntry) {
            PageDB pageDB = entry.getKey();
            float relatRelevance = entry.getValue();
            String uri = pageDB.getPath();
            String content = pageDB.getContent();
            SiteDB pageDBSite = pageDB.getSite();
            String siteUrl = pageDBSite.getUrl();
            String siteName = pageDBSite.getName();

            String title = lemmaAnalyzer.clearHTML(content, "title");
            StringBuilder contentClear = new StringBuilder(title);
            contentClear.append(" ");
            contentClear.append(lemmaAnalyzer.clearHTML(content, "body"));
            String snippet = getSnippet(contentClear.toString(), lemmaStringList);
            resultList.add(new SearchDto(siteUrl, siteName, uri, title, snippet, relatRelevance));
        }
        return resultList;
    }

    private String getSnippet(String content, List<String> lemmaStringList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder snippet = new StringBuilder();
        for (String lemma : lemmaStringList) {
            lemmaIndex.addAll(lemmaAnalyzer.findPositionsLemmaInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsStringList = getWordsFromText(content, lemmaIndex);
        int qty = Math.min(5, wordsStringList.size());
        for (int i = 0; i < qty; i++) {
            snippet.append(wordsStringList.get(i)).append("... ");
        }
        return snippet.toString();
    }

    private List<String> getWordsFromText(String text, List<Integer> lemmaIndex) {
        List<String> wordsList = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = text.indexOf(" ", start);
            int nextP = i + 1;
            while (nextP < lemmaIndex.size() && lemmaIndex.get(nextP) - end > 0 && lemmaIndex.get(nextP) - end < 5) {
                end = text.indexOf(" ", lemmaIndex.get(nextP));
                nextP += 1;
            }
            i = nextP - 1;
            wordsList.add(getBoldWordsByIndex(start, end, text));
        }
        wordsList.sort(Comparator.comparingInt(String::length).reversed());
        return wordsList;
    }

    private String getBoldWordsByIndex(int begin, int end, String text) {
        String word;
        try {
            if (end > begin) {
                word = text.substring(begin, end);
            } else {
                word = text.substring(begin);
            }
        }catch (Exception e) {
            return "";
        }

        int prevP;
        int lastP;

        if (text.lastIndexOf(" ", begin) != -1) {
            prevP = text.lastIndexOf(" ", begin);
        } else prevP = begin;
        if (text.indexOf(" ", end + 30) != -1) {
            lastP = text.indexOf(" ", end + 30);
        } else lastP = text.indexOf(" ", end);
        String textNew;
        if (end > begin) {
            textNew = text.substring(prevP, lastP);
        } else {
            textNew = text.substring(prevP);
        }

        textNew = textNew.replace(word, "<b>" + word + "</b>");

        return textNew;
    }

    private List<Map.Entry<PageDB, Float>> getPagesRelatRelevance(List<PageDB> pageDBList, List<IndexDB> indexDBList) {
        HashMap<PageDB, Float> pageDBAbsRelevanceMap = new HashMap<>();
        for (PageDB pageDB : pageDBList) {
            float relevant = (float) indexDBList.stream()
                    .filter(indDB -> indDB.getPage() == pageDB)
                    .mapToDouble(IndexDB::getRank)
                    .sum();
            pageDBAbsRelevanceMap.put(pageDB, relevant);
        }

        if (searchProperties.isSortByAbsoluteRelevance()) {
            return pageDBAbsRelevanceMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();
        }

        HashMap<PageDB, Float> pageDBRelRelevanceMap = new HashMap<>();
        float maxAbsRelevance = Collections.max(pageDBAbsRelevanceMap.values());
        for (PageDB pageDB : pageDBAbsRelevanceMap.keySet()) {
            float relativeRelevance = pageDBAbsRelevanceMap.get(pageDB) / maxAbsRelevance;
            pageDBRelRelevanceMap.put(pageDB, relativeRelevance);
        }
        return pageDBRelRelevanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();
    }
}
