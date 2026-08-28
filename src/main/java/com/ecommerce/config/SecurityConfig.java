package com.ecommerce.config;

import com.ecommerce.common.security.JwtAuthenticationEntryPoint;
import com.ecommerce.common.security.JwtAuthenticationFilter;
import com.ecommerce.common.security.LoginRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration // khai @Bean thủ công, đảm bảo singleton
@EnableWebSecurity // tự định nghĩa SecurityFilterChain ( dùng cho REST API)
@EnableMethodSecurity // bật khả năng dùng @PreAuthorize("hasRole('ADMIN')") ngay trên method của controller/service
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final LoginRateLimitFilter loginRateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // quản lí việc xác thực
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean // thực hiện việc xác thực bằng username/password so với dữ liệu trong DB
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean // cấu hình toàn bộ luật bảo mật
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Tắt bảo mật CSRF, CSRF chỉ nguu hiểm khi xác thực dựa trên session/cookie.
                .csrf(csrf -> csrf.disable())
                // không tạo, không dùng HttpSession để lưu trạng thái đăng nhập
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Đăng ký nơi xử lý khi có request bị từ chối vì chưa xác thực
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // /error: endpoint khi có exception xảy ra ở tầng dưới;
                        // nếu rơi vào authenticated(), request bị lỗi bị chặn thêm 1 lớp nữa thành 401 sai bản chất
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        .anyRequest().authenticated() // cần authen
                )
                // báo Spring Security dùng provider này khi cần xác thực username/password thật (áp dụng cho luồng login ban đầu, trước khi có JWT).
                .authenticationProvider(authenticationProvider())
                // rate limit theo IP+path
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                // với mọi request có token, jwtAuthenticationFilter chạy trước, tự set SecurityContextHolder nếu
                // token hợp lệ
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Chốt toàn bộ cấu hình, tạo ra SecurityFilterChain
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // false vì auth qua header Authorization (Bearer), không qua cookie -
        // allowCredentials chỉ cần true khi FE gửi cookie/session kèm request
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
