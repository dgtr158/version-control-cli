package duongtran.vctrl.cli.visitor;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.*;
import duongtran.vctrl.cli.parser.*;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

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
        LogAction logAction = new LogAction();
        try {
            logAction.execute(null);
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
