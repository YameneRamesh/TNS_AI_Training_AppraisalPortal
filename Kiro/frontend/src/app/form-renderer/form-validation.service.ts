import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import {
  FormData,
  FormStatus,
  ResponsibilityItem,
  IdpItem,
  GoalItem,
  PolicyAdherenceData
} from '../core/models';
import { SectionType, RatingScale } from './form-renderer.models';

/**
 * FormValidationService
 * 
 * Provides validation logic for the dynamic appraisal form.
 * Handles field-level and form-level validation based on:
 * - User role (Employee vs Manager)
 * - Form status (determines which fields are required)
 * - Section type (different validation rules per section)
 */
@Injectable({
  providedIn: 'root'
})
export class FormValidationService {

  /**
   * Create validators for a self-comment field
   */
  getSelfCommentValidators(isRequired: boolean): ValidatorFn[] {
    const validators: ValidatorFn[] = [];
    
    if (isRequired) {
      validators.push(Validators.required);
    }
    
    validators.push(Validators.maxLength(2000));
    
    return validators;
  }

  /**
   * Create validators for a self-rating field
   */
  getSelfRatingValidators(isRequired: boolean, ratingScale: RatingScale): ValidatorFn[] {
    const validators: ValidatorFn[] = [];
    
    if (isRequired) {
      validators.push(Validators.required);
    }
    
    if (ratingScale === 'competency') {
      validators.push(this.competencyRatingValidator());
    } else {
      validators.push(this.policyRatingValidator());
    }
    
    return validators;
  }

  /**
   * Create validators for a manager comment field
   */
  getManagerCommentValidators(isRequired: boolean): ValidatorFn[] {
    const validators: ValidatorFn[] = [];
    
    if (isRequired) {
      validators.push(Validators.required);
    }
    
    validators.push(Validators.maxLength(2000));
    
    return validators;
  }

  /**
   * Create validators for a manager rating field
   */
  getManagerRatingValidators(isRequired: boolean, ratingScale: RatingScale): ValidatorFn[] {
    const validators: ValidatorFn[] = [];
    
    if (isRequired) {
      validators.push(Validators.required);
    }
    
    if (ratingScale === 'competency') {
      validators.push(this.competencyRatingValidator());
    } else {
      validators.push(this.policyRatingValidator());
    }
    
    return validators;
  }

