package duongtran.vctrl.cli.visitor;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.actions.InitAction;
import duongtran.vctrl.cli.parser.*;

import java.nio.file.Path;

public class CommandVisitor implements VctrlParserVisitor {

    @Override
    public Object visit(ASTInitCommand node, Object data) {
        String path = (String) node.jjtGetValue();
        InitAction.init(path);
        return null;
    }

    @Override
    public Object visit(ASTAddCommand node, Object data) {
        String parsed = (String) node.jjtGetValue();
        Path path = Workspace.getInstance().getRootPath().resolve(parsed);
        new AddAction().execute(path);
        return null;
    }

    /* ===== required boilerplate ===== */

    @Override
    public Object visit(SimpleNode node, Object data) {
        return node.childrenAccept(this, data);
    }

    @Override
    public Object visit(ASTCommand node, Object data) {
        return node.childrenAccept(this, data);
    }
}
