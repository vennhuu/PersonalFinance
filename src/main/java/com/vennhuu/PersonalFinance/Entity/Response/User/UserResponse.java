package com.vennhuu.PersonalFinance.Entity.Response.User;

import java.math.BigDecimal;

import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;
import com.vennhuu.PersonalFinance.Enum.WalletType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    private WalletUser wallet ;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WalletUser {
        private Long id ;
        private String name ;
        private WalletType type;
        private BigDecimal money ;
    }
}
