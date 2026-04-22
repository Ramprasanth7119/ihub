package com.ihub.service;

import com.ihub.search.IdeaDocument;
import com.ihub.search.IdeaSearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdeaSearchService {

    private final IdeaSearchRepository repository;

    public IdeaSearchService(IdeaSearchRepository repository) {
		super();
		this.repository = repository;
	}

	public List<IdeaDocument> search(String keyword) {
        return repository.findByTitleContainingOrDescriptionContaining(keyword, keyword);
    }

    public List<IdeaDocument> filterByCategory(String category) {
        return repository.findByCategory(category);
    }
    
    public void updateStatus(Long ideaId, String status) {

        IdeaDocument doc = repository.findById(ideaId).orElse(null);

        if (doc != null) {
            doc.setAuctionStatus(status);
            repository.save(doc);
        }
    }

    public List<IdeaDocument> getLiveIdeas() {
        return repository.findByAuctionStatus("ACTIVE");
    }

    public void indexIdea(IdeaDocument doc) {
        repository.save(doc);
    }
}