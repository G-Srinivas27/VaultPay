package com.srinivas.vaultpay.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A generic wrapper for paginated API responses.
 *
 * <p>Spring Data returns a {@link Page} object which contains both the data
 * AND the pagination metadata. This record extracts the fields we want to
 * expose to the API consumer — we don't expose the full internal Page object
 * because it contains Spring-specific internals clients don't need.
 *
 * <p><b>Fields explained:</b>
 * <ul>
 *   <li>{@code content}       — the actual data for this page (e.g., list of users)</li>
 *   <li>{@code page}          — current page number (0-indexed)</li>
 *   <li>{@code size}          — requested page size (items per page)</li>
 *   <li>{@code totalElements} — total number of records in the database</li>
 *   <li>{@code totalPages}    — total number of pages available</li>
 *   <li>{@code last}          — true if this is the last page (useful for client "load more" logic)</li>
 * </ul>
 *
 * <p><b>Why a separate class instead of using Page directly?</b>
 * {@code Page<T>} is a Spring interface with many internal methods. Serializing it
 * directly to JSON produces a complex, verbose structure with nested objects clients
 * don't need. {@code PagedResponse<T>} gives us full control over the JSON shape.
 *
 * @param <T> the type of items in this page (e.g., UserResponse, TransactionResponse)
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    /**
     * Factory method — converts Spring's {@link Page} into our clean DTO.
     *
     * <p>Usage example:
     * <pre>
     *   Page&lt;User&gt; userPage = userRepository.findAll(pageable);
     *   Page&lt;UserResponse&gt; dtoPage = userPage.map(UserResponse::from);
     *   return PagedResponse.from(dtoPage);
     * </pre>
     *
     * @param page the Spring Data Page object
     * @param <T>  the type of items
     * @return a PagedResponse wrapping the page data and metadata
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),           // the list of items for this page
                page.getNumber(),            // current page index (0-based)
                page.getSize(),              // page size requested
                page.getTotalElements(),     // total records in DB (from COUNT query)
                page.getTotalPages(),        // total pages = ceil(totalElements / size)
                page.isLast()               // is this the final page?
        );
    }
}
