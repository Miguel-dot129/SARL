# SARL — Search And Rescue Language (TFG)

Repositorio del Trabajo de Fin de Grado centrado en el diseño e implementación de **SARL**, un lenguaje de alto nivel, **Turing completo**, orientado a la definición y ejecución de misiones para drones en escenarios SAR (Search and Rescue).

El proyecto abarca desde la definición del lenguaje (gramática ANTLR) hasta su ejecución completa sobre un simulador físico (Webots), pasando por un intérprete, runtime de misión y sistema de adaptación a diferentes backends.

---

## Estado actual del proyecto

El proyecto ha evolucionado desde una fase puramente conceptual a una arquitectura funcional completa:

- Gramática ANTLR operativa
- Intérprete funcional basado en Visitor
- Runtime de misión síncrono
- Sistema de adapters (consola / Webots)
- Controlador de dron en Webots integrado
- Ejecución completa de scripts `.sarl`

Actualmente es posible:

- ejecutar misiones en consola (modo determinista)
- ejecutar misiones en Webots (simulación física)
- validar sintaxis, semántica y ejecución end-to-end

---

## Arquitectura global

El sistema sigue una arquitectura en capas claramente separadas:


SARL (.sarl)
↓
ANTLR (Lexer + Parser)
↓
ParseTree (AST)
↓
Interpreter (semántica del lenguaje)
↓
SyncMissionRuntime (semántica de misión)
↓
MissionAdapter (abstracción de backend)
↓
Backend concreto:
- ConsoleMissionAdapter
- WebotsMissionAdapter
↓
Controlador (Webots)
↓
Simulación física (drone)


Esta separación permite:

- desacoplar lenguaje y ejecución
- cambiar fácilmente el backend
- evolucionar el sistema hacia modelos más complejos (eventos, asincronía, etc.)

---

## Estructura del repositorio


/
├── grammar/ # Gramática ANTLR del lenguaje SARL (.g4)
├── examples/ # Scripts .sarl de prueba (ejecutables y exploratorios)
├── protos/ # Recursos de Webots (PROTO del dron, etc.)
├── worlds/ # Escenarios de simulación en Webots

├── src/main/java/es/upm/sarl/
│ ├── adapter/ # Adaptadores de backend (consola / Webots)
│ ├── controller/ # Controlador del dron en Webots
│ ├── geometry/ # Primitivas geométricas (Point2D, Point3D, áreas)
│ ├── interpreter/ # Intérprete del lenguaje SARL
│ ├── main/ # Puntos de entrada (console / Webots)
│ ├── planning/ # Algoritmos de planificación (lawnmower, etc.)
│ └── runtime/ # Runtime de misión (estado + ejecución síncrona)

├── build/ # Artefactos de compilación
├── target/ # Salida de Maven

├── script_ejecucion.ps1 # Ejecución en consola
├── script_ejecucion_sarl_webots.ps1 # Ejecución en Webots

├── drone_log.csv # Logs de vuelo (generado)
├── pom.xml # Configuración Maven
├── README.md # Este documento
└── .gitignore


---

## Descripción de los módulos principales

### grammar/

Define la sintaxis del lenguaje SARL mediante ANTLR.

Incluye:

- definición de tokens
- reglas sintácticas
- generación de lexer y parser

Es el punto de entrada del lenguaje.

---
### interpreter/

Implementa la semántica del lenguaje SARL.

Incluye:

- evaluación de expresiones
- control de flujo
- gestión de variables
- invocación de acciones de misión

Utiliza el patrón Visitor sobre el AST generado por ANTLR.

---

### examples/

Contiene scripts `.sarl` que:

- sirven como casos de prueba reales
- validan el lenguaje y el runtime
- exploran futuras capacidades del DSL

Incluye:

- ejemplos básicos (variables, control de flujo)
- ejemplos de modo síncrono
- ejemplos de misiones SAR más complejas

---

### runtime/

Gestiona la ejecución de la misión.

Incluye:

- `MissionState` → estado de ejecución
- `SyncMissionRuntime` → ejecución síncrona bloqueante

Se encarga de:

- coordinar acciones
- garantizar orden de ejecución
- conectar con el adapter

---
### geometry/

Define primitivas geométricas inmutables:

- `Point2D`
- `Point3D`
- `RectangleArea`

Base para planificación y navegación.

---

### planning/

Contiene algoritmos de planificación de trayectorias.

Actualmente:

- `LawnmowerGenerator` (barrido tipo "cortacésped")
- generación de waypoints a partir de áreas

Será clave para misiones de búsqueda.

---

### adapter/

Define la abstracción entre el lenguaje y el backend.

Incluye:

- `MissionAdapter` (interfaz)
- `ConsoleMissionAdapter` (modo determinista)
- `WebotsMissionAdapter` (simulación física)

Permite desacoplar completamente la ejecución del lenguaje.

---

### controller/

Implementación del controlador del dron en Webots.

Incluye:

- control PID en cascada
- navegación a objetivos
- gestión de yaw (manual y automático)
- despegue, aterrizaje y hover
- logging de telemetría

Es el backend físico/simulado del sistema.

---

### main/

Puntos de entrada del sistema:

- `MainRunSarl` → ejecución en consola
- `MainRunSarlWebots` → ejecución en Webots

Orquestan todo el pipeline del sistema.

---

### worlds/ y protos/

Recursos de Webots:

- mundos de simulación (`.wbt`)
- definición del dron (`PROTO`)

---

## Cómo ejecutar el proyecto

### 1. Ejecución en consola

Permite validar el lenguaje sin simulación física:


MainRunSarl examples/10_modo_sincrono_v1.sarl


O usando el script:


script_ejecucion.ps1


---

### 2. Ejecución en Webots

Permite ejecutar la misión sobre el dron simulado:

1. abrir Webots con el mundo correspondiente
2. ejecutar:


MainRunSarlWebots examples/10_modo_sincrono_v1.sarl


O usando:


script_ejecucion_sarl_webots.ps1


---

## Filosofía del lenguaje SARL

SARL está diseñado como un DSL de alto nivel con los siguientes principios:

- expresar **qué hacer**, no **cómo hacerlo**
- sintaxis clara y cercana a Python
- abstracción del control de bajo nivel
- orientado a misiones, no a control de vuelo
- extensible hacia modelos reactivos

---

## Estado actual y siguientes pasos

### Actualmente implementado

- ejecución síncrona completa
- integración con Webots
- control de flujo en el lenguaje
- planificación básica
- separación clara de capas

---

### Próximas líneas de trabajo

- soporte de eventos (modo reactivo)
- ampliación del lenguaje (más comandos de misión)
- mejoras en planificación
- integración más rica con sensores
- optimización del controlador
- análisis avanzado de logs

---

## Notas finales

Este repositorio no solo implementa un lenguaje,
sino que demuestra su viabilidad completa:

> desde una especificación de alto nivel hasta la ejecución real sobre un dron simulado.

SARL constituye una base sólida para futuras extensiones en el ámbito de robótica, UAVs y lenguajes específicos de dominio.