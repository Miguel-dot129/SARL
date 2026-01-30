# grammar/ — Lenguaje SARL con ANTLR (V2)

Este directorio contiene la definición formal de la gramática del lenguaje **SARL (Search And Rescue Language)**  
en su **segunda versión evolutiva (SARL V2)**.

SARL es un **lenguaje específico de dominio (DSL)** diseñado para describir misiones de drones
en escenarios de **búsqueda y rescate (Search And Rescue, SAR)**.

La gramática se implementa mediante **ANTLR4** y constituye el núcleo sintáctico del lenguaje,
a partir del cual se generan el lexer y el parser utilizados por el intérprete en Java.

---

## Estado actual del lenguaje

La gramática incluida en este directorio corresponde a **SARL V2**, una versión que amplía
de forma significativa las capacidades expresivas del lenguaje respecto a SARL V1.

Esta versión:

- Parte de la base **estable y congelada de SARL V1**
- Introduce **control de flujo**, **bloques** y **condiciones booleanas completas**
- Se encuentra versionada en el repositorio bajo la rama **`sarl-v2-control-flow`**

SARL V2 es una versión **funcional y validada** mediante pruebas de parsing e interpretación,
y se utiliza como **base conceptual para la definición de misiones más realistas**.

---

## Propósito de la carpeta `grammar/`

Los objetivos principales de este directorio en SARL V2 son:

- Mantener una gramática **no ambigua, legible y fácilmente extensible**.
- Servir como **contrato sintáctico claro** entre el lenguaje y su intérprete.
- Facilitar pruebas mediante programas `.sarl` ubicados en la carpeta `examples/`.

---

## Principales novedades de SARL V2

SARL V2 amplía el lenguaje en **cuatro ejes fundamentales**:

---

### 1. Control de flujo

Se incorporan por primera vez **estructuras de control**:

#### Condicional `if / else`

.sarl:
    if (x > 0) {
    takeOff();
    } else {
    land();
    }
    Bucle while
    while (battery > 20) {
    moveForward(1);
    }

Estas estructuras permiten describir comportamientos reactivos y repetitivos,
imprescindibles para misiones reales de búsqueda y rescate.

### 2. Bloques de instrucciones
Se introduce el concepto de **bloque** mediante llaves { ... }:

- Un bloque contiene cero o más sentencias
- Se utiliza como cuerpo de if y while
- No introduce (por ahora) un nuevo ámbito de variables

Ejemplo:

    {
    moveForward(1);
    rotate(90);
    }

### 3. Condiciones booleanas completas
SARL V2 introduce un sistema completo de expresiones lógicas, con soporte para:

#### Literales booleanos
    true
    false
#### Operadores de comparación
    x > 0
    a == b
    x + 8 < y + 9
#### Operadores lógicos
- not (mayor precedencia)
- and
- or (menor precedencia)

La precedencia está explícitamente definida en la gramática:

    NOT  >  AND  >  OR

Ejemplo:

if (x > 0 or y > 0 and z > 0) {
  moveForward(1);
}

Se interpreta como:
(x > 0) OR ((y > 0) AND (z > 0))

### 4. Separación clara entre expresiones y condiciones
Una decisión clave de diseño en SARL V2 es la separación sintáctica y semántica entre:

- **Expresiones aritméticas** (expr)
- **Condiciones lógicas** (condition)

Esto implica que:

- Las operaciones aritméticas solo trabajan con números
- Las condiciones siempre producen un booleano real

Ejemplo válido:

    if (x > 0) { ... }

Ejemplo no permitido:

    if (x) { ... }   # explícitamente no soportado

Esta decisión prioriza la claridad semántica y la ausencia de ambigüedad
frente a la flexibilidad implícita.

### Ejemplo completo SARL V2
    mission ConditionsPrecedence;

    let x = 1;
    let y = 0;
    let z = 1;

    if (x > 0 and z == 1) {
    moveForward(2);
    }

    if (x > 0 or y > 0 and z > 0) {
    moveForward(4);
    }

    if ((x > 0 or y > 0) and z > 0) {
    moveForward(6);
    }

    if (not (x > 0)) {
    land();
    } else {
    takeOff();
    }

    while (false) {
    moveForward(999);
    }

### Limitaciones de SARL V2
SARL V2 tiene actualmente
algunas restricciones que pueden cambiar en V3:

#### Tipado dinámico simple
- Las variables almacenan actualmente valores numéricos (y strings, según soporte actual).
- Los valores booleanos existen únicamente como literales dentro de condiciones (`true`, `false`).
- No es posible, en SARL V2, asignar valores booleanos a variables (`let x = true` no está soportado).

La posibilidad de permitir variables booleanas se considera una extensión natural
para versiones futuras del lenguaje (SARL V3).

#### Operaciones restringidas
- Las operaciones aritméticas solo admiten operandos numéricos
- Las comparaciones solo se permiten entre valores numéricos

#### Soporte limitado de strings

SARL V2 permite la existencia de literales y variables de tipo string,
principalmente con fines de depuración y pruebas del intérprete (dry-run).

Los strings pueden:
- Asignarse a variables
- Pasarse como argumentos a comandos

No pueden:
- Participar en operaciones aritméticas
- Ser comparados
- Intervenir en condiciones lógicas

En SARL V2, los strings no tienen semántica operativa real.
Su soporte se mantiene deliberadamente limitado y queda
reservado para una definición más completa en SARL V3.

#### Sin ámbitos de variables
- No existen scopes locales
- Todas las variables son globales a la misión

Esto simplifica el modelo conceptual del lenguaje y facilita su análisis actual.

### Relación con el intérprete
La gramática SARL V2 está diseñada en **paralelo con su intérprete en Java**,
siguiendo un patrón **Visitor explícito**.

Cada nivel de la gramática (OR, AND, NOT, comparación) tiene
una contraparte directa en el intérprete, lo que garantiza:

- Correspondencia 1:1 entre sintaxis y semántica
- Evaluación predecible y trazable de condiciones
-Facilidad de depuración y extensión futura

### Justificación de SARL V2 como versión intermedia
SARL V2 no pretende cerrar el diseño del lenguaje, sino:

- Establecer una base sólida para misiones realistas
- Validar decisiones de diseño antes de integrar con Webots
- Servir como punto de partida para SARL V3

Muchas decisiones de V2 podrán ajustarse una vez el lenguaje
se utilice con misiones reales sobre el controlador del dron.

### Próxima evolución: SARL V3
La siguiente iteración del lenguaje estudiará, entre otros aspectos:

- Comandos o construcciones específicas de misión
- Estructuras más expresivas (patrones, estados, eventos)
- Integración directa con sensores y estados del dron

Todas estas decisiones se tomarán a partir de la experiencia práctica
obtenida con SARL V2, con el objetivo de que V3 sea prácticamente el lenguaje completo.

