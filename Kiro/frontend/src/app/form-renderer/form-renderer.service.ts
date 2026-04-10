import { Injectable } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import {
  AppraisalTemplateSchema,
  TemplateSection,
  RenderedSection,
  RenderedItem,
  FieldEditability,
  SectionType,
  RatingScale
} from './form-renderer.models';
import { FormStatus, FormData } from '../core/models';
import { FormValidationService } from './form-validation.service';

/**
 * FormRendererService
 * 
 * Interprets the JSON template schema from appraisal_templates.schema_json
 * and provides utilities for rendering dynamic forms.
 * 
 * Responsibilities:
 * - Parse and validate template JSON
 * - Map template sections to renderable structures
 * - Determine field editability based on role and form status
 * - Provide section ordering and metadata
 * - Create form groups with appropriate validation
 */
@Injectable({
  providedIn: 'root'
})
export class FormRendererService {

  constructor(private validationService: FormValidationService) {}

  /**
   * Parse the schema_json string from AppraisalTemplate into a typed structure
   */
  parseTemplateSchema(schemaJson: string): AppraisalTemplateSchema {
    try {
      const parsed = JSON.parse(schemaJson);
      
      if (!parsed.version || !Array.isArray(parsed.sections)) {
        throw new Error('Invalid template schema: missing version or sections');
      }

      return parsed as AppraisalTemplateSchema;
    } catch (error) {
      console.error('Failed to parse template schema:', error);
      throw new Error('Invalid template JSON');
    }
  }

  /**
   * Convert template sections into renderable sections with data-binding keys
   */
  renderSections(schema: AppraisalTemplateSchema): RenderedSection[] {
    return schema.sections
      .filter(section => this.isSectionRenderable(section.sectionType))
      .map(section => this.renderSection(section));
  }

  /**
   * Render a single section with its items
   */
  private renderSection(section: TemplateSection): RenderedSection {
    const items = section.items || [];
    
    return {
      sectionType: section.sectionType,
      title: section.title,
      items: items.map((item, index) => this.renderItem(section.sectionType, item, index))
    };
  }

  /**
   * Render a single item with its data-binding key
   */
  private renderItem(sectionType: SectionType, item: any, index: number): RenderedItem {
    return {
      id: item.id,
      label: item.label,
      ratingScale: item.ratingScale,
      dataKey: this.buildDataKey(sectionType, index)
    };
  }

  /**
   * Build the dot-notation data key for binding to FormData
   * e.g. "keyResponsibilities[0]" or "idp[1]"
   */
  private buildDataKey(sectionType: SectionType, index: number): string {
    const sectionMap: Record<string, string> = {
      'key_responsibilities': 'keyResponsibilities',
      'idp': 'idp',
      'goals': 'goals',
      'policy_adherence': 'policyAdherence'
    };

    const key = sectionMap[sectionType];
    if (!key) {
      return '';
    }

    // For array-based sections, include index
    if (['keyResponsibilities', 'idp', 'goals'].includes(key)) {
      return `${key}[${index}]`;
    }

    return key;
  }

  /**
   * Determine if a section should be rendered (excludes metadata-only sections)
   */
  private isSectionRenderable(sectionType: SectionType): boolean {
    // rating_key is display-only, not a data entry section
    return sectionType !== 'rating_key';
  }

  /**
   * Get field editability based on user role and form status
   * 
   * @param isEmployee - true if current user is the form's employee
   * @param isManager - true if current user is the form's manager or backup reviewer
   * @param formStatus - current status of the appraisal form
   */
  getFieldEditability(
    isEmployee: boolean,
    isManager: boolean,
    formStatus: FormStatus
  ): FieldEditability {
    // Employee can edit self fields only when form is NOT_STARTED or DRAFT_SAVED
    const employeeCanEdit = isEmployee && 
      (formStatus === 'NOT_STARTED' || formStatus === 'DRAFT_SAVED');

    // Manager can edit manager fields when form is SUBMITTED, UNDER_REVIEW, or REVIEW_DRAFT_SAVED
    const managerCanEdit = isManager && 
      (formStatus === 'SUBMITTED' || formStatus === 'UNDER_REVIEW' || formStatus === 'REVIEW_DRAFT_SAVED');

    return {
      selfCommentEditable: employeeCanEdit,
      selfRatingEditable: employeeCanEdit,
      managerCommentEditable: managerCanEdit,
      managerRatingEditable: managerCanEdit
    };
  }

