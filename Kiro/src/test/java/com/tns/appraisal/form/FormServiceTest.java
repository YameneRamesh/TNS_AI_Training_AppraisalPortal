package com.tns.appraisal.form;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.InvalidStateTransitionException;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormServiceTest {

    @Mock
    private AppraisalFormRepository formRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FormStateMachine formStateMachine;

    @InjectMocks
    private FormService formService;

    private static final Long EMPLOYEE_ID = 1L;
    private static final Long MANAGER_ID  = 2L;
    private static final Long OTHER_ID    = 99L;
    private static final Long FORM_ID     = 10L;

    private AppraisalForm buildForm(FormStatus status) {
        AppraisalForm form = new AppraisalForm();
        form.setId(FORM_ID);
        form.setCycleId(100L);
        form.setEmployeeId(EMPLOYEE_ID);
        form.setManagerId(MANAGER_ID);
        form.setTemplateId(5L);
        form.setStatus(status);
        return form;
    }

    private SaveDraftRequest buildDraftRequest() {
        SaveDraftRequest req = new SaveDraftRequest();
        req.setNextYearGoals("Improve skills");
        req.setTeamMemberComments("Good year");
        return req;
    }

    @BeforeEach
    void setUp() {
        // formRepository.save returns the argument by default
        when(formRepository.save(any(AppraisalForm.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // saveDraft
    // -------------------------------------------------------------------------

    @Test
    void saveDraft_whenNotStarted_transitionsToDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.NOT_STARTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.saveDraft(FORM_ID, buildDraftRequest(), EMPLOYEE_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.DRAFT_SAVED);
        verify(formStateMachine).validateTransition(FormStatus.NOT_STARTED, FormStatus.DRAFT_SAVED);
    }

    @Test
    void saveDraft_whenAlreadyDraftSaved_staysInDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.saveDraft(FORM_ID, buildDraftRequest(), EMPLOYEE_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.DRAFT_SAVED);
        // No state machine call needed for re-save
        verify(formStateMachine, never()).validateTransition(any(), any());
    }

    @Test
    void saveDraft_whenSubmitted_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.saveDraft(FORM_ID, buildDraftRequest(), EMPLOYEE_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveDraft_whenUnderReview_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.saveDraft(FORM_ID, buildDraftRequest(), EMPLOYEE_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveDraft_whenReviewedAndCompleted_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.REVIEWED_AND_COMPLETED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.saveDraft(FORM_ID, buildDraftRequest(), EMPLOYEE_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveDraft_whenNotOwner_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.NOT_STARTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.saveDraft(FORM_ID, buildDraftRequest(), OTHER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveDraft_persistsEmployeeFields() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        SaveDraftRequest req = buildDraftRequest();
        req.setNextYearGoals("Learn Java 21");
        req.setTeamMemberComments("Great performance");

        FormDetailDto result = formService.saveDraft(FORM_ID, req, EMPLOYEE_ID);

        assertThat(result.getFormData().getNextYearGoals()).isEqualTo("Learn Java 21");
        assertThat(result.getFormData().getOverallEvaluation().getTeamMemberComments())
                .isEqualTo("Great performance");
    }

    // -------------------------------------------------------------------------
    // submitForm
    // -------------------------------------------------------------------------

    @Test
    void submitForm_whenNotStarted_transitionsToSubmitted() {
        AppraisalForm form = buildForm(FormStatus.NOT_STARTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.submitForm(FORM_ID, EMPLOYEE_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.SUBMITTED);
        verify(formStateMachine).validateTransition(FormStatus.NOT_STARTED, FormStatus.SUBMITTED);
    }

    @Test
    void submitForm_whenDraftSaved_transitionsToSubmitted() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.submitForm(FORM_ID, EMPLOYEE_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.SUBMITTED);
        verify(formStateMachine).validateTransition(FormStatus.DRAFT_SAVED, FormStatus.SUBMITTED);
    }

    @Test
    void submitForm_setsSubmittedAtTimestamp() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        Instant before = Instant.now();
        FormDetailDto result = formService.submitForm(FORM_ID, EMPLOYEE_ID);
        Instant after = Instant.now();

        assertThat(result.getSubmittedAt()).isNotNull();
        assertThat(result.getSubmittedAt()).isBetween(before, after);
    }

    @Test
    void submitForm_whenAlreadySubmitted_throwsUnauthorized() {
        // checkEmployeeEditableStatus rejects SUBMITTED status before state machine is reached
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.submitForm(FORM_ID, EMPLOYEE_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void submitForm_whenNotOwner_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.submitForm(FORM_ID, OTHER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // getFormById — access control
    // -------------------------------------------------------------------------

    @Test
    void getFormById_employeeCanAccessOwnForm() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.getFormById(FORM_ID, EMPLOYEE_ID, Set.of("EMPLOYEE"));

        assertThat(result.getId()).isEqualTo(FORM_ID);
    }

    @Test
    void getFormById_employeeCannotAccessOtherEmployeeForm() {
        AppraisalForm form = buildForm(FormStatus.DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formService.getFormById(FORM_ID, OTHER_ID, Set.of("EMPLOYEE")))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void getFormById_managerCanAccessTeamForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.getFormById(FORM_ID, MANAGER_ID, Set.of("MANAGER"));

        assertThat(result.getId()).isEqualTo(FORM_ID);
    }

    @Test
    void getFormById_hrCanAccessAnyForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.getFormById(FORM_ID, OTHER_ID, Set.of("HR"));

        assertThat(result.getId()).isEqualTo(FORM_ID);
    }

    @Test
    void getFormById_adminCanAccessAnyForm() {
        AppraisalForm form = buildForm(FormStatus.REVIEWED_AND_COMPLETED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.getFormById(FORM_ID, OTHER_ID, Set.of("ADMIN"));

        assertThat(result.getId()).isEqualTo(FORM_ID);
    }

    @Test
    void getFormById_backupReviewerCanAccessForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(OTHER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.getFormById(FORM_ID, OTHER_ID, Set.of("MANAGER"));

        assertThat(result.getId()).isEqualTo(FORM_ID);
    }

    @Test
    void getFormById_throwsNotFoundWhenFormMissing() {
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.getFormById(FORM_ID, EMPLOYEE_ID, Set.of("EMPLOYEE")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // State machine — invalid transitions
    // -------------------------------------------------------------------------

    @Test
    void validateStateTransition_delegatesToStateMachine() {
        doThrow(new InvalidStateTransitionException("DRAFT_SAVED", "NOT_STARTED"))
                .when(formStateMachine).validateTransition(FormStatus.DRAFT_SAVED, FormStatus.NOT_STARTED);

        assertThatThrownBy(() ->
                formService.validateStateTransition(FormStatus.DRAFT_SAVED, FormStatus.NOT_STARTED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // FormData JSON round-trip (via FormDataConverter)
    // -------------------------------------------------------------------------

    @Test
    void formDataConverter_roundTrip_preservesAllFields() {
        FormDataConverter converter = new FormDataConverter();

        FormData original = new FormData();

        FormData.Header header = new FormData.Header();
        header.setDateOfHire("2020-01-15");
        header.setDateOfReview("2025-04-01");
        header.setReviewPeriod("2025-26");
        header.setTypeOfReview("Annual");
        original.setHeader(header);

        FormData.RatedItem item = new FormData.RatedItem();
        item.setItemId("kr_1");
        item.setSelfComment("Did well");
        item.setSelfRating(Rating.MEETS);
        item.setManagerComment("Agreed");
        item.setManagerRating(Rating.EXCEEDS);
        original.setKeyResponsibilities(List.of(item));

        original.setNextYearGoals("Grow leadership skills");

        FormData.OverallEvaluation eval = new FormData.OverallEvaluation();
        eval.setTeamMemberComments("Good year");
        eval.setManagerComments("Strong performer");
        original.setOverallEvaluation(eval);

        String json = converter.convertToDatabaseColumn(original);
        assertThat(json).isNotBlank();

        FormData restored = converter.convertToEntityAttribute(json);

        assertThat(restored.getHeader().getDateOfHire()).isEqualTo("2020-01-15");
        assertThat(restored.getHeader().getReviewPeriod()).isEqualTo("2025-26");
        assertThat(restored.getKeyResponsibilities()).hasSize(1);
        assertThat(restored.getKeyResponsibilities().get(0).getSelfRating()).isEqualTo(Rating.MEETS);
        assertThat(restored.getKeyResponsibilities().get(0).getManagerRating()).isEqualTo(Rating.EXCEEDS);
        assertThat(restored.getNextYearGoals()).isEqualTo("Grow leadership skills");
        assertThat(restored.getOverallEvaluation().getTeamMemberComments()).isEqualTo("Good year");
    }

    @Test
    void formDataConverter_nullInput_returnsNull() {
        FormDataConverter converter = new FormDataConverter();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    // -------------------------------------------------------------------------
    // isAuthorizedReviewer
    // -------------------------------------------------------------------------

    @Test
    void isAuthorizedReviewer_primaryManagerIsAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        assertThat(formService.isAuthorizedReviewer(form, MANAGER_ID)).isTrue();
    }

    @Test
    void isAuthorizedReviewer_backupReviewerIsAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(OTHER_ID);
        assertThat(formService.isAuthorizedReviewer(form, OTHER_ID)).isTrue();
    }

    @Test
    void isAuthorizedReviewer_unrelatedUserIsNotAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        assertThat(formService.isAuthorizedReviewer(form, OTHER_ID)).isFalse();
    }

    // -------------------------------------------------------------------------
    // saveReviewDraft (via FormService)
    // -------------------------------------------------------------------------

    @Test
    void saveReviewDraft_whenSubmitted_autoAdvancesToUnderReviewThenDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        SaveReviewDraftRequest req = new SaveReviewDraftRequest();
        req.setManagerComments("Looking good");

        FormDetailDto result = formService.saveReviewDraft(FORM_ID, req, MANAGER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
        verify(formStateMachine).validateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
        verify(formStateMachine).validateTransition(FormStatus.UNDER_REVIEW, FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void saveReviewDraft_whenUnderReview_transitionsToReviewDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        SaveReviewDraftRequest req = new SaveReviewDraftRequest();
        req.setManagerComments("Draft comments");

        FormDetailDto result = formService.saveReviewDraft(FORM_ID, req, MANAGER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void saveReviewDraft_whenNotManager_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() ->
                formService.saveReviewDraft(FORM_ID, new SaveReviewDraftRequest(), OTHER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // completeReview (via FormService)
    // -------------------------------------------------------------------------

    @Test
    void completeReview_whenUnderReview_transitionsToReviewedAndCompleted() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.completeReview(FORM_ID, new SaveReviewDraftRequest(), MANAGER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void completeReview_whenReviewDraftSaved_transitionsToReviewedAndCompleted() {
        AppraisalForm form = buildForm(FormStatus.REVIEW_DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        FormDetailDto result = formService.completeReview(FORM_ID, new SaveReviewDraftRequest(), MANAGER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void completeReview_setsReviewedAtTimestamp() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        Instant before = Instant.now();
        FormDetailDto result = formService.completeReview(FORM_ID, new SaveReviewDraftRequest(), MANAGER_ID);
        Instant after = Instant.now();

        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getReviewedAt()).isBetween(before, after);
    }
}
