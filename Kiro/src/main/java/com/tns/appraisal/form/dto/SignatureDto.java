package com.tns.appraisal.form.dto;

/**
 * DTO for the signature block of the appraisal form.
 */
public class SignatureDto {

    private String preparedBy;
    private String reviewedBy;
    private String teamMemberAcknowledgement;

    public String getPreparedBy() { return preparedBy; }
    public void setPreparedBy(String preparedBy) { this.preparedBy = preparedBy; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getTeamMemberAcknowledgement() { return teamMemberAcknowledgement; }
    public void setTeamMemberAcknowledgement(String teamMemberAcknowledgement) {
        this.teamMemberAcknowledgement = teamMemberAcknowledgement;
    }
}
