package pt.up.fe.comp2023.semanticAnalysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

public class SemanticTableVisitor extends AJmmVisitor<SymbolTableImpl, Void> {

    private List<String> imports;
    public ArrayList<String> classes;
    public HashMap<String, Method> methods;
    private String mainClass;
    private String extension;
    public ArrayList<String> context;
    private HashMap <String, ArrayList<Symbol>> vars;
    private ArrayList<Report> reports;

    public SemanticTableVisitor(HashMap<String, Method> meths) {
        imports = new ArrayList<>();
        classes = new ArrayList<>();
        context = new ArrayList<>();
        reports = new ArrayList<>();
        extension = "";
        mainClass = "";
        vars = new HashMap<String, ArrayList<Symbol>>();
        methods = meths;
        classes.add("int");
        classes.add("String");
        classes.add("boolean");
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Method", this::visitMethod);
        addVisit("MainMethod", this::visitMainMethod);
        addVisit("Stmt", this::visitStatement);
        addVisit("IfElseStmt", this::visitConditionStatement);
        addVisit("IfStmt", this::visitConditionStatement);
        addVisit("WhileStmt", this::visitConditionStatement);
        addVisit("ExprStmt", this::visitExprStatement);
        addVisit("AssignmentStmt", this::visitAssignStatement);
        addVisit("ArrayAssignmentStmt", this::visitAssignBracketStatement);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("BoolOp", this::visitBoolOp);
        addVisit("ArrayAccess", this::visitSqrBrackets);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("This", this::visitThis);
        addVisit("Length", this::visitLength);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        setDefaultVisit(this::setDefaultVisit);
    }

    private Void visitVarDeclaration(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        getSymbol(jmmNode);
        return null;
    }

    private Void setDefaultVisit(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        return null;
    }

