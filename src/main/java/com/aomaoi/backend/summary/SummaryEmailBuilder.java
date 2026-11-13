package com.aomaoi.backend.summary;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Renders the summary (Thai). All names and notes are HTML-escaped in the HTML body. */
@Component
public class SummaryEmailBuilder {

    private static final Locale THAI = Locale.forLanguageTag("th-TH");
    /** Buddhist-era year, matching what Thai users see in the app. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", THAI)
            .withChronology(ThaiBuddhistChronology.INSTANCE);

    public SummaryEmail build(DailySummary s) {
        String date = DATE.format(ThaiBuddhistDate.from(s.date()));
        String subject = "สรุปงานประจำวัน " + oneLine(s.farmName()) + " " + date;

        String top = s.topWorker() == null
                ? "ไม่มีการบันทึกงานในวันนี้"
                : s.topWorker().name() + " - ค่าแรง " + money(s.topWorker().totalWage()) + " บาท ("
                        + s.topWorker().jobCount() + " รายการ)";
        List<String> leaveLines = s.leaveWorkers().stream()
                .map(l -> l.note() == null || l.note().isBlank() ? l.name() : l.name() + " (" + l.note() + ")")
                .toList();
        String absentText = s.absentWorkerNames().isEmpty() ? "ไม่มี" : String.join(", ", s.absentWorkerNames());
        String leaveText = leaveLines.isEmpty() ? "ไม่มี" : String.join(", ", leaveLines);

        String text = "สรุปงานประจำวัน " + date + "\n"
                + "ฟาร์ม: " + s.farmName() + "\n\n"
                + "คนงานเด่นของวัน: " + top + "\n"
                + "มาทำงาน: " + s.presentCount() + " จาก " + s.checkedWorkerCount() + " คน\n"
                + "ขาด (" + s.absentWorkerNames().size() + " คน): " + absentText + "\n"
                + "ลา (" + s.leaveWorkers().size() + " คน): " + leaveText + "\n"
                + "ค่าแรงรวมของวัน: " + money(s.totalWage()) + " บาท\n";

        String html = "<div style=\"font-family:sans-serif;line-height:1.5\">"
                + "<h2 style=\"margin:0 0 4px\">สรุปงานประจำวัน " + esc(date) + "</h2>"
                + "<p style=\"margin:0 0 16px;color:#555\">ฟาร์ม: " + esc(s.farmName()) + "</p>"
                + "<p><strong>คนงานเด่นของวัน:</strong> " + esc(top) + "</p>"
                + "<p><strong>มาทำงาน:</strong> " + s.presentCount() + " จาก " + s.checkedWorkerCount() + " คน</p>"
                + "<div><strong>ขาด (" + s.absentWorkerNames().size() + " คน):</strong> "
                + (s.absentWorkerNames().isEmpty() ? esc(absentText) : listItems(s.absentWorkerNames())) + "</div>"
                + "<div><strong>ลา (" + s.leaveWorkers().size() + " คน):</strong> "
                + (leaveLines.isEmpty() ? esc(leaveText) : listItems(leaveLines)) + "</div>"
                + "<p><strong>ค่าแรงรวมของวัน:</strong> " + esc(money(s.totalWage())) + " บาท</p>"
                + "</div>";
        return new SummaryEmail(subject, text, html);
    }

    private static String listItems(List<String> items) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:4px 0\">");
        items.forEach(item -> sb.append("<li>").append(esc(item)).append("</li>"));
        return sb.append("</ul>").toString();
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(THAI);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    /** Header values must not contain line breaks. */
    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
