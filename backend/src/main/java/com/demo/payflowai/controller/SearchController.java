package com.demo.payflowai.controller;

import com.demo.payflowai.service.SemanticSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SemanticSearchService searchService;

    public SearchController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<SemanticSearchService.SearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int topK) {
        return searchService.search(q, topK);
    }
}
