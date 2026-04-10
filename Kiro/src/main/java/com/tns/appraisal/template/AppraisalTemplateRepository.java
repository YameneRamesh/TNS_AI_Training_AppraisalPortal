package com.tns.appraisal.template;

import com.tns.appraisal.common.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for AppraisalTemplate entity operations.
 */
@Repository
public interface AppraisalTemplateRepository extends BaseRepository<AppraisalTemplate, Long> {

    /**
     * Find the currently active template.
     * 
     * @return Optional containing the active template if one exists
     */
    Optional<AppraisalTemplate> findByIsActiveTrue();

    /**
     * Find a template by its version.
     * 
     * @param version the template version
     * @return Optional containing the template if found
     */
    Optional<AppraisalTemplate> findByVersion(String version);
}
