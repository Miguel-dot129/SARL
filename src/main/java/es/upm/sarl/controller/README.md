**README.md - Controlador Java para el Dron SAR (Webots)**

Este directorio contiene el controlador actual en Java que se utilizará junto con Webots para el control de un cuadricóptero en misiones de búsqueda y rescate (SAR).

---

## Estado del Proyecto

El archivo principal es `Controlador.java`, que implementa un sistema de **control PID en cascada** para regular la posición y orientación del dron en el entorno simulado. Este controlador está en desarrollo activo y se encuentra en fase de ajustes y mejoras.

Otros archivos en esta carpeta (`ControladorV1.java`, `ControladorV1_alt.java`) son versiones experimentales previas y se mantienen por motivos de referencia histórica. No deben utilizarse.

---

## Arquitectura del Controlador

El controlador utiliza una estructura **en cascada**, que divide el problema en dos niveles jerárquicos:

1. **Lazo Externo de Posición**

   * PID de Altitud (Z): controla la altura con una señal de `baseThrottle` ajustada en cada paso.
   * PD de Posición Horizontal (X, Y): genera ángulos objetivo de inclinación (`pitchRef`, `rollRef`).

2. **Lazo Interno de Estabilización**

   * PD de Actitud (Roll, Pitch): corrige los ángulos actuales hacia los ángulos deseados.
   * PD de Yaw: estabiliza la orientación en el eje vertical (guiñada).

Cada uno de estos lazos se ejecuta secuencialmente en el método `run()`, que representa el bucle principal de control del dron.

---

## Situación Actual

* **Control de Altitud** (✔): Implementado con PID completo (P, I, D). Utiliza medidas de altura del GPS para mantener la altitud deseada (`targetAltZ`).
* **Control Horizontal** (✔): PD que convierte errores en X/Y a inclinaciones deseadas. Aún no incluye término integral.
* **Control de Actitud** (✔): PD que estabiliza rápidamente el dron hacia `rollRef` y `pitchRef`, usando medidas angulares y derivadas desde IMU y giroscopio.
* **Control de Yaw** (⚠): Implementado como PD, pero **todavía no es funcional para cambiar la orientación (yawRef)** sin comprometer la estabilidad. El cambio de rumbo necesita ser reestructurado.

---

## Próximos Pasos

* **Mejorar el control de Yaw** para permitir rotaciones activas sin perder estabilidad.
* **Agregar componente Integral (I)** a los controladores horizontal y de actitud si es necesario, para mayor precisión.
* **Reajustar las ganancias PID** después de introducir cambios.
* **Modo Manual / Control Directo** para pruebas.
* **Interfaz con lenguaje SARL-ANTLR** a través de clases adaptadoras.

---

## Métodos Relevantes

* `run()`: ciclo principal de control (altitud → posición → actitud).
* `PDAltitudeControl()`: calcula la corrección de potencia total según la altitud.
* `PDHorizontalControl()`: transforma error de posición en inclinación deseada.
* `PDAttitude()`: estabiliza roll y pitch al valor deseado (aplica mezcla de motores).
* `PDYawControl()`: regula el yaw actual frente al objetivo deseado.
* `hoverHere()`, `moveTo()`, `changeAltitude()`, `setYaw()`: métodos de alto nivel que permiten fijar objetivos al controlador.

---

Este controlador forma parte del desarrollo del TFG y servirá como base para la ejecución del lenguaje definido por ANTLR.
