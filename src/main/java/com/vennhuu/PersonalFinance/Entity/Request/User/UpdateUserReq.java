package com.vennhuu.PersonalFinance.Entity.Request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserReq {
    
    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
    )
    private String phoneNumber;
    private String fullName ;
}
