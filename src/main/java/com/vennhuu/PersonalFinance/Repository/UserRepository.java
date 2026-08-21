package com.vennhuu.PersonalFinance.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String username);
    
}
