package com.vennhuu.PersonalFinance.Config;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.vennhuu.PersonalFinance.Repository.UserRepository;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService{
    
    private final UserRepository userRepository ;

    public UserDetailsCustom(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        com.vennhuu.PersonalFinance.Entity.User user = this.userRepository.findByEmail(username) ;
        if ( user == null ) {
            throw new UsernameNotFoundException("Email/Password không đúng") ;
        }
        return new User(user.getEmail(), user.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    
}
