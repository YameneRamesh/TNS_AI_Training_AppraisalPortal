package com.tns.appraisal.review;

import com.tns.appraisal.audit.AuditLogService;
import com.tns.appraisal.exception.UnauthorizedAccessException;
import com.tns.appraisal.form.*;
import com.tns.appraisal.notification.NotificationService;
import com.tns.appraisal.pdf.PdfGenerationService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;

import java.util.List;

/**
 * Property-Based Test: Backup Reviewer Permission Equivalence (Property 19)
 *
 * <p><b>Validates: Requirements 15.2, 15.3</b>
 *
 * <p>For any backup reviewer assignment, the backup reviewer SHALL have exactly the same
 * review permissions as the primary manager for the assigned appraisal forms — able to
 * perform all review actions (save draft, complete review, open review) that the primary
 * manager can perform.
 *
 * <p>Also verifies the negative case: a user who is neither the primary manager nor the
 * backup reviewer is denied all review actions.
 */
class BackupReviewerPermissionPropertyTest {

    /** Statuses where a reviewer can initiate or continue a review. */
    private static final List<FormStatus> REVIEWABLE_STATUSES = List.of(
        FormStatus.SUBMITTED,
        FormStatus.UNDER_REVIEW,
        FormStatus.REVIEW_DRAFT_SAVED
    );

    /** Statuses where startReview (SUBMITTED → UNDER_REVIEW) is the entry point. */
    private static final List<FormStatus> START_REVIEW_STATUS = List.of(
        FormStatus.SUBMITTED
    );

    /** Statuses where saveReviewDraft is valid. */
    private static final List<FormStatus> SAVE_DRAFT_STATUSES = List.of(
        FormStatus.SUBMITTED,
        FormStatus.UNDER_REVIEW,
        FormStatus.REVIEW_DRAFT_SAVED
    );

    /** Statuses where completeReview is valid. */
    private static final List<FormStatus> COMPLETE_REVIEW_STATUSES = List.of(
        FormStatus.SUBMITTED,
        FormStatus.UNDER_REVIEW,
        FormStatus.REVIEW_DRAFT_SAVED
    );

    private static final Long FORM_ID = 1L;

    private AppraisalFormRepository formRepository;
    private FormStateMachine        formStateMachine;
    private AuditLogService         auditLogService;
    private PdfGenerationService    pdfGenerationService;
    private NotificationService     notificationService;
    private ReviewService           reviewService;

