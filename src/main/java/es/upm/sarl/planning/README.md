# planning/ — Servicios de planificación de misión SARL (V3)

Este directorio contiene los componentes de **planificación de misión**
utilizados por el proyecto **SARL** en la versión **SARL V3**.

Estas clases no forman parte del lenguaje SARL en sentido estricto:
no definen la gramática, no interpretan scripts y no ejecutan la misión
por sí mismas.

Su papel es proporcionar **servicios de dominio reutilizables**
para construir áreas de trabajo y generar rutas de navegación
a partir de primitivas geométricas simples.

En la versión actual, este paquete cubre principalmente:

- construcción de zonas rectangulares de barrido
- generación de rutas tipo *lawnmower* (cortacésped)
- representación de waypoints 2D
- mains de prueba aislados para validar el comportamiento geométrico y de planificación

---

## Rol del paquete `planning/` en la arquitectura SARL

Dentro del proyecto SARL, este paquete se sitúa como capa de soporte
para el runtime de misión.

Su función principal es transformar información geométrica simple en
estructuras útiles para ejecutar una misión.

Por ejemplo:

- dos puntos del plano → un área rectangular válida
- un área rectangular → una secuencia ordenada de waypoints
- una lista de waypoints → un plan que el runtime puede ejecutar paso a paso

En este sentido, `planning/` no pertenece al núcleo sintáctico del lenguaje,
pero sí forma parte del **ecosistema de misión SARL**,
ya que ofrece servicios concretos que el runtime invoca.

---

## Estructura actual del directorio

    planning/
    ├── LawnmowerGenerator.java
    ├── MainTestLawnmower.java
    ├── MainTestSweepAreaBuilder.java
    ├── SweepAreaBuilder.java
    ├── Waypoint2D.java
    └── README.md

Actualmente el paquete se compone de:

- una clase para construir zonas de trabajo (`SweepAreaBuilder`)
- un generador de rutas de barrido (`LawnmowerGenerator`)
- un tipo de waypoint orientado a misión (`Waypoint2D`)
- dos programas de prueba aislados

---

## Componentes principales

### `SweepAreaBuilder.java`

`SweepAreaBuilder` es la clase encargada de construir zonas de barrido
a partir de información geométrica básica.

Su objetivo es convertir dos puntos del plano en un `RectangleArea` válido
y listo para ser usado por algoritmos de planificación.

Responsabilidades principales:

- construir un rectángulo a partir de dos puntos arbitrarios
- aplicar un margen de seguridad
- controlar que el área final no supere un límite máximo
- ajustar automáticamente el margen si fuera necesario

Ejemplo conceptual:

    Point2D a = new Point2D(2, 5);
    Point2D b = new Point2D(50, 100);

    SweepAreaBuilder builder = new SweepAreaBuilder(2.0, 10000.0);
    RectangleArea area = builder.fromTwoPoints(a, b);

Esta clase no genera rutas ni waypoints.
Solo prepara una representación geométrica válida del área de trabajo.

---

### `LawnmowerGenerator.java`

`LawnmowerGenerator` es el algoritmo de planificación actualmente integrado
en SARL V3 para generar rutas de barrido.

Genera una secuencia ordenada de `Waypoint2D` siguiendo un patrón tipo
**cortacésped** (*lawnmower*), es decir:

- el dron recorre una línea recta
- al llegar al extremo cambia de fila
- recorre la siguiente línea en el sentido contrario
- repite este patrón hasta cubrir el área

En la versión actual:

- las líneas de barrido son paralelas al eje X
- se avanza sobre Y con un espaciado fijo (`laneSpacing`)
- la esquina inicial se elige en función de un `startHint`
- no se fuerza una última fila extra si no encaja exactamente

Ejemplo conceptual:

    List<Waypoint2D> plan = generator.generate(area, 10.0, startHint);

Esta clase devuelve únicamente una lista ordenada de waypoints.
No ejecuta la ruta ni conoce nada del runtime o de Webots.

---

### `Waypoint2D.java`

