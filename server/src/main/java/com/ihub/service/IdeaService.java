package com.ihub.service;

import com.ihub.dao.IdeaDao;
import com.ihub.dao.TagDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaResponse;
import com.ihub.dto.IdeaUpdateRequest;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.Idea;
import com.ihub.model.PagedResult;
import com.ihub.model.User;
import com.ihub.search.IdeaDocument;
import com.ihub.util.Pagination;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

        // The body may carry a creatorId; it is never trusted as the owner, only
        // rejected when it disagrees with the authenticated principal.
        if (request.getCreatorId() != null && !request.getCreatorId().equals(creator.getId())) {
            throw new ForbiddenException("Creator ID does not match authenticated user");
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

    public PagedResult<IdeaResponse> getIdeas(
            String status, String category, Double minBudget, Double maxBudget,
            Boolean mine, Integer page, Integer size) {

        Long creatorFilter = null;
        String statusFilter;

        if (Boolean.TRUE.equals(mine)) {
            creatorFilter = getAuthenticatedCreator().getId();
            statusFilter = (status != null && !status.isBlank()) ? status : null;
        } else {
            // Public discovery only ever exposes published ideas.
            statusFilter = "PUBLISHED";
        }

        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size);

        List<Idea> ideas = ideaDao.findIdeas(
                statusFilter, category, minBudget, maxBudget, creatorFilter,
                resolvedSize, Pagination.offset(resolvedPage, resolvedSize));
        long total = ideaDao.countIdeas(statusFilter, category, minBudget, maxBudget, creatorFilter);

        return new PagedResult<>(mapToResponses(ideas), total, resolvedPage, resolvedSize);
    }

    @Transactional
    public IdeaResponse updateIdea(Long id, IdeaUpdateRequest request) {
        Idea existing = findIdeaOrThrow(id);
        assertCreatorOwns(existing);

        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw new ConflictException("Only draft ideas can be updated");
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
            throw new ConflictException("Idea is no longer in draft status");
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
            throw new ConflictException("Only draft ideas can be published");
        }

        try {
            ideaDao.publishIdea(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ConflictException("Idea is no longer in draft status");
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
            throw new ConflictException("Idea is already archived");
        }

        ideaSearchService.removeFromIndex(id);
    }

    private Idea findIdeaOrThrow(Long id) {
        try {
            return ideaDao.getIdeaById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Idea not found");
        }
    }

    /**
     * Published ideas are public. Anything else is visible only to its own creator
     * (and to admins, who moderate the catalogue). Archived ideas are reported as
     * missing rather than forbidden so their existence is not disclosed.
     */
    private void assertCanView(Idea idea) {
        if ("PUBLISHED".equalsIgnoreCase(idea.getStatus())) {
            return;
        }
        if ("ARCHIVED".equalsIgnoreCase(idea.getStatus())) {
            throw new NotFoundException("Idea not found");
        }

        User current = getAuthenticatedUser();
        if ("ADMIN".equalsIgnoreCase(current.getRole())) {
            return;
        }
        if (!idea.getCreatorId().equals(current.getId())) {
            throw new NotFoundException("Idea not found");
        }
    }

    private void assertCreatorOwns(Idea idea) {
        User creator = getAuthenticatedCreator();
        if (!idea.getCreatorId().equals(creator.getId())) {
            throw new ForbiddenException("You can only modify your own ideas");
        }
    }

    private User getAuthenticatedCreator() {
        User user = getAuthenticatedUser();
        if (!"CREATOR".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Only creators can manage ideas");
        }
        return user;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new UnauthorizedException("Authenticated user no longer exists");
        }
        return user;
    }

    private void validateBudgetRange(Double basePrice, Double maxBudget) {
        if (basePrice == null || maxBudget == null) {
            return;
        }
        if (maxBudget < basePrice) {
            throw new UnprocessableEntityException("Max budget must be greater than or equal to base price");
        }
    }

    private IdeaResponse mapToResponse(Idea idea) {
        return toResponse(idea, tagDao.findTagNamesByIdeaId(idea.getId()));
    }

    /** Maps a collection while loading every idea's tags in one query rather than N. */
    private List<IdeaResponse> mapToResponses(List<Idea> ideas) {
        if (ideas.isEmpty()) {
            return List.of();
        }

        List<Long> ideaIds = ideas.stream().map(Idea::getId).toList();
        Map<Long, List<String>> tagsByIdea = tagDao.findTagNamesGroupedByIdeaIds(ideaIds);

        return ideas.stream()
                .map(idea -> toResponse(idea, tagsByIdea.getOrDefault(idea.getId(), List.of())))
                .toList();
    }

    private IdeaResponse toResponse(Idea idea, List<String> tags) {
        return new IdeaResponse(
                idea.getId(),
                idea.getCreatorId(),
                idea.getTitle(),
                idea.getDescription(),
                idea.getCategory(),
                idea.getBasePrice(),
                idea.getMaxBudget(),
                idea.getStatus(),
                tags
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
