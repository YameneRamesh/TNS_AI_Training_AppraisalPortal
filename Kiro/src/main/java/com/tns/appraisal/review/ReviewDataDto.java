package com.tns.appraisal.review;

import java.util.List;

/**
 * DTO carrying manager review data (comments and ratings) for save-draft and complete-review operations.
 */
public class ReviewDataDto {

    private List<ReviewItemDto> keyResponsibilities;
    private List<ReviewItemDto> idp;
    private List<ReviewItemDto> goals;
    private PolicyAdherenceDto policyAdherence;
    private String managerComments;
    private SignatureDto signature;

    // --- Getters & Setters ---

    public List<ReviewItemDto> getKeyResponsibilities() { return keyResponsibilities; }
    public void setKeyResponsibilities(List<ReviewItemDto> keyResponsibilities) { this.keyResponsibilities = keyResponsibilities; }

    public List<ReviewItemDto> getIdp() { return idp; }
    public void setIdp(List<ReviewItemDto> idp) { this.idp = idp; }

    public List<ReviewItemDto> getGoals() { return goals; }
    public void setGoals(List<ReviewItemDto> goals) { this.goals = goals; }

    public PolicyAdherenceDto getPolicyAdherence() { return policyAdherence; }
    public void setPolicyAdherence(PolicyAdherenceDto policyAdherence) { this.policyAdherence = policyAdherence; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public SignatureDto getSignature() { return signature; }
    public void setSignature(SignatureDto signature) { this.signature = signature; }

    // --- Nested DTOs ---

    public static class ReviewItemDto {
        private String itemId;
        private String managerComment;
        private String managerRating;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getManagerComment() { return managerComment; }
        public void setManagerComment(String managerComment) { this.managerComment = managerComment; }

        public String getManagerRating() { return managerRating; }
        public void setManagerRating(String managerRating) { this.managerRating = managerRating; }
    }

    public static class PolicyAdherenceDto {
        private PolicyRatingDto hrPolicy;
        private PolicyRatingDto availability;
        private PolicyRatingDto additionalSupport;
        private String managerComments;

        public PolicyRatingDto getHrPolicy() { return hrPolicy; }
        public void setHrPolicy(PolicyRatingDto hrPolicy) { this.hrPolicy = hrPolicy; }

        public PolicyRatingDto getAvailability() { return availability; }
        public void setAvailability(PolicyRatingDto availability) { this.availability = availability; }

        public PolicyRatingDto getAdditionalSupport() { return additionalSupport; }
        public void setAdditionalSupport(PolicyRatingDto additionalSupport) { this.additionalSupport = additionalSupport; }

        public String getManagerComments() { return managerComments; }
        public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

        public static class PolicyRatingDto {
            private Integer managerRating;

            public Integer getManagerRating() { return managerRating; }
            public void setManagerRating(Integer managerRating) { this.managerRating = managerRating; }
        }
    }

    public static class SignatureDto {
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
