package com.ihub.search;

public enum IdeaSearchSort {

    RELEVANCE,
    MIN_BUDGET_ASC,
    MIN_BUDGET_DESC,
    MAX_BUDGET_ASC,
    MAX_BUDGET_DESC,
    TITLE_ASC,
    TITLE_DESC;

    public static IdeaSearchSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return RELEVANCE;
        }
        try {
            return IdeaSearchSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.ihub.exception.CustomException(
                    "Invalid sort. Allowed: RELEVANCE, MIN_BUDGET_ASC, MIN_BUDGET_DESC, " +
                            "MAX_BUDGET_ASC, MAX_BUDGET_DESC, TITLE_ASC, TITLE_DESC"
            );
        }
    }
}
