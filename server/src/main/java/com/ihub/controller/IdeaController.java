package com.ihub.controller;

import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaResponse;
import com.ihub.service.IdeaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Idea APIs
 */
@RestController
@RequestMapping("/api/ideas")
@RequiredArgsConstructor
public class IdeaController {

    private final IdeaService ideaService;
    
    

    public IdeaController(IdeaService ideaService) {
		super();
		this.ideaService = ideaService;
	}

	/**
     * Create idea
     */
    @PostMapping
    public IdeaResponse createIdea(@RequestBody IdeaRequest request) {
        return ideaService.createIdea(request);
    }

    /**
     * Get idea by ID
     */
    @GetMapping("/{id}")
    public IdeaResponse getIdea(@PathVariable Long id) {
        return ideaService.getIdea(id);
    }

    /**
     * Get all ideas
     */
    @GetMapping
    public List<IdeaResponse> getAllIdeas() {
        return ideaService.getAllIdeas();
    }
}