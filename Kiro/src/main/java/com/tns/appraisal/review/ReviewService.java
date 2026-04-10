package com.tns.appraisal.review;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.*;
import com.tns.appraisal.notification.NotificationService;
import com.tns.appraisal.pdf.PdfGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service handling all manager review operations for appraisal forms.
 *
 * <p>Covers the following state transitions:
 * <ul>
 *   <li>SUBMITTED → UNDER_REVIEW (startReview)</li>
 *   <li>UNDER_REVIEW → REVIEW_DRAFT_SAVED (saveReviewDraft)</li>
 *   <li>REVIEW_DRAFT_SAVED → REVIEWED_AND_COMPLETED (completeReview)</li>
 * </ul>
 *
 * <p>Both the primary manager and any assigned backup reviewer are treated as authorized
 * reviewers (Req 15.3, Property 19).
 */
@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private static final String ENTITY_TYPE = "APPRAISAL_FORM";
    private static final String ACTION_REVIEW_STARTED       = "REVIEW_STARTED";
    private static final String ACTION_REVIEW_DRAFT_SAVED   = "REVIEW_DRAFT_SAVED";
    private static final String ACTION_REVIEW_COMPLETE      = "REVIEW_COMPLETE";

    private final AppraisalFormRepository formRepository;
    private final FormStateMachine formStateMachine;
    private final AuditLogService auditLogService;
    private final PdfGenerationService pdfGenerationService;
    private final NotificationService notificationService;

    public ReviewService(AppraisalFormRepository formRepository,
                         FormStateMachine formStateMachine,
                         AuditLogService auditLogService,
                         PdfGenerationService pdfGenerationService,
                         NotificationService notificationService) {
        this.formRepository = formRepository;
        this.formStateMachine = formStateMachine;
        this.auditLogService = auditLogService;
        this.pdfGenerationService = pdfGenerationService;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Transitions a form from SUBMITTED → UNDER_REVIEW.
     * Only the primary manager or backup reviewer may call this (Req 15.3).
     *
     * @param formId     the form to open for review
     * @param reviewerId the user attempting to start the review
     * @return the updated form
     */
    @Transactional
    public AppraisalForm startReview(Long formId, Long reviewerId) {
        AppraisalForm form = getReviewableForm(formId, reviewerId);

        FormStatus current = form.getStatus();
        formStateMachine.validateTransition(current, FormStatus.UNDER_REVIEW);

        form.setStatus(FormStatus.UNDER_REVIEW);
        form.setReviewStartedAt(Instant.now());
        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(reviewerId, ACTION_REVIEW_STARTED, ENTITY_TYPE, formId,
                Map.of("previousStatus", current.name(), "newStatus", FormStatus.UNDER_REVIEW.name()),
                null);

        logger.info("Review started for form {} by reviewer {}", formId, reviewerId);
        return saved;
    }

    /**
     * Saves manager review data as a draft.
     * Transitions SUBMITTED → UNDER_REVIEW → REVIEW_DRAFT_SAVED on first manager touch.
     * Re-saves when already in REVIEW_DRAFT_SAVED stay in REVIEW_DRAFT_SAVED (idempotent re-save).
     *
     * <p>Satisfies Req 6.3: Manager SHALL be able to save the review as a draft,
     * transitioning the status to "Review Draft Saved". The manager can re-save the draft
     * multiple times without completing the review.
     *
     * @param formId     the form to update
     * @param reviewerId the reviewer performing the save
     * @param reviewData the manager's comments and ratings
     * @return the updated form
     */
    @Transactional
    public AppraisalForm saveReviewDraft(Long formId, Long reviewerId, ReviewDataDto reviewData) {
        AppraisalForm form = getReviewableForm(formId, reviewerId);
        checkManagerEditableStatus(form);

        // Auto-advance from SUBMITTED → UNDER_REVIEW on first manager touch
        if (form.getStatus() == FormStatus.SUBMITTED) {
            formStateMachine.validateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
            form.setStatus(FormStatus.UNDER_REVIEW);
            form.setReviewStartedAt(Instant.now());
        }

        FormStatus previous = form.getStatus();

        // If already REVIEW_DRAFT_SAVED, skip the state machine transition (idempotent re-save).
        // Otherwise validate and transition UNDER_REVIEW → REVIEW_DRAFT_SAVED.
        if (previous != FormStatus.REVIEW_DRAFT_SAVED) {
            formStateMachine.validateTransition(previous, FormStatus.REVIEW_DRAFT_SAVED);
        }

        applyReviewData(form, reviewData);
        form.setStatus(FormStatus.REVIEW_DRAFT_SAVED);
        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(reviewerId, ACTION_REVIEW_DRAFT_SAVED, ENTITY_TYPE, formId,
                Map.of("previousStatus", previous.name(), "newStatus", FormStatus.REVIEW_DRAFT_SAVED.name()),
                null);

        logger.info("Review draft saved for form {} by reviewer {}", formId, reviewerId);
        return saved;
    }

    /**
     * Completes the manager review, transitioning to REVIEWED_AND_COMPLETED.
     *
     * <p>On completion (Property 16): generates a PDF and stores its path on the form.
     * <p>On completion (Property 13): triggers exactly 3 async notification emails
     * (employee, manager, HR).
     *
     * @param formId     the form to complete
     * @param reviewerId the reviewer completing the review
     * @param reviewData the final manager comments and ratings
     * @return the updated form
     */
    @Transactional
    public AppraisalForm completeReview(Long formId, Long reviewerId, ReviewDataDto reviewData) {
        AppraisalForm form = getReviewableForm(formId, reviewerId);
        checkManagerEditableStatus(form);

        // Auto-advance from SUBMITTED → UNDER_REVIEW if manager goes straight to complete
        if (form.getStatus() == FormStatus.SUBMITTED) {
            formStateMachine.validateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
            form.setStatus(FormStatus.UNDER_REVIEW);
            form.setReviewStartedAt(Instant.now());
        }

        FormStatus previous = form.getStatus();
        formStateMachine.validateTransition(previous, FormStatus.REVIEWED_AND_COMPLETED);

        applyReviewData(form, reviewData);
        form.setStatus(FormStatus.REVIEWED_AND_COMPLETED);
        form.setReviewedAt(Instant.now());

        // Property 16: generate PDF and persist path before saving
        String pdfPath = pdfGenerationService.generateAndStore(form);
        form.setPdfStoragePath(pdfPath);

        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(reviewerId, ACTION_REVIEW_COMPLETE, ENTITY_TYPE, formId,
                Map.of("employeeId", form.getEmployeeId(),
                       "managerId", form.getManagerId(),
                       "newStatus", FormStatus.REVIEWED_AND_COMPLETED.name()),
                null);

        // Property 13: exactly 3 notification emails (employee, manager, HR) — sent asynchronously
        notificationService.sendReviewCompletionNotifications(
                formId, form.getEmployeeId(), form.getManagerId(), pdfPath);

        logger.info("Review completed for form {} by reviewer {}", formId, reviewerId);
        return saved;
    }

    /**
     * Fetches a form and verifies the given reviewer is authorized (primary manager or backup reviewer).
     *
     * @param formId     the form to fetch
     * @param reviewerId the user requesting access
     * @return the form if authorized
     * @throws ResourceNotFoundException   if the form does not exist
     * @throws UnauthorizedAccessException if the reviewer is not authorized for this form
     */
    @Transactional(readOnly = true)
    public AppraisalForm getReviewableForm(Long formId, Long reviewerId) {
        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE, formId));

        if (!isAuthorizedReviewer(form, reviewerId)) {
            throw new UnauthorizedAccessException("review", ENTITY_TYPE + "#" + formId);
        }
        return form;
    }

    // -------------------------------------------------------------------------
    // Access control helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if {@code userId} is the primary manager or backup reviewer for the form.
     * Satisfies Req 15.3 and Property 19.
     */
    public boolean isAuthorizedReviewer(AppraisalForm form, Long userId) {
        return userId.equals(form.getManagerId())
                || (form.getBackupReviewerId() != null && userId.equals(form.getBackupReviewerId()));
    }

    private void checkManagerEditableStatus(AppraisalForm form) {
        if (form.getStatus() != FormStatus.SUBMITTED
                && form.getStatus() != FormStatus.UNDER_REVIEW
                && form.getStatus() != FormStatus.REVIEW_DRAFT_SAVED) {
            throw new UnauthorizedAccessException(
                    "edit review",
                    "Form " + form.getId() + " is in status " + form.getStatus() + " and cannot be reviewed");
        }
    }

    // -------------------------------------------------------------------------
    // Field merge helpers
    // -------------------------------------------------------------------------

    /** Merges all manager-supplied review fields into the form's FormData. */
    private void applyReviewData(AppraisalForm form, ReviewDataDto reviewData) {
        if (reviewData == null) return;

        FormData data = form.getFormData() != null ? form.getFormData() : new FormData();

        if (reviewData.getKeyResponsibilities() != null) {
            data.setKeyResponsibilities(mergeManagerItems(
                    data.getKeyResponsibilities(), reviewData.getKeyResponsibilities()));
        }

        if (reviewData.getIdp() != null) {
            data.setIdp(mergeManagerItems(data.getIdp(), reviewData.getIdp()));
        }

        if (reviewData.getGoals() != null) {
            data.setGoals(mergeManagerItems(data.getGoals(), reviewData.getGoals()));
        }

        if (reviewData.getPolicyAdherence() != null) {
            data.setPolicyAdherence(mapPolicyAdherence(reviewData.getPolicyAdherence()));
        }

        if (reviewData.getManagerComments() != null) {
            FormData.OverallEvaluation eval = data.getOverallEvaluation() != null
                    ? data.getOverallEvaluation() : new FormData.OverallEvaluation();
            eval.setManagerComments(reviewData.getManagerComments());
            data.setOverallEvaluation(eval);
        }

        if (reviewData.getSignature() != null) {
            data.setSignature(mapSignature(reviewData.getSignature()));
        }

        form.setFormData(data);
    }

    /**
     * Merges manager review fields into existing RatedItems, preserving employee self-appraisal fields.
     */
    private List<FormData.RatedItem> mergeManagerItems(
            List<FormData.RatedItem> existing,
            List<ReviewDataDto.ReviewItemDto> incoming) {

        Map<String, FormData.RatedItem> existingMap = existing == null ? Map.of() :
                existing.stream().collect(Collectors.toMap(FormData.RatedItem::getItemId, i -> i));

        return incoming.stream().map(src -> {
            FormData.RatedItem item = existingMap.containsKey(src.getItemId())
                    ? existingMap.get(src.getItemId()) : new FormData.RatedItem();
            item.setItemId(src.getItemId());
            item.setManagerComment(src.getManagerComment());
            item.setManagerRating(src.getManagerRating() != null ? Rating.fromValue(src.getManagerRating()) : null);
            return item;
        }).collect(Collectors.toList());
    }

    private FormData.PolicyAdherence mapPolicyAdherence(ReviewDataDto.PolicyAdherenceDto dto) {
        FormData.PolicyAdherence pa = new FormData.PolicyAdherence();
        if (dto.getHrPolicy() != null) {
            FormData.PolicyScore s = new FormData.PolicyScore();
            s.setManagerRating(dto.getHrPolicy().getManagerRating());
            pa.setHrPolicy(s);
        }
        if (dto.getAvailability() != null) {
            FormData.PolicyScore s = new FormData.PolicyScore();
            s.setManagerRating(dto.getAvailability().getManagerRating());
            pa.setAvailability(s);
        }
        if (dto.getAdditionalSupport() != null) {
            FormData.PolicyScore s = new FormData.PolicyScore();
            s.setManagerRating(dto.getAdditionalSupport().getManagerRating());
            pa.setAdditionalSupport(s);
        }
        pa.setManagerComments(dto.getManagerComments());
        return pa;
    }

    private FormData.Signature mapSignature(ReviewDataDto.SignatureDto dto) {
        FormData.Signature sig = new FormData.Signature();
        sig.setPreparedBy(dto.getPreparedBy());
        sig.setReviewedBy(dto.getReviewedBy());
        sig.setTeamMemberAcknowledgement(dto.getTeamMemberAcknowledgement());
        return sig;
    }
}
