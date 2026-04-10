import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { SignatureSectionComponent } from './signature-section.component';
import { FieldEditability } from '../form-renderer.models';

function buildSignatureFormGroup(overrides: Record<string, string> = {}): FormGroup {
  return new FormGroup({
    preparedBy: new FormControl(overrides['preparedBy'] ?? ''),
    reviewedBy: new FormControl(overrides['reviewedBy'] ?? ''),
    teamMemberAcknowledgement: new FormControl(overrides['teamMemberAcknowledgement'] ?? '')
  });
}

const MANAGER_EDITABLE: FieldEditability = {
  selfCommentEditable: false,
  selfRatingEditable: false,
  managerCommentEditable: true,
  managerRatingEditable: true
};

const EMPLOYEE_EDITABLE: FieldEditability = {
  selfCommentEditable: true,
  selfRatingEditable: true,
  managerCommentEditable: false,
  managerRatingEditable: false
};

const ALL_READ_ONLY: FieldEditability = {
  selfCommentEditable: false,
  selfRatingEditable: false,
  managerCommentEditable: false,
  managerRatingEditable: false
};

describe('SignatureSectionComponent', () => {
  let component: SignatureSectionComponent;
  let fixture: ComponentFixture<SignatureSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignatureSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(SignatureSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render the section title', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Signatures and Acknowledgement');
  });

  it('should render the Manager block title', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Manager');
  });

  it('should render the Team Member block title', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Team Member');
  });

  it('should render Prepared/Delivered By field', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Prepared/Delivered By');
  });

  it('should render Reviewed By field', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Reviewed By');
  });

  it('should render Team Member Acknowledgement field', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Acknowledgement');
  });

  it('should bind preparedBy control value', () => {
    component.formGroup = buildSignatureFormGroup({ preparedBy: 'John Manager' });
    component.editability = MANAGER_EDITABLE;
    fixture.detectChanges();

    const inputs: HTMLInputElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('input')
    );
    const preparedInput = inputs.find((el) => el.value === 'John Manager');
    expect(preparedInput).toBeTruthy();
  });

  it('should bind teamMemberAcknowledgement control value', () => {
    component.formGroup = buildSignatureFormGroup({
      teamMemberAcknowledgement: 'I acknowledge this appraisal'
    });
    component.editability = EMPLOYEE_EDITABLE;
    fixture.detectChanges();

    const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    expect(textarea?.value).toContain('I acknowledge this appraisal');
  });

  describe('read-only mode', () => {
    it('should make manager fields readonly when managerCommentEditable is false', () => {
      component.formGroup = buildSignatureFormGroup({ preparedBy: 'Manager' });
      component.editability = ALL_READ_ONLY;
      fixture.detectChanges();

      const inputs: HTMLInputElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('input')
      );
      inputs.forEach((input) => expect(input.readOnly).toBe(true));
    });

    it('should make acknowledgement textarea readonly when selfCommentEditable is false', () => {
      component.formGroup = buildSignatureFormGroup();
      component.editability = ALL_READ_ONLY;
      fixture.detectChanges();

      const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
      expect(textarea?.readOnly).toBe(true);
    });

    it('should allow manager fields to be editable when managerCommentEditable is true', () => {
      component.formGroup = buildSignatureFormGroup({ preparedBy: 'Manager' });
      component.editability = MANAGER_EDITABLE;
      fixture.detectChanges();

      const inputs: HTMLInputElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('input')
      );
      // preparedBy and reviewedBy should not be readonly
      inputs.forEach((input) => expect(input.readOnly).toBe(false));
    });

    it('should allow acknowledgement to be editable when selfCommentEditable is true', () => {
      component.formGroup = buildSignatureFormGroup();
      component.editability = EMPLOYEE_EDITABLE;
      fixture.detectChanges();

      const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
      expect(textarea?.readOnly).toBe(false);
    });
  });

  it('should render the acknowledgement note', () => {
    component.formGroup = buildSignatureFormGroup();
    component.editability = ALL_READ_ONLY;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Note');
  });
});
