package searchengine.services;

import searchengine.dto.search.SearchResponse;


public interface SearchService {
    SearchResponse allSiteSearch(String text, int offset, int limit);
    SearchResponse siteSearch(String searchText, String url, int offset, int limit);
}
