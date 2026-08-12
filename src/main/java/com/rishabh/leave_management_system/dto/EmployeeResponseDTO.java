package com.rishabh.leave_management_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeResponseDTO {

    private String id;
    private String name;
    private String email;
    private String department;
    private String role;

}
