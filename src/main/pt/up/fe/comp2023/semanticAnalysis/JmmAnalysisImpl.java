package pt.up.fe.comp2023.semanticAnalysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;

public class JmmAnalysisImpl implements JmmAnalysis {



    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SymbolTableImpl symbolTable = new SymbolTableImpl();
        ArrayList <Report> reports = new ArrayList<>();
        TableVisitor tbv = new TableVisitor();
        tbv.visit(jmmParserResult.getRootNode(), symbolTable);
        SemanticTableVisitor smtv = new SemanticTableVisitor(tbv.getMethods());
        smtv.visit(jmmParserResult.getRootNode(), symbolTable);
        reports = smtv.getReports();

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
