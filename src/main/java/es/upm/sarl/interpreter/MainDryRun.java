package es.upm.sarl.interpreter;

import es.upm.sarl.gen.SARLLexer;
import es.upm.sarl.gen.SARLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class MainDryRun {

    public static void main(String[] args) throws Exception {

        String file = "examples/05_control_flow_condition_true_false.sarl";

        var input = CharStreams.fromFileName(file);
        var lexer = new SARLLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new SARLParser(tokens);

        var tree = parser.program(); 

        var interpreter = new Interpreter(null);
        interpreter.visit(tree);
    }
}
