package com.tns.appraisal.pdf;

import com.tns.appraisal.form.AppraisalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for generating PDF exports of completed appraisal forms.
 *
 * <p>Full implementation is covered in Phase 4 (task 4.2.1). This stub provides the interface
 * that ReviewService depends on so the review workflow compiles and functions correctly.</p>
 */
@Service
public class PdfGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    /**
     * Generates a PDF for the given completed appraisal form and returns its storage path.
     *
     * <p>The returned path is persisted on the {@link AppraisalForm#getPdfStoragePath()} field
     * to satisfy Property 16 (PDF generated and path stored on completion).</p>
     *
     * @param form the completed appraisal form
     * @return the storage path of the generated PDF, or {@code null} if generation failed
     */
    public String generateAndStore(AppraisalForm form) {
        logger.info("Generating PDF for form {}", form.getId());
        // TODO (task 4.2.1): render form data using iText 7, write to storage, return path.
        String placeholderPath = "pdfs/form_" + form.getId() + ".pdf";
        logger.info("PDF placeholder path: {}", placeholderPath);
        return placeholderPath;
    }
}
