package com.coachapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.domain}")
    private String appDomain;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendInvitation(String toEmail, String tenantSubdomain, String rawToken) {
        String link = "https://" + tenantSubdomain + "." + appDomain + "/join?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("You've been invited to join your coach's workspace");
        message.setText(
                "Your coach has invited you to their coaching workspace.\n\n"
                + "Click the link below to complete your registration:\n\n"
                + link + "\n\n"
                + "This link expires in 7 days.");
        mailSender.send(message);
    }
}
