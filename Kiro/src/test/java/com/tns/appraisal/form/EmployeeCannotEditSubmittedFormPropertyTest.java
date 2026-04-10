package com.tns.appraisal.form;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.dto.FormHeaderDto;
import com.tns.appraisal.form.dto.SaveDraftRequest;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

/**
 * Property-Based Test: Employee Cannot Edit Submitted Form (Property 12)
 *
 * <p><b>Validates: Requirements 5.6</b>
 *
 * <p>For any appraisal form in Submitted, Under_Review, Review_Draft_Saved, or
 * Reviewed_and_Completed status, an employee's attempt to modify self-appraisal
 * fields SHALL be rejected with an UnauthorizedAccessException (mapped to 403 Forbidden).
 *
 * <p>Also verifies the positive case: NOT_STARTED and DRAFT_SAVED statuses DO allow
 * employee edits.
 */
class EmployeeCannotEditSubmittedFormPropertyTest {

    /** Statuses where the employee is locked out from editing. */
    private static final List<FormStatus> LOCKED_STATUSES = List.of(
        FormStatus.SUBMITTED,
        FormStatus.UNDER_REVIEW,
        FormStatus.REVIEW_DRAFT_SAVED,
        FormStatus.REVIEWED_AND_COMPLETED
    );

    /** Statuses where the employee is allowed to edit. */
    private static final List<FormStatus> EDITABLE_STATUSES = List.of(
        FormStatus.NOT_STARTED,
        FormStatus.DRAFT_SAVED
    );

    private static final Long EMPLOYEE_ID = 42L;
    private static final Long FORM_ID     = 1L;

    private AppraisalFormRepository formRepository;
    private AuditLogService         auditLogService;
    private FormService             formService;

