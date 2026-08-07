package com.rishabh.leave_management_system.dto;

import com.rishabh.leave_management_system.entity.enums.LeaveType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class LeaveRequestDTO {

    @NotNull
    private String fromDate;

    @NotNull
    private LocalDate toDate;

    @NotNull
    @Size(max = 500)
    private LocalDate reason;

    @NotNull
    private LeaveType leaveType;



}
