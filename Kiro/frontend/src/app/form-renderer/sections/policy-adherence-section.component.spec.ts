import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideZonelessChangeDetection } from '@angular/core';
import { PolicyAdherenceSectionComponent } from './policy-adherence-section.component';
import { RenderedItem, FieldEditability } from '../form-renderer.models';

function buildPolicyFormGroup(overrides: Record<string, any> = {}): FormGroup {
  return new FormGroup({
    0: new FormGroup({ managerRating: new FormControl(overrides['hrPolicy'] ?? null) }),
    1: new FormGroup({ managerRating: new FormControl(overrides['availability'] ?? null) }),
    2: new FormGroup({ managerRating: new FormControl(overrides['additionalSupport'] ?? null) }),
    managerComments: new FormControl(overrides['managerComments'] ?? '')
  });
}

const POLICY_ITEMS: RenderedItem[] = [
  { id: 'policy_hr', label: 'Follow HR Policy', ratingScale: 'policy_1_10', dataKey: 'policyAdherence' },
  { id: 'policy_avail', label: 'Team Member Availability During Critical Deliverables', ratingScale: 'policy_1_10', dataKey: 'policyAdherence' },
  { id: 'policy_support', label: 'Additional Support Beyond Regular Work Assignments', ratingScale: 'policy_1_10', dataKey: 'policyAdherence' }
];

const EDITABLE: FieldEditability = {
  selfCommentEditable: false,
  selfRatingEditable: false,
  managerCommentEditable: true,
  managerRatingEditable: true
};

const READ_ONLY_EDITABILITY: FieldEditability = {
  selfCommentEditable: false,
  selfRatingEditable: false,
  managerCommentEditable: false,
  managerRatingEditable: false
};

describe('PolicyAdherenceSectionComponent', () => {
  let component: PolicyAdherenceSectionComponent;
  let fixture: ComponentFixture<PolicyAdherenceSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyAdherenceSectionComponent, ReactiveFormsModule],
      providers: [provideZonelessChangeDetection(), provideAnimationsAsync()]
    }).compileComponents();

    fixture = TestBed.createComponent(PolicyAdherenceSectionComponent);
    component = fixture.componentInstance;
  }, 30000);

  it('should create', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render the section title', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    component.title = 'Company Policies and Business Continuity Support Adherence';
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('h2');
    expect(heading?.textContent).toContain('Company Policies');
  });

  it('should render the 1-10 scale description', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('1 to 10');
  });

  it('should render one policy item block per item', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();

    const itemBlocks = fixture.nativeElement.querySelectorAll('.policy-item');
    expect(itemBlocks.length).toBe(3);
  });

  it('should render all three standard policy item labels', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Follow HR Policy');
    expect(html).toContain('Team Member Availability');
    expect(html).toContain('Additional Support');
  });

  it('should use numeric 1-10 rating options', () => {
    expect(component.ratingOptions).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  });

  it('should render a Manager\'s Comments textarea', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();

    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain("Manager's Comments");
  });

  it('should render numbered item indicators', () => {
    component.formGroup = buildPolicyFormGroup();
    component.items = POLICY_ITEMS;
    component.editability = EDITABLE;
    fixture.detectChanges();

    const numbers = fixture.nativeElement.querySelectorAll('.item-number');
    expect(numbers.length).toBe(3);
    expect(numbers[0].textContent).toContain('1');
    expect(numbers[1].textContent).toContain('2');
    expect(numbers[2].textContent).toContain('3');
  });

  describe('read-only mode', () => {
    it('should make manager comments textarea readonly when editability disables it', () => {
      component.formGroup = buildPolicyFormGroup();
      component.items = POLICY_ITEMS;
      component.editability = READ_ONLY_EDITABILITY;
      fixture.detectChanges();

      const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
      expect(textarea?.readOnly).toBe(true);
    });
  });
});
