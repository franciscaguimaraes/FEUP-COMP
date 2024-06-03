package pt.up.fe.comp2023.semanticAnalysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Method {

    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> variables;
    private boolean isStatic;

    public Method(Type returnType, List<Symbol> parameters, List<Symbol> variables, boolean isStatic) {
        this.returnType = returnType;
        this.parameters = parameters;
        this.variables = variables;
        this.isStatic = isStatic;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return variables;
    }

    public boolean isStatic() { return isStatic; }
}
