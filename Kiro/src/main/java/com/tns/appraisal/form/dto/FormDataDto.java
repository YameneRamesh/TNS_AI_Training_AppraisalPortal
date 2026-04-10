package com.tns.appraisal.form.dto;

import java.util.List;

/**
 * DTO mirroring the full form_data JSON structure stored in appraisal_forms.
 */
public class FormDataDto {

    private FormHeaderDto header;
    private List<FormItemDto> keyResponsibilities;
    private List<FormItemDto> idp;
    private PolicyAdherenceDto policyAdherence;
    private List<FormItemDto> goals;
    private String nextYearGoals;
    private OverallEvaluationDto overallEvaluation;
    private SignatureDto signature;

    public FormHeaderDto getHeader() { return header; }
    public void setHeader(FormHeaderDto header) { this.header = header; }

    public List<FormItemDto> getKeyResponsibilities() { return keyResponsibilities; }
    public void setKeyResponsibilities(List<FormItemDto> keyResponsibilities) {
        this.keyResponsibilities = keyResponsibilities;
    }

    public List<FormItemDto> getIdp() { return idp; }
    public void setIdp(List<FormItemDto> idp) { this.idp = idp; }

    public PolicyAdherenceDto getPolicyAdherence() { return policyAdherence; }
    public void setPolicyAdherence(PolicyAdherenceDto policyAdherence) {
        this.policyAdherence = policyAdherence;
    }

    public List<FormItemDto> getGoals() { return goals; }
    public void setGoals(List<FormItemDto> goals) { this.goals = goals; }

    public String getNextYearGoals() { return nextYearGoals; }
    public void setNextYearGoals(String nextYearGoals) { this.nextYearGoals = nextYearGoals; }

    public OverallEvaluationDto getOverallEvaluation() { return overallEvaluation; }
    public void setOverallEvaluation(OverallEvaluationDto overallEvaluation) {
        this.overallEvaluation = overallEvaluation;
    }

    public SignatureDto getSignature() { return signature; }
    public void setSignature(SignatureDto signature) { this.signature = signature; }
}
