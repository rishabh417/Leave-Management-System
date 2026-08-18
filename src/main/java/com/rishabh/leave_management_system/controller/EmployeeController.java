package com.rishabh.leave_management_system.controller;

import com.rishabh.leave_management_system.dto.EmployeeRequestDTO;
import com.rishabh.leave_management_system.dto.EmployeeResponseDTO;
import com.rishabh.leave_management_system.dto.EmployeeUpdateRequestDTO;
import com.rishabh.leave_management_system.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    public EmployeeResponseDTO createEmployee(
            @Valid @RequestBody EmployeeRequestDTO employeeRequest){
        return employeeService.saveEmployee(employeeRequest);
    }

    @GetMapping("/{id}")
    public EmployeeResponseDTO getEmployeeById(@PathVariable String id){
        return employeeService.getEmployeeById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<EmployeeResponseDTO> getAllEmployees(){
        return employeeService.getAllEmployee();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteEmployeeById(@PathVariable String id){
         employeeService.deleteByid(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @PutMapping("/{id}")
    public EmployeeResponseDTO updateEmployee(@Valid @RequestBody EmployeeUpdateRequestDTO employee, @PathVariable String id){
        return employeeService.updateById(employee,id);
    }


}
