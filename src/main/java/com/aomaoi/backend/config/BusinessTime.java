package com.aomaoi.backend.config;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "What day is it" for the farm: the business time zone (Asia/Bangkok by default), not the server's.
 * Containers usually run in UTC, so {@code LocalDate.now()} would still be yesterday between 00:00 and 07:00
 * Thai time. Anything that stamps or compares a calendar day must use this (or the injected Clock, which is
 * built from the same zone).
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of(System.getenv().getOrDefault("APP_BUSINESS_ZONE", "Asia/Bangkok"));

    private BusinessTime() { }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
