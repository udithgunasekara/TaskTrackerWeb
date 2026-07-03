# Task Tracker — Full Implementation Guide

> **Purpose:** This document is a complete, task-by-task implementation specification for a full-stack Task Tracker application (take-home assignment). It is written so that an AI coding agent (or a developer) can execute it top-to-bottom. Each task maps to **one git branch and one PR**, so progress and commits are easy to manage.

---

## 1. Project Overview

Build a full-stack Task Tracker with:

- **Backend:** Spring Boot 3.x (Java 17), layered architecture: `controller → service (interface) → serviceImpl → repository`, with `entity`, `dto`, `mapper` (MapStruct), Hibernate/Spring Data JPA
- **Database:** MySQL 8 (local instance managed via MySQL Workbench for development; H2 in-memory for tests/CI)
- **Auth:** JWT (stateless), BCrypt password hashing, Role-Based Access Control (USER, ADMIN)
- **Real-time:** Spring WebSocket + STOMP, JWT-authenticated, role-aware topics
- **Frontend:** React 18 + Vite, TanStack Query (server state), Zustand (auth state), Tailwind CSS, React Router, react-hook-form + zod, @stomp/rx-stomp
- **CI:** GitHub Actions (build, lint, test) on push + PR to `main` and `dev`
- **Deployment:** AWS EC2 via Docker Compose (backend + frontend/Nginx + MySQL)

### Functional requirements checklist (must all be satisfied)

| # | Requirement |
|---|-------------|
| F1 | User registration |
| F2 | User login (JWT issued) |
| F3 | RBAC: USER (own tasks only), ADMIN (all tasks) |
| F4 | Task CRUD: create, get by ID, list, update, delete |
| F5 | Task fields: title, description, status, dueDate, owner |
| F6 | Task list: pagination + filter by status + filter by owner |
| F7 | Validation on all requests, meaningful errors, correct HTTP codes |
| F8 | MySQL persistence |
| F9 | Real-time task updates over WebSocket (no page refresh) |
| F10 | Frontend: register/login/logout, task list/detail/create/update/delete, filters, pagination, live updates |
| F11 | Automated tests (backend priority) |
| F12 | README (setup, design decisions, assumptions, future improvements) |
| F13 | Postman collection + environment |
| F14 | CI pipeline (install deps, lint, test) on push + PR |
| F15 | Clean git history with feature branches |

---

## 2. Git Workflow (applies to EVERY task)

### Branch model

```
main   ← protected, release-ready only (final merge at the end)
└── dev ← integration branch (all feature PRs target dev)
     ├── feat/<task-name>
     ├── fix/<task-name>
     └── hotfix/<task-name>
```

### Rules

1. First commit goes directly to `main` (Task 01 only). Then create `dev` from `main`. All subsequent work happens on feature branches off `dev`.
2. One task in this document = one branch = one PR into `dev`.
3. Commit messages follow **Conventional Commits**:
   - `feat: add jwt authentication filter`
   - `fix: force owner scope on task list for non-admins`
   - `test: add cross-user access denial tests`
   - `docs: add architecture overview to readme`
   - `chore: configure github actions ci`
4. Commit in small, logical units (2–6 commits per task is typical). Never one giant commit per task.
5. Merge PRs into `dev` only when CI is green. Delete the branch after merge.
6. Final step of the project: single PR `dev → main`.

Each task below specifies its **branch name** and **suggested commits**.

---

## 3. Global Contracts (implement exactly as specified)

### 3.1 Data model

**users**

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt hash) |
| full_name | VARCHAR(100) | NOT NULL |
| role | VARCHAR(20) | NOT NULL, enum: `USER`, `ADMIN` |
| created_at | TIMESTAMP | auditing |

**tasks**

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| title | VARCHAR(150) | NOT NULL |
| description | TEXT | nullable |
| status | VARCHAR(20) | NOT NULL, enum: `TODO`, `IN_PROGRESS`, `DONE` |
| due_date | DATE | nullable |
| owner_id | BIGINT | FK → users.id, NOT NULL, `@ManyToOne(fetch = LAZY)` |
| created_at | TIMESTAMP | auditing |
| updated_at | TIMESTAMP | auditing |

### 3.2 REST API contract

