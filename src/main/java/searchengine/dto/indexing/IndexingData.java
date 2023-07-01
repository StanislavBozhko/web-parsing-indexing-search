package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.LemmaDB;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IndexingData implements IndexParser {

    private HashMap<String, LemmaDB> lemmaDBmap;
    private ArrayList<IndexDto> indexDtoList;


    @Override
    public void run(SiteDB siteDB, HashMap<String, LemmaDB> lemmaDBmap, Map<PageDB, Map<String, Integer>> indexMap) {
        indexDtoList = new ArrayList<>();
        this.lemmaDBmap = lemmaDBmap;
        indexMap.forEach(this::saveIndexDto);
    }

    @Override
    public List<IndexDto> getIndexList() {
        return indexDtoList;
    }

    private void saveIndexDto(PageDB pageDB, Map<String, Integer> lemmasMap) {
        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            float rank = entry.getValue().floatValue();
            indexDtoList.add(new IndexDto(pageDB, lemmaDBmap.get(entry.getKey()), rank));
        }
    }


}
