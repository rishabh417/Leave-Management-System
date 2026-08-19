package com.rishabh.leave_management_system.controller;
import com.rishabh.leave_management_system.dto.LeaveRequestDTO;
import com.rishabh.leave_management_system.dto.LeaveResponseDTO;
import com.rishabh.leave_management_system.entity.Leave;
import com.rishabh.leave_management_system.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public LeaveResponseDTO applyLeave(@Valid @RequestBody LeaveRequestDTO leaveRequest){
        return leaveService.applyLeave(leaveRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<LeaveResponseDTO> getAllLeave(){
        return leaveService.getAllLeave();
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveResponseDTO> getLeaveByEmployeeId(@PathVariable String employeeId){

        return leaveService.getLeaveByEmployeeId(employeeId);

    }

    @GetMapping("/id/{id}")
    public LeaveResponseDTO getLeaveById(@PathVariable String id){
        return leaveService.getLeaveById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("approve/{leaveId}")
    public LeaveResponseDTO approveLeave(@PathVariable String leaveId){
        return leaveService.approveLeave(leaveId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("reject/{leaveId}")
    public LeaveResponseDTO rejectLeave(@PathVariable String leaveId){
        return leaveService.rejectLeave(leaveId);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("delete/{leaveId}")
    public void deleteLeave(@PathVariable String leaveId){
         leaveService.deleteLeave(leaveId);
    }

}
