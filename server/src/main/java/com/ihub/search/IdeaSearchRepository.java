package com.ihub.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface IdeaSearchRepository extends ElasticsearchRepository<IdeaDocument, Long> {

    List<IdeaDocument> findByTitleContainingOrDescriptionContaining(String title, String desc);

    List<IdeaDocument> findByCategory(String category);

    List<IdeaDocument> findByAuctionStatus(String status);
}