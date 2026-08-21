package com.vennhuu.PersonalFinance.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Service.UserService;


@RestController
@RequestMapping("/api/v1")
public class UserController {
    
    private final UserService userService ;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<String> getAllUsers() {
        return ResponseEntity.ok("Đã lấy thành công");
    }
    
}
