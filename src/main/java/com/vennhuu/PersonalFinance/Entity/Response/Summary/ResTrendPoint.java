package com.vennhuu.PersonalFinance.Entity.Response.Summary;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor
public class ResTrendPoint {
    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;
}