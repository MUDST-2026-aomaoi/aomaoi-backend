package com.aomaoi.backend.summary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryEmailBuilderTest {

    private final SummaryEmailBuilder builder = new SummaryEmailBuilder();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    private static DailySummary summary(String farm, DailySummary.TopWorker top, List<String> absent,
                                        List<DailySummary.LeaveWorker> leave, int present, int checked) {
        return new DailySummary(farm, DAY, top, absent, leave, present, checked, new BigDecimal("2100"));
    }

    @Test
    void includesTopPerformerAbsentAndLeaveWithNotesAndTheFarmInTheSubject() {
        SummaryEmail email = builder.build(summary("ฟาร์มเพื่อพ่อ",
                new DailySummary.TopWorker("สมชาย ใจกล้า", new BigDecimal("1500"), 2),
                List.of("สมหญิง ขยัน", "มาลี ใจดี"),
                List.of(new DailySummary.LeaveWorker("วิชัย รักงาน", "ป่วย โทรบอกเช้านี้")), 2, 5));

        assertTrue(email.subject().contains("ฟาร์มเพื่อพ่อ"));
        assertTrue(email.subject().contains("2569"), "Thai Buddhist year expected: " + email.subject());
        assertTrue(email.textBody().contains("สมชาย ใจกล้า"));
        assertTrue(email.textBody().contains("มาทำงาน: 2 จาก 5 คน"));
        assertTrue(email.textBody().contains("ขาด (2 คน): สมหญิง ขยัน, มาลี ใจดี"));
        assertTrue(email.textBody().contains("ลา (1 คน): วิชัย รักงาน (ป่วย โทรบอกเช้านี้)"));
        assertTrue(email.htmlBody().contains("<li>สมหญิง ขยัน</li>"));
        assertTrue(email.htmlBody().contains("<li>วิชัย รักงาน (ป่วย โทรบอกเช้านี้)</li>"));
    }

    @Test
    void leaveWithoutANoteShowsOnlyTheName() {
        SummaryEmail email = builder.build(summary("ฟาร์ม", null, List.of(),
                List.of(new DailySummary.LeaveWorker("วิชัย", null)), 1, 2));

        assertTrue(email.textBody().contains("ลา (1 คน): วิชัย\n"));
    }

    @Test
    void saysSoWhenNobodyLoggedWorkAndNobodyIsAbsentOrOnLeave() {
        SummaryEmail email = builder.build(summary("ฟาร์ม", null, List.of(), List.of(), 3, 3));

        assertTrue(email.textBody().contains("ไม่มีการบันทึกงานในวันนี้"));
        assertTrue(email.textBody().contains("ขาด (0 คน): ไม่มี"));
        assertTrue(email.textBody().contains("ลา (0 คน): ไม่มี"));
    }

    @Test
    void escapesHtmlInNamesAndNotesAndKeepsLineBreaksOutOfTheSubject() {
        SummaryEmail email = builder.build(summary("Farm\r\nBcc: evil@example.com",
                new DailySummary.TopWorker("<script>alert(1)</script>", BigDecimal.TEN, 1),
                List.of("A&B <b>"),
                List.of(new DailySummary.LeaveWorker("C", "<img src=x onerror=alert(1)>")), 1, 3));

        assertFalse(email.subject().contains("\r"));
        assertFalse(email.subject().contains("\n"));
        assertFalse(email.htmlBody().contains("<script>"));
        assertFalse(email.htmlBody().contains("<img"));
        assertTrue(email.htmlBody().contains("&lt;script&gt;"));
        assertTrue(email.htmlBody().contains("A&amp;B &lt;b&gt;"));
        assertTrue(email.htmlBody().contains("&lt;img src=x onerror=alert(1)&gt;"));
    }
}
