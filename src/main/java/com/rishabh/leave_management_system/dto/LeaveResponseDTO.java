package com.rishabh.leave_management_system.dto;

import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import com.rishabh.leave_management_system.entity.enums.LeaveType;

import java.time.LocalDate;

public class LeaveResponseDTO {

    private String leaveId;
    private LeaveType leaveType;
    private LeaveStatus leaveStatus;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate appliedDate;
    private String reason;

}
