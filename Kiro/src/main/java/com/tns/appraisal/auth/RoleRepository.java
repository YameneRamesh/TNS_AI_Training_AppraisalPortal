package com.tns.appraisal.auth;

import com.tns.appraisal.common.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, Integer> {

    /**
     * Find a role by its name.
     *
     * @param name the role name (e.g., "EMPLOYEE", "MANAGER", "HR", "ADMIN")
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists by name.
     *
     * @param name the role name
     * @return true if the role exists, false otherwise
     */
    boolean existsByName(String name);
}
