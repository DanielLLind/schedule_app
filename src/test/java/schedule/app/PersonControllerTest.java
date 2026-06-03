package schedule.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PersonControllerTest {

    private SchedulingService schedulingService;
    private PersonController controller;

    @BeforeEach
    void setUp() {
        schedulingService = new SchedulingService();
        controller = new PersonController(schedulingService);
    }

    @Test
    void addPersonReturnsCreated() {
        PersonDTO request = new PersonDTO("Alice", "alice@example.com");

        ResponseEntity<Person> response = controller.addPerson(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Alice", response.getBody().getName());
        assertEquals("alice@example.com", response.getBody().getEmail());
    }

    @Test
    void getPersonReturnsOkWhenFound() {
        schedulingService.addPerson("Alice", "alice@example.com");

        ResponseEntity<Person> response = controller.getPerson("alice@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("alice@example.com", response.getBody().getEmail());
    }

    @Test
    void getPersonReturnsNotFoundWhenMissing() {
        ResponseEntity<Person> response = controller.getPerson("missing@example.com");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getScheduleReturnsEmptyListForPersonWithNoMeetings() {
        schedulingService.addPerson("Alice", "alice@example.com");

        ResponseEntity<List<Meeting>> response = controller.getSchedule("alice@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
