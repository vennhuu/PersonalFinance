package com.vennhuu.PersonalFinance.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;
import com.vennhuu.PersonalFinance.Exception.ExistsEmailException;
import com.vennhuu.PersonalFinance.Exception.ExistsPhoneNumberException;
import com.vennhuu.PersonalFinance.Repository.RoleRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

@Service
public class AuthService {
    
    private final UserService userService ;
    private final PasswordEncoder passwordEncoder ;
    private final RoleRepository roleRepository ;
    private final UserRepository userRepository ;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, RoleRepository roleRepository, UserRepository userRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder ;
        this.roleRepository = roleRepository ;
        this.userRepository = userRepository ;
    }


    // check exist email
    public boolean existsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    // check exist phoneNumber
    public boolean existsByPhoneNumber(String phone) {
        return this.userRepository.existsByPhoneNumber(phone);
    }

    // register new user
    public UserResponse registerNewUser(User user) {

        if (this.existsByEmail(user.getEmail())) {
            throw new ExistsEmailException("Email đã tồn tại");
        }

        if (this.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new ExistsPhoneNumberException("Số điện thoại đã tồn tại");
        }

        String hashPassword = this.passwordEncoder.encode(user.getPassword()) ;
        user.setPassword(hashPassword);
        user.setStatus(UserStatus.ACTIVE);

        Role r = this.roleRepository.findByName(RoleName.ROLE_USER) ;
        user.setRole(r);
        this.userService.save(user) ;

        Wallet wallet = new Wallet() ;
        return this.userService.convertToUserResponse(user, wallet) ;
    }
}
