package duongtran.vctrl.diff;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyersDiffTest {

    @Test
    void testDiff() {

        String first = "ABCABBA";
        String second = "CBABAC";

        List<Edit> actual = MyersDiff.diff(toStringArray(first), toStringArray(second));
        List<Edit> expected = new ArrayList<>(List.of(
                new Edit(EditType.DELETE, 1, -1, "A")
                , new Edit(EditType.DELETE, 2, -1, "B")
                , new Edit(EditType.EQUAL, 3, 1, "C")
                , new Edit(EditType.INSERT, -1, 2, "B")
                , new Edit(EditType.EQUAL, 4, 3, "A")
                , new Edit(EditType.EQUAL, 5, 4, "B")
                , new Edit(EditType.DELETE, 6, -1, "B")
                , new Edit(EditType.EQUAL, 7, 5, "A")
                , new Edit(EditType.INSERT, -1, 6, "C")
        ));

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }

    }

    private List<String> toStringArray(String s) {
        List<String> list = new ArrayList<>();

        char[] chars = s.toCharArray();
        for (char c : chars) {
            list.add(String.valueOf(c));
        }
        return list;
    }

}
