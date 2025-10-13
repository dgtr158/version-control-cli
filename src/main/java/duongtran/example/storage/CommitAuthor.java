package duongtran.example.storage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CommitAuthor {
    private final String name;
    private final String email;
    private final Instant time;

    public CommitAuthor(String name, String email, Instant time) {
        this.name = name;
        this.email = email;
        this.time = time;
    }

    public String toString() {
        long epochSeconds = time.getEpochSecond();
        String offset = ZonedDateTime.ofInstant(time, ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ofPattern("xx"));
        return String.format("%s <%s> %d %s", name, email, epochSeconds, offset);
    }
}
