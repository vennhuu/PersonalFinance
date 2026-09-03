package com.vennhuu.PersonalFinance.Service.Producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Config.RabbitMQConfig;
import com.vennhuu.PersonalFinance.Entity.Response.RabbitMQ.OtpEmailMessage;

@Service
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOtpEmail(OtpEmailMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.OTP_EXCHANGE,
                RabbitMQConfig.OTP_ROUTING_KEY,
                message
        );
    }
}