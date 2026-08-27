package com.vennhuu.PersonalFinance.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    

    Page<Category> getAllCategoriesByUserId(Long userId, Pageable pageable);

    Optional<Category> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
