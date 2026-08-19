package com.rishabh.leave_management_system.exception;

public class InvalidLeaveDateException extends RuntimeException {

    public InvalidLeaveDateException(String message) {
        super(message);
    }
}
