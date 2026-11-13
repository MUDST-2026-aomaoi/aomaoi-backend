package com.aomaoi.backend.summary;

/** Delivery channel for the summary. Swappable (tests use a mock; production uses SMTP). */
public interface SummaryMailer {
    /** False when no SMTP server is configured; callers then skip sending instead of failing. */
    boolean isConfigured();

    /** @throws RuntimeException when delivery fails (the caller records the failure and retries later) */
    void send(String to, SummaryEmail email);
}
