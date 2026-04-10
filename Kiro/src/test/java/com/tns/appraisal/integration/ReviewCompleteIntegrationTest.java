package com.tns.appraisal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.appraisal.config.SecurityConfig;
import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.form.AppraisalForm;
import com.tns.appraisal.form.FormStatus;
import com.tns.appraisal.form.dto.PolicyAdherenceDto;
import com.tns.appraisal.form.dto.SaveReviewDraftRequest;
import com.tns.appraisal.notification.NotificationService;
import com.tns.appraisal.pdf.PdfGenerationService;
import com.tns.appraisal.review.ReviewController;
import com.tns.appraisal.review.ReviewDataDto;
import com.tns.appraisal.review.ReviewService;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the manager review completion workflow.
 *
 * <p>Tests the {@link ReviewController} endpoints:
 * <ul>
 *   <li>PUT  /api/forms/{id}/review/draft  — save review draft</li>
 *   <li>POST /api/forms/{id}/review/complete — complete review</li>
 * </ul>
 *
 * <p>Covers the final steps of Property 11 (state machine: UNDER_REVIEW → REVIEWED_AND_COMPLETED)
 * and verifies that reviewedAt is recorded on completion.</p>
 */
@WebMvcTest(controllers = {ReviewController.class})
@Import(SecurityConfig.class)
class ReviewCompleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private PdfGenerationService pdfGenerationService;

    @MockBean
    private NotificationService notificationService;

    private static final Long EMPLOYEE_ID = 1L;
    private static final Long MANAGER_ID  = 2L;
    private static final Long FORM_ID     = 10L;
    private static final Long CYCLE_ID    = 100L;
    private static final Long TEMPLATE_ID = 5L;

    @BeforeEach
    void setUp() {
        doNothing().when(auditLogService)
                .logAsync(anyLong(), anyString(), anyString(), anyLong(), anyMap(), any());
    }

    // =========================================================================
    // Manager completes review — status becomes REVIEWED_AND_COMPLETED
    // =========================================================================

    @Test
    @WithMockUser(username = "2", roles = {"MANAGER"})
    void completeReview_statusBecomesReviewedAndCompleted() throws Exception {
        AppraisalForm completedForm = buildForm(FormStatus.REVIEWED_AND_COMPLETED);
        completedForm.setReviewedAt(Instant.now());
        when(reviewService.completeReview(eq(FORM_ID), eq(MANAGER_ID), any(ReviewDataDto.class)))
                .thenReturn(completedForm);

        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("Final manager comments"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REVIEWED_AND_COMPLETED"));
    }

    @Test
    @WithMockUser(username = "2", roles = {"MANAGER"})
    void completeReview_reviewedAtIsRecorded() throws Exception {
        AppraisalForm completedForm = buildForm(FormStatus.REVIEWED_AND_COMPLETED);
        completedForm.setReviewedAt(Instant.now());
        when(reviewService.completeReview(eq(FORM_ID), eq(MANAGER_ID), any(ReviewDataDto.class)))
                .thenReturn(completedForm);

        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("Excellent work"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWED_AND_COMPLETED"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "2", roles = {"MANAGER"})
    void completeReview_whenInvalidTransition_returns409() throws Exception {
        when(reviewService.completeReview(eq(FORM_ID), eq(MANAGER_ID), any(ReviewDataDto.class)))
                .thenThrow(new com.tns.appraisal.exception.InvalidStateTransitionException(
                        "NOT_STARTED", "REVIEWED_AND_COMPLETED"));

        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("Trying to complete"))))
                .andExpect(status().isConflict());
    }

    @Test
    void completeReview_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("test"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = {"EMPLOYEE"})
    void completeReview_byEmployee_returns403() throws Exception {
        // @PreAuthorize("hasRole('MANAGER')") on ReviewController.completeReview
        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = {"MANAGER"})
    void completeReview_whenNotAuthorizedReviewer_returns403() throws Exception {
        when(reviewService.completeReview(eq(FORM_ID), eq(MANAGER_ID), any(ReviewDataDto.class)))
                .thenThrow(new com.tns.appraisal.exception.UnauthorizedAccessException(
                        "review", "APPRAISAL_FORM#" + FORM_ID));

        mockMvc.perform(post("/api/forms/{id}/review/complete", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("test"))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Manager saves review draft via ReviewController
    // =========================================================================

    @Test
    @WithMockUser(username = "2", roles = {"MANAGER"})
    void saveReviewDraft_statusBecomesReviewDraftSaved() throws Exception {
        AppraisalForm draftForm = buildForm(FormStatus.REVIEW_DRAFT_SAVED);
        when(reviewService.saveReviewDraft(eq(FORM_ID), eq(MANAGER_ID), any(ReviewDataDto.class)))
                .thenReturn(draftForm);

        mockMvc.perform(put("/api/forms/{id}/review/draft", FORM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReviewDraftRequest("Draft comments"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEW_DRAFT_SAVED"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AppraisalForm buildForm(FormStatus status) {
        AppraisalForm form = new AppraisalForm();
        form.setId(FORM_ID);
        form.setCycleId(CYCLE_ID);
        form.setEmployeeId(EMPLOYEE_ID);
        form.setManagerId(MANAGER_ID);
        form.setTemplateId(TEMPLATE_ID);
        form.setStatus(status);
        form.setCreatedAt(Instant.now());
        form.setUpdatedAt(Instant.now());
        return form;
    }

    private SaveReviewDraftRequest buildReviewDraftRequest(String managerComments) {
        SaveReviewDraftRequest req = new SaveReviewDraftRequest();
        req.setManagerComments(managerComments);

        SaveReviewDraftRequest.ReviewItemDto item = new SaveReviewDraftRequest.ReviewItemDto();
        item.setItemId("kr_1");
        item.setManagerComment("Good performance");
        item.setManagerRating("EXCEEDS");
        req.setKeyResponsibilities(List.of(item));

        PolicyAdherenceDto pa = new PolicyAdherenceDto();
        PolicyAdherenceDto.PolicyRatingDto hrPolicy = new PolicyAdherenceDto.PolicyRatingDto();
        hrPolicy.setManagerRating(8);
        pa.setHrPolicy(hrPolicy);
        pa.setManagerComments("Follows all policies");
        req.setPolicyAdherence(pa);

        return req;
    }
}
