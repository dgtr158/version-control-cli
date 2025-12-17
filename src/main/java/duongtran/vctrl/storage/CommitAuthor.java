package duongtran.vctrl.storage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents the author of a commit, including the author's name, email,
 * and the timestamp of the commit.
 */
public class CommitAuthor {
    private final String name;
    private final String email;
    private final Instant time;

    public CommitAuthor(String name, String email, Instant time) {
        this.name = name;
        this.email = email;
        this.time = time.truncatedTo(ChronoUnit.SECONDS);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getTime() {
        return time;
    }

    /**
     * Returns a string representation of the commit author.
     * The string includes the author's name, email, the commit timestamp in epoch seconds,
     * and the timezone offset.
     *
     * @return a formatted string representing the author's details, including name, email,
     *         timestamp in epoch seconds, and timezone offset.
     */
    public String toString() {
        long epochSeconds = time.getEpochSecond();
        String offset = ZonedDateTime.ofInstant(time, ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ofPattern("xx"));
        return String.format("%s <%s> %d %s", name, email, epochSeconds, offset);
    }

    /**
     * Parses a string representation of a commit author and converts it into a
     * {@code CommitAuthor} instance. The input string is expected to contain the author's
     * name, email, commit timestamp in epoch seconds, and timezone offset.
     *
     * @param input the string containing the author's details in the format:
     *              "Name <email> epochSeconds timezoneOffset". The format must strictly
     *              follow this structure, where:
     *              - "Name" is the author's name.
     *              - "<email>" is the author's email enclosed in angle brackets.
     *              - "epochSeconds" is the commit timestamp in seconds since the epoch.
     *              - "timezoneOffset" is the commit timezone offset (e.g., "+0000").
     * @return a {@code CommitAuthor} instance created from the parsed string.
     * @throws IllegalArgumentException if the input string is in an invalid format,
     *                                  missing components, or contains invalid data.
     */
    public static CommitAuthor fromString(String input) {
        int emailStart = input.indexOf('<');
        int emailEnd = input.indexOf('>');

        if (emailStart < 0 || emailEnd < 0 || emailEnd < emailStart) {
            throw new IllegalArgumentException("Invalid author format: " + input);
        }

        String name = input.substring(0, emailStart).trim();
        String email = input.substring(emailStart + 1, emailEnd).trim();

        String rest = input.substring(emailEnd + 1).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid author timestamp format: " + input);
        }

        long epochSeconds = Long.parseLong(parts[0]);
        ZoneOffset offset = ZoneOffset.of(parts[1]);

        Instant time = Instant.ofEpochSecond(epochSeconds)
                .atOffset(offset)
                .toInstant();

        return new CommitAuthor(name, email, time);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommitAuthor that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(email, that.email) && Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, time);
    }

}
