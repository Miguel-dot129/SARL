package es.upm.sarl.interpreter;

import java.nio.file.Files;
import java.nio.file.Path;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import es.upm.sarl.gen.*;
import es.upm.sarl.adapter.Adapter;

public class Runner {
    public static void runScript(String scriptPath, Adapter adapter) throws Exception {
        String code = Files.readString(Path.of(scriptPath));
        CharStream input = CharStreams.fromString(code);
        SARLLexer lexer = new SARLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SARLParser parser = new SARLParser(tokens);
        ParseTree tree = parser.program();
        new Interpreter(adapter).visit(tree);
    }
}
