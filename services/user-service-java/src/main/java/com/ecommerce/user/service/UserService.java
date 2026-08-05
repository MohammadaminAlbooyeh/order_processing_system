package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse register(RegisterRequest body) {
        if (userRepository.existsByEmail(body.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (!isValidPassword(body.getPassword())) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter and one digit");
        }

        User user = new User();
        user.setEmail(body.getEmail());
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        user.setFullName(body.getFullName());
        user.setPhone(body.getPhone());
        userRepository.save(user);

        String token = jwtService.createToken(user.getId());
        return new TokenResponse(token, toResponse(user));
    }

    public TokenResponse login(LoginRequest body) {
        User user = userRepository.findByEmail(body.getEmail());
        if (user == null || !passwordEncoder.matches(body.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtService.createToken(user.getId());
        return new TokenResponse(token, toResponse(user));
    }

    public UserResponse getUser(Long userId) {
        return userRepository.findById(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    public UserResponse updateProfile(Long userId, String fullName, String phone) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        userRepository.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getIsActive()
        );
    }

    private boolean isValidPassword(String password) {
        boolean hasUpper = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasDigit;
    }
}