Base path: `/api/v1`

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/auth/register` | Public | — | Register; always creates role USER |
| POST | `/auth/login` | Public | — | Login; returns JWT + user info |
| GET | `/auth/me` | JWT | any | Current user info |
| POST | `/tasks` | JWT | any | Create task (owner = current user) |
| GET | `/tasks/{id}` | JWT | USER: own only; ADMIN: any | Get task by ID |
| GET | `/tasks` | JWT | USER: own only; ADMIN: all | Paginated list with filters |
| PUT | `/tasks/{id}` | JWT | USER: own only; ADMIN: any | Update task |
| DELETE | `/tasks/{id}` | JWT | USER: own only; ADMIN: any | Delete task |

**GET /tasks query parameters:**

| Param | Type | Default | Notes |
|-------|------|---------|-------|
| page | int | 0 | zero-based |
| size | int | 10 | max 50 |
| status | enum | — | optional: TODO / IN_PROGRESS / DONE |
| ownerId | long | — | optional; **ignored for non-admins** (server forces owner = current user) |
| sort | string | `createdAt,desc` | Spring format |

**Security decision (document in README):** when a USER requests a task they don't own → return **404** (not 403) to avoid leaking task existence. Admin bypasses ownership checks everywhere.

### 3.3 DTOs (Java records)

```
dto/request/RegisterRequest(email, password, fullName)
dto/request/LoginRequest(email, password)
dto/request/TaskRequest(title, description, status, dueDate)
dto/response/AuthResponse(token, id, email, fullName, role)
dto/response/UserResponse(id, email, fullName, role)
dto/response/TaskResponse(id, title, description, status, dueDate, owner: UserResponse, createdAt, updatedAt)
dto/response/PageResponse<T>(content, page, size, totalElements, totalPages, last)
dto/response/ErrorResponse(timestamp, status, error, message, path, fieldErrors: Map<String,String>)
```

Validation on requests:
- `RegisterRequest`: email `@Email @NotBlank`; password `@NotBlank @Size(min=8, max=72)`; fullName `@NotBlank @Size(max=100)`
- `TaskRequest`: title `@NotBlank @Size(max=150)`; description `@Size(max=2000)`; status `@NotNull`; dueDate optional (`@FutureOrPresent` on create — document as an assumption)

### 3.4 Error envelope (ALL errors use this shape)

```json
{
  "timestamp": "2026-07-03T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/tasks",
  "fieldErrors": { "title": "must not be blank" }
}
```

Status code mapping:
- 400 — validation failure, malformed body, invalid enum value
- 401 — missing/invalid/expired JWT, bad login credentials
- 403 — authenticated but role not permitted (rare given the 404 decision)
- 404 — resource not found OR owned by another user (for non-admins)
- 409 — duplicate email on registration

### 3.5 WebSocket contract

- STOMP endpoint: `/ws` (SockJS optional; plain WebSocket is fine)
- Client sends JWT in STOMP CONNECT header: `Authorization: Bearer <token>`
- Server destinations:
  - `/user/queue/tasks` — per-user queue; the task **owner** receives events for their tasks
  - `/topic/admin/tasks` — all task events; **only ADMIN subscriptions allowed** (enforce in an inbound channel interceptor)
- Event payload:

```json
{ "type": "CREATED", "task": { ...TaskResponse } }
```

`type` ∈ `CREATED | UPDATED | DELETED` (for DELETED, `task` may contain only `id`).

### 3.6 Backend package structure

```
com.tasktracker
├── config/        SecurityConfig, WebSocketConfig, CorsConfig, JpaAuditingConfig
├── controller/    AuthController, TaskController
├── dto/
│   ├── request/
│   └── response/
├── entity/        User, Task, Role (enum), TaskStatus (enum)
├── mapper/        UserMapper, TaskMapper (MapStruct)
├── repository/    UserRepository, TaskRepository
├── service/       AuthService, TaskService (interfaces)
│   └── impl/      AuthServiceImpl, TaskServiceImpl
├── security/      JwtUtil, JwtAuthFilter, UserDetailsServiceImpl, SecurityUser
├── exception/     GlobalExceptionHandler, ResourceNotFoundException, DuplicateEmailException
└── event/         TaskEventPublisher, TaskEvent, TaskEventType (enum)
```

### 3.7 Frontend structure

```
frontend/src
├── api/           axiosClient.ts, authApi.ts, taskApi.ts
├── stores/        authStore.ts (Zustand, persisted)
├── hooks/         useTasks.ts, useTaskMutations.ts, useRealtime.ts
├── components/    TaskList.tsx, TaskCard.tsx, TaskFormModal.tsx, Filters.tsx,
│                  Pagination.tsx, ProtectedRoute.tsx, Navbar.tsx, Toast/…
├── pages/         LoginPage.tsx, RegisterPage.tsx, DashboardPage.tsx, TaskDetailPage.tsx
├── ws/            stompClient.ts
├── types/         index.ts (Task, User, PageResponse, TaskEvent, enums)
└── App.tsx, main.tsx, index.css
```

### 3.8 Environment / config files

Backend — four property files (see Task 02 for full contents):
`application.properties`, `application-dev.properties` (gitignored; commit `.example`), `application-prod.properties`, `application-test.properties`.

Frontend — `.env.example` (commit) / `.env` (gitignored):

```
VITE_API_BASE_URL=http://localhost:8082/api/v1
VITE_WS_URL=ws://localhost:8082/ws
```

---

## 4. Task Breakdown

> Execute tasks strictly in order. Each task states: branch, goal, steps, key code, acceptance criteria, and suggested commits. Do not start a task until the previous task's PR is merged into `dev` with CI green (Task 01–03 bootstrap CI itself).

---

### TASK 01 — Repository & Git bootstrap

**Branch:** direct commit to `main`, then create `dev`
**Estimated effort:** 15 min

**Steps:**
1. Create a new GitHub repository `task-tracker` (private, grant reviewer access later).
2. On `main`, create:
   - `README.md` — title + one-line description + "Work in progress" note (full README comes in Task 15)
   - `.gitignore` — combined Java/Maven + Node + IDE + OS ignores. Must include: `target/`, `node_modules/`, `dist/`, `.env`, `backend/src/main/resources/application-dev.properties`, `.idea/`, `.vscode/`, `*.iml`, `.DS_Store`
3. Commit and push to `main`.
4. Create `dev` branch from `main` and push it.
5. (If repo settings allow) protect `main`: require PR + passing checks.

**Acceptance criteria:**
- Repo has `main` and `dev`, both containing README stub and .gitignore.

**Suggested commits (on main):**
- `chore: initialize repository with readme and gitignore`

---

### TASK 02 — Backend + frontend scaffold, configuration profiles

**Branch:** `feat/project-setup` (off `dev`)
**Estimated effort:** 1 hr

#### 2.1 Backend scaffold

Generate a Maven Spring Boot 3.x project (Java 17) in `backend/` with dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-websocket`
- `mysql-connector-j` (runtime)
- `lombok` (provided)
- `mapstruct` 1.5.5.Final + `mapstruct-processor`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` — version 0.12.5
- Test scope: `spring-boot-starter-test`, `spring-security-test`, `h2`

**CRITICAL pom.xml detail — annotation processor ordering** (Lombok + MapStruct break silently if wrong). In `maven-compiler-plugin`:

```xml
<annotationProcessorPaths>
  <path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
  </path>
  <path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>0.2.0</version>
  </path>
  <path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
  </path>
</annotationProcessorPaths>
```

**Backend linting (required — the assignment's CI must "run linting" for the whole project, not just the frontend).** Add the Spotless plugin to `pom.xml`:

```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>2.43.0</version>
  <configuration>
    <java>
      <googleJavaFormat/>
      <removeUnusedImports/>
    </java>
  </configuration>
</plugin>
```

Usage: `mvn spotless:apply` formats the code (run before committing); `mvn spotless:check` fails the build on violations (CI runs this). Run `spotless:apply` once now so the scaffold passes.

Main class: `com.tasktracker.TaskTrackerApplication`.

#### 2.2 Property files

`backend/src/main/resources/application.properties`:
```properties
spring.profiles.active=dev
server.port=8082
spring.jpa.open-in-view=false
spring.jackson.serialization.write-dates-as-timestamps=false
```

`application-dev.properties` (DO NOT COMMIT — gitignored; commit an identical `application-dev.properties.example` instead):
```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/tasktracker?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.jwt.secret=dev-only-secret-please-change-0123456789abcdef0123456789abcdef
app.jwt.expiration-ms=86400000
app.cors.allowed-origins=http://localhost:5173
app.admin.email=admin@tasktracker.local
app.admin.password=Admin@12345
app.admin.full-name=System Admin
```

`application-prod.properties` (committed — values come from environment):
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=86400000
app.cors.allowed-origins=${FRONTEND_URL}
app.admin.email=${ADMIN_EMAIL}
app.admin.password=${ADMIN_PASSWORD}
app.admin.full-name=System Admin
```
*(Note: `ddl-auto=update` in prod is a documented pragmatic choice for this assignment; list "Flyway migrations + validate" under Future Improvements.)*

`application-test.properties` (committed):
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
app.jwt.secret=test-secret-0123456789abcdef0123456789abcdef0123456789abcdef
app.jwt.expiration-ms=3600000
app.cors.allowed-origins=http://localhost:5173
app.admin.email=admin@test.local
app.admin.password=Admin@12345
app.admin.full-name=Test Admin
```

Verify: create the `tasktracker` schema via MySQL Workbench (or rely on `createDatabaseIfNotExist=true`), run `mvn spring-boot:run`, confirm startup on port 8082.

#### 2.3 Frontend scaffold (minimal — full features come later)

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install
```

