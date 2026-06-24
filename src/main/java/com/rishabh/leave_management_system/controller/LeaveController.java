package com.rishabh.leave_management_system.controller;

import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.repository.LeaveRepository;
import com.rishabh.leave_management_system.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public Leave applyLeave(@RequestBody Leave leave){
        leaveService.applyLeave(leave);
        return leave;
    }

    @GetMapping
    public List<Leave> getAllLeave(){
        return leaveService.getAllLeave();
    }

    @GetMapping("/{id}")
    public List<Leave> getLeaveByEmployeeId(@PathVariable String id){
        return leaveService.getLeaveByEmployeeId(id);
    }

}
