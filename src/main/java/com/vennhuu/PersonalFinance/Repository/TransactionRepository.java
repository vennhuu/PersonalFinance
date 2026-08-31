package com.vennhuu.PersonalFinance.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> getAllTransactionByUserId( Long id, Pageable pageable ) ;

    Optional<Transaction> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);

    // total money spent
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.category.id = :categoryId
            AND t.user.id = :userId
            AND t.transactionDate BETWEEN :from AND :to
            AND t.type = 'EXPENSE'
    """)
    BigDecimal sumExpenseByCategoryAndDateRange(Long userId, Long categoryId, LocalDate from, LocalDate to);

    List<Transaction> findByUser_IdAndTransactionDateBetween(Long userId, LocalDate from, LocalDate to);
}
