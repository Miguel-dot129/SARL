package es.upm.sarl.interpreter;

public final class Value {

    public enum Type { NUMBER, BOOLEAN, STRING }

    private final Type type;
    private final Object raw;

    private Value(Type type, Object raw) {
        this.type = type;
        this.raw = raw;
    }

    public static Value ofNumber(double v)   { return new Value(Type.NUMBER, v); }
    public static Value ofBoolean(boolean v) { return new Value(Type.BOOLEAN, v); }
    public static Value ofString(String v)   { return new Value(Type.STRING, v); }

    public Type type() { return type; }

    public double asNumber() {
        if (type != Type.NUMBER) throw new RuntimeException("Se esperaba NUMBER, pero era " + type);
        return (double) raw;
    }

    public boolean asBoolean() {
        if (type != Type.BOOLEAN) throw new RuntimeException("Se esperaba BOOLEAN, pero era " + type);
        return (boolean) raw;
    }

    public String asString() {
        if (type != Type.STRING) throw new RuntimeException("Se esperaba STRING, pero era " + type);
        return (String) raw;
    }

    @Override
    public String toString() {
        return raw.toString();
    }
}
