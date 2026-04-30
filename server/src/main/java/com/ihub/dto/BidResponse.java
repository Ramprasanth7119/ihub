package com.ihub.dto;

import lombok.Data;

@Data
public class BidResponse {

    private String message;
    private Double currentHighest;
	public BidResponse(String message, Double currentHighest) {
		super();
		this.message = message;
		this.currentHighest = currentHighest;
	}
	public BidResponse() {
		super();
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Double getCurrentHighest() {
		return currentHighest;
	}
	public void setCurrentHighest(Double currentHighest) {
		this.currentHighest = currentHighest;
	}
    
    
}