import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { GoalsSectionComponent } from './goals-section.component';
import { RenderedItem } from '../form-renderer.models';

function buildGoalsFormGroup(itemCount: number, nextYearGoals = ''): FormGroup {
  const items = Array.from({ length: itemCount }, (_, i) =>
    new FormGroup({
      selfComment: new FormControl(`Goal self comment ${i + 1}`),
      selfRating: new FormControl('Meets'),
      managerComment: new FormControl(`Goal manager comment ${i + 1}`),
      managerRating: new FormControl('Exceeds')
    })
  );
  return new FormGroup({
    goals: new FormArray(items),
    nextYearGoals: new FormControl(nextYearGoals)
  });
}

function buildGoalItems(count: number): RenderedItem[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `goal_${i + 1}`,
    label: `Goal ${i + 1}`,
    ratingScale: 'competency' as const,
    dataKey: `goals[${i}]`
  }));
}

describe('GoalsSectionComponent', () => {
  let component: GoalsSectionComponent;
  let fixture: ComponentFixture<GoalsSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoalsSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(GoalsSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildGoalsFormGroup(1);
    component.items = buildGoalItems(1);
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render the section title', () => {
    component.formGroup = buildGoalsFormGroup(1);
    component.items = buildGoalItems(1);
    component.title = 'Goals';
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Goals');
  });

  it('should render the "carried over from previous year" description', () => {
    component.formGroup = buildGoalsFormGroup(1);
    component.items = buildGoalItems(1);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('previous year');
  });

  it('should render one goal item block per item', () => {
    component.formGroup = buildGoalsFormGroup(3);
    component.items = buildGoalItems(3);
    fixture.detectChanges();

    const itemBlocks = fixture.nativeElement.querySelectorAll('.goal-item');
    expect(itemBlocks.length).toBe(3);
  });

  it('should render goal item labels with numbering', () => {
    component.formGroup = buildGoalsFormGroup(2);
    component.items = buildGoalItems(2);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Goal 1');
    expect(html).toContain('Goal 2');
  });

  it('should expose itemsFormArray from the form group', () => {
    component.formGroup = buildGoalsFormGroup(2);
    component.items = buildGoalItems(2);
    fixture.detectChanges();

    const arr = component.itemsFormArray;
    expect(arr).toBeTruthy();
    expect(arr.length).toBe(2);
  });

  it('should return the correct FormGroup for a given index', () => {
    component.formGroup = buildGoalsFormGroup(2);
    component.items = buildGoalItems(2);
    fixture.detectChanges();

    const group = component.getItemFormGroup(0);
    expect(group).toBeInstanceOf(FormGroup);
    expect(group.get('selfComment')?.value).toBe('Goal self comment 1');
  });

  it('should render all four required fields per goal item', () => {
    component.formGroup = buildGoalsFormGroup(1);
    component.items = buildGoalItems(1);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Team Member Comments');
    expect(html).toContain('Self Rating');
    expect(html).toContain('Manager Comments');
    expect(html).toContain('Manager Rating');
  });

  it('should render the Next Year Goals subsection', () => {
    component.formGroup = buildGoalsFormGroup(1, 'Improve cloud skills');
    component.items = buildGoalItems(1);
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Next Year Goals');
  });

  it('should bind nextYearGoals control value', () => {
    component.formGroup = buildGoalsFormGroup(1, 'Improve cloud skills');
    component.items = buildGoalItems(1);
    fixture.detectChanges();

    const textareas: HTMLTextAreaElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('textarea')
    );
    const nextYearTextarea = textareas.find((ta) => ta.value === 'Improve cloud skills');
    expect(nextYearTextarea).toBeTruthy();
  });

  it('should use competency rating options', () => {
    expect(component.ratingOptions).toEqual(['Excels', 'Exceeds', 'Meets', 'Developing']);
  });

  describe('read-only mode', () => {
    it('should make all textareas readonly when readonly is true', () => {
      component.formGroup = buildGoalsFormGroup(1);
      component.items = buildGoalItems(1);
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

  describe('edge cases', () => {
    it('should handle zero goal items (only next year goals shown)', () => {
      component.formGroup = buildGoalsFormGroup(0);
      component.items = [];
      fixture.detectChanges();

      const itemBlocks = fixture.nativeElement.querySelectorAll('.goal-item');
      expect(itemBlocks.length).toBe(0);

      // Next Year Goals section should still render
      const html: string = fixture.nativeElement.innerHTML;
      expect(html).toContain('Next Year Goals');
    });
  });
});
