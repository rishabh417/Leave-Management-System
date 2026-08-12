package com.rishabh.leave_management_system.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeUpdateRequestDTO {

    // for employee
    private String name;
    private String password;

    // for admin in addition with above
    private String role;
    private String department;
}
