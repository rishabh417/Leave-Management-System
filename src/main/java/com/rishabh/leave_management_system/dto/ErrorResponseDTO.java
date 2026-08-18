package com.rishabh.leave_management_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ErrorResponseDTO {

    private int status;
    private String message;
    private Map<String, String> errors;

    public ErrorResponseDTO(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public ErrorResponseDTO(int status, String message, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
    }
}
