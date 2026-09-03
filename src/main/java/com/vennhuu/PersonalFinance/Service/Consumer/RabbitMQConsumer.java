package com.vennhuu.PersonalFinance.Service.Consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Config.RabbitMQConfig;
import com.vennhuu.PersonalFinance.Entity.Response.RabbitMQ.OtpEmailMessage;
import com.vennhuu.PersonalFinance.Service.EmailService;

@Service
public class RabbitMQConsumer {

    private final EmailService emailService;

    public RabbitMQConsumer( EmailService emailService ) {
        this.emailService = emailService;
    }

    @RabbitListener( queues = RabbitMQConfig.OTP_QUEUE )
    public void receiveAssignTaskEmail( OtpEmailMessage message ) {
        emailService.sendOtpEmail(
                message.getEmail(),
                message.getFullName(),
                message.getOtpCode()
        );
    }
}