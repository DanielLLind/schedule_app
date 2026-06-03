package schedule.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MeetingControllerTest {

    private SchedulingService schedulingService;
    private MeetingController controller;

    @BeforeEach
    void setUp() {
        schedulingService = new SchedulingService();
        controller = new MeetingController(schedulingService);

        schedulingService.addPerson("Alice", "alice@example.com");
        schedulingService.addPerson("Bob", "bob@example.com");
    }

    @Test
    void createMeetingReturnsCreated() {
        MeetingDTO request = new MeetingDTO("2024-06-01T09:00", Set.of("alice@example.com", "bob@example.com"));

        ResponseEntity<Meeting> response = controller.createMeeting(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LocalDateTime.parse("2024-06-01T09:00"), response.getBody().getStartTime());
        assertEquals(2, response.getBody().getAttendees().size());
    }

    @Test
    void getMeetingReturnsOkWhenFound() {
        Meeting meeting = schedulingService.scheduleMeeting(
                LocalDateTime.parse("2024-06-01T09:00"),
                Set.of("alice@example.com", "bob@example.com")
        );

        ResponseEntity<Meeting> response = controller.getMeeting(meeting.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(meeting.getId(), response.getBody().getId());
    }

    @Test
    void getMeetingReturnsNotFoundWhenMissing() {
        UUID missingId = UUID.randomUUID();

        ResponseEntity<Meeting> response = controller.getMeeting(missingId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void suggestMeetingSlotReturnsAvailableSlots() {
        schedulingService.scheduleMeeting(
                LocalDateTime.parse("2024-06-01T09:00"),
                Set.of("alice@example.com")
        );

        SuggestSlotDTO request = new SuggestSlotDTO(
                List.of("alice@example.com", "bob@example.com"),
                "2024-06-01T09:00",
                "2024-06-01T12:00"
        );

        ResponseEntity<List<LocalDateTime>> response = controller.suggestMeetingSlot(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(LocalDateTime.parse("2024-06-01T10:00")));
    }
}
