package es.upm.sarl.interpreter;

import es.upm.sarl.adapter.Adapter;
import es.upm.sarl.gen.SARLBaseVisitor;
import es.upm.sarl.gen.SARLParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interpreter SARL (V1.0): 
 * - Recorre el AST usando el patrón Visitor
 * - Mantiene un estado interno (variables)
 * - De momento solo imprime por consola (dry run)
 */
public class Interpreter extends SARLBaseVisitor<Void> {

    private final Adapter adapter;

    /**
     * Memoria del programa SARL:
     * relaciona un nombre de variable con su valor
     */
    private final Map<String, Object> variables = new HashMap<>();

    public Interpreter(Adapter adapter) {
        this.adapter = adapter;
    }

    /* 
       VISITADORES DE INSTRUCCIONES
    */

     /**
     * llamada a función: nombre(expr, expr, ...)
     */
    @Override
    public Void visitCallStmt(SARLParser.CallStmtContext ctx) {
        String funcName = ctx.ID().getText(); //obitne nombre de la funcion

        // 1) Sacamos la lista de expresiones que vienen como argumentos:
        //  - si no hay argList => lista vacía
        //  - si hay argList => la lista de ExprContext
        var exprArgs = (ctx.argList() == null) 
                ? java.util.List.<SARLParser.ExprContext>of() //extraer expresiones argumentos
                : ctx.argList().expr();

        // 2) Evaluamos cada expresión según el estado actual del mapa 'variables'
        // - si expr es "alt" => evalFactor -> variables.get("alt")
        // - si expr es "5 + 2*3" => evalExpr/Term/Factor aplican precedencia y devuelven el valor de la expr
        var values = exprArgs.stream()
                .map(this::evalExpr)   // devuelve Object (Double o String por ahora)
                .toList(); //devuelve una lista de las expresiones ya evaluadas
        
        // 3) también guardamos el texto original de los argumentos para imprimirlos por pantalla por ahora
        var rawTexts = exprArgs.stream()
                .map(SARLParser.ExprContext::getText)
                .toList();

        // 4) Renderizamos "texto=valor" para cada argumento
        StringBuilder rendered = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) rendered.append(", ");
            rendered.append(rawTexts.get(i)).append("=").append(values.get(i));
        }
        //paso 4 es simplemente para imprimir por pantalla y probar el funcionamiento
        System.out.println("CALL: " + funcName + "(" + rendered + ")");

        return null;
    }

    /**
     * let x = expr;
     * Declaración de variable
     * Semántica:
     * - Evalúa la expresión
     * - Guarda el resultado en la tabla de variables
     */
    @Override
    public Void visitLetStmt(SARLParser.LetStmtContext ctx) {
        String varName = ctx.ID().getText();

        // Evaluación semántica de la expresión
        Object value = evalExpr(ctx.expr());

        variables.put(varName, value);//añadimos a mapa variables

        System.out.println("LET: " + varName + " = " + value);
        return null;
    }

    /**
     * x = expr;
     * Asignación de variable
     */
    @Override
    public Void visitAssignStmt(SARLParser.AssignStmtContext ctx) {
        String varName = ctx.ID().getText();

        if (!variables.containsKey(varName)) {
            throw new RuntimeException("Variable no declarada: " + varName);
        }

        Object value = evalExpr(ctx.expr());

        variables.put(varName, value);

        System.out.println("ASSIGN: " + varName + " = " + value);
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
     * if (expr) { ... } else { ... }
     * Semántica V2.0:
     * - Evalúa expr (numérica)
     * - Si expr != 0 ejecuta el primer bloque
     * - Si expr == 0 y existe else, ejecuta el bloque else
     */
    @Override
    public Void visitIfStmt(SARLParser.IfStmtContext ctx) {
        Object condValue = evalExpr(ctx.expr());
        boolean cond = isTruthy(condValue);

        System.out.println("IF: condition=" + ctx.expr().getText() + " => " + condValue + " (" + cond + ")");

        if (cond) {
            visit(ctx.block(0));  // bloque del if
        } else if (ctx.block().size() > 1) {
            visit(ctx.block(1));  // bloque del else (si existe)
        }

        return null;
    }

    /**
     * while (expr) { ... }
     * Semántica V2.0:
     * - Evalúa expr antes de cada iteración
     * - Mientras expr != 0, ejecuta el bloque
     *
     * Nota: como el bloque puede modificar variables, la condición puede cambiar
     */
    @Override
    public Void visitWhileStmt(SARLParser.WhileStmtContext ctx) {
        int guard = 0; // protección simple ante bucles infinitos en dry-run 
        while (true) {
            Object condValue = evalExpr(ctx.expr());
            boolean cond = isTruthy(condValue);

            System.out.println("WHILE: condition=" + ctx.expr().getText() + " => " + condValue + " (" + cond + ")");

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
    private Object evalExpr(SARLParser.ExprContext ctx) {
        Object left = evalTerm(ctx.term(0));

        for (int i = 1; i < ctx.term().size(); i++) {
            Object right = evalTerm(ctx.term(i));

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
    private Object evalTerm(SARLParser.TermContext ctx) {
        Object left = evalFactor(ctx.factor(0));

        for (int i = 1; i < ctx.factor().size(); i++) {
            Object right = evalFactor(ctx.factor(i));
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
    private Object evalFactor(SARLParser.FactorContext ctx) {
        if (ctx.NUMBER() != null) {
            return Double.valueOf(ctx.NUMBER().getText());
        }

        if (ctx.STRING() != null) {
            return ctx.STRING().getText().replace("\"", "");
        }

        if (ctx.ID() != null) {
            String var = ctx.ID().getText();
            if (!variables.containsKey(var)) {
                throw new RuntimeException("Variable no definida: " + var);
            }
            return variables.get(var);
        }

        if (ctx.expr() != null) {
            return evalExpr(ctx.expr());
        }

        if (ctx.MINUS() != null) {
            Object value = evalFactor(ctx.factor());
            return -((Number) value).doubleValue();
        }

        throw new RuntimeException("Factor no soportado");
    }

    /**
     * Aplica un operador binario aritmético.
     *
     * Se asume (por ahora):
     * - ambos operandos son numéricos
     * - no hay comprobación de tipos avanzada 
     */
    private Object applyBinaryOp(Object a, Object b, String op) {

        double x = ((Number) a).doubleValue();
        double y = ((Number) b).doubleValue();

        return switch (op) {
            case "+" -> x + y;
            case "-" -> x - y;
            case "*" -> x * y;
            case "/" -> x / y;
            default -> throw new RuntimeException("Operador desconocido: " + op);
        };
    }

    /**
     * Convierte un valor evaluado (por ahora numérico) a booleano.
     * Regla temporal (V2.0):
     * - 0 => false
     * - cualquier otro número => true
     *
     * Más adelante (V2.x) se ampliará a booleanos reales y comparadores.
     */
    private boolean isTruthy(Object value) {
        if (value == null) return false;

        if (value instanceof Number n) {
            return n.doubleValue() != 0.0;
        }

        throw new RuntimeException("Condición no soportada (se esperaba número): " + value);
    }


}