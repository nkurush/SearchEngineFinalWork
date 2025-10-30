package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.responses.OkResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.ApiService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final SearchService searchService;
    private final StatisticsService statisticsService;
    private final ApiService apiService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() throws MalformedURLException {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public OkResponse startIndexing() {
        apiService.startIndexing();
        return new OkResponse();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public OkResponse stopIndexing() {
        apiService.stopIndexing();
        return new OkResponse();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.OK)
    public OkResponse indexPage(@RequestParam String url) throws IOException {
        apiService.indexPage(url);
        return new OkResponse();
    }

    @GetMapping("/search")
    public Object search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) throws IOException {
        return searchService.search(query, site, offset, limit);
    }
}