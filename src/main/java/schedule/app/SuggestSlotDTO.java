package schedule.app;

import java.util.List;

public record SuggestSlotDTO(List<String> participantEmails, String from, String to) {
    
}
