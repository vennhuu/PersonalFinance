package com.vennhuu.PersonalFinance.Config;

import java.util.Collections;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.vennhuu.PersonalFinance.Repository.UserRepository;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService{
    
    private final UserRepository userRepository ;

    public UserDetailsCustom(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws BadCredentialsException {
        
        com.vennhuu.PersonalFinance.Entity.User user = this.userRepository.findByEmail(username) ;
        if ( user == null ) {
            throw new BadCredentialsException("Email/Password không đúng") ;
        }
        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName().name()
                : "ROLE_USER";
        return new User(user.getEmail(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority(roleName)));
    }

    
}
