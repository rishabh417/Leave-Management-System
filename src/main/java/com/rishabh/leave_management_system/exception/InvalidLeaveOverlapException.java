package com.rishabh.leave_management_system.exception;

public class InvalidLeaveOverlapException extends RuntimeException {

    public InvalidLeaveOverlapException(String message) {
        super(message);
    }
}
