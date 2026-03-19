# adapter/ — Adaptadores de ejecución de misión SARL (V3)

Este directorio contiene la capa de **adaptación a plataforma** del proyecto SARL
correspondiente a la versión **SARL V3**.

La función de este paquete es desacoplar el lenguaje y la lógica de misión
de cualquier backend físico o simulado concreto.

Gracias a esta capa, el sistema SARL puede mantener independientes:

- la **sintaxis del lenguaje**
- la **semántica de misión**
- la **plataforma sobre la que realmente se ejecuta la misión**

En la arquitectura actual, los adapters permiten que el mismo runtime
pueda trabajar indistintamente con:

- una simulación sencilla basada en consola
- el simulador Webots
- futuras extensiones hacia otros simuladores o drones reales

---

## Rol del paquete `adapter/` en la arquitectura SARL

Dentro del proyecto SARL, esta capa ocupa el último tramo del flujo de ejecución:

- la **gramática** define qué se puede escribir
- el **Interpreter** interpreta el programa SARL
- el **SyncMissionRuntime** coordina la lógica de misión
- el **MissionAdapter** traduce esa lógica a acciones concretas
- una implementación concreta ejecuta la misión sobre una plataforma determinada

Es decir:

    Interpreter
        ↓
    SyncMissionRuntime
        ↓
    MissionAdapter
        ↓
    ConsoleMissionAdapter / WebotsMissionAdapter / futuras implementaciones

Esta separación es una de las decisiones más importantes de diseño en SARL V3,
porque evita acoplar directamente el lenguaje a Webots o a cualquier backend particular.

---

## Estructura actual del directorio

    adapter/
    ├── ConsoleMissionAdapter.java
    ├── MissionAdapter.java
    ├── WebotsMissionAdapter.java
    └── README.md

Este paquete contiene:

- una **interfaz común de adaptación**
- una implementación simple para consola
- una implementación conectada a Webots

---

## Componentes principales

### `MissionAdapter.java`

`MissionAdapter` es la interfaz que define el contrato entre:

- el runtime de misión
- el backend encargado de ejecutar físicamente las acciones del dron

Su objetivo es desacoplar completamente el sistema SARL
de cualquier plataforma concreta.

Actualmente define las operaciones de más alto nivel que el runtime necesita:

- `takeoff(double altitude)`
- `moveTo(double x, double y, double z)`
- `hover(double seconds)`
- `land()`
- `getCurrentPosition()`

Estas operaciones se consideran **bloqueantes desde el punto de vista del runtime**:
el adapter no debe devolver el control hasta que la acción se considere completada.

Esto permite que el runtime mantenga una semántica de misión clara y secuencial.

---

### `ConsoleMissionAdapter.java`

`ConsoleMissionAdapter` es una implementación simple de `MissionAdapter`
basada únicamente en consola.

No controla un dron real ni un simulador físico.
Su objetivo es permitir probar el runtime y el intérprete SARL
sin depender de Webots ni de ningún backend complejo.

Su funcionamiento es idealizado:

- mantiene una posición interna del dron (`currentPosition`)
- cada acción actualiza esa posición de forma instantánea
- imprime en consola la acción ejecutada
- no existe dinámica física ni tiempos de movimiento reales

Ejemplos conceptuales:

    takeoff(10)  → cambia directamente Z a 10
    moveTo(x,y,z) → cambia instantáneamente la posición
    land() → fija la altitud a 0

Este adapter resulta muy útil para:

- depuración del lenguaje SARL
- pruebas del intérprete
- pruebas del runtime
- validación de la planificación sin simulación física

---

### `WebotsMissionAdapter.java`

`WebotsMissionAdapter` es la implementación de `MissionAdapter`
conectada al controlador real del dron dentro de Webots.

Actúa como puente entre:

- el runtime abstracto de misión (`SyncMissionRuntime`)
- el controlador físico/simulado del dron en Webots (`Controlador`)

Su responsabilidad es traducir operaciones de misión de alto nivel como:

- `takeoff(...)`
- `moveTo(...)`
- `hover(...)`
- `land()`

en acciones reales sobre el controlador.

A diferencia de `ConsoleMissionAdapter`, aquí sí existe:

- movimiento real en el simulador
- espera real hasta completar una acción
- comprobación de llegada
- sincronización continua con la posición del dron
- timeouts de seguridad

Esta implementación es, por tanto, la pieza que permite
que un programa SARL se ejecute sobre Webots como una secuencia
real de acciones de misión.

---

## Diferencia entre la interfaz y sus implementaciones

Una de las claves de este paquete es distinguir entre:

### La interfaz común

`MissionAdapter` no sabe nada de Webots ni de la consola.
Solo expresa qué operaciones necesita el runtime.

### Las implementaciones concretas

Cada implementación decide cómo realizar esas operaciones:

