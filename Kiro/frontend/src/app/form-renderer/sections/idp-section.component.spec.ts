import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { IdpSectionComponent } from './idp-section.component';
import { RenderedItem } from '../form-renderer.models';

function buildIdpFormGroup(itemCount: number): FormGroup {
  const items = Array.from({ length: itemCount }, (_, i) =>
    new FormGroup({
      selfComment: new FormControl(`IDP self comment ${i + 1}`),
      selfRating: new FormControl('Excels'),
      managerComment: new FormControl(`IDP manager comment ${i + 1}`),
      managerRating: new FormControl('Exceeds')
    })
  );
  return new FormGroup({
    idp: new FormArray(items)
  });
}

function buildIdpItems(count: number): RenderedItem[] {
  const labels = ['NextGen Tech Skills', 'Value Addition', 'Leadership'];
  return Array.from({ length: count }, (_, i) => ({
    id: `idp_${i}`,
    label: labels[i] ?? `IDP Item ${i + 1}`,
    ratingScale: 'competency' as const,
    dataKey: `idp[${i}]`
  }));
}

describe('IdpSectionComponent', () => {
  let component: IdpSectionComponent;
  let fixture: ComponentFixture<IdpSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdpSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(IdpSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildIdpFormGroup(1);
    component.items = buildIdpItems(1);
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render the section title', () => {
    component.formGroup = buildIdpFormGroup(1);
    component.items = buildIdpItems(1);
    component.title = 'Individual Development Plan';
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Individual Development Plan');
  });

  it('should render the IDP description text', () => {
    component.formGroup = buildIdpFormGroup(1);
    component.items = buildIdpItems(1);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Individual Development Plan');
  });

  it('should render one item block per IDP item', () => {
    component.formGroup = buildIdpFormGroup(3);
    component.items = buildIdpItems(3);
    fixture.detectChanges();

    const itemBlocks = fixture.nativeElement.querySelectorAll('.idp-item');
    expect(itemBlocks.length).toBe(3);
  });

  it('should render the three standard IDP category labels', () => {
    component.formGroup = buildIdpFormGroup(3);
    component.items = buildIdpItems(3);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('NextGen Tech Skills');
    expect(html).toContain('Value Addition');
    expect(html).toContain('Leadership');
  });

  it('should expose itemsFormArray from the form group', () => {
    component.formGroup = buildIdpFormGroup(3);
    component.items = buildIdpItems(3);
    fixture.detectChanges();

    const arr = component.itemsFormArray;
    expect(arr).toBeTruthy();
    expect(arr.length).toBe(3);
  });

  it('should return the correct FormGroup for a given index', () => {
    component.formGroup = buildIdpFormGroup(2);
    component.items = buildIdpItems(2);
    fixture.detectChanges();

    const group = component.getItemFormGroup(1);
    expect(group).toBeInstanceOf(FormGroup);
    expect(group.get('selfComment')?.value).toBe('IDP self comment 2');
  });

  it('should render all four required fields per item', () => {
    component.formGroup = buildIdpFormGroup(1);
    component.items = buildIdpItems(1);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Team Member Comments');
    expect(html).toContain('Self Rating');
    expect(html).toContain('Manager Comments');
    expect(html).toContain('Manager Rating');
  });

  it('should use competency rating options', () => {
    expect(component.ratingOptions).toEqual(['Excels', 'Exceeds', 'Meets', 'Developing']);
  });

  describe('read-only mode', () => {
    it('should make all textareas readonly when readonly is true', () => {
      component.formGroup = buildIdpFormGroup(1);
      component.items = buildIdpItems(1);
      component.readonly = true;
      component.canEditSelf = false;
      component.canEditManager = false;
      fixture.detectChanges();

      const textareas: HTMLTextAreaElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('textarea')
      );
      textareas.forEach((ta) => expect(ta.readOnly).toBe(true));
    });
  });
});
