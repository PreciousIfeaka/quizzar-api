package com.quizzar.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendOtpEmail(String to, String otp, String purpose) {
        String purposeDisplay = purpose.replace("_", " ").toLowerCase();
        String title = purpose.replace("_", " ");
        String subject = "Quizzar - " + title;

        Context context = new Context();
        context.setVariable("title", title);
        context.setVariable("purpose", purpose);
        context.setVariable("purposeDisplay", purposeDisplay);
        context.setVariable("otp", otp);

        String htmlContent;
        try {
            htmlContent = templateEngine.process("email-otp", context);
        } catch (Exception e) {
            log.error("Failed to process Thymeleaf template. Falling back to inline markup.", e);
            htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <body>
                      <h2>Quizzar</h2>
                      <h3>%s</h3>
                      <p>You requested a code for <strong>%s</strong>. Please use the following OTP: %s</p>
                    </body>
                    </html>
                    """, title, purposeDisplay, otp);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Successfully sent OTP HTML email to {} for purpose {}", to, purpose);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {} due to: {}. Logging OTP code fallback: EMAIL={} OTP=[{}] PURPOSE={}", 
                    to, e.getMessage(), to, otp, purpose);
        }
    }
}
