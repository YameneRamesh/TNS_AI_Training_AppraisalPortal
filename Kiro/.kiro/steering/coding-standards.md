---
inclusion: auto
---

# Coding Standards

## Backend (Java / Spring Boot)
- Java 17+ features encouraged (records, sealed classes, pattern matching)
- Use constructor injection (no @Autowired on fields)
- Service layer handles business logic; controllers are thin
- Use `@Transactional` appropriately (readOnly for queries)
- Validate inputs with Jakarta Bean Validation annotations
- Custom exceptions extend RuntimeException with meaningful messages
- Use SLF4J logging with appropriate levels (INFO for business events, DEBUG for details, ERROR for failures)
- All new endpoints must have OpenAPI/Swagger annotations

## Frontend (Angular / TypeScript)
- Use Angular 17+ with TypeScript strict mode
- Standalone components preferred over NgModules
- Use Angular Router for navigation with lazy-loaded routes
- State management: Angular Signals or NgRx for complex state
- API calls via HttpClient with interceptors for auth/error handling
- Use Angular Material for consistent UI components
- Form validation with Reactive Forms and custom validators
- Responsive design with Angular Flex Layout or CSS Grid
- Use Angular CLI for generating components, services, and modules

## Database
- Schema changes managed manually by the developer (no Flyway)
- Use JSONB for flexible structured data in PostgreSQL
- Index foreign keys and frequently queried columns
- Add table and column comments for documentation
- Use ENUM types or CHECK constraints for status fields

## Testing
- Backend: JUnit 5 + Mockito for unit tests, Spring Boot Test for integration
- Frontend: Karma/Jasmine or Jest for unit tests, Cypress for e2e
- Property-based testing with jqwik (Java) or fast-check (TypeScript/Angular)

## Security
- Never log PII or sensitive data
- Encrypt PII at rest using EncryptionUtil
- Validate and sanitize all user inputs
- Use parameterized queries (JPA handles this)
- CORS configured per environment
