package com.serviciocancha.macthappbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    @Async
    public void sendPasswordReset(String to, String link) {
        send(
                to,
                "Recupera tu contraseña de MatchApp",
                """
                Recibimos una solicitud para restablecer tu contraseña de MatchApp.
                Abre este enlace para crear una nueva contraseña (expira en 30 minutos):
                """,
                link,
                "Si no solicitaste este cambio, ignora este correo.",
                "recuperación"
        );
    }

    @Async
    public void sendEmailVerification(String to, String link) {
        send(
                to,
                "Verifica tu correo de MatchApp",
                """
                Gracias por registrarte en MatchApp.
                Abre este enlace para verificar tu correo (expira en 24 horas):
                """,
                link,
                "Si no creaste esta cuenta, ignora este correo.",
                "verificación"
        );
    }

    private void send(String to, String subject, String intro, String link, String footer, String kind) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("MAIL_HOST no configurado. Enlace de {} para {}: {}", kind, to, link);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("No hay JavaMailSender configurado. Enlace de {} para {}: {}", kind, to, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText("""
                Hola,

                %s

                %s

                %s
                """.formatted(intro.strip(), link, footer));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error(
                    "No se pudo enviar el correo de {} a {}. Enlace: {}",
                    kind,
                    to,
                    link,
                    exception
            );
        }
    }
}
