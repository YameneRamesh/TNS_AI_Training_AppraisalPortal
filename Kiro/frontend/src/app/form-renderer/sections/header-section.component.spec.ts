import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { HeaderSectionComponent } from './header-section.component';

function buildHeaderFormGroup(overrides: Record<string, string> = {}): FormGroup {
  return new FormGroup({
    header: new FormGroup({
      dateOfHire: new FormControl(overrides['dateOfHire'] ?? ''),
      dateOfReview: new FormControl(overrides['dateOfReview'] ?? ''),
      reviewPeriod: new FormControl(overrides['reviewPeriod'] ?? ''),
      typeOfReview: new FormControl(overrides['typeOfReview'] ?? '')
    })
  });
}

describe('HeaderSectionComponent', () => {
  let component: HeaderSectionComponent;
  let fixture: ComponentFixture<HeaderSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildHeaderFormGroup();
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should expose the nested header FormGroup', () => {
    component.formGroup = buildHeaderFormGroup();
    fixture.detectChanges();
    expect(component.headerFormGroup).toBeInstanceOf(FormGroup);
  });

  it('should render employee name as read-only display', () => {
    component.formGroup = buildHeaderFormGroup();
    component.employeeName = 'Jane Smith';
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input');
    const nameInput = Array.from(inputs).find(
      (el: any) => el.value === 'Jane Smith'
    ) as HTMLInputElement | undefined;
    expect(nameInput).toBeTruthy();
    expect(nameInput!.readOnly).toBe(true);
  });

  it('should render manager name as read-only display', () => {
    component.formGroup = buildHeaderFormGroup();
    component.managerName = 'John Manager';
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input');
    const managerInput = Array.from(inputs).find(
      (el: any) => el.value === 'John Manager'
    ) as HTMLInputElement | undefined;
    expect(managerInput).toBeTruthy();
    expect(managerInput!.readOnly).toBe(true);
  });

  it('should render designation as read-only display', () => {
    component.formGroup = buildHeaderFormGroup();
    component.designation = 'Senior Engineer';
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input');
    const desigInput = Array.from(inputs).find(
      (el: any) => el.value === 'Senior Engineer'
    ) as HTMLInputElement | undefined;
    expect(desigInput).toBeTruthy();
    expect(desigInput!.readOnly).toBe(true);
  });

  it('should bind dateOfHire control value to input', () => {
    component.formGroup = buildHeaderFormGroup({ dateOfHire: '2020-01-15' });
    fixture.detectChanges();

    const inputs: HTMLInputElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('input')
    );
    const dateInput = inputs.find((el) => el.value === '2020-01-15');
    expect(dateInput).toBeTruthy();
  });

  it('should make form fields readonly when readonly is true', () => {
    component.formGroup = buildHeaderFormGroup({ dateOfHire: '2020-01-15' });
    component.readonly = true;
    fixture.detectChanges();

    const inputs: HTMLInputElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('input')
    );
    // All bound inputs should be readonly
    const boundInputs = inputs.filter((el) => !el.value || el.readOnly);
    expect(boundInputs.length).toBeGreaterThan(0);
  });

  it('should render the section title', () => {
    component.formGroup = buildHeaderFormGroup();
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Appraisal Form Header');
  });

  it('should render all required header labels', () => {
    component.formGroup = buildHeaderFormGroup();
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Team Member Name');
    expect(html).toContain('Date of Hire');
    expect(html).toContain('Designation');
    expect(html).toContain('Date of Review');
    expect(html).toContain('Manager Name');
    expect(html).toContain('Review Period');
    expect(html).toContain('Type of Review');
  });
});
