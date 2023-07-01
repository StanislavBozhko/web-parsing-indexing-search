package searchengine.dto.indexing;

import searchengine.model.LemmaDB;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IndexParser {

    void run(SiteDB siteDB, HashMap<String, LemmaDB> lemmaDBmap, Map<PageDB, Map<String, Integer>> indexMap);
    List<IndexDto> getIndexList();

}
