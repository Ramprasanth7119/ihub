package com.ihub.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

@Document(indexName = "ideas")
@Setting(shards = 1, replicas = 0)
public class IdeaDocument {

    @Id
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private Double minBudget;

    @Field(type = FieldType.Double)
    private Double maxBudget;

    @Field(type = FieldType.Keyword)
    private String ideaStatus;

    @Field(type = FieldType.Keyword)
    private String auctionStatus;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    public IdeaDocument() {
    }

    public IdeaDocument(Long id, String title, String description, String category,
                        Double minBudget, Double maxBudget, String auctionStatus) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.auctionStatus = auctionStatus;
        this.ideaStatus = "PUBLISHED";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinBudget() {
        return minBudget;
    }

    public void setMinBudget(Double minBudget) {
        this.minBudget = minBudget;
    }

    public Double getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(Double maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getIdeaStatus() {
        return ideaStatus;
    }

    public void setIdeaStatus(String ideaStatus) {
        this.ideaStatus = ideaStatus;
    }

    public String getAuctionStatus() {
        return auctionStatus;
    }

    public void setAuctionStatus(String auctionStatus) {
        this.auctionStatus = auctionStatus;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
