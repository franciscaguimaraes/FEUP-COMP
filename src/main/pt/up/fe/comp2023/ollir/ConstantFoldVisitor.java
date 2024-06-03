package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;

public class ConstantFoldVisitor extends AJmmVisitor<Void, Void> {

    private boolean changed;
    @Override
    protected void buildVisitor() {
        changed = false;
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("BoolOp", this::visitBoolOp);
        addVisit("Denial", this::visitDenial);
        setDefaultVisit(this::defaultVisit);
    }

    private Void visitDenial(JmmNode jmmNode, Void v) {
        visit(jmmNode.getJmmChild(0), null);
        if(jmmNode.getJmmChild(0).getKind().equals("Boolean")){
            JmmNode newNode = new JmmNodeImpl("Boolean");
            boolean value = jmmNode.get("value").equals("false");
            newNode.put("value", Boolean.toString(value));
            changeNode(jmmNode, newNode);
        }
        return null;
    }

    private Void visitBoolOp(JmmNode jmmNode, Void v) {
        visitAllChildren(jmmNode, null);
        boolean value;
        if(jmmNode.getJmmChild(0).getKind().equals("Boolean")
                && jmmNode.getJmmChild(1).getKind().equals("Boolean")) {
            value = jmmNode.getJmmChild(0).get("value").equals("true")
                    && jmmNode.getJmmChild(1).get("value").equals("true");
        } else if (jmmNode.getJmmChild(0).getKind().equals("Integer")
                && jmmNode.getJmmChild(1).getKind().equals("Integer")){
            int leftInt = Integer.parseInt(jmmNode.getJmmChild(0).get("value"));
            int rightInt = Integer.parseInt(jmmNode.getJmmChild(1).get("value"));
            value = leftInt < rightInt;
        } else
            return null;
        JmmNode newNode = new JmmNodeImpl("Boolean");
        newNode.put("value", Boolean.toString(value));
        changeNode(jmmNode, newNode);
        return null;
    }

    private Void visitBinaryOp(JmmNode jmmNode, Void v) {
        visitAllChildren(jmmNode, null);
        if(!jmmNode.getJmmChild(0).getKind().equals("Integer")
                || !jmmNode.getJmmChild(1).getKind().equals("Integer"))
            return null;
        int value;
        int leftInt = Integer.parseInt(jmmNode.getJmmChild(0).get("value"));
        int rightInt = Integer.parseInt(jmmNode.getJmmChild(1).get("value"));

        switch (jmmNode.get("op")) {
            case "+" -> value = leftInt + rightInt;
            case "-" -> value = leftInt - rightInt;
            case "*" -> value = leftInt * rightInt;
            case "/" -> value = leftInt / rightInt;
            default -> value = 0;
        }
        JmmNode newNode = new JmmNodeImpl("Integer");
        newNode.put("value", Integer.toString(value));
        changeNode(jmmNode, newNode);
        return null;
    }


    private Void defaultVisit(JmmNode jmmNode, Void v) {
        visitAllChildren(jmmNode, null);
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