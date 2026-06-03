package schedule.app;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/persons")
public class PersonController {

    private final SchedulingService schedulingService;

    public PersonController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping
    public ResponseEntity<Person> addPerson(
            @RequestBody PersonDTO request) {

        Person person = schedulingService.addPerson(
                request.name(),
                request.email());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(person);
    }

    @GetMapping("/{email}")
    public ResponseEntity<Person> getPerson(@PathVariable String email) {
        Person person = schedulingService.getPerson(email);
        if (person == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(person);
    }

    @GetMapping("/{email}/schedule")
    public ResponseEntity<List<Meeting>> getSchedule(@PathVariable String email) {
        List<Meeting> schedule = schedulingService.getSchedule(email);
        return ResponseEntity.ok(schedule);
    }
}
