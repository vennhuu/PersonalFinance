package com.vennhuu.PersonalFinance.Service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Value("${spring.mail.username}")
    private String emailFrom;

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender javaMailSender, SpringTemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    public void sendOtpEmail(String to, String fullName, String otpCode) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setSubject("Mã xác thực OTP đặt lại mật khẩu");

            Context context = new Context();
            context.setVariable("name", fullName != null && !fullName.isBlank() ? fullName : to);
            context.setVariable("otpCode", otpCode);

            String html = templateEngine.process("otp-email", context);
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không gửi được email mã OTP: " + e.getMessage());
        }
    }
}
