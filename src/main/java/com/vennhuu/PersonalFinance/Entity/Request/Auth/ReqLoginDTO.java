package com.vennhuu.PersonalFinance.Entity.Request.Auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqLoginDTO {
    
    @NotNull(message="Email khong duoc de trong")
    private String email ;

    @NotNull(message="Mat khau khong duoc de trong")
    private String password ;
}
