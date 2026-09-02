# Leave Management System --- System Design

## 1. System Context

The Leave Management System is a backend application for managing
employees and their leave requests.

There are two primary actors:

-   **Employee** --- logs in, manages permitted employee information,
    submits leave requests, and views permitted leave information.
-   **Administrator** --- performs administrative employee operations
    and manages leave requests, including approval and rejection.

The application is currently a **single Spring Boot application**.
MongoDB is used for persistence and an email provider is used for
leave-related notifications.

``` text
                         Employee
                            |
                            | REST API
                            v
                  +----------------------+
                  | Leave Management     |
                  |       System         |
                  |    Spring Boot API   |
                  +----------+-----------+
                             |
                  +----------+----------+
                  |                     |
                  v                     v
             +---------+          +-----------+
             | MongoDB |          |   Email   |
             |         |          |  Service  |
             +---------+          +-----------+
                             ^
                             |
                         REST API
                             |
                       Administrator
```

The system boundary is the Spring Boot application. MongoDB and the
email provider are external dependencies.

------------------------------------------------------------------------

## 2. Architecture

The application follows a layered monolithic architecture.

``` text
                         Client
                           |
                           v
                +---------------------+
                |    Controllers      |
                |---------------------|
                | LoginController     |
                | EmployeeController  |
                | LeaveController     |
                +----------+----------+
                           |
                           v
                +---------------------+
                |      Services       |
                |---------------------|
                | EmployeeService     |
                | LeaveService        |
                | EmailService        |
                +----------+----------+
                           |
              +------------+------------+
              |                         |
              v                         v
      +---------------+          +-------------+
      |  Repositories  |          |   Security  |
      |---------------|          |-------------|
      | EmployeeRepo  |          | JWT Service |
      | LeaveRepo     |          | JWT Filter  |
      +-------+-------+          | UserDetails |
              |                  +-------------+
              v
        +-----------+
        |  MongoDB   |
        +-----------+
```

### 2.1 Controller Layer

Controllers expose the REST API and are responsible for receiving HTTP
requests and passing validated input to the service layer.

The main controllers are:

-   `LoginController`
-   `EmployeeController`
-   `LeaveController`

Request validation is performed using Jakarta Bean Validation, for
example through `@Valid`, `@NotBlank`, `@NotNull`, `@Email`, and
`@Size`.

### 2.2 Service Layer

The service layer contains the application's business rules.

Examples include:

-   Employee creation and update rules.
-   Leave date validation.
-   Leave overlap detection.
-   Determining whether the authenticated user can access a resource.
-   Changing leave status.
-   Triggering leave-related email notifications.

This keeps business logic out of the controllers.

### 2.3 Repository Layer

Spring Data MongoDB repositories provide persistence operations.

The application has repositories for employees and leaves. The leave
repository also contains a query used to check whether an employee
already has an overlapping pending or approved leave.

### 2.4 Security Components

Spring Security handles request authentication and authorization.

The security flow uses:

-   `AuthenticationManager`
-   `EmployeeUserDetailsService`
-   `JWTService`
-   `JWTAuthenticationFilter`
-   role-based authorization through `@PreAuthorize`

The JWT filter authenticates requests containing a valid token before
protected controller methods are executed.

### 2.5 Why a Monolith?

The current application uses a monolith intentionally rather than
splitting the system into multiple services.

For the current problem size, a single deployment keeps:

-   deployment simpler,
-   communication between components local,
-   debugging easier,
-   transactional/business flows easier to reason about,
-   infrastructure requirements relatively small.

Introducing microservices without a clear scaling or organizational
requirement would add network calls, service discovery/deployment
concerns, distributed failure modes, and additional operational
complexity.

------------------------------------------------------------------------

# 3. Request Flows

## 3.1 Authentication Flow

The client first authenticates with email and password.

``` text
Client
  |
  | POST /api/auth/login
  | email + password
  v
LoginController
  |
  v
AuthenticationManager
  |
  v
EmployeeUserDetailsService
  |
  v
EmployeeRepository
  |
  v
MongoDB
  |
  | Employee details
  v
PasswordEncoder
  |
  | credentials valid
  v
JWTService
  |
  | generate JWT
  v
LoginController
  |
  v
Client
  |
  | JWT
  v
Subsequent requests
```

The authenticated user's identity is represented by the JWT for
subsequent protected requests.

------------------------------------------------------------------------

## 3.2 Authenticated Request Flow

For a protected endpoint:

``` text
Client
  |
  | Authorization: Bearer <JWT>
  v
JWTAuthenticationFilter
  |
  | validate token
  | extract user identity
  v
SecurityContext
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
MongoDB
```

Authorization can then be applied at the controller level with
annotations such as:

``` text
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
```

