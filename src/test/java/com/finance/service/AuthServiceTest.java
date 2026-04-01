package com.finance.service;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.model.UserStatus;
import com.finance.security.JwtUtil;
import com.finance.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil               jwtUtil;

    @InjectMocks AuthService authService;

    private User        testUser;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("admin@finance.com")
                .passwordHash("$2a$12$hashedpassword")
                .fullName("Test Admin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        principal = new UserPrincipal(testUser);
    }

    @Test
    @DisplayName("login() returns AuthResponse with token on valid credentials")
    void login_validCredentials_returnsAuthResponse() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@finance.com");
        request.setPassword("Admin@123");

        var authToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtUtil.generateToken(any(), any())).thenReturn("mocked.jwt.token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("admin@finance.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        verify(jwtUtil).generateToken(any(UserPrincipal.class), eq("ADMIN"));
    }

    @Test
    @DisplayName("login() throws BadCredentialsException on wrong password")
    void login_invalidCredentials_throwsBadCredentialsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@finance.com");
        request.setPassword("WrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
