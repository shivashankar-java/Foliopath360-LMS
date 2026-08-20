package com.foliopath360.lms.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(
            @Value("${jwt.secret}") String secret
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public String extractUsername(String token) {

        return extractAllClaims(token)
                .get("username", String.class);
    }

    public boolean isTokenExpired(String token) {

        Date expiration =
                extractAllClaims(token).getExpiration();

        return expiration.before(new Date());
    }

    public boolean isTokenValid(String token) {

        try {
            extractAllClaims(token);

            return !isTokenExpired(token);

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
