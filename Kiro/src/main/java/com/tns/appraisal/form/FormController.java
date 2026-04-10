package com.tns.appraisal.form;

import com.tns.appraisal.common.dto.ApiResponse;
import com.tns.appraisal.form.dto.FormDetailDto;
import com.tns.appraisal.form.dto.FormSummaryDto;
import com.tns.appraisal.form.dto.SaveDraftRequest;
import com.tns.appraisal.form.dto.SaveReviewDraftRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing all /api/forms endpoints for the appraisal form lifecycle.
 *
 * <p>Access control summary:</p>
 * <ul>
 *   <li>GET  /api/forms/my              – EMPLOYEE</li>
 *   <li>GET  /api/forms/{id}            – Employee (own), Manager (team), HR, Admin</li>
 *   <li>PUT  /api/forms/{id}/draft      – EMPLOYEE (own form only)</li>
 *   <li>POST /api/forms/{id}/submit     – EMPLOYEE (own form only)</li>
 *   <li>PUT  /api/forms/{id}/review/draft    – MANAGER (assigned forms only, delegates to ReviewController)</li>
 *   <li>GET  /api/forms/{id}/pdf        – Employee (own), Manager, HR</li>
 *   <li>GET  /api/forms/history         – Employee (own), Manager (team), HR</li>
 * </ul>
 * <p>Note: POST /api/forms/{id}/review/complete is handled by ReviewController.</p>
 */
@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    // -------------------------------------------------------------------------
    // GET /api/forms/my  – Employee: get own active form
    // NOTE: must be declared before /{id} to avoid path ambiguity
    // -------------------------------------------------------------------------

    /**
     * Returns the current user's active appraisal form.
     * Accessible by EMPLOYEE role only.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<FormDetailDto>> getMyForm(Authentication auth) {
        Long userId = getUserId(auth);
        FormDetailDto dto = formService.getMyForm(userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // -------------------------------------------------------------------------
    // GET /api/forms/history  – Employee (own), Manager (team), HR
    // NOTE: must be declared before /{id} to avoid path ambiguity
    // -------------------------------------------------------------------------

    /**
     * Returns historical appraisal forms for the current user.
     * Employees see only their own; Managers see their team's; HR sees all.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FormSummaryDto>>> getFormHistory(Authentication auth) {
        Long userId = getUserId(auth);
        Set<String> roles = getRoles(auth);
        List<FormSummaryDto> history = formService.getFormHistory(userId, roles);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // -------------------------------------------------------------------------
    // GET /api/forms/{id}  – Employee (own), Manager (team), HR, Admin
    // -------------------------------------------------------------------------

    /**
     * Returns a specific appraisal form by ID.
     * Access is enforced inside FormService based on role and ownership.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<FormDetailDto>> getFormById(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = getUserId(auth);
        Set<String> roles = getRoles(auth);
        FormDetailDto dto = formService.getFormById(id, userId, roles);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // -------------------------------------------------------------------------
    // PUT /api/forms/{id}/draft  – EMPLOYEE: save self-appraisal draft
    // -------------------------------------------------------------------------

    /**
     * Saves the employee's self-appraisal as a draft.
     * Transitions status NOT_STARTED → DRAFT_SAVED on first save.
     */
    @PutMapping("/{id}/draft")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<FormDetailDto>> saveDraft(
            @PathVariable Long id,
            @RequestBody SaveDraftRequest request,
            Authentication auth) {
        Long userId = getUserId(auth);
        FormDetailDto dto = formService.saveDraft(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Draft saved successfully", dto));
    }

    // -------------------------------------------------------------------------
    // POST /api/forms/{id}/submit  – EMPLOYEE: submit self-appraisal
    // -------------------------------------------------------------------------

    /**
     * Submits the employee's self-appraisal.
     * Transitions NOT_STARTED or DRAFT_SAVED → SUBMITTED.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<FormDetailDto>> submitForm(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = getUserId(auth);
        FormDetailDto dto = formService.submitForm(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Form submitted successfully", dto));
    }

    // -------------------------------------------------------------------------
    // PUT /api/forms/{id}/review/draft  – MANAGER: save review draft
    // -------------------------------------------------------------------------

    /**
     * Saves the manager's review as a draft.
     * Transitions SUBMITTED → UNDER_REVIEW on first save, then → REVIEW_DRAFT_SAVED.
     */
    @PutMapping("/{id}/review/draft")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FormDetailDto>> saveReviewDraft(
            @PathVariable Long id,
            @RequestBody SaveReviewDraftRequest request,
            Authentication auth) {
        Long userId = getUserId(auth);
        FormDetailDto dto = formService.saveReviewDraft(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Review draft saved successfully", dto));
    }

    // -------------------------------------------------------------------------
    // GET /api/forms/{id}/pdf  – Employee (own), Manager, HR
    // -------------------------------------------------------------------------

    /**
     * Downloads the PDF for a completed appraisal form.
     * Returns 404 if the PDF has not been generated yet.
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = getUserId(auth);
        Set<String> roles = getRoles(auth);

        // Verify access — reuse getFormById which enforces ownership/role checks
        FormDetailDto dto = formService.getFormById(id, userId, roles);

        String pdfPath = dto.getPdfStoragePath();
        if (pdfPath == null || pdfPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // PDF serving: return the stored path as a redirect or serve bytes.
        // For now, return 204 No Content with the path header until PdfGenerationService
        // is wired in (task 3.x). The path is available for the frontend to use.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
            "appraisal-form-" + id + ".pdf");
        headers.add("X-PDF-Path", pdfPath);

        return ResponseEntity.noContent().headers(headers).build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the numeric user ID from the Spring Security principal name.
     * Assumes the principal name is the user's database ID (set during login).
     */
    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    /**
     * Extracts role names (without the ROLE_ prefix) from the authentication object.
     */
    private Set<String> getRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
            .collect(Collectors.toSet());
    }
}
