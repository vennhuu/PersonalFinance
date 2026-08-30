package com.vennhuu.PersonalFinance.Entity.Request.Goal;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributeReq {

    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền nạp phải lớn hơn 0")
    private BigDecimal amount;
}