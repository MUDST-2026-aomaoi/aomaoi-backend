package com.aomaoi.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.SalaryAdvance;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SalaryAdvanceServiceTest {
    @Mock SalaryAdvanceRepository advances;
    @Mock FinancialAccessService access;
    @Mock AuditLogService audit;
    @InjectMocks SalaryAdvanceService service;

    @Test
    void recordsGoodsAdvanceWithAuditableValue() {
        Worker worker = worker();
        when(access.requireManager()).thenReturn(admin());
        when(access.requireManagedWorker(1L)).thenReturn(worker);
        when(advances.save(any(SalaryAdvance.class))).thenAnswer(call -> {
            SalaryAdvance saved = call.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        var result = service.create(new SalaryAdvanceRequestDTO(1L, LocalDate.of(2026, 10, 27),
                AdvanceType.GOODS, new BigDecimal("125.50"), "ปุ๋ย"));

        assertEquals(8L, result.id());
        assertEquals(new BigDecimal("125.50"), result.amount());
        assertEquals("ปุ๋ย", result.itemName());
        assertEquals("admin1", result.recordedBy());
        assertNotNull(result.createdAt());
        verify(audit).logAction("CREATE_SALARY_ADVANCE", "admin1", "worker1",
                "Recorded GOODS advance #8");
    }

    @Test
    void rejectsGoodsWithoutItemAndDoesNotSave() {
        when(access.requireManager()).thenReturn(admin());
        assertThrows(IllegalArgumentException.class, () -> service.create(new SalaryAdvanceRequestDTO(
                1L, LocalDate.now(), AdvanceType.GOODS, new BigDecimal("20.00"), null)));
        verify(advances, never()).save(any());
    }

    @Test
    void rejectsCashWithItemAndNonPositiveAmount() {
        when(access.requireManager()).thenReturn(admin());
        assertThrows(IllegalArgumentException.class, () -> service.create(new SalaryAdvanceRequestDTO(
                1L, LocalDate.now(), AdvanceType.CASH, new BigDecimal("20.00"), "ปุ๋ย")));
        assertThrows(IllegalArgumentException.class, () -> service.create(new SalaryAdvanceRequestDTO(
                1L, LocalDate.now(), AdvanceType.CASH, BigDecimal.ZERO, null)));
        verify(advances, never()).save(any());
    }

    @Test
    void rejectsAdvanceForWorkerWithoutFarm() {
        Worker worker = worker();
        worker.setFarmId(null);
        when(access.requireManager()).thenReturn(admin());
        when(access.requireManagedWorker(1L)).thenReturn(worker);

        assertThrows(IllegalArgumentException.class, () -> service.create(new SalaryAdvanceRequestDTO(
                1L, LocalDate.now(), AdvanceType.CASH, new BigDecimal("20.00"), null)));
        verify(advances, never()).save(any());
    }

    @Test
    void cancellingKeepsAuditRecordAndMarksAdvanceInactiveForTotals() {
        Worker worker = worker();
        SalaryAdvance advance = new SalaryAdvance();
        advance.setId(8L);
        advance.setWorker(worker);
        advance.setAdvanceDate(LocalDate.of(2026, 10, 27));
        advance.setType(AdvanceType.CASH);
        advance.setAmount(new BigDecimal("100.00"));
        advance.setRecordedBy("admin1");
        advance.setCreatedAt(Instant.now());
        when(access.requireManager()).thenReturn(admin());
        when(access.requireManagedWorker(1L)).thenReturn(worker);
        when(advances.findLockedById(8L)).thenReturn(Optional.of(advance));
        when(advances.save(advance)).thenReturn(advance);

        var result = service.cancel(8L);

        assertNotNull(result.cancelledAt());
        assertEquals("admin1", result.cancelledBy());
        verify(audit).logAction("CANCEL_SALARY_ADVANCE", "admin1", "worker1",
                "Cancelled salary advance #8");
    }

    @Test
    void repeatedCancellationDoesNotRewriteHistory() {
        Worker worker = worker();
        SalaryAdvance advance = new SalaryAdvance();
        advance.setId(8L);
        advance.setWorker(worker);
        advance.setCancelledAt(Instant.now());
        advance.setCancelledBy("admin1");
        when(access.requireManager()).thenReturn(admin());
        when(access.requireManagedWorker(1L)).thenReturn(worker);
        when(advances.findLockedById(8L)).thenReturn(Optional.of(advance));

        assertThrows(ResponseStatusException.class, () -> service.cancel(8L));
        verify(advances, never()).save(any());
    }

    @Test
    void listUsesFarmScopedPagination() {
        when(access.managerFarmIdOrNull()).thenReturn(7L);
        when(advances.findByWorkerFarmIdOrderByAdvanceDateDescIdDesc(7L, PageRequest.of(1, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        assertEquals(List.of(), service.listForManager(1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.listForManager(0, 501));
    }

    private Worker worker() {
        User user = new User();
        user.setUsername("worker1");
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setFarmId(1L);
        worker.setFullName("สมชาย");
        worker.setNickname("ชาย");
        worker.setUser(user);
        return worker;
    }

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken("admin1", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
