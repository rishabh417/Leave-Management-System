package com.rishabh.leave_management_system.controller;

import com.rishabh.leave_management_system.entity.Leave;
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
        return leaveService.applyLeave(leave);
    }

    @GetMapping
    public List<Leave> getAllLeave(){
        return leaveService.getAllLeave();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Leave> getLeaveByEmployeeId(@PathVariable String employeeId){
        return leaveService.getLeaveByEmployeeId(employeeId);
    }

    @GetMapping("/id/{id}")
    public Leave getLeaveById(@PathVariable String id){
        return leaveService.getLeaveById(id);
    }

    @PutMapping("approve/{leaveId}")
    public Leave approveLeave(@PathVariable String leaveId){
        return leaveService.approveLeave(leaveId);
    }

    @PutMapping("reject/{leaveId}")
    public Leave rejectLeave(@PathVariable String leaveId){
        return leaveService.rejectLeave(leaveId);
    }


}
