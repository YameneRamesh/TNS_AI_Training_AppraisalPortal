package com.tns.appraisal.form.dto;

/**
 * DTO for the header section of the appraisal form.
 */
public class FormHeaderDto {

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
