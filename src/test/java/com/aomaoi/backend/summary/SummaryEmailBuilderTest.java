package com.aomaoi.backend.summary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryEmailBuilderTest {

    private final SummaryEmailBuilder builder = new SummaryEmailBuilder();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Test
    void includesTheTopPerformerTheAbsentNamesAndTheFarmInTheSubject() {
        DailySummary summary = new DailySummary("ฟาร์มเพื่อพ่อ", DAY,
                new DailySummary.TopWorker("สมชาย ใจกล้า", new BigDecimal("1500"), 2),
                List.of("สมหญิง ขยัน", "มาลี ใจดี"), 5, new BigDecimal("2100"));

        SummaryEmail email = builder.build(summary);

        assertTrue(email.subject().contains("ฟาร์มเพื่อพ่อ"));
        assertTrue(email.subject().contains("2569"), "Thai Buddhist year expected: " + email.subject());
        assertTrue(email.textBody().contains("สมชาย ใจกล้า"));
        assertTrue(email.textBody().contains("สมหญิง ขยัน, มาลี ใจดี"));
        assertTrue(email.textBody().contains("2 จาก 5"));
        assertTrue(email.htmlBody().contains("<li>สมหญิง ขยัน</li>"));
    }

    @Test
    void saysSoWhenNobodyLoggedWorkAndNobodyIsAbsent() {
        DailySummary none = new DailySummary("ฟาร์ม", DAY, null, List.of(), 3, BigDecimal.ZERO);

        SummaryEmail email = builder.build(none);

        assertTrue(email.textBody().contains("ไม่มีการบันทึกงานในวันนี้"));
        assertTrue(email.textBody().contains("ไม่มี (ทุกคนมีบันทึกงาน)"));
    }

    @Test
    void escapesHtmlInNamesAndKeepsLineBreaksOutOfTheSubject() {
        DailySummary hostile = new DailySummary("Farm\r\nBcc: evil@example.com", DAY,
                new DailySummary.TopWorker("<script>alert(1)</script>", BigDecimal.TEN, 1),
                List.of("A&B <b>"), 2, BigDecimal.TEN);

        SummaryEmail email = builder.build(hostile);

        assertFalse(email.subject().contains("\r"));
        assertFalse(email.subject().contains("\n"));
        assertFalse(email.htmlBody().contains("<script>"));
        assertTrue(email.htmlBody().contains("&lt;script&gt;"));
        assertTrue(email.htmlBody().contains("A&amp;B &lt;b&gt;"));
    }
}
