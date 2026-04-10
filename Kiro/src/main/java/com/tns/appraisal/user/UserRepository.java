package com.tns.appraisal.user;

import com.tns.appraisal.common.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    /**
     * Find a user by employee ID.
     *
     * @param employeeId the employee ID
     * @return Optional containing the user if found
     */
    Optional<User> findByEmployeeId(String employeeId);

    /**
     * Find a user by email address.
     *
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given employee ID.
     *
     * @param employeeId the employee ID
     * @return true if exists, false otherwise
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Check if a user exists with the given email.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
