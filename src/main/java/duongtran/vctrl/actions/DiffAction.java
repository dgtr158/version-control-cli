package duongtran.vctrl.actions;

import duongtran.vctrl.diff.DiffResult;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class DiffAction {

    private final StatusAction statusAction;
    private final int CONTEXT = 3;

    public DiffAction() {
        this.statusAction = new StatusAction();
    }

    public DiffResult execute(boolean isCached) throws IOException, NoSuchAlgorithmException {
        // Get the workspace's statuses
        Status status = statusAction.execute();

        // Get the modified files
        TreeMap<Path, StatusEntry> modifiedFiles = null;
        if (isCached) {
            modifiedFiles = status.getIndexModifiedMap();
        } else {
            modifiedFiles = status.getWorkspaceModifiedMap();
        }

        // Spot Differences
        return spotDifferences(status, modifiedFiles);

    }

    private DiffResult spotDifferences(Status status, TreeMap<Path, StatusEntry> modifiedFiles) {
        for (Map.Entry<Path, StatusEntry> entryMap : modifiedFiles.entrySet()) {
            Path path = entryMap.getKey();

        }
        return null;
    }

}
