package com.aomaoi.backend.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E2eTestSafetyControllerTest {

    @Test
    void permitsOnlyTheLocalDisposableE2eDatabase() {
        assertTrue(E2eTestSafetyController.isDisposableLocalDatabase(
                "jdbc:postgresql://localhost:5432/aomaoi_test"));
        assertTrue(E2eTestSafetyController.isDisposableLocalDatabase(
                "jdbc:postgresql://127.0.0.1:5432/aomaoi_test?sslmode=require"));
        assertFalse(E2eTestSafetyController.isDisposableLocalDatabase(
                "jdbc:postgresql://localhost:5432/postgres"));
        assertFalse(E2eTestSafetyController.isDisposableLocalDatabase(
                "jdbc:postgresql://db.example.com:5432/postgres"));
    }
}