  /**
   * Validator for competency ratings (Excels, Exceeds, Meets, Developing)
   */
  private competencyRatingValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null; // Let required validator handle empty values
      }
      
      const validRatings = ['Excels', 'Exceeds', 'Meets', 'Developing'];
      return validRatings.includes(control.value) 
        ? null 
        : { invalidCompetencyRating: { value: control.value } };
    };
  }

  /**
   * Validator for policy ratings (1-10)
   */
  private policyRatingValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null; // Let required validator handle empty values
      }
      
      const value = Number(control.value);
      return (Number.isInteger(value) && value >= 1 && value <= 10)
        ? null
        : { invalidPolicyRating: { value: control.value } };
    };
  }

  /**
   * Determine if self-appraisal fields are required based on form status
   * Self-appraisal fields are required when employee is submitting the form
   */
  isSelfAppraisalRequired(formStatus: FormStatus): boolean {
    return formStatus === 'DRAFT_SAVED' || formStatus === 'SUBMITTED';
  }

  /**
   * Determine if manager review fields are required based on form status
   * Manager fields are required when manager is completing the review
   */
  isManagerReviewRequired(formStatus: FormStatus): boolean {
    return formStatus === 'REVIEW_DRAFT_SAVED' || formStatus === 'REVIEWED_AND_COMPLETED';
  }

  /**
   * Validate form data completeness for employee submission
   * Returns validation errors if any required fields are missing
   */
  validateEmployeeSubmission(formData: FormData): ValidationErrors | null {
    const errors: any = {};

    // Validate key responsibilities
    if (!formData.keyResponsibilities || formData.keyResponsibilities.length === 0) {
      errors.keyResponsibilities = 'At least one key responsibility is required';
    } else {
      const krErrors = this.validateResponsibilityItems(formData.keyResponsibilities, true);
      if (krErrors) {
        errors.keyResponsibilities = krErrors;
      }
    }

    // Validate IDP
    if (!formData.idp || formData.idp.length === 0) {
      errors.idp = 'IDP items are required';
    } else {
      const idpErrors = this.validateIdpItems(formData.idp, true);
      if (idpErrors) {
        errors.idp = idpErrors;
      }
    }

    // Validate goals
    if (!formData.goals || formData.goals.length === 0) {
      errors.goals = 'At least one goal is required';
    } else {
      const goalErrors = this.validateGoalItems(formData.goals, true);
      if (goalErrors) {
        errors.goals = goalErrors;
      }
    }

    return Object.keys(errors).length > 0 ? errors : null;
  }

  /**
   * Validate form data completeness for manager review completion
   * Returns validation errors if any required fields are missing
   */
  validateManagerReviewCompletion(formData: FormData): ValidationErrors | null {
    const errors: any = {};

    // Validate key responsibilities manager fields
    if (formData.keyResponsibilities) {
      const krErrors = this.validateResponsibilityItems(formData.keyResponsibilities, false, true);
      if (krErrors) {
        errors.keyResponsibilities = krErrors;
      }
    }

    // Validate IDP manager fields
    if (formData.idp) {
      const idpErrors = this.validateIdpItems(formData.idp, false, true);
      if (idpErrors) {
        errors.idp = idpErrors;
      }
    }

    // Validate goals manager fields
    if (formData.goals) {
      const goalErrors = this.validateGoalItems(formData.goals, false, true);
      if (goalErrors) {
        errors.goals = goalErrors;
      }
    }

    // Validate policy adherence
    if (!formData.policyAdherence) {
      errors.policyAdherence = 'Policy adherence ratings are required';
    } else {
      const policyErrors = this.validatePolicyAdherence(formData.policyAdherence);
      if (policyErrors) {
        errors.policyAdherence = policyErrors;
      }
    }

    // Validate overall evaluation
    if (!formData.overallEvaluation?.managerComments) {
      errors.overallEvaluation = 'Manager comments are required';
    }

    return Object.keys(errors).length > 0 ? errors : null;
  }

  /**
   * Validate responsibility items
   */
  private validateResponsibilityItems(
    items: ResponsibilityItem[], 
    validateSelf: boolean = false,
    validateManager: boolean = false
  ): string | null {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      
      if (validateSelf) {
        if (!item.selfComment || item.selfComment.trim() === '') {
          return `Self comment is required for responsibility ${i + 1}`;
        }
        if (!item.selfRating) {
          return `Self rating is required for responsibility ${i + 1}`;
        }
      }
      
      if (validateManager) {
        if (!item.managerComment || item.managerComment.trim() === '') {
          return `Manager comment is required for responsibility ${i + 1}`;
        }
        if (!item.managerRating) {
          return `Manager rating is required for responsibility ${i + 1}`;
        }
      }
    }
    
    return null;
  }

  /**
   * Validate IDP items
   */
  private validateIdpItems(
    items: IdpItem[], 
    validateSelf: boolean = false,
    validateManager: boolean = false
  ): string | null {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      
      if (validateSelf) {
        if (!item.selfComment || item.selfComment.trim() === '') {
          return `Self comment is required for IDP item ${i + 1}`;
        }
        if (!item.selfRating) {
          return `Self rating is required for IDP item ${i + 1}`;
        }
      }
      
      if (validateManager) {
        if (!item.managerComment || item.managerComment.trim() === '') {
          return `Manager comment is required for IDP item ${i + 1}`;
        }
        if (!item.managerRating) {
          return `Manager rating is required for IDP item ${i + 1}`;
        }
      }
    }
    
    return null;
  }

  /**
   * Validate goal items
   */
  private validateGoalItems(
    items: GoalItem[], 
    validateSelf: boolean = false,
    validateManager: boolean = false
  ): string | null {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      
      if (validateSelf) {
        if (!item.selfComment || item.selfComment.trim() === '') {
          return `Self comment is required for goal ${i + 1}`;
        }
        if (!item.selfRating) {
          return `Self rating is required for goal ${i + 1}`;
        }
      }
      
      if (validateManager) {
        if (!item.managerComment || item.managerComment.trim() === '') {
          return `Manager comment is required for goal ${i + 1}`;
        }
        if (!item.managerRating) {
          return `Manager rating is required for goal ${i + 1}`;
        }
      }
    }
    
    return null;
  }

  /**
   * Validate policy adherence data
   */
  private validatePolicyAdherence(data: PolicyAdherenceData): string | null {
    if (!data.hrPolicy?.managerRating) {
      return 'HR Policy rating is required';
    }
    if (!data.availability?.managerRating) {
      return 'Availability rating is required';
    }
    if (!data.additionalSupport?.managerRating) {
      return 'Additional Support rating is required';
    }
    
    // Validate ratings are in range 1-10
    const ratings = [
      data.hrPolicy.managerRating,
      data.availability.managerRating,
      data.additionalSupport.managerRating
    ];
    
    for (const rating of ratings) {
      if (rating < 1 || rating > 10) {
        return 'Policy ratings must be between 1 and 10';
      }
    }
    
    return null;
  }

  /**
   * Get validation error messages for display
   */
  getValidationErrorMessage(errors: ValidationErrors): string {
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['maxlength']) {
      return `Maximum length is ${errors['maxlength'].requiredLength} characters`;
    }
    if (errors['invalidCompetencyRating']) {
      return 'Please select a valid rating (Excels, Exceeds, Meets, or Developing)';
    }
    if (errors['invalidPolicyRating']) {
      return 'Please select a rating between 1 and 10';
    }
    return 'Invalid input';
  }

  /**
   * Check if a form group has any validation errors
   */
  hasValidationErrors(formGroup: FormGroup): boolean {
    return formGroup.invalid;
  }

  /**
   * Get all validation errors from a form group
   */
  getAllValidationErrors(formGroup: FormGroup): string[] {
    const errors: string[] = [];
    
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      if (control && control.invalid && control.errors) {
        const errorMessage = this.getValidationErrorMessage(control.errors);
        errors.push(`${key}: ${errorMessage}`);
      }
    });
    
    return errors;
  }

  /**
   * Mark all fields in a form group as touched to trigger validation display
   */
  markAllAsTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      if (control) {
        control.markAsTouched();
        
        if (control instanceof FormGroup) {
          this.markAllAsTouched(control);
        }
      }
    });
  }
}
