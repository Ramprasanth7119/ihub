package com.ihub.controller;

import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaResponse;
import com.ihub.dto.IdeaUpdateRequest;
import com.ihub.service.IdeaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @PostMapping
    public IdeaResponse createIdea(@Valid @RequestBody IdeaRequest request) {
        return ideaService.createIdea(request);
    }

    @GetMapping("/{id}")
    public IdeaResponse getIdea(@PathVariable Long id) {
        return ideaService.getIdea(id);
    }

    @GetMapping
    public List<IdeaResponse> getIdeas(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false, defaultValue = "false") Boolean mine) {
        return ideaService.getIdeas(status, category, minBudget, maxBudget, mine);
    }

    @PutMapping("/{id}")
    public IdeaResponse updateIdea(@PathVariable Long id, @Valid @RequestBody IdeaUpdateRequest request) {
        return ideaService.updateIdea(id, request);
    }

    @PostMapping("/{id}/publish")
    public IdeaResponse publishIdea(@PathVariable Long id) {
        return ideaService.publishIdea(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdea(@PathVariable Long id) {
        ideaService.deleteIdea(id);
    }
}
