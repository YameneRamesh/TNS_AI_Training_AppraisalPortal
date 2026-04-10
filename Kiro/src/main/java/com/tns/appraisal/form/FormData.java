package com.tns.appraisal.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Represents the JSON structure stored in the form_data column of appraisal_forms.
 * All fields are nullable to support partial saves (draft state).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormData {

    private Header header;
    private List<RatedItem> keyResponsibilities;
    private List<RatedItem> idp;
    private PolicyAdherence policyAdherence;
    private List<RatedItem> goals;
    private String nextYearGoals;
    private OverallEvaluation overallEvaluation;
    private Signature signature;

    // --- Getters & Setters ---

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }

    public List<RatedItem> getKeyResponsibilities() { return keyResponsibilities; }
    public void setKeyResponsibilities(List<RatedItem> keyResponsibilities) { this.keyResponsibilities = keyResponsibilities; }

    public List<RatedItem> getIdp() { return idp; }
    public void setIdp(List<RatedItem> idp) { this.idp = idp; }

    public PolicyAdherence getPolicyAdherence() { return policyAdherence; }
    public void setPolicyAdherence(PolicyAdherence policyAdherence) { this.policyAdherence = policyAdherence; }

    public List<RatedItem> getGoals() { return goals; }
    public void setGoals(List<RatedItem> goals) { this.goals = goals; }

    public String getNextYearGoals() { return nextYearGoals; }
    public void setNextYearGoals(String nextYearGoals) { this.nextYearGoals = nextYearGoals; }

    public OverallEvaluation getOverallEvaluation() { return overallEvaluation; }
    public void setOverallEvaluation(OverallEvaluation overallEvaluation) { this.overallEvaluation = overallEvaluation; }

    public Signature getSignature() { return signature; }
    public void setSignature(Signature signature) { this.signature = signature; }

    // --- Nested Classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String dateOfHire;
        private String dateOfReview;
        private String reviewPeriod;
        private String typeOfReview;

        public String getDateOfHire() { return dateOfHire; }
        public void setDateOfHire(String dateOfHire) { this.dateOfHire = dateOfHire; }

        public String getDateOfReview() { return dateOfReview; }
        public void setDateOfReview(String dateOfReview) { this.dateOfReview = dateOfReview; }

        public String getReviewPeriod() { return reviewPeriod; }
        public void setReviewPeriod(String reviewPeriod) { this.reviewPeriod = reviewPeriod; }

        public String getTypeOfReview() { return typeOfReview; }
        public void setTypeOfReview(String typeOfReview) { this.typeOfReview = typeOfReview; }
    }

    /**
     * Represents a rated item in key responsibilities, IDP, or goals sections.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RatedItem {
        private String itemId;
        private String selfComment;
        private Rating selfRating;
        private String managerComment;
        private Rating managerRating;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getSelfComment() { return selfComment; }
        public void setSelfComment(String selfComment) { this.selfComment = selfComment; }

        public Rating getSelfRating() { return selfRating; }
        public void setSelfRating(Rating selfRating) { this.selfRating = selfRating; }

        public String getManagerComment() { return managerComment; }
        public void setManagerComment(String managerComment) { this.managerComment = managerComment; }

        public Rating getManagerRating() { return managerRating; }
        public void setManagerRating(Rating managerRating) { this.managerRating = managerRating; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PolicyAdherence {
        private PolicyScore hrPolicy;
        private PolicyScore availability;
        private PolicyScore additionalSupport;
        private String managerComments;

        public PolicyScore getHrPolicy() { return hrPolicy; }
        public void setHrPolicy(PolicyScore hrPolicy) { this.hrPolicy = hrPolicy; }

        public PolicyScore getAvailability() { return availability; }
        public void setAvailability(PolicyScore availability) { this.availability = availability; }

        public PolicyScore getAdditionalSupport() { return additionalSupport; }
        public void setAdditionalSupport(PolicyScore additionalSupport) { this.additionalSupport = additionalSupport; }

        public String getManagerComments() { return managerComments; }
        public void setManagerComments(String managerComments) { this.managerComments = managerComments; }
    }

    /** Holds a 1–10 manager rating for a policy adherence item. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PolicyScore {
        private Integer managerRating;

        public Integer getManagerRating() { return managerRating; }
        public void setManagerRating(Integer managerRating) { this.managerRating = managerRating; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverallEvaluation {
        private String managerComments;
        private String teamMemberComments;

        public String getManagerComments() { return managerComments; }
        public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

        public String getTeamMemberComments() { return teamMemberComments; }
        public void setTeamMemberComments(String teamMemberComments) { this.teamMemberComments = teamMemberComments; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Signature {
        private String preparedBy;
        private String reviewedBy;
        private String teamMemberAcknowledgement;

        public String getPreparedBy() { return preparedBy; }
        public void setPreparedBy(String preparedBy) { this.preparedBy = preparedBy; }

        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

        public String getTeamMemberAcknowledgement() { return teamMemberAcknowledgement; }
        public void setTeamMemberAcknowledgement(String teamMemberAcknowledgement) { this.teamMemberAcknowledgement = teamMemberAcknowledgement; }
    }
}
