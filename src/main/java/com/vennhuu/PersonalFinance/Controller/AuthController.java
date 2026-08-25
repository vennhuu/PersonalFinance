package com.vennhuu.PersonalFinance.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqChangePasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqForgotPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqLoginDTO;
import com.vennhuu.PersonalFinance.Entity.Request.Auth.ReqResetPasswordDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Auth.ResLoginDTO;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Service.AuthService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${venn.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @APIMessage("Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.authService.registerNewUser(user));
    }

    @PostMapping("/login")
    @APIMessage("Login account")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO req, HttpServletRequest request) {
        ResLoginDTO res = this.authService.login(req, request);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", res.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(res);
    }

    @GetMapping("/account")
    @APIMessage("Fetch user account profile")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        return ResponseEntity.ok(this.authService.getAccount());
    }

    @GetMapping("/refresh")
    @APIMessage("Get User by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        ResLoginDTO res = this.authService.refreshToken(refreshToken, request);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", res.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(res);
    }

    @PostMapping("/logout")
    @APIMessage("Logout user")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        this.authService.logout(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/send-otp")
    @APIMessage("Send OTP to email")
    public ResponseEntity<String> sendForgotPasswordOtp(@Valid @RequestBody ReqForgotPasswordDTO req) {
        this.authService.sendForgotPasswordOtp(req);
        return ResponseEntity.ok("Mã OTP đã được gửi về email của bạn");
    }

    @PostMapping("/reset-password")
    @APIMessage("Reset password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ReqResetPasswordDTO req) {
        this.authService.resetPassword(req);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PostMapping("/change-password")
    @APIMessage("Change password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ReqChangePasswordDTO req) {
        this.authService.changePassword(req);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
