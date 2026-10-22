package com.applicationtracker.controller;

import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.ApplicationDetailResponse;
import com.applicationtracker.dto.ApplicationResponse;
import com.applicationtracker.dto.ApplicationStatsResponse;
import com.applicationtracker.dto.PagedResponse;
import com.applicationtracker.dto.UpdateApplicationRequest;
import com.applicationtracker.service.ApplicationService;
import com.applicationtracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for job applications, status progression, pipeline analytics, and follow-ups.
 * All operations are strictly scoped to the currently authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Job applications management, pipeline statistics, and follow-ups")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    @Operation(summary = "List applications", description = "Retrieves paginated job applications filtered by status and/or company name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved applications"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/applications")
    public ResponseEntity<PagedResponse<ApplicationResponse>> getApplications(
        Authentication authentication,
        @Parameter(description = "Filter by application status (optional)")
        @RequestParam(required = false) ApplicationStatus status,
        @Parameter(description = "Search by company name substring (optional)")
        @RequestParam(required = false) String company,
        @PageableDefault(size = 20, sort = "lastUpdatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = getAuthenticatedUser(authentication);
        var page = applicationService.getApplications(user, status, company, pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @Operation(summary = "Get application details", description = "Retrieves a single application along with its complete status history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application found"),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDetailResponse> getApplication(
        Authentication authentication,
        @Parameter(description = "UUID of the application", required = true)
        @PathVariable UUID id
    ) {
        User user = getAuthenticatedUser(authentication);
        ApplicationDetailResponse response = applicationService.getApplication(user, id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update application", description = "Manually updates the status and/or personal notes of an application")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid status"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @PatchMapping("/applications/{id}")
    public ResponseEntity<ApplicationDetailResponse> updateApplication(
        Authentication authentication,
        @Parameter(description = "UUID of the application", required = true)
        @PathVariable UUID id,
        @Valid @RequestBody UpdateApplicationRequest request
    ) {
        User user = getAuthenticatedUser(authentication);
        ApplicationDetailResponse updated = applicationService.updateApplication(user, id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get application pipeline stats", description = "Calculates total counts per status, response rate, active count, and weekly timeline")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pipeline statistics computed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApplicationStatsResponse> getStats(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ApplicationStatsResponse stats = applicationService.getStats(user);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get pending follow-ups", description = "Finds applications in APPLIED status with no company updates for N days")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pending follow-ups retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/followups")
    public ResponseEntity<List<ApplicationResponse>> getFollowups(
        Authentication authentication,
        @Parameter(description = "Number of days since last update (minimum 1, default 7)")
        @RequestParam(defaultValue = "7") int days
    ) {
        User user = getAuthenticatedUser(authentication);
        List<ApplicationResponse> followups = applicationService.getPendingFollowups(user, days);
        return ResponseEntity.ok(followups);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        User user = userService.resolveAuthenticatedUser(authentication);
        if (user == null) {
            throw new AccessDeniedException("Authenticated user profile not found");
        }
        return user;
    }
}
