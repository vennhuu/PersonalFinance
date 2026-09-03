package com.vennhuu.PersonalFinance.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.vennhuu.PersonalFinance.Entity.RefreshToken;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqChangePasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqForgotPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqLoginDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqResetPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Auth.ResLoginDTO;
import com.vennhuu.PersonalFinance.Entity.Response.RabbitMQ.OtpEmailMessage;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.Role;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.RoleName;
import com.vennhuu.PersonalFinance.Enum.UserStatus;
import com.vennhuu.PersonalFinance.Exception.ExistsEmailException;
import com.vennhuu.PersonalFinance.Exception.ExistsPhoneNumberException;
import com.vennhuu.PersonalFinance.Exception.IdInvalidException;
import com.vennhuu.PersonalFinance.Repository.RoleRepository;
import com.vennhuu.PersonalFinance.Repository.UserRepository;
import com.vennhuu.PersonalFinance.Service.Producer.RabbitMQProducer;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final RabbitMQProducer rabbitMQProducer ;

    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            SecurityUtil securityUtil,
            RefreshTokenService refreshTokenService,
            EmailService emailService,
            RabbitMQProducer rabbitMQProducer
        ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.securityUtil = securityUtil;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.rabbitMQProducer = rabbitMQProducer ;
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

        String hashPassword = this.passwordEncoder.encode(user.getPassword());
        user.setPassword(hashPassword);
        user.setStatus(UserStatus.ACTIVE);

        Role r = this.roleRepository.findByName(RoleName.ROLE_USER);
        user.setRole(r);
        this.userService.save(user);

        Wallet wallet = new Wallet();
        return this.userService.convertToUserResponse(user, wallet);
    }

    // login logic
    public ResLoginDTO login(ReqLoginDTO req, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword());

        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User currentUser = userService.findByEmail(req.getEmail());

        ResLoginDTO res = new ResLoginDTO();
        if (currentUser != null) {
            res.setUser(new ResLoginDTO.UserLogin(
                    currentUser.getId(),
                    currentUser.getEmail(),
                    currentUser.getFullName()));
        }

        String accessToken = securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(accessToken);

        String refreshToken = securityUtil.createRefreshToken(req.getEmail(), res);
        res.setRefreshToken(refreshToken);
        refreshTokenService.createToken(refreshToken, req.getEmail(), request.getHeader("User-Agent"));

        return res;
    }

    // refresh token logic
    public ResLoginDTO refreshToken(String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Bạn không truyền refresh token ở cookie");
        }

        Jwt jwt = this.securityUtil.checkValidRefreshToken(refreshToken);
        String email = jwt.getSubject();

        RefreshToken storedToken = this.refreshTokenService.findByToken(refreshToken);
        if (storedToken == null || storedToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã bị thu hồi");
        }

        User currentUser = this.userService.findByEmail(email);
        if (currentUser == null) {
            throw new BadCredentialsException("Tài khoản không tồn tại");
        }

        this.refreshTokenService.deleteByToken(refreshToken);

        ResLoginDTO res = new ResLoginDTO();
        res.setUser(new ResLoginDTO.UserLogin(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getFullName()));

        String newAccessToken = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(newAccessToken);

        String newRefreshToken = this.securityUtil.createRefreshToken(email, res);
        res.setRefreshToken(newRefreshToken);

        this.refreshTokenService.createToken(newRefreshToken, email, request.getHeader("User-Agent"));

        return res;
    }

    // fetch logged-in user profile
    public ResLoginDTO.UserGetAccount getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) {
            throw new BadCredentialsException("Vui lòng đăng nhập");
        }

        User currentUser = this.userService.findByEmail(email);
        if (currentUser == null) {
            throw new BadCredentialsException("Tài khoản không tồn tại");
        }

        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getFullName());

        return new ResLoginDTO.UserGetAccount(userLogin);
    }

    // logout
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshTokenService.deleteByToken(refreshToken);
        }
        SecurityContextHolder.clearContext();
    }

    // generate 6 digit OTP
    public String generate6DigitNumber() {
        int randomInt = ThreadLocalRandom.current().nextInt(1000000);
        return String.format("%06d", randomInt);
    }

    // send OTP for forgot password
    public void sendForgotPasswordOtp(ReqForgotPasswordDTO req) {
        User user = this.userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new ExistsEmailException("Email không tồn tại trong hệ thống");
        }

        String otpCode = generate6DigitNumber();
        user.setOtpCode(otpCode);
        user.setOtpExpiredAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        this.userRepository.save(user);

        OtpEmailMessage message = new OtpEmailMessage(user.getEmail(), user.getFullName(), otpCode);
        this.rabbitMQProducer.sendOtpEmail(message);
    }

    // reset password using OTP
    public void resetPassword(ReqResetPasswordDTO req) {
        User user = this.userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new ExistsEmailException("Email không tồn tại trong hệ thống");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.getOtpCode())) {
            throw new ExistsEmailException("Mã OTP không chính xác");
        }

        if (user.getOtpExpiredAt() == null || user.getOtpExpiredAt().isBefore(Instant.now())) {
            throw new ExistsEmailException("Mã OTP đã hết hạn");
        }

        user.setPassword(this.passwordEncoder.encode(req.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        this.userRepository.save(user);
    }

    // change password when login
    public void changePassword(ReqChangePasswordDTO req) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập để đổi mật khẩu"));

        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new ExistsEmailException("Tài khoản không tồn tại");
        }

        if (!this.passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new IdInvalidException("Mật khẩu hiện tại không chính xác");
        }

        user.setPassword(this.passwordEncoder.encode(req.getNewPassword()));
        this.userRepository.save(user);
    }
}
