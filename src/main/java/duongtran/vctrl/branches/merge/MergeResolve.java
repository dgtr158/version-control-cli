package duongtran.vctrl.branches.merge;

import duongtran.vctrl.branches.migration.ConflictException;
import duongtran.vctrl.branches.migration.Migration;
import duongtran.vctrl.branches.migration.TreeDiff;
import duongtran.vctrl.branches.migration.TreeDiffEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * The MergeResolve class is responsible for orchestrating the process of
 * merging one branch into another. It takes the merge inputs, identifies the differences
 * between the branches, and applies the changes to resolve the merge.
 * <p>
 * This class leverages a three-way merge strategy by comparing the base, head,
 * and other branches involved in the merging process. It detects differences
 * between the branches and applies those changes using a migration process.
 * <p>
 * Key Responsibilities:
 * - Detect differences between the base and other branches using TreeDiff.
 * - Apply the detected changes to achieve the merge using a Migration instance.
 * - Handle potential conflicts during the merge by throwing ConflictException.
 */
public class MergeResolve {

    private final MergeInputs inputs;

    public MergeResolve(MergeInputs inputs) {
        this.inputs = inputs;
    }

    public void execute() throws IOException, NoSuchAlgorithmException {
        TreeDiff treeDiff = new TreeDiff();

        Map<Path, TreeDiffEntry> diff =
                treeDiff.detectTreeDiff(
                        inputs.getBase(),
                        inputs.getOther()
                );

        try {
            Migration migration = new Migration(
                    inputs.getBase(),
                    inputs.getOther(),
                    diff
            );
            migration.applyChanges();
        } catch (ConflictException e) {
            throw new ConflictException(e.getMessage());
        }
    }

}
