package com.vennhuu.PersonalFinance.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vennhuu.PersonalFinance.Entity.Request.User.UpdateUserReq;
import com.vennhuu.PersonalFinance.Entity.Response.User.UserResponse;
import com.vennhuu.PersonalFinance.Service.UserService;
import com.vennhuu.PersonalFinance.Utils.Annotation.APIMessage;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1")
public class UserController {
    
    private final UserService userService ;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @APIMessage("Get all Users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(this.userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    @APIMessage("Get user by id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable long id) {
        return ResponseEntity.ok(this.userService.findById(id));
    }

    @PutMapping("/users/{id}")
    @APIMessage("Update user by id")
    public ResponseEntity<UserResponse> updateUser(@PathVariable long id, @Valid @RequestBody UpdateUserReq updateUser) {
        return ResponseEntity.ok(this.userService.updateUser(id, updateUser));
    }

}
