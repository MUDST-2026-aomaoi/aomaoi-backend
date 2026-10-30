package com.aomaoi.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class E2eRunServiceTest {

    private static final String VALID_KEY = "12345678901234567890123456789012"; // 32 chars

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private PasswordEncoder passwordEncoder;

    private E2eRunService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(jdbc.queryForObject(contains("set_config"), eq(String.class), any())).thenReturn("ok");
        service = new E2eRunService(jdbc, transactionManager, passwordEncoder, VALID_KEY);
    }

    private String hashToken(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void enabled_WhenKeyAtLeast32Chars_ReturnsTrue() {
        assertTrue(service.enabled());
    }

    @Test
    void enabled_WhenKeyTooShort_ReturnsFalse() {
        E2eRunService disabledService = new E2eRunService(jdbc, transactionManager, passwordEncoder, "short-key");
        assertFalse(disabledService.enabled());
    }

    @Test
    void authorize_ValidKey_DoesNotThrow() {
        assertDoesNotThrow(() -> service.authorize(VALID_KEY));
    }

    @Test
    void authorize_InvalidKey_ThrowsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.authorize("wrong-key-with-enough-characters-32"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void authorize_NullKey_ThrowsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.authorize(null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void authorize_WhenDisabled_ThrowsForbidden() {
        E2eRunService disabledService = new E2eRunService(jdbc, transactionManager, passwordEncoder, "");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> disabledService.authorize(VALID_KEY));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void create_ValidKey_CreatesRunAndReturnsCredentials() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");

        Map<String, String> result = service.create(VALID_KEY);

        assertNotNull(result);
        assertNotNull(result.get("id"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("username"));
        assertNotNull(result.get("password"));
        assertTrue(result.get("username").startsWith("e2e_super_"));

        verify(jdbc).update(startsWith("INSERT INTO e2e_runs"), eq(result.get("id")), any(String.class));
        verify(jdbc).update(startsWith("INSERT INTO users"), eq(result.get("username")), eq("encoded_pass"));
        verify(jdbc).queryForObject(contains("set_config"), eq(String.class), eq(result.get("id")));
    }

    @Test
    void enter_NullCredentials_ThrowsForbidden() {
        assertThrows(ResponseStatusException.class, () -> service.enter(null, "token"));
        assertThrows(ResponseStatusException.class, () -> service.enter("run-id", null));
    }

    @Test
    void enter_RunNotFoundOrExpired_ThrowsForbidden() {
        when(jdbc.queryForList(anyString(), eq("run-id"))).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.enter("run-id", "some-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void enter_WrongTokenHash_ThrowsForbidden() throws Exception {
        when(jdbc.queryForList(anyString(), eq("run-id"))).thenReturn(List.of(
                Map.of("token_hash", hashToken("correct-token"))
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.enter("run-id", "wrong-token"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void enter_ValidCredentials_UpdatesExpiryAndSetsContext() throws Exception {
        String token = "my-secret-token";
        when(jdbc.queryForList(anyString(), eq("run-id"))).thenReturn(List.of(
                Map.of("token_hash", hashToken(token))
        ));

        service.enter("run-id", token);

        verify(jdbc).update(contains("UPDATE e2e_runs SET expires_at="), eq("run-id"));
        verify(jdbc).queryForObject(contains("set_config"), eq(String.class), eq("run-id"));
    }

    @Test
    void heartbeat_CallsEnter() throws Exception {
        String token = "my-secret-token";
        when(jdbc.queryForList(anyString(), eq("run-id"))).thenReturn(List.of(
                Map.of("token_hash", hashToken(token))
        ));

        service.heartbeat("run-id", token);

        verify(jdbc).update(contains("UPDATE e2e_runs SET expires_at="), eq("run-id"));
    }

    @Test
    void cleanup_Success_DeletesAllTablesAndRun() {
        when(jdbc.queryForList(contains("SELECT id FROM e2e_runs"), eq("run-1"))).thenReturn(List.of(Map.of("id", "run-1")));
        when(jdbc.update(contains("DELETE FROM"), eq("run-1"))).thenReturn(1);
        when(jdbc.queryForObject(contains("SELECT count(*) FROM"), eq(Long.class), eq("run-1"))).thenReturn(0L);

        Map<String, Object> result = service.cleanup("run-1", VALID_KEY);

        assertNotNull(result);
        assertEquals(0, result.get("remaining"));
        verify(jdbc).update(eq("DELETE FROM e2e_runs WHERE id=?"), eq("run-1"));
    }

    @Test
    void cleanup_IncompleteCleanup_ThrowsIllegalStateException() {
        when(jdbc.queryForList(contains("SELECT id FROM e2e_runs"), eq("run-1"))).thenReturn(List.of(Map.of("id", "run-1")));
        when(jdbc.update(contains("DELETE FROM"), eq("run-1"))).thenReturn(1);
        when(jdbc.queryForObject(contains("SELECT count(*) FROM"), eq(Long.class), eq("run-1"))).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.cleanup("run-1", VALID_KEY));
    }

    @Test
    void cleanupExpired_RemovesExpiredRuns() {
        when(jdbc.queryForList(contains("SELECT id FROM e2e_runs WHERE expires_at < now()"), eq(String.class)))
                .thenReturn(List.of("expired-run-1"));
        when(jdbc.update(contains("DELETE FROM"), eq("expired-run-1"))).thenReturn(1);
        when(jdbc.queryForObject(contains("SELECT count(*) FROM"), eq(Long.class), eq("expired-run-1"))).thenReturn(0L);

        service.cleanupExpired();

        verify(jdbc).update(eq("DELETE FROM e2e_runs WHERE id=?"), eq("expired-run-1"));
    }
}
