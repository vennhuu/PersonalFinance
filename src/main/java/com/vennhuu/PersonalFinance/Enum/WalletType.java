package com.vennhuu.PersonalFinance.Enum;

import lombok.Getter;

@Getter
public enum WalletType {
    CASH("Tiền mặt"), 
    BANK("Tiền ngân hàng"), 
    E_WALLET("Tiền ví điện tử"), 
    CREDIT_CARD("Thẻ tín dụng") ;
    
    private String description ;

    private WalletType(String description) {
        this.description = description;
    }

}
