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

/** Renders the summary (Thai). All names are HTML-escaped in the HTML body. */
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
        String absent = s.absentWorkerNames().isEmpty()
                ? "ไม่มี (ทุกคนมีบันทึกงาน)"
                : String.join(", ", s.absentWorkerNames());

        String text = "สรุปงานประจำวัน " + date + "\n"
                + "ฟาร์ม: " + s.farmName() + "\n\n"
                + "คนงานเด่นของวัน: " + top + "\n"
                + "คนงานที่ไม่มีบันทึกงาน (" + s.absentWorkerNames().size() + " จาก " + s.activeWorkerCount()
                + " คน): " + absent + "\n"
                + "ค่าแรงรวมของวัน: " + money(s.totalWage()) + " บาท\n";

        String html = "<div style=\"font-family:sans-serif;line-height:1.5\">"
                + "<h2 style=\"margin:0 0 4px\">สรุปงานประจำวัน " + esc(date) + "</h2>"
                + "<p style=\"margin:0 0 16px;color:#555\">ฟาร์ม: " + esc(s.farmName()) + "</p>"
                + "<p><strong>คนงานเด่นของวัน:</strong> " + esc(top) + "</p>"
                + "<p><strong>คนงานที่ไม่มีบันทึกงาน (" + s.absentWorkerNames().size() + " จาก "
                + s.activeWorkerCount() + " คน):</strong> "
                + (s.absentWorkerNames().isEmpty() ? esc(absent) : listItems(s.absentWorkerNames())) + "</p>"
                + "<p><strong>ค่าแรงรวมของวัน:</strong> " + esc(money(s.totalWage())) + " บาท</p>"
                + "</div>";
        return new SummaryEmail(subject, text, html);
    }

    private static String listItems(List<String> names) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:4px 0\">");
        names.forEach(name -> sb.append("<li>").append(esc(name)).append("</li>"));
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
