package es.upm.sarl.adapter;

import es.upm.sarl.controller.Controlador;
import es.upm.sarl.geometry.Point3D;

/**
 * WebotsMissionAdapter
 * --------------------
 * Implementación de MissionAdapter conectada al controlador real
 * del dron dentro de Webots.
 *
 * Esta clase actúa como puente entre:
 *
 * - el runtime abstracto de misión (SyncMissionRuntime)
 * - el controlador físico/simulado del dron en Webots
 *
 * Su responsabilidad es traducir las operaciones de alto nivel
 * del runtime:
 *
 * - takeoff(...)
 * - moveTo(...)
 * - hover(...)
 * - land()
 *
 * en acciones reales sobre el Controlador.
 *
 * Además, esta implementación introduce semántica síncrona real:
 *
 * - no devuelve el control al runtime hasta que la acción física
 *   ha finalizado
 *
 * Por eso, a diferencia de ConsoleMissionAdapter:
 *
 * - aquí sí existe espera real
 * - aquí sí existe comprobación de llegada
 * - aquí sí existe timeout
 * - aquí sí se consulta continuamente la posición del dron
 *
 * Este adapter es, por tanto, la pieza que permite que una misión
 * SARL se ejecute sobre Webots como una secuencia síncrona de acciones.
 */
public class WebotsMissionAdapter implements MissionAdapter {

    
    /**
     * Controlador real del dron dentro de Webots.
     *
     * Delegamos en Controlador la gestión de:
     * - PIDs
     * - motores
     * - sensores
     * - yaw
     * - aterrizaje
     *
     * WebotsMissionAdapter se limita a orquestar acciones de alto nivel
     * y a esperar hasta que dichas acciones se completen.
     */
    private final Controlador controller;

    // =========================================================
    // PARÁMETROS DE SINCRONIZACIÓN
    // =========================================================

    /**
     * Tolerancia horizontal de llegada (plano XY), en metros.
     *
     * Se considera que el dron está "dentro del objetivo"
     * si la distancia horizontal al target es menor o igual que este valor.
     */
    private final double posTol;

    /**
     * Tolerancia vertical de llegada (eje Z), en metros.
     *
     * Permite aceptar pequeñas desviaciones de altitud
     * sin considerar que la maniobra ha fallado.
     */
    private final double altTol;

    /**
     * Tiempo mínimo durante el cual el dron debe permanecer
     * dentro del umbral de llegada para confirmar que realmente ha llegado.
     *
     * Esto evita falsos positivos cuando el dron cruza momentáneamente
     * por la zona objetivo pero todavía está oscilando.
     */
    private final double stableSec;

    /**
     * Tiempo máximo de espera para una acción bloqueante.
     *
     * Si el dron no alcanza el objetivo dentro de este tiempo,
     * se considera que la maniobra ha fallado y se lanza una excepción.
     */
    private final double timeoutSec;

    /**
     * Constructor por defecto.
     *
     * Usa tolerancias y tiempos razonables para las pruebas actuales:
     * - 0.50 m en XY
     * - 0.35 m en Z
     * - 0.60 s de estabilidad
     * - 60 s de timeout
     */
    public WebotsMissionAdapter(Controlador controller) {
        this(controller, 0.50, 0.35, 0.60, 60.0);
    }

    /**
     * Constructor configurable.
     *
     * Permite ajustar tolerancias y tiempos de sincronización
     * según el comportamiento del dron o el escenario de simulación.
     */
    public WebotsMissionAdapter(Controlador controller,
                                double posTol,
                                double altTol,
                                double stableSec,
                                double timeoutSec) {
        this.controller = controller;
        this.posTol = posTol;
        this.altTol = altTol;
        this.stableSec = stableSec;
        this.timeoutSec = timeoutSec;
    }

