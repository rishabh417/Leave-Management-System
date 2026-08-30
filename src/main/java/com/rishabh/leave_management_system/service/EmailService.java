package com.rishabh.leave_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendLeaveAppliedEmail(String to, String leaveType)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Leave Applied");
        message.setText(
                "Your " + leaveType + " leave application has been submitted successfully.\n\n" +
                        "Status: PENDING\n\n" +
                        "You will receive another email once your leave is approved or rejected."
        );
        mailSender.send(message);
    }

    public void sendLeaveApprovedEmail(String to, String leaveType){

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Leave Approved");
        message.setText(
                "Your " + leaveType + " leave application has been approved successfully.\n\n" +
                        "Status: APPROVED\n\n"
        );
        mailSender.send(message);
    }

    public void sendLeaveRejectedEmail(String to, String leaveType){

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Leave Rejected");
        message.setText(
                "Your " + leaveType + " leave application has been rejected.\n\n" +
                        "Status: REJECTED\n\n"
        );
        mailSender.send(message);
    }

}
