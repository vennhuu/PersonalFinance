package com.vennhuu.PersonalFinance.Entity.Request.Category;

import com.vennhuu.PersonalFinance.Enum.TransactionType;

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
public class CategoryReq {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Loại danh mục không được để trống")
    private TransactionType type; // INCOME | EXPENSE
}