package es.upm.sarl.geometry;

/**
 * MainTestGeometry
 * ----------------
 * Programa de prueba manual para validar las clases geométricas básicas:
 *  - Point2D
 *  - RectangleArea
 *
 * Objetivo:
 *  - Verificar que el cálculo de min/max funciona correctamente.
 *  - Verificar que width() y height() devuelven valores coherentes.
 *  - Verificar que expanded(margin) amplía el rectángulo correctamente.
 *
 * Este main NO forma parte del sistema de misión.
 * Se usa únicamente para pruebas aisladas de la capa geométrica.
 */
public class MainTestGeometry {
    public static void main(String[] args) {

        // Creamos dos puntos arbitrarios del plano
        Point2D a = new Point2D(2, 5);
        Point2D b = new Point2D(10, 1);

        // Construimos un rectángulo usando min/max explícitamente.
        // Esto simula lo que hará luego SweepAreaBuilder.
        RectangleArea area = new RectangleArea(
                Math.min(a.x(), b.x()),
                Math.max(a.x(), b.x()),
                Math.min(a.y(), b.y()),
                Math.max(a.y(), b.y())
        );

        // Mostramos el rectángulo generado
        System.out.println(area);
        // Comprobamos dimensiones
        System.out.println("Width: " + area.width());
        System.out.println("Height: " + area.height());

        // Probamos expansión con margen 2.0
        RectangleArea expanded = area.expanded(2.0);
        // Mostramos resultado expandido
        System.out.println("Expanded: " + expanded);
    }
}
