package es.upm.sarl.interpreter;

import es.upm.sarl.adapter.Adapter;
import es.upm.sarl.gen.SARLBaseVisitor;
import es.upm.sarl.gen.SARLParser;

public class Interpreter extends SARLBaseVisitor<Void> {
    private final Adapter adapter;
    public Interpreter(Adapter adapter) { this.adapter = adapter; }

    @Override
    public Void visitCommand(SARLParser.CommandContext ctx) {
        if (ctx.TAKEOFF() != null) adapter.takeoff();
        else if (ctx.LAND() != null) adapter.land();
        else if (ctx.GOTO() != null) {
            double x = Double.parseDouble(ctx.INT(0).getText());
            double y = Double.parseDouble(ctx.INT(1).getText());
            adapter.gotoXY(x, y);
        }
        return null;
    }
}

