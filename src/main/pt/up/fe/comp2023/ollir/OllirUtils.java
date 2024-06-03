package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirUtils {

    private static int tempCounter = 0;
    private static int labelCounter = 0;

    public static String nextTemp(){
        return "t" + (tempCounter++);
    }

    public static String nextLabel(String prefix){
        return prefix + "_" + labelCounter++;
    }

    public static String getOllirType(String type){
        return switch (type) {
            case "static void" -> "V";
            case "boolean" -> "bool";
            case "int" -> "i32";
            case "Integer"  -> "i32";
            default -> type;
        };
    }

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        return new Type(jmmNode.get("value"), isArray);
    }

    public static String extractTypeFromVar(String type) {
        String[] typeSplitted = type.split("\\.");
        int lastIndex = typeSplitted.length - 1;

        if (type.contains("array")) {
            return typeSplitted[lastIndex - 1] + "." + typeSplitted[lastIndex];
        }

        return typeSplitted[lastIndex];
    }

    public static String getOppositeOp(String op) {
        return switch (op) {
            case "<" -> ">=";
            case ">" -> "<=";
            case "<=" -> ">";
            case ">=" -> "<";
            case "==" -> "!=";
            case "!=" -> "==";
            default -> "";
        };
    }

    public static String getOpType(String op) {
        return switch (op) {
            case "+", "-", "*", "/" -> ".i32";
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> ".bool";
            default -> "";
        };
    }
}

