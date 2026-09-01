package com.vennhuu.PersonalFinance.Entity.Response.Summary;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResCategoryStat {
    private String categoryName;
    private BigDecimal totalAmount;
}