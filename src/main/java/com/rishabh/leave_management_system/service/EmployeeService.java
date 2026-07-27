package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.config.PasswordConfig;
import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.exception.EmployeeNotFoundException;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Employee saveEmployee(Employee employee){

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        employee.setRole(employee.getRole() == null ? "EMPLOYEE" : employee.getRole());

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


    public Employee updateById(Employee newEmployee, String id) {

        Optional<Employee> oldEmployee = employeeRepository.findById(id);

        if(oldEmployee.isPresent()){
            Employee employee = oldEmployee.get();
            employee.setName(newEmployee.getName());
            employee.setEmail(newEmployee.getEmail());
            employee.setDepartment(newEmployee.getDepartment());
            employee.setPassword(newEmployee.getPassword());

            employeeRepository.save(employee);
            return employee;
        }
        throw new EmployeeNotFoundException("Given Employee with id: "+id+" not found");
    }


}
