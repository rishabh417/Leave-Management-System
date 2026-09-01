package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.dto.LeaveRequestDTO;
import com.rishabh.leave_management_system.dto.LeaveResponseDTO;
import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import com.rishabh.leave_management_system.entity.enums.LeaveType;
import com.rishabh.leave_management_system.exception.EmployeeNotFoundException;
import com.rishabh.leave_management_system.exception.InvalidLeaveDateException;
import com.rishabh.leave_management_system.exception.InvalidLeaveOverlapException;
import com.rishabh.leave_management_system.exception.LeaveNotFoundException;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import com.rishabh.leave_management_system.repository.LeaveRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveService leaveService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void applyLeave_shouldThrowException_whenEmployeeDoesNotExist() {

        LeaveRequestDTO leaveRequestDTO = new LeaveRequestDTO();

        when(authentication.getName()).thenReturn("test@gmail.com");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(employeeRepository.findByEmail("test@gmail.com"))
                .thenReturn(null);

        // act and assert
        assertThrows(
                EmployeeNotFoundException.class,
                () -> leaveService.applyLeave(leaveRequestDTO)
        );

        verify(
                leaveRepository, never()
        ).save(any());

        verify(
                emailService, never()
        ).sendLeaveAppliedEmail(any(), any());


    }

    @Test
    void applyLeave_shouldCreateLeave_whenRequestIsValid() {
        when(authentication.getName()).thenReturn("test@gmail.com");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        LeaveRequestDTO leaveRequestDTO = new LeaveRequestDTO();
        leaveRequestDTO.setFromDate(LocalDate.of(2026, 9, 10));
        leaveRequestDTO.setToDate(LocalDate.of(2026, 9, 12));
        leaveRequestDTO.setReason("Personal Work");
        leaveRequestDTO.setLeaveType(LeaveType.CASUAL);

        when(employeeRepository.findByEmail(employee.getEmail())).thenReturn(employee);

        when(leaveRepository
                .existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        "employee-123",
                        List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                        LocalDate.of(2026, 9, 12),
                        LocalDate.of(2026, 9, 10)
                ))
                .thenReturn(false);

        LeaveResponseDTO response = leaveService.applyLeave(leaveRequestDTO);

        assertEquals(LeaveType.CASUAL, response.getLeaveType());
        assertEquals(LeaveStatus.PENDING, response.getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.getToDate());
        assertEquals("Personal Work", response.getReason());

        verify(leaveRepository).save(any(Leave.class));

        verify(emailService).sendLeaveAppliedEmail("test@gmail.com", "CASUAL");

    }

    @Test
    void invalidLeaveDates_shouldThrowException() {

        when(authentication.getName()).thenReturn("test@gmail.com");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        LeaveRequestDTO leaveRequestDTO = new LeaveRequestDTO();
        leaveRequestDTO.setFromDate(LocalDate.of(2026, 9, 12));
        leaveRequestDTO.setToDate(LocalDate.of(2026, 9, 10));
        leaveRequestDTO.setReason("Personal Work");
        leaveRequestDTO.setLeaveType(LeaveType.CASUAL);

        when(employeeRepository.findByEmail(employee.getEmail())).thenReturn(employee);

        assertThrows(
                InvalidLeaveDateException.class,
                () -> leaveService.applyLeave(leaveRequestDTO)
        );

        verify(leaveRepository, never()).save(any(Leave.class));
        verify(emailService, never()).sendLeaveAppliedEmail(any(), any());

    }

    @Test
    void overLappingLeaveDates_shouldThrowException() {

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        LeaveRequestDTO leaveRequestDTO = new LeaveRequestDTO();
        leaveRequestDTO.setFromDate(LocalDate.of(2026, 9, 10));
        leaveRequestDTO.setToDate(LocalDate.of(2026, 9, 12));
        leaveRequestDTO.setReason("Personal Work");
        leaveRequestDTO.setLeaveType(LeaveType.CASUAL);

        when(employeeRepository.findByEmail(employee.getEmail())).thenReturn(employee);

        when(
                leaveRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        "employee-123",
                        List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                        LocalDate.of(2026, 9, 12),
                        LocalDate.of(2026, 9, 10)
                )
        ).thenReturn(true);

        assertThrows(
                InvalidLeaveOverlapException.class,
                () -> {
                    leaveService.applyLeave(leaveRequestDTO);
                }
        );

        verify(leaveRepository, never()).save(any(Leave.class));
        verify(emailService, never()).sendLeaveAppliedEmail(any(), any());

    }

    @Test
    void getLeavesByEmployeeId_shouldReturnLeaves_whenEmployeeRequestsOwnLeaves(){
        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        when(employeeRepository.findById(employee.getId()))
                .thenReturn(Optional.of(employee));

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(
                leaveRepository.findByEmployeeId(employee.getId())
        ).thenReturn(List.of(leave));

        List<LeaveResponseDTO> response =
                leaveService.getLeaveByEmployeeId(employee.getId());

        assertEquals(1, response.size());
        assertEquals(LeaveType.CASUAL, response.get(0).getLeaveType());
        assertEquals(LeaveStatus.PENDING, response.get(0).getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.get(0).getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.get(0).getToDate());

        verify(leaveRepository).findByEmployeeId("employee-123");
    }

    @Test
    void getLeavesByEmployeeId_shouldThrowException_whenEmployeeRequestsAnotherEmployeesLeaves(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-456",
                "Another employee",
                "another@gmail.com",
                "password"
        );

        when(employeeRepository.findById("employee-456"))
                .thenReturn(Optional.of(employee));

        assertThrows(
                AccessDeniedException.class,
                () -> leaveService.getLeaveByEmployeeId("employee-456")
        );

        verify(leaveRepository,never())
                .findByEmployeeId("employee-456");

    }

    @Test
    void getLeaveById_shouldReturnLeave_whenEmployeeRequestsOwnLeave(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        when(employeeRepository.findByEmail("test@gmail.com"))
                .thenReturn(employee);


        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        LeaveResponseDTO response = leaveService.getLeaveById("leave-123");

        assertEquals("leave-123", response.getLeaveId());
        assertEquals(LeaveType.CASUAL, response.getLeaveType());
        assertEquals(LeaveStatus.PENDING, response.getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.getToDate());
        assertEquals("Personal Work", response.getReason());

        verify(leaveRepository).findById("leave-123");
        verify(employeeRepository).findByEmail("test@gmail.com");

    }

    @Test
    void getLeaveById_shouldThrowException_whenLeaveDoesNotExist(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);



        when(leaveRepository.findById("leave-999")).thenReturn(Optional.empty());

        assertThrows(
                LeaveNotFoundException.class,
                () ->  leaveService.getLeaveById("leave-999")
        );

        verify(leaveRepository).findById("leave-999");


    }

    @Test
    void approveLeave_shouldApproveLeave_whenAdminApproves(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        when(employeeRepository.findById("employee-123"))
                .thenReturn(Optional.of(employee));

        LeaveResponseDTO response = leaveService.approveLeave("leave-123");

        assertEquals("leave-123", response.getLeaveId());
        assertEquals(LeaveType.CASUAL, response.getLeaveType());
        assertEquals(LeaveStatus.APPROVED, response.getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.getToDate());
        assertEquals("Personal Work", response.getReason());

        verify(leaveRepository).save(leave);

        verify(emailService).sendLeaveApprovedEmail("test@gmail.com","CASUAL");


    }

    @Test
    void approveLeave_shouldThrowException_whenEmployeeWantsToApproveLeave(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        )).when(authentication).getAuthorities();



        assertThrows(
                AccessDeniedException.class,
                () ->  leaveService.approveLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);

        verify(emailService,never()).sendLeaveApprovedEmail(any(),any());


    }


    @Test
    void approveLeave_shouldThrowException_whenLeaveDoesNotExist(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(leaveRepository.findById("leave-999"))
                .thenReturn(Optional.empty());


        assertThrows(
                LeaveNotFoundException.class,
                () ->  leaveService.approveLeave("leave-999")
        );

        verify(leaveRepository,never()).save(any());

        verify(emailService,never()).sendLeaveApprovedEmail(any(),any());


    }

    @Test
    void rejectLeave_shouldRejectLeave_whenAdminRejectLeaves(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        when(employeeRepository.findById("employee-123"))
                .thenReturn(Optional.of(employee));

        LeaveResponseDTO response = leaveService.rejectLeave("leave-123");

        assertEquals("leave-123", response.getLeaveId());
        assertEquals(LeaveType.CASUAL, response.getLeaveType());
        assertEquals(LeaveStatus.REJECTED, response.getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.getToDate());
        assertEquals("Personal Work", response.getReason());


        verify(leaveRepository).save(leave);

        verify(emailService).sendLeaveRejectedEmail("test@gmail.com","CASUAL");


    }

    @Test
    void rejectLeave_shouldThrowException_whenEmployeeRejectsLeave(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        assertThrows(
                AccessDeniedException.class,
                () -> leaveService.rejectLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);
        verify(emailService,never()).sendLeaveRejectedEmail("test@gmail.com","CASUAL");

    }

    @Test
    void rejectLeave_shouldThrowException_whenLeaveDoesNotExist(){

        when(leaveRepository.findById("leave-999"))
                .thenReturn(Optional.empty());

        assertThrows(
                LeaveNotFoundException.class,
                () -> leaveService.rejectLeave("leave-999")
        );

        verify(leaveRepository,never()).save(any());
        verify(emailService,never()).sendLeaveRejectedEmail(any(),any());
    }

    @Test
    void deleteLeave_shouldDeleteLeave_whenAdminDeleteLeaves(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        leaveService.deleteLeave("leave-123");

        verify(leaveRepository).deleteById("leave-123");

    }

    @Test
    void deleteLeave_shouldThrowException_whenEmployeeDeleteLeaves(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));


        assertThrows(
                AccessDeniedException.class,
                () -> leaveService.deleteLeave("leave-123")
        );

        verify(leaveRepository,never()).deleteById("leave-123");

    }

    @Test
    void deleteLeave_shouldThrowException_whenLeaveDoesNotExist(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.empty());

        assertThrows(
            LeaveNotFoundException.class,
                () -> leaveService.deleteLeave("leave-123")
        );

        verify(leaveRepository,never()).deleteById("leave-123");

    }

    @Test
    void getAllLeaves_shouldGiveAllLeaves_whenAdminAsked(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findAll())
                .thenReturn(Collections.singletonList(leave));

        List<LeaveResponseDTO> response =
                leaveService.getAllLeave();

        assertEquals(1, response.size());
        assertEquals(LeaveType.CASUAL, response.get(0).getLeaveType());
        assertEquals(LeaveStatus.PENDING, response.get(0).getLeaveStatus());
        assertEquals(LocalDate.of(2026, 9, 10), response.get(0).getFromDate());
        assertEquals(LocalDate.of(2026, 9, 12), response.get(0).getToDate());

        verify(leaveRepository).findAll();

    }

    @Test
    void getAllLeaves_shouldThrowException_whenEmployeeAsked(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

       assertThrows(
               AccessDeniedException.class,
               () -> leaveService.getAllLeave()
       );

        verify(leaveRepository,never()).findAll();

    }

    @Test
    void getLeavesByEmployeeId_shouldThrowException_whenEmployeeDoesNotExist(){

        when(authentication.getName()).thenReturn("employee-123");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(employeeRepository.findById("employee-123"))
                .thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> leaveService.getLeaveByEmployeeId("employee-123")
        );

        verify(leaveRepository,never()).findByEmployeeId("employee-123");
    }


    @Test
    void getLeaveById_shouldThrowException_whenEmployeeRequestsAnotherEmployeesLeave(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-456");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.PENDING);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        Employee employee = new Employee(
                "employee-123",
                "Test Employee",
                "test@gmail.com",
                "password"
        );

        when(employeeRepository.findByEmail("test@gmail.com"))
                .thenReturn(employee);


        assertThrows(
                AccessDeniedException.class,
                () -> leaveService.getLeaveById("leave-123")
        );

        verify(leaveRepository).findById("leave-123");

    }

    @Test
    void approveLeave_shouldThrowException_whenLeaveIsAlreadyApproved(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.APPROVED);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        assertThrows(
                IllegalStateException.class,
                () -> leaveService.approveLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);
        verify(emailService,never()).sendLeaveApprovedEmail("leave-123","CASUAL");

    }

    @Test
    void approveLeave_shouldThrowException_whenLeaveIsAlreadyRejected(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.REJECTED);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        assertThrows(
                IllegalStateException.class,
                () -> leaveService.approveLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);
        verify(emailService,never()).sendLeaveApprovedEmail("leave-123","CASUAL");

    }

    @Test
    void rejectLeave_shouldThrowException_whenLeaveIsAlreadyRejected(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.REJECTED);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        assertThrows(
                IllegalStateException.class,
                () -> leaveService.rejectLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);
        verify(emailService,never()).sendLeaveRejectedEmail("leave-123","CASUAL");
    }

    @Test
    void rejectLeave_shouldThrowException_whenLeaveIsAlreadyApproved(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Leave leave = new Leave();

        leave.setId("leave-123");
        leave.setEmployeeId("employee-123");
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 9, 10));
        leave.setEndDate(LocalDate.of(2026, 9, 12));
        leave.setReason("Personal Work");
        leave.setStatus(LeaveStatus.APPROVED);

        when(leaveRepository.findById("leave-123"))
                .thenReturn(Optional.of(leave));

        assertThrows(
                IllegalStateException.class,
                () -> leaveService.rejectLeave("leave-123")
        );

        verify(leaveRepository,never()).save(leave);
        verify(emailService,never()).sendLeaveRejectedEmail("leave-123","CASUAL");
    }


}


