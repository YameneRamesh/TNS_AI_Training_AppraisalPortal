import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormNavigationComponent } from './form-navigation.component';
import { RenderedSection, SectionType } from './form-renderer.models';
import { FormStatus } from '../core/models';
import { provideZonelessChangeDetection } from '@angular/core';
import { vi } from 'vitest';

describe('FormNavigationComponent', () => {
  let component: FormNavigationComponent;
  let fixture: ComponentFixture<FormNavigationComponent>;

  const mockSections: RenderedSection[] = [
    {
      sectionType: 'header',
      title: 'Header Information',
      items: []
    },
    {
      sectionType: 'rating_key',
      title: 'Rating Key',
      items: []
    },
    {
      sectionType: 'key_responsibilities',
      title: 'Key Responsibilities',
      items: [
        { id: 'kr_1', label: 'Duty 1', ratingScale: 'competency', dataKey: 'keyResponsibilities[0]' }
      ]
    },
    {
      sectionType: 'idp',
      title: 'Individual Development Plan',
      items: [
        { id: 'idp_1', label: 'NextGen Tech', ratingScale: 'competency', dataKey: 'idp[0]' }
      ]
    },
    {
      sectionType: 'signature',
      title: 'Signature',
      items: []
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormNavigationComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();

    fixture = TestBed.createComponent(FormNavigationComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter out rating_key section from navigation', () => {
    component.sections = mockSections;
    component.ngOnInit();

    expect(component.navigableSections.length).toBe(4);
    expect(component.navigableSections.find(s => s.sectionType === 'rating_key')).toBeUndefined();
  });

  it('should calculate progress correctly with no completed sections', () => {
    component.sections = mockSections;
    component.sectionCompletionStatus = new Map();
    component.ngOnInit();

    expect(component.completedSections).toBe(0);
    expect(component.progressPercentage).toBe(0);
  });

  it('should calculate progress correctly with some completed sections', () => {
    component.sections = mockSections;
    component.sectionCompletionStatus = new Map([
      ['header', true],
      ['key_responsibilities', true]
    ]);
    component.ngOnInit();

    expect(component.completedSections).toBe(2);
    expect(component.progressPercentage).toBe(50); // 2 out of 4 navigable sections
  });

  it('should calculate 100% progress when all sections completed', () => {
    component.sections = mockSections;
    component.sectionCompletionStatus = new Map([
      ['header', true],
      ['key_responsibilities', true],
      ['idp', true],
      ['signature', true]
    ]);
    component.ngOnInit();

    expect(component.completedSections).toBe(4);
    expect(component.progressPercentage).toBe(100);
  });

  it('should emit sectionChange when navigating to a section', () => {
    component.sections = mockSections;
    component.ngOnInit();

    vi.spyOn(component.sectionChange, 'emit');
    component.navigateToSection(2);

    expect(component.sectionChange.emit).toHaveBeenCalledWith(2);
  });

  it('should allow navigation to previous section when not at start', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 2;
    component.ngOnInit();

    expect(component.canNavigatePrevious()).toBe(true);
  });

  it('should not allow navigation to previous section when at start', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 0;
    component.ngOnInit();

    expect(component.canNavigatePrevious()).toBe(false);
  });

  it('should allow navigation to next section when not at end', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 1;
    component.ngOnInit();

    expect(component.canNavigateNext()).toBe(true);
  });

  it('should not allow navigation to next section when at end', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 3; // Last navigable section
    component.ngOnInit();

    expect(component.canNavigateNext()).toBe(false);
  });

  it('should emit navigationRequest and sectionChange when navigating previous', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 2;
    component.ngOnInit();

    vi.spyOn(component.navigationRequest, 'emit');
    vi.spyOn(component.sectionChange, 'emit');

    component.navigatePrevious();

    expect(component.navigationRequest.emit).toHaveBeenCalledWith('previous');
    expect(component.sectionChange.emit).toHaveBeenCalledWith(1);
  });

  it('should emit navigationRequest and sectionChange when navigating next', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 1;
    component.ngOnInit();

    vi.spyOn(component.navigationRequest, 'emit');
    vi.spyOn(component.sectionChange, 'emit');

    component.navigateNext();

    expect(component.navigationRequest.emit).toHaveBeenCalledWith('next');
    expect(component.sectionChange.emit).toHaveBeenCalledWith(2);
  });

  it('should return correct section display names', () => {
    expect(component.getSectionDisplayName('header')).toBe('Header Information');
    expect(component.getSectionDisplayName('key_responsibilities')).toBe('Key Responsibilities');
    expect(component.getSectionDisplayName('idp')).toBe('Individual Development Plan');
    expect(component.getSectionDisplayName('policy_adherence')).toBe('Policy Adherence');
  });

  it('should identify completed sections correctly', () => {
    component.sectionCompletionStatus = new Map([
      ['header', true],
      ['key_responsibilities', false]
    ]);

    const headerSection: RenderedSection = {
      sectionType: 'header',
      title: 'Header',
      items: []
    };

    const krSection: RenderedSection = {
      sectionType: 'key_responsibilities',
      title: 'Key Responsibilities',
      items: []
    };

    expect(component.isSectionCompleted(headerSection)).toBe(true);
    expect(component.isSectionCompleted(krSection)).toBe(false);
  });

  it('should return current section type', () => {
    component.sections = mockSections;
    component.currentSectionIndex = 1;
    component.ngOnInit();

    expect(component.getCurrentSectionType()).toBe('key_responsibilities');
  });

  it('should return completion summary', () => {
    component.sections = mockSections;
    component.sectionCompletionStatus = new Map([
      ['header', true],
      ['key_responsibilities', true]
    ]);
    component.ngOnInit();

    expect(component.getCompletionSummary()).toBe('2 of 4 sections completed (50%)');
  });

  it('should update navigation when sections input changes', () => {
    component.sections = mockSections;
    component.ngOnInit();

    const initialTotal = component.totalSections;

    const newSections = [...mockSections, {
      sectionType: 'goals' as SectionType,
      title: 'Goals',
      items: []
    }];

    component.sections = newSections;
    component.ngOnChanges({
      sections: {
        currentValue: newSections,
        previousValue: mockSections,
        firstChange: false,
        isFirstChange: () => false
      }
    });

    expect(component.totalSections).toBeGreaterThan(initialTotal);
  });

  it('should recalculate progress when completion status changes', () => {
    component.sections = mockSections;
    component.sectionCompletionStatus = new Map();
    component.ngOnInit();

    expect(component.progressPercentage).toBe(0);

    const newStatus = new Map<SectionType, boolean>([['header', true]]);
    component.sectionCompletionStatus = newStatus;
    component.ngOnChanges({
      sectionCompletionStatus: {
        currentValue: newStatus,
        previousValue: new Map(),
        firstChange: false,
        isFirstChange: () => false
      }
    });

    expect(component.progressPercentage).toBeGreaterThan(0);
  });
});
