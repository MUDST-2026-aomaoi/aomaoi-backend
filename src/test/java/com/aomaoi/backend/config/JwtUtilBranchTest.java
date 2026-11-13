package com.aomaoi.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** Fills the branch gaps left by {@link JwtUtilTest}. */
class JwtUtilBranchTest {

    private static final String SECRET = "aomaoi_super_secret_key_for_farm_accounting_system_make_it_long";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3_600_000L);
    }

    private static UserDetails user(String name, String password, boolean enabled) {
        return User.withUsername(name).password(password).authorities(Collections.emptyList())
                .disabled(!enabled).build();
    }

    @Test
    void validateSecret_acceptsLongSecret() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret"));
    }

    @Test
    void validateSecret_rejectsShortSecret() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "too-short");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret"));
        assertEquals("JWT_SECRET must contain at least 32 UTF-8 bytes", ex.getMessage());
    }

    @Test
    void validateSecret_rejectsMissingSecret() {
        ReflectionTestUtils.setField(jwtUtil, "secret", null);

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret"));
    }

    @Test
    void validateToken_falseWhenUserDisabled() {
        String token = jwtUtil.generateToken(user("u", "pw", true), "ADMIN");

        assertFalse(jwtUtil.validateToken(token, user("u", "pw", false)));
    }

    @Test
    void validateToken_falseWhenPasswordChangedSinceIssue() {
        String token = jwtUtil.generateToken(user("u", "old-hash", true), "ADMIN");

        assertFalse(jwtUtil.validateToken(token, user("u", "new-hash", true)));
    }

    @Test
    void validateToken_falseWhenTokenCarriesNoCredentialVersion() {
        String legacyToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("u")
                .claim("role", "ADMIN")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtUtil.validateToken(legacyToken, user("u", "pw", true)));
    }

    @Test
    void validateToken_trueForMatchingActiveUser() {
        UserDetails details = user("u", "pw", true);

        assertTrue(jwtUtil.validateToken(jwtUtil.generateToken(details, "ADMIN"), details));
    }

    @Test
    void generateToken_wrapsFailureWhenCredentialVersionCannotBeComputed() {
        // A null password makes the HMAC input invalid -> wrapped as IllegalStateException
        UserDetails noPassword = new UserDetails() {
            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return Collections.emptyList();
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getUsername() {
                return "u";
            }
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jwtUtil.generateToken(noPassword, "ADMIN"));
        assertEquals("Cannot create credential version", ex.getMessage());
    }
}
