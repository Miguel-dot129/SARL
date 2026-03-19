# controller/ — Controlador Java del dron en Webots (V3)

Este directorio contiene el controlador actual en Java utilizado junto con **Webots**
para gobernar el cuadricóptero en las pruebas del proyecto **SARL**.

Aunque el controlador no forma parte del lenguaje SARL en sentido estricto,
sí constituye una pieza clave del ecosistema del TFG, ya que proporciona
el backend físico/simulado necesario para validar que una misión escrita en SARL
puede ejecutarse sobre un dron dentro del simulador.

En la arquitectura actual, este controlador actúa como la capa más cercana
al comportamiento real del dron en Webots.

---

## Rol del controlador en la arquitectura SARL

Dentro del proyecto, el controlador ocupa el último nivel de la cadena de ejecución:

- la **gramática** define la sintaxis del lenguaje
- el **Interpreter** interpreta el programa SARL
- el **SyncMissionRuntime** coordina la lógica de misión
- el **WebotsMissionAdapter** traduce la misión a acciones físicas
- el **Controlador** ejecuta dichas acciones dentro de Webots

Es decir:

    SARL
        ↓
    Interpreter
        ↓
    SyncMissionRuntime
        ↓
    WebotsMissionAdapter
        ↓
    Controlador
        ↓
    Webots

Esto significa que el controlador no entiende SARL directamente.
Su función es ofrecer una capa de control de vuelo suficientemente estable
para que otras capas superiores puedan utilizarlo.

---

## Estado actual del paquete

El archivo principal es `Controlador.java`.

En esta versión, el controlador ya no debe entenderse solo como
un experimento aislado de vuelo, sino como una pieza integrada
en la arquitectura síncrona de SARL V3.

Actualmente proporciona:

- control PID en cascada para altitud, actitud, navegación horizontal y yaw
- métodos de alto nivel para despegar, moverse, hacer hover y aterrizar
- soporte de auto-yaw hacia el objetivo
- soporte de yaw manual
- logs CSV para análisis de comportamiento
- un `main` interno de pruebas aisladas

Las antiguas clases experimentales ya no forman parte del flujo activo
de esta versión y el foco queda puesto en `Controlador.java`.

---

## Arquitectura general del controlador

El controlador utiliza una arquitectura de control **en cascada**.

Esto significa que el problema de controlar el dron se descompone
en varios niveles jerárquicos:

### 1. Lazo externo de navegación

Se encarga de transformar objetivos espaciales en referencias de movimiento.

Incluye:

- control de altitud
- control horizontal X/Y
- control de yaw orientado al objetivo

### 2. Lazo interno de estabilización

Se encarga de convertir esas referencias en correcciones físicas
sobre los motores del dron.

Incluye:

- control de actitud (roll y pitch)
- control de yaw
- mezcla de motores

Cada uno de estos lazos se ejecuta de forma secuencial dentro del método `run()`,
que representa el bucle principal de control del dron en Webots.

---

## Sensores y actuadores utilizados

El controlador se apoya en los dispositivos del PROTO del dron en Webots.

### Sensores

- `GPS`
  - proporciona posición `(x, y, z)`
- `InertialUnit`
  - proporciona orientación `(roll, pitch, yaw)`
- `Gyro`
  - proporciona velocidades angulares
- `Compass`
  - disponible aunque todavía no se usa activamente
- `Camera`
  - inicializada para futuras extensiones o pruebas

### Actuadores

- `rear left propeller`
- `rear right propeller`
- `front right propeller`
- `front left propeller`

Los motores se controlan en modo velocidad,
y el controlador se encarga de aplicar el signo correcto
según la orientación física de cada hélice en el PROTO.

---

## Principales bloques de control

### Control de actitud (`PDAttitude`)

Es el lazo interno encargado de estabilizar:

- `roll`
- `pitch`

Flujo general:

1. lee la orientación actual desde la IMU
2. calcula el error respecto a `rollRef` y `pitchRef`
3. usa el giroscopio para el término derivativo
4. calcula correcciones PD
5. añade la corrección de yaw
6. realiza la mezcla de motores
7. aplica velocidades resultantes a cada hélice

Este lazo es el responsable directo de mantener el dron estable
y de materializar físicamente las referencias generadas por otros lazos.

---

### Control de altitud (`PDAltitudeControl`)

Es el controlador vertical del dron.

En la versión actual utiliza un **PID completo** con varias mejoras de estabilidad.

Características importantes:

- usa la altitud medida por GPS
- utiliza una referencia suavizada (`targetAltZ`)
- diferencia entre:
  - altitud pedida por la misión (`targetAltZCmd`)
  - altitud realmente seguida por el PID (`targetAltZ`)
- limita la velocidad de cambio de referencia con `ALT_REF_RATE`
- usa derivada sobre la medida (`vz`) para evitar *derivative kick*
- limita la integral para evitar *wind-up*
- genera `baseThrottle` como empuje común

Resultado práctico:

