package com.tns.appraisal.form.dto;

/**
 * DTO for the policy adherence section of the appraisal form.
 */
public class PolicyAdherenceDto {

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
