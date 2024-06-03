package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static pt.up.fe.comp2023.ollir.OllirUtils.*;

public class OllirBuilder extends AJmmVisitor<String, ExprToOllir> {

    private final StringBuilder ollirGeneratedCode;
    private final SymbolTable symbolTable;

    public OllirBuilder(SymbolTable symbolTable){

        this.ollirGeneratedCode = new StringBuilder();
        this.symbolTable = symbolTable;

        buildVisitor();
    }

    @Override
    protected void buildVisitor() {

        addVisit("Program", this::visitProgram); // import here also
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethod", this::visitMethod);
        addVisit("Method", this::visitMethod);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("ReturnStatement", this::visitReturnStatement);
        addVisit("Integer", this::visitInteger);
        addVisit("Boolean", this::visitBoolean);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("NewObject", this::visitNewObject);
        addVisit("AssignmentStmt", this::visitAssignment);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("BoolOp", this::visitBinaryOp);
        addVisit("ExprStmt", this::visitExprStmt);


        addVisit("Stmt", this::visitStmt);
        addVisit("This", this::visitThis);
        addVisit("Length", this::visitLength);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayAssignmentStmt", this::visitArrayAssignment);
        addVisit("IfElseStmt", this::visitIfElseStmt);
        addVisit("IntDeclaration", this::visitIntDeclaration);
        addVisit("Brackets", this::visitBrackets);
        addVisit("Denial", this::visitDenial);

        setDefaultVisit(this::visitDefault);
    }

    private ExprToOllir visitDefault(JmmNode node, String s){
        return new ExprToOllir();
    }

    @Override
    public void addVisit(Object kind, BiFunction<JmmNode, String, ExprToOllir> method) {
        super.addVisit(kind, method);
    }

    @Override
    public ExprToOllir visit(JmmNode jmmNode) {
        return super.visit(jmmNode);
    }

    public String getOllirCode() {
        return ollirGeneratedCode.toString();
    }

    private ExprToOllir visitProgram(JmmNode program, String s){

        for (String imports : symbolTable.getImports()){
            ollirGeneratedCode
                    .append("import ")
                    .append(imports)
                    .append(";\n");
        }

        for (JmmNode child : program.getChildren()) {
            if(!child.getKind().equals("Import")){
                visit(child);
            }
        }

        ollirGeneratedCode.append("}");

        return new ExprToOllir();
    }

    private ExprToOllir visitClassDeclaration(JmmNode classDeclaration, String s){

        ollirGeneratedCode.append(symbolTable.getClassName());

        if(symbolTable.getSuper() != null){
            ollirGeneratedCode
                    .append(" extends ")
                    .append(symbolTable.getSuper());
        }

        ollirGeneratedCode.append(" {\n");

        boolean hasConstructor = false;

        for (JmmNode child : classDeclaration.getChildren()) {

            if(!child.getKind().equals("VarDeclaration") && !hasConstructor){
                ollirGeneratedCode.append(emptyConstructor());
                hasConstructor = true;
            }
            visit(child, s);
        }

        return new ExprToOllir();
    }

    private ExprToOllir visitVarDeclaration(JmmNode var, String s) {

        if(!var.getJmmParent().getKind().equals("Class")){
            return new ExprToOllir();
        }

        Type type = OllirUtils.getType(var.getJmmChild(0));
        Symbol symbol = new Symbol(type, var.get("value"));

        ollirGeneratedCode
                .append(".field private ")
                .append(getCode(symbol))
                .append(";\n");

        return new ExprToOllir();
    }

