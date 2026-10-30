package com.aomaoi.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class E2eRunService {
    private static final String[] TABLES = {"audit_logs", "salary_advances", "work_logs", "worker", "admin_profile", "users", "farm"};
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PasswordEncoder encoder;
    private final String supportKey;

    public E2eRunService(JdbcTemplate jdbc, PlatformTransactionManager manager, PasswordEncoder encoder,
                         @Value("${app.e2e.support-key:}") String supportKey) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
        this.encoder = encoder;
        this.supportKey = supportKey;
    }

    public boolean enabled() { return supportKey.length() >= 32; }

    public void authorize(String key) {
        if (!enabled() || key == null || !MessageDigest.isEqual(
                supportKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E2E support is disabled or key is invalid");
        }
    }

    public Map<String, String> create(String key) {
        authorize(key);
        return tx.execute(status -> {
            String id = UUID.randomUUID().toString();
            String token = UUID.randomUUID() + "-" + UUID.randomUUID();
            String username = "e2e_super_" + id.replace("-", "");
            String password = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO e2e_runs VALUES (?, ?, now() + interval '15 minutes')", id, hash(token));
            setContext(id);
            jdbc.update("INSERT INTO users(username,password,role,is_first_login) VALUES (?,?,'superadmin',false)",
                    username, encoder.encode(password));
            return Map.of("id", id, "token", token, "username", username, "password", password);
        });
    }

    /** Must run inside the request transaction; the row lock excludes concurrent cleanup. */
    public void enter(String id, String token) {
        if (!enabled() || id == null || token == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E2E run credentials required");
        }
        var rows = jdbc.queryForList(
                "SELECT token_hash FROM e2e_runs WHERE id=? AND expires_at > now() FOR UPDATE", id);
        if (rows.isEmpty() || !hash(token).equals(rows.getFirst().get("token_hash"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E2E run expired or invalid");
        }
        jdbc.update("UPDATE e2e_runs SET expires_at=now()+interval '15 minutes' WHERE id=?", id);
        setContext(id);
    }

    public void heartbeat(String id, String token) {
        tx.executeWithoutResult(status -> enter(id, token));
    }

    public Map<String, Object> cleanup(String id, String key) {
        authorize(key);
        return tx.execute(status -> {
            jdbc.queryForList("SELECT id FROM e2e_runs WHERE id=? FOR UPDATE", id);
            return remove(id);
        });
    }

    private Map<String, Object> remove(String id) {
        setContext(id);
        int deleted = 0;
        for (String table : TABLES) {
            deleted += jdbc.update("DELETE FROM " + table + " WHERE e2e_run_id=?", id);
        }
        for (String table : TABLES) {
            if (jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE e2e_run_id=?", Long.class, id) != 0) {
                throw new IllegalStateException("E2E cleanup incomplete");
            }
        }
        jdbc.update("DELETE FROM e2e_runs WHERE id=?", id);
        return Map.of("deleted", deleted, "remaining", 0);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupExpired() {
        tx.executeWithoutResult(status -> {
            var ids = jdbc.queryForList(
                    "SELECT id FROM e2e_runs WHERE expires_at < now() FOR UPDATE SKIP LOCKED", String.class);
            for (String id : ids) remove(id);
        });
    }

    private void setContext(String id) {
        jdbc.queryForObject("SELECT set_config('aomaoi.e2e_run', ?, true)", String.class, id);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
