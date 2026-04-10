package com.tns.appraisal.form.dto;

/**
 * DTO for the overall evaluation section of the appraisal form.
 */
public class OverallEvaluationDto {

    private String managerComments;
    private String teamMemberComments;

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public String getTeamMemberComments() { return teamMemberComments; }
    public void setTeamMemberComments(String teamMemberComments) { this.teamMemberComments = teamMemberComments; }
}
