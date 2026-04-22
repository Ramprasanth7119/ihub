package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dto.AuctionRequest;
import com.ihub.dto.AuctionResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.Auction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for auction
 */
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionDao auctionDao;
    
    

    public AuctionService(AuctionDao auctionDao) {
		super();
		this.auctionDao = auctionDao;
	}

	public AuctionResponse createAuction(AuctionRequest request) {

        // Validate idea exists
        if (!auctionDao.ideaExists(request.getIdeaId())) {
            throw new CustomException("Idea not found");
        }

        // Prevent duplicate auction
        if (auctionDao.auctionExistsForIdea(request.getIdeaId())) {
            throw new CustomException("Auction already exists for this idea");
        }

        Long id = auctionDao.createAuction(request);

        return new AuctionResponse(
                id,
                request.getIdeaId(),
                request.getStartTime(),
                request.getEndTime(),
                "SCHEDULED"
        );
    }

    public AuctionResponse getAuction(Long id) {

        Auction auction = auctionDao.getAuctionById(id);

        return map(auction);
    }

    public List<AuctionResponse> getAllAuctions() {

        return auctionDao.getAllAuctions()
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    private AuctionResponse map(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getIdeaId(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus()
        );
    }
}