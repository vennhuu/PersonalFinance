package com.vennhuu.PersonalFinance.Entity.Response.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vennhuu.PersonalFinance.Enum.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResTransaction {
    private Long id;
    private Long walletId;
    private String walletName;
    private Long categoryId;
    private String categoryName;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String note;
}