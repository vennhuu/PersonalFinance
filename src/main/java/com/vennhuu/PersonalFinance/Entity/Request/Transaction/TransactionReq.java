package com.vennhuu.PersonalFinance.Entity.Request.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vennhuu.PersonalFinance.Enum.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReq {
    
    @NotNull(message = "Vui lòng chọn ví")
    private Long walletId;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Loại giao dịch không được để trống")
    private TransactionType type;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Ngày giao dịch không được để trống")
    @PastOrPresent(message = "Ngày giao dịch không được ở tương lai")
    private LocalDate transactionDate;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
