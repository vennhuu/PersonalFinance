package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vennhuu.PersonalFinance.Entity.Request.User.UpdateUserReq;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;
import com.vennhuu.PersonalFinance.Enum.WalletType;
import com.vennhuu.PersonalFinance.Exception.ExistsEmailException;
import com.vennhuu.PersonalFinance.Exception.ExistsPhoneNumberException;
import com.vennhuu.PersonalFinance.Exception.IdInvalidException;
import com.vennhuu.PersonalFinance.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private Role sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new Role();
        sampleRole.setName(RoleName.ROLE_USER);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setEmail("a@example.com");
        sampleUser.setPhoneNumber("0912345678");
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setRole(sampleRole);
    }

    // convertToUserResponse(User)
    @Nested
    @DisplayName("convertToUserResponse(User)")
    class ConvertToUserResponse {

        @Test
        @DisplayName("Return null when user is null")
        void shouldReturnNullWhenUserIsNull() {
            UserResponse result = userService.convertToUserResponse((User) null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Map all fields including role")
        void shouldMapAllFieldsWithRole() {
            UserResponse result = userService.convertToUserResponse(sampleUser);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFullName()).isEqualTo("Nguyen Van A");
            assertThat(result.getEmail()).isEqualTo("a@example.com");
            assertThat(result.getPhoneNumber()).isEqualTo("0912345678");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.getRole()).isEqualTo(RoleName.ROLE_USER);
        }

        @Test
        @DisplayName("Null role is handled gracefully")
        void shouldHandleNullRole() {
            sampleUser.setRole(null);
            UserResponse result = userService.convertToUserResponse(sampleUser);

            assertThat(result).isNotNull();
            assertThat(result.getRole()).isNull();
        }
    }

    // convertToUserResponse(User, Wallet)
    @Nested
    @DisplayName("convertToUserResponse(User, Wallet)")
    class ConvertToUserResponseWithWallet {

        @Test
        @DisplayName("Attach wallet data to response")
        void shouldAttachWalletData() {
            Wallet wallet = new Wallet();
            wallet.setId(10L);
            wallet.setName("Tien mat");
            wallet.setType(WalletType.CASH);
            wallet.setMoney(BigDecimal.valueOf(500_000));

            UserResponse result = userService.convertToUserResponse(sampleUser, wallet);

            assertThat(result).isNotNull();
            assertThat(result.getWallet()).isNotNull();
            assertThat(result.getWallet().getId()).isEqualTo(10L);
            assertThat(result.getWallet().getName()).isEqualTo("Tien mat");
            assertThat(result.getWallet().getType()).isEqualTo(WalletType.CASH);
            assertThat(result.getWallet().getMoney()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
        }

        @Test
        @DisplayName("No wallet set when wallet is null")
        void shouldNotSetWalletWhenNull() {
            UserResponse result = userService.convertToUserResponse(sampleUser, null);

            assertThat(result).isNotNull();
            assertThat(result.getWallet()).isNull();
        }
    }

    // getAllUsers()
    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Return empty list when no users exist")
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Return mapped list of users")
        void shouldReturnMappedUsers() {
            User user2 = new User();
            user2.setId(2L);
            user2.setFullName("Tran Thi B");
            user2.setEmail("b@example.com");
            user2.setStatus(UserStatus.ACTIVE);

            when(userRepository.findAll()).thenReturn(List.of(sampleUser, user2));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(1).getId()).isEqualTo(2L);
        }
    }

    // fetchUserById(Long)
    @Nested
    @DisplayName("fetchUserById(Long)")
    class FetchUserById {

        @Test
        @DisplayName("Return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            User result = userService.fetchUserById(1L);

            assertThat(result).isEqualTo(sampleUser);
        }

        @Test
        @DisplayName("Throw IdInvalidException when not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.fetchUserById(99L))
                    .isInstanceOf(IdInvalidException.class)
                    .hasMessageContaining("không tồn tại");
        }
    }

    // findById(Long) → returns UserResponse
    @Nested
    @DisplayName("findById(Long)")
    class FindById {

        @Test
        @DisplayName("Return UserResponse when user exists")
        void shouldReturnUserResponse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            UserResponse result = userService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("a@example.com");
        }
    }

    // findByEmail(String)
    @Nested
    @DisplayName("findByEmail(String)")
    class FindByEmail {

        @Test
        @DisplayName("Delegate to repository and return result")
        void shouldDelegateToRepository() {
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);

            User result = userService.findByEmail("a@example.com");

            assertThat(result).isEqualTo(sampleUser);
        }

        @Test
        @DisplayName("Return null when email not found")
        void shouldReturnNullWhenNotFound() {
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(null);

            User result = userService.findByEmail("notfound@example.com");

            assertThat(result).isNull();
        }
    }

    // save(User)
    @Nested
    @DisplayName("save(User)")
    class SaveUser {

        @Test
        @DisplayName("Delegate to repository and return saved user")
        void shouldSaveUser() {
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);

            User result = userService.save(sampleUser);

            assertThat(result).isEqualTo(sampleUser);
            verify(userRepository).save(sampleUser);
        }
    }

    // updateUser(Long, UpdateUserReq)
    @Nested
    @DisplayName("updateUser(Long, UpdateUserReq)")
    class UpdateUser {

        @Test
        @DisplayName("Update fullName successfully")
        void shouldUpdateFullName() {
            UpdateUserReq req = new UpdateUserReq();
            req.setFullName("New Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            UserResponse result = userService.updateUser(1L, req);

            assertThat(result).isNotNull();
            verify(userRepository).save(sampleUser);
        }

        @Test
        @DisplayName("Throw ExistsEmailException when new email already taken")
        void shouldThrowWhenNewEmailAlreadyExists() {
            UpdateUserReq req = new UpdateUserReq();
            req.setEmail("taken@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, req))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("Email đã tồn tại");
        }

        @Test
        @DisplayName("Allow keeping same email without conflict check")
        void shouldNotCheckEmailWhenSameEmail() {
            UpdateUserReq req = new UpdateUserReq();
            req.setEmail(sampleUser.getEmail()); // same email, no conflict

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            UserResponse result = userService.updateUser(1L, req);

            assertThat(result).isNotNull();
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Throw ExistsPhoneNumberException when new phone already taken")
        void shouldThrowWhenNewPhoneAlreadyExists() {
            UpdateUserReq req = new UpdateUserReq();
            req.setPhoneNumber("0999999999");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, req))
                    .isInstanceOf(ExistsPhoneNumberException.class)
                    .hasMessageContaining("Số điện thoại đã tồn tại");
        }

        @Test
        @DisplayName("Allow keeping same phone without conflict check")
        void shouldNotCheckPhoneWhenSamePhone() {
            UpdateUserReq req = new UpdateUserReq();
            req.setPhoneNumber(sampleUser.getPhoneNumber()); // same phone

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            UserResponse result = userService.updateUser(1L, req);

            assertThat(result).isNotNull();
            verify(userRepository, never()).existsByPhoneNumber(anyString());
        }

        @Test
        @DisplayName("Update all fields successfully")
        void shouldUpdateAllFields() {
            UpdateUserReq req = new UpdateUserReq();
            req.setFullName("Updated Name");
            req.setEmail("new@example.com");
            req.setPhoneNumber("0988888888");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("0988888888")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            UserResponse result = userService.updateUser(1L, req);

            assertThat(result).isNotNull();
            verify(userRepository).save(sampleUser);
        }

        @Test
        @DisplayName("Throw IdInvalidException when user not found")
        void shouldThrowWhenUserNotFound() {
            UpdateUserReq req = new UpdateUserReq();
            req.setFullName("Name");

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, req))
                    .isInstanceOf(IdInvalidException.class);
        }
    }
}
