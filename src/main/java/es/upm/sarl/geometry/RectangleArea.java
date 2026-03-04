package es.upm.sarl.geometry;

/**
 * Representa un área rectangular alineada con los ejes del plano XY.
 *
 * Importante:
 * - El rectángulo está alineado con los ejes X e Y.
 * - No admite rotación.
 * - Se define por sus límites extremos.
 *
 * Este diseño simplifica:
 * - Generación de barridos
 * - Cálculo de dimensiones
 * - Comprobaciones de límites
 */
public final class RectangleArea {

    // Límite izquierdo del rectángulo
    private final double minX;
    // Límite derecho del rectángulo
    private final double maxX;
    // Límite inferior del rectángulo
    private final double minY;
    // Límite superior del rectángulo
    private final double maxY;

    /**
     * Constructor del rectángulo.
     *
     * Se exige que:
     *  - minX <= maxX
     *  - minY <= maxY
     *
     * Esto garantiza que el rectángulo sea válido
     * y evita errores posteriores.
     */
    public RectangleArea(double minX, double maxX,
                         double minY, double maxY) {

        if (minX > maxX)
            throw new IllegalArgumentException("minX cannot be greater than maxX");

        if (minY > maxY)
            throw new IllegalArgumentException("minY cannot be greater than maxY");

        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    // Getters simples (no permiten modificar el estado)
    public double minX() { return minX; }
    public double maxX() { return maxX; }
    public double minY() { return minY; }
    public double maxY() { return maxY; }

    /**
     * Devuelve el ancho del rectángulo.
     *
     * width = maxX - minX
     */
    public double width() {
        return maxX - minX;
    }

    /**
     * Devuelve la altura del rectángulo.
     *
     * height = maxY - minY
     */
    public double height() {
        return maxY - minY;
    }

    /**
     * Devuelve un nuevo rectángulo expandido en todas direcciones
     * por un margen dado.
     *
     * Es importante notar que:
     * - NO modifica el rectángulo actual (inmutabilidad).
     * - Devuelve una nueva instancia.
     *
     * Esto es clave para evitar efectos secundarios
     * en el motor de planificación.
     *
     * @param margin cantidad de expansión en metros
     * @return nuevo RectangleArea expandido
     */
    public RectangleArea expanded(double margin) {
        if (margin < 0)
            throw new IllegalArgumentException("El amrgen tiene que ser >= 0");

        // Expandimos simétricamente en las cuatro direcciones
        return new RectangleArea(
                minX - margin,
                maxX + margin,
                minY - margin,
                maxY + margin
        );
    }

    /**
     * Representación legible del rectángulo
     * útil para depuración y logs.
     */
    @Override
    public String toString() {
        return String.format(
            "RectangleArea[minX=%.2f, maxX=%.2f, minY=%.2f, maxY=%.2f]",
            minX, maxX, minY, maxY
        );
    }
}