# Historical Forms Viewer Component

## Overview

The Historical Forms Viewer component provides employees with a comprehensive view of all their past appraisal forms. This component implements Requirements 7.2 and 13 from the specification, ensuring employees can access and review their historical performance appraisals.

## Features

### Core Functionality
- **Display Historical Forms**: Shows all completed appraisal forms from previous cycles
- **Read-Only Access**: All historical forms are displayed in read-only mode
- **PDF Download**: Allows downloading completed appraisals as PDF documents
- **Filtering**: Search by cycle name and filter by status
- **Detailed View**: Navigate to full form view for any historical appraisal

### User Interface
- Clean, table-based layout with Material Design components
- Responsive design that works on desktop and mobile devices
- Status chips with color coding for quick visual identification
- Action buttons for viewing forms and downloading PDFs
- Empty state messaging when no historical forms exist
- Loading and error states with retry functionality

## Requirements Validation

### Requirement 7.2: Employee Dashboard
✅ Display list of appraisal forms from previous cycles
✅ Each form accessible in read-only mode

### Requirement 13: Historical Appraisal Access
✅ 13.1: System preserves all appraisal forms after cycle closure
✅ 13.2: Historical forms rendered in read-only mode
✅ 13.3: Employee can access only their own historical forms

## API Integration

### Endpoint Used
```
GET /api/forms/history
```

**Response Format:**
```typescript
AppraisalForm[] = [
  {
    id: number;
    cycleId: number;
    cycleName: string;
    employeeId: number;
    managerId: number;
    managerName: string;
    status: FormStatus;
    submittedAt?: string;
    reviewedAt?: string;
    pdfStoragePath?: string;
    createdAt: string;
    updatedAt: string;
  }
]
```

## Component Structure

### Template Sections
1. **Header**: Title and description
2. **Filters Card**: Search and status filter controls
3. **Forms Table**: Displays historical forms with columns:
   - Appraisal Cycle (with creation date)
   - Reviewed By (manager name)
   - Status (with color-coded chip)
   - Submitted On (date and time)
   - Reviewed On (date and time)
   - Actions (view and download buttons)
4. **Empty State**: Shown when no forms match filters
5. **Loading State**: Spinner during data fetch
6. **Error State**: Error message with retry button

### Key Methods

#### `loadHistoricalForms()`
Fetches historical forms from the API endpoint.

#### `applyFilters()`
Filters the forms based on search term and status filter.

#### `clearFilters()`
Resets all filters to show all historical forms.

#### `viewForm(formId: number)`
Navigates to the form detail view with readonly and historical query parameters.

#### `downloadPdf(formId: number)`
Opens the PDF download endpoint in a new browser tab.

## Routing

The component is accessible at:
```
/employee/history
```

Route configuration:
```typescript
{
  path: 'history',
  loadComponent: () => import('./historical-forms-viewer.component')
    .then(m => m.HistoricalFormsViewerComponent),
  canActivate: [authGuard, roleGuard],
  data: { roles: ['EMPLOYEE'] }
}
```

## Integration with Employee Dashboard

The employee dashboard includes a "View All History" button in the Historical Appraisals card that navigates to this component for a more detailed view.

## Status Display

The component uses color-coded chips to display form status:

| Status | Color | Label |
|--------|-------|-------|
| SUBMITTED | Blue | Submitted |
| UNDER_REVIEW | Purple | Under Review |
| REVIEW_DRAFT_SAVED | Purple | Review in Progress |
| REVIEWED_AND_COMPLETED | Green | Completed |

## Filtering Capabilities

### Search Filter
- Searches through cycle names (case-insensitive)
- Real-time filtering as user types
- Example: "2024" will show all cycles containing "2024"

### Status Filter
- Dropdown with predefined status options
- Options: All Statuses, Submitted, Completed
- Combines with search filter for refined results

## Testing

The component includes comprehensive unit tests covering:
- Component initialization
- Data loading and error handling
- Filter functionality (search and status)
- Navigation to form view
- PDF download
- Status label and class mapping
- Retry functionality after errors

Run tests with:
```bash
ng test
```

## Accessibility

- Proper ARIA labels on interactive elements
- Keyboard navigation support
- Screen reader friendly status announcements
- High contrast color schemes for status chips
- Tooltip descriptions for icon buttons

## Future Enhancements

Potential improvements for future iterations:
- Export multiple forms as a batch
- Advanced filtering (date ranges, manager)
- Sorting by column headers
- Pagination for large datasets
- Comparison view between multiple cycles
- Print-friendly view
- Email sharing functionality

## Dependencies

- Angular 21
- Angular Material (Card, Table, Button, Icon, Chips, Form Fields, Progress Spinner)
- Angular Router
- HttpClient for API communication

## Related Components

- `EmployeeDashboardComponent`: Main dashboard with summary view
- `SelfAppraisalFormComponent`: Form detail view (read-only for historical)
- `AuthGuard`: Ensures user is authenticated
- `RoleGuard`: Ensures user has EMPLOYEE role
