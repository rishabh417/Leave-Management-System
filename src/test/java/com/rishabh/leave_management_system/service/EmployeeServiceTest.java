package com.rishabh.leave_management_system.service;


import com.rishabh.leave_management_system.dto.EmployeeRequestDTO;
import com.rishabh.leave_management_system.dto.EmployeeResponseDTO;
import com.rishabh.leave_management_system.dto.EmployeeUpdateRequestDTO;
import com.rishabh.leave_management_system.entity.Employee;
import com.rishabh.leave_management_system.exception.EmployeeNotFoundException;
import com.rishabh.leave_management_system.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;


    @Test
    void saveEmployee_shouldSaveEmployee() {

        EmployeeRequestDTO employeeRequestDTO = new EmployeeRequestDTO();

        employeeRequestDTO.setName("Test Employee");
        employeeRequestDTO.setEmail("test@gmail.com");
        employeeRequestDTO.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        Employee employee = new Employee();

        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setPassword("encoded-password");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDTO employeeResponseDTO = employeeService.saveEmployee(employeeRequestDTO);

        assertEquals("employee-123", employeeResponseDTO.getId());
        assertEquals("Test Employee", employeeResponseDTO.getName());
        assertEquals("test@gmail.com", employeeResponseDTO.getEmail());
        assertEquals("EMPLOYEE", employeeResponseDTO.getRole());
        assertEquals("UNASSIGNED", employeeResponseDTO.getDepartment());

        verify(passwordEncoder).encode("password");
        verify(employeeRepository).save(any(Employee.class));

    }

    @Test
    void getEmployeeById_shouldReturnEmployee_whenEmployeeExists(){

        Employee employee = new Employee();

        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setPassword("encoded-password");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));

        EmployeeResponseDTO response = employeeService.getEmployeeById("employee-123");
        assertEquals("employee-123", response.getId());
        assertEquals("Test Employee", response.getName());
        assertEquals("test@gmail.com", response.getEmail());
        assertEquals("UNASSIGNED", response.getDepartment());
        assertEquals("EMPLOYEE", response.getRole());

        verify(employeeRepository).findById("employee-123");

    }

    @Test
    void getEmployeeById_shouldThrowException_whenEmployeeDoesNotExist(){

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.getEmployeeById("employee-123")
        );

        verify(employeeRepository).findById("employee-123");

    }

    @Test
    void deleteById_shouldDeleteEmployee(){

        employeeService.deleteByid("employee-123");

        verify(employeeRepository).deleteById("employee-123");
    }

    @Test
    void getAllEmployees_shouldReturnAllEmployees(){

        Employee employee1 = new Employee();
        employee1.setId("employee-123");
        employee1.setName("Test Employee");
        employee1.setEmail("test@gmail.com");
        employee1.setRole("EMPLOYEE");
        employee1.setDepartment("UNASSIGNED");

        Employee employee2 = new Employee();
        employee2.setId("employee-456");
        employee2.setName("Test Employee2");
        employee2.setEmail("test2@gmail.com");
        employee2.setRole("EMPLOYEE");
        employee2.setDepartment("UNASSIGNED");

        when(employeeRepository.findAll())
                .thenReturn(Arrays.asList(employee1,employee2));

        List<EmployeeResponseDTO> response = employeeService.getAllEmployee();

        assertEquals(2, response.size());
        assertEquals("employee-123", response.get(0).getId());
        assertEquals("Test Employee", response.get(0).getName());
        assertEquals("test@gmail.com", response.get(0).getEmail());
        assertEquals("UNASSIGNED", response.get(0).getDepartment());
        assertEquals("EMPLOYEE", response.get(0).getRole());

        assertEquals("employee-456", response.get(1).getId());
        assertEquals("Test Employee2", response.get(1).getName());
        assertEquals("test2@gmail.com", response.get(1).getEmail());
        assertEquals("UNASSIGNED", response.get(1).getDepartment());
        assertEquals("EMPLOYEE", response.get(1).getRole());

        verify(employeeRepository).findAll();

    }

    @Test
    void updateById_shouldUpdateEmployee_whenUpdatedByEmployee(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setName("Test Employee");
        employee2.setPassword("password");

        EmployeeResponseDTO response = employeeService.updateById(employee2,"employee-123");

        verify(passwordEncoder).encode("password");
        verify(employeeRepository).save(employee);

    }

    @Test
    void updateById_shouldUpdateEmployee_whenUpdatedByAdmin(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setName("Test Employee");
        employee2.setPassword("password");
        employee2.setRole("ROLE_ADMIN");
        employee2.setDepartment("IT");

        EmployeeResponseDTO response = employeeService.updateById(employee2,"employee-123");

        assertEquals("ROLE_ADMIN", response.getRole());
        assertEquals("IT", response.getDepartment());

        verify(passwordEncoder).encode("password");
        verify(employeeRepository).save(employee);

    }

    @Test
    void updateById_shouldThrowException_whenEmployeeUpdateOtherEmployee(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("other@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setName("Test Employee2");
        employee2.setPassword("password2");

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.updateById(employee2,"employee-123")
        );

        verify(employeeRepository,never()).save(any());

    }

    @Test
    void updateById_shouldThrowException_whenEmployeeUpdateRoleOrDepartment(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_EMPLOYEE")
        )).when(authentication).getAuthorities();

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setRole("ROLE_ADMIN");
        employee2.setDepartment("IT");

        assertThrows(
                AccessDeniedException.class,
                () -> employeeService.updateById(employee2,"employee-123")
        );

        verify(employeeRepository,never()).save(any());

    }

    @Test
    void updateById_shouldThrowException_whenEmployeeDoesNotExist(){

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.empty());

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setRole("ROLE_ADMIN");
        employee2.setDepartment("IT");

        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.updateById(employee2,"employee-123")
        );

        verify(employeeRepository,never()).save(any());

    }

    @Test
    void updateById_shouldUpdateEmployeeRoleOrDepartmeant_whenAdminUpdateRoleOrDepartment(){

        SecurityContextHolder.getContext().setAuthentication(authentication);

        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )).when(authentication).getAuthorities();

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setRole("ROLE_ADMIN");
        employee2.setDepartment("IT");

        EmployeeResponseDTO response = employeeService.updateById(employee2,"employee-123");
        assertEquals("ROLE_ADMIN", response.getRole());
        assertEquals("IT", response.getDepartment());


        verify(employeeRepository).save(employee);

    }

    @Test
    void updateById_shouldUpdateEmployeenNameOrPasswordPartially_whenUpdatedByEmployee(){

        when(authentication.getName()).thenReturn("test@gmail.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee employee = new Employee();
        employee.setId("employee-123");
        employee.setName("Test Employee");
        employee.setEmail("test@gmail.com");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("UNASSIGNED");

        when(employeeRepository.findById("employee-123")).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("password2")).thenReturn("encoded-password");

        EmployeeUpdateRequestDTO employee2 = new EmployeeUpdateRequestDTO();
        employee2.setName("Test Employee2");
        employee2.setPassword("password2");

        EmployeeResponseDTO response = employeeService.updateById(employee2,"employee-123");

        assertEquals("Test Employee2", response.getName());
        assertEquals("EMPLOYEE", response.getRole());
        assertEquals("UNASSIGNED", response.getDepartment());
        assertEquals("encoded-password", employee.getPassword());

        verify(passwordEncoder).encode("password2");
        verify(employeeRepository).save(employee);

    }




}