- `ConsoleMissionAdapter` → simulación idealizada e instantánea
- `WebotsMissionAdapter` → ejecución física/simulada real

Gracias a esto, el runtime no necesita cambiar si cambia la plataforma.

---

## Semántica bloqueante en los adapters

En la arquitectura actual de SARL V3, el modo de ejecución es síncrono.

Eso significa que el runtime espera que cada operación del adapter
no devuelva el control hasta que la acción haya terminado.

Por ejemplo:

### En consola

La operación termina inmediatamente:

    currentPosition = new Point3D(x, y, z);

### En Webots

La operación termina solo cuando el dron ha llegado realmente
y se ha mantenido dentro de una tolerancia durante cierto tiempo.

Esta diferencia de comportamiento es totalmente transparente para el runtime,
que solo conoce la interfaz `MissionAdapter`.

---

## Funcionalidad actual de `ConsoleMissionAdapter`

Actualmente esta implementación soporta:

- despegue instantáneo
- movimiento instantáneo
- hover registrado en consola
- aterrizaje instantáneo
- consulta de posición simulada

Ejemplo conceptual de uso:

    MissionAdapter adapter = new ConsoleMissionAdapter();

Esto permite ejecutar misiones SARL de forma rápida
sin necesidad de abrir Webots.

---

## Funcionalidad actual de `WebotsMissionAdapter`

Actualmente esta implementación soporta:

- despegue real con espera hasta alcanzar altitud
- movimiento real con espera hasta llegada estable
- hover bloqueante
- aterrizaje real con detección de suelo
- consulta de posición real desde el controlador

Además, incorpora parámetros de sincronización como:

- tolerancia horizontal (`posTol`)
- tolerancia vertical (`altTol`)
- tiempo mínimo de estabilidad (`stableSec`)
- timeout máximo (`timeoutSec`)

Esto permite definir con bastante precisión cuándo una acción
debe considerarse realmente completada.

---

## Relación con el controlador de Webots

`WebotsMissionAdapter` no implementa por sí mismo
la física ni el control fino del dron.

Para eso delega en `Controlador`, que se encarga de:

- PIDs
- motores
- sensores
- control de yaw
- lógica de aterrizaje
- navegación física dentro del simulador

El adapter no reemplaza al controlador:
simplemente lo envuelve desde una perspectiva más abstracta
y orientada a misión.

En otras palabras:

- `Controlador` sabe mover el dron
- `WebotsMissionAdapter` sabe cuándo considerar completada una acción
- `SyncMissionRuntime` sabe cómo encadenar esas acciones dentro de la misión

---

## Ventajas arquitectónicas de esta capa

La introducción del paquete `adapter/` aporta varias ventajas importantes:

### 1. Desacoplamiento de plataforma

SARL no depende directamente de Webots.

### 2. Reutilización del runtime

El mismo `SyncMissionRuntime` puede ejecutarse sobre varios backends.

### 3. Facilidad de pruebas

El adapter de consola permite validar gran parte del sistema
sin necesidad de simulación física.

### 4. Escalabilidad futura

Permite incorporar futuras implementaciones como:

- adapter para dron real
- adapter para otros simuladores
- adapter para pruebas automáticas

---

## Limitaciones actuales

Aunque la capa de adaptación es ya funcional, todavía existen algunas limitaciones.

### Catálogo reducido de operaciones

La interfaz actual cubre solo las operaciones básicas necesarias
para el modo síncrono actual:

- despegar
- moverse
- hacer hover
- aterrizar
- consultar posición

### Semántica todavía orientada al modo síncrono

Los adapters actuales están diseñados para una arquitectura bloqueante.
Una futura evolución reactiva puede requerir ampliar la interfaz.

### Integración parcial de otros parámetros de misión

Por ejemplo, la velocidad ya existe en el estado de misión,
pero todavía no se integra de forma explícita como parámetro
de la interfaz del adapter.

---

## Estado actual del paquete `adapter/`

Actualmente esta capa proporciona:

- una interfaz abstracta estable para ejecución de misión
- una implementación ligera de consola
- una implementación funcional conectada a Webots
- trazabilidad mediante logs
- desacoplamiento entre lenguaje, misión y plataforma

Esto convierte a `adapter/` en una de las piezas esenciales
de la arquitectura SARL V3.

---

## Próxima evolución

Las siguientes iteraciones de esta capa podrán estudiar, entre otros aspectos:

- integración de eventos físicos o simulados
- notificación de estados intermedios al runtime
- soporte para interrupción o cancelación de acciones
- adapters adicionales para otras plataformas
- integración más fina de velocidad, sensores o telemetría

La arquitectura actual ya deja preparado el camino
para que SARL pueda ejecutarse sobre entornos distintos
sin cambiar ni la gramática ni el núcleo de misión.