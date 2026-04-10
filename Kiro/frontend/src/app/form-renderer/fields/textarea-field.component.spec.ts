import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { TextareaFieldComponent } from './textarea-field.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';

describe('TextareaFieldComponent', () => {
  let component: TextareaFieldComponent;
  let fixture: ComponentFixture<TextareaFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextareaFieldComponent],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(TextareaFieldComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display label', () => {
    fixture.componentRef.setInput('label', 'Comments');
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('mat-label');
    expect(label.textContent).toContain('Comments');
  });

  it('should bind to form control', () => {
    const control = new FormControl('Test comment');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Comment');
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea');
    expect(textarea.value).toBe('Test comment');
  });

  it('should set number of rows', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('rows', 5);
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea');
    expect(textarea.rows).toBe(5);
  });

  it('should be readonly when readonly is true', () => {
    fixture.componentRef.setInput('control', new FormControl('Test'));
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('readonly', true);
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('textarea');
    expect(textarea.readOnly).toBe(true);
  });

  it('should show character count when enabled', () => {
    const control = new FormControl('Hello');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('showCharCount', true);
    fixture.componentRef.setInput('maxLength', 100);
    fixture.detectChanges();

    const hint = fixture.nativeElement.querySelector('mat-hint');
    expect(hint.textContent).toContain('5 / 100');
  });

  it('should update character count on input', () => {
    const control = new FormControl('');
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('showCharCount', true);
    fixture.componentRef.setInput('maxLength', 100);
    fixture.detectChanges();

    control.setValue('Hello World');
    fixture.detectChanges();

    const hint = fixture.nativeElement.querySelector('mat-hint');
    expect(hint.textContent).toContain('11 / 100');
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

  it('should show maxlength error message', () => {
    const control = new FormControl('This is a very long text', Validators.maxLength(10));
    fixture.componentRef.setInput('control', control);
    fixture.componentRef.setInput('label', 'Comment');
    fixture.detectChanges();

    control.markAsTouched();
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Maximum length is 10 characters');
  });

  it('should apply custom field class', () => {
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('label', 'Field');
    fixture.componentRef.setInput('fieldClass', 'full-width');
    fixture.detectChanges();

    const matFormField = fixture.nativeElement.querySelector('mat-form-field');
    expect(matFormField.classList.contains('full-width')).toBe(true);
  });
});