Add to `package.json` scripts:
```json
"lint": "eslint .",
"test": "vitest run",
"build": "tsc -b && vite build"
```

Install vitest: `npm i -D vitest`. Add one placeholder test `src/smoke.test.ts`:
```ts
import { expect, test } from 'vitest'
test('smoke', () => expect(1 + 1).toBe(2))
```
This guarantees the CI frontend job passes from day one.

Create `frontend/.env.example`:
```
VITE_API_BASE_URL=http://localhost:8082/api/v1
VITE_WS_URL=ws://localhost:8082/ws
```

**Acceptance criteria:**
- `mvn -B verify` succeeds in `backend/` (no tests yet — fine).
- `npm run lint && npm test && npm run build` succeed in `frontend/`.
- Backend boots against local MySQL; `tasktracker` schema exists.
- `application-dev.properties` is NOT tracked by git; the `.example` is.

**Suggested commits:**
- `chore: scaffold spring boot backend with layered structure`
- `chore: add configuration profiles for dev prod and test`
- `chore: add spotless code formatting`
- `chore: scaffold vite react frontend with lint and test scripts`

**PR:** `feat/project-setup → dev`

---

### TASK 03 — CI pipeline (GitHub Actions)

**Branch:** `feat/ci-pipeline`
**Estimated effort:** 30 min

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main, dev]
  pull_request:
    branches: [main, dev]

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - name: Lint
        run: mvn -B spotless:check
      - name: Build and test
        run: mvn -B verify

  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install dependencies
        run: npm ci
      - name: Lint
        run: npm run lint
      - name: Test
        run: npm test
      - name: Build
        run: npm run build
```

Notes:
- Backend tests will use `@ActiveProfiles("test")` (H2), so CI needs no MySQL service.
- Add the CI status badge to README later (Task 15).

**Acceptance criteria:** PR to `dev` shows both jobs green.

**Suggested commits:**
- `chore: add github actions ci for backend and frontend`

**PR:** `feat/ci-pipeline → dev`

---

### TASK 04 — Domain model: entities, repositories, DTOs, mappers, admin seeder

**Branch:** `feat/domain-model`
**Estimated effort:** 1 hr

#### 4.1 Enums

```java
public enum Role { USER, ADMIN }
public enum TaskStatus { TODO, IN_PROGRESS, DONE }
```

#### 4.2 Entities

`entity/User.java` — `@Entity @Table(name = "users")`, Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`:
- `id` Long `@Id @GeneratedValue(strategy = IDENTITY)`
- `email` String `@Column(nullable = false, unique = true)`
- `password` String `@Column(nullable = false)`
- `fullName` String `@Column(nullable = false, length = 100)`
- `role` Role `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)`
- `createdAt` Instant `@CreatedDate @Column(updatable = false)`
- class-level `@EntityListeners(AuditingEntityListener.class)`

`entity/Task.java` — same Lombok set + auditing listener:
- `id`, `title` (`nullable=false, length=150`), `description` (`@Column(columnDefinition = "TEXT")`), `status` (`@Enumerated(STRING), nullable=false`), `dueDate` LocalDate
- `owner` — `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id")`
- `createdAt` / `updatedAt` — `@CreatedDate` / `@LastModifiedDate`

`config/JpaAuditingConfig.java` — `@Configuration @EnableJpaAuditing`.

#### 4.3 Repositories

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

public interface TaskRepository extends JpaRepository<Task, Long>,
                                        JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
```

#### 4.4 DTOs

Create all records exactly as in section 3.3, with the validation annotations listed there. `PageResponse<T>` is a record with a static factory:

```java
public record PageResponse<T>(List<T> content, int page, int size,
                              long totalElements, int totalPages, boolean last) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isLast());
    }
}
```

#### 4.5 MapStruct mappers

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}

@Mapper(componentModel = "spring", uses = UserMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TaskMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskRequest request);

    TaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(TaskRequest request, @MappingTarget Task task);
}
```

Run `mvn compile` and confirm generated impls appear under `target/generated-sources/annotations`.

#### 4.6 Admin seeder

`config/AdminSeeder.java` — a `@Configuration` exposing a `CommandLineRunner` bean that reads `app.admin.*` properties and inserts the admin **only if** `existsByEmail` is false (idempotent, because `ddl-auto=update` preserves data). Password must be BCrypt-encoded (a `PasswordEncoder` bean is created in Task 05 — if compiling before that, temporarily instantiate `new BCryptPasswordEncoder()` and refactor in Task 05).

**Acceptance criteria:**
- App boots; Workbench shows `users` and `tasks` tables with correct columns; admin row exists.
- `mvn -B verify` green.

**Suggested commits:**
- `feat: add user and task entities with jpa auditing`
- `feat: add repositories request and response dtos`
- `feat: add mapstruct mappers and admin seeder`

**PR:** `feat/domain-model → dev`

---

### TASK 05 — Authentication & security (JWT, RBAC foundation)

**Branch:** `feat/auth-jwt`
**Estimated effort:** 2 hrs

#### 5.1 `security/JwtUtil.java`

Using jjwt 0.12 API. Reads `app.jwt.secret` and `app.jwt.expiration-ms`.

```java
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("uid", user.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) { /* parse claims, return subject */ }
    public boolean isValid(String token)     { /* parse; catch JwtException -> false */ }
}
```

#### 5.2 `security/SecurityUser.java` + `UserDetailsServiceImpl`

`SecurityUser` implements `UserDetails`, wraps the `User` entity, exposes `getUser()`, authority = `ROLE_<role>`. `UserDetailsServiceImpl` loads by email from `UserRepository`, throws `UsernameNotFoundException`.

Add a convenience for controllers/services:
```java
public static User currentUser() {
    return ((SecurityUser) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal()).getUser();
}
```
(Place in a small `security/CurrentUser.java` helper or use `@AuthenticationPrincipal SecurityUser` in controllers — pick ONE style and use it consistently.)

#### 5.3 `security/JwtAuthFilter.java`

`OncePerRequestFilter`:
1. Read `Authorization` header; if absent or not starting `Bearer `, continue chain.
2. Extract token; if `jwtUtil.isValid` false → continue chain (entry point will 401 on protected routes).
3. Load `UserDetails` by extracted email; build `UsernamePasswordAuthenticationToken` with authorities; set into `SecurityContextHolder`.
4. Continue chain. Never throw from the filter for a bad token — just skip authentication.

