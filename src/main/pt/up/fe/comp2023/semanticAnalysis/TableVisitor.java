package pt.up.fe.comp2023.semanticAnalysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

public class TableVisitor extends AJmmVisitor<SymbolTableImpl, Void> {

    private List<String> imports;
    public ArrayList<String> classes;
    public HashMap<String, Method> methods;
    private String mainClass;
    private String extension;
    public ArrayList<String> context;
    private HashMap <String, ArrayList<Symbol>> vars;

    public TableVisitor() {
        imports = new ArrayList<>();
        classes = new ArrayList<>();
        context = new ArrayList<>();
        extension = "";
        mainClass = "";
        vars = new HashMap<String, ArrayList<Symbol>>();
        methods = new HashMap<String, Method>();
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
    }

    private Void visitProgram(JmmNode jmmNode, SymbolTableImpl table) {
        context.add("global");
        vars.put("global", new ArrayList<>());
        for (JmmNode child: jmmNode.getChildren()) {
            visit(child, table);
        }
        return null;
    }

    private Void visitImport(JmmNode impNode, SymbolTableImpl table) {
        var names = (List<String>) impNode.getObject("names");
        var impS = String.join(".", names);

        table.addImport(impS);

        imports.add(names.get(names.size() - 1));

        return null;
    }

    private Void visitClassDeclaration(JmmNode jmmNode, SymbolTableImpl table) {
        ArrayList<String> valuesList = (ArrayList<String>) jmmNode.getObject("names");
        String className = valuesList.get(0);
        table.setClassName(className);
        classes.add(valuesList.get(0));
        mainClass = className;
        if(valuesList.size() > 1) {
            table.setSuper(valuesList.get(1));
            extension = valuesList.get(1);
        }
        context.add(className);
        vars.put(className, new ArrayList<>());
        vars.get(className).add(new Symbol(new Type(className, false), "this"));
        for (JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("VarDeclaration")) {
                table.addField(getSymbol(child));
            } else {
                visit(child, table);
            }
        }
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
            }
        }
        table.addMethod(methodName, returnType, parameters, variables,
                jmmNode.getAttributes().contains("isStatic") && jmmNode.get("isStatic").equals("true"));
        methods.put(methodName, new Method(returnType, parameters, variables,
                jmmNode.getAttributes().contains("isStatic") && jmmNode.get("isStatic").equals("true")));
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
            }
        }
        context.remove(context.size() - 1);

        table.addMethod(methodName, returnType, parameters, variables,
                jmmNode.getAttributes().contains("isStatic") && jmmNode.get("isStatic").equals("true"));
        methods.put(methodName, new Method(returnType, parameters, variables,
                jmmNode.getAttributes().contains("isStatic") && jmmNode.get("isStatic").equals("true")));
        return null;
    }

    private Type getType(JmmNode jmmNode) {
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
                    (getType(jmmNode.getJmmChild(0)).getName().equals(mainClass) && !extension.equals(""))){
                return new Type("AssumeType", false);
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
        for(String scope : context){
            for(Symbol var : vars.get(scope)) {
                if(var.getName().equals(jmmNode.get(getValue)))
                    return var.getType();
            }
        }
        return null;
    }

    private Symbol getSymbol(JmmNode jmmNode) {
        Type nodeType = getType(jmmNode);
        String nodeName = jmmNode.get("value");
        if(classes.contains(nodeType.getName()) || imports.contains(nodeType.getName())){
            this.vars.get(context.get(context.size() - 1)).add(new Symbol(nodeType, nodeName));
        }
        return new Symbol(nodeType, nodeName);
    }

    private boolean existsVar(JmmNode jmmNode, String getString) {
        for(String scope : context){
            for(Symbol var : vars.get(scope)) {
                if(var.getName().equals(jmmNode.get(getString)))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void addVisit(Object kind, BiFunction<JmmNode, SymbolTableImpl, Void> method) {
        super.addVisit(kind, method);
    }

    public HashMap<String, Method> getMethods() {
        return methods;
    }
}
