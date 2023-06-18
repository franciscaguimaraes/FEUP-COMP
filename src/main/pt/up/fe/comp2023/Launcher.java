package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.jasmin.JasminBackendImpl;
import pt.up.fe.comp2023.ollir.JmmOllirImpl;
import pt.up.fe.comp2023.semanticAnalysis.JmmAnalysisImpl;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // ... add remaining stages
        JmmAnalysisImpl analyser =  new JmmAnalysisImpl();
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        System.out.println(analysisResult.getSymbolTable().print());
        TestUtils.noErrors(analysisResult.getReports());



        JmmOllirImpl optimizer = new JmmOllirImpl();
        analysisResult = optimizer.optimize(analysisResult);
        TestUtils.noErrors(analysisResult);
        OllirResult ol = optimizer.toOllir(analysisResult);
        TestUtils.noErrors(ol);
        ol = optimizer.optimize(ol);
        TestUtils.noErrors(ol);
        System.out.println("Ollir code: \n");
        System.out.println(ol.getOllirCode());

        JasminBackendImpl implementedJasminBackend = new JasminBackendImpl();
        JasminResult jasminResult = implementedJasminBackend.toJasmin(ol);
        jasminResult.compile();
        System.out.println("\n\nJasmin code: \n");
        jasminResult.run();
        TestUtils.noErrors(jasminResult);
        System.out.println(jasminResult.getJasminCode());

    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Create config: jmm [-r=<num>] [-o] [-d] -i=<input file.jmm>
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", "");
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // parseArgs
        for (String arg : args) {
            String[] word = arg.split("=");

            if (word.length < 1) {
                throw new RuntimeException("Unknown argument: " + arg);
            }

            switch (word[0]) {
                case "-r" -> {
                    if (word.length != 2) {
                        throw new RuntimeException("-r requires an argument (integer >= -1)");
                    }
                    try {
                        if (Integer.parseInt(word[1]) < -1) {
                            throw new RuntimeException("-r requires an integer >= -1");
                        }
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("-r requires an integer >= -1");
                    }
                    config.put("registerAllocation", word[1]);
                }
                case "-o" -> {
                    if (word.length != 1) {
                        throw new RuntimeException("-o does not require an argument");
                    }
                    config.put("optimize", "true");
                }
                case "-d" -> {
                    if (word.length != 1) {
                        throw new RuntimeException("-d does not require an argument");
                    }
                    config.put("debug", "true");
                }
                case "-i" -> {
                    if (word.length != 2) {
                        throw new RuntimeException("-i requires an argument (input file)");
                    }
                    config.put("inputFile", word[1]);
                }
                default -> throw new RuntimeException("Unknown argument: " + arg);
            }
        }

        return config;
    }
}
