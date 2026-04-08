export interface EmployeeDashboard {
  currentForm?: AppraisalFormSummary;
  historicalForms: AppraisalFormSummary[];
}

export interface ManagerDashboard {
  ownForm?: AppraisalFormSummary;
  teamForms: TeamMemberForm[];
  pendingReviews: number;
  completedReviews: number;
  completionPercentage: number;
}

export interface HRDashboard {
  activeCycle?: CycleSummary;
  eligibleEmployees: number;
  pendingSubmissions: number;
  pendingReviews: number;
  completedAppraisals: number;
  departmentProgress: DepartmentProgress[];
}

export interface AppraisalFormSummary {
  id: number;
  cycleName: string;
  status: string;
  submittedAt?: string;
  reviewedAt?: string;
  pdfAvailable: boolean;
}

export interface TeamMemberForm {
  formId: number;
  employeeId: number;
  employeeName: string;
  designation: string;
  status: string;
  submittedAt?: string;
}

export interface CycleSummary {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
}

export interface DepartmentProgress {
  department: string;
  totalEmployees: number;
  completedAppraisals: number;
  completionPercentage: number;
}

// Re-export from appraisal.model for convenience
import { FormStatus } from './appraisal.model';
export { FormStatus };
