# Task Tracker

![CI](https://github.com/udithgunasekara/TaskTrackerWeb/actions/workflows/ci.yml/badge.svg)

Task Tracker is a full-stack, real-time web application for managing tasks across a team. It provides robust JWT-based authentication, Role-Based Access Control (RBAC), and instantaneous real-time UI updates across connected clients using WebSockets.

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Backend Framework** | Spring Boot 3.2 | Fast, opinionated enterprise-grade Java web application framework. |
| **Language** | Java 17 | LTS release with modern language features (Records, Text Blocks). |
| **Database** | MySQL 8 | Relational data persistence with robust ACID guarantees. |
| **Data Access** | Spring Data JPA / Hibernate | Reduces boilerplate database querying and ORM management. |
| **Security** | Spring Security + JWT | Stateless, secure authentication that scales without server sessions. |
| **Real-time** | Spring WebSocket (STOMP) | Provides pub/sub messaging channels over WebSockets for live UI updates. |
| **Frontend Framework** | React 18 (Vite) | Lightning fast HMR and optimized production builds. |
| **State Management** | TanStack Query + Zustand | Query for server-state caching; Zustand for global client-state (Auth/Toasts). |
| **Styling** | Tailwind CSS v4 | Utility-first CSS for rapid, responsive UI development. |
| **Testing** | JUnit 5, Mockito, Vitest | Comprehensive backend unit/integration testing and fast frontend testing. |

## Architecture Overview

**Package Layout:**
- `controller`: REST endpoints & STOMP controllers
- `service`: Business logic interfaces and implementations (`impl`)
- `repository`: Spring Data JPA interfaces
- `entity`: Database models
- `dto`: Data Transfer Objects (Requests & Responses)
- `security`: JWT filters, authentication providers, user details
- `config`: Beans configuration (Security, WebSockets)
- `event`: Application event publishers and listeners
- `exception`: Global exception handler and custom exceptions

**Request Flow:**  
`Client` → `Controller` → `Service` → `Repository` → `MySQL`

**WebSocket Flow:**  
`Service` → `EventPublisher` → `STOMP Broker` → `STOMP Topics/Queues` → `Client`

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

## Real-Time Design
- **Authentication:** STOMP connections are secured by passing the JWT in the `CONNECT` frame.
- **Channels:** 
  - `/user/queue/tasks`: Users subscribe to this to receive updates about their own tasks.
  - `/topic/admin/tasks`: Admins subscribe to this to receive updates about ALL tasks across the system.
- **Admin Verification:** The system verifies the user's role at the time of subscription to prevent unauthorized access to the admin topic.
- **Duplicate Prevention:** Admins receive messages on BOTH channels. Duplicate handling is managed gracefully by TanStack Query's automated deduplication and invalidation logic.

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
*(Filled in during the Dockerization phase)*
