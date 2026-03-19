# runtime/ — Runtime de misión SARL en modo síncrono (V3)

Este directorio contiene la implementación del **runtime de misión** de SARL
correspondiente a la versión **SARL V3**.

A diferencia de la gramática o del intérprete, el runtime no define
la sintaxis del lenguaje ni evalúa expresiones del programa.
Su responsabilidad es ejecutar la **lógica operativa de misión**
una vez que el intérprete ya ha decidido qué comando SARL debe ejecutarse.

En esta versión, el runtime funciona en **modo síncrono**:

- cada acción de misión se ejecuta de forma bloqueante
- el intérprete no continúa hasta que la acción anterior ha finalizado
- la semántica de ejecución es simple, predecible y fácil de depurar

Esto convierte al runtime en la capa que conecta:

- el **Interpreter**, que entiende el lenguaje
- el **MissionState**, que almacena el estado de misión
- el **MissionAdapter**, que conecta con una plataforma concreta

---

## Rol del runtime en la arquitectura SARL

Dentro del proyecto SARL, el runtime ocupa una posición intermedia entre:

- el **intérprete del lenguaje**
- el **estado lógico de la misión**
- la **plataforma real o simulada** sobre la que se ejecuta la misión

Su responsabilidad principal es:

- mantener y actualizar el estado lógico de la misión
- traducir comandos SARL de alto nivel a operaciones de misión
- coordinar planificación y ejecución de waypoints
- sincronizar la posición lógica con la posición real del dron
- abstraer la ejecución física mediante `MissionAdapter`

En otras palabras:

- la **gramática** define qué se puede escribir
- el **intérprete** decide qué significa cada instrucción
- el **runtime** decide cómo se ejecuta esa instrucción dentro de una misión
- el **adapter** la traduce a una plataforma concreta

---

## Estructura actual del directorio

    runtime/
    ├── MainDryRunSync.java
    ├── MissionState.java
    ├── SyncMissionRuntime.java
    └── README.md

Este paquete contiene tres piezas principales:

- `MissionState.java` → estado lógico de misión
- `SyncMissionRuntime.java` → motor de ejecución síncrono
- `MainDryRunSync.java` → prueba directa del runtime sin pasar por SARL

---

## Clases principales

### `MissionState.java`

`MissionState` representa el **estado interno de una misión SARL en ejecución**.

No interpreta el lenguaje ni ejecuta acciones físicas sobre el dron.
Su única responsabilidad es almacenar el estado lógico actual de la misión
para que otras capas del sistema lo consulten y actualicen de forma controlada.

Centraliza información como:

- altitud objetivo de misión
- velocidad objetivo de misión
- punto `home`
- posición actual conocida del dron
- área activa de trabajo
- plan actual de waypoints
- progreso dentro del plan

Ejemplos de datos que viven en `MissionState`:

- `altitude`
- `speed`
- `home`
- `currentPosition`
- `currentArea`
- `currentPlan`
- `currentWaypointIndex`

Una decisión importante de diseño en SARL V3 es separar claramente:

- **estado del programa SARL** → mantenido por `Interpreter`
- **estado de la misión** → mantenido por `MissionState`

Esto evita mezclar:

- variables del script (`i`, `x`, `puntos`)
- datos operativos de misión (`home`, área, waypoint actual, altitud, etc.)

---

### `SyncMissionRuntime.java`

`SyncMissionRuntime` es el **núcleo de ejecución de misiones SARL en modo síncrono**.

Actúa como capa intermedia entre:

- el **Interpreter**
- el **MissionState**
- el **MissionAdapter**

Sus responsabilidades principales son:

1. gestionar el estado de misión a través de `MissionState`
2. traducir comandos SARL de alto nivel a operaciones del adapter
3. coordinar planificación y ejecución secuencial
4. mantener sincronizada la posición lógica con la posición real

Ejemplos de operaciones expuestas por el runtime:

- `setAltitude(...)`
- `setSpeed(...)`
- `setHome(...)`
- `defineAreaRect(...)`
- `planLawnmower(...)`
- `takeoff()`
- `goNextPoint()`
- `hover(...)`
- `land()`
- `remainingPoints()`

El runtime es **síncrono** porque cada comando bloquea hasta completarse.
Eso significa que, mientras una acción no termina, el intérprete no continúa
con la siguiente instrucción del script SARL.

Esta semántica simplifica mucho la ejecución en la fase actual del proyecto.

---

### `MainDryRunSync.java`

`MainDryRunSync` es una clase de prueba directa del runtime,
sin pasar por la gramática ni por el intérprete.

Su objetivo es validar de forma aislada:

