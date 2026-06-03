import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import schedule.app.Meeting;
import schedule.app.SchedulingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulingServiceTest {

    @Test
    public void testAddPersonAndGetScheduleEmpty() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");

        List<Meeting> schedule = service.getSchedule("alice@example.com");
        assertNotNull(schedule);
        assertTrue(schedule.isEmpty(), "New person should have an empty schedule");
    }

    @Test
    public void testAddPersonDuplicateEmailThrowsException() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");
        
        assertThrows(ResponseStatusException.class, () ->
                service.addPerson("Bob", "alice@example.com")
        );

    }

    @Test
    public void testGetScheduleForNonExistentPersonThrowsException() {
        SchedulingService service = new SchedulingService();

        assertThrows(IllegalArgumentException.class, () ->
                service.getSchedule("nonexistent@example.com")
        );
    }

    @Test
    public void testScheduleMeetingWithNonExistentAttendeeThrowsException() {
        SchedulingService service = new SchedulingService();

        assertThrows(IllegalArgumentException.class, () ->
                service.scheduleMeeting(LocalDateTime.of(2024, 6, 1, 9, 0), Set.of("alice@example.com", "nonexistent@example.com"))
        );
    }


    @Test
    public void testScheduleMeetingNotOnTheHourAttendeesThrowsException() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");

        assertThrows(IllegalArgumentException.class, () ->
                service.scheduleMeeting(LocalDateTime.of(2024, 6, 1, 9, 30), Set.of("alice@example.com"))
        );
    }

    @Test
    public void testScheduleMeetingAddsMeetingAndReturnsMeeting() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");
        service.addPerson("Bob", "bob@example.com");

        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 9, 0);
        Meeting meeting = service.scheduleMeeting(startTime, Set.of("alice@example.com", "bob@example.com"));

        assertNotNull(meeting);
        assertEquals(startTime, meeting.getStartTime());
        assertEquals(2, meeting.getAttendees().size());
        assertTrue(meeting.getAttendees().stream().anyMatch(p -> "alice@example.com".equals(p.getEmail())));
        assertTrue(meeting.getAttendees().stream().anyMatch(p -> "bob@example.com".equals(p.getEmail())));

        List<Meeting> aliceSchedule = service.getSchedule("alice@example.com");
        assertEquals(1, aliceSchedule.size());
        assertEquals(meeting.getId(), aliceSchedule.get(0).getId());
    }

    @Test
    public void testScheduleMeetingConflictThrowsException() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");
        service.addPerson("Bob", "bob@example.com");

        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 9, 0);
        service.scheduleMeeting(startTime, Set.of("alice@example.com"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.scheduleMeeting(startTime, Set.of("alice@example.com", "bob@example.com"))
        );

        assertTrue(exception.getMessage().contains("Scheduling conflict"));
    }

    @Test
    public void testGetMeetingReturnsCorrectMeeting() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");
        service.addPerson("Bob", "bob@example.com");

        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 9, 0);
        Meeting meeting = service.scheduleMeeting(startTime, Set.of("alice@example.com", "bob@example.com"));

        Meeting found = service.getMeeting(meeting.getId());
        assertNotNull(found);
        assertEquals(meeting.getId(), found.getId());
        assertEquals(startTime, found.getStartTime());
    }

    @Test
    public void testGetScheduleReturnsSortedMeetings() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");

        LocalDateTime startTime1 = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime startTime2 = LocalDateTime.of(2024, 6, 1, 9, 0);
        LocalDateTime startTime3 = LocalDateTime.of(2024, 5, 1, 11, 0);
        service.scheduleMeeting(startTime1, Set.of("alice@example.com"));
        service.scheduleMeeting(startTime2, Set.of("alice@example.com"));
        service.scheduleMeeting(startTime3, Set.of("alice@example.com"));

        List<Meeting> schedule = service.getSchedule("alice@example.com");
        assertEquals(3, schedule.size());
        assertEquals(startTime3, schedule.get(0).getStartTime());
        assertEquals(startTime2, schedule.get(1).getStartTime());
        assertEquals(startTime1, schedule.get(2).getStartTime());
    }

    @Test
    public void testSuggestSlotsReturnsAvailableSlots() {
        SchedulingService service = new SchedulingService();
        service.addPerson("Alice", "alice@example.com");
        service.addPerson("Bob", "bob@example.com");

        service.scheduleMeeting(
                LocalDateTime.of(2024, 6, 1, 10, 0),
                Set.of("alice@example.com")
        );

        List<LocalDateTime> availableSlots = service.suggestSlots(
                List.of("alice@example.com", "bob@example.com"),
                LocalDateTime.of(2024, 6, 1, 9, 0),
                LocalDateTime.of(2024, 6, 1, 12, 0)
        );

        assertTrue(availableSlots.contains(LocalDateTime.of(2024, 6, 1, 9, 0)));
        assertTrue(availableSlots.contains(LocalDateTime.of(2024, 6, 1, 11, 0)));
        assertTrue(availableSlots.contains(LocalDateTime.of(2024, 6, 1, 12, 0)));
        assertFalse(availableSlots.contains(LocalDateTime.of(2024, 6, 1, 10, 0)));
    }
}
