package com.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    public void sendOrderConfirmation(String toEmail, String orderNumber, BigDecimal amount) {
        String subject = "Order confirmed — " + orderNumber;
        String body = buildConfirmationHtml(orderNumber, amount);
        sendEmail(toEmail, subject, body);
    }

    public void sendPaymentFailedNotification(String orderNumber, String reason) {
        // In a real system you'd store the customer email on the order.
        // For now we log — the saga still rolls back correctly.
        log.warn("Payment failed for order {} — reason: {}. Email not sent (no customer email on PaymentFailed event).",
                orderNumber, reason);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent — to={}, subject={}", to, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Log and continue — email failure should not block the saga
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildConfirmationHtml(String orderNumber, BigDecimal amount) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; color: #333; padding: 32px;">
              <h2 style="color: #1D9E75;">Your order is confirmed!</h2>
              <p>Thank you for your purchase. Here are your order details:</p>
              <table style="border-collapse: collapse; width: 100%%; max-width: 480px;">
                <tr>
                  <td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: 500;">Order number</td>
                  <td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td>
                </tr>
                <tr>
                  <td style="padding: 8px; font-weight: 500;">Amount charged</td>
                  <td style="padding: 8px;">$%s</td>
                </tr>
              </table>
              <p style="margin-top: 24px; color: #666; font-size: 13px;">
                If you have questions, reply to this email.
              </p>
            </body>
            </html>
            """.formatted(orderNumber, amount.toPlainString());
    }
}