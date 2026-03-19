package es.upm.sarl.runtime;

import es.upm.sarl.planning.Waypoint2D;
import es.upm.sarl.geometry.Point3D;
import es.upm.sarl.geometry.RectangleArea;

import java.util.List;

/**
 * MissionState
 * ------------
 * Representa el estado interno de una misión SARL en ejecución.
 *
 * Esta clase NO interpreta el lenguaje ni ejecuta acciones físicas
 * sobre el dron. Su responsabilidad es únicamente almacenar el estado
 * lógico actual de la misión para que otras capas del sistema lo consulten
 * y actualicen de forma controlada.
 *
 * En particular, aquí se centraliza información como:
 * - configuración general de misión (altitud, velocidad, home)
 * - posición actual conocida del dron
 * - área activa de trabajo
 * - plan actual de waypoints
 * - progreso dentro de dicho plan
 *
 * Importante:
 * - El Interpreter mantiene el estado del programa SARL (variables).
 * - MissionState mantiene el estado de la misión.
 *
 * Esta separación permite distinguir claramente entre:
 *   - estado computacional del lenguaje
 *   - estado operativo de la misión
 */
public class MissionState {

    // =========================================================
    // CONFIGURACIÓN GENERAL DE MISIÓN
    // =========================================================

    /**
     * Altitud objetivo por defecto de la misión.
     *
     * Se usa como referencia general para comandos de movimiento
     * si no se especifica otra altitud más concreta.
     */
    private double altitude = 10.0;

    /**
     * Velocidad objetivo por defecto de la misión.
     *
     * En la versión actual se almacena en el estado de misión,
     * aunque su integración fina con el controlador físico
     * puede ampliarse en versiones posteriores.
     */
    private double speed = 1.0;

    /**
     * Punto de referencia "home" de la misión.
     *
     * Puede representar:
     * - punto de inicio lógico
     * - punto de retorno
     * - referencia espacial conocida de la operación
     */
    private Point3D home;

    /**
     * Posición actual conocida del dron.
     *
     * Este valor puede actualizarse:
     * - al fijar home por primera vez
     * - tras ejecutar acciones del adapter
     * - mediante lecturas reales del backend (ej. Webots)
     *
     * No representa una medición continua automática por sí sola,
     * sino el último estado de posición sincronizado con la misión.
     */
    private Point3D currentPosition;

    // =========================================================
    // GEOMETRÍA Y PLANIFICACIÓN
    // =========================================================

    /**
     * Área actual de trabajo de la misión.
     *
     * En V3 se modela como un rectángulo (RectangleArea),
     * normalmente construido a partir de dos puntos con ayuda
     * de SweepAreaBuilder.
     *
     * Esta área es la base geométrica sobre la que luego se genera
     * un plan de barrido.
     */
    private RectangleArea currentArea;

    /**
     * Plan actual de misión expresado como lista ordenada de waypoints.
     *
     * Cada waypoint representa un punto objetivo del recorrido.
     * El orden de la lista define el orden de ejecución.
     *
     * En la versión actual, el plan suele generarse mediante
     * LawnmowerGenerator, pero MissionState no depende del algoritmo
     * concreto: solo almacena el resultado.
     */
    private List<Waypoint2D> currentPlan;

    /**
     * Índice del waypoint actual a ejecutar.
     *
     * Este índice apunta al siguiente waypoint pendiente dentro del plan.
     *
     * Ejemplo:
     * - si currentWaypointIndex = 0, todavía no se ha ejecutado ninguno
     * - si currentWaypointIndex = 3, el siguiente waypoint a ejecutar es el cuarto
     *
     * El índice avanza únicamente cuando la misión considera completado
     * el waypoint actual.
     */
    private int currentWaypointIndex = 0;

    // -------------------------
    // Configuración
    // -------------------------

    /**
     * Devuelve la altitud objetivo actual de la misión.
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * Actualiza la altitud objetivo por defecto de la misión.
     */
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Point3D getHome() {
        return home;
    }

    public void setHome(Point3D home) {
        this.home = home;
    }

    public Point3D getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Actualiza la altitud objetivo por defecto de la misión.
     */
    public void updatePosition(Point3D pos) {
        this.currentPosition = pos;
    }

    // -------------------------
    // Área
    // -------------------------

    /**
     * Devuelve el área actual de trabajo de la misión.
     */
    public RectangleArea getCurrentArea() {
        return currentArea;
    }

    /**
     * Fija el área actual de trabajo de la misión.
     */
    public void setCurrentArea(RectangleArea currentArea) {
        this.currentArea = currentArea;
    }

    // -------------------------
    // Plan
    // -------------------------

    /**
     * Devuelve el plan actual de waypoints.
     */
    public List<Waypoint2D> getCurrentPlan() {
        return currentPlan;
    }

    /**
     * Fija un nuevo plan de misión.
     *
     * Al cargar un nuevo plan:
     * - se sustituye la lista anterior
     * - el índice del waypoint actual se reinicia a 0
     *
     * Esto garantiza que el nuevo plan se ejecuta desde el principio.
     */
    public void setCurrentPlan(List<Waypoint2D> plan) {
        this.currentPlan = plan;
        this.currentWaypointIndex = 0;
    }

    /**
     * Indica si existe actualmente un plan válido y no vacío.
     */

    public boolean hasPlan() {
        return currentPlan != null && !currentPlan.isEmpty();
    }

    // -------------------------
    // Waypoints
    // -------------------------

    /**
     * Indica si todavía queda al menos un waypoint pendiente de ejecutar.
     *
     * Devuelve true si:
     * - existe un plan válido
     * - el índice actual todavía está dentro del rango de la lista
     */
    public boolean hasNextWaypoint() {
        return hasPlan() && currentWaypointIndex < currentPlan.size();
    }

    /**
     * Avanza al siguiente waypoint del plan.
     *
     * Solo incrementa el índice si todavía existe un waypoint pendiente.
     * Esto evita pasarse del final de la lista por error.
     */
    public void advanceWaypoint() {
        if (hasNextWaypoint()) {
            currentWaypointIndex++;
        }
    }

    /**
     * Devuelve cuántos waypoints quedan pendientes en el plan actual.
     *
     * Si no hay plan activo, devuelve 0.
     */
    public int remainingWaypoints() {
        if (!hasPlan()) {
            return 0;
        }
        return currentPlan.size() - currentWaypointIndex;
    }

    /**
     * Devuelve el índice actual del plan.
     *
     * Este índice representa el waypoint que tocaría ejecutar a continuación.
     */
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    /**
     * Devuelve el waypoint actual pendiente de ejecutar.
     *
     * Si no hay plan o ya no quedan waypoints, devuelve null.
     *
     * Importante:
     * este método devuelve el waypoint "activo" actual,
     * no un waypoint futuro arbitrario.
     */
    public Waypoint2D getCurrentWaypoint() {
        if (!hasNextWaypoint()) {
            return null;
        }
        return currentPlan.get(currentWaypointIndex);
    }

    //Para depuracion
    @Override
    public String toString() {
        return "MissionState{" +
                "altitude=" + altitude +
                ", speed=" + speed +
                ", home=" + home +
                ", currentPosition=" + currentPosition +
                ", currentArea=" + currentArea +
                ", currentWaypointIndex=" + currentWaypointIndex +
                ", remainingWaypoints=" + remainingWaypoints() +
                '}';
    }

}
