package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import com.rishabh.leave_management_system.exception.LeaveNotFoundException;
import com.rishabh.leave_management_system.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    public Leave applyLeave(Leave leave){

        if(leave.getStartDate().isAfter(leave.getEndDate())){
            throw new RuntimeException("Start date cannot be after end date");
        }

        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedDate(LocalDate.now());

        return leaveRepository.save(leave);
    }

    public List<Leave> getLeaveByEmployeeId(String employeeId){
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public Leave getLeaveById(String id){
        return leaveRepository.findById(id).orElse(null);
    }

    public List<Leave> getAllLeave(){
        return leaveRepository.findAll();
    }

    public Leave approveLeave(String leaveId){

        Leave leave = leaveRepository.findById(leaveId).orElse(null);
        if(leave == null) throw new LeaveNotFoundException("Leave for the given leave id not found");

        leave.setStatus(LeaveStatus.APPROVED);
        return leaveRepository.save(leave);

    }

    public Leave rejectLeave(String leaveId){

        Leave leave = leaveRepository.findById(leaveId).orElse(null);
        if(leave == null) throw new LeaveNotFoundException("Leave for the given leave id not found");

        leave.setStatus(LeaveStatus.REJECTED);
        return leaveRepository.save(leave);
    }
}
