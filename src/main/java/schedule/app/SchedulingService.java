package schedule.app;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchedulingService {
    private Map<UUID, Meeting> meetingRepository = new HashMap<>();
    private Map<String, Person> personRepository = new HashMap<>();

    private Map<String, Set<LocalDateTime>> personSchedule = new HashMap<>();

    public Person addPerson(String name, String email) {
        if (personRepository.containsKey(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Person with this email already exists");
        }
        Person person = new Person(name, email);
        personRepository.put(email, person);
        personSchedule.put(email, new HashSet<>());
        return person;
    }

    public Meeting scheduleMeeting(LocalDateTime startTime, Set<String> attendeeEmails) {
        Set<Person> attendees = new HashSet<>();
        for (String email : attendeeEmails) {
            Person person = personRepository.get(email);
            if (person == null) {
                throw new IllegalArgumentException("Person with email " + email + " does not exist");
            }
            attendees.add(person);
        }
        if (attendees.isEmpty()) {
            throw new IllegalArgumentException("At least one attendee is required to schedule a meeting");
        }

        // Check for scheduling conflicts
        for (Person attendee : attendees) {
            Set<LocalDateTime> scheduledTimes = personSchedule.get(attendee.getEmail());
            if (scheduledTimes.contains(startTime)) {
                throw new IllegalArgumentException("Scheduling conflict for " + attendee.getEmail());
            }
        }

        Meeting meeting = new Meeting(startTime, attendees);
        meetingRepository.put(meeting.getId(), meeting);

        // Update the schedule for each attendee
        for (Person attendee : attendees) {
            personSchedule.get(attendee.getEmail()).add(startTime);
        }

        return meeting;
    }

    public Meeting getMeeting(UUID meetingId) {
        return meetingRepository.get(meetingId);
    }

    public Person getPerson(String email) {
        return personRepository.get(email);
    }

    public List<Meeting> getSchedule(String email) {
        Person person = personRepository.get(email);
        if (person == null) {
            throw new IllegalArgumentException("Person with email " + email + " does not exist");
        }

        return meetingRepository.values()
                .stream()
                .filter(m -> m.getAttendees().contains(person))
                .sorted(Comparator.comparing(
                        Meeting::getStartTime))
                .collect(Collectors.toList());
    }

    public List<LocalDateTime> suggestSlots(List<String> participantEmails, LocalDateTime from,LocalDateTime to ) {

        List<Person> participants = participantEmails.stream()
        .map(email -> Optional.ofNullable(getPerson(email))
                .orElseThrow(() -> new IllegalArgumentException("Person with email " + email + " does not exist")))
        .toList();

        List<LocalDateTime> available = new ArrayList<>();

        LocalDateTime current = from;

        while (!current.isAfter(to)) {

            boolean everyoneFree = true;

            for (Person person : participants) {

                Set<LocalDateTime> booked =
                        personSchedule.get(person.getEmail());

                if (booked.contains(current)) {
                    everyoneFree = false;
                    break;
                }
            }

            if (everyoneFree) {
                available.add(current);
            }

            current = current.plusHours(1);
        }

        return available;
    }
}





