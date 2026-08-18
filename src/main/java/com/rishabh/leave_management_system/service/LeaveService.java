package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.dto.LeaveRequestDTO;
import com.rishabh.leave_management_system.dto.LeaveResponseDTO;
import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import com.rishabh.leave_management_system.exception.EmployeeNotFoundException;
import com.rishabh.leave_management_system.exception.LeaveNotFoundException;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import com.rishabh.leave_management_system.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public LeaveResponseDTO applyLeave(LeaveRequestDTO leaveRequest){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Employee employee = employeeRepository.findByEmail(currentUser);

        if(employee == null){
            throw new EmployeeNotFoundException("Employee with email " + currentUser + " not found");
        }

        Leave leave = new Leave();
        leave.setEmployeeId(employee.getId());
        leave.setLeaveType(leaveRequest.getLeaveType());
        leave.setStartDate(leaveRequest.getFromDate());
        leave.setEndDate(leaveRequest.getToDate());
        leave.setReason(leaveRequest.getReason());

        if(leave.getStartDate().isAfter(leave.getEndDate())){
            throw new RuntimeException("Start date cannot be after end date");
        }

        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedDate(LocalDate.now());

        leaveRepository.save(leave);

        LeaveResponseDTO response = new LeaveResponseDTO();

        response.setLeaveId(leave.getId());
        response.setLeaveType(leave.getLeaveType());
        response.setLeaveStatus(leave.getStatus());
        response.setFromDate(leave.getStartDate());
        response.setToDate(leave.getEndDate());
        response.setAppliedDate(leave.getAppliedDate());
        response.setReason(leave.getReason());

        return response;
    }

    public List<LeaveResponseDTO> getLeaveByEmployeeId(String employeeId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if(employee.isEmpty()){
            throw new EmployeeNotFoundException("Employee with id " + employeeId + " not found");
        }

        if(!isAdmin && !currentUser.equals(employee.get().getEmail())){
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        List<Leave> leaves = leaveRepository.findByEmployeeId(employeeId);

        List<LeaveResponseDTO> leaveResponseDTOS = new ArrayList<>();
        for (Leave leave1 : leaves) {
            LeaveResponseDTO leaveResponseDTO = new LeaveResponseDTO();
            leaveResponseDTO.setLeaveId(leave1.getId());
            leaveResponseDTO.setLeaveType(leave1.getLeaveType());
            leaveResponseDTO.setLeaveStatus(leave1.getStatus());
            leaveResponseDTO.setReason(leave1.getReason());
            leaveResponseDTO.setAppliedDate(leave1.getAppliedDate());
            leaveResponseDTO.setFromDate(leave1.getStartDate());
            leaveResponseDTO.setToDate(leave1.getEndDate());
            leaveResponseDTOS.add(leaveResponseDTO);
        }

        return leaveResponseDTOS;
    }

    public LeaveResponseDTO getLeaveById(String id){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        Leave leave = leaveRepository.findById(id).orElseThrow(
                () -> new LeaveNotFoundException("Leave with id " + id + " not found")
        );

        if(!isAdmin){
            Employee employee = employeeRepository.findByEmail(currentUser);

            if(employee == null){
                throw new EmployeeNotFoundException("Employee with email " + currentUser + " not found");
            }
            if(!leave.getEmployeeId().equals(employee.getId())){
            throw new AccessDeniedException("You are not allowed to perform this action");
            }
        }

        LeaveResponseDTO response = new LeaveResponseDTO();

        response.setLeaveId(leave.getId());
        response.setLeaveType(leave.getLeaveType());
        response.setLeaveStatus(leave.getStatus());
        response.setFromDate(leave.getStartDate());
        response.setToDate(leave.getEndDate());
        response.setAppliedDate(leave.getAppliedDate());
        response.setReason(leave.getReason());

        return response;

    }

    public List<Leave> getAllLeave(){

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        if(!isAdmin){
            throw new AccessDeniedException(
                    "You are not allowed to perform this action"
            );
        }

        return leaveRepository.findAll();
    }

    public Leave approveLeave(String leaveId){

        Leave leave = leaveRepository.findById(leaveId).orElseThrow(
                () -> new LeaveNotFoundException("Leave with id " + leaveId + " not found")
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        if(!isAdmin){
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        if(leave.getStatus() == LeaveStatus.APPROVED){
            throw new IllegalStateException("Leave is already approved");
        }
        if(leave.getStatus() == LeaveStatus.REJECTED){
            throw new IllegalStateException("Leave is already rejected");
        }

        leave.setStatus(LeaveStatus.APPROVED);
        return leaveRepository.save(leave);

    }

    public Leave rejectLeave(String leaveId){

        Leave leave = leaveRepository.findById(leaveId).orElseThrow(
                () -> new LeaveNotFoundException("Leave with id " + leaveId + " not found")
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        if(!isAdmin){
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        if(leave.getStatus() == LeaveStatus.REJECTED){
            throw new IllegalStateException("Leave is already rejected");
        }

        if(leave.getStatus() == LeaveStatus.APPROVED){
            throw new IllegalStateException("Leave is already approved");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        return leaveRepository.save(leave);
    }

    public void deleteLeave(String leaveId){

        Leave leave = leaveRepository.findById(leaveId).orElseThrow(
                () -> new LeaveNotFoundException("Leave with id " + leaveId + " not found")
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(
                        a -> a.getAuthority().equals("ROLE_ADMIN")
                );

        if(!isAdmin){
            throw new AccessDeniedException("You are not allowed to perform this action");
        }

        leaveRepository.deleteById(leaveId);
    }

}