    private ExprToOllir visitMethod(JmmNode method, String s) {

        String methodName;
        boolean isMainMethod = method.getKind().equals("MainMethod");

        ollirGeneratedCode.append(".method public ");
        if (!isMainMethod) {
            methodName = method.get("name");
        } else {
            ollirGeneratedCode.append("static ");
            methodName = "main";
        }
        ollirGeneratedCode.append(methodName).append("(");

        String parametersCode = getMethodParameters(methodName);
        ollirGeneratedCode.append(parametersCode).append(").").append(getCode(symbolTable.getReturnType(methodName))).append(" {\n");

        // visit all children except the last one (return statement)
        List<JmmNode> children = method.getChildren();

        for (JmmNode child : children.subList(0, children.size() - 1)) {
            if (!child.getKind().equals("Type") && !child.getKind().equals("Parameter")) {
                visit(child, methodName);
            }
        }

        JmmNode returnStatement = children.get(children.size() - 1);
        if (!returnStatement.getKind().equals("Parameter")) {
            visitReturnStatement(returnStatement, methodName);
        }

        if(symbolTable.getReturnType(methodName).getName().equals("static void") || symbolTable.getReturnType(methodName).getName().equals("void")){
            ollirGeneratedCode.append("ret.V;\n");
        }

        ollirGeneratedCode.append("}\n\n");

        return new ExprToOllir();
    }

    private ExprToOllir visitReturnStatement(JmmNode jmmNode, String methodName) {

        if(jmmNode.getKind().equals("BinaryOp")){

            ExprToOllir ollirCode = visit(jmmNode, methodName);
            ollirGeneratedCode.append(ollirCode.prefix).append("ret.")
                    .append(getCode(symbolTable.getReturnType(methodName)))
                    .append(" ").append(ollirCode.value);

            if(!ollirCode.value.contains(".")) {
                ollirGeneratedCode.append(".").append(getCode(symbolTable.getReturnType(methodName)));
            }
            ollirGeneratedCode.append(";\n");

        } else if(jmmNode.getKind().equals("This")){
            String returnType = getCode(symbolTable.getReturnType(methodName));
            ollirGeneratedCode.append("ret.").append(returnType).append(" this.").append(returnType).append(";\n");

        } else {

            ExprToOllir ollirCode = visit(jmmNode, methodName);

            if(!symbolTable.getReturnType(methodName).getName().equals("static void") && !symbolTable.getReturnType(methodName).getName().equals("void")) {

                ollirGeneratedCode.append(ollirCode.prefix);
                ollirGeneratedCode.append("ret.").append(getCode(symbolTable.getReturnType(methodName))).append(" ");
                ollirGeneratedCode.append(ollirCode.value).append(";\n");
            } else {
                ollirGeneratedCode.append(ollirCode.prefix);
            }
        }

        return new ExprToOllir();
    }

    private ExprToOllir visitBoolean(JmmNode returnNode, String s) {

        String value = returnNode.get("value");
        String ollirValue = value.equals("true") ? "1" : "0";
        String ollirType = "bool";
        return new ExprToOllir("", ollirValue + "." + ollirType);
    }

    private ExprToOllir visitInteger(JmmNode returnNode, String s) {

        return new ExprToOllir("", returnNode.get("value") + ".i32");
    }

    private ExprToOllir visitIdentifier(JmmNode identifier, String methodName){

        if(methodName == null) {
            return new ExprToOllir();
        }

        String value = identifier.get("value");
        String ollirCode = getVariableType(identifier, methodName, value);
        ExprToOllir field = getFieldExpression(identifier, value);

        if(!field.value.isEmpty() && ollirCode.isEmpty()){
            return field;
        }

        return new ExprToOllir("", ollirCode);
    }

