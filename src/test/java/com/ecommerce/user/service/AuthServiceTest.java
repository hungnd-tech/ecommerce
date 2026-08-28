package com.ecommerce.user.service;

import com.ecommerce.common.security.JwtService;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // ---------- register ----------

    @Test
    void register_khiEmailChuaTonTai_taoUserRoleCustomer_traVeToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("a@gmail.com");
        request.setPassword("password123");
        request.setFullName("Nguyen Van A");

        when(userRepository.existsByEmail("a@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(jwtService.generateToken("a@gmail.com", "CUSTOMER")).thenReturn("fake-jwt");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashed-password")
                        && user.getRole() == User.Role.CUSTOMER));
    }

    @Test
    void register_khiEmailDaTonTai_nem409_khongGoiSave() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("a@gmail.com");
        request.setPassword("password123");
        request.setFullName("Nguyen Van A");

        when(userRepository.existsByEmail("a@gmail.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_khiDungThongTin_traVeTokenDungUser() {
        User user = User.builder()
                .id(1L)
                .email("a@gmail.com")
                .passwordHash("hashed")
                .fullName("Nguyen Van A")
                .role(User.Role.ADMIN)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("a@gmail.com", "ADMIN")).thenReturn("fake-jwt");

        LoginRequest request = new LoginRequest();
        request.setEmail("a@gmail.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void login_khiSaiMatKhauHoacBiKhoa_nem401() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setEmail("a@gmail.com");
        request.setPassword("sai-mat-khau");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));

        assertThat(ex.getStatusCode().value()).isEqualTo(401);
    }
}
