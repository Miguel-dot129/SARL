package es.upm.sarl.runtime;

import es.upm.sarl.planning.Waypoint2D;
import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.Point3D;
import es.upm.sarl.planning.SweepAreaBuilder;
import es.upm.sarl.planning.LawnmowerGenerator;
import es.upm.sarl.geometry.RectangleArea;
import es.upm.sarl.adapter.MissionAdapter;

import java.util.List;

/**
 * SyncMissionRuntime
 * ------------------
 * Núcleo de ejecución de misiones SARL en modo síncrono.
 *
 * Esta clase actúa como capa intermedia entre:
 *
 * - El Interpreter (que ejecuta el programa SARL)
 * - El estado lógico de la misión (MissionState)
 * - El backend físico o simulado que ejecuta las acciones (MissionAdapter)
 *
 * Responsabilidades principales:
 *
 * 1. Gestionar el estado de la misión mediante MissionState.
 * 2. Traducir comandos de alto nivel del lenguaje SARL a operaciones del adapter.
 * 3. Coordinar planificación de rutas y ejecución paso a paso.
 * 4. Mantener sincronizada la posición lógica con la posición real del dron.
 *
 * El runtime es "síncrono" porque:
 *
 * - Cada comando bloquea hasta completarse.
 * - El Interpreter avanza solo cuando la acción anterior ha terminado.
 *
 * Esto simplifica enormemente la semántica del lenguaje y el modelo de ejecución.
 */

public class SyncMissionRuntime {

    
    /**
     * Estado interno de la misión.
     *
     * Aquí se almacenan:
     * - configuración de misión
     * - área actual
     * - plan de waypoints
     * - progreso dentro del plan
     */
    private final MissionState state;

    /**
     * Adapter que conecta el runtime con el backend real o simulado.
     *
     * El runtime no conoce cómo se mueve el dron realmente.
     * Simplemente invoca métodos abstractos como:
     *
     * - takeoff()
     * - moveTo()
     * - hover()
     * - land()
     *
     * El adapter se encarga de traducir estas órdenes al entorno concreto
     * (Webots, simulador simple, robot real, etc.).
     */
    private final MissionAdapter adapter;

    /**
     * Constructor de áreas de trabajo.
     *
     * Se utiliza para convertir definiciones geométricas simples
     * (por ejemplo dos puntos) en un objeto RectangleArea validado.
     *
     * Parámetros configurados:
     * - margen de seguridad
     * - área máxima permitida
     */
    private final SweepAreaBuilder areaBuilder;

    /**
     * Constructor del runtime.
     *
     * Inicializa:
     * - el estado de misión
     * - el adapter
     * - el generador de áreas
     */
    public SyncMissionRuntime(MissionAdapter adapter) {
        this.state = new MissionState();
        this.adapter = adapter;

        // margen = 0.5m
        // maxArea = límite grande de seguridad
        this.areaBuilder = new SweepAreaBuilder(0.5, 1_000_000); // margen y maxArea
    }

    /**
     * Método auxiliar de logging interno del runtime.
     *
     * Permite diferenciar los mensajes de misión
     * del resto de logs del sistema.
     */
    private void log(String msg) {
        System.out.println("[MISSION] " + msg);
    }

    /**
     * Devuelve el estado actual de la misión.
     *
     * Se usa principalmente por el Interpreter para consultar
     * variables operativas como número de waypoints restantes.
     */
    public MissionState getState() {
        return state;
    }

    // ---------------------------------------------------------
    // CONFIGURACIÓN DE MISIÓN
    // ---------------------------------------------------------

    /**
     * Fija la altitud objetivo de la misión.
     *
     * Este valor se utilizará posteriormente cuando se ejecuten
     * comandos de navegación como moveTo().
     */
    public void setAltitude(double altitude) {
        state.setAltitude(altitude);
        log("Altitude set to " + altitude);
    }

    /**
     * Fija la velocidad objetivo de la misión.
     *
     * Actualmente se almacena en el estado de misión.
     * Su integración directa con el controlador físico
     * puede ampliarse en futuras versiones.
     */
    public void setSpeed(double speed) {
        state.setSpeed(speed);
        log("Speed set to " + speed);
    }

    /**
     * Define el punto "home" de la misión.
     *
     * Si aún no se conoce la posición actual del dron,
     * se inicializa también currentPosition con este valor.
     *
     * Esto permite empezar la misión con una referencia espacial clara.
     */
    public void setHome(Point3D home) {
        state.setHome(home);
        if (state.getCurrentPosition() == null) {
            state.updatePosition(home);
        }
        log("Home set to " + home);
    }

    // -------------------------
    // GEOMETRÍA
    // -------------------------

    // ---------------------------------------------------------
    // DEFINICIÓN DE GEOMETRÍA
    // ---------------------------------------------------------