    private ExprToOllir visitAssignment(JmmNode assignmentNode, String methodName) {

        StringBuilder ollirCode = new StringBuilder();
        ExprToOllir fieldExpr = getFieldExpression(assignmentNode, assignmentNode.get("name"));
        String type = getVariableType(assignmentNode, methodName, assignmentNode.get("name"));

        if(!fieldExpr.value.equals("") && type.equals("")){
            ExprToOllir assignmentChild = visit(assignmentNode.getJmmChild(0), methodName);
            type = extractTypeFromVar(fieldExpr.value);
            ollirGeneratedCode.append(assignmentChild.prefix);
            ollirGeneratedCode.append("putfield(this, ").append(assignmentNode.get("name")).append(".").append(type).append(", ").append(assignmentChild.value).append(").V;\n");
            return new ExprToOllir();
        }

        ExprToOllir ollirCodeRhs = visit(assignmentNode.getJmmChild(0), methodName);
        ollirGeneratedCode.append(ollirCodeRhs.prefix);
        String varType = extractTypeFromVar(type);

        if(assignmentNode.getJmmChild(0).getKind().equals("NewObject")){
            ollirGeneratedCode.append(type).append(" :=.").append(varType).append(" ").append(ollirCodeRhs.value).append(";\n");
            return new ExprToOllir(ollirCode.toString(), ollirCodeRhs.value);
        }

        ollirGeneratedCode.append(type).append(" :=.").append(varType).append(" ").append(ollirCodeRhs.value).append(";\n");

        return new ExprToOllir(ollirCode.toString(), ollirCodeRhs.value);

    }

    private ExprToOllir visitBinaryOp(JmmNode jmmNode, String s) {

        ExprToOllir ollirCodeLhs = visit(jmmNode.getJmmChild(0), s);
        ExprToOllir ollirCodeRhs = visit(jmmNode.getJmmChild(1), s);

        String operator;
        String operatorType = getOpType(jmmNode.get("op"));

        if (jmmNode.getJmmParent().getKind().equals("WhileStmt")) {
            operator = getOppositeOp(jmmNode.get("op")) + operatorType;
        } else {
            operator = jmmNode.get("op") + operatorType;
        }

        String temp = nextTemp() + operatorType;

        StringBuilder ollirCode = new StringBuilder()
                .append(ollirCodeLhs.prefix)
                .append(ollirCodeRhs.prefix)
                .append(temp).append(" :=").append(operatorType).append(" ")
                .append(ollirCodeLhs.value).append(" ")
                .append(operator).append(" ")
                .append(ollirCodeRhs.value).append(";\n");

        return new ExprToOllir(ollirCode.toString(), temp);
    }

    private ExprToOllir visitExprStmt (JmmNode exprStmt, String s){

        ExprToOllir ollirCode = visit(exprStmt.getJmmChild(0), s);

        ollirGeneratedCode.append(ollirCode.prefix);
        ollirGeneratedCode.append(ollirCode.value);

        return new ExprToOllir("", ollirCode.value);
    }

    private ExprToOllir visitMethodCall (JmmNode methodCall, String methodName) {

        StringBuilder ollirCode = new StringBuilder();
        StringBuilder prefixCode = new StringBuilder();
        ExprToOllir childCode = new ExprToOllir("", "");

        if (!methodCall.getJmmChild(0).getKind().equals("Identifier")) {
            childCode = visit(methodCall.getJmmChild(0), methodName);
        }

        ExprToOllir arguments = new ExprToOllir("", "");
        List<String> argumentsList = new ArrayList<>();
        if (methodCall.getChildren().size() > 1) {
            for (int i = 1; i < methodCall.getChildren().size(); i++) {
                JmmNode argumentChild = methodCall.getJmmChild(i);
                arguments = visit(argumentChild, methodName);
                prefixCode.append(arguments.prefix);
                argumentsList.add(arguments.value);
            }
            arguments.prefix = prefixCode.toString();
        }

        String identifierType;
        if (methodCall.getJmmChild(0).getKind().equals("Identifier")) {
            identifierType=  visit(methodCall.getJmmChild(0), methodName).value;
        }else{
            identifierType = childCode.value;
        }
        String returnType;

        if(!identifierType.equals("")){

            if(methodCall.getJmmChild(0).getKind().equals("Identifier")){
                returnType = "." + getMethodReturnFromClassNode(methodCall, methodCall.get("name"));
            } else {
                returnType = "." + extractTypeFromVar(childCode.value);
            }

            ollirCode.append("invokevirtual(").append(identifierType);

        } else {
            returnType = ".V";
            ollirCode.append("invokestatic(");
            ollirCode.append(methodCall.getJmmChild(0).get("value"));
        }

        ollirCode.append(", \"").append(methodCall.get("name")).append("\"");

        if(!arguments.value.equals("")){
            for (String s : argumentsList) {
                ollirCode.append(", ").append(s);
            }
        }
        ollirCode.append(")");

        if(!methodCall.getJmmParent().getKind().equals("AssignmentStmt")){

            if(methodCall.getJmmParent().getKind().equals("ArrayAccess")){
                ollirCode.append(returnType);
                ollirGeneratedCode.append(arguments.prefix);
                return new ExprToOllir("", ollirCode.toString());
            }

            if((!methodCall.getJmmParent().getKind().equals("ExprStmt")) && !returnType.equals(".V")){
                ollirCode.append(returnType).append(";\n");;
                String temp = nextTemp();
                childCode.value = temp + returnType;
                arguments.prefix = childCode.value +  " :=" + returnType + " " + ollirCode;

            } else {

                if(!returnType.equals(".V")){
                    returnType = "." + getOllirType(symbolTable.getReturnType(methodCall.get("name")).getName());
                }
                ollirCode.append(returnType).append(";\n");
                childCode.value = ollirCode.toString();
            }

            return new ExprToOllir(childCode.prefix + arguments.prefix, childCode.value);
        }

        JmmNode assignment = methodCall.getJmmParent();
        returnType = getVariableType(assignment, methodName, assignment.get("name"));
        ollirCode.append(".").append(extractTypeFromVar(returnType));

        return new ExprToOllir(arguments.prefix, ollirCode.toString());
    }