    private Void visitLength(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!getType(jmmNode.getJmmChild(0)).isArray())
            addSemanticReport(jmmNode.getJmmChild(0), "Cannot get length of: " + getType(jmmNode.getJmmChild(0)).getName());
        return null;
    }

    private Void visitThis(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        for(String scope : context)
            if(methods.containsKey(scope) && methods.get(scope).isStatic())
                addSemanticReport(jmmNode, "this used in static method: " + scope);
        return null;
    }

    private Void visitProgram(JmmNode jmmNode, SymbolTableImpl table) {
        context.add("global");
        vars.put("global", new ArrayList<>());
        for (JmmNode child: jmmNode.getChildren()) {
            visit(child);
        }
        return null;
    }

    private Void visitImport(JmmNode impNode, SymbolTableImpl table) {
        var names = (List<String>) impNode.getObject("names");

        imports.add(names.get(names.size() - 1));

        return null;
    }

    private Void visitClassDeclaration(JmmNode jmmNode, SymbolTableImpl table) {
        ArrayList<String> valuesList = (ArrayList<String>) jmmNode.getObject("names");
        String className = valuesList.get(0);
        classes.add(valuesList.get(0));
        mainClass = className;
        if(valuesList.size() > 1) {
            extension = valuesList.get(1);
            if(!imports.contains(extension)){
                addSemanticReport(jmmNode, "Class undeclared in extend: " + extension);
                return null;
            }
        }
        context.add(className);
        vars.put(className, new ArrayList<>());
        vars.get(className).add(new Symbol(new Type(className, false), "this"));
        visitAllChildren(jmmNode, table);
        context.remove(context.size() - 1);
        return null;
    }

    private Void visitMethod(JmmNode jmmNode, SymbolTableImpl table) {
        String methodName = jmmNode.get("name");
        Type returnType = getType(jmmNode);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();
        context.add(methodName);
        vars.put(methodName, new ArrayList<>());

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("Parameter")) {
                parameters.add(getSymbol(child));
            } else if (child.getKind().equals("VarDeclaration")) {
                variables.add(getSymbol(child));
            } else
                visit(child);
        }

        JmmNode returnNode = jmmNode.getJmmChild(jmmNode.getNumChildren() - 1);
        if(!getType(returnNode).getName().equals("++AssumeType++") && !getType(returnNode).getName().equals(returnType.getName())
                || getType(returnNode).isArray() != returnType.isArray()){
            addSemanticReport(returnNode, "Wrong return type: " + returnType.getName() + "; Returning: " + getType(returnNode).getName());
        } else if (returnNode.getKind().equals("Identifier")) {
            existsVar(returnNode, "value");
        }
        context.remove(context.size() - 1);

        return null;
    }

    private Void visitMainMethod(JmmNode jmmNode, SymbolTableImpl table) {
        String methodName = "main";
        Type returnType = new Type("static void", false);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();
        context.add(methodName);
        vars.put(methodName, new ArrayList<>());

        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("Parameter")) {
                parameters.add(getSymbol(child));
            } else if (child.getKind().equals("VarDeclaration")) {
                variables.add(getSymbol(child));
            } else
                visit(child);
        }
        context.remove(context.size() - 1);
        return null;
    }

    private Void visitStatement(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        for(JmmNode child : jmmNode.getChildren()){
            visit(child);
            if(reports.size() > 0)
                break;
        }
        return null;
    }

    private Void visitAssignBracketStatement(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!existsVar(jmmNode, "name"))
            return null;
        Type type = getType(jmmNode);
        if(!type.isArray()){
            addSemanticReport(jmmNode, "Not array: " + jmmNode.get("name"));
            return null;
        }
        Type indexType = getType(jmmNode.getJmmChild(0));
        if(!indexType.getName().equals("int") && !indexType.getName().equals("++AssumeType++")){
            addSemanticReport(jmmNode.getJmmChild(0), "Array expression not integer: " + indexType);
            return null;
        }
        visit(jmmNode.getJmmChild(0));
        if(reports.size() > 0)
            return null;
        Type assignType = getType(jmmNode.getJmmChild(1));
        if(!assignType.getName().equals("++AssumeType++") && !type.getName().equals(assignType.getName())
                && !(type.getName().equals(mainClass) && extension.equals(assignType.getName()))
                && !(type.getName().equals(extension) && mainClass.equals(assignType.getName()))
                && !(imports.contains(type.getName()) && imports.contains(assignType.getName()))){
            String message = "";
            if(type.isArray() ^ assignType.isArray())
                message = "Types don't match " + type.getName() + " - " + assignType.getName();
            else
                message = "Types don't match " + type.getName() + " is array " + type.isArray() +
                        " - " + assignType.getName() + " is array " + assignType.isArray();
            addSemanticReport(jmmNode.getJmmChild(1), message);
            return null;
        }
        visit(jmmNode.getJmmChild(1));
        return null;
    }

    private Void visitAssignStatement(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!existsVar(jmmNode, "name"))
            return null;
        visit(jmmNode.getJmmChild(0));
        if(reports.size() > 0)
            return null;
        Type type = getType(jmmNode);
        Type assignType = getType(jmmNode.getJmmChild(0));
        if(!assignType.getName().equals("++AssumeType++") && !type.equals(assignType)
                && !(type.getName().equals(mainClass) && extension.equals(assignType.getName()))
                && !(type.getName().equals(extension) && mainClass.equals(assignType.getName()))
                && !(imports.contains(type.getName()) && imports.contains(assignType.getName()))){
            String message = "";
            if(type.isArray() ^ assignType.isArray())
                message = "Types don't match " + type.getName() + " - " + assignType.getName();
            else
                message = "Types don't match " + type.getName() + " is array " + type.isArray() +
                        " - " + assignType.getName() + " is array " + assignType.isArray();
            addSemanticReport(jmmNode.getJmmChild(0), message);
            return null;
        }
        return null;
    }

    private Void visitExprStatement(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!jmmNode.getJmmChild(0).getKind().equals("MethodCall")){
            JmmNode errorNode = jmmNode.getJmmChild(0);
            addSemanticReport(errorNode, "Blank statement: " + jmmNode.getJmmChild(0));
        }
        visit(jmmNode.getJmmChild(0));
        return null;
    }

    private Void visitConditionStatement(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        Type type = getType(jmmNode.getJmmChild(0));

        if (type.isArray() || !type.getName().equals("boolean")) {
            JmmNode errorNode = jmmNode.getJmmChild(0);
            addSemanticReport(errorNode, "Condition not boolean: " + type.getName());
        }
        for (int i = 0; i < jmmNode.getNumChildren(); i++) {
            visit(jmmNode.getJmmChild(i));
        }

        return null;
    }

    private Void visitMethodCall(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        String methodName = jmmNode.get("name");
        visitAllChildren(jmmNode, symbolTable);
        if(reports.size() > 0)
            return null;
        if((getType(jmmNode.getJmmChild(0)).getName().equals(mainClass) && !extension.equals("")) ||
                imports.contains(getType(jmmNode.getJmmChild(0)).getName()) ||
                getType(jmmNode.getJmmChild(0)).getName().equals("++AssumeType++") ||
                (jmmNode.getJmmChild(0).hasAttribute("value") && imports.contains(jmmNode.getJmmChild(0).get("value")))){
            return null;
        }
        if(methods.containsKey(methodName) && getType(jmmNode.getJmmChild(0)).getName().equals(mainClass)){
            if(methods.get(methodName).getParameters().size() != jmmNode.getNumChildren() - 1){
                addSemanticReport(jmmNode, "Wrong num of parameters for method call: " + methodName);
                return null;
            }
            int i = 1;
            for (Symbol param : methods.get(methodName).getParameters()) {
                visit(jmmNode.getJmmChild(i));
                if(reports.size() > 0)
                    break;
                if(!param.getType().getName().equals(getType(jmmNode.getJmmChild(i)).getName()) ||
                    param.getType().isArray() != getType(jmmNode.getJmmChild(i)).isArray()){
                    addSemanticReport(jmmNode, "Wrong parameters for method call: " + methodName);
                    break;
                }
                i++;
            }
        } else if(!methods.containsKey(methodName)){
            addSemanticReport(jmmNode, "Method undeclared: " + methodName);
        } else if(!getType(jmmNode.getJmmChild(0)).getName().equals(mainClass))
            addSemanticReport(jmmNode, "Class has no methods: " + getType(jmmNode.getJmmChild(0)).getName());
        return null;
    }

    private void addSemanticReport(JmmNode jmmNode, String message) {
        var line = jmmNode.get("lineStart");
        var col = jmmNode.get("colStart");
        Report report = new Report(ReportType.ERROR ,Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col),
                message);
        this.reports.add(report);
    }

    private Void visitSqrBrackets(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!getType(jmmNode.getJmmChild(0)).isArray()){
            addSemanticReport(jmmNode, "Not array: " + getType(jmmNode).getName());
            return null;
        }
        visit(jmmNode.getJmmChild(0));
        visit(jmmNode.getJmmChild(1));
        if (reports.size() > 0)
            return null;

        Type indexType = getType(jmmNode.getJmmChild(1));
        if(indexType.getName().equals("++AssumeType++"))
            return null;
        if(!indexType.getName().equals("int") || indexType.isArray()){
            String message = "Array expression not integer: " + indexType.getName();
            if(indexType.getName().equals("") && jmmNode.getJmmChild(1).hasAttribute("value"))
                message = "Var not declared: " + jmmNode.getJmmChild(1).get("value");
            if(indexType.isArray())
                message = "Can't access array through another array";
            addSemanticReport(jmmNode.getJmmChild(1), message);
        }
        return null;
    }

    private Void visitBinaryOp(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        if(!getType(jmmNode.getJmmChild(0)).getName().equals("int")
                || getType(jmmNode.getJmmChild(0)).isArray()) {
            addSemanticReport(jmmNode.getJmmChild(0), "Int operation with wrong type: " + getType(jmmNode.getJmmChild(0)).getName());
        } else if(!getType(jmmNode.getJmmChild(1)).getName().equals("int")
                || getType(jmmNode.getJmmChild(1)).isArray()) {
            addSemanticReport(jmmNode.getJmmChild(1), "Int operation with wrong type: " + getType(jmmNode.getJmmChild(1)).getName());
        }
        visit(jmmNode.getJmmChild(0));
        visit(jmmNode.getJmmChild(1));
        return null;
    }

    private Void visitBoolOp(JmmNode jmmNode, SymbolTableImpl symbolTable) {
        String typeNeeded = "";
        if(jmmNode.get("op").equals("<") || jmmNode.get("op").equals(">")){
            typeNeeded = "int";
        } else if(jmmNode.get("op").equals("&&") || jmmNode.get("op").equals("||")){
            typeNeeded = "boolean";
        }
        if(!getType(jmmNode.getJmmChild(0)).getName().equals(typeNeeded) ||
                getType(jmmNode.getJmmChild(0)).isArray()){
            addSemanticReport(jmmNode.getJmmChild(0),
                    "Wrong type operation: " + getType(jmmNode.getJmmChild(0)).getName());
        } else if(!getType(jmmNode.getJmmChild(1)).getName().equals(typeNeeded)||
                getType(jmmNode.getJmmChild(1)).isArray()){
            addSemanticReport(jmmNode.getJmmChild(1),
                    "Wrong type operation: " + getType(jmmNode.getJmmChild(1)).getName());
        }

        visit(jmmNode.getJmmChild(0));
        visit(jmmNode.getJmmChild(1));

        return null;
    }

    private Type getType(JmmNode jmmNode) {
        if(jmmNode.getKind().equals("VarDeclaration")){
            Type type = new Type(jmmNode.getJmmChild(0).get("value"),
                    jmmNode.getJmmChild(0).hasAttribute("isArray") &&
                            jmmNode.getJmmChild(0).get("isArray").equals("true"));
            if(!classes.contains(type.getName()) && !imports.contains(type.getName())){
                addSemanticReport(jmmNode.getJmmChild(0), "Type not declared: " + type.getName());
            }
            return type;

        }
        if(jmmNode.hasAttribute("value") && getVarType(jmmNode, "value") != null){
            return new Type(getVarType(jmmNode, "value").getName(),
                    getVarType(jmmNode, "value").isArray() && !jmmNode.getKind().equals("ArrayAccess"));
        }
        if(jmmNode.hasAttribute("name") && getVarType(jmmNode, "name") != null)
            return new Type(getVarType(jmmNode, "name").getName(),
                    getVarType(jmmNode, "name").isArray() && !jmmNode.getKind().equals("ArrayAccess"));
        String type = "";
        boolean isArray = false;
        if (jmmNode.getKind().contains("Type")) {
            isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
            type = jmmNode.get("value");
        } else if (jmmNode.getKind().contains("BinaryOp") || jmmNode.getKind().equals("Length")
                || jmmNode.getKind().equals("Integer")){
            return new Type("int", false);
        } else if (jmmNode.getKind().equals("IntDeclaration")){
            return new Type("int", true);
        } else if (jmmNode.getKind().contains("BoolOp") || jmmNode.getKind().equals("Boolean")
                || jmmNode.getKind().contains("Denial")){
            return new Type("boolean", false);
        } else if(jmmNode.getKind().equals("NewObject")) {
            type = jmmNode.get("name");
            isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        } else if(jmmNode.getKind().equals("MethodCall")) {
            String methodName = jmmNode.get("name");
            if(methods.containsKey(methodName))
                return methods.get(methodName).getReturnType();
            else if (imports.contains(getType(jmmNode.getJmmChild(0)).getName()) ||
                    (getType(jmmNode.getJmmChild(0)).getName().equals(mainClass) && !extension.equals("")) ||
                    (jmmNode.getJmmChild(0).hasAttribute("value") && imports.contains(jmmNode.getJmmChild(0).get("value")))){
                return new Type("++AssumeType++", false);
            } else {
                addSemanticReport(jmmNode, "Method undeclared: " + methodName);
            }
        } else if(jmmNode.getNumChildren() > 0){
            Type childType;
            for(JmmNode child : jmmNode.getChildren()){
                childType = getType(child);
                if(!childType.getName().equals("")){
                    return new Type(childType.getName(),
                            childType.isArray() && !jmmNode.getKind().equals("ArrayAccess"));
                }
            }
        }
        return new Type(type, isArray);
    }

    private Type getVarType(JmmNode jmmNode, String getValue) {
        for (int i = context.size() - 1; i >= 0; i--) {
            for(Symbol var : vars.get(context.get(i))) {
                if(var.getName().equals(jmmNode.get(getValue))){
                    return var.getType();
                }
            }
        }
        return null;
    }

    private Symbol getSymbol(JmmNode jmmNode) {
        Type nodeType = getType(jmmNode);
        String nodeName = jmmNode.get("value");
        if(!classes.contains(nodeType.getName()) && !imports.contains(nodeType.getName())){
            addSemanticReport(jmmNode, "Unknown Type: " + nodeType.getName());
        } else {
            this.vars.get(context.get(context.size() - 1)).add(new Symbol(nodeType, nodeName));
        }
        return new Symbol(nodeType, nodeName);
    }

    public ArrayList<Report> getReports() {
        return reports;
    }

    private boolean existsVar(JmmNode jmmNode, String getString) {
        boolean isStatic = false;
        for(String scope : context){
            if(methods.containsKey(scope) && methods.get(scope).isStatic()){
                isStatic = true;
                break;
            }
        }
        Collections.reverse(context);
        for(String scope : context){
            for(Symbol var : vars.get(scope)) {
                if(var.getName().equals(jmmNode.get(getString))){
                    if(var.getName().equals("this") && isStatic){
                        addSemanticReport(jmmNode, "this used in static method: " + scope);
                        Collections.reverse(context);
                        return false;
                    } else if(scope.equals(mainClass) && isStatic){
                        addSemanticReport(jmmNode, "Class field modified in static method: " + var.getName());
                        Collections.reverse(context);
                        return false;
                    }
                    Collections.reverse(context);
                    return true;
                }
            }
        }
        addSemanticReport(jmmNode, "Variable not declared: " + jmmNode.get(getString));
        Collections.reverse(context);
        return false;
    }

    @Override
    public void addVisit(Object kind, BiFunction<JmmNode, SymbolTableImpl, Void> method) {
        super.addVisit(kind, method);
    }
}
