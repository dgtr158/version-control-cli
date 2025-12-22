package duongtran.vctrl.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class responsible for constructing {@code Hunk} objects based on a list of edit scripts
 * and specified context lines. Each hunk represents a contiguous block of changes between two versions
 * of a file and includes a user-defined number of contextual lines around the changes.
 */
public final class HunkBuilder {

    private HunkBuilder() {
    }

    /**
     * Builds a list of {@code Hunk} objects from the given edit scripts and context.
     * This method processes a list of edit scripts that represent changes between two files.
     * It groups changes into contiguous ranges, merges overlapping ranges, and constructs hunks
     * for each range, including a specified number of surrounding context lines.
     *
     * @param editScripts the list of {@code EditScript} objects representing the changes
     *                    between the two files; must not be null
     * @param context     the number of context lines to include before and after the actual
     *                    changes in the hunks; must be non-negative
     * @return a list of {@code Hunk} objects representing the grouped and contextualized changes;
     * if there are no changes, an empty list is returned
     */
    public static List<Hunk> build(List<EditScript> editScripts, int context) {
        List<Range> ranges = new ArrayList<>();

        // Step 1: find change ranges
        for (int i = 0; i < editScripts.size(); i++) {
            if (editScripts.get(i).getType() != EditType.EQUAL) {
                int start = Math.max(0, i - context);
                int end = Math.min(editScripts.size(), i + context + 1);
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
            List<EditScript> hunkEditScripts = editScripts.subList(r.start, r.end);
            hunks.add(buildHunk(hunkEditScripts));
        }

        return hunks;
    }

    /**
     * Merges a list of ranges by combining overlapping or contiguous ranges.
     * The input ranges are sorted by their start values, and any overlaps are resolved
     * into a single continuous range in the resulting list.
     *
     * @param ranges the list of {@code Range} objects to be merged; must not be null,
     *               and all ranges must define valid start and end points
     * @return a list of merged {@code Range} objects, with no overlapping or contiguous ranges
     */
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

    /**
     * Builds a single {@code Hunk} object from a list of {@code EditScript} objects.
     * This method calculates line number ranges and lengths for both the old and new files
     * based on the provided edit scripts and compiles them into a {@code Hunk} object.
     *
     * @param editScripts the list of {@code EditScript} objects representing the changes between the two files;
     *                    must not be null, and may contain operations with or without associated line numbers
     * @return a {@code Hunk} object encapsulating the data from the provided edit scripts, including starting
     * line numbers, the number of lines impacted in both files, and the list of edit scripts
     */
    private static Hunk buildHunk(List<EditScript> editScripts) {
        int oldStart = -1;
        int newStart = -1;
        int oldLength = 0;
        int newLength = 0;

        for (EditScript e : editScripts) {
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
                oldStart == -1 ? 0 : oldStart
                , oldLength
                , newStart == -1 ? 0 : newStart
                , newLength
                , new ArrayList<>(editScripts)
        );
    }

    private record Range(int start, int end) {
    }
}

