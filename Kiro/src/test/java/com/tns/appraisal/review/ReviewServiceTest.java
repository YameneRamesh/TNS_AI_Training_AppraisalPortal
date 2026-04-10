package com.tns.appraisal.review;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.InvalidStateTransitionException;
import com.tns.appraisal.exception.ResourceNotFoundException;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.*;
import com.tns.appraisal.notification.NotificationService;
import com.tns.appraisal.pdf.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock
    private AppraisalFormRepository formRepository;

    @Mock
    private FormStateMachine formStateMachine;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long MANAGER_ID        = 2L;
    private static final Long BACKUP_REVIEWER_ID = 3L;
    private static final Long EMPLOYEE_ID        = 1L;
    private static final Long UNRELATED_USER_ID  = 99L;
    private static final Long FORM_ID            = 10L;
    private static final String PDF_PATH         = "pdfs/form_10.pdf";

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

    private ReviewDataDto buildReviewData() {
        ReviewDataDto dto = new ReviewDataDto();
        dto.setManagerComments("Good work overall");
        return dto;
    }

    @BeforeEach
    void setUp() {
        when(formRepository.save(any(AppraisalForm.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // saveReviewDraft
    // -------------------------------------------------------------------------

    @Test
    void saveReviewDraft_whenSubmitted_autoAdvancesToUnderReviewThenDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
        verify(formStateMachine).validateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
        verify(formStateMachine).validateTransition(FormStatus.UNDER_REVIEW, FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void saveReviewDraft_whenUnderReview_transitionsToReviewDraftSaved() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
        verify(formStateMachine).validateTransition(FormStatus.UNDER_REVIEW, FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void saveReviewDraft_whenAlreadyReviewDraftSaved_idempotentReSave() {
        AppraisalForm form = buildForm(FormStatus.REVIEW_DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
        // State machine NOT called for idempotent re-save
        verify(formStateMachine, never()).validateTransition(FormStatus.REVIEW_DRAFT_SAVED, FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void saveReviewDraft_whenNotManagerOrBackup_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() ->
                reviewService.saveReviewDraft(FORM_ID, UNRELATED_USER_ID, buildReviewData()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveReviewDraft_whenFormNotFound_throwsNotFound() {
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, buildReviewData()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveReviewDraft_whenFormReviewedAndCompleted_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.REVIEWED_AND_COMPLETED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() ->
                reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, buildReviewData()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void saveReviewDraft_persistsManagerComments() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        ReviewDataDto data = buildReviewData();
        data.setManagerComments("Excellent contribution");

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, MANAGER_ID, data);

        assertThat(result.getFormData()).isNotNull();
        assertThat(result.getFormData().getOverallEvaluation().getManagerComments())
                .isEqualTo("Excellent contribution");
    }

    // -------------------------------------------------------------------------
    // completeReview
    // -------------------------------------------------------------------------

    @Test
    void completeReview_whenUnderReview_transitionsToReviewedAndCompleted() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        AppraisalForm result = reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void completeReview_whenReviewDraftSaved_transitionsToReviewedAndCompleted() {
        AppraisalForm form = buildForm(FormStatus.REVIEW_DRAFT_SAVED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        AppraisalForm result = reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void completeReview_setsReviewedAtTimestamp() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        Instant before = Instant.now();
        AppraisalForm result = reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());
        Instant after = Instant.now();

        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getReviewedAt()).isBetween(before, after);
    }

    @Test
    void completeReview_triggersPdfGeneration() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());

        verify(pdfGenerationService).generateAndStore(any(AppraisalForm.class));
    }

    @Test
    void completeReview_storesPdfPathOnForm() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        AppraisalForm result = reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());

        assertThat(result.getPdfStoragePath()).isEqualTo(PDF_PATH);
    }

    @Test
    void completeReview_triggersNotificationService() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData());

        verify(notificationService).sendReviewCompletionNotifications(
                eq(FORM_ID), eq(EMPLOYEE_ID), eq(MANAGER_ID), eq(PDF_PATH));
    }

    @Test
    void completeReview_whenNotManagerOrBackup_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() ->
                reviewService.completeReview(FORM_ID, UNRELATED_USER_ID, buildReviewData()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void completeReview_whenFormNotFound_throwsNotFound() {
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.completeReview(FORM_ID, MANAGER_ID, buildReviewData()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Backup reviewer — same permissions as primary manager (Property 19)
    // -------------------------------------------------------------------------

    @Test
    void backupReviewer_canSaveReviewDraft() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, BACKUP_REVIEWER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void backupReviewer_canCompleteReview() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW);
        form.setBackupReviewerId(BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(pdfGenerationService.generateAndStore(any())).thenReturn(PDF_PATH);

        AppraisalForm result = reviewService.completeReview(FORM_ID, BACKUP_REVIEWER_ID, buildReviewData());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void backupReviewer_canStartReview() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.startReview(FORM_ID, BACKUP_REVIEWER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.UNDER_REVIEW);
    }

    // -------------------------------------------------------------------------
    // isAuthorizedReviewer
    // -------------------------------------------------------------------------

    @Test
    void isAuthorizedReviewer_primaryManagerIsAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        assertThat(reviewService.isAuthorizedReviewer(form, MANAGER_ID)).isTrue();
    }

    @Test
    void isAuthorizedReviewer_backupReviewerIsAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(BACKUP_REVIEWER_ID);
        assertThat(reviewService.isAuthorizedReviewer(form, BACKUP_REVIEWER_ID)).isTrue();
    }

    @Test
    void isAuthorizedReviewer_unrelatedUserIsNotAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        assertThat(reviewService.isAuthorizedReviewer(form, UNRELATED_USER_ID)).isFalse();
    }

    @Test
    void isAuthorizedReviewer_noBackupReviewerSet_unrelatedUserNotAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        form.setBackupReviewerId(null);
        assertThat(reviewService.isAuthorizedReviewer(form, BACKUP_REVIEWER_ID)).isFalse();
    }

    // -------------------------------------------------------------------------
    // startReview
    // -------------------------------------------------------------------------

    @Test
    void startReview_whenSubmitted_transitionsToUnderReview() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.startReview(FORM_ID, MANAGER_ID);

        assertThat(result.getStatus()).isEqualTo(FormStatus.UNDER_REVIEW);
        assertThat(result.getReviewStartedAt()).isNotNull();
        verify(formStateMachine).validateTransition(FormStatus.SUBMITTED, FormStatus.UNDER_REVIEW);
    }

    @Test
    void startReview_whenNotManager_throwsUnauthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> reviewService.startReview(FORM_ID, UNRELATED_USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}
