package com.tns.appraisal.form;

import com.tns.appraisal.common.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalFormRepository extends BaseRepository<AppraisalForm, Long> {

    List<AppraisalForm> findByCycleId(Long cycleId);

    Optional<AppraisalForm> findByCycleIdAndEmployeeId(Long cycleId, Long employeeId);

    boolean existsByCycleIdAndEmployeeId(Long cycleId, Long employeeId);

    List<AppraisalForm> findByEmployeeId(Long employeeId);

    List<AppraisalForm> findByManagerId(Long managerId);

    List<AppraisalForm> findByManagerIdAndStatus(Long managerId, String status);
}
