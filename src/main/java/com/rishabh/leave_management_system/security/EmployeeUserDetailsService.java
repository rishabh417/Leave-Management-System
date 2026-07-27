package com.rishabh.leave_management_system.security;

import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Employee employee = employeeRepository.findByEmail(username);
        if(employee == null){
            throw new UsernameNotFoundException("User with email: " + username + " not found");
        }

        return new EmployeeUserDetails(employee);
    }

}
