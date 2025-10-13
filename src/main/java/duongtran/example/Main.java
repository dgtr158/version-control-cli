package duongtran.example;

import duongtran.example.actions.AddAction;
import duongtran.example.actions.CommitAction;
import duongtran.example.actions.InitAction;
import duongtran.example.metadata.Workspace;
import duongtran.example.storage.Database;
import duongtran.example.utils.ActionConstants;
import duongtran.example.utils.DirectoryNames;

import java.io.IOException;
import java.text.MessageFormat;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 3) {
            System.out.println("USAGE: java Main.java init [file_path]");
            return;
        }

        // Get the first argument
        String command = args[0];

        // Initialize Workspace at startup
        // TODO: Remove after done storing changes part
        Workspace.initialize();
        Database.initialize();

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
                addAction.execute();
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