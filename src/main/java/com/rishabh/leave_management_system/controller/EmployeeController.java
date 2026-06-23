package com.rishabh.leave_management_system.controller;

import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee){
        return employeeService.saveEmployee(employee);
    }

    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable String id){
        return employeeService.getEmployeeById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteEmployeeById(@PathVariable String id){
         employeeService.deleteByid(id);
    }

    @GetMapping
    public List<Employee> getAllEmployees(){
        return employeeService.getAllEmployee();
    }


}
