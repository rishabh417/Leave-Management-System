package com.rishabh.leave_management_system.repository;

import com.rishabh.leave_management_system.entity.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

    Employee findByEmail(String email);

}
