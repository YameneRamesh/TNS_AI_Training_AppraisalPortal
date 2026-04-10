package com.tns.appraisal.review;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.form.AppraisalForm;
import com.tns.appraisal.form.dto.FormDetailDto;
import com.tns.appraisal.form.dto.SaveReviewDraftRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller exposing manager review endpoints under /api/forms/{id}/review.
 *
 * <p>Access control summary:</p>
 * <ul>
 *   <li>PUT  /api/forms/{id}/review/draft     – MANAGER (primary or backup reviewer)</li>
 *   <li>POST /api/forms/{id}/review/complete  – MANAGER (primary or backup reviewer)</li>
 * </ul>
 *
 * <p>Both endpoints require MANAGER role at the Spring Security layer. Fine-grained
 * ownership checks (primary vs. backup reviewer) are enforced inside ReviewService.</p>
 */
@RestController
@RequestMapping("/api/forms/{id}/review")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final AuditLogService auditLogService;

    public ReviewController(ReviewService reviewService, AuditLogService auditLogService) {
        this.reviewService = reviewService;
        this.auditLogService = auditLogService;
    }

    // -------------------------------------------------------------------------
    // PUT /api/forms/{id}/review/draft  – MANAGER: save review draft
    // -------------------------------------------------------------------------

    /**
     * Saves the manager's review as a draft.
     * Transitions SUBMITTED → UNDER_REVIEW → REVIEW_DRAFT_SAVED on first manager touch.
     * Re-saves when already in REVIEW_DRAFT_SAVED stay in REVIEW_DRAFT_SAVED (idempotent re-save).
     * Both the primary manager and any assigned backup reviewer are authorized (Req 15.3).
     */
    @PutMapping("/draft")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FormDetailDto>> saveReviewDraft(
            @PathVariable Long id,
            @RequestBody SaveReviewDraftRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        Long userId = getUserId(auth);
        logger.info("Manager {} saving review draft for form {}", userId, id);

        ReviewDataDto reviewData = toReviewDataDto(request);
        AppraisalForm saved = reviewService.saveReviewDraft(id, userId, reviewData);
        FormDetailDto dto = toDetailDto(saved);

        auditLogService.logAsync(
                userId, "REVIEW_DRAFT_SAVED", "APPRAISAL_FORM", id,
                Map.of("employeeId", dto.getEmployeeId(), "status", dto.getStatus().name()),
                httpRequest.getRemoteAddr()
        );

        return ResponseEntity.ok(ApiResponse.success("Review draft saved successfully", dto));
    }

    // -------------------------------------------------------------------------
    // POST /api/forms/{id}/review/complete  – MANAGER: complete review
    // -------------------------------------------------------------------------

    /**
     * Completes the manager's review, transitioning to REVIEWED_AND_COMPLETED.
     * Triggers PDF generation and sends 3 notification emails (employee, manager, HR).
     * Both the primary manager and any assigned backup reviewer are authorized (Req 15.3).
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FormDetailDto>> completeReview(
            @PathVariable Long id,
            @RequestBody SaveReviewDraftRequest request,
            Authentication auth) {

        Long userId = getUserId(auth);
        logger.info("Manager {} completing review for form {}", userId, id);

        ReviewDataDto reviewData = toReviewDataDto(request);
        AppraisalForm completed = reviewService.completeReview(id, userId, reviewData);
        FormDetailDto dto = toDetailDto(completed);

        return ResponseEntity.ok(ApiResponse.success("Review completed successfully", dto));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Extracts the numeric user ID from the Spring Security principal name. */
    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    /**
     * Converts the inbound {@link SaveReviewDraftRequest} (form-package DTO) into the
     * {@link ReviewDataDto} expected by ReviewService.
     */
    private ReviewDataDto toReviewDataDto(SaveReviewDraftRequest req) {
        if (req == null) return new ReviewDataDto();

        ReviewDataDto dto = new ReviewDataDto();

        if (req.getKeyResponsibilities() != null) {
            dto.setKeyResponsibilities(req.getKeyResponsibilities().stream()
                    .map(this::toReviewItemDto)
                    .collect(Collectors.toList()));
        }

        if (req.getIdp() != null) {
            dto.setIdp(req.getIdp().stream()
                    .map(this::toReviewItemDto)
                    .collect(Collectors.toList()));
        }

        if (req.getGoals() != null) {
            dto.setGoals(req.getGoals().stream()
                    .map(this::toReviewItemDto)
                    .collect(Collectors.toList()));
        }

        if (req.getPolicyAdherence() != null) {
            dto.setPolicyAdherence(toPolicyAdherenceDto(req.getPolicyAdherence()));
        }

        dto.setManagerComments(req.getManagerComments());

        if (req.getSignature() != null) {
            ReviewDataDto.SignatureDto sig = new ReviewDataDto.SignatureDto();
            sig.setPreparedBy(req.getSignature().getPreparedBy());
            sig.setReviewedBy(req.getSignature().getReviewedBy());
            sig.setTeamMemberAcknowledgement(req.getSignature().getTeamMemberAcknowledgement());
            dto.setSignature(sig);
        }

        return dto;
    }

    private ReviewDataDto.ReviewItemDto toReviewItemDto(SaveReviewDraftRequest.ReviewItemDto src) {
        ReviewDataDto.ReviewItemDto item = new ReviewDataDto.ReviewItemDto();
        item.setItemId(src.getItemId());
        item.setManagerComment(src.getManagerComment());
        item.setManagerRating(src.getManagerRating());
        return item;
    }

    private ReviewDataDto.PolicyAdherenceDto toPolicyAdherenceDto(
            com.tns.appraisal.form.dto.PolicyAdherenceDto src) {
        ReviewDataDto.PolicyAdherenceDto pa = new ReviewDataDto.PolicyAdherenceDto();
        pa.setManagerComments(src.getManagerComments());

        if (src.getHrPolicy() != null) {
            ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto r = new ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto();
            r.setManagerRating(src.getHrPolicy().getManagerRating());
            pa.setHrPolicy(r);
        }
        if (src.getAvailability() != null) {
            ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto r = new ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto();
            r.setManagerRating(src.getAvailability().getManagerRating());
            pa.setAvailability(r);
        }
        if (src.getAdditionalSupport() != null) {
            ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto r = new ReviewDataDto.PolicyAdherenceDto.PolicyRatingDto();
            r.setManagerRating(src.getAdditionalSupport().getManagerRating());
            pa.setAdditionalSupport(r);
        }
        return pa;
    }

    /**
     * Maps an {@link AppraisalForm} entity to a {@link FormDetailDto} for the API response.
     * Mirrors the mapping in FormService#toDetailDto.
     */
    private FormDetailDto toDetailDto(AppraisalForm form) {
        FormDetailDto dto = new FormDetailDto();
        dto.setId(form.getId());
        dto.setCycleId(form.getCycleId());
        dto.setEmployeeId(form.getEmployeeId());
        dto.setManagerId(form.getManagerId());
        dto.setBackupReviewerId(form.getBackupReviewerId());
        dto.setTemplateId(form.getTemplateId());
        dto.setStatus(form.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setReviewStartedAt(form.getReviewStartedAt());
        dto.setReviewedAt(form.getReviewedAt());
        dto.setPdfStoragePath(form.getPdfStoragePath());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setUpdatedAt(form.getUpdatedAt());
        // formData mapping is omitted here — FormService owns the full toFormDataDto conversion.
        // The controller returns the essential status/metadata fields sufficient for the review response.
        return dto;
    }
}
