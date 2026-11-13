package com.aomaoi.backend.summary;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpSummaryMailerTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;
    @Mock
    private JavaMailSender sender;

    private SummaryEmail email() {
        return new SummaryEmail("Subject line", "plain body", "<p>html body</p>");
    }

    @Test
    void isConfigured_trueWhenHostFromAndSenderPresent() {
        when(provider.getIfAvailable()).thenReturn(sender);

        assertTrue(new SmtpSummaryMailer(provider, "smtp.example.com", "from@example.com").isConfigured());
    }

    @Test
    void isConfigured_falseWhenHostBlank() {
        assertFalse(new SmtpSummaryMailer(provider, " ", "from@example.com").isConfigured());
        verifyNoInteractions(provider);
    }

    @Test
    void isConfigured_falseWhenFromBlank() {
        assertFalse(new SmtpSummaryMailer(provider, "smtp.example.com", "").isConfigured());
        verifyNoInteractions(provider);
    }

    @Test
    void isConfigured_falseWhenNoMailSenderBean() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertFalse(new SmtpSummaryMailer(provider, "smtp.example.com", "from@example.com").isConfigured());
    }

    @Test
    void send_notConfigured_throws() {
        SmtpSummaryMailer mailer = new SmtpSummaryMailer(provider, "", "");
        when(provider.getIfAvailable()).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> mailer.send("to@example.com", email()));
        assertEquals("SMTP is not configured", ex.getMessage());
    }

    @Test
    void send_buildsAndSendsMimeMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(message);

        new SmtpSummaryMailer(provider, "smtp.example.com", "from@example.com")
                .send("to@example.com", email());

        verify(sender).send(message);
        assertEquals("Subject line", message.getSubject());
        assertEquals("from@example.com", message.getFrom()[0].toString());
        assertEquals("to@example.com", message.getAllRecipients()[0].toString());
    }

    @Test
    void send_invalidRecipient_wrapsMessagingException() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(message);

        SmtpSummaryMailer mailer = new SmtpSummaryMailer(provider, "smtp.example.com", "from@example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> mailer.send("not an address @@", email()));
        assertEquals("Could not build the summary e-mail", ex.getMessage());
        verify(sender, never()).send(any(MimeMessage.class));
    }
}
