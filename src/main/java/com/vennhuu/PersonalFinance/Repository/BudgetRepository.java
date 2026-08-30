package com.vennhuu.PersonalFinance.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Budget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long>{
    List<Budget> findByUser_Id(Long userId);

    Optional<Budget> findByIdAndUser_Id(Long id, Long userId);

    Page<Budget> getAllBudgetsByUserId( Long id, Pageable pageable) ;
}
