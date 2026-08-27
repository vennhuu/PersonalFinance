package com.vennhuu.PersonalFinance.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    Page<Wallet> getAllWalletByUserId( Long id, Pageable pageable ) ;

    Optional<Wallet> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
