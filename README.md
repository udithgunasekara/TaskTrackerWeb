# Task Tracker

![CI](https://github.com/udithgunasekara/TaskTrackerWeb/actions/workflows/ci.yml/badge.svg)

Task Tracker is a full-stack web application for managing tasks across a team. It provides robust JWT-based authentication and Role-Based Access Control (RBAC), with a React frontend that keeps the task list in sync after every change.

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Backend Framework** | Spring Boot 3.2 | Fast, opinionated enterprise-grade Java web application framework. |
| **Language** | Java 17 | LTS release with modern language features (Records, Text Blocks). |
| **Database** | MySQL 8 | Relational data persistence with robust ACID guarantees. |
| **Data Access** | Spring Data JPA / Hibernate | Reduces boilerplate database querying and ORM management. |
| **Security** | Spring Security + JWT | Stateless, secure authentication that scales without server sessions. |
| **Frontend Framework** | React 18 (Vite) | Lightning fast HMR and optimized production builds. |
| **State Management** | TanStack Query + Zustand | Query for server-state caching; Zustand for global client-state (Auth/Toasts). |
| **Styling** | Tailwind CSS v4 | Utility-first CSS for rapid, responsive UI development. |
| **Testing** | JUnit 5, Mockito, Vitest | Comprehensive backend unit/integration testing and fast frontend testing. |

## Architecture Overview

The backend is organized **package-by-feature** rather than package-by-layer: each business domain owns its full vertical slice (`controller` → `service`/`impl` → `repository` → `model`), with a shared `common` module for identity, security, and cross-cutting infrastructure.

- `common`: shared identity/auth — `config` (security, CORS, JPA auditing, admin seeding), `constant` (message-code enums), `controller` (`AuthController`), `exception` (`ModuleException`, `EntityNotFoundException`, `GlobalExceptionHandler`), `mapper`, `model` (`User`), `payload/{request,response}`, `repository` (`UserDao`), `security` (JWT filter, user details, entry point), `service`/`impl` (`AuthService`), `type` (`Role`)
- `task`: the Task feature — same shape (`constant`, `controller`, `mapper`, `model`, `payload`, `repository`, `service`/`impl`, `type`) scoped to `Task`

**Response envelope:** every endpoint returns `{ "status": "successful" | "unsuccessful", "results": [...] }`. Controllers wrap a service's typed return value at the HTTP boundary (`new ResponseEntityDto(false, data)`); services themselves keep returning plain typed DTOs, preserving unit-testability.

**Request Flow:**  
`Client` → `Controller` → `Service` → `Repository` → `MySQL`

## Getting Started

### Prerequisites
- Java 17
- Maven
- Node.js 20+
- MySQL 8
- Git

### Database Setup
Create a new MySQL schema named `tasktracker`:
```sql
CREATE DATABASE tasktracker;
```
Ensure you have a database user configured.

### Backend Setup
Navigate to the `Backend` directory:
1. Copy the example properties:
   `cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties`
2. Adjust your database credentials in `application-dev.properties`.
3. Run the backend:
   `mvn spring-boot:run`
   The backend will be available at `http://localhost:8082`.

### Frontend Setup
Navigate to the `Frontend` directory:
1. Copy the example environment variables:
   `cp .env.example .env`
2. Install dependencies:
   `npm ci`
3. Start the dev server:
   `npm run dev`
   The frontend will be available at `http://localhost:5173`.

### Default Admin
Upon backend startup, an admin account is automatically seeded if it doesn't exist.
- Email: `admin@example.com`
- Password: `admin`
*(Note: These can be configured in `application-dev.properties` via `app.admin.*`)*

### Running Tests
- **Backend:** `mvn verify`
- **Frontend:** `npm test`

## API Documentation

| Method | Endpoint | Description | Auth Required | Role |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Register new user | No | - |
| POST | `/api/v1/auth/login` | Authenticate & get JWT | No | - |
| GET | `/api/v1/auth/me` | Get current user details | Yes | Any |
| GET | `/api/v1/tasks` | List tasks (paginated, filterable) | Yes | Any (Admin sees all, User sees own) |
| GET | `/api/v1/tasks/{id}` | Get specific task | Yes | Any (Same scope as list) |
| POST | `/api/v1/tasks` | Create new task | Yes | Any |
| PUT | `/api/v1/tasks/{id}` | Update existing task | Yes | Any |
| DELETE | `/api/v1/tasks/{id}` | Delete task | Yes | Any |

You can explore the endpoints by importing the Postman collection located in the `/postman` folder into your Postman workspace.

## Keeping the UI in Sync
- After a create, update, or delete succeeds, the frontend invalidates the relevant TanStack Query cache keys (`['tasks']` and `['task', id]`), which triggers an automatic refetch so the list and detail views show the latest data.
- A short-lived toast confirms each action so the user gets immediate feedback.

## Design Decisions
- **MapStruct:** Used for robust, compile-time type-safe DTO mapping.
- **Interface+Impl Layering:** Enforces a strong boundary for business logic services.
- **404 vs 403:** The application returns a `404 Not Found` when a user attempts to access another user's task to prevent enumeration and leaking data existence.
- **Server-side Owner Scoping:** The `TaskService` directly enforces that non-admin users only ever fetch or manipulate tasks where `ownerId == currentUser.id`.
- **Client/Server State Split:** We explicitly separate server state (TanStack Query) from global client state (Zustand) for an optimal caching and architecture approach.
- **JWT Storage:** We store the JWT in `localStorage`. This is vulnerable to XSS but mitigates CSRF. Acknowledged tradeoff for architectural simplicity in this scale.
- **DDL Strategy:** Using `ddl-auto=update` for development simplicity.
- **Profiles:** We separate configs using Spring Profiles (dev, test, prod).

## Git Workflow
- We use the `main`/`dev`/`feature` branching model.
- `main` is production-ready. `dev` is the active integration branch.
- Feature branches prefix with `feat/` or `fix/`.
- We use **Conventional Commits** (e.g., `feat: ...`, `fix: ...`, `chore: ...`).

## Assumptions
- Registration only creates standard `USER` roles.
- The Admin role is seeded through application properties; users cannot promote themselves to Admin.
- Task status is a fixed Enum (`TODO`, `IN_PROGRESS`, `DONE`).
- A task's `dueDate` must be today or a future date upon creation.
- Email addresses must be unique.
- Deleting a task is a hard delete (no soft-delete mechanism).

## Known Limitations
- Logout is currently handled entirely client-side by discarding the JWT token. The token remains valid until expiration. Implementing a server-side blacklisting mechanism or short-lived tokens with refresh tokens would solve this.
- Profile editing (e.g. changing name or password) is not yet implemented.

## Future Improvements
- Refresh token rotation strategy.
- Implement Flyway for robust database migrations instead of `ddl-auto`.
- Build a user-list endpoint to allow Admins to select specific task owners from a dropdown.
- Add Optimistic UI updates for a snappier feeling.
- Implement soft delete functionality.
- Allow task comments and file attachments.
- Add API rate limiting.
- Complete E2E testing with Playwright or Cypress.

## Deployment
We provide a complete Dockerized setup using `docker-compose`.

1. Ensure Docker and Docker Compose are installed.
2. From the root directory, run:
   ```bash
   docker-compose up -d --build
   ```
3. The application will be available at:
   - Frontend: `http://localhost:80`
   - Backend API: `http://localhost:8082/api/v1`

### Images:
- **Backend**: Uses a multi-stage build (`maven` -> `eclipse-temurin:17-jre`) to produce and run the jar.
- **Frontend**: Uses a multi-stage build (`node:20` -> `nginx:alpine`) to build the static React bundle and serve it via Nginx, which also proxies `/api` requests to the backend.
