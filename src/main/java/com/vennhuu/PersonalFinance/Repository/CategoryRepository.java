package com.vennhuu.PersonalFinance.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    

    List<Category> getAllCategoriesByUserId(Long userId);

    Optional<Category> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