    @BeforeProperty
    void setUp() {
        formRepository      = Mockito.mock(AppraisalFormRepository.class);
        formStateMachine    = Mockito.mock(FormStateMachine.class);
        auditLogService     = Mockito.mock(AuditLogService.class);
        pdfGenerationService = Mockito.mock(PdfGenerationService.class);
        notificationService = Mockito.mock(NotificationService.class);

        reviewService = new ReviewService(
            formRepository, formStateMachine, auditLogService,
            pdfGenerationService, notificationService);

        // Default: state machine never throws (structural transitions are valid)
        Mockito.doNothing().when(formStateMachine).validateTransition(Mockito.any(), Mockito.any());
        // Default: PDF generation returns a path
        Mockito.when(pdfGenerationService.generateAndStore(Mockito.any())).thenReturn("/pdfs/form-1.pdf");
        // Default: save returns the argument
        Mockito.when(formRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        // Default: audit log is a no-op
        Mockito.doNothing().when(auditLogService).logAsync(
            Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any());
    }

    // -------------------------------------------------------------------------
    // Property 1: isAuthorizedReviewer — primary manager is always authorized
    // -------------------------------------------------------------------------

    /**
     * For any (managerId, backupReviewerId) pair, the primary manager is always authorized.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 200)
    void primaryManager_isAlwaysAuthorizedReviewer(
            @ForAll("managerIds")        Long managerId,
            @ForAll("backupReviewerIds") Long backupReviewerId) {

        AppraisalForm form = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);

        Assertions.assertThat(reviewService.isAuthorizedReviewer(form, managerId))
            .as("Primary manager %d must always be authorized", managerId)
            .isTrue();
    }

    // -------------------------------------------------------------------------
    // Property 2: isAuthorizedReviewer — backup reviewer is always authorized
    // -------------------------------------------------------------------------

    /**
     * For any (managerId, backupReviewerId) pair where backup != manager,
     * the backup reviewer is always authorized.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 200)
    void backupReviewer_isAlwaysAuthorizedReviewer(
            @ForAll("distinctManagerAndBackup") long[] ids) {

        Long managerId       = ids[0];
        Long backupReviewerId = ids[1];
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);

        Assertions.assertThat(reviewService.isAuthorizedReviewer(form, backupReviewerId))
            .as("Backup reviewer %d must always be authorized", backupReviewerId)
            .isTrue();
    }

    // -------------------------------------------------------------------------
    // Property 3: isAuthorizedReviewer — unrelated user is never authorized
    // -------------------------------------------------------------------------

    /**
     * A user who is neither the primary manager nor the backup reviewer is never authorized.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 200)
    void unrelatedUser_isNeverAuthorizedReviewer(
            @ForAll("threeDistinctIds") long[] ids) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        Long unrelatedUserId  = ids[2];
        AppraisalForm form = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);

        Assertions.assertThat(reviewService.isAuthorizedReviewer(form, unrelatedUserId))
            .as("Unrelated user %d must never be authorized", unrelatedUserId)
            .isFalse();
    }

    // -------------------------------------------------------------------------
    // Property 4: startReview — backup reviewer has same permission as primary manager
    // -------------------------------------------------------------------------

    /**
     * For any form in SUBMITTED status, both the primary manager and the backup reviewer
     * can call startReview without an exception.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 100)
    void startReview_backupReviewerEquivalentToPrimaryManager(
            @ForAll("distinctManagerAndBackup") long[] ids) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];

        // Primary manager can start review
        AppraisalForm formForManager = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForManager));

        Assertions.assertThatCode(() -> reviewService.startReview(FORM_ID, managerId))
            .as("Primary manager must be able to start review")
            .doesNotThrowAnyException();

        // Backup reviewer can start review
        AppraisalForm formForBackup = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForBackup));

        Assertions.assertThatCode(() -> reviewService.startReview(FORM_ID, backupReviewerId))
            .as("Backup reviewer must be able to start review (same as primary manager)")
            .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Property 5: saveReviewDraft — backup reviewer has same permission as primary manager
    // -------------------------------------------------------------------------

    /**
     * For any form in a reviewable status, both the primary manager and the backup reviewer
     * can call saveReviewDraft without an exception, and the resulting status is REVIEW_DRAFT_SAVED.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 100)
    void saveReviewDraft_backupReviewerEquivalentToPrimaryManager(
            @ForAll("distinctManagerAndBackup") long[] ids,
            @ForAll("saveDraftStatuses")        FormStatus status) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        ReviewDataDto reviewData = new ReviewDataDto();

        // Primary manager can save draft
        AppraisalForm formForManager = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForManager));

        Assertions.assertThatCode(() -> reviewService.saveReviewDraft(FORM_ID, managerId, reviewData))
            .as("Primary manager must be able to save review draft in status %s", status)
            .doesNotThrowAnyException();

        // Backup reviewer can save draft
        AppraisalForm formForBackup = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForBackup));

        Assertions.assertThatCode(() -> reviewService.saveReviewDraft(FORM_ID, backupReviewerId, reviewData))
            .as("Backup reviewer must be able to save review draft in status %s (same as primary manager)", status)
            .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Property 6: saveReviewDraft result — status transitions to REVIEW_DRAFT_SAVED
    // -------------------------------------------------------------------------

    /**
     * After saveReviewDraft, the form status is REVIEW_DRAFT_SAVED for both the primary
     * manager and the backup reviewer.
     *
     * <p><b>Validates: Requirements 15.3</b>
     */
    @Property(tries = 100)
    void saveReviewDraft_resultStatusIsReviewDraftSaved(
            @ForAll("distinctManagerAndBackup") long[] ids,
            @ForAll("saveDraftStatuses")        FormStatus status) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        ReviewDataDto reviewData = new ReviewDataDto();

        // Primary manager result
        AppraisalForm formForManager = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForManager));
        AppraisalForm managerResult = reviewService.saveReviewDraft(FORM_ID, managerId, reviewData);

        Assertions.assertThat(managerResult.getStatus())
            .as("After saveReviewDraft by primary manager, status must be REVIEW_DRAFT_SAVED")
            .isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);

        // Backup reviewer result
        AppraisalForm formForBackup = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForBackup));
        AppraisalForm backupResult = reviewService.saveReviewDraft(FORM_ID, backupReviewerId, reviewData);

        Assertions.assertThat(backupResult.getStatus())
            .as("After saveReviewDraft by backup reviewer, status must be REVIEW_DRAFT_SAVED (same as primary manager)")
            .isEqualTo(FormStatus.REVIEW_DRAFT_SAVED);
    }

    // -------------------------------------------------------------------------
    // Property 7: completeReview — backup reviewer has same permission as primary manager
    // -------------------------------------------------------------------------

    /**
     * For any form in a reviewable status, both the primary manager and the backup reviewer
     * can call completeReview without an exception, and the resulting status is REVIEWED_AND_COMPLETED.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 100)
    void completeReview_backupReviewerEquivalentToPrimaryManager(
            @ForAll("distinctManagerAndBackup") long[] ids,
            @ForAll("completeReviewStatuses")   FormStatus status) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        ReviewDataDto reviewData = new ReviewDataDto();

        // Primary manager can complete review
        AppraisalForm formForManager = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForManager));

        Assertions.assertThatCode(() -> reviewService.completeReview(FORM_ID, managerId, reviewData))
            .as("Primary manager must be able to complete review in status %s", status)
            .doesNotThrowAnyException();

        // Backup reviewer can complete review
        AppraisalForm formForBackup = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForBackup));

        Assertions.assertThatCode(() -> reviewService.completeReview(FORM_ID, backupReviewerId, reviewData))
            .as("Backup reviewer must be able to complete review in status %s (same as primary manager)", status)
            .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Property 8: completeReview result — status transitions to REVIEWED_AND_COMPLETED
    // -------------------------------------------------------------------------

    /**
     * After completeReview, the form status is REVIEWED_AND_COMPLETED for both the primary
     * manager and the backup reviewer.
     *
     * <p><b>Validates: Requirements 15.3</b>
     */
    @Property(tries = 100)
    void completeReview_resultStatusIsReviewedAndCompleted(
            @ForAll("distinctManagerAndBackup") long[] ids,
            @ForAll("completeReviewStatuses")   FormStatus status) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        ReviewDataDto reviewData = new ReviewDataDto();

        // Primary manager result
        AppraisalForm formForManager = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForManager));
        AppraisalForm managerResult = reviewService.completeReview(FORM_ID, managerId, reviewData);

        Assertions.assertThat(managerResult.getStatus())
            .as("After completeReview by primary manager, status must be REVIEWED_AND_COMPLETED")
            .isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);

        // Backup reviewer result
        AppraisalForm formForBackup = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(formForBackup));
        AppraisalForm backupResult = reviewService.completeReview(FORM_ID, backupReviewerId, reviewData);

        Assertions.assertThat(backupResult.getStatus())
            .as("After completeReview by backup reviewer, status must be REVIEWED_AND_COMPLETED (same as primary manager)")
            .isEqualTo(FormStatus.REVIEWED_AND_COMPLETED);
    }

    // -------------------------------------------------------------------------
    // Property 9: unrelated user is denied all review actions (negative case)
    // -------------------------------------------------------------------------

    /**
     * A user who is neither the primary manager nor the backup reviewer is denied
     * all review actions: startReview, saveReviewDraft, and completeReview.
     *
     * <p><b>Validates: Requirements 15.2, 15.3</b>
     */
    @Property(tries = 100)
    void unrelatedUser_isDeniedAllReviewActions(
            @ForAll("threeDistinctIds")       long[] ids,
            @ForAll("reviewableStatuses")     FormStatus status) {

        Long managerId        = ids[0];
        Long backupReviewerId = ids[1];
        Long unrelatedUserId  = ids[2];
        ReviewDataDto reviewData = new ReviewDataDto();

        // startReview denied
        AppraisalForm form1 = buildForm(FormStatus.SUBMITTED, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(form1));
        Assertions.assertThatThrownBy(() -> reviewService.startReview(FORM_ID, unrelatedUserId))
            .as("Unrelated user must be denied startReview")
            .isInstanceOf(UnauthorizedAccessException.class);

        // saveReviewDraft denied
        AppraisalForm form2 = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(form2));
        Assertions.assertThatThrownBy(() -> reviewService.saveReviewDraft(FORM_ID, unrelatedUserId, reviewData))
            .as("Unrelated user must be denied saveReviewDraft in status %s", status)
            .isInstanceOf(UnauthorizedAccessException.class);

        // completeReview denied
        AppraisalForm form3 = buildForm(status, managerId, backupReviewerId);
        Mockito.when(formRepository.findById(FORM_ID)).thenReturn(java.util.Optional.of(form3));
        Assertions.assertThatThrownBy(() -> reviewService.completeReview(FORM_ID, unrelatedUserId, reviewData))
            .as("Unrelated user must be denied completeReview in status %s", status)
            .isInstanceOf(UnauthorizedAccessException.class);
    }

    // =========================================================================
    // Arbitraries (providers)
    // =========================================================================

    /** Generates arbitrary manager IDs in range [1, 500]. */
    @Provide
    Arbitrary<Long> managerIds() {
        return Arbitraries.longs().between(1L, 500L);
    }

    /** Generates arbitrary backup reviewer IDs in range [501, 1000]. */
    @Provide
    Arbitrary<Long> backupReviewerIds() {
        return Arbitraries.longs().between(501L, 1000L);
    }

    /**
     * Generates a pair [managerId, backupReviewerId] guaranteed to be distinct.
     * managerId in [1, 500], backupReviewerId in [501, 1000].
     */
    @Provide
    Arbitrary<long[]> distinctManagerAndBackup() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 500L),
            Arbitraries.longs().between(501L, 1000L)
        ).as((m, b) -> new long[]{m, b});
    }

    /**
     * Generates a triple [managerId, backupReviewerId, unrelatedUserId] all distinct.
     * managerId in [1, 500], backupReviewerId in [501, 1000], unrelatedUserId in [1001, 2000].
     */
    @Provide
    Arbitrary<long[]> threeDistinctIds() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 500L),
            Arbitraries.longs().between(501L, 1000L),
            Arbitraries.longs().between(1001L, 2000L)
        ).as((m, b, u) -> new long[]{m, b, u});
    }

    @Provide
    Arbitrary<FormStatus> reviewableStatuses() {
        return Arbitraries.of(REVIEWABLE_STATUSES);
    }

    @Provide
    Arbitrary<FormStatus> saveDraftStatuses() {
        return Arbitraries.of(SAVE_DRAFT_STATUSES);
    }

    @Provide
    Arbitrary<FormStatus> completeReviewStatuses() {
        return Arbitraries.of(COMPLETE_REVIEW_STATUSES);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AppraisalForm buildForm(FormStatus status, Long managerId, Long backupReviewerId) {
        AppraisalForm form = new AppraisalForm();
        form.setId(FORM_ID);
        form.setCycleId(100L);
        form.setEmployeeId(5L);
        form.setManagerId(managerId);
        form.setBackupReviewerId(backupReviewerId);
        form.setTemplateId(1L);
        form.setStatus(status);
        return form;
    }
}
