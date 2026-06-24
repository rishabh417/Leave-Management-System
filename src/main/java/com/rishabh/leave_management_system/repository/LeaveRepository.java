package com.rishabh.leave_management_system.repository;

import com.rishabh.leave_management_system.entity.Leave;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LeaveRepository extends MongoRepository<Leave, String> {

    List<Leave> findByEmployeeId(String employeeId);
}
