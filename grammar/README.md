# grammar/ — Lenguaje SARL con ANTLR (V1)

Este directorio contiene la **definición formal de la gramática del lenguaje SARL (Search And Rescue Language)**,
un lenguaje específico de dominio (DSL) diseñado para describir misiones de drones en escenarios de búsqueda y rescate (SAR).

La gramática se implementa mediante **ANTLR4** y constituye el núcleo sintáctico del lenguaje, a partir del cual se generan
el lexer y el parser utilizados por el intérprete en Java.

---

## Estado actual del lenguaje

La gramática incluida en este directorio corresponde a la **versión funcional inicial del lenguaje (SARL V1)**.

Esta versión se encuentra **congelada y versionada** en el repositorio mediante el siguiente tag de Git: **sarl-v1**

SARL V1 es una versión **estable y operativa**, utilizada para validar el flujo completo del sistema:

definición gramatical → generación del parser → recorrido del árbol sintáctico → interpretación.

---

## Propósito de la carpeta grammar/

Los objetivos principales de este directorio son:

- Contener los archivos `.g4` que definen la sintaxis y estructura del lenguaje SARL.
- Servir como base para la generación automática del lexer y parser mediante ANTLR4.
- Proporcionar una base estable sobre la que evolucionar el lenguaje de forma incremental.
- Facilitar pruebas de parsing e interpretación a partir de programas `.sarl`
  definidos en la carpeta `examples/`.

---

## Capacidades de SARL V1

La versión V1 del lenguaje permite describir misiones de forma **secuencial** mediante:

- Definición de una misión con `mission <ID>;`
- Declaración de variables mediante `let`
- Asignación de valores a variables
- Ejecución de comandos de alto nivel como llamadas a función
- Uso de expresiones aritméticas básicas
- Ejecución lineal de sentencias

### Ejemplo ilustrativo

mission Demo;
let x = 5;
x = x + 1;
takeOff();
moveForward(x);
land();

Todas las sentencias finalizan explícitamente con ; 

## Expresiones soportadas en SARL V1

SARL V1 incluye soporte para:

- Literales numéricos
- Identificadores
- Expresiones aritméticas simples (`+`, `-`, `*`, `/`)
- Uso de expresiones en declaraciones y asignaciones

No se incluyen en esta versión expresiones booleanas ni operadores de comparación.

---

## Limitaciones conocidas de SARL V1

La versión V1 presenta de forma deliberada las siguientes limitaciones:

### Ausencia de control de flujo

- No existen estructuras condicionales (`if`, `else`) ni bucles (`while`).
- La ejecución del programa es estrictamente secuencial.

### Condiciones y lógica

- No se soportan operadores de comparación (`<`, `>`, `==`, etc.).
- No existen valores booleanos (`true`, `false`).
- No se incluyen operadores lógicos (`and`, `or`, `not`).

### Bloques de código

- No existe el concepto de bloque de sentencias (`{ ... }`) ni indentación semántica.
- Todas las sentencias deben terminar en `;`.

### Expresividad acotada

- El lenguaje está diseñado para validar el núcleo del sistema  
  (gramática + intérprete) con una complejidad mínima.
- No permite todavía describir comportamientos de misión complejos o reactivos.

Estas restricciones permiten mantener una gramática y un intérprete sencillos,
facilitando su validación y evolución controlada.

---

## Relación con el intérprete

La gramática SARL V1 se interpreta mediante un **visitor implementado en Java**,
que recorre el árbol sintáctico generado por ANTLR y ejecuta las acciones asociadas
en modo *dry-run*.

Esta arquitectura desacoplada permite evolucionar el lenguaje de forma progresiva,
extendiendo tanto la gramática como el intérprete sin romper versiones anteriores.

---

## Justificación de la evolución a SARL V2

Para poder describir comportamientos de misión más realistas —como patrullas,
repetición de patrones o reacción a eventos y detecciones— es necesario introducir:

- Estructuras de control de flujo
- Evaluación de condiciones
- Bloques de ejecución

La versión **SARL V2** abordará estas carencias de forma incremental,
partiendo de la base estable definida en SARL V1.

---

## Línea de evolución prevista (SARL V2)

Los objetivos iniciales de SARL V2 son:

- Incorporar estructuras `if / else`
- Incorporar bucles `while`
- Introducir bloques de sentencias mediante `{ stmt* }`
- Añadir operadores de comparación y valores booleanos
- Evaluar condiciones en el intérprete

En una fase posterior se valorará la introducción de una sintaxis alternativa
basada en indentación (estilo Python), manteniendo siempre la claridad
y la trazabilidad del diseño.

---

## Recomendaciones de desarrollo

Cualquier modificación de la gramática debe realizarse:

- En una rama de desarrollo específica (por ejemplo, `sarl-v2-control-flow`)
- Acompañada de ejemplos `.sarl` reproducibles
- Manteniendo siempre versiones etiquetadas del lenguaje

Este enfoque garantiza la trazabilidad del proceso de diseño del lenguaje
y su correcta documentación en el contexto del TFG.
