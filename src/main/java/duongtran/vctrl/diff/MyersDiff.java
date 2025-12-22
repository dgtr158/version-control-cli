package duongtran.vctrl.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The {@code MyersDiff} class implements the Myers difference algorithm for
 * computing the differences between two sequences of strings. This algorithm
 * identifies the sequence of edit operations required to transform one sequence
 * into another, using an efficient approach based on edit distances.
 * <p>
 * This class cannot be instantiated and only provides static utility methods
 * to compute differences.
 * <p>
 * Edit operations are represented as {@code EditScript} objects, which include
 * the operation type (insertion, deletion, or equality), line numbers, and the
 * associated content.
 * <p>
 * Features:
 * - Efficient computation of differences between sequences using the Myers algorithm.
 * - Generation of an edit script representing the changes between the sequences.
 */
public class MyersDiff {

    private MyersDiff() {
    }

    /**
     * Computes the differences between two lists of strings using the Myers difference algorithm.
     * The result represents a series of edit operations (insertion, deletion, or equality)
     * required to transform the first list into the second list.
     *
     * @param a the first list of strings, representing the original content
     * @param b the second list of strings, representing the target content
     * @return a list of {@code EditScript} objects representing the sequence of edits
     * needed to transform {@code a} into {@code b}
     * @throws IllegalStateException if the computation fails to produce a valid diff
     */
    public static List<EditScript> diff(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        int max = n + m;

        int offset = max;
        int size = 2 * max + 1;

        int[] v = new int[size];
        Arrays.fill(v, 0);

        // Trace for backtracking
        List<int[]> trace = new ArrayList<>();

        v[offset + 1] = 0;

        for (int d = 0; d <= max; d++) {
            int[] vCopy = v.clone();
            trace.add(vCopy);

            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;

                int x;
                if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    // Down: insertion
                    x = v[index + 1];
                } else {
                    // Right: deletion
                    x = v[index - 1] + 1;
                }

                int y = x - k;

                // Follow diagonal
                while (x < n && y < m && a.get(x).equals(b.get(y))) {
                    x++;
                    y++;
                }

                v[index] = x;

                // Reached the end
                if (x >= n && y >= m) {
                    return backtrack(trace, a, b, offset);
                }
            }
        }

        throw new IllegalStateException("Diff failed");
    }

    /**
     * Reconstructs the sequence of edit operations (edit script) from a provided trace
     * generated during the computation of the Myers diff algorithm. The method performs
     * a backtracking process to build a list of edit operations (equality, insertion, or deletion)
     * that represent the differences between two input lists.
     *
     * @param trace  a list of integer arrays, representing the computation trace of
     *               the Myers diff algorithm across edit distances
     * @param a      the first list of strings, representing the original content
     * @param b      the second list of strings, representing the target content
     * @param offset the offset used to transform the edit distance index into a valid
     *               index for the `trace` array
     * @return a list of {@code EditScript} objects representing the sequence of edits
     * that transform {@code a} into {@code b}
     */
    private static List<EditScript> backtrack(
            List<int[]> trace,
            List<String> a,
            List<String> b,
            int offset
    ) {
        List<EditScript> result = new ArrayList<>();

        int x = a.size();
        int y = b.size();

        for (int d = trace.size() - 1; d >= 0; d--) {
            int[] v = trace.get(d);
            int k = x - y;
            int index = offset + k;

            int prevK;
            if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                prevK = k + 1; // insertion
            } else {
                prevK = k - 1; // deletion
            }

            int prevX = v[offset + prevK];
            int prevY = prevX - prevK;

            // Diagonal (equal)
            while (x > prevX && y > prevY) {
                result.add(new EditScript(EditType.EQUAL, x, y, a.get(x - 1)));
                x--;
                y--;
            }

            if (d == 0) break;

            if (x == prevX) {
                // Insert
                result.add(new EditScript(EditType.INSERT, -1, y, b.get(y - 1)));
                y--;
            } else {
                // Delete
                result.add(new EditScript(EditType.DELETE, x, -1, a.get(x - 1)));
                x--;
            }
        }

        Collections.reverse(result);
        return result;
    }


}
