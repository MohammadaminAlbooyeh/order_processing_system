package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    private RegisterRequest registerRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFullName("Jane Doe");
        req.setPhone("+15551234567");
        return req;
    }

    @Test
    void register_success_savesUserAndReturnsToken() {
        RegisterRequest req = registerRequest("jane@example.com", "Password1");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(jwtService.createToken(any())).thenReturn("token123");

        TokenResponse response = userService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(response.getAccessToken()).isEqualTo("token123");
        assertThat(response.getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void register_emailAlreadyRegistered_throws() {
        RegisterRequest req = registerRequest("jane@example.com", "Password1");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_weakPassword_throws() {
        RegisterRequest req = registerRequest("jane@example.com", "lowercase");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setPasswordHash("hashed");
        user.setIsActive(true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(user);
        when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);
        when(jwtService.createToken(1L)).thenReturn("token123");

        LoginRequest req = new LoginRequest();
        req.setEmail("jane@example.com");
        req.setPassword("Password1");

        TokenResponse response = userService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("token123");
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setEmail("missing@example.com");
        req.setPassword("Password1");

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("jane@example.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
        verify(jwtService, never()).createToken(anyLong());
    }

    @Test
    void getUser_found_returnsResponse() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setIsActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getUser_notFound_returnsNull() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        UserResponse response = userService.getUser(404L);

        assertThat(response).isNull();
    }

    @Test
    void updateProfile_updatesFieldsAndSaves() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setFullName("Old Name");
        user.setPhone("+15550000000");
        user.setIsActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateProfile(1L, "New Name", "+15551234567");

        assertThat(response.getFullName()).isEqualTo("New Name");
        assertThat(response.getPhone()).isEqualTo("+15551234567");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_userNotFound_returnsNull() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        UserResponse response = userService.updateProfile(404L, "New Name", null);

        assertThat(response).isNull();
        verify(userRepository, never()).save(any());
    }
}
