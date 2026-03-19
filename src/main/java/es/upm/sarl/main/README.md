# main/ — Puntos de entrada para ejecución completa de SARL (V3)

Este directorio contiene los **puntos de entrada principales**
para ejecutar programas SARL en la versión **SARL V3**.

A diferencia de otros paquetes del proyecto:

- `grammar/` define la sintaxis del lenguaje
- `interpreter/` define su semántica
- `runtime/` ejecuta la lógica de misión
- `adapter/` conecta con la plataforma concreta

el paquete `main/` se encarga de **orquestar la ejecución completa**
del sistema desde un script `.sarl` hasta su backend final.

En otras palabras, aquí viven las clases que arrancan el pipeline completo.

---

## Rol del paquete `main/` en la arquitectura SARL

Dentro del proyecto SARL, este paquete tiene una responsabilidad muy clara:

- cargar un script SARL desde disco
- construir el pipeline de ANTLR
- crear las capas necesarias de ejecución
- lanzar la misión completa sobre el backend deseado

Esto convierte a `main/` en la puerta de entrada práctica al sistema.

Su existencia permite separar claramente:

- la lógica del lenguaje
- la lógica de misión
- la lógica de arranque y composición del sistema

---

## Estructura actual del directorio

    main/
    ├── MainRunSarl.java
    ├── MainRunSarlWebots.java
    └── README.md

Actualmente el paquete contiene dos puntos de entrada principales:

- `MainRunSarl.java`
- `MainRunSarlWebots.java`

Ambos ejecutan scripts SARL completos,
pero lo hacen sobre backends distintos.

---

## Componentes principales

### `MainRunSarl.java`

`MainRunSarl` es el punto de entrada principal para ejecutar un script SARL
en modo síncrono usando el adapter de consola.

Su objetivo es permitir probar de forma completa:

- el parser ANTLR
- el intérprete
- el runtime de misión
- la planificación
- la ejecución de comandos

sin depender todavía de Webots.

Es, por tanto, la forma más simple de ejecutar una misión SARL
completa sobre un backend ligero y determinista.

### Flujo general de `MainRunSarl`

El flujo de ejecución es:

1. leer el script `.sarl` desde disco
2. construir el pipeline clásico de ANTLR
3. parsear el programa completo
4. crear el adapter de consola
5. crear el runtime síncrono
6. crear el intérprete
7. ejecutar el script recorriendo el AST

Conceptualmente, la cadena que monta es:

    SARL script
        ↓
    ANTLR lexer/parser
        ↓
    Interpreter
        ↓
    SyncMissionRuntime
        ↓
    ConsoleMissionAdapter

Esto permite validar toda la arquitectura sin meter todavía
la complejidad física del simulador.

### Utilidad principal de `MainRunSarl`

Se utiliza principalmente para:

- probar ejemplos `.sarl`
- depurar errores de sintaxis
- validar semántica del intérprete
- comprobar el funcionamiento del runtime
- verificar la planificación sin depender de Webots

---

### `MainRunSarlWebots.java`

`MainRunSarlWebots` es el punto de entrada principal para ejecutar un script SARL
sobre el simulador **Webots**.

A diferencia de `MainRunSarl`, aquí el sistema se conecta al controlador real
del dron dentro del simulador mediante `WebotsMissionAdapter`.

Esta clase representa la conexión completa de la fase síncrona:

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

Es, por tanto, el main que demuestra la integración completa
del lenguaje con el simulador.

### Flujo general de `MainRunSarlWebots`

El flujo de ejecución es:

1. leer el script `.sarl`
2. construir el pipeline ANTLR
3. parsear el programa completo
4. crear el `Controlador` de Webots
5. arrancar el bucle continuo del controlador en un hilo separado
6. crear `WebotsMissionAdapter`
7. crear `SyncMissionRuntime`
8. crear `Interpreter`
9. ejecutar el script SARL sobre el simulador

Este punto de entrada es especialmente importante porque conecta
por primera vez todas las capas del proyecto en una ejecución real.

---

## Pipeline común de ambos mains

Ambos puntos de entrada comparten un esquema básico de arranque.

### 1. Carga del script SARL

Si el usuario pasa una ruta por argumento, se usa esa.
En caso contrario, se toma un ejemplo por defecto.

Ejemplo conceptual:

    String missionPath = (args.length > 0)
            ? args[0]
            : "examples/10_modo_sincrono_v1.sarl";

### 2. Lectura del fichero

Se lee el contenido completo del script desde disco.

### 3. Pipeline ANTLR

Ambos mains siguen el pipeline clásico de ANTLR:

- `CharStream`
- `SARLLexer`
- `CommonTokenStream`
- `SARLParser`

Es decir:

    CharStream
        ↓
    Lexer
        ↓
    TokenStream
        ↓
    Parser
        ↓
    ParseTree

### 4. Listener de errores personalizado

En ambos casos se sustituye el manejador de errores por defecto de ANTLR
por un listener propio que lanza una excepción clara y directa.

Esto mejora mucho la experiencia de depuración de scripts SARL,
porque evita mensajes genéricos poco útiles.

