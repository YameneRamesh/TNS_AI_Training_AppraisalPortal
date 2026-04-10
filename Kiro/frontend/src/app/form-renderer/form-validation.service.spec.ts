import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { FormValidationService } from './form-validation.service';
import {
  FormData,
  ResponsibilityItem,
  IdpItem,
  GoalItem,
  PolicyAdherenceData
} from '../core/models';

describe('FormValidationService', () => {
  let service: FormValidationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()]
    });
    service = TestBed.inject(FormValidationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Competency Rating Validation', () => {
    it('should accept valid competency ratings', () => {
      const validators = service.getSelfRatingValidators(true, 'competency');
      const control = new FormControl('Excels', validators);
      
      expect(control.valid).toBe(true);
      
      control.setValue('Exceeds');
      expect(control.valid).toBe(true);
      
      control.setValue('Meets');
      expect(control.valid).toBe(true);
      
      control.setValue('Developing');
      expect(control.valid).toBe(true);
    });

    it('should reject invalid competency ratings', () => {
      const validators = service.getSelfRatingValidators(true, 'competency');
      const control = new FormControl('Invalid', validators);
      
      expect(control.valid).toBe(false);
      expect(control.errors?.['invalidCompetencyRating']).toBeTruthy();
    });

    it('should require rating when isRequired is true', () => {
      const validators = service.getSelfRatingValidators(true, 'competency');
      const control = new FormControl('', validators);
      
      expect(control.valid).toBe(false);
      expect(control.errors?.['required']).toBeTruthy();
    });

    it('should not require rating when isRequired is false', () => {
      const validators = service.getSelfRatingValidators(false, 'competency');
      const control = new FormControl('', validators);
      
      expect(control.valid).toBe(true);
    });
  });

  describe('Policy Rating Validation', () => {
    it('should accept valid policy ratings (1-10)', () => {
      const validators = service.getManagerRatingValidators(true, 'policy_1_10');
      
      for (let i = 1; i <= 10; i++) {
        const control = new FormControl(i, validators);
        expect(control.valid).toBe(true);
      }
    });

    it('should reject policy ratings outside 1-10 range', () => {
      const validators = service.getManagerRatingValidators(true, 'policy_1_10');
      
      const control0 = new FormControl(0, validators);
      expect(control0.valid).toBe(false);
      expect(control0.errors?.['invalidPolicyRating']).toBeTruthy();
      
      const control11 = new FormControl(11, validators);
      expect(control11.valid).toBe(false);
      expect(control11.errors?.['invalidPolicyRating']).toBeTruthy();
    });

    it('should reject non-integer policy ratings', () => {
      const validators = service.getManagerRatingValidators(true, 'policy_1_10');
      const control = new FormControl(5.5, validators);
      
      expect(control.valid).toBe(false);
      expect(control.errors?.['invalidPolicyRating']).toBeTruthy();
    });
  });

  describe('Comment Validation', () => {
    it('should enforce maxLength of 2000 characters', () => {
      const validators = service.getSelfCommentValidators(true);
      const longText = 'a'.repeat(2001);
      const control = new FormControl(longText, validators);
      
      expect(control.valid).toBe(false);
      expect(control.errors?.['maxlength']).toBeTruthy();
    });

    it('should accept comments within maxLength', () => {
      const validators = service.getSelfCommentValidators(true);
      const validText = 'a'.repeat(2000);
      const control = new FormControl(validText, validators);
      
      expect(control.valid).toBe(true);
    });
  });

  describe('Form Status Requirements', () => {
    it('should identify when self-appraisal is required', () => {
      expect(service.isSelfAppraisalRequired('DRAFT_SAVED')).toBe(true);
      expect(service.isSelfAppraisalRequired('SUBMITTED')).toBe(true);
      expect(service.isSelfAppraisalRequired('NOT_STARTED')).toBe(false);
      expect(service.isSelfAppraisalRequired('UNDER_REVIEW')).toBe(false);
    });

    it('should identify when manager review is required', () => {
      expect(service.isManagerReviewRequired('REVIEW_DRAFT_SAVED')).toBe(true);
      expect(service.isManagerReviewRequired('REVIEWED_AND_COMPLETED')).toBe(true);
      expect(service.isManagerReviewRequired('SUBMITTED')).toBe(false);
      expect(service.isManagerReviewRequired('DRAFT_SAVED')).toBe(false);
    });
  });

  describe('Employee Submission Validation', () => {
    it('should pass validation for complete employee submission', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Test comment',
            selfRating: 'Meets'
          }
        ],
        idp: [
          {
            itemId: 'idp_1',
            selfComment: 'Test IDP comment',
            selfRating: 'Exceeds'
          }
        ],
        goals: [
          {
            itemId: 'goal_1',
            selfComment: 'Test goal comment',
            selfRating: 'Meets'
          }
        ]
      };

      const errors = service.validateEmployeeSubmission(formData);
      expect(errors).toBeNull();
    });

    it('should fail validation when key responsibilities are missing', () => {
      const formData: FormData = {
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets' }]
      };

      const errors = service.validateEmployeeSubmission(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['keyResponsibilities']).toBeTruthy();
    });

    it('should fail validation when self comments are missing', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: '',
            selfRating: 'Meets'
          }
        ],
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets' }]
      };

      const errors = service.validateEmployeeSubmission(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['keyResponsibilities']).toContain('Self comment is required');
    });

    it('should fail validation when self ratings are missing', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Test comment',
            selfRating: undefined
          }
        ],
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets' }]
      };

      const errors = service.validateEmployeeSubmission(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['keyResponsibilities']).toContain('Self rating is required');
    });
  });

  describe('Manager Review Validation', () => {
    it('should pass validation for complete manager review', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Employee comment',
            selfRating: 'Meets',
            managerComment: 'Manager comment',
            managerRating: 'Exceeds'
          }
        ],
        idp: [
          {
            itemId: 'idp_1',
            selfComment: 'Employee IDP',
            selfRating: 'Meets',
            managerComment: 'Manager IDP comment',
            managerRating: 'Exceeds'
          }
        ],
        goals: [
          {
            itemId: 'goal_1',
            selfComment: 'Employee goal',
            selfRating: 'Meets',
            managerComment: 'Manager goal comment',
            managerRating: 'Exceeds'
          }
        ],
        policyAdherence: {
          hrPolicy: { managerRating: 8 },
          availability: { managerRating: 9 },
          additionalSupport: { managerRating: 7 }
        },
        overallEvaluation: {
          managerComments: 'Overall manager comments'
        }
      };

      const errors = service.validateManagerReviewCompletion(formData);
      expect(errors).toBeNull();
    });

    it('should fail validation when manager comments are missing', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Test',
            selfRating: 'Meets',
            managerComment: '',
            managerRating: 'Exceeds'
          }
        ],
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        policyAdherence: {
          hrPolicy: { managerRating: 8 },
          availability: { managerRating: 9 },
          additionalSupport: { managerRating: 7 }
        },
        overallEvaluation: { managerComments: 'Test' }
      };

      const errors = service.validateManagerReviewCompletion(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['keyResponsibilities']).toContain('Manager comment is required');
    });

    it('should fail validation when policy adherence is missing', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Test',
            selfRating: 'Meets',
            managerComment: 'Test',
            managerRating: 'Exceeds'
          }
        ],
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        overallEvaluation: { managerComments: 'Test' }
      };

      const errors = service.validateManagerReviewCompletion(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['policyAdherence']).toBeTruthy();
    });

    it('should fail validation when overall evaluation manager comments are missing', () => {
      const formData: FormData = {
        keyResponsibilities: [
          {
            itemId: 'kr_1',
            selfComment: 'Test',
            selfRating: 'Meets',
            managerComment: 'Test',
            managerRating: 'Exceeds'
          }
        ],
        idp: [{ itemId: 'idp_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        goals: [{ itemId: 'goal_1', selfComment: 'Test', selfRating: 'Meets', managerComment: 'Test', managerRating: 'Meets' }],
        policyAdherence: {
          hrPolicy: { managerRating: 8 },
          availability: { managerRating: 9 },
          additionalSupport: { managerRating: 7 }
        }
      };

      const errors = service.validateManagerReviewCompletion(formData);
      expect(errors).toBeTruthy();
      expect(errors?.['overallEvaluation']).toBeTruthy();
    });
  });

  describe('Form Group Utilities', () => {
    it('should detect validation errors in form group', () => {
      const formGroup = new FormGroup({
        field1: new FormControl('', service.getSelfCommentValidators(true)),
        field2: new FormControl('', service.getSelfRatingValidators(true, 'competency'))
      });

      expect(service.hasValidationErrors(formGroup)).toBe(true);
    });

    it('should mark all controls as touched', () => {
      const formGroup = new FormGroup({
        field1: new FormControl(''),
        field2: new FormControl('')
      });

      service.markAllAsTouched(formGroup);

      expect(formGroup.get('field1')?.touched).toBe(true);
      expect(formGroup.get('field2')?.touched).toBe(true);
    });

    it('should get all validation errors from form group', () => {
      const formGroup = new FormGroup({
        field1: new FormControl('', service.getSelfCommentValidators(true)),
        field2: new FormControl('', service.getSelfRatingValidators(true, 'competency'))
      });

      const errors = service.getAllValidationErrors(formGroup);
      expect(errors.length).toBeGreaterThan(0);
    });
  });

  describe('Error Messages', () => {
    it('should return appropriate error message for required field', () => {
      const message = service.getValidationErrorMessage({ required: true });
      expect(message).toContain('required');
    });

    it('should return appropriate error message for maxlength', () => {
      const message = service.getValidationErrorMessage({ 
        maxlength: { requiredLength: 2000, actualLength: 2001 } 
      });
      expect(message).toContain('2000');
    });

    it('should return appropriate error message for invalid competency rating', () => {
      const message = service.getValidationErrorMessage({ 
        invalidCompetencyRating: { value: 'Invalid' } 
      });
      expect(message).toContain('Excels');
    });

    it('should return appropriate error message for invalid policy rating', () => {
      const message = service.getValidationErrorMessage({ 
        invalidPolicyRating: { value: 11 } 
      });
      expect(message).toContain('1 and 10');
    });
  });
});
