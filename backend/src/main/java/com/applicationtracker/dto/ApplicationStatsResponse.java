package com.applicationtracker.dto;

import com.applicationtracker.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * High-level job search metrics, pipeline breakdown, response rates, and activity over time.
 */
@Schema(description = "Aggregated application metrics and pipeline statistics")
public record ApplicationStatsResponse(
    @Schema(description = "Total number of job applications", example = "42")
    long totalApplications,

    @Schema(description = "Number of active applications (APPLIED, ASSESSMENT, INTERVIEW)", example = "12")
    long activeApplications,

    @Schema(description = "Percentage of applications that received any company response (ASSESSMENT, INTERVIEW, OFFER, REJECTED)", example = "35.7")
    double responseRate,

    @Schema(description = "Counts per status")
    Map<ApplicationStatus, Long> countsByStatus,

    @Schema(description = "Applications created per week")
    List<WeeklyApplicationStatDto> applicationsPerWeek
) {}
