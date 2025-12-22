package duongtran.vctrl.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HunkBuilder {

    private HunkBuilder() {}

    public static List<Hunk> build(List<Edit> edits, int context) {
        List<Range> ranges = new ArrayList<>();

        // Step 1: find change ranges
        for (int i = 0; i < edits.size(); i++) {
            if (edits.get(i).getType() != EditType.EQUAL) {
                int start = Math.max(0, i - context);
                int end = Math.min(edits.size(), i + context + 1);
                ranges.add(new Range(start, end));
            }
        }

        if (ranges.isEmpty()) {
            return List.of();
        }

        // Step 2: merge overlapping ranges
        List<Range> merged = mergeRanges(ranges);

        // Step 3: build hunks
        List<Hunk> hunks = new ArrayList<>();
        for (Range r : merged) {
            List<Edit> hunkEdits = edits.subList(r.start, r.end);
            hunks.add(buildHunk(hunkEdits));
        }

        return hunks;
    }

    private static List<Range> mergeRanges(List<Range> ranges) {
        ranges.sort(Comparator.comparingInt(r -> r.start));

        List<Range> result = new ArrayList<>();
        Range current = ranges.get(0);

        for (int i = 1; i < ranges.size(); i++) {
            Range next = ranges.get(i);
            if (next.start <= current.end) {
                current = new Range(
                        current.start,
                        Math.max(current.end, next.end)
                );
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);

        return result;
    }

    private static Hunk buildHunk(List<Edit> edits) {
        int oldStart = -1;
        int newStart = -1;
        int oldLength = 0;
        int newLength = 0;

        for (Edit e : edits) {
            if (oldStart == -1 && e.getOldLineNo() != -1) {
                oldStart = e.getOldLineNo();
            }
            if (newStart == -1 && e.getNewLineNo() != -1) {
                newStart = e.getNewLineNo();
            }

            if (e.getOldLineNo() != -1) oldLength++;
            if (e.getNewLineNo() != -1) newLength++;
        }

        return new Hunk(
                oldStart == -1 ? 0 : oldStart,
                oldLength,
                newStart == -1 ? 0 : newStart,
                newLength,
                new ArrayList<>(edits)
        );
    }

    private record Range(int start, int end) {}
}

