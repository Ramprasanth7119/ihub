package com.ihub.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for creating auction
 */
@Data
public class AuctionRequest {

    private Long ideaId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
	public AuctionRequest(Long ideaId, LocalDateTime startTime, LocalDateTime endTime) {
		super();
		this.ideaId = ideaId;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	public AuctionRequest() {
		super();
	}
	public Long getIdeaId() {
		return ideaId;
	}
	public void setIdeaId(Long ideaId) {
		this.ideaId = ideaId;
	}
	public LocalDateTime getStartTime() {
		return startTime;
	}
	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}
	public LocalDateTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}
    
    
}