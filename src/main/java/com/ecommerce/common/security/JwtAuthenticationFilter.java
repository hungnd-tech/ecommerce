package com.ecommerce.common.security;

import com.ecommerce.user.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // check header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7); // cắt bỏ 7 ký tự đầu "Bearer "
            String email = jwtService.extractEmail(token); // vừa verify chữ ký vừa lấy claim sub ra.
            // Nếu token bị giả mạo/hết hạn/sai định dạng, ném JwtException ngay tại đây

            // kiểm tra request này chưa được authenticate bởi bước nào khác trước đó
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Token còn hợp lệ (chữ ký đúng, chưa hết hạn) KHÔNG có nghĩa user vẫn còn dùng được:
                // token là stateless, được phát hành có thể trước khi user bị soft-delete/khoá.
                // Đã có sẵn userDetails trong tay (không tốn thêm query) nên check luôn ở đây,
                // để 1 token đã phát hành mất hiệu lực ngay khi tài khoản bị khoá, thay vì phải
                // chờ tự hết hạn (tối đa 24h).
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    // thêm metadata phụ (như IP address, session ID) vào token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken); // request đã login
                }
                // Nếu tài khoản bị khoá: không set Authentication -> request đi tiếp như chưa đăng nhập,
                // endpoint cần login sẽ tự bị chặn 401 ở bước authorize (SecurityConfig), đúng bản chất.
            }

        } catch (JwtException | UsernameNotFoundException ex) {
            // Token sai/hết hạn/user không tồn tại -> để request đi tiếp không xác thực,
            // endpoint nào cần login sẽ tự bị chặn ở bước authorize (SecurityConfig)
        }
        filterChain.doFilter(request, response);
    }
}
