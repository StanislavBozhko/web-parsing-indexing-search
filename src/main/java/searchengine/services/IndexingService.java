package searchengine.services;

import searchengine.dto.ResponseBoolean;
import searchengine.dto.ResponseWithMsg;

public interface IndexingService {

    ResponseBoolean startIndexing();
    boolean stopIndexing();
    boolean pageIndexing(String url);
}
