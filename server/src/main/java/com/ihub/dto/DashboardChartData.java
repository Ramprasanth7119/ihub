package com.ihub.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardChartData {
    private List<CategoryData> ideasByCategory;
    private List<StatusData> auctionStatusDistribution;
    private List<MonthlyData> monthlyAuctions;
    private List<TopInvestorData> topInvestors;
    
    @Data
    public static class CategoryData {
        private String category;
        private Long count;
    }
    
    @Data
    public static class StatusData {
        private String status;
        private Long count;
    }
    
    @Data
    public static class MonthlyData {
        private String month;
        private Long count;
    }
    
    @Data
    public static class TopInvestorData {
        private Long investorId;
        private String investorName;
        private Long bidCount;
        private Double totalInvestment;
    }
}
