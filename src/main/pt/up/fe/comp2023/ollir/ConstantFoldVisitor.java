package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;

public class ConstantFoldVisitor extends AJmmVisitor<String, Void> {

    private boolean changed;
    @Override
    protected void buildVisitor() {
        changed = false;
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("BoolOp", this::visitBoolOp);
        setDefaultVisit(this::defaultVisit);
    }

    private Void visitBoolOp(JmmNode jmmNode, String s) {
        return null;
    }

    private Void visitBinaryOp(JmmNode jmmNode, String s) {
        return null;
    }


    private Void defaultVisit(JmmNode jmmNode, String s) {
        visitAllChildren(jmmNode, s);
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