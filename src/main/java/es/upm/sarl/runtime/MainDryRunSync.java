package es.upm.sarl.runtime;

import es.upm.sarl.adapter.ConsoleMissionAdapter;
import es.upm.sarl.adapter.MissionAdapter;
import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.Point3D;
import es.upm.sarl.runtime.SyncMissionRuntime;

/**
 * MainDryRunSync
 * --------------
 * Main de prueba directa del runtime síncrono sin pasar por SARL.
 *
 * Su objetivo es validar de forma aislada:
 * - MissionState
 * - SyncMissionRuntime
 * - generación de áreas
 * - planificación de rutas
 * - ejecución secuencial sobre ConsoleMissionAdapter
 *
 * Es útil como prueba intermedia entre:
 *
 * - pruebas puramente geométricas/planning
 * - ejecución completa desde scripts SARL
 *
 * Aquí no se usa ANTLR ni Interpreter.
 * La misión se construye manualmente desde Java.
 */
public class MainDryRunSync {

    public static void main(String[] args) {

        /**
         * Se usa el adapter de consola para simular la ejecución
         * sin necesidad de Webots.
         */
        MissionAdapter adapter = new ConsoleMissionAdapter();
        SyncMissionRuntime runtime = new SyncMissionRuntime(adapter);

        System.out.println("=== DRY RUN SYNC START ===");

        // ---------------------------------------------------------
        // Configuración inicial de la misión
        // ---------------------------------------------------------

        runtime.setHome(new Point3D(0.0, 0.0, 0.0));
        runtime.setAltitude(10.0);
        runtime.setSpeed(3.0);

        /**
         * Para esta prueba fijamos manualmente una posición inicial
         * distinta de home, de forma que el planner pueda usarla
         * como startHint al generar la ruta.
         */
        runtime.getState().updatePosition(new Point3D(2.0, 1.0, 0.0));

        // ---------------------------------------------------------
        // Definición de zona de trabajo y generación del plan
        // ---------------------------------------------------------

        runtime.defineAreaRect(
                new Point2D(0.0, 0.0),
                new Point2D(20.0, 10.0)
        );

        runtime.planLawnmower(5.0);

        System.out.println("Waypoints generados: " + runtime.remainingPoints());

        // ---------------------------------------------------------
        // Ejecución simulada de la misión
        // ---------------------------------------------------------
        
        runtime.takeoff();

        while (runtime.remainingPoints() > 0) {
            runtime.goNextPoint();
            runtime.hover(1.0);
            System.out.println("Waypoints restantes: " + runtime.remainingPoints());
        }

        runtime.land();

        System.out.println("=== DRY RUN SYNC END ===");
    }
}