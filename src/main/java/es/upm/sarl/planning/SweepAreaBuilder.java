package es.upm.sarl.planning;

import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.RectangleArea;

/**
 * SweepAreaBuilder
 * ---------------
 * Clase "oficial" para construir zonas de barrido (por ahora rectángulos).
 *
 * Objetivo:
 * - A partir de 2 puntos del plano (X,Y), construir un RectangleArea válido.
 * - Aplicar un margen (por defecto o manual) para "ampliar" la zona.
 * - Asegurar que la zona final no supera un área máxima permitida (maxArea).
 *
 * Importante:
 * - Esta clase NO genera waypoints (eso lo hace el generador de barrido).
 * - Esta clase NO habla con Webots.
 * - Solo se encarga de convertir "inputs geométricos" en una zona rectangular usable.
 */
public final class SweepAreaBuilder {

    /**
     * Margen por defecto (en metros).
     * Si el usuario no indica margen, este es el que se aplica automáticamente.
     */
    private final double defaultMargin;
    /**
     * Área máxima permitida para una zona de barrido (en metros cuadrados).
     * Sirve como "guardarraíl" para evitar zonas absurdamente grandes.
     */
    private final double maxArea;

    /**
     * Constructor.
     *
     * @param defaultMargin margen por defecto (>= 0)
     * @param maxArea área máxima permitida para la zona resultante (> 0)
     *
     * Validamos los parámetros para evitar estados inválidos desde el inicio.
     */
    public SweepAreaBuilder(double defaultMargin, double maxArea) {
        if (defaultMargin < 0) {
            throw new IllegalArgumentException("defaultMargin debe ser >= 0");
        }
        if (maxArea <= 0) {
            throw new IllegalArgumentException("maxArea debe ser > 0");
        }
        this.defaultMargin = defaultMargin;
        this.maxArea = maxArea;
    }

    // Getters: exponen la configuración (útil para logs o debug)
    public double defaultMargin() { return defaultMargin; }
    public double maxArea() { return maxArea; }

    /**
     * Construye zona a partir de 2 puntos usando el margen por defecto.
     *
     * Es un "atajo" para no tener que pasar margin siempre.
     */
    public RectangleArea fromTwoPoints(Point2D a, Point2D b) {
        return fromTwoPoints(a, b, defaultMargin);
    }

    /**
     * Construye zona a partir de 2 puntos usando un margen manual.
     *
     * Flujo:
     * 1) Construir rectángulo base (sin margen), calculando min/max de X/Y
     * 2) Validar que el rectángulo base no supere el área máxima (si ya se pasa, no hay solución)
     * 3) Expandir con margen
     * 4) Si la expansión se pasa de maxArea, reducir el margen automáticamente al máximo posible
     */
    public RectangleArea fromTwoPoints(Point2D a, Point2D b, double margin) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Points cannot be null");
        }
        if (margin < 0) {
            throw new IllegalArgumentException("margin must be >= 0");
        }

        // ------------------------------------------------------------
        // 1) Rectángulo base (sin margen)
        // ------------------------------------------------------------
        // Dado que a y b pueden venir en cualquier orden (a puede estar "arriba" o "abajo"),
        // calculamos min y max para crear un rectángulo siempre válido.
        double minX = Math.min(a.x(), b.x());
        double maxX = Math.max(a.x(), b.x());
        double minY = Math.min(a.y(), b.y());
        double maxY = Math.max(a.y(), b.y());

        // Creamos el rectángulo base SIN margen
        RectangleArea base = new RectangleArea(minX, maxX, minY, maxY);

        // ------------------------------------------------------------
        // 2) Control de tamaño: ¿cabe el rectángulo base en maxArea?
        // ------------------------------------------------------------
        // Área = width * height
        double baseArea = base.width() * base.height();

        if (baseArea > maxArea) {
            // Ni siquiera sin margen cabe en el máximo permitido
            throw new IllegalArgumentException(
                "Base sweep area (" + baseArea + ") exceeds maxArea (" + maxArea + ")."
            );
        }

        // ------------------------------------------------------------
        // 3) Expandir con margen solicitado
        // ------------------------------------------------------------
        RectangleArea expanded = base.expanded(margin);
        double expandedArea = expanded.width() * expanded.height();

        // ------------------------------------------------------------
        // 4) Si el margen hace que superemos maxArea, reducimos margen automáticamente
        // ------------------------------------------------------------
        if (expandedArea > maxArea) {
            // Calcula el margen máximo que se puede aplicar sin superar maxArea
            double adjustedMargin = computeMaxMarginToFit(base.width(), base.height(), maxArea);
            // Aplicamos ese margen ajustado y devolvemos un rectángulo que cumple la restricción
            expanded = base.expanded(adjustedMargin);
        }

        // Devolvemos la zona final válida
        return expanded;
    }

    /**
     * computeMaxMarginToFit
     * ---------------------
     * Calcula el margen máximo m >= 0 tal que:
     *
     *      (w + 2m)(h + 2m) <= maxArea
     *
     * donde:
     *  - w = ancho del rectángulo base
     *  - h = alto del rectángulo base
     *
     * Interpretación:
     * - Expandir por margen m significa:
     *      nuevoWidth  = w + 2m
     *      nuevoHeight = h + 2m
     *
     * Queremos que el área expandida no exceda maxArea:
     *      (w + 2m)(h + 2m) <= maxArea
     *
     * Para hallar m, resolvemos la ecuación cuadrática:
     *      (w + 2m)(h + 2m) = maxArea
     *
     * y nos quedamos con la raíz positiva.
     */
    static double computeMaxMarginToFit(double w, double h, double maxArea) {
        // Área base
        double baseArea = w * h;
        // Si maxArea es menor o igual que el área base,
        // no se puede aplicar margen (m=0 es lo máximo)
        if (maxArea <= baseArea) {
            return 0.0;
        }

        // Resolver:
        // (w + 2m)(h + 2m) = maxArea
        // wh + 2wm + 2hm + 4m^2 = maxArea
        // 4m^2 + 2(w+h)m + (wh - maxArea) = 0

        // s = (w + h) para simplificar fórmulas
        double s = w + h;               
        double p = w * h;               

        // Calculamos el discriminante usando una forma estable:
        // m = (-s + sqrt((w-h)^2 + 4*maxArea)) / 4
        double discriminant = (w - h) * (w - h) + 4.0 * maxArea;
        double sqrt = Math.sqrt(discriminant);

        // Raíz positiva (la negativa no nos interesa porque m debe ser >= 0)
        double m = (-s + sqrt) / 4.0;
        // Robustez numérica: si por redondeos sale algo como -1e-15, devolvemos 0.
        return Math.max(0.0, m);
    }
}