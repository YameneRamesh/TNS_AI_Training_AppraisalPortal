import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TeamAppraisalListComponent, TeamMemberAction } from './team-appraisal-list.component';
import { TeamMemberForm } from '../../core/models/dashboard.model';
import { provideZonelessChangeDetection } from '@angular/core';

describe('TeamAppraisalListComponent', () => {
  let component: TeamAppraisalListComponent;
  let fixture: ComponentFixture<TeamAppraisalListComponent>;

  const mockTeamForms: TeamMemberForm[] = [
    { formId: 1, employeeId: 101, employeeName: 'Alice Smith', designation: 'Software Engineer', status: 'SUBMITTED', submittedAt: '2025-04-10T09:00:00Z' },
    { formId: 2, employeeId: 102, employeeName: 'Bob Jones', designation: 'QA Engineer', status: 'NOT_STARTED' },
    { formId: 3, employeeId: 103, employeeName: 'Carol White', designation: 'Tech Lead', status: 'REVIEWED_AND_COMPLETED', submittedAt: '2025-04-05T10:00:00Z' },
    { formId: 4, employeeId: 104, employeeName: 'Dave Brown', designation: 'Developer', status: 'UNDER_REVIEW' },
    { formId: 5, employeeId: 105, employeeName: 'Eve Davis', designation: 'Developer', status: 'REVIEW_DRAFT_SAVED' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamAppraisalListComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamAppraisalListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('teamForms', mockTeamForms);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a row for each team member', () => {
    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(mockTeamForms.length);
  });

  it('should show empty state when teamForms is empty', () => {
    fixture.componentRef.setInput('teamForms', []);
    fixture.detectChanges();
    const noData = fixture.nativeElement.querySelector('.no-data');
    expect(noData).toBeTruthy();
    expect(noData.textContent).toContain('No team members');
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('NOT_STARTED')).toBe('Not Started');
    expect(component.getStatusLabel('DRAFT_SAVED')).toBe('Draft Saved');
    expect(component.getStatusLabel('SUBMITTED')).toBe('Submitted');
    expect(component.getStatusLabel('UNDER_REVIEW')).toBe('Under Review');
    expect(component.getStatusLabel('REVIEW_DRAFT_SAVED')).toBe('Review in Progress');
    expect(component.getStatusLabel('REVIEWED_AND_COMPLETED')).toBe('Completed');
  });

  it('should return the raw status string for unknown statuses', () => {
    expect(component.getStatusLabel('UNKNOWN_STATUS')).toBe('UNKNOWN_STATUS');
  });

  it('should return correct CSS class for NOT_STARTED', () => {
    expect(component.getStatusClass('NOT_STARTED')).toBe('status-not-started');
  });

  it('should return correct CSS class for DRAFT_SAVED', () => {
    expect(component.getStatusClass('DRAFT_SAVED')).toBe('status-draft');
  });

  it('should return correct CSS class for SUBMITTED', () => {
    expect(component.getStatusClass('SUBMITTED')).toBe('status-submitted');
  });

  it('should return correct CSS class for UNDER_REVIEW', () => {
    expect(component.getStatusClass('UNDER_REVIEW')).toBe('status-under-review');
  });

  it('should return correct CSS class for REVIEW_DRAFT_SAVED', () => {
    expect(component.getStatusClass('REVIEW_DRAFT_SAVED')).toBe('status-review-draft');
  });

  it('should return correct CSS class for REVIEWED_AND_COMPLETED', () => {
    expect(component.getStatusClass('REVIEWED_AND_COMPLETED')).toBe('status-completed');
  });

  it('should allow review for SUBMITTED status', () => {
    expect(component.canReview('SUBMITTED')).toBe(true);
  });

  it('should allow review for UNDER_REVIEW status', () => {
    expect(component.canReview('UNDER_REVIEW')).toBe(true);
  });

  it('should allow review for REVIEW_DRAFT_SAVED status', () => {
    expect(component.canReview('REVIEW_DRAFT_SAVED')).toBe(true);
  });

  it('should not allow review for NOT_STARTED status', () => {
    expect(component.canReview('NOT_STARTED')).toBe(false);
  });

  it('should not allow review for DRAFT_SAVED status', () => {
    expect(component.canReview('DRAFT_SAVED')).toBe(false);
  });

  it('should not allow review for REVIEWED_AND_COMPLETED status', () => {
    expect(component.canReview('REVIEWED_AND_COMPLETED')).toBe(false);
  });

  it('should allow view for REVIEWED_AND_COMPLETED status', () => {
    expect(component.canView('REVIEWED_AND_COMPLETED')).toBe(true);
  });

  it('should not allow view for SUBMITTED status', () => {
    expect(component.canView('SUBMITTED')).toBe(false);
  });

  it('should not allow view for NOT_STARTED status', () => {
    expect(component.canView('NOT_STARTED')).toBe(false);
  });

  it('should emit memberSelected with readonly=false when onReview is called', () => {
    let emitted: TeamMemberAction | undefined;
    component.memberSelected.subscribe((action: TeamMemberAction) => (emitted = action));

    component.onReview(mockTeamForms[0]);

    expect(emitted).toEqual({ formId: 1, readonly: false });
  });

  it('should emit memberSelected with readonly=true when onView is called', () => {
    let emitted: TeamMemberAction | undefined;
    component.memberSelected.subscribe((action: TeamMemberAction) => (emitted = action));

    component.onView(mockTeamForms[2]);

    expect(emitted).toEqual({ formId: 3, readonly: true });
  });

  it('should treat SUBMITTED forms as reviewable (Req 6.1)', () => {
    const submittedForms = mockTeamForms.filter(f => f.status === 'SUBMITTED');
    submittedForms.forEach(f => expect(component.canReview(f.status)).toBe(true));
  });

  it('should display all five table columns', () => {
    expect(component.displayedColumns).toEqual([
      'employeeName', 'designation', 'department', 'status', 'actions'
    ]);
  });
});
