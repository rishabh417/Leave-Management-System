package com.rishabh.leave_management_system;

import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LeaveManagementSystemApplicationTests {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer =
			new MongoDBContainer("mongo:8.0");


	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private com.rishabh.leave_management_system.service.EmailService emailService;

	@BeforeEach
	void setup() {

		employeeRepository.deleteAll();

		Employee employee = new Employee();
		employee.setName("Integration Test User");
		employee.setEmail("integration@test.com");
		employee.setPassword(passwordEncoder.encode("password123"));
		employee.setDepartment("IT");
		employee.setRole("EMPLOYEE");

		employeeRepository.save(employee);

	}

	@Test
	void login_shouldReturnJwtToken_whenCredentialsAreValid() throws Exception {

		String requestBody = """
                {
                    "email": "integration@test.com",
                    "password": "password123"
                }
                """;

		mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(requestBody)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void login_shouldReturnUnauthorized_whenPasswordIsInvalid() throws Exception {

		String requestBody = """
            {
                "email": "integration@test.com",
                "password": "wrongPassword"
            }
            """;

		mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(requestBody)
				)
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getEmployeeById_shouldReturnEmployee_whenAuthenticated() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/employee/" + employee.getId())
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("integration@test.com"))
				.andExpect(jsonPath("$.name").value("Integration Test User"));
	}

	@Test
	void createEmployee_shouldCreateEmployee_whenRequestIsValid() throws Exception {

		String requestBody = """
            {
                "name": "New Integration Employee",
                "email": "newemployee@test.com",
                "password": "password123"
            }
            """;

		mockMvc.perform(
						post("/api/employee")
								.contentType(MediaType.APPLICATION_JSON)
								.content(requestBody)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("New Integration Employee"))
				.andExpect(jsonPath("$.email").value("newemployee@test.com"));
	}

	@Test
	void createEmployee_shouldReturnBadRequest_whenPasswordIsTooShort() throws Exception {

		String requestBody = """
            {
                "name": "Invalid Employee",
                "email": "invalid@test.com",
                "password": "123"
            }
            """;

		mockMvc.perform(
						post("/api/employee")
								.contentType(MediaType.APPLICATION_JSON)
								.content(requestBody)
				)
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateEmployee_shouldUpdateName_whenEmployeeIsAuthenticated() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String updateRequest = """
            {
                "name": "Updated Integration User"
            }
            """;

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.put("/api/employee/" + employee.getId())
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(updateRequest)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Integration User"))
				.andExpect(jsonPath("$.email").value("integration@test.com"));
	}

	@Test
	void getAllEmployees_shouldReturnForbidden_whenUserIsEmployee() throws Exception {

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/employee")
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void getAllEmployees_shouldReturnEmployees_whenUserIsAdmin() throws Exception {

		Employee admin = new Employee();
		admin.setName("Integration Admin");
		admin.setEmail("admin@test.com");
		admin.setPassword(passwordEncoder.encode("admin12345"));
		admin.setDepartment("HR");
		admin.setRole("ADMIN");

		employeeRepository.save(admin);

		String loginRequest = """
            {
                "email": "admin@test.com",
                "password": "admin12345"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/employee")
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}


	@Test
	void deleteEmployee_shouldReturnForbidden_whenUserIsEmployee() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.delete("/api/employee/" + employee.getId())
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void applyLeave_shouldCreateLeave_whenEmployeeIsAuthenticated() throws Exception {

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-09-10",
                "toDate": "2026-09-12",
                "reason": "Personal work",
                "leaveType": "CASUAL"
            }
            """;

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveType").value("CASUAL"))
				.andExpect(jsonPath("$.leaveStatus").value("PENDING"))
				.andExpect(jsonPath("$.fromDate").value("2026-09-10"))
				.andExpect(jsonPath("$.toDate").value("2026-09-12"))
				.andExpect(jsonPath("$.reason").value("Personal work"));
	}

	@Test
	void applyLeave_shouldReturnBadRequest_whenToDateIsBeforeFromDate() throws Exception {

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-09-15",
                "toDate": "2026-09-10",
                "reason": "Invalid date range",
                "leaveType": "CASUAL"
            }
            """;

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isBadRequest());
	}

	@Test
	void getLeaveByEmployeeId_shouldReturnEmployeeLeaves() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-09-20",
                "toDate": "2026-09-22",
                "reason": "Family function",
                "leaveType": "CASUAL"
            }
            """;

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk());

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/leave/employee/" + employee.getId())
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].reason").value("Family function"))
				.andExpect(jsonPath("$[0].leaveType").value("CASUAL"));
	}

	@Test
	void approveLeave_shouldApproveLeave_whenUserIsAdmin() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		// Login as employee
		String employeeLoginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String employeeLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(employeeLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String employeeToken = com.jayway.jsonpath.JsonPath
				.parse(employeeLoginResponse)
				.read("$.token");

		// Apply leave
		String leaveRequest = """
            {
                "fromDate": "2026-09-25",
                "toDate": "2026-09-26",
                "reason": "Family function",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + employeeToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveStatus").value("PENDING"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		// Create admin
		Employee admin = new Employee();
		admin.setName("Integration Admin");
		admin.setEmail("admin@test.com");
		admin.setPassword(passwordEncoder.encode("admin12345"));
		admin.setDepartment("HR");
		admin.setRole("ADMIN");

		employeeRepository.save(admin);

		// Login as admin
		String adminLoginRequest = """
            {
                "email": "admin@test.com",
                "password": "admin12345"
            }
            """;

		String adminLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(adminLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String adminToken = com.jayway.jsonpath.JsonPath
				.parse(adminLoginResponse)
				.read("$.token");

		// Approve leave
		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.put("/api/leave/approve/" + leaveId)
								.header("Authorization", "Bearer " + adminToken)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveId").value(leaveId))
				.andExpect(jsonPath("$.leaveStatus").value("APPROVED"));
	}

	@Test
	void rejectLeave_shouldRejectLeave_whenUserIsAdmin() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		// Login as employee
		String employeeLoginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String employeeLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(employeeLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String employeeToken = com.jayway.jsonpath.JsonPath
				.parse(employeeLoginResponse)
				.read("$.token");

		// Apply leave
		String leaveRequest = """
            {
                "fromDate": "2026-10-01",
                "toDate": "2026-10-02",
                "reason": "Personal work",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + employeeToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveStatus").value("PENDING"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		// Create admin
		Employee admin = new Employee();
		admin.setName("Integration Admin");
		admin.setEmail("admin@test.com");
		admin.setPassword(passwordEncoder.encode("admin12345"));
		admin.setDepartment("HR");
		admin.setRole("ADMIN");

		employeeRepository.save(admin);

		// Login as admin
		String adminLoginRequest = """
            {
                "email": "admin@test.com",
                "password": "admin12345"
            }
            """;

		String adminLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(adminLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String adminToken = com.jayway.jsonpath.JsonPath
				.parse(adminLoginResponse)
				.read("$.token");

		// Reject leave
		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.put("/api/leave/reject/" + leaveId)
								.header("Authorization", "Bearer " + adminToken)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveId").value(leaveId))
				.andExpect(jsonPath("$.leaveStatus").value("REJECTED"));
	}

	@Test
	void approveLeave_shouldReturnForbidden_whenUserIsEmployee() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-10-10",
                "toDate": "2026-10-11",
                "reason": "Personal work",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.put("/api/leave/approve/" + leaveId)
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectLeave_shouldReturnForbidden_whenUserIsEmployee() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-10-15",
                "toDate": "2026-10-16",
                "reason": "Personal work",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.put("/api/leave/reject/" + leaveId)
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void getLeaveById_shouldReturnLeave_whenLeaveExists() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		String leaveRequest = """
            {
                "fromDate": "2026-10-20",
                "toDate": "2026-10-21",
                "reason": "Personal work",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/leave/id/" + leaveId)
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leaveId").value(leaveId))
				.andExpect(jsonPath("$.leaveType").value("CASUAL"))
				.andExpect(jsonPath("$.leaveStatus").value("PENDING"))
				.andExpect(jsonPath("$.reason").value("Personal work"))
				.andExpect(jsonPath("$.fromDate").value("2026-10-20"))
				.andExpect(jsonPath("$.toDate").value("2026-10-21"));
	}

	@Test
	void getAllLeave_shouldReturnLeaves_whenUserIsAdmin() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		// Login as employee
		String employeeLoginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String employeeLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(employeeLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String employeeToken = com.jayway.jsonpath.JsonPath
				.parse(employeeLoginResponse)
				.read("$.token");

		// Apply leave
		String leaveRequest = """
            {
                "fromDate": "2026-10-25",
                "toDate": "2026-10-26",
                "reason": "Family function",
                "leaveType": "CASUAL"
            }
            """;

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + employeeToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk());

		// Create admin
		Employee admin = new Employee();
		admin.setName("Integration Admin");
		admin.setEmail("admin@test.com");
		admin.setPassword(passwordEncoder.encode("admin12345"));
		admin.setDepartment("HR");
		admin.setRole("ADMIN");

		employeeRepository.save(admin);

		// Login as admin
		String adminLoginRequest = """
            {
                "email": "admin@test.com",
                "password": "admin12345"
            }
            """;

		String adminLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(adminLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String adminToken = com.jayway.jsonpath.JsonPath
				.parse(adminLoginResponse)
				.read("$.token");

		// Get all leaves
		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/leave")
								.header("Authorization", "Bearer " + adminToken)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.reason == 'Family function')]").isNotEmpty());
	}

	@Test
	void getAllLeave_shouldReturnForbidden_whenUserIsEmployee() throws Exception {

		String loginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String loginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(loginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.jayway.jsonpath.JsonPath
				.parse(loginResponse)
				.read("$.token");

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/leave")
								.header("Authorization", "Bearer " + token)
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteLeave_shouldDeleteLeave_whenUserIsAdmin() throws Exception {

		Employee employee = employeeRepository.findByEmail("integration@test.com");

		// Login as employee
		String employeeLoginRequest = """
            {
                "email": "integration@test.com",
                "password": "password123"
            }
            """;

		String employeeLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(employeeLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String employeeToken = com.jayway.jsonpath.JsonPath
				.parse(employeeLoginResponse)
				.read("$.token");

		// Apply leave
		String leaveRequest = """
            {
                "fromDate": "2026-11-01",
                "toDate": "2026-11-02",
                "reason": "Leave to be deleted",
                "leaveType": "CASUAL"
            }
            """;

		String leaveResponse = mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.post("/api/leave")
								.header("Authorization", "Bearer " + employeeToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content(leaveRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String leaveId = com.jayway.jsonpath.JsonPath
				.parse(leaveResponse)
				.read("$.leaveId");

		// Create admin
		Employee admin = new Employee();
		admin.setName("Integration Admin");
		admin.setEmail("admin@test.com");
		admin.setPassword(passwordEncoder.encode("admin12345"));
		admin.setDepartment("HR");
		admin.setRole("ADMIN");

		employeeRepository.save(admin);

		// Login as admin
		String adminLoginRequest = """
            {
                "email": "admin@test.com",
                "password": "admin12345"
            }
            """;

		String adminLoginResponse = mockMvc.perform(
						post("/api/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(adminLoginRequest)
				)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String adminToken = com.jayway.jsonpath.JsonPath
				.parse(adminLoginResponse)
				.read("$.token");

		// Delete leave
		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.delete("/api/leave/delete/" + leaveId)
								.header("Authorization", "Bearer " + adminToken)
				)
				.andExpect(status().isOk());

		// Verify it no longer exists
		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/leave/id/" + leaveId)
								.header("Authorization", "Bearer " + adminToken)
				)
				.andExpect(status().isNotFound());
	}

	@Test
	void getAllEmployees_shouldReturnUnauthorized_whenUserIsNotAuthenticated() throws Exception {

		mockMvc.perform(
						org.springframework.test.web.servlet.request.MockMvcRequestBuilders
								.get("/api/employee")
				)
				.andExpect(status().isUnauthorized());
	}


}
