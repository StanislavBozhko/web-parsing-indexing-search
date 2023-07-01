package searchengine.dto.search;

import lombok.Value;

@Value
public class SearchDto {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
}
