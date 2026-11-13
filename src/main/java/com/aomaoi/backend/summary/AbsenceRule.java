package com.aomaoi.backend.summary;

import com.aomaoi.backend.entity.Worker;

import java.time.LocalDate;
import java.util.List;

/**
 * Decides who counts as absent on a day. Kept as an interface so the current "no work log" rule
 * can later be replaced by a real check-in based one without touching the summary or the e-mail.
 */
public interface AbsenceRule {
    List<Worker> absentOn(Long farmId, LocalDate date);
}
