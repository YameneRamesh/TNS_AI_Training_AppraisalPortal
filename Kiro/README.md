# Employee Appraisal Cycle Management System

A role-based web platform for Think n Solutions (TnS) that manages the end-to-end annual performance appraisal process.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.4.1
- **Frontend**: Angular 21
- **Database**: MS SQL Server
- **Build Tool**: Maven

## Project Structure

```
com.tns.appraisal
├── config/          - Security, web, and async configuration
├── auth/            - Authentication and session management
├── user/            - User management and role assignments
├── cycle/           - Appraisal cycle lifecycle management
├── template/        - JSON schema template management
├── form/            - Employee self-appraisal operations
├── review/          - Manager review operations
├── notification/    - Asynchronous email notifications
├── pdf/             - PDF generation for completed appraisals
├── audit/           - Audit logging
└── dashboard/       - Role-specific dashboard data
```

## Prerequisites

- Java 21
- Maven 3.8+
- MS SQL Server

## Getting Started

1. **Configure Database**
   
   Update `src/main/resources/application.properties` with your MS SQL Server connection details:
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=appraisal_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

2. **Build the Project**
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the Application**
   
   The application will be available at `http://localhost:8080`

## Features

- Role-based access control (Employee, Manager, HR, Admin)
- Dynamic form rendering from JSON templates
- Full audit trail for all actions
- Asynchronous email notifications
- PDF generation for completed appraisals
- Historical form preservation with template versioning

## Development

- The application uses Spring Boot DevTools for hot reload during development
- JPA is configured with `ddl-auto=update` for automatic schema updates
- Session timeout is set to 15 minutes

## Testing

Run tests with:
```bash
mvn test
```

## License

Proprietary - Think n Solutions (TnS)
