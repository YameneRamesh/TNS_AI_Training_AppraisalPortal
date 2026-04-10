package com.tns.appraisal.form.dto;

import java.util.List;

/**
 * Request DTO for an employee saving a self-appraisal draft.
 * Only contains employee-editable fields.
 */
public class SaveDraftRequest {

    private FormHeaderDto header;
    private List<SelfAppraisalItemDto> keyResponsibilities;
    private List<SelfAppraisalItemDto> idp;
    private List<SelfAppraisalItemDto> goals;
    private String nextYearGoals;
    private String teamMemberComments;

    public FormHeaderDto getHeader() { return header; }
    public void setHeader(FormHeaderDto header) { this.header = header; }

    public List<SelfAppraisalItemDto> getKeyResponsibilities() { return keyResponsibilities; }
    public void setKeyResponsibilities(List<SelfAppraisalItemDto> keyResponsibilities) {
        this.keyResponsibilities = keyResponsibilities;
    }

    public List<SelfAppraisalItemDto> getIdp() { return idp; }
    public void setIdp(List<SelfAppraisalItemDto> idp) { this.idp = idp; }

    public List<SelfAppraisalItemDto> getGoals() { return goals; }
    public void setGoals(List<SelfAppraisalItemDto> goals) { this.goals = goals; }

    public String getNextYearGoals() { return nextYearGoals; }
    public void setNextYearGoals(String nextYearGoals) { this.nextYearGoals = nextYearGoals; }

    public String getTeamMemberComments() { return teamMemberComments; }
    public void setTeamMemberComments(String teamMemberComments) {
        this.teamMemberComments = teamMemberComments;
    }

    /**
     * Employee-editable fields for a single rated item.
     */
    public static class SelfAppraisalItemDto {
        private String itemId;
        private String selfComment;
        private String selfRating;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getSelfComment() { return selfComment; }
        public void setSelfComment(String selfComment) { this.selfComment = selfComment; }

        public String getSelfRating() { return selfRating; }
        public void setSelfRating(String selfRating) { this.selfRating = selfRating; }
    }
}
