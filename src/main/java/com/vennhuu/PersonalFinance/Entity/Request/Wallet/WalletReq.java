package com.vennhuu.PersonalFinance.Entity.Request.Wallet;

import java.math.BigDecimal;

import com.vennhuu.PersonalFinance.Enum.WalletType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletReq {

    @NotBlank(message = "Tên ví không được để trống")
    @Size(max = 100, message = "Tên ví tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Loại ví không được để trống")
    private WalletType type;

    @NotNull(message = "Số dư không được để trống")
    @DecimalMin(value = "0.0", message = "Số dư ban đầu không được âm")
    private BigDecimal money;
}
