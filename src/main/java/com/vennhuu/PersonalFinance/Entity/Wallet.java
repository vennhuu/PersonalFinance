package com.vennhuu.PersonalFinance.Entity;

import java.math.BigDecimal;

import com.vennhuu.PersonalFinance.Enum.WalletType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wallets")
@Getter
@Setter
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name; // tên user tự đặt, vd "Vietcombank", "Tiền mặt"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal money  = BigDecimal.ZERO;
}