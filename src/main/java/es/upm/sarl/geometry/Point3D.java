package es.upm.sarl.geometry;

/**
 * Representa un punto en el espacio tridimensional (X,Y,Z).
 *
 * Se utiliza para modelar posiciones completas del dron
 * en el mundo de simulación o en una misión.
 *
 * A diferencia de Point2D, este tipo incluye la altitud (Z).
 *
 * También es inmutable porque está definido como 'record':
 * - Una vez creado, sus valores no pueden cambiar.
 * - Esto evita efectos secundarios inesperados al compartir
 *   posiciones entre distintas capas del sistema.
 */

public record Point3D(double x, double y, double z) {
    
    /**
     * Calcula la distancia horizontal (plano XY) entre dos puntos 3D.
     *
     * Se ignora la diferencia en altura (Z).
     *
     * Fórmula usada:
     *
     *   d = sqrt( (x1 - x2)^2 + (y1 - y2)^2 )
     *
     * Esta distancia es especialmente útil para:
     * - comprobar llegada a un waypoint
     * - calcular error horizontal del dron
     * - evaluar tolerancias de navegación
     *
     * @param other punto con el que se calcula la distancia
     * @return distancia horizontal entre ambos puntos
     */
    public double distanceXY(Point3D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

     /**
     * Calcula la distancia euclídea completa en el espacio 3D.
     *
     * Fórmula usada:
     *
     *   d = sqrt( (x1 - x2)^2 + (y1 - y2)^2 + (z1 - z2)^2 )
     *
     * Esta distancia puede utilizarse para:
     * - análisis de error total de navegación
     * - métricas de rendimiento del vuelo
     * - cálculos geométricos más avanzados
     *
     * @param other punto con el que se calcula la distancia
     * @return distancia espacial completa
     */
    public double distance3D(Point3D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Representación legible del punto para depuración y logs.
     *
     * Se formatea con dos decimales para facilitar la lectura
     * en consola o en archivos de registro.
     */
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}