    private ExprToOllir visitNewObject (JmmNode jmmNode, String methodName) {

        StringBuilder ollirCode = new StringBuilder();
        String temp = nextTemp() + "." + jmmNode.get("name");

        ollirCode.append(temp).append(" :=.").append(jmmNode.get("name"));
        ollirCode.append(" new(")
                .append(jmmNode.get("name"))
                .append(").")
                .append(jmmNode.get("name")).append(";\n");
        ollirCode.append("invokespecial(").append(temp).append(", \"<init>\").V;\n");

        return new ExprToOllir(ollirCode.toString(), temp);

    }

    private ExprToOllir visitStmt(JmmNode stmt, String s){
        for(JmmNode child : stmt.getChildren()){
            visit(child, s);
        }

        return new ExprToOllir();
    }

    private ExprToOllir visitThis(JmmNode thisNode, String s){
        return new ExprToOllir("", "this");
    }

    private ExprToOllir visitLength (JmmNode lengthNode, String methodName) {

        StringBuilder ollirCode = new StringBuilder();
        ExprToOllir idValue = visit(lengthNode.getJmmChild(0), methodName);

        String temp = nextTemp() + ".i32";
        ollirCode.append(temp).append(":=.i32 arraylength(").append(idValue.value).append(").i32;\n");

        return new ExprToOllir(ollirCode.toString(), temp);
    }

    private ExprToOllir visitWhileStmt (JmmNode whileNode, String methodName) {

        ExprToOllir whileCondition = visit(whileNode.getJmmChild(0), methodName);
        ollirGeneratedCode.append(whileCondition.prefix);

        String whileLabel = nextLabel("WHILE");
        String endwhileLabel = nextLabel("ENDWHILE");

        ollirGeneratedCode.append(whileLabel).append(":\n")
                .append("if (").append(whileCondition.value).append(") goto ")
                .append(endwhileLabel).append(";\n");

        visit(whileNode.getJmmChild(1), methodName);

        ollirGeneratedCode.append("goto ").append(whileLabel).append(";\n")
                .append(endwhileLabel).append(":\n");

        return new ExprToOllir();
    }

