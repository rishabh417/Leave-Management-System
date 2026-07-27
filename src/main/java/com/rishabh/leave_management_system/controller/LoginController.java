package com.rishabh.leave_management_system.controller;

import com.rishabh.leave_management_system.dto.LoginRequest;
import com.rishabh.leave_management_system.dto.LoginResponse;
import com.rishabh.leave_management_system.security.EmployeeUserDetailsService;
import com.rishabh.leave_management_system.security.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {


    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;


    public LoginController(AuthenticationManager authenticationManager, JWTService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }


    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest){
        // Implement your login logic here

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token =  jwtService.generateToken(userDetails);

        return new LoginResponse(token);
    }



}
