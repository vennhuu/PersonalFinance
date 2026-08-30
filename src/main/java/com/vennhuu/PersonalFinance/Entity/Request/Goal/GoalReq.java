package com.vennhuu.PersonalFinance.Entity.Request.Goal;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalReq {

    @NotBlank(message = "Tên mục tiêu không được để trống")
    @Size(max = 100, message = "Tên mục tiêu tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Số tiền mục tiêu không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền mục tiêu phải lớn hơn 0")
    private BigDecimal targetAmount;

    @FutureOrPresent(message = "Hạn chót phải từ hôm nay trở về sau")
    private LocalDate deadline; // không bắt buộc (@NotNull không dùng)
}