    /**
     * Helper de logging del adapter.
     *
     * Se incluye timestamp para poder ver claramente:
     * - cuándo empieza una acción
     * - cuándo termina
     * - cuánto tiempo ha tardado
     *
     * Esto es especialmente útil para demostrar que el runtime
     * está funcionando de forma realmente síncrona.
     */
    private void log(String msg) {
        System.out.printf("[WEBOTS_ADAPTER][%d] %s%n",
                System.currentTimeMillis(), msg);
    }

    /**
     * Ejecuta un despegue real en Webots.
     *
     * Flujo:
     * 1. Registra el inicio del comando.
     * 2. Ordena al controlador despegar.
     * 3. Espera bloqueando hasta alcanzar la altitud objetivo.
     * 4. Registra la posición final alcanzada.
     *
     * Como el despegue en este controlador es vertical,
     * usamos la posición XY actual como referencia de llegada
     * y solo cambiamos la altitud objetivo.
     */
    @Override
    public void takeoff(double altitude) {

        log(String.format("TAKEOFF start targetAlt=%.2f", altitude));

        controller.takeoff(altitude);

        Point3D startPos = getCurrentPosition();

        waitUntilArrived(startPos.x(), startPos.y(), altitude);

        Point3D p = getCurrentPosition();

        log(String.format(
                "TAKEOFF reached alt=%.2f realPos=%s",
                p.z(), p));
    }

    /**
     * Ejecuta un desplazamiento real en Webots.
     *
     * Flujo:
     * 1. Registra el inicio del movimiento.
     * 2. Ordena al controlador moverse al objetivo.
     * 3. Espera bloqueando hasta confirmar llegada estable.
     * 4. Calcula y registra el error real final respecto al target.
     *
     * Este método es uno de los puntos clave del modo síncrono:
     * el runtime no continúa hasta que el dron realmente ha llegado.
     */
    @Override
    public void moveTo(double x, double y, double z) {

        log(String.format(
                "MOVE start target=(%.2f, %.2f, %.2f)",
                x, y, z));

        long start = System.currentTimeMillis();

        controller.moveTo(x, y, z);

        waitUntilArrived(x, y, z);

        Point3D p = getCurrentPosition();

        double dx = x - p.x();
        double dy = y - p.y();
        double dz = z - p.z();

        double errXY = Math.sqrt(dx * dx + dy * dy);
        double errZ = Math.abs(dz);

        double elapsed = (System.currentTimeMillis() - start) / 1000.0;

        log(String.format(
                "MOVE reached real=(%.2f, %.2f, %.2f) errXY=%.3f errZ=%.3f elapsed=%.2fs",
                p.x(), p.y(), p.z(),
                errXY, errZ, elapsed));
    }

    /**
     * Mantiene el dron en hover durante un tiempo dado.
     *
     * Flujo:
     * 1. Fija el objetivo actual de hover en el controlador.
     * 2. Espera el tiempo especificado.
     * 3. Registra el tiempo real transcurrido.
     *
     * En esta versión, el hover es síncrono y bloqueante:
     * el runtime no continúa hasta completar la espera.
     */
    @Override
    public void hover(double seconds) {

        log(String.format("HOVER start seconds=%.2f", seconds));

        long start = System.currentTimeMillis();

        controller.hoverHere();

        sleepSeconds(seconds);

        double elapsed = (System.currentTimeMillis() - start) / 1000.0;

        log(String.format("HOVER end elapsed=%.2fs", elapsed));
    }

    /**
     * Ejecuta un aterrizaje real en Webots.
     *
     * Flujo:
     * 1. Registra el inicio del aterrizaje.
     * 2. Ordena al controlador iniciar el descenso.
     * 3. Espera hasta detectar que el dron ya está en el suelo.
     * 4. Registra el tiempo total empleado.
     */
    @Override
    public void land() {

        log("LAND start");

        long start = System.currentTimeMillis();

        controller.land();

        waitUntilLanded();

        double elapsed = (System.currentTimeMillis() - start) / 1000.0;

        log(String.format("LAND completed elapsed=%.2fs", elapsed));
    }

