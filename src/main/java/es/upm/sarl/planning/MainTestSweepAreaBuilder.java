package es.upm.sarl.planning;

import es.upm.sarl.geometry.Point2D;
import es.upm.sarl.geometry.RectangleArea;

/**
 * MainTestSweepAreaBuilder
 * ------------------------
 * Programa de prueba para validar el comportamiento de SweepAreaBuilder.
 *
 * Se comprueba:
 *  - Construcción correcta de zona desde dos puntos.
 *  - Aplicación automática del margen por defecto.
 *  - Aplicación de margen manual.
 *  - Ajuste automático cuando el margen supera maxArea.
 *
 * Este main permite validar el control de tamaño (maxArea)
 * antes de integrar con algoritmos de barrido.
 */
public class MainTestSweepAreaBuilder {

    public static void main(String[] args) {

        // Configuración de la construcción de zonas
        double defaultMargin = 2.0; // margen automático
        double maxArea = 10000.0; // límite superior de área permitida

        // Creamos el builder con configuración
        SweepAreaBuilder builder = new SweepAreaBuilder(defaultMargin, maxArea);

         // Dos puntos que definen la zona base
        Point2D a = new Point2D(2, 5);
        Point2D b = new Point2D(50, 100);

        System.out.println("Default margin = " + builder.defaultMargin());
        System.out.println("Max area       = " + builder.maxArea());

        // Construcción usando margen por defecto
        RectangleArea areaDefault = builder.fromTwoPoints(a, b);

        System.out.println("\nArea with DEFAULT margin:");
        System.out.println(areaDefault);
        System.out.println("Area = " + (areaDefault.width() * areaDefault.height()));

        // Construcción usando margen exagerado para forzar ajuste
        RectangleArea areaManual = builder.fromTwoPoints(a, b, 50.0); 
        
        System.out.println("\nArea with MANUAL margin=50.0 (should be capped):");
        System.out.println(areaManual);
        System.out.println("Area = " + (areaManual.width() * areaManual.height()));
    }
}