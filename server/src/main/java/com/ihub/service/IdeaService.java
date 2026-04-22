package com.ihub.service;

import com.ihub.dao.IdeaDao;
import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.Idea;
import com.ihub.search.IdeaDocument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Idea logic
 * Handles:
 * - Validation
 * - DB operations
 * - Elasticsearch indexing
 */
@Service
@RequiredArgsConstructor
public class IdeaService {

    private final IdeaDao ideaDao;
    private final IdeaSearchService ideaSearchService;

    public IdeaService(IdeaDao ideaDao, IdeaSearchService ideaSearchService) {
		super();
		this.ideaDao = ideaDao;
		this.ideaSearchService = ideaSearchService;
	}

	/**
     * Create idea and index into Elasticsearch
     */
    public IdeaResponse createIdea(IdeaRequest request) {

        // ✅ Step 1: Validate creator
        if (!ideaDao.existsUser(request.getCreatorId())) {
            throw new CustomException("Creator not found");
        }

        // ✅ Step 2: Save in MySQL
        Long id = ideaDao.createIdea(request);

        // ✅ Step 3: Fetch saved idea (important for consistency)
        Idea idea = ideaDao.getIdeaById(id);

        // ✅ Step 4: Index into Elasticsearch
        ideaSearchService.indexIdea(
                new IdeaDocument(
                        idea.getId(),
                        idea.getTitle(),
                        idea.getDescription(),
                        idea.getCategory(),
                        idea.getBasePrice(),   // minBudget
                        idea.getBasePrice(),   // maxBudget (same for now)
                        "SCHEDULED"            // initial auction state
                )
        );

        // ✅ Step 5: Return response
        return mapToResponse(idea);
    }

    /**
     * Get idea by ID
     */
    public IdeaResponse getIdea(Long id) {

        Idea idea = ideaDao.getIdeaById(id);

        return mapToResponse(idea);
    }

    /**
     * Get all ideas
     */
    public List<IdeaResponse> getAllIdeas() {

        return ideaDao.getAllIdeas()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Common mapper
     */
    private IdeaResponse mapToResponse(Idea idea) {
        return new IdeaResponse(
                idea.getId(),
                idea.getCreatorId(),
                idea.getTitle(),
                idea.getDescription(),
                idea.getCategory(),
                idea.getBasePrice(),
                idea.getStatus()
        );
    }
}