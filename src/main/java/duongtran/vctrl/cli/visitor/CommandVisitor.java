package duongtran.vctrl.cli.visitor;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.*;
import duongtran.vctrl.cli.visitor.args.*;
import duongtran.vctrl.cli.parser.*;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class CommandVisitor implements VctrlParserVisitor {

    private static final Logger log = LoggerFactory.getLogger(CommandVisitor.class);

    @Override
    public Object visit(ASTInitCommand node, Object data) {
        String path = (String) node.jjtGetValue();
        InitAction.init(path);
        return null;
    }

    @Override
    public Object visit(ASTAddCommand node, Object data) {
        AddAction addAction = new AddAction();
        String parsed = (String) node.jjtGetValue();
        if (parsed.equals(DirectoryNames.ADD_ALL_FILES)) {
            addAction.execute();
        } else {
            Path path = Workspace.getInstance().getRootPath().resolve(parsed);
            addAction.execute(path);
        }
        return null;
    }

    @Override
    public Object visit(ASTCommitCommand node, Object data) {
        String commitMsg = (String) node.jjtGetValue();
        CommitAction commitAction = new CommitAction();
        try {
            commitAction.execute(commitMsg);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    @Override
    public Object visit(ASTStatusCommand node, Object data) {
       StatusAction statusAction = new StatusAction();
       try {
           Status status = statusAction.execute();
           statusAction.displayStatus(status);
       } catch (Exception ex) {
           System.out.println(ex.getMessage());
       }

       return null;
    }

    @Override
    public Object visit(ASTLogCommand node, Object data) {
        LogCommandData cmdArgs = (LogCommandData) node.jjtGetValue();
        LogAction logAction = new LogAction();
        try {
            logAction.execute(cmdArgs);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }

       return null;
    }


    @Override
    public Object visit(ASTBranchCommand node, Object data) {
        BranchCommandData args = (BranchCommandData) node.jjtGetValue();
        BranchAction branchAction = new BranchAction();
        try {
            if (args.name == null) {
                List<String> branches = branchAction.listBranches();
                branchAction.displayBranches(branches);
            } else if (args.delete) {
                branchAction.deleteBranch(args.name);
            } else { // Create a new branch
                branchAction.execute(args.name, 0);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return null;
    }

    @Override
    public Object visit(ASTCheckoutCommand node, Object data) {
        CheckoutCommandData args = (CheckoutCommandData) node.jjtGetValue();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        try {
            String branchName = args.name;
            int revision = 0;
            if (args.create) {
                branchAction.execute(branchName, revision);
            }
            checkoutAction.execute(branchName, revision);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }


    @Override
    public Object visit(ASTMergeCommand node, Object data) {

        MergeCommandData cmdArgs = (MergeCommandData) node.jjtGetValue();
        MergeAction mergeAction = new MergeAction();
        try {
            // TODO: right now just support merge one branch
            String mergeBranch = cmdArgs.sources.get(0);
            int revision = 0;
            mergeAction.execute(mergeBranch, revision);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }

        return null;
    }

    @Override
    public Object visit(ASTDiffCommand node, Object data) {
        DiffCommandData args = (DiffCommandData) node.jjtGetValue();
        DiffAction diffAction = new DiffAction();
        try {
            diffAction.execute(args.cached);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return node.childrenAccept(this, data);
    }

    @Override
    public Object visit(ASTCommand node, Object data) {
        return node.childrenAccept(this, data);
    }
}