Some resource-ownership checks are additionally performed inside the
service layer.

------------------------------------------------------------------------

## 3.3 Employee Management Flow

A typical employee request follows:

``` text
Client
  |
  v
EmployeeController
  |
  | validate request
  v
EmployeeService
  |
  | business rules
  v
EmployeeRepository
  |
  v
MongoDB
  |
  v
EmployeeResponseDTO
  |
  v
Client
```

The controller works with request/response DTOs rather than exposing the
persistence entity directly.

------------------------------------------------------------------------

## 3.4 Leave Application Flow

The leave application flow contains the main business logic of the
system.

``` text
Client
  |
  | POST /api/leave
  v
LeaveController
  |
  | @Valid
  v
LeaveService
  |
  +--> Get authenticated user
  |
  +--> Find employee by authenticated email
  |
  +--> Validate fromDate <= toDate
  |
  +--> Check overlapping PENDING/APPROVED leave
  |
  +--> Create Leave with PENDING status
  |
  +--> Save Leave
  |
  +--> Send leave-applied email
  |
  v
LeaveResponseDTO
  |
  v
Client
```

The employee ID is obtained from the authenticated user rather than
trusting an employee ID supplied by the client for the application
operation.

### Overlap check

The repository checks for an existing leave belonging to the employee
where:

-   the existing leave has `PENDING` or `APPROVED` status,
-   the existing leave overlaps the requested date range.

Rejected leave does not participate in this blocking check.

Conceptually:

``` text
Existing leave:
        |-----------|

Requested leave:
              |-----------|

              overlap
                 |
                 v
          reject request
```

If the requested dates are invalid, an `InvalidLeaveDateException` is
raised.

If an overlapping leave exists, an `InvalidLeaveOverlapException` is
raised.

------------------------------------------------------------------------

## 3.5 Leave Approval Flow

Only an administrator is allowed to approve a leave through the
protected endpoint.

``` text
Admin
  |
  | PUT /api/leave/approve/{leaveId}
  v
LeaveController
  |
  | @PreAuthorize ADMIN
  v
LeaveService
  |
  +--> Find leave
  |
  +--> Confirm administrative authorization
  |
  +--> Find employee associated with leave
  |
  +--> Change status to APPROVED
  |
  +--> Save leave
  |
  +--> Send approval email
  |
  v
LeaveResponseDTO
```

The important state transition is:

``` text
PENDING
   |
   | admin approval
   v
APPROVED
```

------------------------------------------------------------------------

## 3.6 Leave Rejection Flow

The rejection flow follows the same general structure:

``` text
Admin
  |
  | PUT /api/leave/reject/{leaveId}
  v
LeaveController
  |
  | @PreAuthorize ADMIN
  v
LeaveService
  |
  +--> Find leave
  |
  +--> Change status to REJECTED
  |
  +--> Save leave
  |
  +--> Send rejection email
  |
  v
LeaveResponseDTO
```

State transition:

``` text
PENDING
   |
   | admin rejection
   v
REJECTED
```

------------------------------------------------------------------------

# 4. Data Design

The application stores employees and leaves as MongoDB documents.

## 4.1 Employee

Conceptually, an employee document contains:

``` text
Employee
--------------------------------
_id
name
email
password
department
role
```

The password is stored as an encoded password rather than the original
plaintext password.

## 4.2 Leave

A leave document contains:

``` text
Leave
--------------------------------
_id
employeeId
leaveType
startDate
endDate
reason
status
appliedDate
```

The leave stores `employeeId` as a reference value instead of embedding
the entire employee document.

Conceptually:

``` text
Employee
   |
   | 1
   |
   | employeeId
   |
   | N
   v
Leave
```

This allows leave records to remain focused on leave-specific
information while employee information remains in the employee
collection.

------------------------------------------------------------------------

# 5. Security Design

## 5.1 Authentication

Authentication is based on email and password.

The authentication process retrieves the employee using the supplied
email and verifies the password using the configured password encoder.

After successful authentication, a JWT is generated.

The client sends this token with subsequent protected requests.

## 5.2 Authorization

Authorization is role based.

The application uses roles such as:

``` text
ROLE_EMPLOYEE
ROLE_ADMIN
```

Controller-level restrictions include:

``` text
ADMIN
  |
  +-- Get all employees
  +-- Delete employees
  +-- Get all leaves
  +-- Approve leaves
  +-- Reject leaves
  +-- Delete leaves

EMPLOYEE
  |
  +-- Apply for leave
  +-- Access permitted employee operations
  +-- Access own leave information
```

Authorization is not limited to controller annotations. The service
layer also performs ownership checks for operations where an employee
should only access their own resources.

For example, when an employee requests another employee's leave
information, the service compares the authenticated identity with the
requested employee's identity and rejects the operation.

