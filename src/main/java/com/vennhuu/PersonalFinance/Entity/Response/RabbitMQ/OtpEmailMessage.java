package com.vennhuu.PersonalFinance.Entity.Response.RabbitMQ;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpEmailMessage {
    private String email;
    private String fullName;
    private String otpCode;
}