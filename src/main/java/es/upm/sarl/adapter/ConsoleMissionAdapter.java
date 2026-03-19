package es.upm.sarl.adapter;

import es.upm.sarl.geometry.Point3D;

/**
 * ConsoleMissionAdapter
 * ---------------------
 * Implementación simple de MissionAdapter basada en consola.
 *
 * Este adapter no controla un dron real ni un simulador físico.
 * Su objetivo es permitir probar el runtime y el intérprete SARL
 * sin depender de Webots u otro entorno de simulación.
 *
 * Funcionamiento:
 *
 * - Mantiene una posición interna del dron (currentPosition).
 * - Cada comando de misión actualiza esa posición de forma
 *   instantánea.
 * - Se imprime en consola la acción ejecutada.
 *
 * Este comportamiento simula una ejecución idealizada donde
 * todas las operaciones se completan inmediatamente.
 *
 * Utilidad principal:
 *
 * - Depuración del lenguaje SARL
 * - pruebas del Interpreter
 * - pruebas del runtime sin simulación física
 * - validación de la planificación de misiones
 *
 * En contraste, WebotsMissionAdapter delega el movimiento real
 * en el controlador físico del dron dentro del simulador.
 */
public class ConsoleMissionAdapter implements MissionAdapter {

    /**
     * Posición actual simulada del dron.
     *
     * En este adapter la posición se actualiza directamente
     * cuando se ejecuta un comando como moveTo() o takeoff().
     *
     * No existe dinámica física ni tiempo de movimiento.
     */
    private Point3D currentPosition = new Point3D(0.0, 0.0, 0.0);

    /**
     * Simula el despegue del dron.
     *
     * El despegue simplemente actualiza la coordenada Z de la
     * posición actual y escribe la acción en consola.
     */
    @Override
    public void takeoff(double altitude) {
        currentPosition = new Point3D(currentPosition.x(), currentPosition.y(), altitude);
        System.out.printf("[ADAPTER] takeoff(%.2f)%n", altitude);
    }

    /**
     * Simula el movimiento del dron a una nueva posición.
     *
     * La posición se actualiza instantáneamente sin simulación
     * de trayectoria ni tiempo de desplazamiento.
     */
    @Override
    public void moveTo(double x, double y, double z) {
        currentPosition = new Point3D(x, y, z);
        System.out.printf("[ADAPTER] moveTo(%.2f, %.2f, %.2f)%n", x, y, z);
    }

    /**
     * Simula una pausa en hover.
     *
     * En esta implementación no se simula tiempo real de espera;
     * simplemente se registra el comando en consola.
     */
    @Override
    public void hover(double seconds) {
        System.out.printf("[ADAPTER] hover(%.2f s)%n", seconds);
    }

    /**
     * Simula el aterrizaje del dron.
     *
     * El aterrizaje simplemente fija la altitud a cero
     * manteniendo la posición horizontal actual.
     */
    @Override
    public void land() {
        currentPosition = new Point3D(currentPosition.x(), currentPosition.y(), 0.0);
        System.out.println("[ADAPTER] land()");
    }

    /**
     * Devuelve la posición actual simulada del dron.
     *
     * Este método permite al runtime sincronizar el estado
     * lógico de la misión con la posición mantenida por el adapter.
     */
    @Override
    public Point3D getCurrentPosition() {
        return currentPosition;
    }
}
