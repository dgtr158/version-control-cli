package duongtran.vctrl.branches.migration;

import duongtran.vctrl.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * The Migration class is responsible for handling changes between workspace states
 * and applying the necessary modifications during a migration process. It manages
 * the detection of changes within a given directory structure, categorizes them into
 * various actions (e.g., additions, modifications, deletions), and executes the migration
 * on the workspace.
 * <p>
 * This class uses a tree-diff map to track differences and maintains lists or sets
 * of changes for directory creation or deletion. The main operations consist of
 * planning changes and updating the workspace.
 */
public class Migration {
    private static final Logger log = LoggerFactory.getLogger(Migration.class);

    private final Map<Path, TreeChanges> treeDiffMap;

    // Map change actions with a list of changed files
    Map<String, List<MigrationChange>> changes;
    // List of directories that will be made
    private final Set<Path> makeDirs = new HashSet<>();
    // List of directories that will be removed if it's empty
    private final Set<Path> removeDirs = new HashSet<>();

    public Migration(Map<Path, TreeChanges> treeDiffMap) {
        this.treeDiffMap = treeDiffMap;
        this.changes = new HashMap<>();
        for (MigrationActionType actionType : MigrationActionType.values()) {
            changes.put(actionType.toString(), new ArrayList<>());
        }
    }

    /**
     * Applies the planned migration changes to the workspace.
     * <p>
     * This method performs the following operations:
     * 1. Plans the necessary changes by analyzing the differences provided in the {@code treeDiffMap}.
     * It identifies the required changes (e.g., additions, modifications, deletions) and categorizes
     * them into appropriate lists or sets for further processing.
     * 2. Updates the workspace by executing the planned changes. It interacts with the workspace
     * implementation to apply the migration logic, ensuring the consistency of the workspace
     * after the changes are implemented.
     * <p>
     * If an exception occurs during the update process, it logs an error message to indicate
     * the failure.
     */
    public void applyChanges() {
        // Build changes map, list of added directories, and list of removable directories
        planChanges();
        // Apply those changes to the workspace
        updateWorkspace();
    }

    /**
     * Retrieves the mapping of change actions to the corresponding list of migration changes.
     *
     * @return a map where the keys represent change action types (e.g., "ADD", "MODIFIED", "DELETE"),
     * and the values are lists of {@link MigrationChange} objects associated with those actions.
     */
    public Map<String, List<MigrationChange>> getChanges() {
        return changes;
    }

    /**
     * Retrieves the set of directory paths that need to be created as part of the migration process.
     *
     * @return a set of {@code Path} objects representing the directories to be created.
     */
    public Set<Path> getMakeDirs() {
        return makeDirs;
    }

    /**
     * Retrieves the set of directory paths that need to be removed as part of the migration process.
     *
     * @return a set of {@code Path} objects representing the directories to be removed.
     */
    public Set<Path> getRemoveDirs() {
        return removeDirs;
    }

    /**
     * Plans the changes for the migration process based on the differences provided in the {@code treeDiffMap}.
     * <p>
     * For each entry in the {@code treeDiffMap}, this method determines the type of change
     * (addition, modification, or deletion) for the corresponding path and populates the necessary change details
     * into the migration plan. It also tracks the parent directories that need to be created or removed
     * as part of the migration process.
     *
     */
    private void planChanges() {
        for (Map.Entry<Path, TreeChanges> treeDiffEntry : treeDiffMap.entrySet()) {
            Path path = treeDiffEntry.getKey();
            TreeChanges treeChangePair = treeDiffEntry.getValue();

            MigrationActionType actionType;
            if (treeChangePair.isAdded()) {
                collectParentDirs(path, makeDirs);
                actionType = MigrationActionType.ADD;
            } else if (treeChangePair.isModified()) {
                collectParentDirs(path, makeDirs);
                actionType = MigrationActionType.MODIFIED;
            } else if (treeChangePair.isDeleted()) {
                collectParentDirs(path, removeDirs);
                actionType = MigrationActionType.DELETE;
            } else {
                continue;
            }

            addChangeEntry(actionType, new MigrationChange(path, treeChangePair));
        }
    }

    /**
     * Updates the current workspace by applying necessary migration changes.
     * This method interacts with the {@code Workspace} singleton instance to execute
     * migration logic encapsulated within the {@code applyMigration} method.
     * <p>
     * If an exception occurs during this process, it logs an error message.
     *
     */
    private void updateWorkspace() {
        try {
            Workspace.getInstance().applyMigration(this);
        } catch (Exception ex) {
            log.error("Failed to apply changes to the workspace: {}", ex.getMessage());
        }
    }

    /**
     * Collects all parent directories of the given {@code path} and adds them to the specified {@code target} set.
     *
     * @param path   the starting path whose parent directories are to be collected
     * @param target the set to store the collected parent directories
     */
    private void collectParentDirs(Path path, Set<Path> target) {
        Path parent = path.getParent();
        while (parent != null) {
            target.add(parent);
            parent = parent.getParent();
        }
    }

    /**
     * Adds a migration change entry to the list of changes corresponding to the specified action type.
     *
     * @param actionType the type of migration action (e.g., "ADD", "MODIFIED", "DELETE")
     * @param entry      the {@link MigrationChange} object representing the migration change to be added
     */
    private void addChangeEntry(MigrationActionType actionType, MigrationChange entry) {
        String actionTypeValue = actionType.toString();
        if (!this.changes.containsKey(actionTypeValue)) return;
        List<MigrationChange> actionChangeList = this.changes.get(actionTypeValue);
        actionChangeList.add(entry);
    }

}
