package com.rishabh.leave_management_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JWTService {

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor("ThisIsMyVerySecureSecretKeyForJwtAuthentication12345".getBytes(StandardCharsets.UTF_8)
            );

    public String generateToken(UserDetails userDetails){

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // Token valid for 1 hours
                .signWith(secretKey)
                .compact();
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token){
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    public Date extractExpiration(String token){
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token){
        Date expirationDate = extractExpiration(token);
        return expirationDate.before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        String email = extractUsername(token);
        String userEmail = userDetails.getUsername();
        return (email.equals(userEmail) && !isTokenExpired(token));
    }
}
