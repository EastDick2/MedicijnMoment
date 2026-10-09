import nl.ardio.medicijnmoment.Schedule;
import java.time.*;
import java.util.*;

public class ScheduleTest {
    static int checks=0;
    static void eq(Object expected,Object actual) {
        checks++;
        if(!expected.equals(actual)) throw new AssertionError("Expected "+expected+", got "+actual);
    }
    static long instant(String s) { return Instant.parse(s).toEpochMilli(); }
    public static void main(String[] args) {
        ZoneId amsterdam=ZoneId.of("Europe/Amsterdam");
        eq(List.of("08:00","20:30"),Schedule.parseTimes("20:30, 08:00,08:00"));
        for(String bad:List.of("","8:00","24:00","12:60","-1:00","08:00,","abc")) {
            try { Schedule.parseTimes(bad); throw new AssertionError("Accepted "+bad); }
            catch(IllegalArgumentException expected) { checks++; }
        }
        eq(instant("2026-10-09T06:00:00Z"),Schedule.next("08:00",instant("2026-10-09T05:59:00Z"),amsterdam));
        eq(instant("2026-10-10T06:00:00Z"),Schedule.next("08:00",instant("2026-10-09T06:00:00Z"),amsterdam));
        eq(instant("2026-10-10T06:00:00Z"),Schedule.next("08:00",instant("2026-10-09T06:01:00Z"),amsterdam));
        // Daily times remain at 08:00 local across DST; never add a fixed 24 hours.
        eq(instant("2026-03-29T06:00:00Z"),Schedule.next("08:00",instant("2026-03-28T08:00:00Z"),amsterdam));
        eq(instant("2026-10-25T07:00:00Z"),Schedule.next("08:00",instant("2026-10-24T08:00:00Z"),amsterdam));
        // Nonexistent spring 02:30 shifts forward; autumn overlap fires only once (earlier offset).
        eq(instant("2026-03-29T01:30:00Z"),Schedule.next("02:30",instant("2026-03-28T08:00:00Z"),amsterdam));
        eq(instant("2026-10-25T00:30:00Z"),Schedule.next("02:30",instant("2026-10-24T08:00:00Z"),amsterdam));
        eq(instant("2026-10-26T01:30:00Z"),Schedule.next("02:30",instant("2026-10-25T00:30:00Z"),amsterdam));
        eq(instant("2026-10-09T22:00:00Z"),Schedule.next("00:00",instant("2026-10-09T21:59:00Z"),amsterdam));
        eq(instant("2026-10-09T08:00:00Z"),Schedule.next("08:00",instant("2026-10-09T07:00:00Z"),ZoneId.of("UTC")));
        System.out.println(checks+" schedule checks passed.");
    }
}
