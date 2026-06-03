package schedule.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meetings")
public class MeetingController {

    private final SchedulingService schedulingService;

    public MeetingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping
    public ResponseEntity<Meeting> createMeeting(
            @RequestBody MeetingDTO request) {
        
        LocalDateTime startTime = LocalDateTime.parse(request.startTime());
        Meeting meeting = schedulingService.scheduleMeeting(
                startTime,
                request.participantEmails());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meeting);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeeting(@PathVariable UUID id) {
        Meeting meeting = schedulingService.getMeeting(id);
        if (meeting == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meeting);
    }

    @PostMapping("/suggest")
    public ResponseEntity<List<LocalDateTime>> suggestMeetingSlot(
            @RequestBody SuggestSlotDTO request) {

        LocalDateTime from = LocalDateTime.parse(request.from());
        LocalDateTime to = LocalDateTime.parse(request.to());

        List<LocalDateTime> suggestedSlot = schedulingService.suggestSlots(
                request.participantEmails(),
                from,
                to);

        if (suggestedSlot == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(suggestedSlot);
    }
}