  /**
   * Get the display order for sections (matches TnS Appraisal Form V3.0 layout)
   */
  getSectionOrder(): SectionType[] {
    return [
      'header',
      'rating_key',
      'overall_evaluation',
      'key_responsibilities',
      'idp',
      'policy_adherence',
      'goals',
      'next_year_goals',
      'signature'
    ];
  }

  /**
   * Get a section by type from the rendered sections
   */
  getSectionByType(sections: RenderedSection[], type: SectionType): RenderedSection | undefined {
    return sections.find(s => s.sectionType === type);
  }

  /**
   * Validate that all required sections are present in the template
   */
  validateTemplateCompleteness(schema: AppraisalTemplateSchema): { valid: boolean; missing: SectionType[] } {
    const requiredSections: SectionType[] = [
      'header',
      'key_responsibilities',
      'idp',
      'policy_adherence',
      'goals'
    ];

    const presentSections = new Set(schema.sections.map(s => s.sectionType));
    const missing = requiredSections.filter(req => !presentSections.has(req));

    return {
      valid: missing.length === 0,
      missing
    };
  }

  /**
   * Get rating options for a given rating scale
   */
  getRatingOptions(ratingScale: 'competency' | 'policy_1_10'): string[] | number[] {
    if (ratingScale === 'competency') {
      return ['Excels', 'Exceeds', 'Meets', 'Developing'];
    } else {
      return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
    }
  }

  /**
   * Get the description for competency rating levels (for rating_key section)
   */
  getCompetencyRatingDescriptions(): Record<string, string> {
    return {
      'Excels': 'Consistently exceeds expectations and demonstrates exceptional performance',
      'Exceeds': 'Frequently exceeds expectations and shows strong performance',
      'Meets': 'Consistently meets expectations and performs at expected level',
      'Developing': 'Working toward meeting expectations, requires development'
    };
  }

  /**
   * Create a form control with appropriate validators for a self-comment field
   */
  createSelfCommentControl(
    initialValue: string = '',
    isRequired: boolean = false
  ): FormControl {
    const validators = this.validationService.getSelfCommentValidators(isRequired);
    return new FormControl(initialValue, validators);
  }

  /**
   * Create a form control with appropriate validators for a self-rating field
   */
  createSelfRatingControl(
    initialValue: string | number | null = null,
    isRequired: boolean = false,
    ratingScale: RatingScale = 'competency'
  ): FormControl {
    const validators = this.validationService.getSelfRatingValidators(isRequired, ratingScale);
    return new FormControl(initialValue, validators);
  }

  /**
   * Create a form control with appropriate validators for a manager comment field
   */
  createManagerCommentControl(
    initialValue: string = '',
    isRequired: boolean = false
  ): FormControl {
    const validators = this.validationService.getManagerCommentValidators(isRequired);
    return new FormControl(initialValue, validators);
  }

  /**
   * Create a form control with appropriate validators for a manager rating field
   */
  createManagerRatingControl(
    initialValue: string | number | null = null,
    isRequired: boolean = false,
    ratingScale: RatingScale = 'competency'
  ): FormControl {
    const validators = this.validationService.getManagerRatingValidators(isRequired, ratingScale);
    return new FormControl(initialValue, validators);
  }

  /**
   * Validate form data before employee submission
   */
  validateForEmployeeSubmission(formData: FormData): { valid: boolean; errors: any } {
    const errors = this.validationService.validateEmployeeSubmission(formData);
    return {
      valid: errors === null,
      errors: errors || {}
    };
  }

  /**
   * Validate form data before manager review completion
   */
  validateForManagerCompletion(formData: FormData): { valid: boolean; errors: any } {
    const errors = this.validationService.validateManagerReviewCompletion(formData);
    return {
      valid: errors === null,
      errors: errors || {}
    };
  }

  /**
   * Check if validation is required for self-appraisal fields
   */
  isSelfAppraisalValidationRequired(formStatus: FormStatus): boolean {
    return this.validationService.isSelfAppraisalRequired(formStatus);
  }

  /**
   * Check if validation is required for manager review fields
   */
  isManagerReviewValidationRequired(formStatus: FormStatus): boolean {
    return this.validationService.isManagerReviewRequired(formStatus);
  }
}