`Waypoint2D` representa un punto objetivo del plano XY
que el dron debe visitar en un orden determinado.

Su diferencia conceptual respecto a `Point2D` es importante:

- `Point2D` es una entidad geométrica genérica
- `Waypoint2D` es un punto con significado de misión

Actualmente contiene:

- `index` → posición dentro del plan
- `x`, `y` → coordenadas en el plano horizontal

Esto permite que un waypoint no sea solo un punto matemático,
sino un elemento trazable dentro del recorrido de la misión.

Ejemplo conceptual:

    WP[3](52.00, 13.00)

Más adelante podría ampliarse con:

- yaw objetivo
- velocidad específica
- tiempo de espera
- tipo de waypoint
- prioridad u otros metadatos

---

## Filosofía de diseño del paquete `planning/`

Las clases de este paquete se han diseñado siguiendo una idea clara:

- separar la lógica de planificación del lenguaje
- separar la planificación del backend físico
- hacer que los algoritmos trabajen sobre datos geométricos simples
- devolver resultados reutilizables por el runtime

Esto aporta varias ventajas:

- facilita las pruebas aisladas
- evita acoplar la planificación a Webots
- permite cambiar o añadir algoritmos en el futuro
- mantiene el lenguaje SARL conceptualmente independiente de una implementación concreta

---

## Qué forma parte del lenguaje y qué no

Una idea importante de diseño es distinguir entre:

### Núcleo del lenguaje SARL

- gramática
- parser
- intérprete
- semántica de comandos

### Servicios de planificación usados por SARL

- construcción de áreas
- generación de rutas
- representación de waypoints

El paquete `planning/` pertenece a este segundo grupo.

Es decir:

- SARL puede invocar un servicio de planificación
- pero el algoritmo concreto de planificación no es “el lenguaje”

Por ejemplo, en la versión actual se utiliza `LawnmowerGenerator`
como primera implementación concreta de planificación de barrido,
pero la arquitectura puede ampliarse con nuevos planificadores más adelante.

---

## Flujo conceptual de planificación en V3

En la arquitectura actual, el flujo típico de planificación es:

1. el usuario define una zona de trabajo en SARL
2. el intérprete despacha el comando al runtime
3. el runtime usa `SweepAreaBuilder` para construir el área
4. el runtime invoca `LawnmowerGenerator`
5. el generador produce una lista de `Waypoint2D`
6. el runtime guarda esa lista en `MissionState`
7. la misión ejecuta los waypoints secuencialmente

Esto convierte a `planning/` en una capa de servicio
entre la geometría y la ejecución de misión.

---

## Relación con otros paquetes

### Relación con `geometry/`

El paquete `planning/` depende directamente de las primitivas geométricas.

Utiliza:

- `Point2D`
- `RectangleArea`

Estas clases sirven como entrada y soporte de cálculo
para construir áreas y generar rutas.

### Relación con `runtime/`

El runtime usa directamente este paquete para:

- definir el área activa de trabajo
- generar planes de misión
- obtener listas ordenadas de waypoints

Por ejemplo:

- `SyncMissionRuntime.defineAreaRect(...)` usa `SweepAreaBuilder`
- `SyncMissionRuntime.planLawnmower(...)` usa `LawnmowerGenerator`

### Relación con `interpreter/`

El intérprete no invoca directamente los planificadores.
Solo despacha comandos SARL al runtime.

Esto mantiene separadas:

- semántica del lenguaje
- lógica de misión
- servicio de planificación

---

## Detalle de `SweepAreaBuilder`

Actualmente `SweepAreaBuilder` soporta:

### Construcción a partir de dos puntos

Dado que los puntos pueden venir en cualquier orden,
la clase calcula automáticamente:

- `minX`
- `maxX`
- `minY`
- `maxY`

Esto garantiza que el rectángulo resultante sea válido.

### Margen por defecto

Si no se indica margen manual, se aplica un `defaultMargin`
configurado al construir el builder.

### Margen manual

También permite indicar un margen concreto para una operación puntual.

### Control de área máxima

Si el área resultante supera `maxArea`:

