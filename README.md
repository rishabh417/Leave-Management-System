# Leave Management System

A production-oriented **Spring Boot REST API** for managing employees and leave requests.

Built with **Java 21, Spring Boot 4.1, MongoDB, Spring Security, JWT, Testcontainers, and GitHub Actions**.

> A backend engineering portfolio project focused on authentication, authorization, business-rule enforcement, testing, CI, and containerization.

---

## ✨ What it does

The system supports two roles:

| Role | Capabilities |
|---|---|
| **EMPLOYEE** | Authenticate, view permitted employee information, apply for leave, and access their own leave data |
| **ADMIN** | Manage employees and manage leave requests, including approval, rejection, and deletion |

### Core features

- 🔐 JWT-based authentication
- 🛡️ Role-based authorization (`ADMIN` / `EMPLOYEE`)
- 👤 Employee CRUD operations
- 🗓️ Leave application and management
- ✅ Leave approval / rejection workflow
- 🚫 Leave overlap detection
- 🔎 Ownership checks for protected resources
- ✉️ Email notifications for leave events
- ✔️ Jakarta Bean Validation
- ⚠️ Application-specific exception handling
- 🧪 Unit and integration testing
- 🍃 Real MongoDB integration testing with Testcontainers
- 🔄 GitHub Actions CI
- 📖 OpenAPI / Swagger UI
- 🐳 Docker multi-stage build

---

## 🏗️ Architecture

The application follows a layered monolithic architecture:

```text
                    ┌─────────────────┐
                    │     Client      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ Spring Security │
                    │      + JWT      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Controllers   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    Services     │
                    │ Business Rules  │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Repositories   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     MongoDB     │
                    └─────────────────┘
```

For the detailed architecture, request flows, business rules, trade-offs, and scalability discussion, see **[System Design](src/main/java/com/rishabh/leave_management_system/docs/system-design.md)**.

---

## 🧰 Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring MVC |
| Security | Spring Security + JWT |
| Database | MongoDB |
| Persistence | Spring Data MongoDB |
| Validation | Jakarta Bean Validation |
| Email | Spring Mail |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Unit Testing | JUnit + Mockito |
| Web Testing | MockMvc |
| Integration Testing | Testcontainers + MongoDB |
| Build | Maven |
| CI | GitHub Actions |
| Containerization | Docker |

---

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/rishabh/leave_management_system/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   └── docs/
│   │
│   └── resources/
│
└── test/
    └── java/com/rishabh/leave_management_system/
```

---

# 🚀 Getting Started

## Prerequisites

Install:

- **Java 21**
- **Docker Desktop**
- **Git**

Docker Desktop is required for the integration-test suite because Testcontainers starts a real MongoDB container.

---

## 1. Clone the repository

```bash
git clone <repository-url>
cd leave-management-system
```

---

## 2. Configure local secrets

The application keeps environment-specific secrets outside version control.

Create:

```text
src/main/resources/application-local.properties
```

Add the required values:

```properties
MONGODB_URI=<your-mongodb-connection-string>
JWT_SECRET=<your-jwt-secret>
MAIL_USERNAME=<your-mail-username>
MAIL_PASSWORD=<your-mail-password>
```

The main `application.properties` file references these values through placeholders.

### ⚠️ Important

Do **not** commit:

```text
application-local.properties
```

Never put real credentials, database connection strings, mail passwords, or JWT secrets into the repository.

---

# 🐳 Running with Docker

The project contains a **multi-stage Dockerfile**:

1. Builds the application using a Java 21 JDK image.
2. Runs the generated JAR using a Java 21 JRE image.

### Build the image

From the project root:

```powershell
docker build -t leave-management-system .
```

### Start the container

For local development:

```powershell
docker run --name leave-management-app --env-file src/main/resources/application-local.properties -p 9090:9090 leave-management-system
```

The command:

- loads the local configuration values as environment variables
- maps container port `9090` to host port `9090`
- starts the Spring Boot application

### Verify the application

Base URL:

```text
http://localhost:9090
```

Swagger UI:

```text
http://localhost:9090/swagger-ui/index.html
```

### Stop and remove the container

```powershell
docker stop leave-management-app
docker rm leave-management-app
```

### Remove the image

```powershell
docker rmi leave-management-system
```

> **Security note:** The `--env-file` approach is intended for local development. Do not publish or commit the local secrets file.

---

# 🧪 Running Tests

## Complete test suite

From Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The integration tests use Testcontainers, so **Docker Desktop must be running**.

## Integration tests only

```powershell
.\mvnw.cmd -Dtest=LeaveManagementSystemApplicationTests test
```

The integration tests:

- start the Spring Boot application context
- exercise HTTP endpoints through MockMvc
- use a real MongoDB container through Testcontainers
- isolate email delivery from the test environment

---

# 🔑 API

## Authentication

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Example request:

```json
{
  "email": "employee@example.com",
  "password": "password123"
}
```

A successful login returns a JWT.

Use the token for protected endpoints:

```http
Authorization: Bearer <JWT>
```

---

## 👤 Employee Endpoints

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/employee` | Authenticated | Create an employee |
| `GET` | `/api/employee/{id}` | Authenticated | Get an employee |
| `GET` | `/api/employee` | `ADMIN` | Get all employees |
| `PUT` | `/api/employee/{id}` | `ADMIN`, `EMPLOYEE` | Update an employee |
| `DELETE` | `/api/employee/{id}` | `ADMIN` | Delete an employee |