------------------------------------------------------------------------

# 6. Business Rules

The main business rules currently implemented are:

### Leave date validation

A leave cannot have a start date after its end date.

``` text
fromDate > toDate
       |
       v
InvalidLeaveDateException
```

### Leave overlap

An employee cannot create a leave that overlaps an existing `PENDING` or
`APPROVED` leave.

``` text
PENDING / APPROVED
        |
        | overlapping dates
        v
   reject request
```

### Leave lifecycle

The current lifecycle is:

``` text
             +----------+
             |  PENDING |
             +----+-----+
                  |
          +-------+-------+
          |               |
          v               v
    +-----------+    +-----------+
    | APPROVED  |    | REJECTED  |
    +-----------+    +-----------+
```

### Resource ownership

An employee can access leave data belonging to themselves, while
administrators can access resources permitted by their administrative
role.

------------------------------------------------------------------------

# 7. Error and Failure Handling

The application uses application-specific exceptions for important
business failures.

Examples include:

``` text
EmployeeNotFoundException
LeaveNotFoundException
InvalidLeaveDateException
InvalidLeaveOverlapException
AccessDeniedException
```

The controller layer is kept focused on HTTP handling while exceptions
are handled centrally through the application's exception-handling
mechanism.

The resulting API can distinguish between cases such as:

``` text
Invalid input              -> 400
Resource not found         -> 404
Access denied              -> 403
Conflicting leave request  -> 409
```

This gives API clients a predictable error model instead of requiring
every controller method to construct error responses independently.

------------------------------------------------------------------------

# 8. Notification Design

Leave-related operations trigger email notifications through
`EmailService`.

Current flow:

``` text
LeaveService
     |
     v
EmailService
     |
     v
JavaMailSender
     |
     v
External email provider
```

Notifications are triggered after the relevant leave operation, such as:

``` text
Leave applied  -> application notification
Leave approved -> approval notification
Leave rejected -> rejection notification
```

## Current limitation

Email delivery is part of the synchronous application flow.

This means an external email-provider problem can affect the request
path.

A future design could separate notification delivery from the main
request:

``` text
LeaveService
     |
     | save leave
     v
Publish event
     |
     v
Message Broker
     |
     v
Email Worker
     |
     v
Email Provider
```

This is a proposed future architecture, not part of the current
implementation.

------------------------------------------------------------------------

# 9. Testing Architecture

The project uses both unit tests and integration tests.

## 9.1 Unit Tests

Service tests isolate business logic using Mockito.

For example, `LeaveServiceTest` verifies:

-   successful leave creation,
-   invalid dates,
-   overlapping leave,
-   missing employees,
-   ownership restrictions,
-   approval/rejection behavior,
-   repository interactions,
-   email-service interactions.

The service can therefore be tested without requiring a real MongoDB
instance.

## 9.2 Integration Tests

The integration test starts the Spring Boot application context and uses
MockMvc to exercise HTTP endpoints.

Testcontainers starts a real MongoDB container for the test environment.

``` text
Integration Test
      |
      +--> Spring Boot Application Context
      |
      +--> Spring Security
      |
      +--> MockMvc
      |
      +--> Real MongoDB
              |
              v
        Testcontainers
```

The email service is mocked in these tests so that integration tests do
not send real emails.

This gives a useful boundary:

-   application, security, controllers, services and MongoDB are
    exercised together;
-   external email delivery is isolated.

The integration suite has been executed successfully in the local
environment and through GitHub Actions.

------------------------------------------------------------------------

# 10. CI Pipeline

The repository uses GitHub Actions to automatically execute the test
suite when code is pushed to the main branch and for pull requests
targeting the main branch.

``` text
Developer
    |
    | git push
    v
GitHub
    |
    v
GitHub Actions
    |
    +--> Checkout
    |
    +--> Set up Java
    |
    +--> Maven
    |
    +--> Unit Tests
    |
    +--> Integration Tests
              |
              v
        Testcontainers
              |
              v
           MongoDB
    |
    v
PASS / FAIL
```

The purpose of CI is not to make the application runnable. The
application can run without CI.

CI provides an automated and repeatable verification step so that
changes are tested in a clean environment instead of relying only on the
developer's local machine.

------------------------------------------------------------------------

# 11. Scalability Considerations

The current architecture is appropriate for a relatively small
application, but several areas would need attention as usage increases.

## 11.1 Application scaling

The Spring Boot application is currently a single application instance.

Because the application is a stateless REST API with JWT-based
authentication, multiple application instances could be placed behind a
load balancer.

``` text
                    Load Balancer
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
           API-1       API-2       API-3
             |           |           |
             +-----------+-----------+
                         |
                         v
                      MongoDB
```

