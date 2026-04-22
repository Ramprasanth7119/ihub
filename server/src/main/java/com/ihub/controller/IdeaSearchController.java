package com.ihub.controller;

import com.ihub.search.IdeaDocument;
import com.ihub.service.IdeaSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class IdeaSearchController {

    private final IdeaSearchService service;
    
    

    public IdeaSearchController(IdeaSearchService service) {
		super();
		this.service = service;
	}

	@GetMapping
    public List<IdeaDocument> search(@RequestParam String keyword) {
        return service.search(keyword);
    }

    @GetMapping("/category")
    public List<IdeaDocument> byCategory(@RequestParam String category) {
        return service.filterByCategory(category);
    }

    @GetMapping("/live")
    public List<IdeaDocument> liveIdeas() {
        return service.getLiveIdeas();
    }
}