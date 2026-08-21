package com.vennhuu.PersonalFinance.Enum;

import lombok.Getter;

@Getter
public enum TransactionType {
    INCOME("Tiền vào"), 
    OUTCOME("Tiền ra") ;
    
    private String des ;

    private TransactionType(String des) {
        this.des = des;
    }

}