    @BeforeProperty
    void setUp() {
        formRepository  = Mockito.mock(AppraisalFormRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        formService     = new FormService(formRepository, auditLogService, new FormStateMachine());
    }

    // -------------------------------------------------------------------------
    // Property 1 (negative): locked statuses reject employee draft-save
    // -------------------------------------------------------------------------

    /**
     * For any locked status and any arbitrary self-appraisal draft request,
     * saveDraft MUST throw UnauthorizedAccessException.
     */
    @Property(tries = 100)
    void lockedStatus_rejectsDraftSave(
            @ForAll("lockedStatuses")   FormStatus      status,
            @ForAll("draftRequests")    SaveDraftRequest request) {

        AppraisalForm form = buildForm(EMPLOYEE_ID, status);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        Assertions.assertThatThrownBy(() -> formService.saveDraft(FORM_ID, request, EMPLOYEE_ID))
            .as("saveDraft must be rejected for status %s", status)
            .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // Property 2 (negative): locked statuses reject employee submit
    // -------------------------------------------------------------------------

    /**
     * For any locked status, submitForm MUST throw UnauthorizedAccessException.
     */
    @Property(tries = 100)
    void lockedStatus_rejectsSubmit(@ForAll("lockedStatuses") FormStatus status) {

        AppraisalForm form = buildForm(EMPLOYEE_ID, status);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        Assertions.assertThatThrownBy(() -> formService.submitForm(FORM_ID, EMPLOYEE_ID))
            .as("submitForm must be rejected for status %s", status)
            .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // Property 3 (negative): a different employee cannot edit even in editable status
    // -------------------------------------------------------------------------

    /**
     * An employee who does NOT own the form must be rejected regardless of status.
     */
    @Property(tries = 100)
    void nonOwnerEmployee_isAlwaysRejected(
            @ForAll("allStatuses")   FormStatus      status,
            @ForAll("draftRequests") SaveDraftRequest request) {

        Long otherEmployeeId = EMPLOYEE_ID + 1;
        AppraisalForm form = buildForm(EMPLOYEE_ID, status);   // owned by EMPLOYEE_ID
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        Assertions.assertThatThrownBy(() -> formService.saveDraft(FORM_ID, request, otherEmployeeId))
            .as("Non-owner employee must be rejected for status %s", status)
            .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // Property 4 (positive): editable statuses allow employee draft-save
    // -------------------------------------------------------------------------

    /**
     * For NOT_STARTED and DRAFT_SAVED, saveDraft MUST succeed (no exception).
     */
    @Property(tries = 100)
    void editableStatus_allowsDraftSave(
            @ForAll("editableStatuses") FormStatus      status,
            @ForAll("draftRequests")    SaveDraftRequest request) {

        AppraisalForm form = buildForm(EMPLOYEE_ID, status);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        Mockito.when(formRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.doNothing().when(auditLogService)
            .logAsync(Mockito.any(), Mockito.any(), Mockito.any(),
                      Mockito.any(), Mockito.any(), Mockito.any());

        Assertions.assertThatCode(() -> formService.saveDraft(FORM_ID, request, EMPLOYEE_ID))
            .as("saveDraft must be allowed for status %s", status)
            .doesNotThrowAnyException();
    }

    // =========================================================================
    // Arbitraries (providers)
    // =========================================================================

    @Provide
    Arbitrary<FormStatus> lockedStatuses() {
        return Arbitraries.of(LOCKED_STATUSES);
    }

    @Provide
    Arbitrary<FormStatus> editableStatuses() {
        return Arbitraries.of(EDITABLE_STATUSES);
    }

    @Provide
    Arbitrary<FormStatus> allStatuses() {
        return Arbitraries.of(FormStatus.values());
    }

    /**
     * Generates arbitrary SaveDraftRequest instances with varied self-appraisal content.
     * Covers: null fields, empty strings, long comments, and valid rating strings.
     */
    @Provide
    Arbitrary<SaveDraftRequest> draftRequests() {
        Arbitrary<String> comments = Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(0)
            .ofMaxLength(200)
            .injectNull(0.1);

        Arbitrary<String> ratings = Arbitraries.of(
            "EXCELS", "EXCEEDS", "MEETS", "DEVELOPING", null
        );

        Arbitrary<String> itemIds = Arbitraries.of("kr_1", "kr_2", "idp_nextgen", "goal_1");

        Arbitrary<SaveDraftRequest.SelfAppraisalItemDto> itemArb =
            Combinators.combine(itemIds, comments, ratings).as((id, comment, rating) -> {
                SaveDraftRequest.SelfAppraisalItemDto item = new SaveDraftRequest.SelfAppraisalItemDto();
                item.setItemId(id);
                item.setSelfComment(comment);
                item.setSelfRating(rating);
                return item;
            });

        Arbitrary<List<SaveDraftRequest.SelfAppraisalItemDto>> itemListArb =
            itemArb.list().ofMinSize(0).ofMaxSize(3);

        return Combinators.combine(comments, itemListArb, itemListArb, itemListArb, comments, comments)
            .as((nextYearGoals, kr, idp, goals, teamComments, reviewPeriod) -> {
                SaveDraftRequest req = new SaveDraftRequest();
                req.setNextYearGoals(nextYearGoals);
                req.setKeyResponsibilities(kr.isEmpty() ? null : kr);
                req.setIdp(idp.isEmpty() ? null : idp);
                req.setGoals(goals.isEmpty() ? null : goals);
                req.setTeamMemberComments(teamComments);

                FormHeaderDto header = new FormHeaderDto();
                header.setReviewPeriod(reviewPeriod);
                req.setHeader(header);

                return req;
            });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AppraisalForm buildForm(Long employeeId, FormStatus status) {
        AppraisalForm form = new AppraisalForm();
        form.setId(FORM_ID);
        form.setCycleId(10L);
        form.setEmployeeId(employeeId);
        form.setManagerId(99L);
        form.setTemplateId(1L);
        form.setStatus(status);
        return form;
    }
}
