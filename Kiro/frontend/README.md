# Employee Appraisal System - Frontend

This project was generated with Angular CLI and uses Angular 18+ (compatible with Angular 21).

## Prerequisites

- Node.js 18+ and npm
- Angular CLI: `npm install -g @angular/cli`

## Installation

```bash
cd frontend
npm install
```

## Development Server

Run `npm start` or `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Build

Run `npm run build` or `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Project Structure

```
src/
├── app/
│   ├── core/           # Core services, guards, interceptors
│   ├── shared/         # Shared components, pipes, directives
│   ├── features/       # Feature modules (auth, employee, manager, hr, admin)
│   └── form-renderer/  # Dynamic form rendering module
├── assets/             # Static assets
└── styles.scss         # Global styles
```

## Backend API

The frontend connects to the Spring Boot backend at `http://localhost:8080/api`
