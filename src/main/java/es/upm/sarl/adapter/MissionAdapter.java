package es.upm.sarl.adapter;

import es.upm.sarl.geometry.Point3D;

/**
 * MissionAdapter
 * --------------
 * Interfaz que define el contrato entre el runtime de misión
 * y el backend encargado de ejecutar físicamente las acciones
 * del dron.
 *
 * El objetivo de esta interfaz es desacoplar el sistema SARL
 * de cualquier plataforma concreta de ejecución.
 *
 * Gracias a esta abstracción, el runtime puede ejecutar misiones
 * sin conocer detalles del entorno real o simulado donde se mueve
 * el dron.
 *
 * Implementaciones posibles:
 *
 * - ConsoleMissionAdapter
 *     Simulación simple basada en consola.
 *     Se utiliza para pruebas rápidas del runtime y del intérprete
 *     sin necesidad de un simulador físico.
 *
 * - WebotsMissionAdapter
 *     Conecta el runtime con el controlador real del dron en Webots.
 *
 * - (posible extensión futura)
 *     Adaptador para dron real o simuladores adicionales.
 *
 * Flujo típico:
 *
 * Interpreter
 *     ↓
 * SyncMissionRuntime
 *     ↓
 * MissionAdapter (interfaz)
 *     ↓
 * Implementación concreta (Console/Webots/etc.)
 *
 * De esta forma, el lenguaje SARL y el runtime permanecen
 * completamente independientes del sistema físico de ejecución.
 */
public interface MissionAdapter {

    /**
     * Ordena al dron despegar hasta alcanzar una altitud objetivo.
     *
     * El método es bloqueante desde el punto de vista del runtime:
     * no debe devolver el control hasta que el despegue haya
     * finalizado.
     *
     * @param altitude altitud objetivo en metros
     */
    void takeoff(double altitude);

    /**
     * Ordena al dron desplazarse a una posición objetivo
     * en coordenadas del mundo.
     *
     * La implementación concreta se encarga de mover el dron
     * hasta ese punto y no debe devolver el control hasta
     * completar el movimiento.
     *
     * @param x coordenada X objetivo
     * @param y coordenada Y objetivo
     * @param z coordenada Z objetivo
     */
    void moveTo(double x, double y, double z);

    /**
     * Ordena al dron permanecer en hover durante un tiempo dado.
     *
     * Este comando se usa normalmente entre waypoints para
     * estabilizar el dron o simular pausas de inspección.
     *
     * @param seconds duración del hover en segundos
     */
    void hover(double seconds);

    /**
     * Ordena al dron aterrizar.
     *
     * La implementación debe encargarse de ejecutar el descenso
     * y no devolver el control hasta que el aterrizaje haya
     * finalizado.
     */
    void land();

    /**
     * Devuelve la posición actual del dron.
     *
     * Este método permite al runtime sincronizar el estado lógico
     * de la misión con la posición real reportada por el backend.
     *
     * @return posición actual del dron
     */
    Point3D getCurrentPosition();
}