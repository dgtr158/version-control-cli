package duongtran.vctrl.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyersDiff {

    private MyersDiff() {
    }

    public static List<Edit> diff(List<String> a, List<String> b) {
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

    private static List<Edit> backtrack(
            List<int[]> trace,
            List<String> a,
            List<String> b,
            int offset
    ) {
        List<Edit> result = new ArrayList<>();

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
                result.add(new Edit(EditType.EQUAL, x, y, a.get(x - 1)));
                x--;
                y--;
            }

            if (d == 0) break;

            if (x == prevX) {
                // Insert
                result.add(new Edit(EditType.INSERT, -1, y, b.get(y - 1)));
                y--;
            } else {
                // Delete
                result.add(new Edit(EditType.DELETE, x, -1, a.get(x - 1)));
                x--;
            }
        }

        Collections.reverse(result);
        return result;
    }


}
