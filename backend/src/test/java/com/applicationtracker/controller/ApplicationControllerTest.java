package com.applicationtracker.controller;

import com.applicationtracker.config.GlobalExceptionHandler;
import com.applicationtracker.config.SecurityConfig;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.ApplicationDetailResponse;
import com.applicationtracker.dto.ApplicationResponse;
import com.applicationtracker.dto.ApplicationStatsResponse;
import com.applicationtracker.dto.StatusHistoryResponse;
import com.applicationtracker.dto.UpdateApplicationRequest;
import com.applicationtracker.dto.WeeklyApplicationStatDto;
import com.applicationtracker.exception.ResourceNotFoundException;
import com.applicationtracker.service.ApplicationService;
import com.applicationtracker.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApplicationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private UserService userService;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test.applicant@example.com")
            .googleSubjectId("google-sub-456")
            .createdAt(Instant.now())
            .reauthRequired(false)
            .build();

        when(userService.resolveAuthenticatedUser(any())).thenReturn(testUser);
    }

    // ==========================================
    // GET /api/applications
    // ==========================================

    @Test
    @DisplayName("GET /api/applications returns 401 when unauthenticated")
    void testGetApplicationsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/applications"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/applications returns paginated list of applications")
    void testGetApplicationsReturnsPaginatedList() throws Exception {
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();

        List<ApplicationResponse> list = List.of(
            new ApplicationResponse(appId1, "Stripe", "Software Engineer", ApplicationStatus.INTERVIEW, "Notes 1", "email-1", Instant.now(), Instant.now()),
            new ApplicationResponse(appId2, "Google", "Backend Developer", ApplicationStatus.APPLIED, null, "email-2", Instant.now(), Instant.now())
        );
        Page<ApplicationResponse> page = new PageImpl<>(list, PageRequest.of(0, 20), 2);

        when(applicationService.getApplications(eq(testUser), eq(null), eq(null), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/applications")
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value(appId1.toString()))
            .andExpect(jsonPath("$.content[0].company").value("Stripe"))
            .andExpect(jsonPath("$.content[0].currentStatus").value("INTERVIEW"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /api/applications with status filter and company search query")
    void testGetApplicationsWithFilters() throws Exception {
        UUID appId = UUID.randomUUID();
        List<ApplicationResponse> filtered = List.of(
            new ApplicationResponse(appId, "Figma", "Frontend Architect", ApplicationStatus.ASSESSMENT, "Take-home", "email-3", Instant.now(), Instant.now())
        );
        Page<ApplicationResponse> page = new PageImpl<>(filtered, PageRequest.of(0, 10), 1);

        when(applicationService.getApplications(eq(testUser), eq(ApplicationStatus.ASSESSMENT), eq("Figma"), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/applications")
                .param("status", "ASSESSMENT")
                .param("company", "Figma")
                .param("page", "0")
                .param("size", "10")
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].company").value("Figma"))
            .andExpect(jsonPath("$.content[0].currentStatus").value("ASSESSMENT"));
    }

    // ==========================================
    // GET /api/applications/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/applications/{id} returns application detail with status history")
    void testGetApplicationByIdWithStatusHistory() throws Exception {
        UUID appId = UUID.randomUUID();
        UUID histId1 = UUID.randomUUID();
        UUID histId2 = UUID.randomUUID();

        List<StatusHistoryResponse> history = List.of(
            new StatusHistoryResponse(histId2, ApplicationStatus.INTERVIEW, Instant.now(), "email-int"),
            new StatusHistoryResponse(histId1, ApplicationStatus.APPLIED, Instant.now().minusSeconds(86400), "email-app")
        );

        ApplicationDetailResponse detail = new ApplicationDetailResponse(
            appId,
            "Netflix",
            "Distributed Systems Engineer",
            ApplicationStatus.INTERVIEW,
            "Passed recruiter phone screen",
            "email-app",
            Instant.now().minusSeconds(86400),
            Instant.now(),
            history
        );

        when(applicationService.getApplication(eq(testUser), eq(appId))).thenReturn(detail);

        mockMvc.perform(get("/api/applications/{id}", appId)
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appId.toString()))
            .andExpect(jsonPath("$.company").value("Netflix"))
            .andExpect(jsonPath("$.currentStatus").value("INTERVIEW"))
            .andExpect(jsonPath("$.notes").value("Passed recruiter phone screen"))
            .andExpect(jsonPath("$.statusHistory.length()").value(2))
            .andExpect(jsonPath("$.statusHistory[0].status").value("INTERVIEW"))
            .andExpect(jsonPath("$.statusHistory[1].status").value("APPLIED"));
    }

    @Test
    @DisplayName("GET /api/applications/{id} returns 404 when application does not exist")
    void testGetApplicationByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(applicationService.getApplication(eq(testUser), eq(nonExistentId)))
            .thenThrow(new ResourceNotFoundException("Application not found with id: " + nonExistentId));

        mockMvc.perform(get("/api/applications/{id}", nonExistentId)
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404));
    }

    // ==========================================
    // PATCH /api/applications/{id}
    // ==========================================

    @Test
    @DisplayName("PATCH /api/applications/{id} updates status and notes successfully")
    void testUpdateApplicationStatusAndNotes() throws Exception {
        UUID appId = UUID.randomUUID();
        UpdateApplicationRequest request = new UpdateApplicationRequest(
            ApplicationStatus.OFFER,
            "Received official offer letter with base + equity!"
        );

        ApplicationDetailResponse updated = new ApplicationDetailResponse(
            appId,
            "Apple",
            "iOS Engineer",
            ApplicationStatus.OFFER,
            "Received official offer letter with base + equity!",
            "email-01",
            Instant.now().minusSeconds(86400 * 5),
            Instant.now(),
            List.of(
                new StatusHistoryResponse(UUID.randomUUID(), ApplicationStatus.OFFER, Instant.now(), null),
                new StatusHistoryResponse(UUID.randomUUID(), ApplicationStatus.INTERVIEW, Instant.now().minusSeconds(86400 * 2), "email-int"),
                new StatusHistoryResponse(UUID.randomUUID(), ApplicationStatus.APPLIED, Instant.now().minusSeconds(86400 * 5), "email-01")
            )
        );

        when(applicationService.updateApplication(eq(testUser), eq(appId), any(UpdateApplicationRequest.class)))
            .thenReturn(updated);

        mockMvc.perform(patch("/api/applications/{id}", appId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appId.toString()))
            .andExpect(jsonPath("$.currentStatus").value("OFFER"))
            .andExpect(jsonPath("$.notes").value("Received official offer letter with base + equity!"))
            .andExpect(jsonPath("$.statusHistory.length()").value(3));
    }

    @Test
    @DisplayName("PATCH /api/applications/{id} returns 400 when notes exceed 5000 characters")
    void testUpdateApplicationValidationFailure() throws Exception {
        UUID appId = UUID.randomUUID();
        String hugeNotes = "A".repeat(5001);
        UpdateApplicationRequest request = new UpdateApplicationRequest(ApplicationStatus.INTERVIEW, hugeNotes);

        mockMvc.perform(patch("/api/applications/{id}", appId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("notes"));
    }

    @Test
    @DisplayName("PATCH /api/applications/{id} returns 400 when setting status to UNKNOWN")
    void testUpdateApplicationStatusUnknownReturns400() throws Exception {
        UUID appId = UUID.randomUUID();
        UpdateApplicationRequest request = new UpdateApplicationRequest(ApplicationStatus.UNKNOWN, "Valid note");

        when(applicationService.updateApplication(eq(testUser), eq(appId), any(UpdateApplicationRequest.class)))
            .thenThrow(new IllegalArgumentException("Cannot update application status to UNKNOWN"));

        mockMvc.perform(patch("/api/applications/{id}", appId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Cannot update application status to UNKNOWN"));
    }

    // ==========================================
    // GET /api/stats
    // ==========================================

    @Test
    @DisplayName("GET /api/stats returns pipeline stats, response rate, and weekly breakdown")
    void testGetStatsReturnsMetrics() throws Exception {
        Map<ApplicationStatus, Long> counts = Map.of(
            ApplicationStatus.APPLIED, 5L,
            ApplicationStatus.ASSESSMENT, 3L,
            ApplicationStatus.INTERVIEW, 2L,
            ApplicationStatus.OFFER, 1L,
            ApplicationStatus.REJECTED, 4L
        );

        List<WeeklyApplicationStatDto> weekly = List.of(
            new WeeklyApplicationStatDto("2026-W41", "2026-10-05", 6),
            new WeeklyApplicationStatDto("2026-W42", "2026-10-12", 9)
        );

        ApplicationStatsResponse stats = new ApplicationStatsResponse(
            15,
            10,
            66.7,
            counts,
            weekly
        );

        when(applicationService.getStats(eq(testUser))).thenReturn(stats);

        mockMvc.perform(get("/api/stats")
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalApplications").value(15))
            .andExpect(jsonPath("$.activeApplications").value(10))
            .andExpect(jsonPath("$.responseRate").value(66.7))
            .andExpect(jsonPath("$.countsByStatus.APPLIED").value(5))
            .andExpect(jsonPath("$.countsByStatus.OFFER").value(1))
            .andExpect(jsonPath("$.applicationsPerWeek.length()").value(2))
            .andExpect(jsonPath("$.applicationsPerWeek[0].week").value("2026-W41"))
            .andExpect(jsonPath("$.applicationsPerWeek[0].count").value(6));
    }

    // ==========================================
    // GET /api/followups
    // ==========================================

    @Test
    @DisplayName("GET /api/followups returns pending applications with default 7 days")
    void testGetFollowupsDefaultDays() throws Exception {
        UUID appId = UUID.randomUUID();
        List<ApplicationResponse> followups = List.of(
            new ApplicationResponse(
                appId, "Airbnb", "Backend Engineer", ApplicationStatus.APPLIED,
                null, "email-old", Instant.now().minusSeconds(86400 * 10), Instant.now().minusSeconds(86400 * 10)
            )
        );

        when(applicationService.getPendingFollowups(eq(testUser), eq(7))).thenReturn(followups);

        mockMvc.perform(get("/api/followups")
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].company").value("Airbnb"))
            .andExpect(jsonPath("$[0].currentStatus").value("APPLIED"));

        verify(applicationService).getPendingFollowups(eq(testUser), eq(7));
    }

    @Test
    @DisplayName("GET /api/followups with custom days parameter (?days=14)")
    void testGetFollowupsCustomDays() throws Exception {
        when(applicationService.getPendingFollowups(eq(testUser), eq(14))).thenReturn(List.of());

        mockMvc.perform(get("/api/followups")
                .param("days", "14")
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("sub", "google-sub-456");
                    attrs.put("email", "test.applicant@example.com");
                })))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(applicationService).getPendingFollowups(eq(testUser), eq(14));
    }
}
