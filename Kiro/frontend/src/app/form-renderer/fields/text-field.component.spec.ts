import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { TextFieldComponent } from './text-field.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';

describe('TextFieldComponent', () => {
  let component: TextFieldComponent;
  let fixture: ComponentFixture<TextFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextFieldComponent],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(TextFieldComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display label', () => {
    fixture.componentRef.setInput('label', 'Employee Name');
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('mat-label');
    expect(label.textContent).toContain('Employee Name');
  });

  it('should bind to form control', () => {
    const control = new FormControl('John Doe');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Name');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input');
    expect(input.value).toBe('John Doe');
  });

  it('should be readonly when readonly is true', () => {
    fixture.componentRef.setInput('control', new FormControl('Test'));
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input');
    expect(input.readOnly).toBe(true);
  });

  it('should show required error message', () => {
    const control = new FormControl('', Validators.required);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Required Field');
    fixture.detectChanges();

    control.markAsTouched();
    control.setValue('');
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Required Field is required');
  });

  it('should show email error message', () => {
    const control = new FormControl('invalid-email', Validators.email);
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Email');
    fixture.detectChanges();

    control.markAsTouched();
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Please enter a valid email');
  });

  it('should show minlength error message', () => {
    const control = new FormControl('ab', Validators.minLength(5));
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Username');
    fixture.detectChanges();

    control.markAsTouched();
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Minimum length is 5 characters');
  });

  it('should apply custom field class', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('fieldClass', 'custom-class');
    fixture.detectChanges();

    const matFormField = fixture.nativeElement.querySelector('mat-form-field');
    expect(matFormField.classList.contains('custom-class')).toBe(true);
  });

  it('should set input type', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Date');
    fixture.componentRef.setInput('type', 'date');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input');
    expect(input.type).toBe('date');
  });
});
