package com.vennhuu.PersonalFinance.Entity.Response.Category;

import com.vennhuu.PersonalFinance.Enum.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCategory {

    private Long id;
    private String name;
    private TransactionType type;
}