- `MissionState`
- `SyncMissionRuntime`
- definición de áreas
- planificación de barrido
- ejecución secuencial sobre `ConsoleMissionAdapter`

Es útil como prueba intermedia entre:

- pruebas puramente geométricas
- ejecución completa desde scripts SARL
- integración final con Webots

Aquí la misión se construye manualmente desde Java.

---

## Evolución respecto a versiones anteriores

La introducción del paquete `runtime/` es uno de los cambios arquitectónicos
más importantes de SARL V3.

En versiones anteriores, el intérprete concentraba toda la semántica operativa
y la ejecución se apoyaba sobre todo en dry-run o adapters muy simples.

Con SARL V3 se introduce una separación mucho más clara entre:

- lenguaje
- misión
- plataforma

La evolución puede resumirse en cuatro cambios principales.

### 1. Separación entre programa y misión

Antes, la lógica del lenguaje y la lógica operativa de misión
estaban mucho más mezcladas.

Ahora se separan explícitamente:

- `Interpreter` → estado del programa y semántica del lenguaje
- `MissionState` → estado operativo de la misión

Esto hace la arquitectura más clara y más extensible.

---

### 2. Introducción de una capa de runtime

La ejecución de misión ya no está embebida en el intérprete.

Ahora existe una capa intermedia (`SyncMissionRuntime`) encargada de:

- entender la misión como entidad operativa
- coordinar acciones complejas
- mantener coherencia interna de estado

Esto evita que el intérprete se convierta en una clase gigantesca
mezclando demasiadas responsabilidades.

---

### 3. Conexión real con planificación de misión

El runtime introduce la noción de misión como algo más que una lista
de comandos sueltos.

Ahora existen ya conceptos como:

- área de trabajo
- plan de waypoints
- progreso dentro del plan
- consulta de puntos restantes
- generación de rutas

Esto acerca SARL a un lenguaje verdaderamente orientado a misiones SAR,
aunque los algoritmos concretos de planificación sigan siendo componentes de dominio
y no el núcleo del lenguaje en sentido estricto.

---

### 4. Preparación para futuras extensiones reactivas

Aunque esta versión es síncrona y bloqueante, la existencia de:

- `MissionState`
- `SyncMissionRuntime`

prepara la arquitectura para futuras extensiones, por ejemplo:

- eventos
- pausas de misión
- replanificación
- abortos controlados
- ejecución reactiva

Es decir, estas clases no solo resuelven el presente de V3,
sino que preparan el terreno para la evolución posterior del proyecto.

---

## Estado de misión almacenado en `MissionState`

Actualmente `MissionState` mantiene:

### Configuración general

- altitud por defecto
- velocidad por defecto
- punto `home`

### Estado espacial

- posición actual conocida del dron

### Geometría y planificación

- área actual de trabajo (`RectangleArea`)
- plan actual de waypoints (`List<Waypoint2D>`)
- índice del waypoint actual

### Progreso de misión

- waypoint activo
- número de waypoints restantes

Esto convierte a `MissionState` en la fuente de verdad del estado lógico de la misión.

---

## Funcionalidad principal de `SyncMissionRuntime`

Actualmente el runtime soporta estas grandes áreas de responsabilidad.

### 1. Configuración de misión

Ejemplos de acciones:

    runtime.setHome(new Point3D(0.0, 0.0, 0.0));
    runtime.setAltitude(10.0);
    runtime.setSpeed(3.0);

Estas operaciones actualizan el estado lógico de la misión
y quedan disponibles para acciones posteriores.

---

### 2. Definición de geometría

El runtime permite definir el área de trabajo de la misión.

Ejemplo conceptual:

    runtime.defineAreaRect(
        new Point2D(0.0, 0.0),
        new Point2D(20.0, 10.0)
    );

Internamente se apoya en `SweepAreaBuilder` para construir
un `RectangleArea` válido a partir de dos puntos.

---

### 3. Planificación de barrido

El runtime puede generar un plan de barrido tipo lawnmower:

    runtime.planLawnmower(5.0);

Flujo conceptual:

1. comprueba que existe un área definida
2. obtiene la posición actual del dron
3. usa esa posición como `startHint`
4. genera la lista de waypoints
5. guarda el plan en `MissionState`

Aunque actualmente se utiliza `LawnmowerGenerator`,
el runtime almacena únicamente el resultado del algoritmo:
una lista ordenada de waypoints.

---

### 4. Ejecución de acciones físicas

El runtime puede ejecutar acciones de misión reales a través del adapter:

- despegue
- movimiento al siguiente waypoint
- hover
- aterrizaje

Ejemplo conceptual:

    runtime.takeoff();

    while (runtime.remainingPoints() > 0) {
        runtime.goNextPoint();
        runtime.hover(1.0);
    }

    runtime.land();

