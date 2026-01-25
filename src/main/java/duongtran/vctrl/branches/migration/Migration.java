package duongtran.vctrl.branches.migration;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.*;
import duongtran.vctrl.reportchanges.HeadComparison;
import duongtran.vctrl.reportchanges.Inspector;
import duongtran.vctrl.reportchanges.WorkspaceComparison;
import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.ObjectID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
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

    private final Inspector inspector;
    private final Workspace workspace;

    private final Map<Path, TreeDiffEntry> treeDiffMap;
    private final ObjectID fromCommitId;
    private final ObjectID toCommitId;

    // Map change actions with a list of changed files
    Map<String, List<MigrationChange>> changes;
    // List of directories that will be made
    private final Set<Path> makeDirs = new HashSet<>();
    // List of directories that will be removed if it's empty
    private final Set<Path> removeDirs = new HashSet<>();
    // Conflicted table that maps the conflict type with its file
    private final EnumMap<ConflictType, List<Path>> conflicts;
    // Conflict's message table
    private final EnumMap<ConflictType, List<String>> conflictMessages;
    // Conflict error list
    private final List<String> errorMessages;

    public Migration(ObjectID fromCommitId, ObjectID toCommitId, Map<Path, TreeDiffEntry> treeDiffMap) {
        this.inspector = new Inspector();
        this.workspace = Workspace.getInstance();

        this.fromCommitId = fromCommitId;
        this.toCommitId = toCommitId;
        this.treeDiffMap = treeDiffMap;

        // Change table
        this.changes = new HashMap<>();
        for (MigrationActionType actionType : MigrationActionType.values()) {
            changes.put(actionType.toString(), new ArrayList<>());
        }

        // Conflicts table
        this.conflicts = new EnumMap<>(ConflictType.class);

        for (ConflictType type : ConflictType.values()) {
            conflicts.put(type, new ArrayList<>());
        }

        // Conflicts and its messages
        this.conflictMessages = new EnumMap<>(ConflictType.class);
        buildConflictMessages(this.conflictMessages);
        this.errorMessages = new ArrayList<>();
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
    public void applyChanges() throws ConflictException {
        try {
            // Build changes map, list of added directories, and list of removable directories
            planChanges();
            // Apply those changes to the workspace
            updateWorkspace();
            // Update the index
            updateIndex();
        } catch (ConflictException e) {
            throw new ConflictException(e.getMessage());
        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("Failed to apply changes to the workspace: {}", e.getMessage());
        }

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

    public List<Path> getConflicts(ConflictType type) {
        return conflicts.get(type);
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
    private void planChanges() throws IOException, NoSuchAlgorithmException, ConflictException {
        Index index = Index.loadFromDisk();
        if (index == null) index = new Index();
        for (Map.Entry<Path, TreeDiffEntry> treeDiffEntry : treeDiffMap.entrySet()) {
            Path path = treeDiffEntry.getKey();
            TreeDiffEntry treeChangePair = treeDiffEntry.getValue();

            // Check if there's any conflict
            checkConflict(index, path, treeChangePair);
            // Record the changes
            recordChanges(path, treeChangePair);
        }

        collectErrors();

    }

    /**
     * Records changes by analyzing the given path and tree change pair, determining
     * the type of action required (add, modify, or delete), and updating the migration
     * plan accordingly. Additionally, it ensures that the appropriate parent directories
     * are collected based on the action type.
     *
     * @param path           the file or directory path that is subject to the change
     * @param treeChangePair the {@link TreeDiffEntry} representing the difference
     *                       between the old and new tree states for the given path
     */
    private void recordChanges(Path path, TreeDiffEntry treeChangePair) {

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
            return;
        }

        addChangeEntry(actionType, new MigrationChange(path, treeChangePair));
    }

    /**
     * Checks for potential conflicts during a migration operation by comparing the state of
     * a given path between the index, workspace, and tree entries.
     * <p>
     * This method inspects the differences between the index, workspace file state,
     * and old/new tree state to identify various types of conflicts. It handles scenarios such as
     * deletion of untracked directories, overwriting of untracked or modified files, and replacement
     * of directories with files. Identified conflicts are added to the list for further processing.
     *
     * @param index          the {@link Index} containing the current index state of the repository
     * @param path           the file or directory {@link Path} being analyzed for conflicts
     * @param treeChangePair the {@link TreeDiffEntry} representing the difference
     *                       between the old and new tree states for the given path
     * @throws IOException              if an I/O error occurs during file state retrieval
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    private void checkConflict(Index index, Path path, TreeDiffEntry treeChangePair) throws IOException, NoSuchAlgorithmException {
        IndexKey indexKey = new IndexKey(workspace.getRootPath().resolve(path), StagEnum.STAGE_NORMAL.toValue());
        IndexEntry indexEntry = index.getEntryMap().get(indexKey);
        DataEntry oldEntry = treeChangePair.getOldEntry();
        DataEntry newEntry = treeChangePair.getNewEntry();
        if (isIndexDiffersFromTrees(indexEntry, oldEntry, newEntry)) {
            addConflictEntry(ConflictType.STALE_FILE, Path.of(indexEntry.getPath()));
            return;
        }

        FileStat stat = Workspace.getInstance().toFileStat(workspace.getRootPath().resolve(path));
        ConflictType conflictType = getErrorType(stat, indexEntry, newEntry);

        // Checkout danger: delete a directory that contains untracked files
        if (stat == null) {
            Path parent = untrackedParent(index, path);
            if (parent != null) {
                addConflictEntry(conflictType, indexEntry != null ? path : parent);
            }
            return;
        }

        // Checkout danger: overwriting a modified or untracked file
        if (stat.isFile()) {
            WorkspaceComparison wsComparison =
                    this.inspector.compareIndexToWorkspace(indexEntry, stat);

            if (wsComparison != WorkspaceComparison.CLEAN) {
                conflicts.get(conflictType).add(path);
            }
            return;
        }

        // Checkout danger:
        //  1. replacing a directory with a file
        //  2. deleting a directory that contains untracked files
        if (stat.isDirectory()) {
            if (inspector.trackableFile(stat, index)) {
                conflicts.get(conflictType).add(path);
            }
        }

    }

    /**
     * Determines whether the index entry differs from the two provided tree entries.
     *
     * @param indexEntry   the index entry to be compared
     * @param oldTreeEntry the data entry representing the old tree state
     * @param newTreeEntry the data entry representing the new tree state
     * @return {@code true} if the index entry does not differ from both the old tree entry and new tree entry
     * with a "CLEAN" comparison result; {@code false} otherwise
     */
    private boolean isIndexDiffersFromTrees(IndexEntry indexEntry, DataEntry oldTreeEntry, DataEntry newTreeEntry) {
        boolean isDiffFromOld = inspector.compareIndexToHead(indexEntry, oldTreeEntry) != HeadComparison.CLEAN;
        boolean isDiffFromNew = inspector.compareIndexToHead(indexEntry, newTreeEntry) != HeadComparison.CLEAN;
        return isDiffFromOld && isDiffFromNew;
    }

    /**
     * Finds the nearest parent directory of the given path that is both tracked in the index
     * and exists in the workspace. If no such parent directory is found, returns null.
     *
     * @param index the {@link Index} object used to check if a path is tracked
     * @param path  the {@link Path} whose untracked parent directory is to be located
     * @return the nearest parent {@link Path} that is tracked and exists in the workspace,
     * or {@code null} if no such parent exists
     */
    private Path untrackedParent(Index index, Path path) {
        Path parent = path.getParent();
        while (parent != null) {
            if (index.isTracked(parent)
                    && workspace.exists(parent)) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Updates the current workspace by applying necessary migration changes.
     * This method interacts with the {@code Workspace} singleton instance to execute
     * migration logic encapsulated within the {@code applyMigration} method.
     * <p>
     * If an exception occurs during this process, it logs an error message.
     *
     */
    private void updateWorkspace() throws IOException, NoSuchAlgorithmException {
        Workspace.getInstance().applyMigration(this);
    }

    /**
     * Updates the repository index by loading it from disk and updating its state
     * based on the current migration's target commit ID.
     * <p>
     * If the index cannot be loaded from disk, the method returns immediately
     * without performing any updates.
     * <p>
     * This method initializes an {@link IndexUpdater} instance using the loaded index
     * and applies the necessary updates to align the index with the target commit ID.
     *
     * @throws IOException              if an I/O error occurs while accessing the index or updating it
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    private void updateIndex() throws IOException, NoSuchAlgorithmException {
        Index index = Index.loadFromDisk();
        if (index == null) {
            return;
        }
        IndexUpdater indexUpdater = new IndexUpdater(index);
        indexUpdater.updateFromCommitId(this.toCommitId);

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


    /**
     * Adds a conflict entry to the specified conflict type.
     *
     * @param type the type of conflict to which the entry will be added
     * @param path the path associated with the conflict
     */
    private void addConflictEntry(ConflictType type, Path path) {
        conflicts.get(type).add(path);
    }

    /**
     * Processes a collection of conflicts, constructs error messages for each conflict type,
     * and throws a {@link ConflictException} if there are any errors.
     * <p>
     * This method iterates through the map of conflicts categorized by {@code ConflictType},
     * retrieves the corresponding file paths and conflict-specific message templates,
     * and generates error messages. If any conflicts are detected, it aggregates the error
     * messages and raises an exception.
     *
     * @throws ConflictException if there are any conflicts present in the collection.
     */
    private void collectErrors() throws ConflictException {
        for (Map.Entry<ConflictType, List<Path>> entry : conflicts.entrySet()) {

            List<Path> paths = entry.getValue();
            if (paths.isEmpty()) {
                continue;
            }

            List<String> template = conflictMessages.get(entry.getKey());
            StringBuilder sb = new StringBuilder();

            sb.append(template.get(0)).append('\n');

            for (Path path : paths) {
                sb.append('\t').append(path).append('\n');
            }
            sb.append(template.get(1));
            errorMessages.add(sb.toString());
        }

        if (!errorMessages.isEmpty()) {
            throw new ConflictException(String.join("\n", errorMessages));
        }
    }

    /**
     * Determines the type of error or conflict based on the provided file status, index entry,
     * and new data entry.
     *
     * @param stat the file status information, which may indicate if the file is a directory
     * @param indexEntry the index entry of the file, which may indicate it is stale
     * @param newDataEntry the new data entry of the file, which may indicate it is untracked
     * @return the type of conflict, either STALE_FILE, STALE_DIRECTORY, UNTRACKED_OVERWRITTEN, or UNTRACKED_REMOVED
     */
    public ConflictType getErrorType(FileStat stat, IndexEntry indexEntry, DataEntry newDataEntry) {
        if (indexEntry != null) return ConflictType.STALE_FILE;
        if (stat != null && stat.isDirectory()) return ConflictType.STALE_DIRECTORY;
        if (newDataEntry != null) return ConflictType.UNTRACKED_OVERWRITTEN;
        return ConflictType.UNTRACKED_REMOVED;
    }

    /**
     * Builds and populates conflict messages for specific conflict types into the provided EnumMap.
     * This method maps each {@link ConflictType} to its corresponding list of conflict messages.
     *
     * @param conflictMessages an {@link EnumMap} where the keys are {@link ConflictType} enums
     *                         and the values are lists of corresponding messages. If the map
     *                         is null, the method does nothing.
     */
    private void buildConflictMessages(EnumMap<ConflictType, List<String>> conflictMessages) {
        if (conflictMessages == null) return;
        for (ConflictType conflictType : ConflictType.values()) {
            switch (conflictType) {
                case STALE_FILE -> conflictMessages.put(ConflictType.STALE_FILE, new ArrayList<>(List.of(
                        "Your local changes to the following files would be overwritten by checkout:",
                        "Please commit your changes or stash them before you switch branches."
                )));
                case STALE_DIRECTORY -> conflictMessages.put(ConflictType.STALE_DIRECTORY, new ArrayList<>(List.of(
                        "Updating the following directories would lose untracked files in them:",
                        "\n"
                )));
                case UNTRACKED_OVERWRITTEN ->
                        conflictMessages.put(ConflictType.UNTRACKED_OVERWRITTEN, new ArrayList<>(List.of(
                                "The following untracked working tree files would be overwritten by checkout:",
                                "Please move or remove them before you switch branches."
                        )));
                case UNTRACKED_REMOVED -> conflictMessages.put(ConflictType.UNTRACKED_REMOVED, new ArrayList<>(List.of(
                        "The following untracked working tree files would be removed by checkout:",
                        "Please move or remove them before you switch branches."
                )));
                default -> { /* Do nothing */}
            }
        }
    }

}
