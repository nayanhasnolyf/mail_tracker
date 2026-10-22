package com.applicationtracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper.
 */
@Schema(description = "Paginated response wrapper")
public record PagedResponse<T>(
    @Schema(description = "Page elements")
    List<T> content,

    @Schema(description = "Current page index (0-based)", example = "0")
    int page,

    @Schema(description = "Page size", example = "10")
    int size,

    @Schema(description = "Total number of elements across all pages", example = "42")
    long totalElements,

    @Schema(description = "Total number of pages", example = "5")
    int totalPages,

    @Schema(description = "Whether this is the first page", example = "true")
    boolean isFirst,

    @Schema(description = "Whether this is the last page", example = "false")
    boolean isLast
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
