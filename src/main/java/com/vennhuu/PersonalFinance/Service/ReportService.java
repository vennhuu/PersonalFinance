package com.vennhuu.PersonalFinance.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResCategoryStat;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResSummary;
import com.vennhuu.PersonalFinance.Entity.Response.Summary.ResTrendPoint;
import com.vennhuu.PersonalFinance.Entity.Transaction;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public ReportService(TransactionRepository transactionRepository, SecurityUtil securityUtil, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.securityUtil = securityUtil;
        this.userService = userService;
    }

    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return userService.findByEmail(email);
    }

    private List<Transaction> getTransactionsInRange(LocalDate from, LocalDate to) {
        User user = getCurrentUser();
        return transactionRepository.findByUser_IdAndTransactionDateBetween(user.getId(), from, to);
    }

    //Tổng quan thu/chi
    public ResSummary getSummary(LocalDate from, LocalDate to) {
        List<Transaction> transactions = getTransactionsInRange(from, to);

        BigDecimal income = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expense = sumByType(transactions, TransactionType.EXPENSE);

        return new ResSummary(income, expense, income.subtract(expense));
    }

    //Thống kê theo category
    public List<ResCategoryStat> getStatsByCategory(LocalDate from, LocalDate to, TransactionType type) {
        List<Transaction> transactions = getTransactionsInRange(from, to);

        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .collect(Collectors.groupingBy(
                        tx -> tx.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(e -> new ResCategoryStat(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount())) // sắp xếp giảm dần
                .toList();
    }

    // Xu hướng theo ngày
    public List<ResTrendPoint> getTrend(LocalDate from, LocalDate to) {
        List<Transaction> transactions = getTransactionsInRange(from, to);

        Map<LocalDate, List<Transaction>> byDate = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        return byDate.entrySet().stream()
                .map(e -> new ResTrendPoint(
                        e.getKey(),
                        sumByType(e.getValue(), TransactionType.INCOME),
                        sumByType(e.getValue(), TransactionType.EXPENSE)
                ))
                .sorted(Comparator.comparing(ResTrendPoint::getDate))
                .toList();
    }

    // Helper dùng chung: cộng tổng amount theo type
    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}