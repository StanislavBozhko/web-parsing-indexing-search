package searchengine.dto.indexing;

import lombok.Value;
import searchengine.model.LemmaDB;
import searchengine.model.PageDB;

@Value
public class IndexDto {
    PageDB page;
    LemmaDB lemma;
    float rank;
}
