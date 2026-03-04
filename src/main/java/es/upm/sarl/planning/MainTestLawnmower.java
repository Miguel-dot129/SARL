package es.upm.sarl.planning;

import java.util.List;

import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.RectangleArea;

/**
 * MainTestLawnmower
 * -----------------
 * Programa de prueba para validar el algoritmo de barrido LawnmowerGenerator.
 *
 * Se comprueba:
 *  - Construcción de zona con SweepAreaBuilder.
 *  - Generación correcta de waypoints.
 *  - Alternancia izquierda/derecha del patrón cortacésped.
 *  - Elección correcta de esquina inicial según startHint.
 *
 * Este main simula el flujo:
 *
 *  puntos -> zona -> generador -> lista ordenada de waypoints
 *
 * Sin intervención del controlador ni de Webots.
 */
public class MainTestLawnmower {

    public static void main(String[] args) {

        // 1) Config de construcción de zona
        double defaultMargin = 2.0;
        double maxArea = 10_000.0;

        SweepAreaBuilder builder = new SweepAreaBuilder(defaultMargin, maxArea);

        // 2) Puntos para construir zona
        Point2D a = new Point2D(2, 5);
        Point2D b = new Point2D(50, 100);

        RectangleArea area = builder.fromTwoPoints(a, b);

        System.out.println("Area: " + area);
        System.out.println("Area(m^2): " + (area.width() * area.height()));

        // 3) Parámetros de barrido
        double laneSpacing = 10.0; // distancia entre filas

        // startHint simula la posición inicial del dron
        // Aquí elegimos un punto fuera de la zona para comprobar
        // que se selecciona la esquina más cercana correctamente.
        Point2D startHint = new Point2D(60, 120);

        // 4) Generar waypoints
        LawnmowerGenerator gen = new LawnmowerGenerator();
        List<Waypoint2D> wps = gen.generate(area, laneSpacing, startHint);

        System.out.println("\nWaypoints generated: " + wps.size());

        // Imprimir primeros 10 waypoints
        System.out.println("\nFirst waypoints:");
        for (int i = 0; i < Math.min(10, wps.size()); i++) {
            System.out.println("  " + wps.get(i));
        }

        // Imprimir últimos 10 waypoints
        System.out.println("\nLast waypoints:");
        for (int i = Math.max(0, wps.size() - 10); i < wps.size(); i++) {
            System.out.println("  " + wps.get(i));
        }
    }
}