    private ExprToOllir visitArrayAccess (JmmNode jmmNode, String methodName) {

        ExprToOllir array = visit(jmmNode.getJmmChild(1), methodName);
        ExprToOllir index = visit(jmmNode.getJmmChild(0), methodName);
        String tempArray = nextTemp() + ".i32";
        String indexType = "." + extractTypeFromVar(index.value).split("\\.")[1];
        String tempIndex = nextTemp() + indexType;

        ollirGeneratedCode.append(array.prefix);
        ollirGeneratedCode.append(tempArray).append(" :=.i32 ").append(array.value).append(";\n");

        ollirGeneratedCode.append(index.prefix);
        ollirGeneratedCode.append(tempIndex).append(" :=").append(indexType).append(" ");

        if (index.value.contains("$")) {
            ollirGeneratedCode.append(index.value.split("\\.")[1]);
        } else {
            ollirGeneratedCode.append(index.value.split("\\.")[0]);
        }

        ollirGeneratedCode.append("[").append(tempArray).append("]").append(indexType).append(";\n");

        return new ExprToOllir("", tempIndex);
    }

    private ExprToOllir visitArrayAssignment (JmmNode jmmNode, String methodName) {

        ExprToOllir arrayIndex = visit(jmmNode.getJmmChild(0), methodName);
        ExprToOllir arrayValue = visit(jmmNode.getJmmChild(1), methodName);

        String type = extractTypeFromVar(arrayValue.value);
        String nextTemp = nextTemp() + "." + type;

        ollirGeneratedCode.append(nextTemp)
                .append(" :=.").append(type).append(" ").append(arrayIndex.value).append(";\n");

        ollirGeneratedCode.append(jmmNode.get("name")).append("[").append(nextTemp).append("].")
                .append(type).append(" :=.").append(type).append(" ").append(arrayValue.value).append(";\n");

        return new ExprToOllir();
    }

    private ExprToOllir visitIfElseStmt (JmmNode jmmNode, String methodName) {

        ExprToOllir ifCondition = visit(jmmNode.getJmmChild(0), methodName);

        String thenLabel = nextLabel("THEN");
        String endifLabel = nextLabel("ENDIF");

        // if (condition) goto THEN;
        // (...)
        // goto ENDIF;
        // THEN:
        // (...)
        // ENDIF:
        // (...)

        ollirGeneratedCode.append(ifCondition.prefix);
        ollirGeneratedCode.append("if (").append(ifCondition.value).append(") goto ")
                .append(thenLabel).append(";\n");

        ExprToOllir elseCondition = visit(jmmNode.getJmmChild(2), methodName);
        ollirGeneratedCode.append(elseCondition.prefix).append("goto ").append(endifLabel).append(";\n")
                .append(thenLabel).append(":\n");

        visit(jmmNode.getJmmChild(1), methodName);
        ollirGeneratedCode.append(elseCondition.prefix);

        ollirGeneratedCode.append(endifLabel).append(":\n");

        return new ExprToOllir();
    }

    private ExprToOllir visitIntDeclaration (JmmNode jmmNode, String methodName) {

        StringBuilder ollirCode = new StringBuilder();
        String parentType = getVariableType(jmmNode.getJmmParent(), methodName, jmmNode.getJmmParent().get("name"));
        String type = "." + extractTypeFromVar(parentType);

        StringBuilder prefixOllirCode = new StringBuilder();
        String temp = nextTemp();

        ExprToOllir exprToOllir = visit(jmmNode.getJmmChild(0), methodName);

        prefixOllirCode.append(temp)
                .append(".i32 :=.i32 ")
                .append(exprToOllir.value)
                .append(";\n");

        ollirCode.append("new(array, ")
                .append(temp)
                .append(".i32)")
                .append(type);

        return new ExprToOllir(prefixOllirCode.toString(), ollirCode.toString());
    }

    private ExprToOllir visitDenial(JmmNode jmmNode, String s) {

        ExprToOllir nextNode = visit(jmmNode.getJmmChild(0), s);
        ollirGeneratedCode.append(nextNode.prefix);

        String temp = nextTemp() + ".bool";
        ollirGeneratedCode.append(temp).append(" :=.bool !.bool ").append(nextNode.value).append(";\n");

        return new ExprToOllir("", temp);
    }

    private ExprToOllir visitBrackets(JmmNode jmmNode, String s) {
        return visit(jmmNode.getJmmChild(0), s);
    }

