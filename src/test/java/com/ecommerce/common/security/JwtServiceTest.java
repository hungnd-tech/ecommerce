package com.ecommerce.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // HMAC-SHA256 cần secret tối thiểu 256 bit (32 ký tự)
        ReflectionTestUtils.setField(jwtService, "secretKey", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 tiếng
    }

    @Test
    void generateToken_roiExtract_traVeDungEmailVaRole() {
        String token = jwtService.generateToken("a@gmail.com", "ADMIN");

        assertThat(jwtService.extractEmail(token)).isEqualTo("a@gmail.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_khiDungEmailVaChuaHetHan_traVeTrue() {
        String token = jwtService.generateToken("a@gmail.com", "CUSTOMER");

        assertThat(jwtService.isTokenValid(token, "a@gmail.com")).isTrue();
    }

    @Test
    void isTokenValid_khiSaiEmail_traVeFalse() {
        String token = jwtService.generateToken("a@gmail.com", "CUSTOMER");

        assertThat(jwtService.isTokenValid(token, "khac@gmail.com")).isFalse();
    }

    @Test
    void extractEmail_khiTokenDaHetHan_nemExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // sinh ra là hết hạn ngay
        String expiredToken = jwtService.generateToken("a@gmail.com", "CUSTOMER");

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(expiredToken));
    }

    @Test
    void extractEmail_khiTokenBiKyBangSecretKhac_nemSignatureException() {
        JwtService attacker = new JwtService();
        ReflectionTestUtils.setField(attacker, "secretKey", "abcdefghijklmnopqrstuvwxyz012345");
        ReflectionTestUtils.setField(attacker, "expirationMs", 3600000L);
        String fakeToken = attacker.generateToken("a@gmail.com", "ADMIN");

        assertThrows(SignatureException.class, () -> jwtService.extractEmail(fakeToken));
    }
}