El runtime no conoce cómo se mueve realmente el dron:
solo invoca el adapter y sincroniza después el estado lógico.

---

### 5. Consulta de estado operativo

El runtime expone consultas útiles para el intérprete o para pruebas,
por ejemplo:

    runtime.remainingPoints()

Esto permite construir lógica SARL como:

    MIENTRAS puntos > 0 {
        IR_SIGUIENTE_PUNTO;
        PUNTOS_RESTANTES puntos;
    }

---

## Relación entre `MissionState` y `SyncMissionRuntime`

La relación entre ambas clases es muy directa:

- `MissionState` almacena el estado
- `SyncMissionRuntime` usa y actualiza ese estado

Es decir:

- `MissionState` no decide nada
- `SyncMissionRuntime` sí implementa la lógica operativa

Ejemplo conceptual:

Cuando se llama a `goNextPoint()`:

1. el runtime consulta en `MissionState` cuál es el waypoint actual
2. ordena al adapter mover el dron a ese punto
3. sincroniza la posición real
4. avanza el índice del plan en `MissionState`

Esta separación de responsabilidades simplifica mucho el diseño.

---

## Relación con el intérprete

El runtime no interpreta scripts SARL por sí mismo.

Es el `Interpreter` quien:

- lee el programa
- evalúa variables
- evalúa expresiones y condiciones
- decide qué comando del lenguaje se está ejecutando

Cuando detecta un comando de misión, delega en el runtime.

Ejemplo conceptual:

- `Interpreter` reconoce `PLAN_CORTACESPED 5;`
- evalúa el argumento `5`
- llama a `runtime.planLawnmower(5)`

Esto permite mantener claramente separadas:

- la semántica del lenguaje
- la semántica de misión

---

## Relación con el adapter

El runtime tampoco conoce detalles concretos de Webots
ni de ninguna otra plataforma física o simulada.

Solo depende de `MissionAdapter`, que define una interfaz abstracta con operaciones como:

- `takeoff(...)`
- `moveTo(...)`
- `hover(...)`
- `land()`
- `getCurrentPosition()`

Esto permite que el mismo runtime pueda utilizarse con distintos backends:

- adapter de consola
- adapter de Webots
- adapter futuro para dron real

Así, SARL y su lógica de misión permanecen desacoplados de la plataforma.

---

## Flujo de ejecución típico

En la arquitectura actual, el flujo típico es:

1. el usuario escribe un script SARL
2. la gramática genera el AST mediante ANTLR
3. el `Interpreter` recorre el AST
4. el `Interpreter` delega comandos de misión en `SyncMissionRuntime`
5. el runtime actualiza `MissionState`
6. el runtime invoca `MissionAdapter`
7. el adapter ejecuta la acción sobre la plataforma concreta
8. el runtime sincroniza la posición y el estado de misión

Esto convierte al paquete `runtime/` en una pieza esencial
del puente entre el lenguaje y la ejecución real de la misión.

---

## Estado actual del runtime

Actualmente, el runtime SARL V3 soporta:

- configuración básica de misión
- almacenamiento estructurado del estado de misión
- definición de áreas rectangulares
- planificación tipo lawnmower
- ejecución síncrona de acciones
- navegación secuencial por waypoints
- consulta de puntos restantes
- trazabilidad mediante logs

Esta base ya es suficientemente sólida como para ejecutar
misiones completas en modo síncrono.

---

## Limitaciones actuales

Aunque el runtime actual es funcional, todavía existen limitaciones deliberadas.

### Ejecución bloqueante

Cada comando espera a que termine la acción anterior.
No existe todavía ejecución concurrente ni reactiva.

### Un único modelo principal de planificación

Actualmente el runtime incorpora de forma directa la generación
de planes tipo lawnmower como primer caso de uso.

### Estado de misión todavía sencillo

No existen aún conceptos como:

- misión pausada
- misión abortada
- evento activo
- prioridad de tareas
- replanificación

### Integración parcial de velocidad

La velocidad se almacena ya en `MissionState`,
pero su aplicación fina al backend puede ampliarse más adelante.

---

## Próxima evolución

Las siguientes iteraciones del runtime podrán estudiar, entre otros aspectos:

- evolución hacia un motor de misión reactivo
- gestión de eventos
- interrupción o pausa de acciones
- replanificación dinámica
- integración de más algoritmos de planificación
- separación adicional entre servicios de misión y núcleo de ejecución

La versión actual proporciona una base muy adecuada
para congelar el modo síncrono y evolucionar más adelante
hacia una arquitectura con eventos y ejecución asíncrona.