- subidas y bajadas suaves
- menos latigazos cuando cambia la consigna
- transiciones más estables entre altitudes

---

### Control horizontal (`PDHorizontalControl`)

El control horizontal actual ya no está planteado como una simple conversión
de error de posición a inclinación.

En esta versión se apoya en una idea más robusta:

- calcular una **velocidad deseada** hacia el objetivo
- comparar esa velocidad con la velocidad real estimada
- transformar el error de velocidad en inclinaciones limitadas

Flujo general:

1. lee la posición actual con el GPS
2. estima `vx` y `vy` derivando la posición
3. calcula el vector hacia el objetivo
4. calcula la distancia restante
5. si el objetivo está lejos, usa `V_CRUISE`
6. si el objetivo está cerca, frena progresivamente
7. transforma el error del marco mundo al marco del dron
8. convierte ese error a `pitchRef` y `rollRef`
9. aplica límites de inclinación

Ventajas de este enfoque:

- movimiento más natural
- frenado más suave al llegar
- mejor comportamiento al combinarlo con yaw
- menos agresividad que un control puramente proporcional sobre posición

---

### Control de yaw (`PDYawControl`)

El yaw se controla mediante un PD específico sobre el eje Z.

Características principales:

- `yawRef` se normaliza a `[-pi, pi]`
- el error angular se envuelve también a `[-pi, pi]`
- el término derivativo se obtiene directamente del giroscopio
- la salida del controlador (`yawU`) se mezcla en los motores

Esto permite:

- estabilizar la orientación
- realizar giros manuales controlados
- orientar el dron automáticamente hacia el objetivo cuando corresponde

Además, se almacenan variables de diagnóstico como:

- `yawNow`
- `yawError`
- `yawRate`
- `yawU`

para poder analizarlas luego en el CSV.

---

## Gestión de yaw: modo manual y auto-yaw

Una de las mejoras más importantes respecto al estado anterior
es la incorporación explícita de dos modos de yaw:

### `MANUAL`

En este modo:

- `yawRef` se fija explícitamente
- no se recalcula automáticamente hacia el objetivo

Se utiliza, por ejemplo, en:

- hover estable
- rotaciones manuales
- despegue
- aterrizaje

### `AUTO_FACE_TARGET`

En este modo:

- el dron ajusta automáticamente `yawRef`
  para mirar hacia el punto objetivo actual

Para evitar giros bruscos:

- solo se activa si el objetivo está suficientemente lejos
- el cambio de `yawRef` está limitado por `YAW_REF_MAX_RATE`

Esto aporta una orientación más natural en navegación,
sin introducir saltos bruscos en el heading del dron.

---

## Métodos de alto nivel del controlador

Además de los lazos de control continuo,
el controlador expone varios métodos de alto nivel
que otras capas pueden utilizar.

### `hoverHere()`

Congela como referencia:

- la posición actual
- la altitud actual
- el yaw actual

y cambia a modo `MANUAL`.

Se usa para mantener un hover estable en el punto actual.

---

### `changeAltitude(double newAlt)`

Actualiza la altitud objetivo pedida (`targetAltZCmd`)
sin modificar:

- `targetX`
- `targetY`
- `yawRef`

La transición real se produce progresivamente dentro del control vertical.

---

### `moveTo(double x, double y, double z)`

Actualiza el objetivo espacial del dron:

- `targetX`
- `targetY`
- `targetAltZCmd`

y activa el modo `AUTO_FACE_TARGET`.

Es el método principal de navegación hacia un waypoint.

---

### `setYaw(double newYaw)`

Fija un yaw absoluto en modo manual.

Se utiliza para pruebas de rotación o para inspección orientada.

---

### `enableAutoYaw()`

Reactiva el auto-yaw partiendo del yaw actual
para evitar saltos bruscos de referencia.

---

### `takeoff(double altitude)`

Inicia un despegue controlado.

Flujo general:

- arma motores
- fija posición XY actual como referencia
- fija el yaw actual en modo manual
- reinicia estado del control vertical
- pide una nueva altitud objetivo

El ascenso real ocurre después en el bucle `run()`.

---

### `land()`

Inicia un aterrizaje controlado.

Flujo general:

- mantiene la posición horizontal actual
- congela el yaw actual
- fija una altitud objetivo muy baja
- activa el flag `landing`

El apagado real de motores no ocurre aquí,
sino dentro de `run()` cuando se detecta proximidad al suelo.

---

## Bucle principal de control: `run()`

`run()` es el corazón del controlador.

Mientras la simulación siga activa:

- ejecuta `robot.step(timeStep)`
- si los motores están armados:
  - actualiza el control vertical
  - actualiza el control horizontal
  - actualiza el control de actitud
  - comprueba si debe finalizar el aterrizaje
- registra el estado en el CSV

Este diseño es importante porque deja clara la separación entre:

### Comandos de alto nivel

Métodos como:

- `takeoff()`
- `moveTo()`
- `hoverHere()`
- `land()`

