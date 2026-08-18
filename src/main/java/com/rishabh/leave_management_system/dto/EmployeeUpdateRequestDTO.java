package com.rishabh.leave_management_system.dto;


import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeUpdateRequestDTO {

    // for employee
    @Size(min = 2, message = "Name must be atleast of 2 characters!")
    private String name;
    @Size(min = 8, message = "Password must be atleast of 8 characters!")
    private String password;

    // for admin in addition with above
    private String role;
    private String department;
}