#### 5.4 `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint restAuthEntryPoint; // writes ErrorResponse JSON with 401

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/ws/**").permitAll()   // STOMP CONNECT enforces JWT (Task 09)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(restAuthEntryPoint))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
```

`security/RestAuthEntryPoint.java` — implements `AuthenticationEntryPoint`; writes the standard `ErrorResponse` JSON with status 401 and `message: "Authentication required"` (use `ObjectMapper`).

`config/CorsConfig.java` — `CorsConfigurationSource` bean reading `app.cors.allowed-origins` (comma-split), allowing methods GET/POST/PUT/DELETE/OPTIONS, all headers, credentials true, applied to `/**`.

#### 5.5 Auth service + controller

`service/AuthService` interface: `AuthResponse register(RegisterRequest r)`, `AuthResponse login(LoginRequest r)`, `UserResponse me()`.

`service/impl/AuthServiceImpl`:
- `register`: if `existsByEmail` → throw `DuplicateEmailException` (409, Task 06). Save user with encoded password, role `USER`. Return `AuthResponse` with fresh JWT.
- `login`: `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))` — `BadCredentialsException` propagates (handled as 401 in Task 06). On success, load user, return token + info.
- `me`: map current user to `UserResponse`.

`controller/AuthController` (`@RestController @RequestMapping("/api/v1/auth")`):
- `POST /register` → 201 + AuthResponse
- `POST /login` → 200 + AuthResponse
- `GET /me` → 200 + UserResponse
- All request bodies annotated `@Valid`.

**Verification (curl):**
```bash
curl -s -X POST localhost:8082/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"u1@test.com","password":"Password1!","fullName":"User One"}'
curl -s -X POST localhost:8082/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"u1@test.com","password":"Password1!"}'
curl -s localhost:8082/api/v1/auth/me -H "Authorization: Bearer <token>"
```

**Acceptance criteria:**
- Register creates a USER row (check Workbench); duplicate email → 409 shape (after Task 06; temporarily 500 is acceptable until then only if Tasks 05 and 06 are merged same session).
- Login returns a JWT; `/me` works with it; `/me` without token → 401 JSON envelope.

**Suggested commits:**
- `feat: add jwt util user details service and auth filter`
- `feat: add stateless security config with cors and rest 401 entry point`
- `feat: add register login and me endpoints`

**PR:** `feat/auth-jwt → dev`

---

### TASK 06 — Validation & global error handling

**Branch:** `feat/error-handling`
**Estimated effort:** 45 min

Create `exception/` classes:

- `ResourceNotFoundException extends RuntimeException` — constructor `(String resource, Long id)` → message `"Task not found with id 42"`.
- `DuplicateEmailException extends RuntimeException`.

`exception/GlobalExceptionHandler` — `@RestControllerAdvice`, every handler returns `ResponseEntity<ErrorResponse>` using the envelope from section 3.4 (populate `path` from `HttpServletRequest`):

| Exception | Status | Notes |
|-----------|--------|-------|
| `MethodArgumentNotValidException` | 400 | collect `fieldErrors` map from binding result |
| `HttpMessageNotReadableException` | 400 | message `"Malformed request body"` (covers bad enum values) |
| `MethodArgumentTypeMismatchException` | 400 | bad path/query param types |
| `BadCredentialsException` | 401 | `"Invalid email or password"` |
| `DuplicateEmailException` | 409 | |
| `ResourceNotFoundException` | 404 | |
| `AccessDeniedException` | 403 | |
| `Exception` (fallback) | 500 | log stack trace; generic message, never leak internals |

**Acceptance criteria:**
- `POST /auth/register` with blank fields → 400 with `fieldErrors` populated.
- Duplicate registration → 409 envelope.
- Wrong password login → 401 envelope.

**Suggested commits:**
- `feat: add global exception handler with consistent error envelope`
- `feat: add custom domain exceptions`

**PR:** `feat/error-handling → dev`

---

### TASK 07 — Task CRUD with ownership enforcement

**Branch:** `feat/task-crud`
**Estimated effort:** 1.5 hrs

#### 7.1 Service interface

```java
public interface TaskService {
    TaskResponse create(TaskRequest request);
    TaskResponse getById(Long id);
    PageResponse<TaskResponse> list(TaskStatus status, Long ownerId, Pageable pageable); // Task 08
    TaskResponse update(Long id, TaskRequest request);
    void delete(Long id);
}
```

#### 7.2 `service/impl/TaskServiceImpl`

Inject `TaskRepository`, `UserRepository`, `TaskMapper` (and later `TaskEventPublisher`). Annotate class `@Service @RequiredArgsConstructor @Transactional`; put `@Transactional(readOnly = true)` on `getById` and `list`.

**Central ownership rule — implement as one private helper and reuse everywhere:**

```java
private Task getTaskForCurrentUser(Long id) {
    User current = CurrentUser.get();
    if (current.getRole() == Role.ADMIN) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
    return taskRepository.findByIdAndOwnerId(id, current.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
}
```

> Design decision (README): a USER accessing another user's task gets **404**, not 403, so task IDs don't leak existence. Admin bypasses ownership.

Method behavior:
- `create` — map request → entity, set `owner = current user`, save, return response. (Event publishing added in Task 09.)
- `getById` — `getTaskForCurrentUser(id)` → map.
- `update` — `getTaskForCurrentUser(id)`, then `taskMapper.updateEntity(request, task)`, save, map.
- `delete` — `getTaskForCurrentUser(id)`, then delete.

#### 7.3 Controller

`controller/TaskController` — `@RestController @RequestMapping("/api/v1/tasks") @RequiredArgsConstructor`:

- `POST` → 201 + `TaskResponse`
- `GET /{id}` → 200
- `PUT /{id}` → 200
- `DELETE /{id}` → 204 (no body)
- Bodies `@Valid`. No role annotations needed — RBAC lives in the service ownership rule. (Keep `@EnableMethodSecurity` available; mention in README that `@PreAuthorize` would be used for future admin-only endpoints.)

**Verification:** register two users; user A creates a task; user B `GET /tasks/{aTaskId}` → 404; A updates/deletes own → 200/204; admin (seeded credentials) GETs A's task → 200.

**Acceptance criteria:** all curls above behave exactly as specified; validation errors return the 400 envelope.

**Suggested commits:**
- `feat: add task service with ownership enforcement`
- `feat: add task crud endpoints`

**PR:** `feat/task-crud → dev`

---

### TASK 08 — Task listing: pagination + filtering

**Branch:** `feat/task-list-filter-pagination`
**Estimated effort:** 1 hr

#### 8.1 Specifications

`repository/spec/TaskSpecifications.java`:

```java
public final class TaskSpecifications {
    private TaskSpecifications() {}

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasOwner(Long ownerId) {
        return (root, q, cb) -> ownerId == null ? null : cb.equal(root.get("owner").get("id"), ownerId);
    }
}
```

#### 8.2 Service `list` implementation

```java
@Transactional(readOnly = true)
public PageResponse<TaskResponse> list(TaskStatus status, Long ownerId, Pageable pageable) {
    User current = CurrentUser.get();
    Long effectiveOwnerId = current.getRole() == Role.ADMIN ? ownerId : current.getId();

    Specification<Task> spec = Specification
            .where(TaskSpecifications.hasStatus(status))
            .and(TaskSpecifications.hasOwner(effectiveOwnerId));

    return PageResponse.from(taskRepository.findAll(spec, pageable).map(taskMapper::toResponse));
}
```

> **Security rule (README):** the `ownerId` query param is **overridden server-side** for non-admins. Never trust client filter params for authorization.

#### 8.3 Controller endpoint

```java
@GetMapping
public PageResponse<TaskResponse> list(
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Long ownerId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    // clamp size to max 50
    if (pageable.getPageSize() > 50) {
        pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
    }
    return taskService.list(status, ownerId, pageable);
}
```

To fix lazy-loading of `owner` in list mapping, add `@EntityGraph(attributePaths = "owner")` on a repository override of `findAll(Specification, Pageable)` **or** simply accept the N+1 for this scope and note it under Future Improvements (either is fine; the `@EntityGraph` override is ~5 lines and reads better — prefer it).

**Verification:** create 15 tasks for user A with mixed statuses; `GET /tasks?page=1&size=10` returns 5; `?status=DONE` filters; as user A `?ownerId=<adminId>` still returns only A's tasks; as admin `?ownerId=<aId>` returns A's.

**Suggested commits:**
- `feat: add task list with pagination and jpa specifications`
- `fix: force owner scope for non-admin list queries`

**PR:** `feat/task-list-filter-pagination → dev`

---

### TASK 09 — Real-time updates (WebSocket + STOMP)

**Branch:** `feat/websocket-realtime`
**Estimated effort:** 1.5 hrs

#### 9.1 Event model

```java
public enum TaskEventType { CREATED, UPDATED, DELETED }
public record TaskEvent(TaskEventType type, TaskResponse task) {}
```

#### 9.2 `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WsAuthChannelInterceptor wsAuthChannelInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setUserDestinationPrefix("/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsAuthChannelInterceptor);
    }
}
```

#### 9.3 `security/WsAuthChannelInterceptor.java`

```java
@Component
@RequiredArgsConstructor
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ") || !jwtUtil.isValid(auth.substring(7))) {
                throw new MessagingException("Invalid or missing JWT");
            }
            String email = jwtUtil.extractEmail(auth.substring(7));
            UserDetails details = userDetailsService.loadUserByUsername(email);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    details, null, details.getAuthorities()));
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (dest != null && dest.startsWith("/topic/admin")) {
                Authentication user = (Authentication) accessor.getUser();
                boolean isAdmin = user != null && user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (!isAdmin) throw new MessagingException("Admin subscription denied");
            }
        }
        return message;
    }
}
```

Key points: the CONNECT frame carries the JWT (browser WebSocket API cannot set HTTP headers, which is why `/ws/**` is `permitAll` at the HTTP layer and auth happens here). `accessor.setUser(...)` is what makes `convertAndSendToUser(email, ...)` route correctly — the principal name is the email.

#### 9.4 `event/TaskEventPublisher.java`

```java
@Component
@RequiredArgsConstructor
public class TaskEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(TaskEventType type, TaskResponse task, String ownerEmail) {
        TaskEvent event = new TaskEvent(type, task);
        messagingTemplate.convertAndSendToUser(ownerEmail, "/queue/tasks", event);
        messagingTemplate.convertAndSend("/topic/admin/tasks", event);
    }
}
```

#### 9.5 Wire into `TaskServiceImpl`

Call the publisher **after** the DB write in `create`, `update`, `delete`:
- create → `publish(CREATED, response, task.getOwner().getEmail())`
- update → `publish(UPDATED, response, ...)`
- delete → capture owner email + a minimal `TaskResponse` (id only is acceptable) **before** deletion, then `publish(DELETED, ...)`.

> Known duplicate: an ADMIN who owns a task receives the event on both destinations. Acceptable — the frontend handler (query invalidation) is idempotent. Note it in README.

**Verification (manual):** use two browser tabs after Task 14, or a quick Node script with `@stomp/stompjs` now. Minimum: connect with a valid token succeeds, without → connection rejected.

**Suggested commits:**
- `feat: add stomp websocket config with jwt connect authentication`
- `feat: enforce admin-only subscription on admin topic`
- `feat: publish task events on create update delete`

**PR:** `feat/websocket-realtime → dev`

---

### TASK 10 — Backend automated tests

**Branch:** `feat/backend-tests`
**Estimated effort:** 2 hrs

All integration tests: `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")` (H2). Create a shared test helper:

```java
// TestAuthHelper: registers a user via the real endpoint and returns its JWT
String tokenFor(String email)          // registers (if needed) + logs in
String adminToken()                    // logs in with app.admin.* test credentials
```

#### Required test classes & cases

**`AuthControllerIT`**
1. register → 201, response has token + role USER
2. register duplicate email → 409 envelope
3. register blank fields → 400 with `fieldErrors`
4. login wrong password → 401
5. `GET /me` with token → 200, correct email; without token → 401

**`TaskControllerIT`** (register userA, userB; use seeded admin)
6. unauthenticated `GET /tasks` → 401
7. create task → 201, owner = userA, status persisted
8. create with blank title → 400 `fieldErrors.title`
9. userA gets own task → 200
10. userB gets userA's task → **404**
11. admin gets userA's task → 200
12. userA updates own → 200, fields changed
13. userB updates userA's → 404
14. userA deletes own → 204; subsequent GET → 404
15. admin deletes userA's task → 204

**`TaskListIT`**
16. 15 tasks for userA → `?size=10` returns 10, `totalElements=15`, `totalPages=2`; `?page=1` returns 5, `last=true`
17. `?status=DONE` returns only DONE
18. userA with `?ownerId=<userB id>` → still only userA's tasks (forced scope)
19. admin sees tasks of both users; admin `?ownerId=<userA>` → only A's

**`TaskServiceImplTest`** (pure Mockito unit tests)
20. create sets owner to current user and calls publisher with CREATED
21. update on missing id → ResourceNotFoundException
22. delete publishes DELETED with owner email

**`JwtUtilTest`**
23. generate → isValid true, extractEmail correct
24. token built with 1 ms expiry → isValid false after sleep; garbage string → false

Mock `SimpMessagingTemplate`/`TaskEventPublisher` where needed with `@MockBean` so ITs don't require a broker. Target: ~24 meaningful tests, all green in `mvn -B verify`.

**Suggested commits:**
- `test: add auth registration and login integration tests`
- `test: add task crud and rbac integration tests`
- `test: add pagination filtering and service unit tests`

**PR:** `feat/backend-tests → dev`

> **Milestone check — end of backend:** `dev` now contains 8 merged PRs, CI green, and requirements F1–F9 + F11(backend) complete.

---

### TASK 11 — Frontend foundation (Tailwind, router, axios, auth store, types)

**Branch:** `feat/frontend-setup`
**Estimated effort:** 1 hr

#### 11.1 Install dependencies

```bash
cd frontend
npm i react-router-dom axios zustand @tanstack/react-query \
      react-hook-form zod @hookform/resolvers @stomp/rx-stomp
npm i -D tailwindcss @tailwindcss/vite
```

Configure Tailwind v4 via the Vite plugin (`@tailwindcss/vite` in `vite.config.ts`, `@import "tailwindcss";` at the top of `index.css`). If the installed major version is v3, use the classic `tailwind.config.js + postcss` setup instead — do not mix the two.

#### 11.2 `types/index.ts`

```ts
export type Role = 'USER' | 'ADMIN';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface User { id: number; email: string; fullName: string; role: Role; }
export interface Task {
  id: number; title: string; description: string | null;
  status: TaskStatus; dueDate: string | null;
  owner: User; createdAt: string; updatedAt: string;
}
export interface PageResponse<T> {
  content: T[]; page: number; size: number;
  totalElements: number; totalPages: number; last: boolean;
}
export interface TaskEvent { type: 'CREATED' | 'UPDATED' | 'DELETED'; task: Task; }
export interface AuthResponse extends User { token: string; }
```

#### 11.3 `stores/authStore.ts` (Zustand, persisted)

```ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';

interface AuthState {
  token: string | null;
  user: User | null;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      login: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null }),
    }),
    { name: 'auth' },
  ),
);
```

#### 11.4 `api/axiosClient.ts`

- `baseURL: import.meta.env.VITE_API_BASE_URL`
- Request interceptor: attach `Authorization: Bearer <token>` from `useAuthStore.getState().token` when present.
- Response interceptor: on 401 → `useAuthStore.getState().logout()` and `window.location.href = '/login'`.

`api/authApi.ts`: `register(data)`, `login(data)` → `AuthResponse`; `me()` → `User`.
`api/taskApi.ts`: `getTasks(params)`, `getTask(id)`, `createTask(body)`, `updateTask(id, body)`, `deleteTask(id)` — typed with the interfaces above.

#### 11.5 Routing shell

`main.tsx`: wrap App in `QueryClientProvider` (create one `QueryClient`) and `BrowserRouter`.

`App.tsx` routes:
- `/login` → LoginPage, `/register` → RegisterPage (public)
- `/` → DashboardPage, `/tasks/:id` → TaskDetailPage — both wrapped in `ProtectedRoute`

`components/ProtectedRoute.tsx`: if no token in store → `<Navigate to="/login" replace />`, else render `<Outlet />`. Add a `Navbar` showing user fullName + role badge + Logout button (calls `logout()` and navigates to `/login`).

Create placeholder page components so the app compiles and routes work.

**Acceptance criteria:** app builds; visiting `/` without login redirects to `/login`; lint + tests still green.

**Suggested commits:**
- `chore: add tailwind router query client and core dependencies`
- `feat: add auth store axios client and typed api layer`
- `feat: add protected routing shell and navbar`

**PR:** `feat/frontend-setup → dev`

---

### TASK 12 — Frontend authentication pages

**Branch:** `feat/frontend-auth`
**Estimated effort:** 1 hr

#### 12.1 Zod schemas (`lib/schemas.ts`)

Mirror backend rules:

```ts
export const registerSchema = z.object({
  fullName: z.string().min(1, 'Required').max(100),
  email: z.string().email(),
  password: z.string().min(8, 'Min 8 characters').max(72),
});
export const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1, 'Required'),
});
```

#### 12.2 Pages

`LoginPage` / `RegisterPage`:
- react-hook-form + `zodResolver`; Tailwind-styled centered card, labeled inputs, inline field errors (red text under input), submit button with loading state.
- On success: `useAuthStore.login(res.token, {id, email, fullName, role})` → navigate `/`.
- On 401/409: show the backend `message` from the error envelope in a top-of-form alert (read `err.response?.data?.message`, fallback generic).
- Cross-links: "No account? Register" / "Have an account? Login".

Logout (Navbar) already wired in Task 11 — verify token is cleared from localStorage.

#### 12.3 Minimal tests

`stores/authStore.test.ts` (vitest): login sets token+user; logout clears both. Replace the smoke test from Task 02 or keep alongside.

**Acceptance criteria:** full register → auto-login → dashboard flow works against the running backend; wrong password shows backend message; refresh keeps you logged in (persisted store); logout returns to `/login` and `/` is blocked again.

**Suggested commits:**
- `feat: add login and register pages with validation`
- `test: add auth store unit tests`

**PR:** `feat/frontend-auth → dev`

---

### TASK 13 — Frontend task management (list, filters, pagination, CRUD)

**Branch:** `feat/frontend-tasks`
**Estimated effort:** 2.5 hrs

#### 13.1 Query hooks

`hooks/useTasks.ts`:

```ts
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export interface TaskFilters { page: number; size: number; status?: TaskStatus; ownerId?: number; }

export const useTasks = (filters: TaskFilters) =>
  useQuery({
    queryKey: ['tasks', filters],
    queryFn: () => getTasks(filters),
    placeholderData: keepPreviousData,
  });
```

`hooks/useTaskMutations.ts`: `useCreateTask`, `useUpdateTask`, `useDeleteTask` — each `onSuccess: queryClient.invalidateQueries({ queryKey: ['tasks'] })`.

#### 13.2 Dashboard composition

`DashboardPage` holds filter state (`useState<TaskFilters>` with `{page: 0, size: 10}`) and renders:

- **`Filters`** — status `<select>` (All / TODO / IN_PROGRESS / DONE). If `user.role === 'ADMIN'`, also an owner filter (simplest compliant option: numeric owner-id input with an Apply button; a user dropdown needs an extra endpoint — list under Future Improvements). Changing any filter resets `page` to 0.
- **"New Task" button** → opens `TaskFormModal` in create mode.
- **`TaskList`** — grid/stack of `TaskCard`s: title, status badge (color per status: gray/amber/green), dueDate, owner name (visible for admin), Edit + Delete buttons. Card click → `/tasks/:id`. Handle loading (skeleton or spinner), error (retry button), and empty ("No tasks yet — create one") states.
- **`Pagination`** — Prev/Next + "Page X of Y", disabled appropriately, driven by `totalPages`/`last`.

#### 13.3 Create / Edit / Delete

`TaskFormModal` — one component, two modes (create vs edit via optional `task` prop):
- Fields: title, description (textarea), status select, dueDate (`<input type="date">`).
- zod schema: title required max 150; description max 2000 optional; status enum; dueDate optional string.
- Submit → corresponding mutation → close on success; show backend message on failure.

Delete → `window.confirm('Delete this task?')` → mutation.

#### 13.4 Task detail

`TaskDetailPage` — `useQuery(['task', id])` on `getTask(id)`; render all fields incl. createdAt/updatedAt (format with `toLocaleString`); Edit (same modal) + Delete (navigate back to `/` on success); back link. A 404 (e.g. someone else's task URL) → "Task not found" state.

**Acceptance criteria:** every F10 frontend requirement except real-time works end-to-end against the backend; USER never sees other users' tasks; ADMIN sees all + owner filter; pagination and status filter drive the query correctly (verify in the network tab).

**Suggested commits:**
- `feat: add task list with status filter and pagination`
- `feat: add task create edit modal and delete flow`
- `feat: add task detail page`
- `feat: add admin owner filter`

**PR:** `feat/frontend-tasks → dev`

---

### TASK 14 — Frontend real-time updates

**Branch:** `feat/frontend-realtime`
**Estimated effort:** 1 hr

#### 14.1 `ws/stompClient.ts`

```ts
import { RxStomp } from '@stomp/rx-stomp';

export const createStompClient = (token: string) => {
  const client = new RxStomp();
  const raw = import.meta.env.VITE_WS_URL as string;
  const brokerURL = raw.startsWith('/')
    ? `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}${raw}`
    : raw; // dev: full ws://localhost:8082/ws; prod: relative /ws behind nginx
  client.configure({
    brokerURL,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
};
```

#### 14.2 `hooks/useRealtime.ts`

A hook mounted once inside the protected layout (e.g. in `DashboardPage`'s parent or `ProtectedRoute`):

```ts
export function useRealtime() {
  const { token, user } = useAuthStore();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!token || !user) return;
    const client = createStompClient(token);

    const handle = (msg: IMessage) => {
      const event: TaskEvent = JSON.parse(msg.body);
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['task', event.task.id] });
      showToast(event); // e.g. "Task 'X' was updated"
    };

    const subs = [client.watch('/user/queue/tasks').subscribe(handle)];
    if (user.role === 'ADMIN') subs.push(client.watch('/topic/admin/tasks').subscribe(handle));

    return () => { subs.forEach(s => s.unsubscribe()); client.deactivate(); };
  }, [token, user?.id]);
}
```

#### 14.3 Toast

A minimal toast: small Zustand store (`toasts: {id, text}[]`) + fixed bottom-right container, auto-dismiss after 4 s. No library needed; if preferred, `react-hot-toast` is acceptable.

**Acceptance criteria (the demo that sells the feature):**
1. Browser A: user, dashboard open. Browser B (incognito): admin, dashboard open.
2. User creates a task → admin's list updates within ~1 s without refresh + toast appears.
3. Admin edits that task → user's list/detail updates live.
4. Delete propagates to both.
5. Logout cleanly deactivates the socket (no console errors).

**Suggested commits:**
- `feat: add stomp client with jwt connect headers`
- `feat: invalidate task queries and toast on realtime events`

**PR:** `feat/frontend-realtime → dev`

---

### TASK 15 — Documentation & Postman collection

**Branch:** `feat/docs-postman`
**Estimated effort:** 1 hr

#### 15.1 README.md (replace the stub) — required sections in this order

```
# Task Tracker
CI badge · one-paragraph description · (live URL added in Task 16)

## Tech Stack            — table: layer / technology / why
## Architecture Overview — the package layout from section 3.6 + a request-flow
                           line (Controller → Service → Repository → MySQL) and
                           a WS-flow line (Service → EventPublisher → STOMP topics)
## Getting Started
### Prerequisites        — Java 17, Maven, Node 20+, MySQL 8 (Workbench), Git
### Database Setup       — create schema `tasktracker` (or rely on
                           createDatabaseIfNotExist=true); default root/root note
### Backend Setup        — copy application-dev.properties.example →
                           application-dev.properties, adjust credentials,
                           `mvn spring-boot:run` → http://localhost:8082
### Frontend Setup       — copy .env.example → .env, `npm ci`, `npm run dev`
                           → http://localhost:5173
### Default Admin        — email/password from app.admin.* (dev values)
### Running Tests        — `mvn verify` / `npm test`
## API Documentation     — endpoint table from section 3.2 + "import the Postman
                           collection from /postman"
## Real-Time Design      — CONNECT-frame JWT auth, /user queue vs /topic/admin,
                           subscribe-time admin check, the admin-owner duplicate
                           note
## Design Decisions      — MapStruct; interface+impl layering; 404-vs-403 choice;
                           server-side owner scoping; TanStack Query (server
                           state) + Zustand (client state) split; JWT in
                           localStorage tradeoff (XSS vs CSRF — acknowledged);
                           ddl-auto=update rationale; profiles strategy
## Git Workflow          — main/dev/feature model, naming conventions, conventional
                           commits (make the 10% explicit)
## Assumptions           — registration only creates USER; admin is seeded, not
                           self-promotable; status is a fixed enum; dueDate
                           must be today or later on create; email unique;
                           hard delete
## Known Limitations     — anything incomplete + how you would finish it
                           (explicitly requested by the brief); logout is
                           client-side token discard (stateless JWT — server-side
                           blacklisting listed below)
## Future Improvements   — refresh tokens; Flyway + ddl validate; user-list
                           endpoint for admin owner dropdown; optimistic UI;
                           soft delete; task comments/attachments; rate
                           limiting; E2E tests (Playwright)
## Deployment            — filled in Task 16
```

#### 15.2 Postman (`/postman` folder, committed)

`TaskTracker.postman_collection.json`:
- Collection-level auth: Bearer `{{token}}`.
- Folder **Auth**: Register, Login, Me. On **Login** (and Register), add a *Tests* script:
  ```js
  const json = pm.response.json();
  pm.environment.set('token', json.token);
  ```
- Folder **Tasks**: Create, Get by ID (uses `{{taskId}}` — save it from Create's test script: `pm.environment.set('taskId', pm.response.json().id)`), List (with `page/size/status/ownerId` params, disabled by default), Update, Delete.
- Folder **Error cases** (nice touch): duplicate register, invalid body, unauthorized request (no token — override auth to "No Auth" on that request).

`TaskTracker.postman_environment.json`: `baseUrl = http://localhost:8082/api/v1`, `token` (empty), `taskId` (empty). All request URLs use `{{baseUrl}}/...`.

Export both files from Postman (v2.1 format) and verify a fresh import → Login → Create → List works with zero manual steps.

**Optional 10-minute extra:** add `springdoc-openapi-starter-webmvc-ui` to the backend pom → live Swagger UI at `/swagger-ui.html` (permit `/v3/api-docs/**` and `/swagger-ui/**` in SecurityConfig). Postman alone satisfies the requirement; this is polish only.

**Suggested commits:**
- `docs: add full readme with setup design decisions and assumptions`
- `docs: add postman collection and environment`

**PR:** `feat/docs-postman → dev`

---

### TASK 16 — Containerization & AWS EC2 deployment

**Branch:** `feat/deployment`
**Estimated effort:** 1.5 hrs

#### 16.1 `backend/Dockerfile` (multi-stage)

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 16.2 `frontend/Dockerfile` + `frontend/nginx.conf`

```dockerfile
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ENV VITE_API_BASE_URL=/api/v1
ENV VITE_WS_URL=/ws
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

> Vite bakes env vars at **build** time — that's why prod uses relative `/api/v1` and `/ws`, and the Task 14 client derives the absolute `ws(s)://` URL at runtime.

`nginx.conf`:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://backend:8082;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;   # keep idle sockets alive
    }

    location / {
        try_files $uri /index.html;   # SPA fallback
    }
}
```

> The `Upgrade`/`Connection` headers are the classic WebSocket-behind-Nginx gotcha. Do not omit them.

#### 16.3 `docker-compose.prod.yml` (repo root)

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: tasktracker
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 20

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/tasktracker
      DB_USERNAME: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      FRONTEND_URL: ${FRONTEND_URL}
      ADMIN_EMAIL: ${ADMIN_EMAIL}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

Commit a root `.env.example` (compose reads `.env` automatically):

```
MYSQL_ROOT_PASSWORD=change-me
JWT_SECRET=generate-a-64-char-random-string
FRONTEND_URL=http://<EC2_PUBLIC_IP>
ADMIN_EMAIL=admin@tasktracker.local
ADMIN_PASSWORD=change-me-strong
```

**Local dry-run first:** `docker compose -f docker-compose.prod.yml up --build` on your machine, open `http://localhost`, run the full flow (register, CRUD, two-tab real-time). Only then touch EC2.

#### 16.4 EC2 provisioning & deploy

1. Launch **t3.small** (Ubuntu 24.04). t2/t3.micro will struggle with JVM + MySQL + builds. Key pair for SSH.
2. Security group inbound: **80** from `0.0.0.0/0`; **22** from *your IP only*. Nothing else (3306/8082 stay internal to the compose network).
3. SSH in and install Docker:
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
   sudo usermod -aG docker ubuntu && newgrp docker
   ```
4. `git clone <repo>` (use an HTTPS token for a private repo), `cd task-tracker`.
5. `cp .env.example .env` and fill real secrets (`openssl rand -hex 32` for JWT_SECRET; set `FRONTEND_URL=http://<EC2_PUBLIC_IP>`).
6. `docker compose -f docker-compose.prod.yml up -d --build` (first build ≈ 5–10 min on t3.small).
7. Verify: `docker compose -f docker-compose.prod.yml ps` (3 services up), then from your machine open `http://<EC2_PUBLIC_IP>` → register → create task → two-browser real-time check.
8. Update README: live URL + admin demo credentials at the top, plus a **Deployment** section documenting steps 1–7 and the architecture (Nginx :80 → static SPA + `/api` `/ws` proxied → Spring :8082 → MySQL, all on one compose network).

**Troubleshooting quick refs:** backend restart-looping → `docker compose logs backend` (usually DB not ready → healthcheck covers it, or bad env). WS fails in prod but API works → Nginx upgrade headers or `FRONTEND_URL` CORS mismatch. 502 → backend still starting.

**Optional 10-minute extra:** add `spring-boot-starter-actuator`, permit `/actuator/health` in SecurityConfig, and point a compose healthcheck for the backend service at it (`curl -f http://localhost:8082/actuator/health`). Makes deploy verification and the frontend `depends_on` more robust.

**Suggested commits:**
- `feat: add multi-stage dockerfiles for backend and frontend`
- `feat: add production docker compose with nginx reverse proxy`
- `docs: add ec2 deployment guide and live url`

**PR:** `feat/deployment → dev`

---

### OPTIONAL TASK — Continuous Deployment (bonus; only if ahead of schedule)

**Branch:** `feat/cd-pipeline`
**Estimated effort:** 30 min
**Covers the "Continuous Deployment" bonus item.** Skip without hesitation if time is tight — document it under Future Improvements instead.

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EC2 over SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ubuntu
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd task-tracker
            git pull origin main
            docker compose -f docker-compose.prod.yml up -d --build
```

Setup:
1. Repo → Settings → Secrets and variables → Actions: add `EC2_HOST` (public IP) and `EC2_SSH_KEY` (contents of the private key `.pem`).
2. Merge this branch into `dev` **before** the final release PR — then the `dev → main` merge in Task 17 triggers the first automated deploy, which doubles as its own test.
3. Add one line to README → Deployment: "Pushes to `main` auto-deploy to EC2 via GitHub Actions."

**Suggested commits:**
- `chore: add continuous deployment workflow to ec2`

**PR:** `feat/cd-pipeline → dev`

---

### TASK 17 — Final release

**Branch:** PR `dev → main`
**Estimated effort:** 30 min

Pre-merge checklist (all must pass):

- [ ] CI green on `dev`
- [ ] **Clean-clone test:** fresh `git clone` into a temp dir, follow README verbatim (backend + frontend + tests). If any step fails, fix with a `fix:` commit — this is the single most common submission killer.
- [ ] Postman collection imports fresh and the Login → Create → List chain works
- [ ] Live EC2 URL responds; real-time demo works in two browsers
- [ ] README contains: badge, live URL, setup, API docs, design decisions, git workflow, assumptions, future improvements
- [ ] No secrets in git history (`git log -p | grep -iE 'password|secret'` spot-check); `application-dev.properties` and `.env` untracked
- [ ] Anything incomplete is documented in README under "Known Limitations" with the intended approach (explicitly requested by the assignment)

Open PR `dev → main` titled `release: task tracker v1.0`, merge, add reviewer access to the repo, submit the link.

---

## 5. Execution Order & Cut-Line

**Timeline mapping (1.5 days):**

| Day | Tasks |
|-----|-------|
| Day 1 (full) | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10 |
| Day 2 (half) | 11, 12, 13, 14, 15, 16, (optional CD), 17 |

**If running out of time, cut in this order (top = cut first):**

1. Task 16 EC2 deploy → keep Dockerfiles + compose + a documented (untested-live) deployment section
2. Task 12 frontend tests beyond the auth store test
3. Admin owner filter UI (backend support stays; note in README)
4. Toast polish (bare `invalidateQueries` still satisfies real-time)

**Never cut:** backend tests (Task 10), error handling (Task 06), README + Postman (Task 15), the clean-clone test (Task 17).

## 6. Definition of Done (applies to every task)

- Code compiles; `mvn -B verify` and frontend `lint + test + build` green locally
- New behavior manually verified (curl or browser) before the PR
- Conventional-commit messages; small logical commits
- PR merged into `dev` with CI green; branch deleted
- Any deviation from this spec is recorded in README → Design Decisions or Assumptions
