package duongtran.vctrl.storage;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CommitAuthorTest {

    @Test
    void testFromStringBasic() {
        String input = "Alice <alice@example.com> 1716030000 +0700";

        CommitAuthor author = CommitAuthor.fromString(input);

        assertEquals("Alice", author.getName());
        assertEquals("alice@example.com", author.getEmail());

        Instant expected = Instant.ofEpochSecond(1716030000)
                .atOffset(ZoneOffset.of("+0700"))
                .toInstant();

        assertEquals(expected, author.getTime());
    }

    @Test
    void testFromStringNameWithSpaces() {
        String input = "Nguyen Van A <a.nguyen@example.com> 1716030000 +0700";

        CommitAuthor author = CommitAuthor.fromString(input);

        assertEquals("Nguyen Van A", author.getName());
        assertEquals("a.nguyen@example.com", author.getEmail());
    }

    @Test
    void testFromStringNegativeTimezone() {
        String input = "Bob <bob@example.com> 1716030000 -0800";

        CommitAuthor author = CommitAuthor.fromString(input);

        Instant expected = Instant.ofEpochSecond(1716030000)
                .atOffset(ZoneOffset.of("-0800"))
                .toInstant();

        assertEquals(expected, author.getTime());
    }

    @Test
    void testFromStringTrimSpaces() {
        String input = "  Alice   <alice@example.com>   1716030000   +0700  ";

        CommitAuthor author = CommitAuthor.fromString(input);

        assertEquals("Alice", author.getName());
        assertEquals("alice@example.com", author.getEmail());
    }

    @Test
    void testFromStringInvalidFormat_missingEmail() {
        String input = "Alice alice@example.com 1716030000 +0700";

        assertThrows(IllegalArgumentException.class, () -> {
            CommitAuthor.fromString(input);
        });
    }

    @Test
    void testFromStringInvalidFormat_missingTimestamp() {
        String input = "Alice <alice@example.com> +0700";

        assertThrows(IllegalArgumentException.class, () -> {
            CommitAuthor.fromString(input);
        });
    }
}
