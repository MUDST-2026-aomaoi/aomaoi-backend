package com.aomaoi.backend.summary;

/** Rendered e-mail, independent of how it is delivered. */
public record SummaryEmail(String subject, String textBody, String htmlBody) { }
