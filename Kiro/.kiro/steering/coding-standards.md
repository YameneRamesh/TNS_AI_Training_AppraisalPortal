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

## Frontend (Angular 21 / TypeScript)

### Core Principles
- TypeScript strict mode always enabled
- Standalone components only — no NgModules
- Zoneless change detection by default (Angular 21 default); do NOT add `provideZoneChangeDetection()` unless a third-party lib requires Zone.js
- All new components use `ChangeDetectionStrategy.OnPush`
- Use the `inject()` function for dependency injection instead of constructor injection in components
- Use `input()`, `output()`, `model()` signal-based APIs instead of `@Input()` / `@Output()` decorators
- Use `viewChild()` / `contentChild()` signal queries instead of `@ViewChild` / `@ContentChild`

### Reactivity & State
- Use Angular Signals (`signal()`, `computed()`, `effect()`) as the primary reactivity model
- Use `linkedSignal()` for derived writable state
- Use `toSignal()` / `toObservable()` to bridge RxJS where needed
- Reserve RxJS Observables for complex async flows (HTTP streams, multi-source merges)
- NgRx only for cross-feature shared state; prefer Signals for local/component state
- Never use `async` pipe with zoneless — convert to signals via `toSignal()`

### Forms
- Use Signal Forms (`@angular/forms/signals`) for all new forms — experimental but preferred for new projects
- Signal Forms pattern:
  ```ts
  private readonly data = signal({ field: '' });
  protected readonly myForm = form(this.data, form => {
    required(form.field, { message: 'Field is required' });
  });
  ```
- Fall back to Reactive Forms only when integrating with existing code or third-party form libraries
- Never use Template-driven forms
- Use `@angular/forms/signals/compat` (`compatForm`) when mixing signal forms with existing reactive controls

### Routing
- All routes lazy-loaded using `loadComponent` (not `loadChildren` with modules)
- Use typed router with `withComponentInputBinding()` to bind route params directly to `input()` signals
- Use `Router` scroll option per-navigation where needed: `router.navigateByUrl('/', { scroll: 'manual' })`
- Use `@defer` blocks for route-level and viewport-triggered lazy loading:
  ```html
  @defer (on viewport({ rootMargin: '50px' })) { <heavy-component /> }
  ```

### Templates
- Use built-in control flow (`@if`, `@for`, `@switch`, `@defer`) — never `*ngIf`, `*ngFor`, `*ngSwitch`
- Use class/style bindings directly — never `NgClass` or `NgStyle`
- RegExp literals are supported in templates (Angular 21+)
- Run optional migrations when updating:
  ```
  ng g @angular/core:ngclass-to-class-migration
  ng g @angular/core:ngstyle-to-style-migration
  ng g @angular/core:common-to-standalone
  ```

### HTTP
- `HttpClient` is provided in the root injector by default in Angular 21 — do NOT add `provideHttpClient()` unless registering interceptors
- Register interceptors explicitly when needed:
  ```ts
  provideHttpClient(withInterceptors([authInterceptor]))
  ```
- Use functional interceptors (not class-based)
- Use `httpResource()` for signal-based reactive data fetching where applicable

### Styles & Accessibility
- SCSS for all component and global styles
- Use Angular Material 21 for UI components
- Use `@angular/aria` (developer preview) for headless accessible primitives (Accordion, Menu, Tabs, ListBox, etc.) when Angular Material doesn't cover the pattern
- Apply `class` and `style` bindings directly; avoid `NgClass`/`NgStyle`
- Use `ExperimentalIsolatedShadowDom` encapsulation only for truly isolated widget components (not production-critical paths yet)

### Testing
- Vitest is the default test runner (Angular 21 default) — use `@angular/build:unit-test` builder
- Minimal `angular.json` test config:
  ```json
  "test": { "builder": "@angular/build:unit-test" }
  ```
- Use `await fixture.whenStable()` instead of `fixture.detectChanges()` in zoneless tests
- Migrate Jasmine tests with: `ng generate refactor-jasmine-vitest`
- Cypress for E2E tests
- Property-based testing with fast-check

### Angular CLI Usage (Mandatory)
Always use Angular CLI commands to scaffold — never create Angular files manually.
Run all `ng` commands from the `Kiro/frontend` directory.

| Artifact | Command |
|---|---|
| New project | `ng new <name> --style=scss --strict` |
| Component | `ng generate component <path/name>` |
| Service | `ng generate service <path/name>` |
| Guard | `ng generate guard <path/name>` |
| Interceptor | `ng generate interceptor <path/name>` |
| Pipe | `ng generate pipe <path/name>` |
| Directive | `ng generate directive <path/name>` |
| Interface/Model | `ng generate interface <path/name> <type>` |
| Enum | `ng generate enum <path/name>` |
| Vitest config | `ng generate config vitest` |

- Always pass `--dry-run` first to preview generated files before committing
- Use `--skip-tests` only when explicitly told to skip test generation
- Generated files must not be moved or renamed manually after creation
- Use `ng version` to verify installed Angular package versions before scaffolding

## Database
- Schema changes managed manually by the developer (no Flyway)
- Use JSONB for flexible structured data in PostgreSQL
- Index foreign keys and frequently queried columns
- Add table and column comments for documentation
- Use ENUM types or CHECK constraints for status fields

## Testing
- Backend: JUnit 5 + Mockito for unit tests, Spring Boot Test for integration
- Frontend: Vitest (default in Angular 21) for unit tests, Cypress for e2e
- Property-based testing with jqwik (Java) or fast-check (TypeScript/Angular)

## Security
- Never log PII or sensitive data
- Encrypt PII at rest using EncryptionUtil
- Validate and sanitize all user inputs
- Use parameterized queries (JPA handles this)
- CORS configured per environment
