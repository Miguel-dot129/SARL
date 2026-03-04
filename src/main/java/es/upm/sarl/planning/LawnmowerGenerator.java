package es.upm.sarl.planning;

import java.util.ArrayList;
import java.util.List;

import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.RectangleArea;

/**
 * LawnmowerGenerator
 * ------------------
 * Genera waypoints siguiendo un patrón de barrido "cortacésped" (lawnmower).
 *
 * Idea del patrón:
 * - El dron recorre la zona en líneas paralelas.
 * - Al final de cada línea, se desplaza a la siguiente "fila".
 * - La dirección alterna (ida/vuelta) para minimizar desplazamientos innecesarios.
 *
 * En esta versión:
 * - Las líneas son paralelas al eje X.
 * - Se avanza sobre Y con laneSpacing.
 *
 * Además:
 * - Elegimos como inicio la esquina del rectángulo más cercana a startHint.
 *   (startHint suele representar la posición inicial del dron).
 * - laneSpacing fijo.
 * - NO forzamos una última fila pegada al borde si no encaja exacto.
 */
public final class LawnmowerGenerator {

    /**
     * Genera una lista ordenada de Waypoint2D.
     *
     * @param area zona rectangular a barrer.
     * @param laneSpacing distancia entre filas (en el eje Y). Debe ser > 0.
     * @param startHint punto que "indica" dónde está el dron al empezar, para elegir esquina inicial.
     * @return lista de waypoints en orden de ejecución.
     */
    public List<Waypoint2D> generate(RectangleArea area, double laneSpacing, Point2D startHint) {
        if (area == null) throw new IllegalArgumentException("El area no puede ser nula");
        if (startHint == null) throw new IllegalArgumentException("startHint no puede ser nulo");
        if (laneSpacing <= 0) throw new IllegalArgumentException("laneSpacing debe ser > 0");

        // ------------------------------------------------------------
        // 1) Construimos las 4 esquinas del rectángulo
        // ------------------------------------------------------------
        Point2D c1 = new Point2D(area.minX(), area.minY()); // esquina inferior izquierda
        Point2D c2 = new Point2D(area.minX(), area.maxY()); // esquina superior izquierda
        Point2D c3 = new Point2D(area.maxX(), area.minY()); // esquina inferior derecha
        Point2D c4 = new Point2D(area.maxX(), area.maxY()); // esquina superior derecha

        // ------------------------------------------------------------
        // 2) Elegimos la esquina inicial más cercana al startHint
        // ------------------------------------------------------------
        Point2D startCorner = closestCorner(startHint, c1, c2, c3, c4);

        // ------------------------------------------------------------
        // 3) Decidimos el sentido de avance en Y
        // ------------------------------------------------------------
        // Si empezamos en minY, subimos (y += laneSpacing)
        // Si empezamos en maxY, bajamos (y -= laneSpacing)
        boolean startAtMinY = almostEqual(startCorner.y(), area.minY());

        // ------------------------------------------------------------
        // 4) Decidimos la dirección inicial en X para la primera fila
        // ------------------------------------------------------------
        // Si empezamos en minX, la primera fila va minX -> maxX
        // Si empezamos en maxX, la primera fila va maxX -> minX
        boolean startAtMinX = almostEqual(startCorner.x(), area.minX());

        // ------------------------------------------------------------
        // 5) Generación de filas (cada fila produce 2 waypoints: extremos)
        // ------------------------------------------------------------
        List<Waypoint2D> result = new ArrayList<>();
        int idx = 0;

        // y inicial depende de la esquina de inicio
        double y = startAtMinY ? area.minY() : area.maxY();
        // y final (límite) depende de si subimos o bajamos
        double yEnd = startAtMinY ? area.maxY() : area.minY();
        // paso en Y: positivo si subimos, negativo si bajamos
        double step = startAtMinY ? laneSpacing : -laneSpacing;

        // Si empezamos en minX, primera fila va izquierda->derecha
        boolean leftToRight = startAtMinX; // si empezamos en minX, la primera fila va hacia maxX

        // ------------------------------------------------------------
        // 6) Bucle principal: generamos filas mientras no nos pasemos del límite
        // ------------------------------------------------------------
        // spacing fijo, sin "fila extra" pegada al borde.
        while (startAtMinY ? (y <= yEnd) : (y >= yEnd)) {

            // En cada fila añadimos los extremos de esa línea de barrido
            if (leftToRight) {
                // Izquierda -> Derecha
                result.add(new Waypoint2D(idx++, area.minX(), y));
                result.add(new Waypoint2D(idx++, area.maxX(), y));
            } else {
                // Derecha -> Izquierda
                result.add(new Waypoint2D(idx++, area.maxX(), y));
                result.add(new Waypoint2D(idx++, area.minX(), y));
            }

            // Pasamos a la siguiente fila
            y += step;
            // Alternamos dirección en X para el patrón cortacésped
            leftToRight = !leftToRight; 
        }

        // Devolvemos el plan generado
        return result;
    }

    /**
     * Devuelve la esquina más cercana al punto 'start'.
     * Se usa para decidir por dónde empezar el barrido.
     */
    private static Point2D closestCorner(Point2D start, Point2D... corners) {
        // Inicialmente asumimos que la mejor es la primera
        Point2D best = corners[0];
        double bestDist = start.distanceTo(best);

        // Probamos el resto de esquinas y nos quedamos con la distancia mínima
        for (int i = 1; i < corners.length; i++) {
            double d = start.distanceTo(corners[i]);
            if (d < bestDist) {
                bestDist = d;
                best = corners[i];
            }
        }
        return best;
    }

    /**
     * Comparación de doubles con tolerancia.
     * Evita problemas típicos de precisión en números en coma flotante.
     */
    private static boolean almostEqual(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }
}