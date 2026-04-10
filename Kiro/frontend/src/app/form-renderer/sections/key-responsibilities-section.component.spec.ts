import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { KeyResponsibilitiesSectionComponent } from './key-responsibilities-section.component';
import { RenderedItem } from '../form-renderer.models';

function buildKrFormGroup(itemCount: number): FormGroup {
  const items = Array.from({ length: itemCount }, (_, i) =>
    new FormGroup({
      selfComment: new FormControl(`Self comment ${i + 1}`),
      selfRating: new FormControl('Meets'),
      managerComment: new FormControl(`Manager comment ${i + 1}`),
      managerRating: new FormControl('Exceeds')
    })
  );
  return new FormGroup({
    keyResponsibilities: new FormArray(items)
  });
}

function buildRenderedItems(count: number): RenderedItem[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `kr_${i + 1}`,
    label: `Essential Duty ${i + 1}`,
    ratingScale: 'competency' as const,
    dataKey: `keyResponsibilities[${i}]`
  }));
}

describe('KeyResponsibilitiesSectionComponent', () => {
  let component: KeyResponsibilitiesSectionComponent;
  let fixture: ComponentFixture<KeyResponsibilitiesSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KeyResponsibilitiesSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(KeyResponsibilitiesSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildKrFormGroup(1);
    component.items = buildRenderedItems(1);
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render the section title', () => {
    component.formGroup = buildKrFormGroup(2);
    component.items = buildRenderedItems(2);
    component.title = 'Key Responsibilities';
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Key Responsibilities');
  });

  it('should render one item block per rendered item', () => {
    component.formGroup = buildKrFormGroup(3);
    component.items = buildRenderedItems(3);
    fixture.detectChanges();

    const itemBlocks = fixture.nativeElement.querySelectorAll('.responsibility-item');
    expect(itemBlocks.length).toBe(3);
  });

  it('should render item labels', () => {
    component.formGroup = buildKrFormGroup(2);
    component.items = buildRenderedItems(2);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Essential Duty 1');
    expect(html).toContain('Essential Duty 2');
  });

  it('should expose itemsFormArray from the form group', () => {
    component.formGroup = buildKrFormGroup(2);
    component.items = buildRenderedItems(2);
    fixture.detectChanges();

    expect(component.itemsFormArray).toBeInstanceOf(FormArray);
    expect(component.itemsFormArray.length).toBe(2);
  });

  it('should return the correct FormGroup for a given index', () => {
    component.formGroup = buildKrFormGroup(2);
    component.items = buildRenderedItems(2);
    fixture.detectChanges();

    const group = component.getItemFormGroup(0);
    expect(group).toBeInstanceOf(FormGroup);
    expect(group.get('selfComment')?.value).toBe('Self comment 1');
  });

  it('should render all four required fields per item (selfComment, selfRating, managerComment, managerRating)', () => {
    component.formGroup = buildKrFormGroup(1);
    component.items = buildRenderedItems(1);
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
    it('should make self fields non-editable when readonly is true', () => {
      component.formGroup = buildKrFormGroup(1);
      component.items = buildRenderedItems(1);
      component.readonly = true;
      component.canEditSelf = false;
      component.canEditManager = false;
      fixture.detectChanges();

      const textareas: HTMLTextAreaElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('textarea')
      );
      textareas.forEach((ta) => expect(ta.readOnly).toBe(true));
    });

    it('should allow self editing when canEditSelf is true and readonly is false', () => {
      component.formGroup = buildKrFormGroup(1);
      component.items = buildRenderedItems(1);
      component.readonly = false;
      component.canEditSelf = true;
      component.canEditManager = false;
      fixture.detectChanges();

      // selfComment textarea should NOT be readonly
      const textareas: HTMLTextAreaElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('textarea')
      );
      // First textarea is selfComment, third is managerComment
      expect(textareas[0].readOnly).toBe(false);
    });
  });

  describe('edge cases', () => {
    it('should handle zero items gracefully', () => {
      component.formGroup = buildKrFormGroup(0);
      component.items = [];
      fixture.detectChanges();

      const itemBlocks = fixture.nativeElement.querySelectorAll('.responsibility-item');
      expect(itemBlocks.length).toBe(0);
    });

    it('should handle five items (maximum per spec)', () => {
      component.formGroup = buildKrFormGroup(5);
      component.items = buildRenderedItems(5);
      fixture.detectChanges();

      const itemBlocks = fixture.nativeElement.querySelectorAll('.responsibility-item');
      expect(itemBlocks.length).toBe(5);
    });
  });
});