    /**
     * Devuelve la posición actual real del dron.
     *
     * Este método traduce el formato expuesto por el controlador
     * (array de doubles) al tipo geométrico Point3D usado por SARL.
     */
    @Override
    public Point3D getCurrentPosition() {
        double[] pos = controller.getCurrentPosition();
        return new Point3D(pos[0], pos[1], pos[2]);
    }

    /**
     * Espera bloqueando hasta confirmar que el dron ha llegado
     * a un objetivo espacial concreto.
     *
     * Criterio de llegada:
     * - el dron debe estar dentro de la tolerancia horizontal (XY)
     * - el dron debe estar dentro de la tolerancia vertical (Z)
     * - y debe permanecer así durante stableSec segundos
     *
     * Esto evita considerar como "llegada" un cruce momentáneo
     * por la zona objetivo mientras todavía hay oscilación.
     *
     * Si se supera timeoutSec, se lanza una excepción.
     */
    private void waitUntilArrived(double tx, double ty, double tz) {

        long start = System.currentTimeMillis();
        long stableStart = -1L;

        while (true) {

            Point3D p = getCurrentPosition();

            double dx = tx - p.x();
            double dy = ty - p.y();
            double dz = tz - p.z();

            double distXY = Math.sqrt(dx * dx + dy * dy);

            boolean inside = (distXY <= posTol) && (Math.abs(dz) <= altTol);

            long now = System.currentTimeMillis();

            if (inside) {

                if (stableStart < 0) {
                    stableStart = now;
                }

                double stableElapsed = (now - stableStart) / 1000.0;

                if (stableElapsed >= stableSec) {

                    double elapsed = (now - start) / 1000.0;

                    log(String.format(
                            "ARRIVED target=(%.2f, %.2f, %.2f) real=(%.2f, %.2f, %.2f) distXY=%.3f dz=%.3f elapsed=%.2fs",
                            tx, ty, tz,
                            p.x(), p.y(), p.z(),
                            distXY, dz,
                            elapsed));

                    return;
                }

            } else {

                stableStart = -1L;

            }

            double elapsed = (now - start) / 1000.0;

            if (elapsed >= timeoutSec) {

                log(String.format(
                        "TIMEOUT waiting target=(%.2f, %.2f, %.2f) lastPos=(%.2f, %.2f, %.2f)",
                        tx, ty, tz,
                        p.x(), p.y(), p.z()));

                throw new RuntimeException(
                        String.format("Timeout esperando llegada a (%.2f, %.2f, %.2f)", tx, ty, tz)
                );
            }

            sleepMillis(50);
        }
    }

    /**
     * Espera bloqueando hasta detectar que el dron ha aterrizado.
     *
     * En esta versión se considera que el aterrizaje ha finalizado
     * cuando la altura medida es suficientemente pequeña (z <= 0.15).
     *
     * Si se supera timeoutSec, se lanza una excepción.
     */
    private void waitUntilLanded() {

        long start = System.currentTimeMillis();

        while (true) {

            Point3D p = getCurrentPosition();

            if (p.z() <= 0.15) {

                log(String.format(
                        "LANDED detected z=%.3f",
                        p.z()));

                return;
            }

            double elapsed = (System.currentTimeMillis() - start) / 1000.0;

            if (elapsed >= timeoutSec) {

                log("TIMEOUT waiting landing");

                throw new RuntimeException("Timeout esperando aterrizaje");
            }

            sleepMillis(50);
        }
    }

    /**
     * Suspende la ejecución durante un número dado de segundos.
     *
     * Se usa principalmente para implementar hover síncrono.
     */
    private void sleepSeconds(double seconds) {
        sleepMillis((long) (seconds * 1000.0));
    }

    /**
     * Suspende la ejecución durante un número dado de milisegundos.
     *
     * Si el hilo es interrumpido, se restaura el estado de interrupción
     * y se lanza una excepción de runtime.
     */
    private void sleepMillis(long ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrumpido mientras esperaba en WebotsMissionAdapter", e);
        }
    }
}