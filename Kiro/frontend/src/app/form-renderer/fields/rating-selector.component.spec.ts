import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { RatingSelectorComponent } from './rating-selector.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';

describe('RatingSelectorComponent', () => {
  let component: RatingSelectorComponent;
  let fixture: ComponentFixture<RatingSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingSelectorComponent],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(RatingSelectorComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display competency rating options', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('ratingScale', 'competency');
    fixture.detectChanges();

    const options = component.ratingOptions();
    expect(options).toEqual(['Excels', 'Exceeds', 'Meets', 'Developing']);
  });

  it('should display policy rating options (1-10)', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Policy Rating');
    fixture.componentRef.setInput('ratingScale', 'policy_1_10');
    fixture.detectChanges();

    const options = component.ratingOptions();
    expect(options).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  });

  it('should bind to form control', () => {
    const control = new FormControl('Meets');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('ratingScale', 'competency');
    fixture.detectChanges();

    expect(component.control().value).toBe('Meets');
  });

  it('should render as dropdown by default', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('displayMode', 'dropdown');
    fixture.detectChanges();

    const dropdown = fixture.nativeElement.querySelector('mat-select');
    expect(dropdown).toBeTruthy();
  });

  it('should render as button toggle when specified', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('displayMode', 'toggle');
    fixture.detectChanges();

    const toggleGroup = fixture.nativeElement.querySelector('mat-button-toggle-group');
    expect(toggleGroup).toBeTruthy();
  });

  it('should disable control when disabled is true', () => {
    const control = new FormControl('Meets');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('disabled', true);
    fixture.componentRef.setInput('displayMode', 'dropdown');
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('mat-select');
    expect(select.getAttribute('aria-disabled')).toBe('true');
  });

  it('should show required error message', () => {
    const control = new FormControl('', Validators.required);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Required Rating');
    fixture.detectChanges();

    control.markAsTouched();
    control.setValue('');
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Required Rating is required');
  });

  it('should apply custom field class in dropdown mode', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('fieldClass', 'custom-class');
    fixture.componentRef.setInput('displayMode', 'dropdown');
    fixture.detectChanges();

    const matFormField = fixture.nativeElement.querySelector('mat-form-field');
    expect(matFormField.classList.contains('custom-class')).toBe(true);
  });

  it('should render all competency options in dropdown', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('ratingScale', 'competency');
    fixture.componentRef.setInput('displayMode', 'dropdown');
    fixture.detectChanges();

    // Open the select
    const select = fixture.nativeElement.querySelector('mat-select');
    select.click();
    fixture.detectChanges();

    const options = document.querySelectorAll('mat-option');
    expect(options.length).toBe(4);
  });

  it('should render all policy options in toggle mode', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Policy Rating');
    fixture.componentRef.setInput('ratingScale', 'policy_1_10');
    fixture.componentRef.setInput('displayMode', 'toggle');
    fixture.detectChanges();

    const toggleButtons = fixture.nativeElement.querySelectorAll('mat-button-toggle');
    expect(toggleButtons.length).toBe(10);
  });

  it('should update control value when option selected', () => {
    const control = new FormControl('');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Rating');
    fixture.componentRef.setInput('ratingScale', 'competency');
    fixture.detectChanges();

    control.setValue('Exceeds');
    fixture.detectChanges();

    expect(component.control().value).toBe('Exceeds');
  });
});
