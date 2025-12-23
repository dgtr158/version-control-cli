package duongtran.vctrl;

import duongtran.vctrl.actions.*;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.utils.ActionConstants;
import duongtran.vctrl.utils.DirectoryNames;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length < 1 || args.length > 3) {
            System.out.println("USAGE: java Main.java init [file_path]");
            return;
        }

        // Get the first argument
        String command = args[0];

        // Initialize Workspace at startup
        // TODO: Remove after done storing changes part
        Workspace.initialize(DirectoryNames.WORKING_DIRECTORY);
        Database.initialize();

        Workspace workspace = Workspace.getInstance();

        switch (command) {
            case ActionConstants.INIT:
                String basePath = args.length == 2 ? args[1] : null;
                InitAction.init(basePath);
                break;
            case ActionConstants.COMMIT:
                CommitAction commitAction = new CommitAction();
                commitAction.execute();
                break;
            case ActionConstants.ADD:
                AddAction addAction = new AddAction();
                addAction.execute(workspace.getRootPath());
                break;
            case ActionConstants.DIFF:
                DiffAction diffAction = new DiffAction();
                diffAction.execute(true);
                break;
            case ActionConstants.CHECKOUT:
                BranchAction branchAction = new BranchAction();
                branchAction.execute("");
                break;
            default:
                String msg = MessageFormat.format("{0}: {1} is not a {2} command"
                        , DirectoryNames.PROJECT_NAME
                        , command
                        , DirectoryNames.PROJECT_NAME
                );
                System.out.println(msg);
        }
    }
}