    // ******** AUX FUNCTIONS ******** //

    private String emptyConstructor(){
        return ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n\n";
    }

    private String getMethodParameters(String methodName) {

        List<Symbol> parameters = symbolTable.getParameters(methodName);
        StringBuilder parametersCode = new StringBuilder();
        int size = parameters.size();

        for (int i = 0; i < size; i++) {
            Symbol parameter = parameters.get(i);
            parametersCode.append(parameter.getName()).append(".").append(getCode(parameter.getType()));
            if (i < size - 1) {
                parametersCode.append(", ");
            }
        }

        return parametersCode.toString();
    }

    private String getMethodReturnFromClassNode(JmmNode jmmNode, String methodName) {

        while(!jmmNode.getKind().equals("Class")){
            jmmNode = jmmNode.getJmmParent();
        }
        for(JmmNode child: jmmNode.getChildren()) {
            if (child.getKind().equals("Method") && child.get("name").equals(methodName)) {
                return getCode(symbolTable.getReturnType(child.get("name")));
            }
        }
        return "";
    }

    private int getParameterIndex(String methodName, String varName){
        List<Symbol> parameters = symbolTable.getParameters(methodName);
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).getName().equals(varName)) {
                return i + 1;
            }
        }
        return -1;
    }

    private String getVariableType(JmmNode node, String methodName, String variableName) {
        StringBuilder resultBuilder = new StringBuilder();
        JmmNode methodNode = node.getJmmParent();

        while (!methodNode.getKind().equals("Method") && !methodNode.getKind().equals("MainMethod")) {
            methodNode = methodNode.getJmmParent();
        }

        for (JmmNode childNode : methodNode.getChildren()) {
            if (childNode.getKind().equals("VarDeclaration") && childNode.get("value").equals(variableName)) {
                Type varType = OllirUtils.getType(childNode.getJmmChild(0));
                Symbol varSymbol = new Symbol(varType, childNode.getJmmChild(0).get("value"));


                resultBuilder.append(variableName).append(".").append(getCode(varSymbol).split("\\.")[1]);
                return resultBuilder.toString();
            }
        }

        int parameterIndex;
        if ((parameterIndex = getParameterIndex(methodName, variableName)) != -1) {
            Type varType = OllirUtils.getType(methodNode.getJmmChild(parameterIndex).getJmmChild(0));
            Symbol varSymbol = new Symbol(varType, methodNode.getJmmChild(parameterIndex).getJmmChild(0).get("value"));

            resultBuilder.append("$").append(parameterIndex).append(".").append(variableName).append(".").append(getCode(varSymbol).split("\\.")[1]);
            return resultBuilder.toString();
        }

        return "";
    }

    private ExprToOllir getFieldExpression(JmmNode jmmNode, String variableName) {

        while(!jmmNode.getKind().equals("Class")) {
            jmmNode = jmmNode.getJmmParent();
        }

        for(JmmNode childNode: jmmNode.getChildren()) {
            if (childNode.getKind().equals("VarDeclaration") && childNode.get("value").equals(variableName)) {
                String tempVar = nextTemp();
                Type varType = OllirUtils.getType(childNode.getJmmChild(0));
                Symbol varSymbol = new Symbol(varType, childNode.getJmmChild(0).get("value"));

                StringBuilder prefixCodeBuilder = new StringBuilder();
                prefixCodeBuilder.append(tempVar)
                        .append(".")
                        .append(getOllirType(varType.getName()))
                        .append(" :=.")
                        .append(getCode(varSymbol).split("\\.")[1])
                        .append(" getfield(this, ")
                        .append(variableName)
                        .append(".")
                        .append(getCode(varSymbol).split("\\.")[1])
                        .append(").")
                        .append(getCode(varSymbol).split("\\.")[1])
                        .append(";\n");
                return new ExprToOllir(prefixCodeBuilder.toString(), tempVar + "." + getOllirType(varType.getName()));
            }
        }
        return new ExprToOllir();
    }

}