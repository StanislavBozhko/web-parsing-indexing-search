package searchengine.dto.lemmas;

import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import java.util.List;
import java.util.Map;

public interface LemmaParser {
    void run(SiteDB siteDB, PageDB pageDB);

    List<LemmaDto> getLemmaDtoList();

    Map<PageDB, Map<String, Integer>> getIndexMap();

}
