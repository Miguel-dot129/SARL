package es.upm.sarl.planning;

/**
 * Waypoint2D
 * ----------
 * Representa un punto objetivo (X,Y) que el dron debe visitar en orden.
 *
 * Diferencia con Point2D:
 * - Point2D es un punto "matemático" genérico.
 * - Waypoint2D es un punto con significado de misión ("ir aquí").
 *
 * De momento solo contiene:
 * - index: orden en la lista (muy útil para logs y depuración)
 * - x, y : posición en el plano horizontal
 *
 * Más adelante podría ampliarse con:
 * - yaw objetivo, velocidad, tiempo de espera, tipo de waypoint, etc.
 */
public record Waypoint2D(int index, double x, double y) {

    /**
     * Formato legible para logs.
     * Ejemplo: WP[3](52.00, 13.00)
     */
    @Override
    public String toString() {
        return String.format("WP[%d](%.2f, %.2f)", index, x, y);
    }
}