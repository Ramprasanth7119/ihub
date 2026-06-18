package com.ihub.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.ihub.dto.CategoryFacetResponse;
import com.ihub.dto.SearchResponse;
import com.ihub.exception.CustomException;
import com.ihub.search.IdeaDocument;
import com.ihub.search.IdeaSearchRepository;
import com.ihub.search.IdeaSearchSort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IdeaSearchService {

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

        Criteria criteria = buildCriteria(keyword, category, tagList, minBudget, maxBudget, auctionStatus);
        Sort springSort = buildSort(resolvedSort, hasKeyword);

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(resolvedPage, resolvedSize, springSort));

        SearchHits<IdeaDocument> hits = elasticsearchOperations.search(query, IdeaDocument.class);

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

        SearchHits<IdeaDocument> hits = elasticsearchOperations.search(query, IdeaDocument.class);
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

    public void updateStatus(Long ideaId, String status) {
        IdeaDocument doc = repository.findById(ideaId).orElse(null);
        if (doc != null) {
            doc.setAuctionStatus(status);
            repository.save(doc);
        }
    }

    public void indexIdea(IdeaDocument doc) {
        repository.save(doc);
    }

    public void removeFromIndex(Long ideaId) {
        repository.deleteById(ideaId);
    }

    private Criteria buildCriteria(
            String keyword,
            String category,
            List<String> tags,
            Double minBudget,
            Double maxBudget,
            String auctionStatus) {

        Criteria criteria = Criteria.where("ideaStatus").is("PUBLISHED");

        if (keyword != null && !keyword.isBlank()) {
            Criteria textMatch = Criteria.where("title").contains(keyword)
                    .or(Criteria.where("description").contains(keyword))
                    .or(Criteria.where("category").is(keyword.toLowerCase()));

            for (String token : keyword.toLowerCase().split("\\s+")) {
                if (!token.isBlank()) {
                    textMatch = textMatch.or(Criteria.where("tags").is(token));
                }
            }
            criteria = criteria.and(textMatch);
        }

        if (category != null && !category.isBlank()) {
            criteria = criteria.and(Criteria.where("category").is(category.toLowerCase()));
        }

        for (String tag : tags) {
            criteria = criteria.and(Criteria.where("tags").is(tag));
        }

        if (minBudget != null) {
            criteria = criteria.and(Criteria.where("maxBudget").greaterThanEqual(minBudget));
        }

        if (maxBudget != null) {
            criteria = criteria.and(Criteria.where("minBudget").lessThanEqual(maxBudget));
        }

        if (auctionStatus != null && !auctionStatus.isBlank()) {
            criteria = criteria.and(Criteria.where("auctionStatus").is(auctionStatus));
        }

        return criteria;
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