The exact scaling strategy would depend on traffic and database
capacity.

## 11.2 Database scaling

As the number of employees and leave records grows, database query
performance becomes increasingly important.

The leave overlap query is especially important because it is part of
the leave-application path.

Indexes should be designed around the actual query patterns and measured
using MongoDB query plans rather than added blindly.

## 11.3 Caching

The current application does not introduce a cache.

If profiling showed repeated reads of relatively stable data, Redis
could be introduced for selected read-heavy operations.

Caching should not be added simply because the system is "at scale"; it
should address a measured bottleneck.

## 11.4 Asynchronous notifications

Email is a natural candidate for asynchronous processing because it is
an external side effect and does not need to be completed before the
core leave state is persisted.

A message broker and worker could isolate email-provider latency and
failures from the API request path.

## 11.5 Concurrent leave requests

The current overlap check follows a read-then-write pattern:

``` text
Check for overlap
       |
       | none found
       v
Save leave
```

Two concurrent requests could potentially both pass the check before
either request is saved.

At higher concurrency, this becomes a consistency concern.

A production-scale solution would require stronger
concurrency/consistency guarantees around leave allocation, potentially
through a different data model, transactional strategy,
serialization/locking approach, or another mechanism appropriate to the
chosen persistence model.

This is an important limitation of the current implementation and should
be considered before claiming the overlap rule is race-condition proof.

------------------------------------------------------------------------

# 12. Future Architecture

The current system should not be split into microservices prematurely.

A reasonable evolution path would be:

``` text
Current

Client
  |
  v
Spring Boot Monolith
  |
  +--> MongoDB
  |
  +--> Email Provider
```

If scale or organizational requirements justify further separation:

``` text
                         Client
                           |
                           v
                     API Gateway
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
       Employee Service  Leave Service  Auth
             |             |
             |             +------+
             |                    |
             v                    v
          Employee DB          Leave DB
                                  |
                                  v
                           Message Broker
                                  |
                                  v
                            Notification
                               Worker
                                  |
                                  v
                           Email Provider
```

The future architecture introduces distributed-system concerns that do
not exist in the current monolith. These changes should therefore be
driven by actual requirements such as independent scaling, team
ownership, deployment independence, or operational isolation.

------------------------------------------------------------------------

# 13. Design Decisions and Trade-offs

## Layered monolith

**Decision:** Keep the application as a single Spring Boot application.

**Reason:** The current domain and scale do not justify the operational
complexity of microservices.

**Trade-off:** Components share one deployment and can therefore not be
independently scaled or deployed.

## MongoDB

**Decision:** Use MongoDB for persistence.

**Reason:** The current domain can be represented naturally as employee
and leave documents, and the application already uses Spring Data
MongoDB.

**Trade-off:** Some consistency/concurrency requirements, especially
around overlapping leave requests, require careful design as concurrency
increases.

## JWT authentication

**Decision:** Use JWT for authenticated API requests.

**Reason:** The API can authenticate requests without maintaining a
traditional server-side HTTP session.

**Trade-off:** Token lifecycle, expiration, revocation, and secret/key
management become important concerns.

## DTOs

**Decision:** Use request and response DTOs.

**Reason:** API contracts are separated from persistence entities and
input validation can be defined at the API boundary.

**Trade-off:** Additional mapping code is required.

## Synchronous email

**Decision:** Trigger email through the service flow after leave
operations.

**Reason:** Simple implementation for the current system.

**Trade-off:** External email-provider latency or failure can affect the
request path. Asynchronous processing would improve isolation at higher
scale.

## Testcontainers

**Decision:** Use a real MongoDB container for integration tests.

**Reason:** MongoDB-dependent behavior should be tested against MongoDB
rather than relying only on mocks.

**Trade-off:** Integration tests require Docker and take longer than
isolated unit tests.

------------------------------------------------------------------------

# 14. Current Architecture Summary

The current system can be summarized as:

``` text
                         +----------------+
                         |     Client     |
                         +-------+--------+
                                 |
                                 | HTTP / REST
                                 v
                    +---------------------------+
                    |      Spring Boot App      |
                    |                           |
                    |  Security / JWT           |
                    |          |                |
                    |  Controllers              |
                    |          |                |
                    |  Services                 |
                    |          |                |
                    |  Repositories             |
                    +----------+----------------+
                               |
                         +-----+-----+
                         |           |
                         v           v
                    +---------+  +---------+
                    | MongoDB |  |  Email  |
                    +---------+  +---------+
```

The main architectural goal is separation of concerns while keeping the
deployment model simple.

The system is currently suitable as a layered backend application. The
most important areas to revisit as scale and concurrency increase are
database query/index design, concurrent leave allocation, and
asynchronous notification processing.
