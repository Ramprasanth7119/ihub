package com.ihub.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Auction entity model
 */
@Data
public class Auction {

    private Long id;
    private Long ideaId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
	public Auction(Long id, Long ideaId, LocalDateTime startTime, LocalDateTime endTime, String status) {
		super();
		this.id = id;
		this.ideaId = ideaId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = status;
	}
	public Auction() {
		super();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
    
    
}