package com.ihub.dto;

import com.ihub.search.IdeaDocument;

import java.util.List;

public class SearchResponse {

    private List<IdeaDocument> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private String sort;

    public SearchResponse() {
    }

    public SearchResponse(List<IdeaDocument> content, long totalElements, int totalPages, int page, int size, String sort) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public List<IdeaDocument> getContent() {
        return content;
    }

    public void setContent(List<IdeaDocument> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
