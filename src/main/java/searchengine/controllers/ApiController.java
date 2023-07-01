package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ResponseBoolean;
import searchengine.dto.ResponseWithMsg;
import searchengine.dto.search.SearchDto;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteDB;
import searchengine.model.SiteStatusEnum;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        ResponseBoolean responce = indexingService.startIndexing();
        if (responce.isResult()) {
            return new ResponseEntity<>(responce, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(responce, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new ResponseBoolean(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ResponseWithMsg(false, "Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new ResponseWithMsg(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.pageIndexing(url)) {
                // добавлена на переиндексацию
                return new ResponseEntity<>(new ResponseBoolean(true), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ResponseWithMsg(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
            }
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (query.isEmpty()) {
            return new ResponseEntity<>(new ResponseWithMsg(false, "Задан пустой поисковый запрос"), HttpStatus.BAD_REQUEST);
        } else {
            SearchResponse searchResponse;
            if (!site.isEmpty()) {
                SiteDB siteDB = siteRepository.findByUrl(site);
                if (siteDB == null) {
                    return new ResponseEntity<>(new ResponseWithMsg(false, "Указанная страница не найдена"), HttpStatus.BAD_REQUEST);
                } else if (!siteDB.getStatus().equals(SiteStatusEnum.INDEXED)) {
                    return new ResponseEntity<>(new ResponseWithMsg(false, "Указанный сайт не проиндексирован"), HttpStatus.BAD_REQUEST);
                } else {
                   searchResponse = searchService.siteSearch(query, site, offset, limit);
                }
            } else {
                searchResponse = searchService.allSiteSearch(query, offset, limit);
            }

            return new ResponseEntity<>(searchResponse, HttpStatus.OK);
        }
    }


}
