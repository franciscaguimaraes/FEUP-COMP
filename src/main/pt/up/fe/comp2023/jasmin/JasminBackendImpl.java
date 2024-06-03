package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class JasminBackendImpl implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirRes) {


        ClassUnit classUnit = ollirRes.getOllirClass();
        try {
            classUnit.checkMethodLabels();
        } catch (OllirErrorException e) {
            return new JasminResult(classUnit.getClassName(), null, Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1, "Jasmin Exception\n", e)));
        }

        classUnit.buildCFGs();
        classUnit.buildVarTables();
        String jasminCode = new JasminBuilder().JasminBuilder(classUnit);

        if (ollirRes.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("JASMIN CODE:");
            System.out.println(jasminCode);
        }

        return new JasminResult(ollirRes, jasminCode, Collections.emptyList());
    }
}