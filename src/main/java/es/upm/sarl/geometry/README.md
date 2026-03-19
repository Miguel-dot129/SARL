# geometry/ — Primitivas geométricas básicas para SARL (V3)

Este directorio contiene las clases geométricas básicas utilizadas por el proyecto **SARL**
en la versión **SARL V3**.

Estas clases no forman parte del lenguaje SARL en sentido estricto:
no definen sintaxis, no interpretan programas y no ejecutan misiones.

Su papel es proporcionar una base geométrica simple, clara y reutilizable
sobre la que otras capas del sistema puedan apoyarse, especialmente:

- planificación de áreas de trabajo
- generación de rutas
- representación de waypoints
- sincronización de posición de misión
- cálculos de distancia y dimensiones

En otras palabras, `geometry/` actúa como una pequeña librería de utilidades geométricas
del dominio de misión.

---

## Rol del paquete `geometry/` en la arquitectura SARL

Dentro del proyecto SARL, este paquete ocupa una posición de soporte
para otras capas de más alto nivel.

Estas clases son utilizadas por componentes como:

- `SweepAreaBuilder`
- `LawnmowerGenerator`
- `MissionState`
- `SyncMissionRuntime`
- `MissionAdapter`
- `WebotsMissionAdapter`

Su función principal es proporcionar representaciones claras y seguras de:

- puntos en 2D
- puntos en 3D
- áreas rectangulares alineadas con los ejes

Esto permite que el resto del sistema trabaje con objetos geométricos explícitos
en lugar de listas de números sueltas o estructuras poco legibles.

---

## Estructura actual del directorio

    geometry/
    ├── MainTestGeometry.java
    ├── Point2D.java
    ├── Point3D.java
    ├── RectangleArea.java
    └── README.md

El paquete contiene tres primitivas geométricas principales
y un pequeño programa de prueba manual.

---

## Componentes principales

### `Point2D.java`

`Point2D` representa un punto en el plano horizontal `(X,Y)`.

Se utiliza para modelar posiciones geométricas puras,
sin significado directo de misión por sí mismas.

Ejemplos de uso:

- vértices de un área
- esquinas de un rectángulo
- coordenadas de waypoints en el plano XY
- ayuda para algoritmos de planificación

Se implementa como `record`, lo que aporta:

- inmutabilidad
- sintaxis más compacta
- semántica clara de “dato puro”

Además, incluye una operación básica de distancia euclídea:

    d = sqrt((x1 - x2)^2 + (y1 - y2)^2)

Esta distancia se utiliza, por ejemplo, para:

- elegir la esquina más cercana de un área
- comparar posiciones geométricas
- preparar posibles optimizaciones futuras

---

### `Point3D.java`

`Point3D` representa un punto en el espacio tridimensional `(X,Y,Z)`.

A diferencia de `Point2D`, aquí sí se incluye la altitud,
por lo que este tipo se utiliza para modelar posiciones completas del dron
dentro del mundo de simulación o del estado lógico de misión.

Ejemplos de uso:

- `home`
- posición actual del dron
- posiciones reportadas por el adapter
- cálculos de error espacial

También se implementa como `record`, por lo que comparte las ventajas de inmutabilidad
y simplicidad de representación.

Incluye dos operaciones útiles:

### Distancia horizontal en XY

Ignora la altitud y calcula solo la distancia en el plano:

    d = sqrt((x1 - x2)^2 + (y1 - y2)^2)

Esto resulta muy útil para:

- comprobar llegada a un waypoint
- medir error horizontal
- evaluar tolerancias de navegación

### Distancia 3D completa

Tiene en cuenta también la coordenada Z:

    d = sqrt((x1 - x2)^2 + (y1 - y2)^2 + (z1 - z2)^2)

Puede utilizarse para:

- análisis de error total
- métricas de rendimiento del vuelo
- cálculos geométricos más avanzados

---

### `RectangleArea.java`

`RectangleArea` representa un área rectangular alineada con los ejes del plano XY.

Importante:

- el rectángulo no admite rotación
- se define por sus límites extremos
- está pensado para simplificar planificación y validación geométrica

Se modela mediante:

- `minX`
- `maxX`
- `minY`
- `maxY`

Esto hace que sea muy adecuado para:

- representar zonas de trabajo
- calcular dimensiones
- generar barridos tipo lawnmower
- expandir áreas con margen
- validar límites geométricos

El constructor exige:

- `minX <= maxX`
- `minY <= maxY`

Esto evita que existan rectángulos inválidos dentro del sistema.

`RectangleArea` ofrece operaciones básicas como:

### Cálculo de ancho

    width = maxX - minX

### Cálculo de altura

    height = maxY - minY

### Expansión por margen

El método `expanded(margin)` devuelve un nuevo rectángulo
expandido simétricamente en las cuatro direcciones.

Ejemplo conceptual:

- rectángulo original: `(minX, maxX, minY, maxY)`
- margen: `2.0`
- resultado:
  - `minX - 2`
  - `maxX + 2`
  - `minY - 2`
  - `maxY + 2`

