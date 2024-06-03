package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.semanticAnalysis.Method;
import pt.up.fe.comp2023.semanticAnalysis.SymbolTableImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

public class ConstantPropVisitor extends AJmmVisitor<HashMap<String, JmmNode>, Void> {

    private boolean changed;
    @Override
    protected void buildVisitor() {
        changed = false;
        addVisit("Method", this::visitMethod);
        addVisit("MainMethod", this::visitMethod);
        addVisit("AssignmentStmt", this::visitAssign);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("WhileStmt", this::visitWhile);
        addVisit("IfElseStmt", this::visitIfElse);
        setDefaultVisit(this::defaultVisit);
    }

    private Void visitIfElse(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        visit(jmmNode.getJmmChild(0),constants);
        visit(jmmNode.getJmmChild(1),constants);

        for(JmmNode child : jmmNode.getJmmChild(1).getChildren()){
            if(child.getKind().equals("Assignment")){
                constants.remove(child.get("name"));
            }
        }
        visit(jmmNode.getJmmChild(2),constants);

        for(JmmNode child : jmmNode.getJmmChild(2).getChildren()){
            if(child.getKind().equals("Assignment")){
                constants.remove(child.get("name"));
            }
        }
        return null;
    }

    private Void visitWhile(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        visit(jmmNode.getJmmChild(1),constants);
        for(JmmNode child: jmmNode.getJmmChild(1).getChildren())
            if(child.getKind().equals("AssignmentStmt"))
                constants.remove(child.get("name"));
        visit(jmmNode.getJmmChild(0),constants);
        return null;
    }

    private Void visitIdentifier(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        if (constants.get(jmmNode.get("value")) != null) {
            JmmNode newNode = new JmmNodeImpl(constants.get(jmmNode.get("value")).getKind());
            newNode.put("value", constants.get(jmmNode.get("value")).get("value"));
            changeNode(jmmNode, newNode);
        }
        return null;
    }

    private Void visitAssign(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        visit(jmmNode.getJmmChild(0), constants);

        if(jmmNode.getJmmChild(0).getKind().equals("Integer")
                || jmmNode.getJmmChild(0).getKind().equals("Boolean")){
            constants.put(jmmNode.get("name"), jmmNode.getJmmChild(0));
        } else
            constants.remove(jmmNode.get("name"));
        return null;
    }

    private Void defaultVisit(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        visitAllChildren(jmmNode, constants);
        return null;
    }

    private Void visitMethod(JmmNode jmmNode, HashMap<String, JmmNode> constants) {
        constants = new HashMap<>();
        visitAllChildren(jmmNode, constants);
        return null;
    }

    private void changeNode(JmmNode oldNode, JmmNode newNode){
        if(oldNode.getJmmParent() == null) return;
        oldNode.getJmmParent().setChild(newNode,oldNode.getJmmParent().getChildren().indexOf(oldNode));
        changed = true;
    }

    public boolean changed() {
        return changed;
    }
}