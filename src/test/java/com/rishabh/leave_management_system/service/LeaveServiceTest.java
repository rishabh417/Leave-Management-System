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
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import com.rishabh.leave_management_system.repository.LeaveRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

}


