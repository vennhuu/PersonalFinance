package com.vennhuu.PersonalFinance.Entity.Response.User;

import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id ;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserStatus status = UserStatus.ACTIVE;
    private RoleName role;
}