- se ajusta automáticamente el margen
- o se rechaza la construcción si ni siquiera el área base cabe

Esto convierte a `SweepAreaBuilder` en una clase robusta
para preparar zonas de trabajo seguras y razonables.

---

## Detalle de `LawnmowerGenerator`

Actualmente `LawnmowerGenerator` soporta:

### Selección de esquina inicial

A partir de `startHint`, se elige la esquina del rectángulo
más cercana al punto de inicio.

Esto reduce desplazamientos innecesarios.

### Dirección inicial de barrido

Según la esquina elegida, el algoritmo decide:

- si avanzar en Y hacia arriba o hacia abajo
- si la primera fila se recorre de izquierda a derecha o al revés

### Generación alternada de filas

Cada fila genera dos waypoints extremos.

La dirección alterna automáticamente:

- ida
- vuelta
- ida
- vuelta

Esto produce el patrón clásico de barrido tipo cortacésped.

### Espaciado fijo entre líneas

La distancia entre filas viene dada por `laneSpacing`.

En esta versión no se fuerza una fila final adicional
si el espaciado no encaja exactamente con el borde del área.

---

## Programas de prueba incluidos

### `MainTestSweepAreaBuilder.java`

Este main permite validar de forma aislada:

- construcción de áreas desde dos puntos
- aplicación del margen por defecto
- aplicación de margen manual
- ajuste automático por límite de área máxima

Es útil para depurar la lógica de construcción de zonas
sin necesidad de integrar runtime ni simulador.

---

### `MainTestLawnmower.java`

Este main permite validar de forma aislada:

- construcción de un área mediante `SweepAreaBuilder`
- generación de waypoints mediante `LawnmowerGenerator`
- elección correcta de esquina inicial según `startHint`
- alternancia izquierda/derecha del patrón lawnmower

Es útil para inspeccionar el plan generado antes de integrarlo
en la ejecución real de una misión.

---

## Ventajas arquitectónicas de esta capa

La existencia de este paquete aporta varias ventajas claras:

### 1. Desacoplamiento

La planificación no está embebida en el intérprete ni en el adapter.

### 2. Reutilización

Los planificadores pueden usarse:

- desde el runtime
- desde pruebas manuales
- desde futuros motores de misión

### 3. Facilidad de evolución

Es posible añadir nuevos algoritmos sin reescribir
el núcleo del lenguaje.

### 4. Trazabilidad

La existencia de `Waypoint2D` con índice facilita:

- logs
- depuración
- análisis del plan generado

---

## Limitaciones actuales

Aunque el paquete ya es funcional, todavía existen algunas limitaciones deliberadas.

### Un único algoritmo de planificación integrado

Actualmente el sistema utiliza `LawnmowerGenerator`
como primera implementación concreta de planificación.

### Áreas limitadas a rectángulos

La entrada actual del builder está pensada para rectángulos alineados con ejes.

### Waypoints 2D sencillos

`Waypoint2D` todavía no incluye información adicional
como orientación, velocidad o acciones asociadas.

### Sin estrategia abstracta de planificación

En esta versión el runtime invoca directamente `LawnmowerGenerator`.
En el futuro podría introducirse una abstracción más general
para elegir entre distintos planificadores.

---

## Estado actual del paquete `planning/`

Actualmente este paquete proporciona una base funcional para:

- construir zonas de barrido
- aplicar márgenes y límites de área
- generar rutas tipo lawnmower
- representar waypoints con significado de misión
- validar algoritmos de forma aislada

Esto lo convierte en una capa de servicio fundamental
para las misiones SARL V3 en modo síncrono.

---

## Próxima evolución

Las siguientes iteraciones podrían ampliar esta capa con:

- nuevos algoritmos de planificación
- abstracciones comunes para distintos planificadores
- nuevos tipos de áreas
- waypoints enriquecidos con más información
- estrategias de cobertura más complejas
- integración con replanificación dinámica

La versión actual ya deja una base clara:
SARL puede invocar servicios de planificación concretos,
pero el lenguaje no queda conceptualmente ligado a un único algoritmo.