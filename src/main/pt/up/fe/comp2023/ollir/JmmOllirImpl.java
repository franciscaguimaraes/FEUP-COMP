package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.semanticAnalysis.TableVisitor;

import java.util.Collections;
import java.util.HashMap;

public class JmmOllirImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        OllirBuilder ollirBuilder = new OllirBuilder(jmmSemanticsResult.getSymbolTable());
        ollirBuilder.visit(jmmSemanticsResult.getRootNode());
        String ollirCode = ollirBuilder.getOllirCode();

        return new OllirResult(jmmSemanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (semanticsResult.getConfig().getOrDefault("optimize", "false").equals("false"))
            return semanticsResult;
        ConstantPropVisitor cpVisitor = new ConstantPropVisitor();
        ConstantFoldVisitor cfVisitor = new ConstantFoldVisitor();
        cpVisitor.visit(semanticsResult.getRootNode(), new HashMap<>());
        cfVisitor.visit(semanticsResult.getRootNode(), "");
        while (cpVisitor.changed() || cfVisitor.changed()){
            cpVisitor = new ConstantPropVisitor();
            cfVisitor = new ConstantFoldVisitor();
            cpVisitor.visit(semanticsResult.getRootNode(), new HashMap<>());
            cfVisitor.visit(semanticsResult.getRootNode(), "");
        }
        return semanticsResult;
    }
}