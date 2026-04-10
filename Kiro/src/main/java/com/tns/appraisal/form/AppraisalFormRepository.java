package com.tns.appraisal.form;

import com.tns.appraisal.common.BaseRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AppraisalForm entity providing standard CRUD and domain-specific queries.
 */
public interface AppraisalFormRepository extends BaseRepository<AppraisalForm, Long> {

    Optional<AppraisalForm> findByCycleIdAndEmployeeId(Long cycleId, Long employeeId);

    List<AppraisalForm> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<AppraisalForm> findByCycleIdAndManagerId(Long cycleId, Long managerId);

    List<AppraisalForm> findByCycleIdAndBackupReviewerId(Long cycleId, Long backupReviewerId);

    List<AppraisalForm> findByCycleId(Long cycleId);

    long countByCycleIdAndStatus(Long cycleId, FormStatus status);
}
