package com.aomaoi.backend.summary;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class SmtpSummaryMailer implements SummaryMailer {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;

    public SmtpSummaryMailer(ObjectProvider<JavaMailSender> mailSender,
                             @Value("${spring.mail.host:}") String host,
                             @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    @Override
    public boolean isConfigured() {
        return !host.isBlank() && !from.isBlank() && mailSender.getIfAvailable() != null;
    }

    @Override
    public void send(String to, SummaryEmail email) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!isConfigured() || sender == null) {
            throw new IllegalStateException("SMTP is not configured");
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(email.subject());
            helper.setText(email.textBody(), email.htmlBody());
            sender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build the summary e-mail", e);
        }
    }
}
