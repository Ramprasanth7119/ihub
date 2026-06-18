package com.ihub.controller;

import com.ihub.dto.CategoryFacetResponse;
import com.ihub.dto.SearchResponse;
import com.ihub.service.IdeaSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class IdeaSearchController {

    private final IdeaSearchService service;

    public IdeaSearchController(IdeaSearchService service) {
        this.service = service;
    }

    /**
     * Unified search with multi-field matching, filters, sorting, and pagination.
     */
    @GetMapping
    public SearchResponse search(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) String auctionStatus,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {

        String resolvedKeyword = (q != null && !q.isBlank()) ? q : keyword;

        return service.search(
                resolvedKeyword, category, tags, minBudget, maxBudget,
                auctionStatus, sort, page, size
        );
    }

    @GetMapping("/category")
    public SearchResponse byCategory(
            @RequestParam String category,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return service.filterByCategory(category, page, size);
    }

    @GetMapping("/live")
    public SearchResponse liveIdeas(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return service.getLiveIdeas(page, size);
    }

    @GetMapping("/facets/categories")
    public List<CategoryFacetResponse> categoryFacets() {
        return service.getCategoryFacets();
    }
}
