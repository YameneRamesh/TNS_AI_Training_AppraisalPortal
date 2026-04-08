/**
 * PDF generation module.
 * 
 * <p>This package contains components for generating PDF exports of completed
 * appraisal forms. PDFs are generated using iText 7 and match the TnS Appraisal
 * Form V3.0 layout.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>PdfGenerationService - PDF creation from form data</li>
 *   <li>PDF template rendering matching TnS form layout</li>
 *   <li>PDF storage and retrieval operations</li>
 * </ul>
 * 
 * <p>PDF generation is triggered automatically when a manager completes a review,
 * and the generated PDF is attached to notification emails and stored for historical access.</p>
 */
package com.tns.appraisal.pdf;
