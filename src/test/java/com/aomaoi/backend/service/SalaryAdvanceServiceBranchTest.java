package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.SalaryAdvanceRequestDTO;
import com.aomaoi.backend.entity.AdvanceType;
import com.aomaoi.backend.entity.SalaryAdvance;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.SalaryAdvanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Fills the branch gaps left by {@link SalaryAdvanceServiceTest}. */
@ExtendWith(MockitoExtension.class)
class SalaryAdvanceServiceBranchTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 27);

    @Mock
    SalaryAdvanceRepository advances;
    @Mock
    FinancialAccessService access;
    @Mock
    AuditLogService audit;
    @InjectMocks
    SalaryAdvanceService service;

    private Worker worker(boolean withUser) {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setFarmId(1L);
        worker.setFullName("สมชาย");
        if (withUser) {
            User user = new User();
            user.setUsername("worker1");
            worker.setUser(user);
        }
        return worker;
    }

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken("admin1", "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private SalaryAdvance advance(Worker worker) {
        SalaryAdvance a = new SalaryAdvance();
        a.setId(5L);
        a.setWorker(worker);
        a.setAdvanceDate(DAY);
        a.setType(AdvanceType.CASH);
        a.setAmount(new BigDecimal("10.00"));
        a.setRecordedBy("admin1");
        a.setCreatedAt(Instant.now());
        return a;
    }

    private void assertCreateRejected(SalaryAdvanceRequestDTO request) {
        when(access.requireManager()).thenReturn(admin());
        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(advances, never()).save(any());
    }

    // ---------- create: required fields ----------

    @Test
    void create_nullRequest_rejected() {
        assertCreateRejected(null);
    }

    @Test
    void create_nullWorkerId_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(null, DAY, AdvanceType.CASH, BigDecimal.TEN, null));
    }

    @Test
    void create_nonPositiveWorkerId_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(0L, DAY, AdvanceType.CASH, BigDecimal.TEN, null));
    }

    @Test
    void create_nullDate_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(1L, null, AdvanceType.CASH, BigDecimal.TEN, null));
    }

    @Test
    void create_nullType_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(1L, DAY, null, BigDecimal.TEN, null));
    }

    @Test
    void create_nullAmount_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(1L, DAY, AdvanceType.CASH, null, null));
    }

    @Test
    void create_goodsWithBlankItemName_rejected() {
        assertCreateRejected(new SalaryAdvanceRequestDTO(1L, DAY, AdvanceType.GOODS, BigDecimal.TEN, "  "));
    }

    @Test
    void create_cashWithBlankItemName_isAccepted() {
        when(access.requireManager()).thenReturn(admin());
        when(access.requireManagedWorker(1L)).thenReturn(worker(true));
        when(advances.save(any(SalaryAdvance.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.create(new SalaryAdvanceRequestDTO(1L, DAY, AdvanceType.CASH, BigDecimal.TEN, "  "));

        assertNull(result.itemName());
    }

    // ---------- listForManager ----------

    @Test
    void listForManager_superadmin_readsAllFarms() {
        when(access.managerFarmIdOrNull()).thenReturn(null);
        when(advances.findAllByOrderByAdvanceDateDescIdDesc(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(advance(worker(true)))));

        var result = service.listForManager(0, 10);

        assertEquals(1, result.size());
        assertEquals("worker1", worker(true).getUsername());
        verify(advances, never()).findByWorkerFarmIdOrderByAdvanceDateDescIdDesc(any(), any());
    }

    @Test
    void listForManager_invalidPaging_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.listForManager(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.listForManager(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.listForManager(0, 501));
        verifyNoInteractions(access, advances);
    }

    @Test
    void listForManager_maxPageSizeAccepted() {
        when(access.managerFarmIdOrNull()).thenReturn(1L);
        when(advances.findByWorkerFarmIdOrderByAdvanceDateDescIdDesc(1L, PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.listForManager(0, 500).isEmpty());
    }

    // ---------- listSelf ----------

    @Test
    void listSelf_returnsOnlyOwnActiveAdvances() {
        Worker self = worker(true);
        when(access.requireSelf()).thenReturn(self);
        when(advances.findByWorkerIdAndCancelledAtIsNullOrderByAdvanceDateDescIdDesc(1L))
                .thenReturn(List.of(advance(self)));

        var result = service.listSelf();

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
    }

    // ---------- cancel ----------

    @Test
    void cancel_unknownAdvance_throws() {
        when(access.requireManager()).thenReturn(admin());
        when(advances.findLockedById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.cancel(99L));
    }

    @Test
    void cancel_workerWithoutUserAccount_auditsAgainstWorkerId() {
        Worker noUser = worker(false);
        SalaryAdvance advance = advance(noUser);
        when(access.requireManager()).thenReturn(admin());
        when(advances.findLockedById(5L)).thenReturn(Optional.of(advance));
        when(advances.save(advance)).thenReturn(advance);

        service.cancel(5L);

        verify(audit).logAction("CANCEL_SALARY_ADVANCE", "admin1", "worker:1", "Cancelled salary advance #5");
    }
}
