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

    @Value("${app.backend.url}")
    private String backendUrl;

    public void sendEmailVerification(String toEmail, String userName, String verificationToken) {
        try {
            log.info("Tentativo di invio email di verifica a: {}", toEmail);

            // CORREZIONE CRITICA: URL corretto con prefisso /api
            String verificationUrl = backendUrl + "/api/users/verify-email?token=" + verificationToken;
            log.info("URL di verifica generato: {}", verificationUrl);

            try {
                Context context = new Context();
                context.setVariable("userName", userName);
                context.setVariable("verificationUrl", verificationUrl);
                context.setVariable("restaurantName", "Ai Canipai");

                String htmlContent = templateEngine.process("email/verification", context);
                sendHtmlEmail(toEmail, "Conferma la tua registrazione - Ai Canipai", htmlContent);
                log.info("Email HTML inviata con successo a: {}", toEmail);
            } catch (Exception e) {
                log.warn("Fallback a email semplice per template error: {}", e.getMessage());
                sendSimpleVerificationEmail(toEmail, userName, verificationUrl);
            }

            log.info("Email di verifica inviata con successo a: {}", toEmail);
        } catch (Exception e) {
            log.error("ERRORE CRITICO nell'invio email di verifica a {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Errore nell'invio dell'email di verifica", e);
        }
    }

    private void sendSimpleVerificationEmail(String toEmail, String userName, String verificationUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Conferma la tua registrazione - Ai Canipai");
            message.setText(String.format(
                    "Ciao %s,\n\n" +
                            "Grazie per esserti registrato su Ai Canipai!\n\n" +
                            "Per completare la registrazione, clicca sul seguente link:\n" +
                            "%s\n\n" +
                            "Se non hai richiesto questa registrazione, ignora questa email.\n\n" +
                            "Cordiali saluti,\n" +
                            "Il team di Ai Canipai",
                    userName, verificationUrl
            ));

            mailSender.send(message);
            log.info("Email semplice inviata con successo a: {}", toEmail);
        } catch (Exception e) {
            log.error("ERRORE nell'invio email semplice a {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Errore nell'invio dell'email semplice", e);
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email HTML inviata con successo a: {}", toEmail);
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Benvenuto in Ai Canipai!");
            message.setText(String.format(
                    "Ciao %s,\n\n" +
                            "La tua email è stata verificata con successo!\n" +
                            "Benvenuto nella famiglia di Ai Canipai.\n\n" +
                            "Ora puoi accedere al tuo account e prenotare i tuoi tavoli preferiti.\n\n" +
                            "Visita il nostro sito: %s\n\n" +
                            "Buon appetito!\n" +
                            "Il team di Ai Canipai",
                    userName, frontendUrl
            ));

            mailSender.send(message);
            log.info("Email di benvenuto inviata a: {}", toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email di benvenuto a {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset della password - Ai Canipai");
            message.setText(String.format(
                    "Ciao %s,\n\n" +
                            "Hai richiesto il reset della tua password.\n\n" +
                            "Clicca sul seguente link per reimpostare la password:\n" +
                            "%s\n\n" +
                            "Questo link è valido per 24 ore.\n" +
                            "Se non hai richiesto il reset, ignora questa email.\n\n" +
                            "Cordiali saluti,\n" +
                            "Il team di Ai Canipai",
                    userName, resetUrl
            ));

            mailSender.send(message);
            log.info("Email di reset password inviata a: {}", toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email di reset password a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email di reset password", e);
        }
    }

    public void sendGenericEmail(String toEmail, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email generica inviata a: {} con oggetto: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Errore nell'invio email generica a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email generica", e);
        }
    }

    public void sendHtmlEmailWithTemplate(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            variables.forEach(context::setVariable);

            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Email HTML con template {} inviata a: {}", templateName, toEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email HTML con template a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email HTML con template", e);
        }
    }
}