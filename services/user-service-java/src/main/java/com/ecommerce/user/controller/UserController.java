package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        if (response == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "User not found"));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        Long userId = (Long) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                .getAttribute("userId", org.springframework.web.context.request.RequestContextHolder.SCOPE_REQUEST);
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "Invalid token"));
        }
        UserResponse response = userService.getUser(userId);
        if (response == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("detail", "User not found"));
        }
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest body) {
        Long userId = (Long) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                .getAttribute("userId", org.springframework.web.context.request.RequestContextHolder.SCOPE_REQUEST);
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "Invalid token"));
        }
        UserResponse response = userService.updateProfile(userId, body.getFullName(), body.getPhone());
        return ResponseEntity.ok(response);
    }
}
