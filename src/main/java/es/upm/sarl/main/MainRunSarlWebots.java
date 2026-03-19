package es.upm.sarl.main;

import es.upm.sarl.adapter.MissionAdapter;
import es.upm.sarl.adapter.WebotsMissionAdapter;
import es.upm.sarl.controller.Controlador;
import es.upm.sarl.gen.SARLLexer;
import es.upm.sarl.gen.SARLParser;
import es.upm.sarl.interpreter.Interpreter;
import es.upm.sarl.runtime.SyncMissionRuntime;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MainRunSarlWebots
 * -----------------
 * Punto de entrada principal para ejecutar un script SARL
 * sobre el simulador Webots.
 *
 * A diferencia de MainRunSarl, este main conecta el lenguaje
 * con el controlador real del dron en Webots mediante
 * WebotsMissionAdapter.
 *
 * Flujo general:
 *
 * 1. Se carga y parsea el script SARL.
 * 2. Se crea el controlador del dron en Webots.
 * 3. Se arranca su bucle continuo de control en un hilo separado.
 * 4. Se crea el adapter Webots.
 * 5. Se ejecuta la misión SARL sobre el simulador.
 *
 * Este main representa la conexión completa de la fase síncrona:
 *
 * SARL → Interpreter → Runtime → Adapter → Controlador → Webots
 */
public class MainRunSarlWebots {

    public static void main(String[] args) throws Exception {

        /**
         * Ruta del script SARL a ejecutar.
         * Si no se pasa argumento, se usa un ejemplo por defecto.
         */
        String missionPath = (args.length > 0)
                ? args[0]
                : "examples/10_modo_sincrono_v1.sarl";

        // 1) Leer script SARL
        String source = Files.readString(Path.of(missionPath));

        // 2) Crear parser ANTLR
        CharStream input = CharStreams.fromString(source);
        SARLLexer lexer = new SARLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SARLParser parser = new SARLParser(tokens);

         /**
         * Listener de errores sintácticos personalizado.
         *
         * En lugar de dejar que ANTLR imprima errores genéricos,
         * se lanza una excepción con línea, columna y mensaje claro.
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
         * Parseo del programa completo.
         */
        ParseTree tree = parser.program();

        // 3) Crear controlador Webots
        Controlador controller = new Controlador();

        /**
         * 4) Lanzar el bucle continuo del controlador en un hilo aparte.
         *
         * El controlador necesita estar ejecutando continuamente
         * robot.step(...) para que Webots avance y el dron responda.
         *
         * Se usa un hilo separado porque la misión SARL se ejecutará
         * en paralelo desde el hilo principal.
         */
        Thread controlThread = new Thread(controller::run);
        controlThread.setDaemon(true);
        controlThread.start();

        /**
         * 5) Dar un pequeño margen temporal para que el controlador
         * empiece a inicializar sensores y entre en régimen normal.
         */
        Thread.sleep(1000);

        /**
         * 6) Crear adapter + runtime + interpreter
         *
         * Aquí se construye la cadena completa de ejecución:
         *
         * WebotsMissionAdapter -> backend físico/simulado
         * SyncMissionRuntime   -> semántica de misión
         * Interpreter          -> semántica del lenguaje
         */
        MissionAdapter adapter = new WebotsMissionAdapter(controller);
        SyncMissionRuntime runtime = new SyncMissionRuntime(adapter);
        Interpreter interpreter = new Interpreter(runtime);

        /**
         * Ejecución real del script SARL sobre Webots.
         */
        System.out.println("=== RUN SARL WEBOTS START ===");
        interpreter.visit(tree);
        System.out.println("=== RUN SARL WEBOTS END ===");
    }
}