### Create employee

```http
POST /api/employee
Content-Type: application/json
```

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

Validation includes:

- name must not be blank
- email must be valid and non-blank
- password must contain at least 8 characters

---

# 🗓️ Leave Endpoints

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/leave` | Authenticated | Apply for leave |
| `GET` | `/api/leave` | `ADMIN` | Get all leave requests |
| `GET` | `/api/leave/employee/{employeeId}` | Authenticated + ownership rules | Get employee leave requests |
| `GET` | `/api/leave/id/{id}` | Authenticated + ownership rules | Get a leave request |
| `PUT` | `/api/leave/approve/{leaveId}` | `ADMIN` | Approve leave |
| `PUT` | `/api/leave/reject/{leaveId}` | `ADMIN` | Reject leave |
| `DELETE` | `/api/leave/delete/{leaveId}` | `ADMIN` | Delete leave |

### Apply for leave

```http
POST /api/leave
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "fromDate": "2026-09-10",
  "toDate": "2026-09-12",
  "reason": "Personal work",
  "leaveType": "CASUAL"
}
```

The authenticated employee is associated with the request; the client does not provide an employee ID for this operation.

The service validates:

- both dates are present
- the reason is present and no longer than 500 characters
- the leave type is present
- the start date is not after the end date
- there is no overlapping `PENDING` or `APPROVED` leave for the employee

A newly accepted leave starts as:

```text
PENDING
```

---

## 🔄 Leave Lifecycle

```text
                 ┌─────────┐
                 │ PENDING │
                 └────┬────┘
                      │
             ┌────────┴────────┐
             ▼                 ▼
       ┌──────────┐      ┌──────────┐
       │ APPROVED │      │ REJECTED │
       └──────────┘      └──────────┘
```

Only administrators can approve or reject leave requests.

---

# 📦 Request / Response Examples

## Employee request

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

## Employee update request

All fields are optional, allowing partial updates.

```json
{
  "name": "John Updated",
  "password": "newpassword123",
  "role": "EMPLOYEE",
  "department": "Engineering"
}
```

## Leave response

```json
{
  "leaveId": "leave-id",
  "leaveType": "CASUAL",
  "leaveStatus": "PENDING",
  "fromDate": "2026-09-10",
  "toDate": "2026-09-12",
  "appliedDate": "2026-09-02",
  "reason": "Personal work"
}
```

---

# ⚠️ Error Handling

The application uses application-specific exceptions for business failures, including:

- `EmployeeNotFoundException`
- `LeaveNotFoundException`
- `InvalidLeaveDateException`
- `InvalidLeaveOverlapException`
- `AccessDeniedException`

Common HTTP responses:

| Situation | Status |
|---|---:|
| Invalid request | `400 Bad Request` |
| Resource not found | `404 Not Found` |
| Access denied | `403 Forbidden` |
| Conflicting leave request | `409 Conflict` |

---

# 📖 API Documentation

With the application running:

**Swagger UI**

```text
http://localhost:9090/swagger-ui/index.html
```

**OpenAPI JSON**

```text
http://localhost:9090/v3/api-docs
```

Swagger UI provides an interactive way to inspect and execute the available REST endpoints.

---

# 🧪 Testing Strategy

The project uses two complementary testing levels.

### Unit tests

Service-layer tests use Mockito to isolate business logic and cover scenarios including:

- successful operations
- validation failures
- leave overlap detection
- missing resources
- ownership restrictions
- authorization rules
- repository interactions
- email-service interactions

### Integration tests

Integration tests verify the application closer to a real deployment boundary:

```text
MockMvc
   │
   ▼
Spring Boot
   │
   ▼
Spring Security
   │
   ▼
Services
   │
   ▼
Repositories
   │
   ▼
Real MongoDB
   │
   ▼
Testcontainers
```

This verifies that the main application layers work together against an actual MongoDB instance rather than relying only on mocked repositories.

---

# 🔄 Continuous Integration

GitHub Actions runs the test suite automatically for repository changes targeting the configured main branch workflow.

The CI pipeline:

```text
Checkout
   ↓
Set up Java
   ↓
Maven
   ↓
Run tests
   ↓
Testcontainers / MongoDB
   ↓
Pass / Fail
```

CI provides a repeatable automated verification that the repository remains healthy after changes.

---

# 📐 Design Documentation

The deeper engineering decisions are documented in:

**[System Design](src/main/java/com/rishabh/leave_management_system/docs/system-design.md)**

It covers:

- system context
- layered architecture
- authentication and authorization flows
- employee and leave request flows
- data design
- business rules
- error handling
- notification flow
- testing architecture
- CI
- scalability considerations
- concurrency limitations
- future architecture
- design decisions and trade-offs

---

# 🚧 Current Limitations & Future Improvements

The current implementation is intentionally a relatively small monolithic backend.

Potential areas for future evolution include:

- stronger concurrency guarantees around overlapping leave requests
- asynchronous email processing
- database indexing based on measured query patterns
- caching where profiling demonstrates a need
- stronger token lifecycle and revocation strategy
- production deployment and infrastructure hardening

These are future considerations rather than claims about the current implementation.

---

## 📄 License

This project is intended as a backend engineering portfolio project.
