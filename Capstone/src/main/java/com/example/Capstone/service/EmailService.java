package com.example.Capstone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendEmailVerification(String toEmail, String userName, String verificationToken) {
        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("restaurantName", "Ai Canipai");

            String htmlContent = templateEngine.process("email/verification", context);

            sendHtmlEmail(toEmail, "Conferma la tua registrazione - Ai Canipai", htmlContent);

            log.info("Email di verifica inviata a: {}", toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email di verifica a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email di verifica", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("restaurantName", "Ai Canipai");
            context.setVariable("frontendUrl", frontendUrl);

            String htmlContent = templateEngine.process("email/welcome", context);

            sendHtmlEmail(toEmail, "Benvenuto in Ai Canipai!", htmlContent);

            log.info("Email di benvenuto inviata a: {}", toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email di benvenuto a {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendReservationConfirmation(String toEmail, String userName, Map<String, Object> reservationDetails) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("reservationDetails", reservationDetails);
            context.setVariable("restaurantName", "Ai Canipai");

            String htmlContent = templateEngine.process("email/reservation-confirmation", context);

            sendHtmlEmail(toEmail, "Conferma Prenotazione - Ai Canipai", htmlContent);

            log.info("Email di conferma prenotazione inviata a: {}", toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email conferma prenotazione a {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(fromEmail, "Ai Canipai");
        } catch (Exception e) {
            // Fallback to simple setFrom if display name fails
            helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email semplice inviata a: {}", to);
        } catch (Exception e) {
            log.error("Errore nell'invio email semplice a {}: {}", to, e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email", e);
        }
    }
}