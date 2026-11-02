package com.aomaoi.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Pulls the secret key and expiration time from your application.properties file
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }

    // Creates the cryptographic signing key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 1. Generate a new Token (Called when a user logs in successfully)
    public String generateToken(UserDetails userDetails, String role) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role", role)
                .claim("credentialVersion", credentialVersion(userDetails))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Extract the Username from an existing Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. Extract the Role from an existing Token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // 4. Validate if the Token is real and hasn't expired
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        String issuedVersion = extractClaim(token,
                claims -> claims.get("credentialVersion", String.class));
        String currentVersion = credentialVersion(userDetails);
        return userDetails.isEnabled() && username.equals(userDetails.getUsername())
                && issuedVersion != null && MessageDigest.isEqual(
                    issuedVersion.getBytes(StandardCharsets.UTF_8), currentVersion.getBytes(StandardCharsets.UTF_8))
                && !isTokenExpired(token);
    }

    private String credentialVersion(UserDetails userDetails) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(userDetails.getPassword().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create credential version", e);
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
