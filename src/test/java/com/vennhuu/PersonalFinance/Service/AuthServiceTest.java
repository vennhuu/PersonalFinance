package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.vennhuu.PersonalFinance.Entity.RefreshToken;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqLoginDTO;

import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqChangePasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqForgotPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqResetPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;
import com.vennhuu.PersonalFinance.Exception.ExistsEmailException;
import com.vennhuu.PersonalFinance.Exception.IdInvalidException;
import com.vennhuu.PersonalFinance.Repository.RoleRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;
import com.vennhuu.PersonalFinance.Service.Producer.RabbitMQProducer;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private RabbitMQProducer rabbitMQProducer;

    @InjectMocks
    private AuthService authService;

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
        sampleUser.setPassword("hashed_pass");
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setRole(sampleRole);
    }

    // existsByEmail / existsByPhoneNumber
    @Nested
    @DisplayName("existsByEmail and existsByPhoneNumber")
    class ExistsChecks {

        @Test
        @DisplayName("existsByEmail returns true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            when(userRepository.existsByEmail("a@example.com")).thenReturn(true);
            assertThat(authService.existsByEmail("a@example.com")).isTrue();
        }

        @Test
        @DisplayName("existsByEmail returns false when email not found")
        void shouldReturnFalseWhenEmailNotFound() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            assertThat(authService.existsByEmail("new@example.com")).isFalse();
        }

        @Test
        @DisplayName("existsByPhoneNumber returns true when phone exists")
        void shouldReturnTrueWhenPhoneExists() {
            when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(true);
            assertThat(authService.existsByPhoneNumber("0912345678")).isTrue();
        }

        @Test
        @DisplayName("existsByPhoneNumber returns false when phone not found")
        void shouldReturnFalseWhenPhoneNotFound() {
            when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
            assertThat(authService.existsByPhoneNumber("0999999999")).isFalse();
        }
    }

    // registerNewUser
    @Nested
    @DisplayName("registerNewUser(User)")
    class RegisterNewUser {

        @Test
        @DisplayName("Throw ExistsEmailException when email already exists")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByEmail(sampleUser.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerNewUser(sampleUser))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("Email đã tồn tại");
        }

        @Test
        @DisplayName("Throw ExistsPhoneNumberException when phone already exists")
        void shouldThrowWhenPhoneExists() {
            when(userRepository.existsByEmail(sampleUser.getEmail())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(sampleUser.getPhoneNumber())).thenReturn(true);

            assertThatThrownBy(() -> authService.registerNewUser(sampleUser))
                    .isInstanceOf(com.vennhuu.PersonalFinance.Exception.ExistsPhoneNumberException.class)
                    .hasMessageContaining("Số điện thoại đã tồn tại");
        }

        @Test
        @DisplayName("Register successfully with ACTIVE status and ROLE_USER")
        void shouldRegisterSuccessfully() {
            when(userRepository.existsByEmail(sampleUser.getEmail())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(sampleUser.getPhoneNumber())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(sampleRole);
            when(userService.save(any(User.class))).thenReturn(sampleUser);

            UserResponse expectedResponse = new UserResponse();
            expectedResponse.setId(1L);
            expectedResponse.setEmail("a@example.com");
            when(userService.convertToUserResponse(any(User.class), any(Wallet.class)))
                    .thenReturn(expectedResponse);

            UserResponse result = authService.registerNewUser(sampleUser);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("a@example.com");
            verify(passwordEncoder).encode(anyString());
            verify(roleRepository).findByName(RoleName.ROLE_USER);
            verify(userService).save(sampleUser);
        }
    }

    // generate6DigitNumber
    @Nested
    @DisplayName("generate6DigitNumber()")
    class GenerateOtp {

        @RepeatedTest(5)
        @DisplayName("Always generates a 6-character numeric string")
        void shouldAlwaysReturn6Digits() {
            String otp = authService.generate6DigitNumber();
            assertThat(otp).hasSize(6);
            assertThat(otp).matches("\\d{6}");
        }
    }

    // sendForgotPasswordOtp
    @Nested
    @DisplayName("sendForgotPasswordOtp(ReqForgotPasswordDTO)")
    class SendForgotPasswordOtp {

        @Test
        @DisplayName("Throw ExistsEmailException when email not in system")
        void shouldThrowWhenEmailNotFound() {
            ReqForgotPasswordDTO req = new ReqForgotPasswordDTO();
            req.setEmail("unknown@example.com");
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

            assertThatThrownBy(() -> authService.sendForgotPasswordOtp(req))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("không tồn tại");
        }

        @Test
        @DisplayName("Send OTP message and save user when email found")
        void shouldSendOtpWhenEmailFound() {
            ReqForgotPasswordDTO req = new ReqForgotPasswordDTO();
            req.setEmail("a@example.com");
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            authService.sendForgotPasswordOtp(req);

            verify(userRepository).save(sampleUser);
            verify(rabbitMQProducer).sendOtpEmail(any());
            assertThat(sampleUser.getOtpCode()).isNotNull().hasSize(6);
        }
    }

    // resetPassword
    @Nested
    @DisplayName("resetPassword(ReqResetPasswordDTO)")
    class ResetPassword {

        @Test
        @DisplayName("Throw when email not found")
        void shouldThrowWhenEmailNotFound() {
            ReqResetPasswordDTO req = new ReqResetPasswordDTO();
            req.setEmail("ghost@example.com");
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(ExistsEmailException.class);
        }

        @Test
        @DisplayName("Throw when OTP is null")
        void shouldThrowWhenOtpIsNull() {
            ReqResetPasswordDTO req = new ReqResetPasswordDTO();
            req.setEmail("a@example.com");
            req.setOtpCode("123456");
            sampleUser.setOtpCode(null);
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("Mã OTP không chính xác");
        }

        @Test
        @DisplayName("Throw when OTP does not match")
        void shouldThrowWhenOtpDoesNotMatch() {
            ReqResetPasswordDTO req = new ReqResetPasswordDTO();
            req.setEmail("a@example.com");
            req.setOtpCode("000000");
            sampleUser.setOtpCode("999999");
            sampleUser.setOtpExpiredAt(Instant.now().plus(5, ChronoUnit.MINUTES));
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("Mã OTP không chính xác");
        }

        @Test
        @DisplayName("Throw when OTP is expired")
        void shouldThrowWhenOtpExpired() {
            ReqResetPasswordDTO req = new ReqResetPasswordDTO();
            req.setEmail("a@example.com");
            req.setOtpCode("123456");
            sampleUser.setOtpCode("123456");
            sampleUser.setOtpExpiredAt(Instant.now().minus(1, ChronoUnit.MINUTES)); // expired
            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(ExistsEmailException.class)
                    .hasMessageContaining("Mã OTP đã hết hạn");
        }

        @Test
        @DisplayName("Reset password successfully when OTP valid")
        void shouldResetPasswordSuccessfully() {
            ReqResetPasswordDTO req = new ReqResetPasswordDTO();
            req.setEmail("a@example.com");
            req.setOtpCode("123456");
            req.setNewPassword("newPass123");
            sampleUser.setOtpCode("123456");
            sampleUser.setOtpExpiredAt(Instant.now().plus(5, ChronoUnit.MINUTES));

            when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);
            when(passwordEncoder.encode("newPass123")).thenReturn("new_hash");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);

            authService.resetPassword(req);

            verify(passwordEncoder).encode("newPass123");
            verify(userRepository).save(sampleUser);
            assertThat(sampleUser.getOtpCode()).isNull();
            assertThat(sampleUser.getOtpExpiredAt()).isNull();
        }
    }

    // changePassword
    @Nested
    @DisplayName("changePassword(ReqChangePasswordDTO)")
    class ChangePassword {

        @Test
        @DisplayName("Throw BadCredentialsException when not logged in")
        void shouldThrowWhenNotLoggedIn() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

                ReqChangePasswordDTO req = new ReqChangePasswordDTO();
                req.setOldPassword("old");
                req.setNewPassword("new");

                assertThatThrownBy(() -> authService.changePassword(req))
                        .isInstanceOf(BadCredentialsException.class)
                        .hasMessageContaining("Vui lòng đăng nhập");
            }
        }

        @Test
        @DisplayName("Throw ExistsEmailException when user not found for email")
        void shouldThrowWhenUserNotFound() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("a@example.com"));
                when(userRepository.findByEmail("a@example.com")).thenReturn(null);

                ReqChangePasswordDTO req = new ReqChangePasswordDTO();
                req.setOldPassword("old");
                req.setNewPassword("new");

                assertThatThrownBy(() -> authService.changePassword(req))
                        .isInstanceOf(ExistsEmailException.class)
                        .hasMessageContaining("Tài khoản không tồn tại");
            }
        }

        @Test
        @DisplayName("Throw IdInvalidException when old password is wrong")
        void shouldThrowWhenOldPasswordWrong() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("a@example.com"));
                when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);
                when(passwordEncoder.matches("wrongPass", sampleUser.getPassword())).thenReturn(false);

                ReqChangePasswordDTO req = new ReqChangePasswordDTO();
                req.setOldPassword("wrongPass");
                req.setNewPassword("newPass");

                assertThatThrownBy(() -> authService.changePassword(req))
                        .isInstanceOf(IdInvalidException.class)
                        .hasMessageContaining("Mật khẩu hiện tại không chính xác");
            }
        }

        @Test
        @DisplayName("Change password successfully")
        void shouldChangePasswordSuccessfully() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("a@example.com"));
                when(userRepository.findByEmail("a@example.com")).thenReturn(sampleUser);
                when(passwordEncoder.matches("correctOld", sampleUser.getPassword())).thenReturn(true);
                when(passwordEncoder.encode("newSecurePass")).thenReturn("new_hash");
                when(userRepository.save(any(User.class))).thenReturn(sampleUser);

                ReqChangePasswordDTO req = new ReqChangePasswordDTO();
                req.setOldPassword("correctOld");
                req.setNewPassword("newSecurePass");

                authService.changePassword(req);

                verify(userRepository).save(sampleUser);
                assertThat(sampleUser.getPassword()).isEqualTo("new_hash");
            }
        }
    }

    // logout
    @Nested
    @DisplayName("logout(String)")
    class Logout {

        @Test
        @DisplayName("Delete refresh token when token is provided")
        void shouldDeleteTokenWhenProvided() {
            authService.logout("some-refresh-token");

            verify(refreshTokenService).deleteByToken("some-refresh-token");
        }

        @Test
        @DisplayName("Skip token deletion when token is null")
        void shouldSkipDeletionWhenTokenIsNull() {
            authService.logout(null);

            verify(refreshTokenService, never()).deleteByToken(anyString());
        }

        @Test
        @DisplayName("Skip token deletion when token is blank")
        void shouldSkipDeletionWhenTokenIsBlank() {
            authService.logout("   ");

            verify(refreshTokenService, never()).deleteByToken(anyString());
        }
    }

    // getAccount
    @Nested
    @DisplayName("getAccount()")
    class GetAccount {

        @Test
        @DisplayName("Throw BadCredentialsException when no authenticated user")
        void shouldThrowWhenNotAuthenticated() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.getAccount())
                        .isInstanceOf(BadCredentialsException.class)
                        .hasMessageContaining("Vui lòng đăng nhập");
            }
        }

        @Test
        @DisplayName("Throw BadCredentialsException when user not found by email")
        void shouldThrowWhenUserNotFound() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("a@example.com"));
                when(userService.findByEmail("a@example.com")).thenReturn(null);

                assertThatThrownBy(() -> authService.getAccount())
                        .isInstanceOf(BadCredentialsException.class)
                        .hasMessageContaining("Tài khoản không tồn tại");
            }
        }

        @Test
        @DisplayName("Return UserGetAccount when authenticated and user found")
        void shouldReturnAccountWhenAuthenticated() {
            try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("a@example.com"));
                when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);

                var result = authService.getAccount();

                assertThat(result).isNotNull();
                assertThat(result.getUser()).isNotNull();
                assertThat(result.getUser().getEmail()).isEqualTo("a@example.com");
                assertThat(result.getUser().getName()).isEqualTo("Nguyen Van A");
            }
        }
    }

    // login
    @Nested
    @DisplayName("login(ReqLoginDTO, HttpServletRequest)")
    class Login {

        @Test
        @DisplayName("Authenticate and return tokens with user details")
        void shouldLoginSuccessfully() {
            ReqLoginDTO req = new ReqLoginDTO();
            req.setEmail("a@example.com");
            req.setPassword("password123");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Mozilla/5.0");

            Authentication auth = new UsernamePasswordAuthenticationToken("a@example.com", "password123");
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
            when(securityUtil.createAccessToken(eq("a@example.com"), any())).thenReturn("access-token-123");
            when(securityUtil.createRefreshToken(eq("a@example.com"), any())).thenReturn("refresh-token-123");

            var result = authService.login(req, request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token-123");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token-123");
            assertThat(result.getUser().getEmail()).isEqualTo("a@example.com");
            verify(refreshTokenService).createToken(eq("refresh-token-123"), eq("a@example.com"), eq("Mozilla/5.0"));
        }
    }

    // refreshToken
    @Nested
    @DisplayName("refreshToken(String, HttpServletRequest)")
    class RefreshTokenTests {

        @Test
        @DisplayName("Throw when refresh token is null or blank")
        void shouldThrowWhenTokenBlank() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThatThrownBy(() -> authService.refreshToken(null, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("cookie");

            assertThatThrownBy(() -> authService.refreshToken("   ", request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("cookie");
        }

        @Test
        @DisplayName("Throw when token is revoked or not found in DB")
        void shouldThrowWhenTokenRevokedOrNotFound() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Jwt mockJwt = mock(Jwt.class);
            when(mockJwt.getSubject()).thenReturn("a@example.com");
            when(securityUtil.checkValidRefreshToken("revoked-token")).thenReturn(mockJwt);
            when(refreshTokenService.findByToken("revoked-token")).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshToken("revoked-token", request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("thu hồi");
        }

        @Test
        @DisplayName("Throw when user not found")
        void shouldThrowWhenUserNotFound() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            Jwt mockJwt = mock(Jwt.class);
            when(mockJwt.getSubject()).thenReturn("a@example.com");
            when(securityUtil.checkValidRefreshToken("valid-token")).thenReturn(mockJwt);

            RefreshToken tokenEntity = new RefreshToken();
            tokenEntity.setRevoked(false);
            when(refreshTokenService.findByToken("valid-token")).thenReturn(tokenEntity);
            when(userService.findByEmail("a@example.com")).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshToken("valid-token", request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Tài khoản không tồn tại");
        }

        @Test
        @DisplayName("Generate new tokens and delete old token successfully")
        void shouldRefreshTokenSuccessfully() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Chrome");

            Jwt mockJwt = mock(Jwt.class);
            when(mockJwt.getSubject()).thenReturn("a@example.com");
            when(securityUtil.checkValidRefreshToken("old-token")).thenReturn(mockJwt);

            RefreshToken tokenEntity = new RefreshToken();
            tokenEntity.setRevoked(false);
            when(refreshTokenService.findByToken("old-token")).thenReturn(tokenEntity);
            when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
            when(securityUtil.createAccessToken(eq("a@example.com"), any())).thenReturn("new-access");
            when(securityUtil.createRefreshToken(eq("a@example.com"), any())).thenReturn("new-refresh");

            var result = authService.refreshToken("old-token", request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-access");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
            verify(refreshTokenService).deleteByToken("old-token");
            verify(refreshTokenService).createToken(eq("new-refresh"), eq("a@example.com"), eq("Chrome"));
        }
    }
}
