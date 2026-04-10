package com.tns.appraisal.form;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.InvalidStateTransitionException;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Service handling CRUD operations, state machine transitions, and validation
 * for appraisal forms.
 */
@Service
public class FormService {

    private static final Logger logger = LoggerFactory.getLogger(FormService.class);

    private static final String ENTITY_TYPE = "APPRAISAL_FORM";
    private static final String ACTION_FORM_DRAFT_SAVED = "FORM_DRAFT_SAVED";
    private static final String ACTION_FORM_SUBMIT = "FORM_SUBMIT";
    private static final String ACTION_REVIEW_COMPLETE = "REVIEW_COMPLETE";
    private static final String ACTION_FORM_REOPEN = "FORM_REOPEN";

    private final AppraisalFormRepository formRepository;
    private final AuditLogService auditLogService;
    private final FormStateMachine formStateMachine;

    public FormService(AppraisalFormRepository formRepository,
                       AuditLogService auditLogService,
                       FormStateMachine formStateMachine) {
        this.formRepository = formRepository;
        this.auditLogService = auditLogService;
        this.formStateMachine = formStateMachine;
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Returns the most recent active form for the given user (as employee).
     */
    @Transactional(readOnly = true)
    public FormDetailDto getMyForm(Long userId) {
        List<AppraisalForm> forms = formRepository.findByEmployeeIdOrderByCreatedAtDesc(userId);
        AppraisalForm form = forms.stream()
            .filter(f -> f.getStatus() != FormStatus.REVIEWED_AND_COMPLETED)
            .findFirst()
            .orElseGet(() -> forms.isEmpty() ? null : forms.get(0));

        if (form == null) {
            throw new ResourceNotFoundException("AppraisalForm", "no active form for user " + userId);
        }
        return toDetailDto(form);
    }

    /**
     * Returns a form by ID, enforcing ownership/role-based access control.
     *
     * @param formId           the form to retrieve
     * @param requestingUserId the user making the request
     * @param roles            the roles of the requesting user
     */
    @Transactional(readOnly = true)
    public FormDetailDto getFormById(Long formId, Long requestingUserId, Set<String> roles) {
        AppraisalForm form = findFormOrThrow(formId);
        checkReadAccess(form, requestingUserId, roles);
        return toDetailDto(form);
    }

    /**
     * Returns all forms for the given user, ordered newest first.
     * Managers and HR see broader history based on their roles.
     */
    @Transactional(readOnly = true)
    public List<FormSummaryDto> getFormHistory(Long userId, Set<String> roles) {
        List<AppraisalForm> forms;
        if (isHrOrAdmin(roles)) {
            forms = formRepository.findAll();
        } else {
            forms = formRepository.findByEmployeeIdOrderByCreatedAtDesc(userId);
        }
        return forms.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Employee write operations
    // -------------------------------------------------------------------------

    /**
     * Saves employee self-appraisal fields as a draft.
     * Allowed when status is NOT_STARTED or DRAFT_SAVED.
     * Transitions NOT_STARTED → DRAFT_SAVED on first save; re-saves stay DRAFT_SAVED.
     */
    @Transactional
    public FormDetailDto saveDraft(Long formId, SaveDraftRequest request, Long employeeId) {
        AppraisalForm form = findFormOrThrow(formId);
        checkEmployeeOwnership(form, employeeId);
        checkEmployeeEditableStatus(form);

        FormData data = readFormData(form);
        applyEmployeeFieldsToFormData(data, request);

        FormStatus previousStatus = form.getStatus();
        if (previousStatus == FormStatus.NOT_STARTED) {
            validateStateTransition(previousStatus, FormStatus.DRAFT_SAVED);
            form.setStatus(FormStatus.DRAFT_SAVED);
        }
        // If already DRAFT_SAVED, status stays DRAFT_SAVED — no transition needed

        form.setFormData(data);
        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(employeeId, ACTION_FORM_DRAFT_SAVED, ENTITY_TYPE, formId,
            Map.of("previousStatus", previousStatus.name(), "newStatus", form.getStatus().name()),
            null);

        logger.info("Draft saved for form {} by employee {}", formId, employeeId);
        return toDetailDto(saved);
    }

    /**
     * Submits the employee self-appraisal.
     * Transitions NOT_STARTED or DRAFT_SAVED → SUBMITTED.
     */
    @Transactional
    public FormDetailDto submitForm(Long formId, Long employeeId) {
        AppraisalForm form = findFormOrThrow(formId);
        checkEmployeeOwnership(form, employeeId);
        checkEmployeeEditableStatus(form);

        FormStatus previousStatus = form.getStatus();
        validateStateTransition(previousStatus, FormStatus.SUBMITTED);
        form.setStatus(FormStatus.SUBMITTED);
        form.setSubmittedAt(Instant.now());

        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(employeeId, ACTION_FORM_SUBMIT, ENTITY_TYPE, formId,
            Map.of("previousStatus", previousStatus.name(), "newStatus", FormStatus.SUBMITTED.name()),
            null);

        logger.info("Form {} submitted by employee {}", formId, employeeId);
        return toDetailDto(saved);
    }

    // -------------------------------------------------------------------------
    // Manager write operations
    // -------------------------------------------------------------------------

    /**
     * Saves manager review fields as a draft.
     * Transitions SUBMITTED → UNDER_REVIEW → REVIEW_DRAFT_SAVED on first save.
     * Re-saves when already in REVIEW_DRAFT_SAVED stay in REVIEW_DRAFT_SAVED (idempotent re-save).
     *
     * <p>Satisfies Req 6.3: Manager SHALL be able to save the review as a draft,
     * transitioning the status to "Review Draft Saved". The manager can re-save the draft
     * multiple times without completing the review.
     */
    @Transactional
    public FormDetailDto saveReviewDraft(Long formId, SaveReviewDraftRequest request, Long managerId) {
        AppraisalForm form = findFormOrThrow(formId);
        checkManagerAccess(form, managerId);
        checkManagerEditableStatus(form);

        FormData data = readFormData(form);
        applyManagerFieldsToFormData(data, request);

        // Auto-advance from SUBMITTED → UNDER_REVIEW on first manager touch
        if (form.getStatus() == FormStatus.SUBMITTED) {
            validateStateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
            form.setStatus(FormStatus.UNDER_REVIEW);
            form.setReviewStartedAt(Instant.now());
        }

        FormStatus previous = form.getStatus();

        // If already REVIEW_DRAFT_SAVED, skip the state machine transition (idempotent re-save).
        // Otherwise validate and transition UNDER_REVIEW → REVIEW_DRAFT_SAVED.
        if (previous != FormStatus.REVIEW_DRAFT_SAVED) {
            validateStateTransition(previous, FormStatus.REVIEW_DRAFT_SAVED);
        }

        form.setStatus(FormStatus.REVIEW_DRAFT_SAVED);
        form.setFormData(data);

        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(managerId, ACTION_FORM_DRAFT_SAVED, ENTITY_TYPE, formId,
            Map.of("previousStatus", previous.name(), "newStatus", FormStatus.REVIEW_DRAFT_SAVED.name()),
            null);

        logger.info("Review draft saved for form {} by manager {}", formId, managerId);
        return toDetailDto(saved);
    }

    /**
     * Completes the manager review.
     * Transitions SUBMITTED/UNDER_REVIEW/REVIEW_DRAFT_SAVED → REVIEWED_AND_COMPLETED.
     */
    @Transactional
    public FormDetailDto completeReview(Long formId, SaveReviewDraftRequest request, Long managerId) {
        AppraisalForm form = findFormOrThrow(formId);
        checkManagerAccess(form, managerId);
        checkManagerEditableStatus(form);

        FormData data = readFormData(form);
        applyManagerFieldsToFormData(data, request);

        // Allow direct completion from UNDER_REVIEW or REVIEW_DRAFT_SAVED
        FormStatus current = form.getStatus();
        if (current == FormStatus.SUBMITTED) {
            // First touch by manager — go straight to UNDER_REVIEW then complete
            validateStateTransition(current, FormStatus.UNDER_REVIEW);
            form.setReviewStartedAt(Instant.now());
        }
        validateStateTransition(
            form.getStatus() == FormStatus.SUBMITTED ? FormStatus.UNDER_REVIEW : form.getStatus(),
            FormStatus.REVIEWED_AND_COMPLETED
        );

        form.setStatus(FormStatus.REVIEWED_AND_COMPLETED);
        form.setReviewedAt(Instant.now());
        if (form.getReviewStartedAt() == null) {
            form.setReviewStartedAt(Instant.now());
        }
        form.setFormData(data);

        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(managerId, ACTION_REVIEW_COMPLETE, ENTITY_TYPE, formId,
            Map.of("employeeId", form.getEmployeeId(), "newStatus", FormStatus.REVIEWED_AND_COMPLETED.name()),
            null);

        logger.info("Form {} review completed by manager {}", formId, managerId);
        return toDetailDto(saved);
    }

    // -------------------------------------------------------------------------
    // HR operations
    // -------------------------------------------------------------------------

    /**
     * Reopens a form, resetting its status to DRAFT_SAVED.
     * HR-only operation.
     */
    @Transactional
    public FormDetailDto reopenForm(Long formId, Long hrUserId) {
        AppraisalForm form = findFormOrThrow(formId);

        validateStateTransition(form.getStatus(), FormStatus.DRAFT_SAVED);
        FormStatus previousStatus = form.getStatus();
        form.setStatus(FormStatus.DRAFT_SAVED);
        form.setSubmittedAt(null);
        form.setReviewStartedAt(null);
        form.setReviewedAt(null);

        AppraisalForm saved = formRepository.save(form);

        auditLogService.logAsync(hrUserId, ACTION_FORM_REOPEN, ENTITY_TYPE, formId,
            Map.of("previousStatus", previousStatus.name(), "newStatus", FormStatus.DRAFT_SAVED.name()),
            null);

        logger.info("Form {} reopened by HR user {}", formId, hrUserId);
        return toDetailDto(saved);
    }

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    /**
     * Validates that a transition from {@code current} to {@code target} is allowed.
     *
     * @throws InvalidStateTransitionException if the transition is not permitted
     */
    public void validateStateTransition(FormStatus current, FormStatus target) {
        formStateMachine.validateTransition(current, target);
    }

    // -------------------------------------------------------------------------
    // Access control helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if {@code userId} is the primary manager or backup reviewer for the form.
     */
    public boolean isAuthorizedReviewer(AppraisalForm form, Long userId) {
        return userId.equals(form.getManagerId())
            || (form.getBackupReviewerId() != null && userId.equals(form.getBackupReviewerId()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AppraisalForm findFormOrThrow(Long formId) {
        return formRepository.findById(formId)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE, formId));
    }

    private void checkReadAccess(AppraisalForm form, Long userId, Set<String> roles) {
        if (isHrOrAdmin(roles)) return;
        if (userId.equals(form.getEmployeeId())) return;
        if (isAuthorizedReviewer(form, userId)) return;
        throw new UnauthorizedAccessException("read", ENTITY_TYPE + "#" + form.getId());
    }

    private void checkEmployeeOwnership(AppraisalForm form, Long employeeId) {
        if (!employeeId.equals(form.getEmployeeId())) {
            throw new UnauthorizedAccessException("edit self-appraisal", ENTITY_TYPE + "#" + form.getId());
        }
    }

    private void checkEmployeeEditableStatus(AppraisalForm form) {
        if (form.getStatus() != FormStatus.NOT_STARTED && form.getStatus() != FormStatus.DRAFT_SAVED) {
            throw new UnauthorizedAccessException(
                "edit self-appraisal",
                "Form is in status " + form.getStatus() + " and cannot be edited by employee"
            );
        }
    }

    private void checkManagerAccess(AppraisalForm form, Long managerId) {
        if (!isAuthorizedReviewer(form, managerId)) {
            throw new UnauthorizedAccessException("review", ENTITY_TYPE + "#" + form.getId());
        }
    }

    private void checkManagerEditableStatus(AppraisalForm form) {
        Set<FormStatus> managerEditable = Set.of(
            FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW, FormStatus.REVIEW_DRAFT_SAVED
        );
        if (!managerEditable.contains(form.getStatus())) {
            throw new UnauthorizedAccessException(
                "edit review",
                "Form is in status " + form.getStatus() + " and cannot be reviewed"
            );
        }
    }

    private boolean isHrOrAdmin(Set<String> roles) {
        return roles != null && (roles.contains("HR") || roles.contains("ADMIN"));
    }

    // -------------------------------------------------------------------------
    // JSON serialization helpers
    // -------------------------------------------------------------------------

    /** Reads the FormData domain object from the entity (returns empty instance if null). */
    private FormData readFormData(AppraisalForm form) {
        FormData data = form.getFormData();
        return data != null ? data : new FormData();
    }

    // -------------------------------------------------------------------------
    // Field merge helpers
    // -------------------------------------------------------------------------

    /** Merges employee-supplied fields into the existing FormData domain object. */
    private void applyEmployeeFieldsToFormData(FormData data, SaveDraftRequest request) {
        if (request.getHeader() != null) {
            FormHeaderDto h = request.getHeader();
            FormData.Header header = new FormData.Header();
            header.setDateOfHire(h.getDateOfHire());
            header.setDateOfReview(h.getDateOfReview());
            header.setReviewPeriod(h.getReviewPeriod());
            header.setTypeOfReview(h.getTypeOfReview());
            data.setHeader(header);
        }

        if (request.getKeyResponsibilities() != null) {
            data.setKeyResponsibilities(mergeEmployeeRatedItems(
                data.getKeyResponsibilities(), request.getKeyResponsibilities()));
        }

        if (request.getIdp() != null) {
            data.setIdp(mergeEmployeeRatedItems(data.getIdp(), request.getIdp()));
        }

        if (request.getGoals() != null) {
            data.setGoals(mergeEmployeeRatedItems(data.getGoals(), request.getGoals()));
        }

        if (request.getNextYearGoals() != null) {
            data.setNextYearGoals(request.getNextYearGoals());
        }

        if (request.getTeamMemberComments() != null) {
            FormData.OverallEvaluation eval = data.getOverallEvaluation() != null
                ? data.getOverallEvaluation() : new FormData.OverallEvaluation();
            eval.setTeamMemberComments(request.getTeamMemberComments());
            data.setOverallEvaluation(eval);
        }
    }

    /** Merges manager-supplied fields into the existing FormData domain object. */
    private void applyManagerFieldsToFormData(FormData data, SaveReviewDraftRequest request) {
        if (request.getKeyResponsibilities() != null) {
            data.setKeyResponsibilities(mergeManagerRatedItems(
                data.getKeyResponsibilities(), request.getKeyResponsibilities()));
        }

        if (request.getIdp() != null) {
            data.setIdp(mergeManagerRatedItems(data.getIdp(), request.getIdp()));
        }

        if (request.getGoals() != null) {
            data.setGoals(mergeManagerRatedItems(data.getGoals(), request.getGoals()));
        }

        if (request.getPolicyAdherence() != null) {
            PolicyAdherenceDto pa = request.getPolicyAdherence();
            FormData.PolicyAdherence policy = new FormData.PolicyAdherence();
            if (pa.getHrPolicy() != null) {
                FormData.PolicyScore score = new FormData.PolicyScore();
                score.setManagerRating(pa.getHrPolicy().getManagerRating());
                policy.setHrPolicy(score);
            }
            if (pa.getAvailability() != null) {
                FormData.PolicyScore score = new FormData.PolicyScore();
                score.setManagerRating(pa.getAvailability().getManagerRating());
                policy.setAvailability(score);
            }
            if (pa.getAdditionalSupport() != null) {
                FormData.PolicyScore score = new FormData.PolicyScore();
                score.setManagerRating(pa.getAdditionalSupport().getManagerRating());
                policy.setAdditionalSupport(score);
            }
            policy.setManagerComments(pa.getManagerComments());
            data.setPolicyAdherence(policy);
        }

        if (request.getManagerComments() != null) {
            FormData.OverallEvaluation eval = data.getOverallEvaluation() != null
                ? data.getOverallEvaluation() : new FormData.OverallEvaluation();
            eval.setManagerComments(request.getManagerComments());
            data.setOverallEvaluation(eval);
        }

        if (request.getSignature() != null) {
            SignatureDto s = request.getSignature();
            FormData.Signature sig = new FormData.Signature();
            sig.setPreparedBy(s.getPreparedBy());
            sig.setReviewedBy(s.getReviewedBy());
            sig.setTeamMemberAcknowledgement(s.getTeamMemberAcknowledgement());
            data.setSignature(sig);
        }
    }

    /**
     * Merges employee self-appraisal fields into existing RatedItems, preserving manager fields.
     */
    private List<FormData.RatedItem> mergeEmployeeRatedItems(
            List<FormData.RatedItem> existing,
            List<SaveDraftRequest.SelfAppraisalItemDto> incoming) {

        Map<String, FormData.RatedItem> existingMap = existing == null ? Map.of() :
            existing.stream().collect(Collectors.toMap(FormData.RatedItem::getItemId, i -> i));

        return incoming.stream().map(src -> {
            FormData.RatedItem item = existingMap.containsKey(src.getItemId())
                ? existingMap.get(src.getItemId()) : new FormData.RatedItem();
            item.setItemId(src.getItemId());
            item.setSelfComment(src.getSelfComment());
            item.setSelfRating(src.getSelfRating() != null ? Rating.fromValue(src.getSelfRating()) : null);
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * Merges manager review fields into existing RatedItems, preserving employee fields.
     */
    private List<FormData.RatedItem> mergeManagerRatedItems(
            List<FormData.RatedItem> existing,
            List<SaveReviewDraftRequest.ReviewItemDto> incoming) {

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

    // -------------------------------------------------------------------------
    // DTO mapping
    // -------------------------------------------------------------------------

    private FormDetailDto toDetailDto(AppraisalForm form) {
        FormDetailDto dto = new FormDetailDto();
        dto.setId(form.getId());
        dto.setCycleId(form.getCycleId());
        dto.setEmployeeId(form.getEmployeeId());
        dto.setManagerId(form.getManagerId());
        dto.setBackupReviewerId(form.getBackupReviewerId());
        dto.setTemplateId(form.getTemplateId());
        dto.setStatus(form.getStatus());
        dto.setFormData(toFormDataDto(form.getFormData()));
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setReviewStartedAt(form.getReviewStartedAt());
        dto.setReviewedAt(form.getReviewedAt());
        dto.setPdfStoragePath(form.getPdfStoragePath());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setUpdatedAt(form.getUpdatedAt());
        return dto;
    }

    /** Converts the FormData domain object to a FormDataDto for API responses. */
    private FormDataDto toFormDataDto(FormData data) {
        if (data == null) return new FormDataDto();
        FormDataDto dto = new FormDataDto();

        if (data.getHeader() != null) {
            FormHeaderDto h = new FormHeaderDto();
            h.setDateOfHire(data.getHeader().getDateOfHire());
            h.setDateOfReview(data.getHeader().getDateOfReview());
            h.setReviewPeriod(data.getHeader().getReviewPeriod());
            h.setTypeOfReview(data.getHeader().getTypeOfReview());
            dto.setHeader(h);
        }

        if (data.getKeyResponsibilities() != null) {
            dto.setKeyResponsibilities(data.getKeyResponsibilities().stream()
                .map(this::toFormItemDto).collect(Collectors.toList()));
        }

        if (data.getIdp() != null) {
            dto.setIdp(data.getIdp().stream()
                .map(this::toFormItemDto).collect(Collectors.toList()));
        }

        if (data.getGoals() != null) {
            dto.setGoals(data.getGoals().stream()
                .map(this::toFormItemDto).collect(Collectors.toList()));
        }

        if (data.getPolicyAdherence() != null) {
            FormData.PolicyAdherence pa = data.getPolicyAdherence();
            PolicyAdherenceDto paDto = new PolicyAdherenceDto();
            if (pa.getHrPolicy() != null) {
                PolicyAdherenceDto.PolicyRatingDto s = new PolicyAdherenceDto.PolicyRatingDto();
                s.setManagerRating(pa.getHrPolicy().getManagerRating());
                paDto.setHrPolicy(s);
            }
            if (pa.getAvailability() != null) {
                PolicyAdherenceDto.PolicyRatingDto s = new PolicyAdherenceDto.PolicyRatingDto();
                s.setManagerRating(pa.getAvailability().getManagerRating());
                paDto.setAvailability(s);
            }
            if (pa.getAdditionalSupport() != null) {
                PolicyAdherenceDto.PolicyRatingDto s = new PolicyAdherenceDto.PolicyRatingDto();
                s.setManagerRating(pa.getAdditionalSupport().getManagerRating());
                paDto.setAdditionalSupport(s);
            }
            paDto.setManagerComments(pa.getManagerComments());
            dto.setPolicyAdherence(paDto);
        }

        dto.setNextYearGoals(data.getNextYearGoals());

        if (data.getOverallEvaluation() != null) {
            OverallEvaluationDto eval = new OverallEvaluationDto();
            eval.setManagerComments(data.getOverallEvaluation().getManagerComments());
            eval.setTeamMemberComments(data.getOverallEvaluation().getTeamMemberComments());
            dto.setOverallEvaluation(eval);
        }

        if (data.getSignature() != null) {
            SignatureDto sig = new SignatureDto();
            sig.setPreparedBy(data.getSignature().getPreparedBy());
            sig.setReviewedBy(data.getSignature().getReviewedBy());
            sig.setTeamMemberAcknowledgement(data.getSignature().getTeamMemberAcknowledgement());
            dto.setSignature(sig);
        }

        return dto;
    }

    private FormItemDto toFormItemDto(FormData.RatedItem item) {
        FormItemDto dto = new FormItemDto();
        dto.setItemId(item.getItemId());
        dto.setSelfComment(item.getSelfComment());
        dto.setSelfRating(item.getSelfRating() != null ? item.getSelfRating().name() : null);
        dto.setManagerComment(item.getManagerComment());
        dto.setManagerRating(item.getManagerRating() != null ? item.getManagerRating().name() : null);
        return dto;
    }

    private FormSummaryDto toSummaryDto(AppraisalForm form) {
        FormSummaryDto dto = new FormSummaryDto();
        dto.setId(form.getId());
        dto.setCycleId(form.getCycleId());
        dto.setEmployeeId(form.getEmployeeId());
        dto.setManagerId(form.getManagerId());
        dto.setStatus(form.getStatus());
        dto.setSubmittedAt(form.getSubmittedAt());
        dto.setReviewedAt(form.getReviewedAt());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setUpdatedAt(form.getUpdatedAt());
        return dto;
    }
}