solo cambian objetivos y flags.

### Ejecución física real

El movimiento real del dron se produce aquí,
paso a paso, dentro del bucle de control.

---

## Gestión de motores

El controlador distingue claramente entre:

### `armMotors()`

- pone los motores en régimen idle
- deja al dron listo para generar empuje
- no despega por sí solo

### `shutdownMotors()`

- apaga motores
- desarma el sistema
- restaura `baseThrottle`
- se usa al finalizar el aterrizaje

Esta separación hace más limpia la lógica de despegue y aterrizaje.

---

## Logs y trazabilidad

El controlador escribe telemetría en `drone_log.csv`.

Actualmente registra, entre otros datos:

- tiempo
- posición
- orientación
- referencias de actitud
- `baseThrottle`
- errores horizontales
- integral de altitud
- variables de diagnóstico del yaw

Esto permite analizar posteriormente:

- estabilidad del vuelo
- comportamiento de los PIDs
- calidad de seguimiento de objetivos
- rendimiento de yaw y navegación

Además, varios métodos imprimen mensajes de consola del tipo:

    CMD | takeoff(...)
    CMD | moveTo(...)
    CMD | hoverHere(...)
    CMD | land()

Esto facilita seguir la ejecución durante pruebas manuales.

---

## Main interno de pruebas

`Controlador.java` mantiene un `main()` interno
que actúa como entorno de prueba aislado del controlador.

Este main no forma parte de la ejecución oficial de SARL,
pero es muy útil para validar de forma independiente:

- despegue
- aterrizaje
- hover
- yaw manual
- navegación por waypoints
- criterio de llegada estable

También incluye helpers como:

- `waitUntilArrived(...)`
- `holdSeconds(...)`

que se usan únicamente en el contexto de esa demo manual.

Este entorno de prueba permite depurar el comportamiento físico
del dron antes de integrarlo con el runtime y el lenguaje.

---

## Evolución respecto al estado anterior

Respecto al estado descrito en la documentación anterior,
esta versión del controlador introduce avances importantes.

### 1. Integración real con SARL V3

El controlador ya no debe verse solo como una pieza experimental,
sino como el backend activo del pipeline completo:

    SARL → Interpreter → Runtime → Adapter → Controlador → Webots

### 2. Mejora del control horizontal

Se ha pasado a un enfoque basado en velocidad deseada,
más suave y robusto que uno basado solo en error de posición.

### 3. Auto-yaw funcional

Ahora existe una lógica explícita de orientación automática
hacia el objetivo, con limitación de velocidad angular de referencia.

### 4. Gestión más clara de despegue y aterrizaje

Se introducen de forma más limpia:

- `armMotors()`
- `shutdownMotors()`
- flag `landing`

### 5. Semántica síncrona soportable desde capas superiores

Gracias al adapter, el runtime puede esperar a que el controlador
complete acciones reales como despegue, movimiento o aterrizaje.

---

## Qué forma parte del TFG y qué no

Es importante distinguir entre:

### Núcleo del TFG

- lenguaje SARL
- gramática ANTLR
- intérprete
- runtime de misión
- adapters
- arquitectura de ejecución

### Infraestructura de validación experimental

- controlador del dron en Webots

El controlador no es el objetivo principal del lenguaje,
pero sí ha sido una pieza indispensable para poder validar
de manera seria el funcionamiento del sistema.

En la práctica, se ha convertido en una base experimental esencial
para demostrar que el lenguaje puede gobernar un dron en simulación.

---

## Limitaciones actuales

Aunque el controlador ya es funcional, siguen existiendo áreas de mejora.

### Hover no perfecto

Todavía pueden existir pequeñas derivas durante el hover.

### Velocidad de misión no integrada del todo

La velocidad existe a nivel de misión,
pero su traducción fina al controlador todavía puede mejorarse.

### Dependencia de ajustes manuales

Las ganancias PID siguen ajustadas a mano
y podrían necesitar recalibración en nuevos escenarios.

### Sin control reactivo avanzado

El controlador aún no participa en una arquitectura de eventos.
Actualmente trabaja bien en el modelo síncrono bloqueante.

---

## Estado actual del controlador

Actualmente el controlador proporciona una base suficientemente sólida para:

- ejecutar navegación básica en Webots
- despegar y aterrizar de forma controlada
- mantener hover
- seguir waypoints
- orientar el dron hacia objetivos
- registrar telemetría para análisis
- servir como backend del modo síncrono de SARL V3

Esto lo convierte en una pieza madura dentro del ecosistema actual del proyecto.

---

## Próximos pasos

Las siguientes iteraciones podrían centrarse en:

- mejorar aún más el hover
- integrar mejor la velocidad de misión
- refinar la orientación previa al movimiento
- introducir detección de eventos o sensores
- apoyar la futura arquitectura reactiva/asíncrona
- seguir puliendo logs y comportamiento fino

En cualquier caso, la base actual ya permite validar de forma convincente
la conexión completa entre SARL y la simulación en Webots.