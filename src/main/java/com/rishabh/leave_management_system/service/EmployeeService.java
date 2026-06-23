package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee saveEmployee(Employee employee){
        return employeeRepository.save(employee);
    }

    public Employee getEmployeeById(String id){
        return employeeRepository.findById(id).orElse(null);
    }

    public void deleteByid(String id){
        employeeRepository.deleteById(id);
    }

    public List<Employee> getAllEmployee(){
        return employeeRepository.findAll();
    }
}
