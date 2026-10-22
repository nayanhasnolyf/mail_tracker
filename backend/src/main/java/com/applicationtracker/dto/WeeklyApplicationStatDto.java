package com.applicationtracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metric representing the count of applications created in a specific calendar week.
 */
@Schema(description = "Weekly application count metric")
public record WeeklyApplicationStatDto(
    @Schema(description = "Calendar week identifier (e.g., 2026-W42)", example = "2026-W42")
    String week,

    @Schema(description = "Start date of the week (Monday, YYYY-MM-DD)", example = "2026-10-12")
    String startDate,

    @Schema(description = "Number of applications submitted/detected in this week", example = "5")
    long count
) {}
