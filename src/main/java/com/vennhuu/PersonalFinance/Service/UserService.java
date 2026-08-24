package com.vennhuu.PersonalFinance.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vennhuu.PersonalFinance.Entity.Request.User.UpdateUserReq;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Exception.ExistsEmailException;
import com.vennhuu.PersonalFinance.Exception.ExistsPhoneNumberException;
import com.vennhuu.PersonalFinance.Exception.IdInvalidException;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository ;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return this.userRepository.save(user) ;
    }

    public UserResponse convertToUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setFullName(user.getFullName());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhoneNumber(user.getPhoneNumber());
        userResponse.setStatus(user.getStatus());
        if (user.getRole() != null) {
            userResponse.setRole(user.getRole().getName());
        }

        return userResponse;
    }

    public UserResponse convertToUserResponse(User user, Wallet wallet) {
        UserResponse userResponse = convertToUserResponse(user);
        if (userResponse != null && wallet != null) {
            UserResponse.WalletUser walletUser = new UserResponse.WalletUser(
                wallet.getId(),
                wallet.getName(),
                wallet.getType(),
                wallet.getMoney()
            );
            userResponse.setWallet(walletUser);
        }
        return userResponse;
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = this.userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .toList();
    }

    public User fetchUserById(Long id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Id người dùng không tồn tại"));
    }

    public UserResponse findById(Long id) {
        User user = this.fetchUserById(id);
        return this.convertToUserResponse(user);
    }

    public User findByEmail( String email ) {
        return this.userRepository.findByEmail(email) ;
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserReq updateUserReq) {
        User currentUser = this.fetchUserById(id);

        if (updateUserReq.getFullName() != null && !updateUserReq.getFullName().isBlank()) {
            currentUser.setFullName(updateUserReq.getFullName());
        }

        if (updateUserReq.getEmail() != null && !updateUserReq.getEmail().isBlank()) {
            String newEmail = updateUserReq.getEmail();
            if (!newEmail.equals(currentUser.getEmail())) {
                if (this.userRepository.existsByEmail(newEmail)) {
                    throw new ExistsEmailException("Email đã tồn tại");
                }
                currentUser.setEmail(newEmail);
            }
        }

        if (updateUserReq.getPhoneNumber() != null && !updateUserReq.getPhoneNumber().isBlank()) {
            String newPhone = updateUserReq.getPhoneNumber();
            if (!newPhone.equals(currentUser.getPhoneNumber())) {
                if (this.userRepository.existsByPhoneNumber(newPhone)) {
                    throw new ExistsPhoneNumberException("Số điện thoại đã tồn tại");
                }
                currentUser.setPhoneNumber(newPhone);
            }
        }

        User updatedUser = this.userRepository.save(currentUser);
        return this.convertToUserResponse(updatedUser);
    }
}

