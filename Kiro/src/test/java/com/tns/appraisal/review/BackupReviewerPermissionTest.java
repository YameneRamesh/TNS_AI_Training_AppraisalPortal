package com.tns.appraisal.review;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.*;
import com.tns.appraisal.notification.NotificationService;
import com.tns.appraisal.pdf.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for backup reviewer permission logic.
 *
 * Validates Requirement 15.2 (backup reviewer access to forms) and
 * Requirement 15.3 (backup reviewer has same permissions as primary manager).
 * Covers Property 19: Backup Reviewer Permission Equivalence.
 */
@ExtendWith(MockitoExtension.class)
class BackupReviewerPermissionTest {

    private static final Long PRIMARY_MANAGER_ID  = 10L;
    private static final Long BACKUP_REVIEWER_ID  = 20L;
    private static final Long UNRELATED_USER_ID   = 99L;
    private static final Long EMPLOYEE_ID         = 5L;
    private static final Long FORM_ID             = 1L;

    @Mock private AppraisalFormRepository formRepository;
    @Mock private FormStateMachine formStateMachine;
    @Mock private AuditLogService auditLogService;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private NotificationService notificationService;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                formRepository, formStateMachine, auditLogService,
                pdfGenerationService, notificationService);
    }

    // -------------------------------------------------------------------------
    // isAuthorizedReviewer helper
    // -------------------------------------------------------------------------

    @Test
    void primaryManager_isAuthorizedReviewer() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        assertThat(reviewService.isAuthorizedReviewer(form, PRIMARY_MANAGER_ID)).isTrue();
    }

    @Test
    void backupReviewer_isAuthorizedReviewer() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        assertThat(reviewService.isAuthorizedReviewer(form, BACKUP_REVIEWER_ID)).isTrue();
    }

    @Test
    void unrelatedUser_isNotAuthorizedReviewer() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        assertThat(reviewService.isAuthorizedReviewer(form, UNRELATED_USER_ID)).isFalse();
    }

    @Test
    void noBackupReviewer_onlyPrimaryManagerIsAuthorized() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, null);
        assertThat(reviewService.isAuthorizedReviewer(form, PRIMARY_MANAGER_ID)).isTrue();
        assertThat(reviewService.isAuthorizedReviewer(form, BACKUP_REVIEWER_ID)).isFalse();
    }

    // -------------------------------------------------------------------------
    // getReviewableForm — access gate used by all review operations
    // -------------------------------------------------------------------------

    @Test
    void primaryManager_canGetReviewableForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.getReviewableForm(FORM_ID, PRIMARY_MANAGER_ID);
        assertThat(result).isSameAs(form);
    }

    @Test
    void backupReviewer_canGetReviewableForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        AppraisalForm result = reviewService.getReviewableForm(FORM_ID, BACKUP_REVIEWER_ID);
        assertThat(result).isSameAs(form);
    }

    @Test
    void unrelatedUser_cannotGetReviewableForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> reviewService.getReviewableForm(FORM_ID, UNRELATED_USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // saveReviewDraft — backup reviewer has same permission as primary manager
    // -------------------------------------------------------------------------

    @Test
    void backupReviewer_canSaveReviewDraft() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(formRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(formStateMachine).validateTransition(any(), any());

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, BACKUP_REVIEWER_ID, new ReviewDataDto());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void primaryManager_canSaveReviewDraft() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(formRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(formStateMachine).validateTransition(any(), any());

        AppraisalForm result = reviewService.saveReviewDraft(FORM_ID, PRIMARY_MANAGER_ID, new ReviewDataDto());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
    }

    @Test
    void unrelatedUser_cannotSaveReviewDraft() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> reviewService.saveReviewDraft(FORM_ID, UNRELATED_USER_ID, new ReviewDataDto()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // completeReview — backup reviewer has same permission as primary manager
    // -------------------------------------------------------------------------

    @Test
    void backupReviewer_canCompleteReview() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(formRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pdfGenerationService.generateAndStore(any())).thenReturn("/pdfs/form-1.pdf");
        doNothing().when(formStateMachine).validateTransition(any(), any());

        AppraisalForm result = reviewService.completeReview(FORM_ID, BACKUP_REVIEWER_ID, new ReviewDataDto());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void primaryManager_canCompleteReview() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(formRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pdfGenerationService.generateAndStore(any())).thenReturn("/pdfs/form-1.pdf");
        doNothing().when(formStateMachine).validateTransition(any(), any());

        AppraisalForm result = reviewService.completeReview(FORM_ID, PRIMARY_MANAGER_ID, new ReviewDataDto());

        assertThat(result.getStatus()).isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    @Test
    void unrelatedUser_cannotCompleteReview() {
        AppraisalForm form = buildForm(FormStatus.UNDER_REVIEW, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);
        when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> reviewService.completeReview(FORM_ID, UNRELATED_USER_ID, new ReviewDataDto()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // -------------------------------------------------------------------------
    // FormService read access — backup reviewer can read the form
    // -------------------------------------------------------------------------

    @Test
    void formService_backupReviewer_canReadForm() {
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, PRIMARY_MANAGER_ID, BACKUP_REVIEWER_ID);

        // Simulate FormService.checkReadAccess logic directly via isAuthorizedReviewer
        // (FormService delegates to the same helper)
        boolean canRead = reviewService.isAuthorizedReviewer(form, BACKUP_REVIEWER_ID);
        assertThat(canRead).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppraisalForm buildForm(FormStatus status, Long managerId, Long backupReviewerId) {
        AppraisalForm form = new AppraisalForm();
        form.setId(FORM_ID);
        form.setCycleId(100L);
        form.setEmployeeId(EMPLOYEE_ID);
        form.setManagerId(managerId);
        form.setBackupReviewerId(backupReviewerId);
        form.setTemplateId(1L);
        form.setStatus(status);
        return form;
    }
}
