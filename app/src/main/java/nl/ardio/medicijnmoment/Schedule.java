package nl.ardio.medicijnmoment;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Wall-clock daily times in the current phone timezone. One occurrence per local date. */
public final class Schedule {
    private Schedule() {}
    public static List<String> parseTimes(String raw) {
        TreeSet<String> result = new TreeSet<>();
        for (String part : raw.split(",", -1)) {
            String value = part.trim();
            if (!value.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))
                throw new IllegalArgumentException("Gebruik tijden als 08:00, 13:00, 20:30.");
            result.add(value);
        }
        return new ArrayList<>(result);
    }
    public static long onDate(String time, LocalDate date, ZoneId zone) {
        return date.atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli();
    }
    public static long next(String time, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        long candidate = onDate(time, today, zone);
        return candidate > now ? candidate : onDate(time, today.plusDays(1), zone);
    }
    public static String date(long instant) {
        return Instant.ofEpochMilli(instant).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }
    public static String display(long instant) {
        return Instant.ofEpochMilli(instant).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd-MM HH:mm"));
    }
}
