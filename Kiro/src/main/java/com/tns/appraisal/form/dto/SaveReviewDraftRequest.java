package com.tns.appraisal.form.dto;

import java.util.List;

/**
 * Request DTO for a manager saving or completing a review.
 * Only contains manager-editable fields.
 */
public class SaveReviewDraftRequest {

    private List<ReviewItemDto> keyResponsibilities;
    private List<ReviewItemDto> idp;
    private PolicyAdherenceDto policyAdherence;
    private List<ReviewItemDto> goals;
    private String managerComments;
    private SignatureDto signature;

    public List<ReviewItemDto> getKeyResponsibilities() { return keyResponsibilities; }
    public void setKeyResponsibilities(List<ReviewItemDto> keyResponsibilities) {
        this.keyResponsibilities = keyResponsibilities;
    }

    public List<ReviewItemDto> getIdp() { return idp; }
    public void setIdp(List<ReviewItemDto> idp) { this.idp = idp; }

    public PolicyAdherenceDto getPolicyAdherence() { return policyAdherence; }
    public void setPolicyAdherence(PolicyAdherenceDto policyAdherence) {
        this.policyAdherence = policyAdherence;
    }

    public List<ReviewItemDto> getGoals() { return goals; }
    public void setGoals(List<ReviewItemDto> goals) { this.goals = goals; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public SignatureDto getSignature() { return signature; }
    public void setSignature(SignatureDto signature) { this.signature = signature; }

    /**
     * Manager-editable fields for a single rated item.
     */
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
}
