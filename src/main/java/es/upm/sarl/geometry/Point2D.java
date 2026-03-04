package es.upm.sarl.geometry;

/**
 * Representa un punto en el plano horizontal (X,Y).
 *
 * Se utiliza para modelar posiciones geométricas puras,
 * sin ningún significado de misión todavía.
 *
 * Es inmutable porque está definido como 'record':
 * - Una vez creado, sus valores no pueden cambiar.
 * - Esto evita errores en planificación.
*/
public record Point2D(double x, double y) {

    /**
     * Calcula la distancia euclídea entre este punto y otro.
     *
     * Fórmula usada:
     *
     *   d = sqrt( (x1 - x2)^2 + (y1 - y2)^2 )
     *
     * Esta distancia se utiliza para:
     * - Elegir esquina más cercana
     * - Comparaciones geométricas
     * - Posibles optimizaciones futuras
     *
     * @param other punto con el que se calcula la distancia
     * @return distancia en unidades del plano (metros en nuestro caso)
    */
    public double distanceTo(Point2D other) {

        // Diferencia en el eje X
        double dx = this.x - other.x;
        // Diferencia en el eje Y
        double dy = this.y - other.y;
        // Aplicación directa de la fórmula euclídea
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Representación legible para depuración.
     * Se formatea con 2 decimales.
    */
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}