package com.vennhuu.PersonalFinance.Enum;

import lombok.Getter;

@Getter
public enum TransactionType {
    INCOME("Tiền vào"), 
    EXPENSE("Tiền ra") ;
    
    private String des ;

    private TransactionType(String des) {
        this.des = des;
    }

}
