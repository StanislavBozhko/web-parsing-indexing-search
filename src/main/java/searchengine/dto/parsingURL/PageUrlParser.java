package searchengine.dto.parsingURL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import searchengine.config.ConnectSettings;
import searchengine.model.SiteStatusEnum;
import searchengine.services.IndexingServiceImpl;

import java.util.*;
import java.util.concurrent.RecursiveTask;


public class PageUrlParser extends RecursiveTask<List<PageDto>> {

    private final String url;
    private final Set<String> urlList;
    private final Vector<PageDto> pageDtoList;
    private final String mainUrl;
    private final boolean onlyPage;
    private final SiteIndexing siteIndexing;
    private final ConnectSettings connectSettings;
    private static final String KEY_HREF = "href";
    private static final String CSSQUERY_HREF = "a[href]";

    public PageUrlParser(String url, Set<String> urlList, Vector<PageDto> pageDtoList, String mainUrl, boolean onlyPage, SiteIndexing siteIndexing, ConnectSettings connectSettings) {
        this.url = url;
        this.urlList = urlList;
        this.pageDtoList = pageDtoList;
        this.mainUrl = mainUrl;
        this.onlyPage = onlyPage;
        this.siteIndexing = siteIndexing;
        this.connectSettings = connectSettings;
    }

    @Override
    protected Vector<PageDto> compute() {
        if (IndexingServiceImpl.isStopped) {
            return null;
        }
        try {
            Connection.Response response = Jsoup.connect(url).userAgent(connectSettings.getUserAgent())
                    .referrer(connectSettings.getReferer()).timeout(10000)
                    .ignoreContentType(true).ignoreHttpErrors(true).followRedirects(false)
                    .execute();
            Document doc = response.parse();
            String html = doc.outerHtml();
            int status = response.statusCode();
            PageDto pageDto = new PageDto(url, html, status);
            pageDtoList.add(pageDto);
            if (pageDtoList.size() % 100 == 0) {
                savePartPagesToBase();
              }
            if (!onlyPage) {
                Elements elements = doc.select(CSSQUERY_HREF);
                List<PageUrlParser> tasks = new ArrayList<>();
                for (Element element : elements) {
                    String attributeUrl = element.absUrl(KEY_HREF).toLowerCase(Locale.ROOT);
                    if (attributeUrl.isEmpty()) {
                        continue;
                    }
                    if (checkAttributeUrl(attributeUrl)) {
                        urlList.add(attributeUrl);
                        PageUrlParser task = new PageUrlParser(attributeUrl, urlList, pageDtoList, mainUrl, onlyPage, siteIndexing, connectSettings);
                        task.fork();
                        tasks.add(task);
                    }
                }
                for (PageUrlParser task : tasks) {
                    task.join();
                }
            }
        } catch (Exception e) {
            //          e.printStackTrace();
            PageDto pageDto = new PageDto(url, "", 500);
            pageDtoList.add(pageDto);
        }
        try {
            Thread.sleep(125);
        } catch (InterruptedException e) {
            //       e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return pageDtoList;
    }

    private void savePartPagesToBase() {
        //      System.out.println(url + " - pageDtoList size = " + pageDtoList.size() + " - сохраняем в базу");
        List<PageDto> tempPageDtoList = pageDtoList.stream().toList();
        if (pageDtoList.size() == tempPageDtoList.size()) {
            pageDtoList.clear();
        } else {
            pageDtoList.removeAll(tempPageDtoList);
        }
        try {
            siteIndexing.saveToBase(tempPageDtoList, null, SiteStatusEnum.INDEXING);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkAttributeUrl(String attributeUrl) {
        return attributeUrl.startsWith(mainUrl) && !attributeUrl.contains("#")
                && !attributeUrl.contains(".pdf") && !attributeUrl.contains(".jpg")
                && !attributeUrl.contains(".png") && !attributeUrl.contains("?") && !attributeUrl.contains(".jpeg")
                && !attributeUrl.contains(".webp") && !attributeUrl.contains(".doc") && !attributeUrl.contains(".docx")
                && !attributeUrl.contains(".xls") && !attributeUrl.contains(".xlsx") && !urlList.contains(attributeUrl);
    }

    public String getUrl() {
        return url;
    }
}
