package com.tns.appraisal.form.dto;

/**
 * DTO for a rated item in keyResponsibilities, idp, or goals sections.
 */
public class FormItemDto {

    private String itemId;
    private String selfComment;
    private String selfRating;
    private String managerComment;
    private String managerRating;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSelfComment() { return selfComment; }
    public void setSelfComment(String selfComment) { this.selfComment = selfComment; }

    public String getSelfRating() { return selfRating; }
    public void setSelfRating(String selfRating) { this.selfRating = selfRating; }

    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }

    public String getManagerRating() { return managerRating; }
    public void setManagerRating(String managerRating) { this.managerRating = managerRating; }
}
