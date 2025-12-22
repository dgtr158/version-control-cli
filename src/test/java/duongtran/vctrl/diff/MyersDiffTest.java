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

        List<EditScript> actual = MyersDiff.diff(toStringArray(first), toStringArray(second));
        List<EditScript> expected = new ArrayList<>(List.of(
                new EditScript(EditType.DELETE, 1, -1, "A")
                , new EditScript(EditType.DELETE, 2, -1, "B")
                , new EditScript(EditType.EQUAL, 3, 1, "C")
                , new EditScript(EditType.INSERT, -1, 2, "B")
                , new EditScript(EditType.EQUAL, 4, 3, "A")
                , new EditScript(EditType.EQUAL, 5, 4, "B")
                , new EditScript(EditType.DELETE, 6, -1, "B")
                , new EditScript(EditType.EQUAL, 7, 5, "A")
                , new EditScript(EditType.INSERT, -1, 6, "C")
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
