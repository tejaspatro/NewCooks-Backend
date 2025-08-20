package com.NewCooks.NewCooks.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    public void sendActivationEmail(String toEmail, String activationLink)
    {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(toEmail);
        msg.setSubject("Activate your NewCooks account");
        msg.setText("Welcome to NewCooks!\n\nClick the link to activate:\n" + activationLink +
                "\n\nIf you didn't sign up, ignore this email.");
        mailSender.send(msg);
    }
}
