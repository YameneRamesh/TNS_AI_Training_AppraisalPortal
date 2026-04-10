export interface AppraisalTemplate {
  id: number;
  version: string;
  schemaJson: string;
  isActive: boolean;
  createdBy: number;
  createdAt: string;
}

export interface AppraisalCycle {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  templateId: number;
  status: CycleStatus;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export type CycleStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED';

export interface AppraisalForm {
  id: number;
  cycleId: number;
  cycleName?: string;
  employeeId: number;
  employeeName?: string;
  designation?: string;
  managerId: number;
  managerName?: string;
  backupReviewerId?: number;
  backupReviewerName?: string;
  templateId: number;
  status: FormStatus;
  formData?: FormData;
  submittedAt?: string;
  reviewStartedAt?: string;
  reviewedAt?: string;
  pdfStoragePath?: string;
  createdAt: string;
  updatedAt: string;
}

export type FormStatus = 
  | 'NOT_STARTED' 
  | 'DRAFT_SAVED' 
  | 'SUBMITTED' 
  | 'UNDER_REVIEW' 
  | 'REVIEW_DRAFT_SAVED' 
  | 'REVIEWED_AND_COMPLETED';

export interface FormData {
  header?: HeaderData;
  keyResponsibilities?: ResponsibilityItem[];
  idp?: IdpItem[];
  policyAdherence?: PolicyAdherenceData;
  goals?: GoalItem[];
  nextYearGoals?: string;
  overallEvaluation?: OverallEvaluationData;
  signature?: SignatureData;
}

export interface HeaderData {
  dateOfHire?: string;
  dateOfReview?: string;
  reviewPeriod?: string;
  typeOfReview?: string;
}

export interface ResponsibilityItem {
  itemId: string;
  selfComment?: string;
  selfRating?: CompetencyRating;
  managerComment?: string;
  managerRating?: CompetencyRating;
}

export interface IdpItem {
  itemId: string;
  selfComment?: string;
  selfRating?: CompetencyRating;
  managerComment?: string;
  managerRating?: CompetencyRating;
}

export interface GoalItem {
  itemId: string;
  selfComment?: string;
  selfRating?: CompetencyRating;
  managerComment?: string;
  managerRating?: CompetencyRating;
}

export interface PolicyAdherenceData {
  hrPolicy?: { managerRating?: number };
  availability?: { managerRating?: number };
  additionalSupport?: { managerRating?: number };
  managerComments?: string;
}

export interface OverallEvaluationData {
  managerComments?: string;
  teamMemberComments?: string;
}

export interface SignatureData {
  preparedBy?: string;
  reviewedBy?: string;
  teamMemberAcknowledgement?: string;
}

export type CompetencyRating = 'Excels' | 'Exceeds' | 'Meets' | 'Developing';
