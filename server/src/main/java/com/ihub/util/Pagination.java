package com.ihub.util;

import com.ihub.exception.CustomException;

/**
 * Shared page/size normalisation so every list endpoint bounds its result set the
 * same way. Several services previously carried a private copy of this logic with
 * slightly different limits.
 */
public final class Pagination {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    public static int resolvePage(Integer page) {
        return page != null && page > 0 ? page : 0;
    }

    public static int resolveSize(Integer size) {
        return resolveSize(size, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    public static int resolveSize(Integer size, int defaultSize, int maxSize) {
        if (size == null || size <= 0) {
            return defaultSize;
        }
        if (size > maxSize) {
            throw new CustomException("Page size cannot exceed " + maxSize);
        }
        return size;
    }

    public static int offset(int page, int size) {
        return Math.multiplyExact(page, size);
    }
}
