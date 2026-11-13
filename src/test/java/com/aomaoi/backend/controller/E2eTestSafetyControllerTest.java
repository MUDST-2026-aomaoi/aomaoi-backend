package com.aomaoi.backend.controller;

import com.aomaoi.backend.entity.AdminProfile;
import com.aomaoi.backend.repository.AdminProfileRepository;
import com.aomaoi.backend.service.AttendanceCloseDayService;
import com.aomaoi.backend.service.AttendanceService;
import com.aomaoi.backend.service.E2eRunService;
import com.aomaoi.backend.summary.DailySummaryScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class E2eTestSafetyControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T11:00:00Z"), ZoneId.of("Asia/Bangkok"));

    private static E2eTestSafetyController controllerFor(E2eRunService service) {
        return new E2eTestSafetyController(service, mock(DailySummaryScheduler.class), mock(AdminProfileRepository.class), CLOCK,
                mock(AttendanceCloseDayService.class), mock(AttendanceService.class));
    }

    @Test
    void readinessUsesExplicitSupportConfiguration() {
        var service = mock(E2eRunService.class);
        var controller = controllerFor(service);
        assertEquals(false, controller.readiness().get("safeForE2e"));
        when(service.enabled()).thenReturn(true);
        assertEquals(true, controller.readiness().get("safeForE2e"));
        assertEquals("shared-tracked", controller.readiness().get("mode"));
        assertEquals(false, controller.readiness().get("attendanceOpen"), "a mock reports closed");
    }

    @Test
    void create_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = controllerFor(service);
        when(service.create("test-key")).thenReturn(Map.of("id", "run-1", "token", "tok-1"));

        var result = controller.create("test-key");

        assertEquals("run-1", result.get("id"));
        assertEquals("tok-1", result.get("token"));
        verify(service).create("test-key");
    }

    @Test
    void heartbeat_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = controllerFor(service);
        doNothing().when(service).heartbeat("run-1", "tok-1");

        var result = controller.heartbeat("run-1", "tok-1");

        assertTrue(result.get("alive"));
        verify(service).heartbeat("run-1", "tok-1");
    }

    @Test
    void cleanup_DelegatesToService() {
        var service = mock(E2eRunService.class);
        var controller = controllerFor(service);
        when(service.cleanup("run-1", "test-key")).thenReturn(Map.of("deleted", 5, "remaining", 0));

        var result = controller.cleanup("run-1", "test-key");

        assertEquals(5, result.get("deleted"));
        assertEquals(0, result.get("remaining"));
        verify(service).cleanup("run-1", "test-key");
    }

    @Test
    void dailySummaryRun_requiresTheSupportKeyAndRunsForOneAdminOnTheGivenDate() {
        var service = mock(E2eRunService.class);
        var scheduler = mock(DailySummaryScheduler.class);
        var admins = mock(AdminProfileRepository.class);
        var admin = new AdminProfile();
        admin.setId(7L);
        when(admins.findById(7L)).thenReturn(Optional.of(admin));
        when(scheduler.sendTo(admin, LocalDate.of(2026, 11, 1))).thenReturn(DailySummaryScheduler.Outcome.SENT);
        var controller = new E2eTestSafetyController(service, scheduler, admins, CLOCK, mock(AttendanceCloseDayService.class), mock(AttendanceService.class));

        var result = controller.runDailySummary("test-key", 7L, LocalDate.of(2026, 11, 1));

        assertEquals("SENT", result.get("outcome"));
        verify(service).authorize("test-key");
        verify(scheduler).sendTo(admin, LocalDate.of(2026, 11, 1));
    }

    @Test
    void dailySummaryRun_defaultsToTodayInBangkok() {
        var service = mock(E2eRunService.class);
        var scheduler = mock(DailySummaryScheduler.class);
        var admins = mock(AdminProfileRepository.class);
        var admin = new AdminProfile();
        when(admins.findById(7L)).thenReturn(Optional.of(admin));
        when(scheduler.sendTo(admin, LocalDate.of(2026, 11, 12))).thenReturn(DailySummaryScheduler.Outcome.ALREADY_HANDLED);
        var controller = new E2eTestSafetyController(service, scheduler, admins, CLOCK, mock(AttendanceCloseDayService.class), mock(AttendanceService.class));

        assertEquals("ALREADY_HANDLED", controller.runDailySummary("test-key", 7L, null).get("outcome"));
    }

    @Test
    void dailySummaryRun_rejectsAnInvalidKeyBeforeTouchingAnything() {
        var service = mock(E2eRunService.class);
        var scheduler = mock(DailySummaryScheduler.class);
        var admins = mock(AdminProfileRepository.class);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(service).authorize("bad");
        var controller = new E2eTestSafetyController(service, scheduler, admins, CLOCK, mock(AttendanceCloseDayService.class), mock(AttendanceService.class));

        assertThrows(ResponseStatusException.class, () -> controller.runDailySummary("bad", 7L, null));
        verifyNoInteractions(scheduler, admins);
    }

    @Test
    void dailySummaryRun_unknownAdminIsNotFound() {
        var service = mock(E2eRunService.class);
        var admins = mock(AdminProfileRepository.class);
        when(admins.findById(9L)).thenReturn(Optional.empty());
        var controller = new E2eTestSafetyController(service, mock(DailySummaryScheduler.class), admins, CLOCK, mock(AttendanceCloseDayService.class), mock(AttendanceService.class));

        var error = assertThrows(ResponseStatusException.class, () -> controller.runDailySummary("k", 9L, null));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void closeDay_requiresTheSupportKeyAndClosesOneFarmOnTheGivenDate() {
        var service = mock(E2eRunService.class);
        var closeDay = mock(AttendanceCloseDayService.class);
        when(closeDay.closeFarm(3L, LocalDate.of(2026, 11, 1))).thenReturn(4);
        var controller = new E2eTestSafetyController(service, mock(DailySummaryScheduler.class),
                mock(AdminProfileRepository.class), CLOCK, closeDay, mock(AttendanceService.class));

        assertEquals(4, controller.closeDay("test-key", 3L, LocalDate.of(2026, 11, 1)).get("created"));
        verify(service).authorize("test-key");
    }

    @Test
    void closeDay_defaultsToTodayInBangkokAndRejectsAnInvalidKey() {
        var service = mock(E2eRunService.class);
        var closeDay = mock(AttendanceCloseDayService.class);
        when(closeDay.closeFarm(3L, LocalDate.of(2026, 11, 12))).thenReturn(0);
        var controller = new E2eTestSafetyController(service, mock(DailySummaryScheduler.class),
                mock(AdminProfileRepository.class), CLOCK, closeDay, mock(AttendanceService.class));
        assertEquals(0, controller.closeDay("k", 3L, null).get("created"));

        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(service).authorize("bad");
        assertThrows(ResponseStatusException.class, () -> controller.closeDay("bad", 3L, null));
        verify(closeDay, times(1)).closeFarm(anyLong(), any());
    }
}