    /**
     * Define el área rectangular de trabajo de la misión.
     *
     * El área se construye a partir de dos puntos opuestos
     * usando SweepAreaBuilder, que valida la geometría.
     */
    public void defineAreaRect(Point2D a, Point2D b) {
        RectangleArea area = areaBuilder.fromTwoPoints(a, b);
        state.setCurrentArea(area);

        log("Area defined from " + a + " to " + b + " => " + area);
    }

    // -------------------------
    // PLANIFICACIÓN
    // -------------------------

    // ---------------------------------------------------------
    // PLANIFICACIÓN DE MISIÓN
    // ---------------------------------------------------------

    /**
     * Genera un plan de barrido tipo "lawnmower".
     *
     * Este patrón recorre el área en líneas paralelas alternadas,
     * similar al movimiento de un cortacésped.
     *
     * Pasos:
     *
     * 1. Verifica que existe un área definida.
     * 2. Obtiene la posición actual del dron.
     * 3. Usa esa posición como pista inicial para el planificador.
     * 4. Genera la lista de waypoints mediante LawnmowerGenerator.
     * 5. Guarda el plan en MissionState.
     */
    public void planLawnmower(double laneSpacing) {

        if (state.getCurrentArea() == null) {
            throw new RuntimeException("PLAN_CORTACESPED requiere AREA_RECT definido");
        }

        Point3D pos = state.getCurrentPosition();

        if (pos == null) {
            throw new RuntimeException("No se conoce la posición actual del dron");
        }

        Point2D startHint = new Point2D(pos.x(), pos.y());

        LawnmowerGenerator planner = new LawnmowerGenerator();

        List<Waypoint2D> plan = planner.generate(
                state.getCurrentArea(),
                laneSpacing,
                startHint
        );

        state.setCurrentPlan(plan);

        log("Lawnmower plan generated: spacing=" + laneSpacing
            + ", startHint=" + startHint
            + ", waypoints=" + plan.size());
    }

    // ---------------------------------------------------------
    // EJECUCIÓN DE ACCIONES
    // ---------------------------------------------------------

    /**
     * Ordena el despegue del dron.
     *
     * Flujo:
     *
     * 1. Se solicita el despegue al adapter.
     * 2. Se espera a que el adapter termine la operación.
     * 3. Se sincroniza la posición real en MissionState.
     */
    public void takeoff() {
        log("Takeoff requested to altitude " + state.getAltitude());
        
        adapter.takeoff(state.getAltitude());
        state.updatePosition(adapter.getCurrentPosition());
        
        log("Takeoff completed. Current position: " + state.getCurrentPosition());
    }

    /**
     * Ejecuta el siguiente waypoint del plan actual.
     *
     * Flujo:
     *
     * 1. Comprueba que quedan puntos por ejecutar.
     * 2. Obtiene el waypoint actual.
     * 3. Ordena al adapter mover el dron a ese punto.
     * 4. Sincroniza la posición real.
     * 5. Avanza el índice del plan.
     */
    public void goNextPoint() {
        
        if (!state.hasNextWaypoint()) {
            throw new RuntimeException("No hay más puntos en el plan");
        }

        Waypoint2D wp = state.getCurrentWaypoint();

        int index = state.getCurrentWaypointIndex();
        int total = state.getCurrentPlan().size();

        adapter.moveTo(
                wp.x(),
                wp.y(),
                state.getAltitude()
        );

        state.updatePosition(adapter.getCurrentPosition());

        log("Waypoint reached [" + index + "/" + total + "]: realPosition=" + state.getCurrentPosition());

        state.advanceWaypoint();
    }

    /**
     * Mantiene el dron en hover durante un tiempo determinado.
     */
    public void hover(double seconds) {
        log("Hover requested for " + seconds + " s");
        
        adapter.hover(seconds);

        state.updatePosition(adapter.getCurrentPosition());

        log("Hover completed. Current position: " + state.getCurrentPosition());
    }

    /**
     * Ordena el aterrizaje del dron.
     *
     * Tras completar la operación, se actualiza la posición real.
     */
    public void land() {
        log("Landing requested");
        
        adapter.land();

        state.updatePosition(adapter.getCurrentPosition());

        log("Landing completed. Current position: " + state.getCurrentPosition());
    }

    // ---------------------------------------------------------
    // CONSULTA DE ESTADO
    // ---------------------------------------------------------

    /**
     * Devuelve cuántos waypoints quedan pendientes en el plan actual.
     *
     * Este método es utilizado por el Interpreter para evaluar
     * condiciones de control como:
     *
     * WHILE puntos_restantes > 0
     */
    public int remainingPoints() {
        return state.remainingWaypoints();
    }

}