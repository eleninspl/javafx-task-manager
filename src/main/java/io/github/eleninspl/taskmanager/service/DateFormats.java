package io.github.eleninspl.taskmanager.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

// One place for how dates read in the interface, so every screen and message agrees
public final class DateFormats {

    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private DateFormats() {}

    // "Tue 6 Oct 2026"
    public static String full(LocalDate date) {
        return date == null ? "" : date.format(FULL);
    }

    // "Today", "Tomorrow", "Yesterday", "Fri 9 Oct", or the full date when it is not this year
    public static String relative(LocalDate date) {
        if (date == null) return "";
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, date);
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        if (days == -1) return "Yesterday";
        return date.getYear() == today.getYear() ? date.format(SHORT) : date.format(FULL);
    }

    // "1 day", "3 days"
    public static String days(long n) {
        return n + (n == 1 ? " day" : " days");
    }
}