Esta operación no modifica la instancia original,
lo que mantiene la inmutabilidad lógica de la geometría.

---

## Filosofía de diseño de `geometry/`

Las clases de este paquete siguen una filosofía muy sencilla:

- representar datos geométricos de forma explícita
- evitar estructuras ambiguas o poco expresivas
- mantener inmutabilidad siempre que sea posible
- ofrecer operaciones pequeñas, claras y reutilizables

Esto hace que el paquete sea fácil de entender, probar y reutilizar
desde distintas capas del proyecto.

---

## Por qué estas clases no forman parte del lenguaje SARL en sentido estricto

Una idea importante de diseño es distinguir entre:

- **lenguaje SARL**
- **servicios y estructuras de dominio usados por SARL**

Las clases de `geometry/` no forman parte de la sintaxis del lenguaje.
No son construcciones gramaticales ni semánticas del intérprete.

Sin embargo, sí forman parte del **ecosistema técnico que permite ejecutar misiones SARL**.

Por ejemplo:

- el lenguaje puede expresar puntos 2D o 3D
- el runtime puede almacenar posiciones
- el planner puede construir rutas

Pero para todo ello hacen falta estructuras geométricas claras.

En ese sentido, `geometry/` no define el lenguaje,
pero sí proporciona una base de dominio reutilizable para la misión.

---

## Relación con otros paquetes

### Relación con `planning/`

El paquete `planning/` utiliza directamente las clases geométricas para:

- construir áreas de trabajo
- generar rutas de barrido
- definir waypoints

Por ejemplo:

- `SweepAreaBuilder` usa `Point2D` y `RectangleArea`
- `LawnmowerGenerator` usa `Point2D` y `RectangleArea`

### Relación con `runtime/`

El runtime utiliza `Point3D` para:

- `home`
- posición actual del dron
- sincronización entre misión y backend

También utiliza `RectangleArea` para almacenar el área activa de trabajo.

### Relación con `adapter/`

Los adapters usan `Point3D` como tipo geométrico común
para devolver la posición actual del dron al runtime.

---

## Programa de prueba: `MainTestGeometry.java`

`MainTestGeometry` es un pequeño programa de prueba manual
para validar las clases geométricas básicas.

Su objetivo es comprobar:

- construcción correcta de un rectángulo a partir de dos puntos
- funcionamiento de `width()`
- funcionamiento de `height()`
- funcionamiento de `expanded(margin)`

No forma parte del sistema de misión ni del lenguaje,
sino que se utiliza únicamente como prueba aislada de la capa geométrica.

Ejemplo conceptual de lo que valida:

- dos puntos arbitrarios del plano
- cálculo de mínimos y máximos
- creación de `RectangleArea`
- expansión del rectángulo con margen

Esto resulta útil para verificar que la capa geométrica funciona correctamente
antes de integrarla en planificación o runtime.

---

## Ventajas de esta capa geométrica

La existencia de este paquete aporta varias ventajas arquitectónicas:

### 1. Claridad semántica

Es más expresivo trabajar con:

- `Point2D`
- `Point3D`
- `RectangleArea`

que con arrays de números o parámetros sueltos.

### 2. Reutilización

La misma geometría puede reutilizarse desde:

- runtime
- planning
- adapters
- pruebas

### 3. Aislamiento de complejidad

Los cálculos básicos de distancia, anchura, altura y expansión
quedan encapsulados en sus propias clases.

### 4. Facilidad de prueba

Al ser clases pequeñas y puras, son fáciles de probar de forma aislada.

---

## Limitaciones actuales

Aunque esta capa es suficiente para la fase actual del proyecto,
tiene algunas limitaciones deliberadas.

### Geometría simple

Actualmente solo se soportan:

- puntos 2D
- puntos 3D
- rectángulos alineados con ejes

No existen todavía:

- polígonos arbitrarios
- áreas rotadas
- curvas
- volúmenes 3D complejos

### Sin sistema de coordenadas avanzado

La geometría se maneja en coordenadas cartesianas simples.
No se contemplan transformaciones más complejas ni marcos múltiples.

### Operaciones limitadas

Las clases proporcionan solo operaciones mínimas,
pensadas para el alcance actual del proyecto.

---

## Estado actual del paquete `geometry/`

Actualmente, este paquete proporciona una base geométrica suficiente para:

- representar posiciones en 2D y 3D
- modelar áreas rectangulares simples
- calcular distancias básicas
- apoyar la generación de rutas
- apoyar la sincronización del estado de misión

Esto lo convierte en una capa pequeña pero esencial
para el resto de la arquitectura SARL V3.

---

## Próxima evolución

Las siguientes iteraciones podrían ampliar esta capa con:

- nuevos tipos de área
- geometría de polígonos
- utilidades adicionales de distancia y orientación
- transformaciones geométricas
- soporte para modelos de planificación más complejos

Por ahora, el diseño actual prioriza simplicidad, claridad e integración
con las necesidades reales de planificación y misión del proyecto.