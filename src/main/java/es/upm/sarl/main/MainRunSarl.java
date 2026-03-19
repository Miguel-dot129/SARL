package es.upm.sarl.main;

import es.upm.sarl.adapter.ConsoleMissionAdapter;
import es.upm.sarl.adapter.MissionAdapter;
import es.upm.sarl.gen.SARLLexer;
import es.upm.sarl.gen.SARLParser;
import es.upm.sarl.interpreter.Interpreter;
import es.upm.sarl.runtime.SyncMissionRuntime;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MainRunSarl
 * -----------
 * Punto de entrada principal para ejecutar un script SARL
 * en modo síncrono usando el adapter de consola.
 *
 * Este main se utiliza para probar:
 * - el parser ANTLR
 * - el intérprete
 * - el runtime de misión
 * - la planificación
 *
 * sin depender todavía de Webots.
 *
 * Es, por tanto, la forma más simple de ejecutar una misión SARL
 * completa sobre un backend ligero y determinista.
 */
public class MainRunSarl{

    public static void main(String[] args) throws Exception {

        /**
         * Si el usuario pasa una ruta por argumento, se usa esa.
         * En caso contrario, se toma un ejemplo por defecto.
         */
        String missionPath = (args.length > 0)
                ? args[0]
                : "examples/10_modo_sincrono_v1.sarl";

        /**
         * Leemos el contenido completo del script SARL desde disco.
         */
        String source = Files.readString(Path.of(missionPath));

        /**
         * Pipeline clásico de ANTLR:
         *
         * 1. CharStream      -> flujo de caracteres de entrada
         * 2. Lexer           -> tokenización
         * 3. TokenStream     -> flujo de tokens
         * 4. Parser          -> construcción del árbol sintáctico
         */
        CharStream input = CharStreams.fromString(source);
        SARLLexer lexer = new SARLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SARLParser parser = new SARLParser(tokens);

        /**
         * Sustituimos el manejador de errores por defecto de ANTLR
         * por uno propio que lanza una excepción clara y directa.
         *
         * Esto evita salidas de error poco amigables y facilita
         * detectar rápidamente fallos de sintaxis en scripts SARL.
         */
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new RuntimeException(
                        "Error de sintaxis en línea " + line + ":" + charPositionInLine + " -> " + msg
                );
            }
        });

        /**
         * Parseamos el programa completo.
         *
         * El resultado es el árbol sintáctico abstracto (AST)
         * que luego recorrerá el intérprete mediante el patrón Visitor.
         */
        ParseTree tree = parser.program();

        /**
         * Construimos la cadena de ejecución del sistema:
         *
         * MissionAdapter     -> backend concreto (consola)
         * SyncMissionRuntime -> semántica de misión
         * Interpreter        -> semántica del lenguaje
         */
        MissionAdapter adapter = new ConsoleMissionAdapter();
        SyncMissionRuntime runtime = new SyncMissionRuntime(adapter);
        Interpreter interpreter = new Interpreter(runtime);

        /**
         * Ejecutamos el script SARL recorriendo el árbol sintáctico.
         */
        System.out.println("=== RUN SARL START ===");
        interpreter.visit(tree);
        System.out.println("=== RUN SARL END ===");
    }
}