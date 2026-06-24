package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import com.rishabh.leave_management_system.entity.enums.LeaveType;
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

        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedDate(LocalDate.now());

        return leaveRepository.save(leave);
    }

    public List<Leave> getLeaveByEmployeeId(String employeeId){
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<Leave> getAllLeave(){
        return leaveRepository.findAll();
    }
}
