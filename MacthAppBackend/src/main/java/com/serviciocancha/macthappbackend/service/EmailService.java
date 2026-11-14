package com.serviciocancha.macthappbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String from;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:no-reply@matchapp.local}") String from
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.from = from;
    }

    public void sendPasswordReset(String to, String link) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("MAIL_HOST no configurado. Enlace de recuperación para {}: {}", to, link);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("No hay JavaMailSender configurado. Enlace de recuperación para {}: {}", to, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Recupera tu contraseña de MatchApp");
        message.setText("""
                Hola,

                Recibimos una solicitud para restablecer tu contraseña de MatchApp.
                Abre este enlace para crear una nueva contraseña (expira en 30 minutos):

                %s

                Si no solicitaste este cambio, ignora este correo.
                """.formatted(link));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("No se pudo enviar el correo de recuperación a {}", to, exception);
        }
    }
}