En lugar de dejar que ANTLR imprima errores dispersos,
el sistema lanza una excepción con:

- línea
- columna
- mensaje de error sintáctico

### 5. Parseo del programa completo

Ambos mains parsean el nodo raíz `program()`,
obteniendo el árbol sintáctico abstracto que luego recorrerá el intérprete.

### 6. Construcción de la cadena de ejecución

Una vez generado el AST, cada main construye la cadena completa
de ejecución según el backend elegido.

---

## Diferencia principal entre ambos mains

La diferencia fundamental entre `MainRunSarl` y `MainRunSarlWebots`
está en el backend que montan.

### `MainRunSarl`

Monta una cadena ligera y determinista:

    ConsoleMissionAdapter
        ↓
    SyncMissionRuntime
        ↓
    Interpreter

Aquí no existe simulación física real.

### `MainRunSarlWebots`

Monta una cadena completa sobre simulador:

    Controlador
        ↓
    WebotsMissionAdapter
        ↓
    SyncMissionRuntime
        ↓
    Interpreter

Aquí sí existe:

- controlador real del dron
- bucle continuo de Webots
- movimientos físicos simulados
- espera real a que las acciones terminen

---

## Gestión del controlador en Webots

Una diferencia crítica del main de Webots es que el controlador necesita
estar ejecutando continuamente `robot.step(...)`
para que el simulador avance y el dron responda.

Por eso `MainRunSarlWebots` arranca el controlador en un hilo aparte.

Flujo conceptual:

- el hilo del controlador mantiene vivo el step continuo
- el hilo principal ejecuta el script SARL
- el adapter bloquea cuando necesita esperar a que una acción termine

Además, se introduce un pequeño retraso inicial para dar margen a que:

- sensores se inicialicen
- el controlador arranque correctamente
- Webots entre en régimen normal

Esto evita condiciones de carrera al empezar la misión.

---

## Por qué este paquete no contiene lógica de lenguaje

Aunque estos mains parecen importantes funcionalmente,
no deben confundirse con el núcleo del lenguaje.

Este paquete no:

- define gramática
- evalúa expresiones
- planifica rutas
- mantiene estado de misión
- mueve físicamente el dron

Su responsabilidad es más simple:
**componer y arrancar** las piezas que ya existen.

Esto aporta claridad arquitectónica y facilita el mantenimiento.

---

## Relación con otros paquetes

### Relación con `grammar/`

Los mains arrancan la ejecución a partir de scripts SARL
y por tanto construyen el pipeline ANTLR usando la gramática generada.

### Relación con `interpreter/`

Una vez obtenido el `ParseTree`, los mains crean el `Interpreter`
y delegan en él la ejecución semántica del programa.

### Relación con `runtime/`

Los mains crean `SyncMissionRuntime`, que será el encargado
de la lógica operativa de misión.

### Relación con `adapter/`

Cada main elige el adapter concreto según el entorno:

- `ConsoleMissionAdapter`
- `WebotsMissionAdapter`

### Relación con `controller/`

Solo `MainRunSarlWebots` depende directamente del controlador de Webots,
porque necesita crear la conexión física con el simulador.

---

## Casos de uso principales

### Uso de `MainRunSarl`

Este main es ideal para:

- validar la arquitectura completa sin simulador
- probar scripts SARL rápidamente
- depurar errores de sintaxis o semántica
- comprobar el comportamiento del runtime con logs limpios

### Uso de `MainRunSarlWebots`

Este main es ideal para:

- ejecutar misiones SARL reales sobre Webots
- validar la integración completa del pipeline
- probar planificación y navegación en simulación física
- demostrar el paso de SARL como DSL a sistema ejecutable real

---

## Estado actual del paquete `main/`

Actualmente este paquete proporciona:

- un punto de entrada simple y ligero basado en consola
- un punto de entrada completo basado en Webots
- integración con ANTLR
- gestión de errores sintácticos más clara
- composición explícita de todas las capas del sistema

Esto convierte a `main/` en una pieza clave para validar
el funcionamiento completo de SARL V3.

---

## Limitaciones actuales

Aunque ya es funcional, esta capa todavía tiene algunas limitaciones deliberadas.

### Gestión de errores sencilla

Actualmente el tratamiento de errores se centra sobre todo
en fallos sintácticos del parser y excepciones directas de ejecución.

### Sin configuración avanzada de arranque

No existe todavía una capa de configuración más sofisticada para elegir:

- backend
- tolerancias
- perfiles de ejecución
- parámetros de misión

### Sin modo reactivo o asíncrono

Los mains actuales están pensados para el modelo síncrono actual.
Una futura evolución hacia eventos puede requerir ampliaciones.

---

## Próxima evolución

Las siguientes iteraciones podrían ampliar este paquete con:

- selección de backend por parámetros
- arranques más configurables
- integración de modos reactivos
- carga de perfiles de misión
- ejecución automatizada de baterías de ejemplos
- herramientas de test o benchmarking

En cualquier caso, la función esencial del paquete seguirá siendo la misma:
montar y arrancar la cadena completa de ejecución de SARL.