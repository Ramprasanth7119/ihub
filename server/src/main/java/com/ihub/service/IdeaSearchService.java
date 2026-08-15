package com.ihub.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.ihub.dto.CategoryFacetResponse;
import com.ihub.dto.SearchResponse;
import com.ihub.exception.CustomException;
import com.ihub.search.IdeaDocument;
import com.ihub.search.IdeaSearchRepository;
import com.ihub.search.IdeaSearchSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class IdeaSearchService {

    private static final Logger log = LoggerFactory.getLogger(IdeaSearchService.class);

    private final IdeaSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final int defaultPageSize;
    private final int maxPageSize;

    public IdeaSearchService(
            IdeaSearchRepository repository,
            ElasticsearchOperations elasticsearchOperations,
            @Value("${search.default-page-size:20}") int defaultPageSize,
            @Value("${search.max-page-size:100}") int maxPageSize) {
        this.repository = repository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    public SearchResponse search(
            String keyword,
            String category,
            String tags,
            Double minBudget,
            Double maxBudget,
            String auctionStatus,
            String sort,
            Integer page,
            Integer size) {

        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        IdeaSearchSort resolvedSort = IdeaSearchSort.fromParam(sort);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        List<String> tagList = parseTags(tags);

        Sort springSort = buildSort(resolvedSort, hasKeyword);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> buildBoolQuery(
                        b, keyword, hasKeyword, category, tagList, minBudget, maxBudget, auctionStatus)))
                .withPageable(PageRequest.of(resolvedPage, resolvedSize, springSort))
                .build();

        SearchHits<IdeaDocument> hits = execute(() -> elasticsearchOperations.search(query, IdeaDocument.class));

        List<IdeaDocument> content = hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        long totalElements = hits.getTotalHits();
        int totalPages = resolvedSize == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);

        return new SearchResponse(
                content,
                totalElements,
                totalPages,
                resolvedPage,
                resolvedSize,
                resolvedSort.name()
        );
    }

    public SearchResponse filterByCategory(String category, Integer page, Integer size) {
        return search(null, category, null, null, null, null, null, page, size);
    }

    public SearchResponse getLiveIdeas(Integer page, Integer size) {
        return search(null, null, null, null, null, "ACTIVE", null, page, size);
    }

    public List<CategoryFacetResponse> getCategoryFacets() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("ideaStatus").value("PUBLISHED")))
                .withAggregation("by_category", Aggregation.of(a -> a
                        .terms(t -> t.field("category").size(50))))
                .withMaxResults(0)
                .build();

        SearchHits<IdeaDocument> hits = execute(() -> elasticsearchOperations.search(query, IdeaDocument.class));
        if (hits.getAggregations() == null) {
            return List.of();
        }

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();
        StringTermsAggregate terms = aggregations.get("by_category")
                .aggregation()
                .getAggregate()
                .sterms();

        List<CategoryFacetResponse> facets = new ArrayList<>();
        for (StringTermsBucket bucket : terms.buckets().array()) {
            facets.add(new CategoryFacetResponse(bucket.key().stringValue(), bucket.docCount()));
        }
        return facets;
    }

    /**
     * Mirrors an auction status change onto the idea's search document.
     *
     * <p>MySQL is the system of record; the index is a derived read model. An
     * Elasticsearch outage must therefore never abort the surrounding database
     * transaction, so indexing failures are logged and swallowed. Drift is repaired
     * by {@link #reindexAll(List)}.</p>
     */
    public void updateStatus(Long ideaId, String status) {
        runQuietly("update auction status for idea " + ideaId, () -> {
            IdeaDocument doc = repository.findById(ideaId).orElse(null);
            if (doc != null) {
                doc.setAuctionStatus(status);
                repository.save(doc);
            }
        });
    }

    /** Best-effort index write. See {@link #updateStatus} for the failure contract. */
    public void indexIdea(IdeaDocument doc) {
        runQuietly("index idea " + doc.getId(), () -> repository.save(doc));
    }

    /** Best-effort index delete. See {@link #updateStatus} for the failure contract. */
    public void removeFromIndex(Long ideaId) {
        runQuietly("remove idea " + ideaId + " from index", () -> repository.deleteById(ideaId));
    }

    /**
     * Rebuilds the whole index from the supplied documents (sourced from MySQL) and
     * drops anything that no longer belongs. Unlike the incremental writes above this
     * is invoked explicitly by an administrator, so failures propagate.
     *
     * @return the number of documents written
     */
    public int reindexAll(List<IdeaDocument> documents) {
        Set<Long> liveIds = documents.stream()
                .map(IdeaDocument::getId)
                .collect(Collectors.toSet());

        repository.findAll().forEach(existing -> {
            if (!liveIds.contains(existing.getId())) {
                repository.deleteById(existing.getId());
            }
        });

        repository.saveAll(documents);
        log.info("Elasticsearch reindex complete: {} documents indexed", documents.size());
        return documents.size();
    }

    /** True when Elasticsearch is reachable — used by the admin search-health endpoint. */
    public boolean isSearchAvailable() {
        try {
            elasticsearchOperations.indexOps(IdeaDocument.class).exists();
            return true;
        } catch (RuntimeException e) {
            log.warn("Elasticsearch health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Runs a read against Elasticsearch, converting connectivity failures into a
     * 503 so clients can distinguish "search is down" from "your query was wrong".
     * A {@link CustomException} raised by validation upstream passes through untouched.
     */
    private <T> T execute(Supplier<T> query) {
        try {
            return query.get();
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Elasticsearch query failed", e);
            throw new CustomException(
                    "Search is temporarily unavailable. Please try again shortly.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void runQuietly(String description, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Search index operation failed and was skipped ({}). "
                    + "MySQL remains authoritative; run an admin reindex to reconcile.", description, e);
        }
    }

    /**
     * Assembles the search as an explicit Elasticsearch bool query.
     *
     * <p>This replaces a {@code Criteria} chain that had two defects. First,
     * {@code Criteria.contains()} compiles to a wildcard, and Spring Data rejects a
     * wildcard containing whitespace — so every multi-word query threw and returned
     * a 500. Second, {@code Criteria} flattens mixed AND/OR into a single level, so
     * once an OR group was combined with the status filter the filters stopped
     * constraining anything and every query matched the whole index.</p>
     *
     * <p>Structural filters go in {@code filter} (no scoring, cacheable); the
     * free-text part goes in {@code must} so results are ranked by relevance.</p>
     */
    private BoolQuery.Builder buildBoolQuery(
            BoolQuery.Builder bool,
            String keyword,
            boolean hasKeyword,
            String category,
            List<String> tags,
            Double minBudget,
            Double maxBudget,
            String auctionStatus) {

        // Only published ideas are ever discoverable.
        bool.filter(f -> f.term(t -> t.field("ideaStatus").value("PUBLISHED")));

        if (hasKeyword) {
            String trimmed = keyword.trim();
            bool.must(m -> m.bool(inner -> {
                // Analysed match across the text fields, title weighted highest.
                inner.should(s -> s.multiMatch(mm -> mm
                        .query(trimmed)
                        .fields("title^3", "description", "tags^2", "category")
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")));

                // Per-token prefix matching so partial words still find results.
                // Tokens are passed individually — a wildcard cannot contain spaces.
                for (String rawToken : trimmed.toLowerCase().split("\\s+")) {
                    String token = rawToken.trim();
                    if (!token.isEmpty()) {
                        inner.should(s -> s.prefix(p -> p.field("title").value(token)));
                        inner.should(s -> s.term(t -> t.field("tags").value(token)));
                        inner.should(s -> s.term(t -> t.field("category").value(token)));
                    }
                }

                // At least one of the above has to match, otherwise a `should`-only
                // bool matches every document.
                return inner.minimumShouldMatch("1");
            }));
        }

        if (category != null && !category.isBlank()) {
            String normalised = category.trim().toLowerCase();
            bool.filter(f -> f.term(t -> t.field("category").value(normalised)));
        }

        // Multiple tags are ANDed: an idea must carry all of them.
        for (String tag : tags) {
            bool.filter(f -> f.term(t -> t.field("tags").value(tag)));
        }

        if (minBudget != null) {
            bool.filter(f -> f.range(r -> r.field("maxBudget").gte(JsonData.of(minBudget))));
        }

        if (maxBudget != null) {
            bool.filter(f -> f.range(r -> r.field("minBudget").lte(JsonData.of(maxBudget))));
        }

        if (auctionStatus != null && !auctionStatus.isBlank()) {
            String normalised = auctionStatus.trim().toUpperCase();
            bool.filter(f -> f.term(t -> t.field("auctionStatus").value(normalised)));
        }

        return bool;
    }

    private Sort buildSort(IdeaSearchSort sort, boolean hasKeyword) {
        if (sort == IdeaSearchSort.RELEVANCE) {
            return hasKeyword ? Sort.unsorted() : Sort.by(Sort.Direction.DESC, "id");
        }

        return switch (sort) {
            case MIN_BUDGET_ASC -> Sort.by(Sort.Direction.ASC, "minBudget");
            case MIN_BUDGET_DESC -> Sort.by(Sort.Direction.DESC, "minBudget");
            case MAX_BUDGET_ASC -> Sort.by(Sort.Direction.ASC, "maxBudget");
            case MAX_BUDGET_DESC -> Sort.by(Sort.Direction.DESC, "maxBudget");
            case TITLE_ASC -> Sort.by(Sort.Direction.ASC, "title.keyword");
            case TITLE_DESC -> Sort.by(Sort.Direction.DESC, "title.keyword");
            default -> Sort.unsorted();
        };
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size <= 0) {
            return defaultPageSize;
        }
        if (size > maxPageSize) {
            throw new CustomException("Page size cannot exceed " + maxPageSize);
        }
        return size;
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
