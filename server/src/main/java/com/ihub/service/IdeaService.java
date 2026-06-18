package com.ihub.service;

import com.ihub.dao.IdeaDao;
import com.ihub.dao.TagDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaResponse;
import com.ihub.dto.IdeaUpdateRequest;
import com.ihub.exception.CustomException;
import com.ihub.model.Idea;
import com.ihub.model.User;
import com.ihub.search.IdeaDocument;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IdeaService {

    private final IdeaDao ideaDao;
    private final TagDao tagDao;
    private final UserDao userDao;
    private final CategoryService categoryService;
    private final IdeaSearchService ideaSearchService;

    public IdeaService(
            IdeaDao ideaDao,
            TagDao tagDao,
            UserDao userDao,
            CategoryService categoryService,
            IdeaSearchService ideaSearchService) {
        this.ideaDao = ideaDao;
        this.tagDao = tagDao;
        this.userDao = userDao;
        this.categoryService = categoryService;
        this.ideaSearchService = ideaSearchService;
    }

    @Transactional
    public IdeaResponse createIdea(IdeaRequest request) {
        User creator = getAuthenticatedCreator();
        validateBudgetRange(request.getBasePrice(), request.getMaxBudget());
        categoryService.validateCategorySlug(request.getCategory());

        if (request.getCreatorId() != null && !request.getCreatorId().equals(creator.getId())) {
            throw new CustomException("Creator ID does not match authenticated user");
        }

        Long id = ideaDao.createIdea(request, creator.getId());
        tagDao.replaceTagsForIdea(id, request.getTags());

        return mapToResponse(ideaDao.getIdeaById(id));
    }

    public IdeaResponse getIdea(Long id) {
        Idea idea = findIdeaOrThrow(id);
        assertCanView(idea);
        return mapToResponse(idea);
    }

    public List<IdeaResponse> getIdeas(String status, String category, Double minBudget, Double maxBudget, Boolean mine) {
        Long creatorFilter = null;
        String statusFilter;

        if (Boolean.TRUE.equals(mine)) {
            creatorFilter = getAuthenticatedCreator().getId();
            statusFilter = (status != null && !status.isBlank()) ? status : null;
        } else {
            statusFilter = "PUBLISHED";
        }

        return ideaDao.findIdeas(statusFilter, category, minBudget, maxBudget, creatorFilter)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IdeaResponse updateIdea(Long id, IdeaUpdateRequest request) {
        Idea existing = findIdeaOrThrow(id);
        assertCreatorOwns(existing);

        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw new CustomException("Only draft ideas can be updated");
        }

        if (request.getCategory() != null) {
            categoryService.validateCategorySlug(request.getCategory());
        }

        Double basePrice = request.getBasePrice() != null ? request.getBasePrice() : existing.getBasePrice();
        Double maxBudget = request.getMaxBudget() != null ? request.getMaxBudget() : existing.getMaxBudget();
        validateBudgetRange(basePrice, maxBudget);

        try {
            ideaDao.updateIdea(id, request);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found or not in draft status");
        }

        if (request.getTags() != null) {
            tagDao.replaceTagsForIdea(id, request.getTags());
        }

        return mapToResponse(ideaDao.getIdeaById(id));
    }

    @Transactional
    public IdeaResponse publishIdea(Long id) {
        Idea existing = findIdeaOrThrow(id);
        assertCreatorOwns(existing);

        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw new CustomException("Only draft ideas can be published");
        }

        try {
            ideaDao.publishIdea(id);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found or not in draft status");
        }

        Idea published = ideaDao.getIdeaById(id);
        List<String> tags = tagDao.findTagNamesByIdeaId(id);
        ideaSearchService.indexIdea(toDocument(published, tags));

        return mapToResponse(published);
    }

    @Transactional
    public void deleteIdea(Long id) {
        Idea existing = findIdeaOrThrow(id);
        assertCreatorOwns(existing);

        try {
            ideaDao.archiveIdea(id);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found");
        }

        ideaSearchService.removeFromIndex(id);
    }

    private Idea findIdeaOrThrow(Long id) {
        try {
            return ideaDao.getIdeaById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found");
        }
    }

    private void assertCanView(Idea idea) {
        if ("PUBLISHED".equalsIgnoreCase(idea.getStatus())) {
            return;
        }
        if ("ARCHIVED".equalsIgnoreCase(idea.getStatus())) {
            throw new CustomException("Idea not found");
        }
        User current = getAuthenticatedCreator();
        if (!idea.getCreatorId().equals(current.getId())) {
            throw new CustomException("Access denied");
        }
    }

    private void assertCreatorOwns(Idea idea) {
        User creator = getAuthenticatedCreator();
        if (!idea.getCreatorId().equals(creator.getId())) {
            throw new CustomException("You can only modify your own ideas");
        }
    }

    private User getAuthenticatedCreator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new CustomException("User not found");
        }
        if (!"CREATOR".equalsIgnoreCase(user.getRole())) {
            throw new CustomException("Only creators can manage ideas");
        }
        return user;
    }

    private void validateBudgetRange(Double basePrice, Double maxBudget) {
        if (maxBudget != null && maxBudget < basePrice) {
            throw new CustomException("Max budget must be greater than or equal to base price");
        }
    }

    private IdeaResponse mapToResponse(Idea idea) {
        return new IdeaResponse(
                idea.getId(),
                idea.getCreatorId(),
                idea.getTitle(),
                idea.getDescription(),
                idea.getCategory(),
                idea.getBasePrice(),
                idea.getMaxBudget(),
                idea.getStatus(),
                tagDao.findTagNamesByIdeaId(idea.getId())
        );
    }

    private IdeaDocument toDocument(Idea idea, List<String> tags) {
        IdeaDocument doc = new IdeaDocument();
        doc.setId(idea.getId());
        doc.setTitle(idea.getTitle());
        doc.setDescription(idea.getDescription());
        doc.setCategory(idea.getCategory());
        doc.setMinBudget(idea.getBasePrice());
        doc.setMaxBudget(idea.getMaxBudget());
        doc.setIdeaStatus(idea.getStatus());
        doc.setAuctionStatus("NONE");
        doc.setTags(tags);
        return doc;
    }
}
