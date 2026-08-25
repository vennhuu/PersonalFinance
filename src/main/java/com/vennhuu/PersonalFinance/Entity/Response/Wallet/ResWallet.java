package com.vennhuu.PersonalFinance.Entity.Response.Wallet;

import java.math.BigDecimal;

import com.vennhuu.PersonalFinance.Enum.WalletType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResWallet {

    private Long id;
    private String name;
    private WalletType type;
    private BigDecimal money;
}
