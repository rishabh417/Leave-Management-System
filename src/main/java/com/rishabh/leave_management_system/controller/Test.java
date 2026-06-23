package com.rishabh.leave_management_system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {

    @Value("${spring.data.mongodb.uri:not-found}")
    private String uri;

    @GetMapping("/test")
    public String test() {
        return "Mongo Connected";
    }
}
