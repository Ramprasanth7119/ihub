package com.ihub.util;

import com.ihub.model.PagedResult;
import org.springframework.http.ResponseEntity;

/**
 * Wraps a {@link PagedResult} as a plain JSON array plus pagination response headers.
 *
 * <p>{@code GET /api/ideas} and {@code GET /api/auctions} shipped as bare arrays and
 * the Next.js client consumes them that way, so paging metadata travels in headers
 * instead of an envelope. Clients that ignore the headers keep working unchanged.</p>
 */
public final class PagedResponse {

    public static final String TOTAL_COUNT = "X-Total-Count";
    public static final String PAGE = "X-Page";
    public static final String PAGE_SIZE = "X-Page-Size";
    public static final String TOTAL_PAGES = "X-Total-Pages";

    private PagedResponse() {
    }

    public static <T> ResponseEntity<java.util.List<T>> of(PagedResult<T> result) {
        return ResponseEntity.ok()
                .header(TOTAL_COUNT, String.valueOf(result.totalElements()))
                .header(PAGE, String.valueOf(result.page()))
                .header(PAGE_SIZE, String.valueOf(result.size()))
                .header(TOTAL_PAGES, String.valueOf(result.totalPages()))
                .body(result.content());
    }
}
