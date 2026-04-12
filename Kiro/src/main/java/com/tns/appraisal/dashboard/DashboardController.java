package com.tns.appraisal.dashboard;

import com.tns.appraisal.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for role-specific dashboard data.
 * Returns stub data until full dashboard aggregation is implemented in Phase 4.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/hr")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHRDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("activeCycle", null);
        data.put("eligibleEmployees", 0);
        data.put("pendingSubmissions", 0);
        data.put("pendingReviews", 0);
        data.put("completedAppraisals", 0);
        data.put("departmentProgress", Collections.emptyList());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/employee")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmployeeDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("currentForm", null);
        data.put("historicalForms", Collections.emptyList());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getManagerDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("ownForm", null);
        data.put("teamForms", Collections.emptyList());
        data.put("pendingReviews", 0);
        data.put("completedReviews", 0);
        data.put("completionPercentage", 0);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
