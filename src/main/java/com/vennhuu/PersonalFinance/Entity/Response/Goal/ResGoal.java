package com.vennhuu.PersonalFinance.Entity.Response.Goal;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResGoal {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount; // targetAmount - currentAmount
    private double percentAchieved;      // currentAmount / targetAmount * 100
    private boolean completed;           // currentAmount >= targetAmount
    private LocalDate deadline;
}
