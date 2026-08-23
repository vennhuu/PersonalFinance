package com.vennhuu.PersonalFinance.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Service.AuthService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService ;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @APIMessage("Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody User user) {
        
        return ResponseEntity.status(HttpStatus.CREATED).body(this.authService.registerNewUser(user));
    }
    
}
