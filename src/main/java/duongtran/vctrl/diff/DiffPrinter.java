package duongtran.vctrl.diff;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.utils.AnsiColor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DiffPrinter {

    private final Workspace workspace;

    public DiffPrinter() {
        this.workspace = Workspace.getInstance();
    }

    public void print(DiffResult result) {
        if (result.isEmpty()) {
            return;
        }

        for (Map.Entry<Path, List<Hunk>> entry : result.getHunks().entrySet()) {
            Path path = entry.getKey();
            List<Hunk> hunks = entry.getValue();

            printFileHeader(path);

            for (Hunk hunk : hunks) {
                printHunk(hunk);
            }
        }
    }

    private void printFileHeader(Path path) {
        Path revPath = this.workspace.getRootPath().relativize(path);
        System.out.println(AnsiColor.boldYellow("diff -- " + revPath));
        System.out.println(AnsiColor.red("--- a/" + revPath));
        System.out.println(AnsiColor.green("+++ b/" + revPath));
    }

    private void printHunk(Hunk hunk) {
        System.out.printf(
                "@@ -%d,%d +%d,%d @@%n",
                hunk.getOldStart(),
                hunk.getOldLength(),
                hunk.getNewStart(),
                hunk.getNewLength()
        );

        for (EditScript edit : hunk.getEdits()) {
            printEdit(edit);
        }
    }

    private void printEdit(EditScript edit) {
        switch (edit.getType()) {
            case EQUAL -> {
                System.out.println(" " + edit.getContent());
            }
            case INSERT -> {
                System.out.println(AnsiColor.green("+" + edit.getContent()));
            }
            case DELETE -> {
                System.out.println(AnsiColor.red("-" + edit.getContent()));
            }
        }
    }
}
