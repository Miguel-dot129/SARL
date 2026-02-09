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
    private final Map<String, Value> variables = new HashMap<>();

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
                .map(this::evalExpr)   // devuelve Value 
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
     * x = expr;
     * Asignación de variable
     */
    @Override
    public Void visitAssignStmt(SARLParser.AssignStmtContext ctx) {
        String varName = ctx.ID().getText();
        Value value = evalExpr(ctx.expr());

        // Si no existe, se declara; si existe, se reasigna
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

        System.out.println("IF: condition=" + ctx.condition().getText() + " => " + cond);

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

            System.out.println("WHILE: condition=" + ctx.condition().getText() + " => " + cond);

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