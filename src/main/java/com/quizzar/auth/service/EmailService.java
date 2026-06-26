package com.quizzar.auth.service;

import com.quizzar.auth.dto.FeedbackRequest;
import com.quizzar.auth.util.SecurityUtils;
import com.quizzar.storage.service.S3StorageService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    @Value("${quizzar.mail.from}")
    private String mailFrom;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final S3StorageService s3StorageService;

    @Async
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
            helper.setFrom(mailFrom);
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

    @Async
    public void sendFeedbackEmail(FeedbackRequest request, String user) {
        Context context = new Context();
        context.setVariable("text", request.getText());
        context.setVariable("user", user != null ? user : "Anon user");
        context.setVariable("user", "Anon user");

        if (request.getImageUrl() != null) {
            context.setVariable("imageUrl", s3StorageService.generatePresignedUrl(request.getImageUrl()));
        }

        try {
            String htmlContent = templateEngine.process("feedback-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo("preciousifeaka@gmail.com");
            helper.setSubject("Quizzar - Feedback");
            helper.setText(htmlContent);

            mailSender.send(message);
            log.info("Successfully sent app feedback message");
        } catch (Exception e) {
            log.error("Failed to send app feedback message, {}", e.getMessage());
        }

    }
}
