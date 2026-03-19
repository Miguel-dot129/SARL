package es.upm.sarl.interpreter;

import es.upm.sarl.gen.SARLBaseVisitor;
import es.upm.sarl.gen.SARLParser;
import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.Point3D;
import es.upm.sarl.runtime.SyncMissionRuntime;

import java.util.HashMap;
import java.util.Map;

/**
 * Interpreter SARL (V3 - Modo síncrono)
 * -------------------------------------
 * Recorre el árbol sintáctico abstracto (AST) generado por ANTLR
 * usando el patrón Visitor e interpreta semánticamente cada instrucción.
 *
 * En esta versión, el intérprete ya no se limita a hacer "dry-run"
 * por consola, sino que actúa como puente entre:
 *
 *   - el programa SARL (script del usuario)
 *   - el runtime de misión síncrono
 *
 * Responsabilidades principales:
 * - Mantener la memoria de variables del programa
 * - Evaluar expresiones aritméticas y condiciones lógicas
 * - Ejecutar estructuras de control (if / while)
 * - Despachar los comandos SARL hacia SyncMissionRuntime
 *
 * Importante:
 * - El Interpreter gestiona el estado del programa (variables)
 * - El Runtime gestiona el estado de la misión (altitud, plan, waypoints, etc.)
 *
 * Esta separación permite desacoplar:
 *   lenguaje  ↔  semántica de misión  ↔  backend físico/simulado
 */
public class Interpreter extends SARLBaseVisitor<Void> {

    /**
     * Runtime de misión síncrono.
     *
     * El intérprete delega en esta clase la ejecución real
     * de los comandos SARL de alto nivel:
     * - configuración
     * - planificación
     * - ejecución de waypoints
     * - interacción con el adapter
     *
     * Es decir:
     *   Interpreter -> entiende el lenguaje
     *   Runtime     -> entiende la misión
     */
    private final SyncMissionRuntime runtime;

    /**
     * Memoria del programa SARL.
     *
     * Relaciona un nombre de variable con su valor actual.
     *
     * Ejemplos de uso:
     * - variables de control de bucles
     * - contadores
     * - resultados temporales
     * - valores obtenidos del runtime (ej. PUNTOS_RESTANTES)
     *
     * Estas variables pertenecen al programa SARL,
     * no al estado físico de la misión.
     */
    private final Map<String, Value> variables = new HashMap<>();

