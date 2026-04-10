package com.tns.appraisal.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending asynchronous email notifications and logging them to email_notification_log.
 *
 * <p>Full implementation is covered in Phase 4 (task 4.1.3). This stub provides the interface
 * that ReviewService depends on so the review workflow compiles and functions correctly.</p>
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Sends review-completion notifications to the employee, manager, and HR.
     * Exactly 3 email log entries are created (Property 13).
     *
     * @param formId     the completed appraisal form ID
     * @param employeeId the employee's user ID
     * @param managerId  the manager's user ID
     * @param pdfPath    storage path of the generated PDF attachment (may be null if generation failed)
     */
    @Async
    public void sendReviewCompletionNotifications(Long formId, Long employeeId, Long managerId, String pdfPath) {
        logger.info("Sending review-completion notifications for form {} (employee={}, manager={})",
                formId, employeeId, managerId);
        // TODO (task 4.1.3): resolve recipient emails, render templates, send via JavaMailSender,
        //  and persist 3 EmailNotificationLog records (employee, manager, HR).
    }
}
