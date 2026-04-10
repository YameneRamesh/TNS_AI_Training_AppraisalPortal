package com.tns.appraisal.auth;

import com.tns.appraisal.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for role lookup operations.
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Get all available roles.
     *
     * GET /api/roles
     *
     * @return list of roles with id and name
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllRoles() {
        List<Map<String, Object>> roles = roleRepository.findAll().stream()
            .map(role -> Map.<String, Object>of("id", role.getId(), "name", role.getName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
