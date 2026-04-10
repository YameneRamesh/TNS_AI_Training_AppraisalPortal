import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { vi } from 'vitest';
import { HistoricalFormsViewerComponent } from './historical-forms-viewer.component';
import { AppraisalForm } from '../../core/models/appraisal.model';
import { provideZonelessChangeDetection } from '@angular/core';

describe('HistoricalFormsViewerComponent', () => {
  let component: HistoricalFormsViewerComponent;
  let fixture: ComponentFixture<HistoricalFormsViewerComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  const mockHistoricalForms: AppraisalForm[] = [
    {
      id: 1, cycleId: 1, cycleName: '2023-24', employeeId: 100, employeeName: 'John Doe',
      managerId: 200, managerName: 'Jane Manager', templateId: 1,
      status: 'REVIEWED_AND_COMPLETED', submittedAt: '2024-03-15T10:00:00Z',
      reviewedAt: '2024-03-20T14:30:00Z', pdfStoragePath: '/pdfs/form-1.pdf',
      createdAt: '2023-04-01T08:00:00Z', updatedAt: '2024-03-20T14:30:00Z'
    },
    {
      id: 2, cycleId: 2, cycleName: '2024-25', employeeId: 100, employeeName: 'John Doe',
      managerId: 200, managerName: 'Jane Manager', templateId: 2,
      status: 'SUBMITTED', submittedAt: '2025-03-10T09:00:00Z',
      createdAt: '2024-04-01T08:00:00Z', updatedAt: '2025-03-10T09:00:00Z'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoricalFormsViewerComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();

    fixture = TestBed.createComponent(HistoricalFormsViewerComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load historical forms on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/forms/history');
    expect(req.request.method).toBe('GET');
    req.flush(mockHistoricalForms);

    expect(component.historicalForms.length).toBe(2);
    expect(component.filteredForms.length).toBe(2);
    expect(component.loading).toBe(false);
  });

  it('should handle error when loading forms fails', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/forms/history');
    req.error(new ProgressEvent('error'));

    expect(component.error).toBeTruthy();
    expect(component.loading).toBe(false);
  });

  it('should filter forms by search term', () => {
    component.historicalForms = mockHistoricalForms;
    component.searchTerm = '2023';
    component.applyFilters();

    expect(component.filteredForms.length).toBe(1);
    expect(component.filteredForms[0].cycleName).toBe('2023-24');
  });

  it('should filter forms by status', () => {
    component.historicalForms = mockHistoricalForms;
    component.statusFilter = 'REVIEWED_AND_COMPLETED';
    component.applyFilters();

    expect(component.filteredForms.length).toBe(1);
    expect(component.filteredForms[0].status).toBe('REVIEWED_AND_COMPLETED');
  });

  it('should apply multiple filters', () => {
    component.historicalForms = mockHistoricalForms;
    component.searchTerm = '2024';
    component.statusFilter = 'SUBMITTED';
    component.applyFilters();

    expect(component.filteredForms.length).toBe(1);
    expect(component.filteredForms[0].cycleName).toBe('2024-25');
    expect(component.filteredForms[0].status).toBe('SUBMITTED');
  });

  it('should clear filters', () => {
    component.historicalForms = mockHistoricalForms;
    component.searchTerm = '2023';
    component.statusFilter = 'SUBMITTED';
    component.clearFilters();

    expect(component.searchTerm).toBe('');
    expect(component.statusFilter).toBe('');
    expect(component.filteredForms.length).toBe(2);
  });

  it('should return correct status label', () => {
    expect(component.getStatusLabel('NOT_STARTED')).toBe('Not Started');
    expect(component.getStatusLabel('DRAFT_SAVED')).toBe('Draft Saved');
    expect(component.getStatusLabel('SUBMITTED')).toBe('Submitted');
    expect(component.getStatusLabel('UNDER_REVIEW')).toBe('Under Review');
    expect(component.getStatusLabel('REVIEWED_AND_COMPLETED')).toBe('Completed');
  });

  it('should return correct status class', () => {
    expect(component.getStatusClass('SUBMITTED')).toBe('status-submitted');
    expect(component.getStatusClass('UNDER_REVIEW')).toBe('status-under-review');
    expect(component.getStatusClass('REVIEWED_AND_COMPLETED')).toBe('status-completed');
  });

  it('should navigate to form view with readonly and historical params', () => {
    component.viewForm(1);

    expect(router.navigate).toHaveBeenCalledWith(
      ['/employee/appraisal', 1],
      { queryParams: { readonly: true, historical: true } }
    );
  });

  it('should open PDF in new window', () => {
    vi.spyOn(window, 'open').mockImplementation(() => null);
    component.downloadPdf(1);

    expect(window.open).toHaveBeenCalledWith('/api/forms/1/pdf', '_blank');
  });

  it('should navigate to dashboard', () => {
    component.goToDashboard();

    expect(router.navigate).toHaveBeenCalledWith(['/employee/dashboard']);
  });

  it('should retry loading forms on error', () => {
    fixture.detectChanges();

    const req1 = httpMock.expectOne('/api/forms/history');
    req1.error(new ProgressEvent('error'));

    expect(component.error).toBeTruthy();

    component.loadHistoricalForms();

    const req2 = httpMock.expectOne('/api/forms/history');
    req2.flush(mockHistoricalForms);

    expect(component.error).toBeNull();
    expect(component.historicalForms.length).toBe(2);
  });
});
