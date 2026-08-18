package com.rishabh.leave_management_system.service;

import com.rishabh.leave_management_system.dto.EmployeeRequestDTO;
import com.rishabh.leave_management_system.dto.EmployeeResponseDTO;
import com.rishabh.leave_management_system.dto.EmployeeUpdateRequestDTO;
import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.exception.EmployeeNotFoundException;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public EmployeeResponseDTO saveEmployee(EmployeeRequestDTO employeeRequest){

        Employee employee = new Employee();

        employee.setEmail(employeeRequest.getEmail());
        employee.setName(employeeRequest.getName());
        employee.setPassword(passwordEncoder.encode(employeeRequest.getPassword()));
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        employee = employeeRepository.save(employee);

        EmployeeResponseDTO employeeResponseDTO = new EmployeeResponseDTO();
        employeeResponseDTO.setEmail(employee.getEmail());
        employeeResponseDTO.setName(employee.getName());
        employeeResponseDTO.setId(employee.getId());
        employeeResponseDTO.setRole(employee.getRole());
        employeeResponseDTO.setDepartment(employee.getDepartment());

        return employeeResponseDTO;

    }

    public EmployeeResponseDTO getEmployeeById(String id){

            Employee employee = employeeRepository.findById(id).
                    orElseThrow(
                            () ->
                                    new EmployeeNotFoundException("Employee with : " + id +"not found")
                    );
            EmployeeResponseDTO employeeResponseDTO = new EmployeeResponseDTO();

            employeeResponseDTO.setEmail(employee.getEmail());
            employeeResponseDTO.setName(employee.getName());
            employeeResponseDTO.setId(employee.getId());
            employeeResponseDTO.setRole(employee.getRole());
            employeeResponseDTO.setDepartment(employee.getDepartment());

            return employeeResponseDTO;

    }

    public void deleteByid(String id){
        employeeRepository.deleteById(id);
    }


    public List<EmployeeResponseDTO> getAllEmployee(){

        List<EmployeeResponseDTO> employeeResponseDTOList = new ArrayList<>();

        for(Employee employee : employeeRepository.findAll()){
            EmployeeResponseDTO employeeResponseDTO = new EmployeeResponseDTO();

            employeeResponseDTO.setEmail(employee.getEmail());
            employeeResponseDTO.setName(employee.getName());
            employeeResponseDTO.setId(employee.getId());
            employeeResponseDTO.setRole(employee.getRole());
            employeeResponseDTO.setDepartment(employee.getDepartment());

            employeeResponseDTOList.add(employeeResponseDTO);
        }
        return employeeResponseDTOList;
    }


    public EmployeeResponseDTO updateById(EmployeeUpdateRequestDTO newEmployee, String id){

        Optional<Employee> oldEmployee = employeeRepository.findById(id);


        if(oldEmployee.isPresent()){
            Employee employee = oldEmployee.get();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String currentUser = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().
                    stream().
                    anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if(!isAdmin && !currentUser.equals(employee.getEmail())){
                throw new AccessDeniedException("You are not allowed to perform this action");
            }

            // checking if employee is trying to access fields not allowed to.
            if(!isAdmin && ( newEmployee.getRole() != null || newEmployee.getDepartment() != null)) {
                throw new AccessDeniedException("You are not allowed to perform this action");
            }


            // Both employee and admin can update these
            if(newEmployee.getName() != null && !newEmployee.getName().isEmpty()){
            employee.setName(newEmployee.getName());
            }
            if(newEmployee.getPassword() != null && !newEmployee.getPassword().isEmpty()){
            employee.setPassword(passwordEncoder.encode(newEmployee.getPassword()));
            }

            // Only Admin can update these
            if(isAdmin){
                if(newEmployee.getRole() != null && !newEmployee.getRole().isEmpty()) {
                    employee.setRole(newEmployee.getRole());
                }
                if(newEmployee.getDepartment() != null && !newEmployee.getDepartment().isEmpty()) {
                    employee.setDepartment(newEmployee.getDepartment());
                }
            }

            employeeRepository.save(employee);

            EmployeeResponseDTO employeeResponseDTO = new EmployeeResponseDTO();
            employeeResponseDTO.setId(employee.getId());
            employeeResponseDTO.setName(employee.getName());
            employeeResponseDTO.setEmail(employee.getEmail());
            employeeResponseDTO.setRole(employee.getRole());
            employeeResponseDTO.setDepartment(employee.getDepartment());

            return employeeResponseDTO;
        }
        throw new EmployeeNotFoundException("Given Employee with id: "+id+" not found");
    }


}
