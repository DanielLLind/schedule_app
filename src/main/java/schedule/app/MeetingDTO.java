package schedule.app;

import java.util.Set;

public record MeetingDTO(String startTime, Set<String> participantEmails) {

}
