package com.vennhuu.PersonalFinance.Entity.Response.Budget;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResBudget {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amountLimit;
    private BigDecimal spentAmount;    // đã chi bao nhiêu (tính động từ transaction)
    private BigDecimal remainingAmount; // còn lại
    private double percentUsed;         // % đã dùng
    private LocalDate startDate;
    private LocalDate endDate;
}