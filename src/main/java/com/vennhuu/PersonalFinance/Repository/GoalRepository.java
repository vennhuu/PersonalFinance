package com.vennhuu.PersonalFinance.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Goal;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser_Id(Long userId);

    Optional<Goal> findByIdAndUser_Id(Long id, Long userId);

    Page<Goal> getAllBudgetsByUserId(Long id, Pageable pageable);
}