    /**
     * Constructor del intérprete.
     *
     * @param runtime motor de ejecución de misión al que se delegan
     *                los comandos SARL de alto nivel
     */
    public Interpreter(SyncMissionRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Helper de logging del intérprete.
     *
     * Centraliza el formato de salida para que todas las trazas
     * generadas desde esta clase tengan el mismo prefijo.
     *
     * Esto facilita distinguir en consola qué capa del sistema
     * está emitiendo cada mensaje:
     * - [INTERPRETER]
     * - [MISSION]
     * - [WEBOTS_ADAPTER]
     * - [CONTROLLER]
     */
    private void log(String msg) {
        System.out.println("[INTERPRETER] " + msg);
    }

    /* 
       VISITADORES DE INSTRUCCIONES
    */

     /**
     * Ejecuta una llamada a comando SARL.
     *
     * En versiones anteriores, una callStmt se limitaba a imprimirse
     * como dry-run por consola.
     *
     * En esta versión, la llamada se interpreta semánticamente:
     * 1) Se obtiene el nombre del comando
     * 2) Se leen y validan sus argumentos
     * 3) Se despacha al método correspondiente del runtime
     *
     * Ejemplos:
     *   ALTITUD 10;
     *   CASA (0,0,0);
     *   AREA_RECT (0,0) (20,10);
     *   PLAN_CORTACESPED 5;
     *
     * El switch actúa como una tabla explícita de comandos permitidos,
     * lo que ayuda a:
     * - cerrar el contrato del lenguaje
     * - dar errores claros
     * - evitar llamadas arbitrarias a métodos Java
     */
    @Override
    public Void visitCallStmt(SARLParser.CallStmtContext ctx) {
        // El nombre del comando se normaliza a mayúsculas
        // para evitar ambigüedades en el despacho
        String cmdName = ctx.ID().getText().toUpperCase();

        // Si no hay argumentos, usamos una lista vacía.
        // Si los hay, recuperamos todos los callArg del nodo.
        var args = (ctx.callArgs() == null)
                ? java.util.List.<SARLParser.CallArgContext>of()
                : ctx.callArgs().callArg();

        switch (cmdName) {
            case "ALTITUD" -> {
                requireArgCount(cmdName, args, 1);
                double altitude = evalExprArg(args.get(0), cmdName).asNumber();
                runtime.setAltitude(altitude);
                log("ALTITUD " + altitude);
            }

            case "VELOCIDAD" -> {
                requireArgCount(cmdName, args, 1);
                double speed = evalExprArg(args.get(0), cmdName).asNumber();
                runtime.setSpeed(speed);
                log("VELOCIDAD " + speed);
            }

            case "CASA" -> {
                requireArgCount(cmdName, args, 1);
                Point3D p = evalPoint3DArg(args.get(0), cmdName);
                runtime.setHome(p);
                log("CASA " + p);
            }

            case "AREA_RECT" -> {
                requireArgCount(cmdName, args, 2);
                Point2D a = evalPoint2DArg(args.get(0), cmdName);
                Point2D b = evalPoint2DArg(args.get(1), cmdName);
                runtime.defineAreaRect(a, b);
                log("AREA_RECT " + a + " " + b);
            }

            case "PLAN_CORTACESPED" -> {
                requireArgCount(cmdName, args, 1);
                double spacing = evalExprArg(args.get(0), cmdName).asNumber();
                runtime.planLawnmower(spacing);
                log("PLAN_CORTACESPED " + spacing);
            }

            case "DESPEGAR" -> {
                requireArgCount(cmdName, args, 0);
                runtime.takeoff();
                log("DESPEGAR");
            }

            case "IR_SIGUIENTE_PUNTO" -> {
                requireArgCount(cmdName, args, 0);
                runtime.goNextPoint();
                log("IR_SIGUIENTE_PUNTO");
            }

            case "ESPERAR" -> {
                requireArgCount(cmdName, args, 1);
                double seconds = evalExprArg(args.get(0), cmdName).asNumber();
                runtime.hover(seconds);
                log("ESPERAR " + seconds);
            }

            case "ATERRIZAR" -> {
                requireArgCount(cmdName, args, 0);
                runtime.land();
                log("ATERRIZAR");
            }

            case "PUNTOS_RESTANTES" -> {
                requireArgCount(cmdName, args, 1);

                // Este comando tiene una semántica especial:
                // no evalúa su argumento como valor, sino como
                // nombre de variable destino donde guardar el resultado.
                String varName = extractVariableName(args.get(0), cmdName);
                int remaining = runtime.remainingPoints();
                // Guardamos el valor obtenido del runtime en la memoria del programa
                variables.put(varName, Value.ofNumber(remaining));
                log("PUNTOS_RESTANTES " + varName + "=" + remaining);
            }

            default -> throw new RuntimeException("Comando no soportado: " + cmdName);
        }

        return null;
    }

    /**
     * Verifica que un comando haya recibido exactamente el número
     * de argumentos esperado.
     *
     * Esto permite:
     * - dar errores semánticos claros al usuario
     * - cerrar el contrato del lenguaje
     * - evitar ambigüedades de invocación
     *
     * @param cmdName  nombre del comando SARL
     * @param args     lista real de argumentos recibidos
     * @param expected número esperado de argumentos
     */
    private void requireArgCount(String cmdName,
                                java.util.List<SARLParser.CallArgContext> args,
                                int expected) {
        if (args.size() != expected) {
            throw new RuntimeException(
                    cmdName + " esperaba " + expected + " argumento(s), pero recibió " + args.size()
            );
        }
    }

    /**
     * Evalúa un argumento que debe ser una expresión aritmética.
     *
     * Se usa en comandos como:
     * - ALTITUD 10;
     * - VELOCIDAD 3;
     * - PLAN_CORTACESPED 5;
     * - ESPERAR 2;
     *
     * Si el argumento no es una expresión, se lanza un error semántico.
     */
    private Value evalExprArg(SARLParser.CallArgContext arg, String cmdName) {
        if (arg.expr() == null) {
            throw new RuntimeException(cmdName + " esperaba una expresión numérica");
        }
        return evalExpr(arg.expr());
    }

    /**
     * Evalúa un argumento que debe ser un punto 2D.
     *
     * Se usa en comandos que trabajan sobre geometría plana,
     * por ejemplo AREA_RECT.
     *
     * El punto se construye evaluando internamente sus dos expresiones:
     *   (expr, expr)
     */
    private Point2D evalPoint2DArg(SARLParser.CallArgContext arg, String cmdName) {
        if (arg.point2D() == null) {
            throw new RuntimeException(cmdName + " esperaba un punto 2D");
        }

        double x = evalExpr(arg.point2D().expr(0)).asNumber();
        double y = evalExpr(arg.point2D().expr(1)).asNumber();

        return new Point2D(x, y);
    }

    /**
     * Evalúa un argumento que debe ser un punto 3D.
     *
     * Se usa en comandos como CASA, donde interesa fijar
     * una posición espacial completa (X,Y,Z).
     *
     * El punto se construye evaluando internamente sus tres expresiones:
     *   (expr, expr, expr)
     */
    private Point3D evalPoint3DArg(SARLParser.CallArgContext arg, String cmdName) {
        if (arg.point3D() == null) {
            throw new RuntimeException(cmdName + " esperaba un punto 3D");
        }

        double x = evalExpr(arg.point3D().expr(0)).asNumber();
        double y = evalExpr(arg.point3D().expr(1)).asNumber();
        double z = evalExpr(arg.point3D().expr(2)).asNumber();

        return new Point3D(x, y, z);
    }

    /**
     * Extrae el nombre textual de una variable.
     *
     * Este helper se usa en comandos con semántica de "asignación implícita",
     * como por ejemplo:
     *
     *   PUNTOS_RESTANTES puntos;
     *
     * En este caso:
     * - no queremos evaluar "puntos" como expresión
     * - queremos tratarlo como identificador destino
     *
     * Por eso aquí se valida que el argumento tenga forma de identificador,
     * pero no se consulta todavía su valor en memoria.
     */
    private String extractVariableName(SARLParser.CallArgContext arg, String cmdName) {
        if (arg.expr() == null) {
            throw new RuntimeException(cmdName + " esperaba una variable");
        }

        String raw = arg.expr().getText();

        if (!raw.matches("[a-zA-Z_][a-zA-Z_0-9]*")) {
            throw new RuntimeException(cmdName + " esperaba un identificador de variable, no: " + raw);
        }

        return raw;
    }

    /**
     * x = expr;
     * Asignación de variable
     */
    @Override
    public Void visitAssignStmt(SARLParser.AssignStmtContext ctx) {
        String varName = ctx.ID().getText();
        Value value = evalExpr(ctx.expr());

        // Si no existe, se declara; si existe, se reasigna
        variables.put(varName, value);

        log("ASSIGN: " + varName + " = " + value);
        return null;
    }

    /**
     * Ejecuta un bloque { stmt* } en orden.
     * Un bloque no introduce (de momento) un nuevo ámbito de variables:
     * la memoria de variables es global al programa
     */
    @Override
    public Void visitBlock(SARLParser.BlockContext ctx) {
        for (var stmt : ctx.stmt()) {
            visit(stmt);
        }
        return null;
    }

    /**
    * if (condition) { ... } else { ... }
    * Evalúa una condición (numérica o booleana)
    * - Si la condición es verdadera, ejecuta el bloque IF
    * - Si es falsa y existe bloque ELSE, ejecuta el bloque ELSE
    *
    * La evaluación de la condición se delega a evalCondition()
     */
    @Override
    public Void visitIfStmt(SARLParser.IfStmtContext ctx) {
        boolean cond = evalCondition(ctx.condition());

        log("IF: condition=" + ctx.condition().getText() + " => " + cond);

        if (cond) {
            visit(ctx.block(0));  // bloque del if
        } else if (ctx.block().size() > 1) {
            visit(ctx.block(1));  // bloque del else (si existe)
        }

        return null;
    }

    /**
     * while (condition) { ... }
     * Semántica V2.0:
     * - Evalúa una condicion antes de cada iteración
     * - Mientras la condición sea verdadera, ejecuta el bloque
     *
     * La condición puede cambiar durante la ejecución del bloque
     */
    @Override
    public Void visitWhileStmt(SARLParser.WhileStmtContext ctx) {
        int guard = 0; // protección simple ante bucles infinitos en dry-run 
        while (true) {
            boolean cond = evalCondition(ctx.condition());

            log("WHILE: condition=" + ctx.condition().getText() + " => " + cond);

            if (!cond) break;

            visit(ctx.block());

            // Evita colgarte en pruebas si te equivocas con el ejemplo
            guard++;
            if (guard > 10_000) {
                throw new RuntimeException("Posible bucle infinito detectado (guard > 10000)");
            }
        }
        return null;
    }

    /*
       EVALUACIÓN DE EXPRESIONES
    */

    /**
     * Evalúa una expresión de tipo:
     *   term ((+|-) term)*
     *
     * Implementa la precedencia de operadores:
     * - Primero evalúa el primer término
     * - Luego aplica secuencialmente los operadores
     */
    private Value evalExpr(SARLParser.ExprContext ctx) {
        Value left = evalTerm(ctx.term(0));

        for (int i = 1; i < ctx.term().size(); i++) {
            Value right = evalTerm(ctx.term(i));

            // El operador está entre los términos en el árbol
            String op = ctx.getChild(2 * i - 1).getText();

            left = applyBinaryOp(left, right, op);
        }

        return left;
    }

     /**
     * Evalúa un término:
     *   factor ((*|/) factor)*
     *
     * Nivel intermedio de precedencia
     */
    private Value evalTerm(SARLParser.TermContext ctx) {
        Value left = evalFactor(ctx.factor(0));

        for (int i = 1; i < ctx.factor().size(); i++) {
            Value right = evalFactor(ctx.factor(i));
            String op = ctx.getChild(2 * i - 1).getText();

            left = applyBinaryOp(left, right, op);
        }

        return left;
    }

    /**
     * Evalúa un factor:
     * - número
     * - string
     * - variable
     * - expresión entre paréntesis
     * - negación unaria
     */
    private Value evalFactor(SARLParser.FactorContext ctx) {
        if (ctx.NUMBER() != null) {
            return Value.ofNumber(Double.parseDouble(ctx.NUMBER().getText()));
        }

        if (ctx.STRING() != null) {
            return Value.ofString(ctx.STRING().getText().replace("\"",""));
        }

        if (ctx.ID() != null) {
            String var = ctx.ID().getText();
            Value v = variables.get(var);
            if (v == null) throw new RuntimeException("Variable no definida: " + var);
            return v;
        }

        if (ctx.expr() != null) {
            return evalExpr(ctx.expr());
        }

        if (ctx.MINUS() != null) {
            Value v = evalFactor(ctx.factor());
            return Value.ofNumber(-v.asNumber());
        }

        throw new RuntimeException("Factor no soportado");
    }

    /*
    EVALUACIÓN DE CONDICIONES LÓGICAS (V2)
    
    Implementa la evaluación completa de expresiones booleanas
    siguiendo la precedencia definida en la gramática:
    
        NOT  >  AND  >  OR
    
    La evaluación se realiza de forma recursiva sobre el AST
    generado por ANTLR, respetando estrictamente la estructura
    sintáctica del lenguaje
    */

    /**
     * Evalúa una condición SARL
     *
     * Punto de entrada principal para la evaluación de condiciones
     * en estructuras de control (if / while)
     *
     * La condición se delega al nivel OR, que representa el operador
     * lógico de menor precedencia
     */
    private boolean evalCondition(SARLParser.ConditionContext ctx) {
        return evalCondOr(ctx.condOr());
    }

    /**
     * Evalúa una expresión lógica OR
     *
     * Semántica:
     * - Evalúa la primera subcondición
     * - Si alguna de las condiciones AND evaluadas es verdadera,
     *   el resultado final es true
     *
     * Representa el operador lógico de menor precedencia
     */
    private boolean evalCondOr(SARLParser.CondOrContext ctx) {
        boolean result = evalCondAnd(ctx.condAnd(0));

        for (int i = 1; i < ctx.condAnd().size(); i++) {
            result = result || evalCondAnd(ctx.condAnd(i));
        }

        return result;
    }

    /**
     * Evalúa una expresión lógica AND
     *
     * Semántica:
     * - Todas las subcondiciones deben evaluarse como true
     *   para que el resultado final sea true
     *
     * Tiene mayor precedencia que OR
     */
    private boolean evalCondAnd(SARLParser.CondAndContext ctx) {
        boolean result = evalCondNot(ctx.condNot(0));

        for (int i = 1; i < ctx.condNot().size(); i++) {
            result = result && evalCondNot(ctx.condNot(i));
        }

        return result;
    }

    /**
     * Evalúa el operador lógico NOT
     *
     * Semántica:
     * - Si aparece el operador NOT, se invierte el resultado
     *   de la subcondición asociada
     *
     * Es el operador lógico de mayor precedencia
     */
    private boolean evalCondNot(SARLParser.CondNotContext ctx) {
        if (ctx.NOT() != null) {
            return !evalCondNot(ctx.condNot());
        }
        return evalCondAtom(ctx.condAtom());
    }

    /**
     * Evalúa un átomo de condición
     *
     * Un átomo puede ser:
     * - un literal booleano (true / false)
     * - una subcondición entre paréntesis
     * - una comparación relacional entre expresiones
     */
    private boolean evalCondAtom(SARLParser.CondAtomContext ctx) {
        if (ctx.TRUE() != null) return true;
        if (ctx.FALSE() != null) return false;

        if (ctx.condition() != null) {
            return evalCondition(ctx.condition());
        }

        return evalComparison(ctx.comparison());
    }

    /**
     * Evalúa una comparación relacional entre dos expresiones aritméticas
     *
     * Ejemplos soportados:
     *   - x > 0
     *   - a == b
     *   - x + 8 < y + 9
     *
     * Siempre devuelve un valor booleano real
     */
    private boolean evalComparison(SARLParser.ComparisonContext ctx) {
        Value left = evalExpr(ctx.expr(0));
        Value right = evalExpr(ctx.expr(1));
        String op = ctx.compOp().getText();

        return applyComparison(left, right, op);
    }

    /**
     * Aplica un operador binario aritmético
     *
     * Reglas semánticas:
     * - Los operadores + - * / solo aceptan operandos numéricos
     * - El uso de valores booleanos produce un error semántico
     */
    private Value applyBinaryOp(Value a, Value b, String op) {

        double x = a.asNumber();
        double y = b.asNumber();

        return switch (op) {
            case "+" -> Value.ofNumber(x + y);
            case "-" -> Value.ofNumber(x - y);
            case "*" -> Value.ofNumber(x * y);
            case "/" -> Value.ofNumber(x / y);
            default -> throw new RuntimeException("Operador desconocido: " + op);
        };
    }

    /**
     * Aplica un operador de comparación entre dos valores numéricos
     *
     * Reglas semánticas:
     * - Solo se permiten comparaciones entre valores numéricos
     * - El resultado es siempre un booleano real (true / false)
     */
    private boolean applyComparison(Value a, Value b, String op) {

        double x = a.asNumber();
        double y = b.asNumber();

        return switch (op) {
            case "<"  -> x < y;
            case "<=" -> x <= y;
            case ">"  -> x > y;
            case ">=" -> x >= y;
            case "==" -> x == y;
            case "!=" -> x != y;
            default -> throw new RuntimeException("Operador de comparación desconocido: " + op);
        };
    }



}