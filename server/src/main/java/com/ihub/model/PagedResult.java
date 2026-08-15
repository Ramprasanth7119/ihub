package com.ihub.model;

import java.util.List;

/**
 * Internal carrier for a page of rows plus the total row count.
 *
 * <p>Used by services whose public REST contract is still a bare JSON array — the
 * controller returns {@link #content()} as the body and surfaces the paging metadata
 * as {@code X-Total-Count} / {@code X-Page} / {@code X-Page-Size} response headers.
 * This bounds the underlying query without breaking existing clients. Endpoints that
 * were designed with an envelope from the start keep using
 * {@link com.ihub.dto.AdminPageResponse} instead.</p>
 */
public record PagedResult<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
