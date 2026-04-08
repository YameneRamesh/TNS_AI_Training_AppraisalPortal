---
inclusion: auto
---

# Coding Standards & Guidelines

## Production-Ready Code Principles

- Write clean, maintainable, and well-documented code
- Follow SOLID principles and design patterns
- Implement proper error handling and logging
- Use meaningful variable and function names
- Add comprehensive comments for complex logic
- Ensure code is optimized for performance
- Use environment-specific configurations (dev, staging, production)
- Add appropriate validation at all layers (frontend, backend, database)

## Backend (Java / Spring Boot)

### Technical Standards
- Java 17+ features encouraged (records, sealed classes, pattern matching)
- Use constructor injection (no @Autowired on fields)
- Service layer handles business logic; controllers are thin
- Use `@Transactional` appropriately (readOnly for queries)
- Validate inputs with Jakarta Bean Validation annotations
- Custom exceptions extend RuntimeException with meaningful messages
- Use SLF4J logging with appropriate levels (INFO for business events, DEBUG for details, ERROR for failures)
- All new endpoints must have OpenAPI/Swagger annotations

### Folder Structure
- Follow package-by-feature structure: `com.tns.appraisal.<feature>`
- Each feature should contain: controller, service, repository, dto, mapper, entity
- Keep common/shared code in `common` package
- Configuration files in `config` package
- Exception handling in `exception` package

### Testing Requirements
- Write unit tests for EVERY service method
- Write unit tests for EVERY controller endpoint
- Write unit tests for EVERY mapper
- Write repository tests using @DataJpaTest
- Use JUnit 5 + Mockito for unit tests, Spring Boot Test for integration
- Property-based testing with jqwik for complex scenarios
- Aim for minimum 80% code coverage
- Use meaningful test names: `should<ExpectedBehavior>When<Condition>`
- Test happy paths AND edge cases
- Mock external dependencies
- Run tests after implementation: `mvn test`
- ALWAYS run tests after completing each module
- Fix any failing tests before moving to next task

## Frontend (Angular / TypeScript)

### Technical Standards
- Use Angular 17+ with TypeScript strict mode
- Standalone components preferred over NgModules
- Use Angular Router for navigation with lazy-loaded routes
- State management: Angular Signals or NgRx for complex state
- API calls via HttpClient with interceptors for auth/error handling
- Use Angular Material for consistent UI components
- Form validation with Reactive Forms and custom validators
- Responsive design with Angular Flex Layout or CSS Grid
- Use Angular CLI for generating components, services, and modules

### Folder Structure
- Follow Angular style guide and best practices
- Structure: `features/<feature-name>/{components, services, models, guards}`
- Shared components in `shared/components`
- Core services in `core/services`
- Models in respective feature or core/models
- Keep components focused and single-responsibility

### Testing Requirements
- Write unit tests for EVERY component
- Write unit tests for EVERY service
- Write unit tests for EVERY guard and interceptor
- Use Karma/Jasmine or Jest for unit tests, Cypress for e2e
- Property-based testing with fast-check for complex scenarios
- Test component logic, not DOM manipulation
- Mock HTTP calls and dependencies
- Aim for minimum 80% code coverage
- Run tests after implementation: `ng test --watch=false`
- ALWAYS run tests after completing each module
- Fix any failing tests before moving to next task

### Icons and Visual Assets
- Use Material Icons from: https://fonts.google.com/icons
- For Angular Material: Use `<mat-icon>` component
- Example: `<mat-icon>home</mat-icon>`
- For custom SVG icons: Register with MatIconRegistry
- Store custom SVG icons in `frontend/src/assets/icons/`
- Optimize SVGs before adding to project
- Keep icon usage consistent across the application
- Use semantic icon names that match their purpose

## Database
- Schema changes managed manually by the developer (no Flyway)
- Use JSONB for flexible structured data in PostgreSQL
- Index foreign keys and frequently queried columns
- Add table and column comments for documentation
- Use ENUM types or CHECK constraints for status fields
- Optimize database queries (avoid N+1 problems)
- Use database indexes appropriately

## Security
- Never log PII or sensitive data
- Encrypt PII at rest using EncryptionUtil
- Validate and sanitize all user inputs
- Use parameterized queries (JPA handles this)
- CORS configured per environment
- Never expose sensitive data in logs or error messages
- Use parameterized queries to prevent SQL injection
- Implement proper authentication and authorization
- Use HTTPS for all communications
- Follow principle of least privilege
- Keep dependencies updated

## Development Workflow

1. Understand the requirement
2. Design the solution (classes, interfaces, data flow)
3. Create folder structure if needed
4. Implement backend code (entity → repository → service → controller)
5. Write backend unit tests and run them
6. Implement frontend code (model → service → component)
7. Write frontend unit tests and run them
8. Integrate and test end-to-end
9. Review code quality checklist
10. Document any special considerations

## Code Quality Checklist

Before completing any task, verify:
- [ ] Code follows project structure
- [ ] All methods have proper documentation
- [ ] Error handling is implemented
- [ ] Input validation is in place
- [ ] Unit tests are written and passing
- [ ] Code coverage meets minimum threshold (80%)
- [ ] No console.log or debug statements in production code
- [ ] No hardcoded values (use configuration)
- [ ] Proper TypeScript/Java types are used
- [ ] Code is formatted according to project standards
- [ ] Security vulnerabilities are addressed
- [ ] Test results and coverage metrics reported

## Performance Considerations

- Use lazy loading for Angular modules
- Implement pagination for large datasets
- Cache frequently accessed data
- Use async operations where appropriate
- Minimize bundle size (tree shaking, code splitting)
