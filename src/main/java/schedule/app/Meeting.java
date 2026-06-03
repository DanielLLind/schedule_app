package schedule.app;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class Meeting {
    private UUID id;
    private LocalDateTime startTime;
    private Set<Person> attendees;

    public Meeting(LocalDateTime startTime, Set<Person> attendees) {
        validateStartTime(startTime);

        this.id = UUID.randomUUID();
        this.startTime = startTime;
        this.attendees = attendees;
    }

    private void validateStartTime(LocalDateTime startTime2) {
        //validate that the start time is on the hour
        if (startTime2.getMinute() != 0 || startTime2.getSecond() != 0 || startTime2.getNano() != 0) {
            throw new IllegalArgumentException("Start time must be on the hour");
        }
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Set<Person> getAttendees() {
        return attendees;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", attendees=" + attendees.stream().map(Person::getEmail).toList() +
                '}';
    }

}
