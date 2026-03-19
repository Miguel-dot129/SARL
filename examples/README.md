# examples/ — Ejemplos de misiones SARL (V3)

Este directorio contiene una colección de archivos `.sarl`
que representan ejemplos de uso del lenguaje **SARL** (Search And Rescue Language).

A diferencia de versiones anteriores del proyecto:

- estos ejemplos **ya no son solo conceptuales**
- ahora forman parte del **flujo real de ejecución del sistema**
- pueden ejecutarse a través de los mains del proyecto (`MainRunSarl`, `MainRunSarlWebots`)

---

## Estado actual

El lenguaje SARL se encuentra en una fase funcional inicial:

- existe una **gramática ANTLR**
- existe un **intérprete**
- existe un **runtime síncrono**
- existe integración con:
  - consola (modo simulación lógica)
  - Webots (modo simulación física)

Por tanto, muchos de los ejemplos de esta carpeta ya pueden utilizarse como:

- casos de prueba del lenguaje
- validación del intérprete
- validación del runtime
- validación de integración con el controlador

---

## Objetivos de esta carpeta

Los ejemplos cumplen varios objetivos dentro del proyecto:

### 1. Validar el lenguaje SARL

Permiten comprobar que:

- la sintaxis es correcta
- el parser funciona
- el intérprete ejecuta correctamente las instrucciones

---

### 2. Validar la semántica de misión

Permiten probar:

- ejecución de comandos de misión
- control de flujo (`if`, `while`, etc.)
- uso de variables
- lógica de navegación

---

### 3. Validar el runtime

Sirven para comprobar que el `SyncMissionRuntime`:

- ejecuta comandos de forma síncrona
- bloquea correctamente hasta finalizar acciones
- mantiene coherencia en la ejecución de la misión

---

### 4. Validar integración con Webots

Algunos ejemplos pueden ejecutarse en el simulador,
permitiendo comprobar:

- despegue y aterrizaje
- navegación a waypoints
- comportamiento físico del dron
- integración completa del pipeline

---

## Tipos de ejemplos incluidos

La carpeta contiene distintos tipos de scripts `.sarl`,
que reflejan diferentes niveles de complejidad del lenguaje.

---

### Ejemplos básicos de lenguaje

Estos ejemplos están orientados a validar la base del DSL:

- `00_minimo.sarl`
- `01_variables.sarl`
- `02_control_flow.sarl`
- `03_control_flow_else.sarl`
- `04_control_flow_condition.sarl`
- `05_control_flow_condition_true_false.sarl`
- `06_condiciones_precedencia.sarl`

Permiten probar:

- sintaxis mínima
- uso de variables
- estructuras de control
- evaluación de condiciones
- precedencia lógica

Son fundamentales para validar la gramática y el intérprete.

---

### Ejemplos intermedios

- `07_ejemplo_slides.sarl`
- `08.sarl`

Introducen combinaciones más complejas de:

- control de flujo
- lógica de misión
- secuencias de acciones

---

### Ejemplos de modo síncrono

- `09_modo_sincrono_v0.sarl`
- `10_modo_sincrono_v0.sarl`
- `11_modo_sincrono_sarl_webots.sarl`

Estos ejemplos están directamente alineados con el runtime actual.

Permiten validar:

- ejecución paso a paso de misiones
- comportamiento bloqueante
- integración con adapters
- compatibilidad con Webots

Son los ejemplos más importantes en la fase actual del proyecto.

---

### Ejemplos de misiones SAR conceptuales

- `Ejemplo_mision_completa.sarl`
- `Ejemplo_mision_exhaustiva.sarl`
- `Ejemplo_mision_rapida`
- `Ejemplo_revisitar_detecciones.sarl`
- `Ejemplo_revisitar_exhaustiva.sarl`

Estos scripts representan estrategias más cercanas a escenarios SAR reales:

- búsqueda rápida
- búsqueda exhaustiva
- revisita de detecciones
- combinación de estrategias

En algunos casos:

- contienen construcciones aún no soportadas completamente
- sirven como guía de evolución del lenguaje

---

## Importante

Aunque el lenguaje ya está parcialmente implementado:

- **no todos los ejemplos están garantizados como ejecutables**
- algunos siguen siendo **exploratorios o de diseño**

Esto es intencional.

La carpeta cumple una doble función:

- test técnico del sistema actual
- exploración del diseño futuro del lenguaje

---

## Cómo ejecutar un ejemplo

### En modo consola

Se puede ejecutar un script con:

    MainRunSarl

Pasando como argumento la ruta al archivo `.sarl`.

---

### En Webots

Se puede ejecutar con:

    MainRunSarlWebots

Esto permite ver el comportamiento real del dron en simulación.

---

## Relación con el resto del proyecto

Esta carpeta conecta directamente con:

- `main/` → ejecuta los ejemplos
- `grammar/` → define su sintaxis
- `interpreter/` → ejecuta el código
- `runtime/` → gestiona la misión
- `adapter/` → conecta con backend
- `controller/` → ejecuta acciones en Webots

Por tanto, los ejemplos son el punto donde todo el sistema converge.

---

## Filosofía de diseño

Los ejemplos siguen una filosofía clara:

- ser legibles y expresivos
- parecerse a un lenguaje de alto nivel tipo Python
- abstraer complejidad del control de bajo nivel
- centrarse en **qué hacer**, no en **cómo hacerlo**

Esto es clave en SARL como DSL:

> el usuario define la misión, no el control del dron.

---

## Estado actual y evolución

Actualmente la carpeta incluye:

- ejemplos básicos del lenguaje
- ejemplos funcionales en modo síncrono
- ejemplos avanzados aún en evolución

En futuras iteraciones se añadirán:

- misiones más complejas
- uso intensivo de planificación
- integración con eventos (modo reactivo)
- escenarios más realistas de SAR

---

## Recomendación

Esta carpeta debe interpretarse como:

- **banco de pruebas del lenguaje**
- **documentación viva del diseño**
- **herramienta de validación del sistema completo**

No todos los ejemplos reflejan el estado final del lenguaje,
pero todos aportan valor para su evolución.

---