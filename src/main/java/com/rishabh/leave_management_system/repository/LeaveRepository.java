package com.rishabh.leave_management_system.repository;

import com.rishabh.leave_management_system.entity.Leave;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.rishabh.leave_management_system.entity.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.List;

import java.util.List;

public interface LeaveRepository extends MongoRepository<Leave, String> {

    List<Leave> findByEmployeeId(String employeeId);

    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String employeeId,
            List<LeaveStatus> statuses,
            LocalDate endDate,
            LocalDate startDate
    );
}
