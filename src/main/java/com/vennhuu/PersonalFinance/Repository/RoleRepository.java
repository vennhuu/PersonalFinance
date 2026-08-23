package com.vennhuu.PersonalFinance.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Enum.RoleName;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(RoleName name);
    
}
