package com.aomaoi.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/** Reports whether this backend is connected to the local disposable E2E database. */
@RestController
@RequestMapping("/api/test-support")
public class E2eTestSafetyController {

    private final String datasourceUrl;
    private final boolean allowDockerTestDatabase;

    public E2eTestSafetyController(
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${app.e2e.docker.database:false}") boolean allowDockerTestDatabase) {
        this.datasourceUrl = datasourceUrl;
        this.allowDockerTestDatabase = allowDockerTestDatabase;
    }

    @GetMapping("/e2e-readiness")
    public ResponseEntity<Map<String, Boolean>> getE2eReadiness() {
        return ResponseEntity.ok(Map.of("safeForE2e", isDisposableLocalDatabase(datasourceUrl, allowDockerTestDatabase)));
    }

    static boolean isDisposableLocalDatabase(String jdbcUrl) {
        return isDisposableLocalDatabase(jdbcUrl, false);
    }

    static boolean isDisposableLocalDatabase(String jdbcUrl, boolean allowDockerTestDatabase) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            return false;
        }

        try {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String host = uri.getHost();
            if (host != null) {
                host = host.replace("[", "").replace("]", "");
            }
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            boolean localHost = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
            boolean composeTestHost = allowDockerTestDatabase && "postgres".equalsIgnoreCase(host);
            return (localHost || composeTestHost) && "aomaoi_test".equalsIgnoreCase(database);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
