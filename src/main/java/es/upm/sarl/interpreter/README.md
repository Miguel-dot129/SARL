# interpreter/ — Intérprete SARL en Java (V3)

Este directorio contiene la implementación del intérprete del lenguaje **SARL**
correspondiente a la versión **SARL V3**.

El intérprete es el componente encargado de ejecutar semánticamente un programa `.sarl`
a partir del árbol sintáctico abstracto (AST) generado por ANTLR, recorriéndolo
mediante el patrón **Visitor**.

En esta versión, el intérprete ya no funciona únicamente en **modo dry-run**,
sino que actúa como puente entre:

- el programa SARL escrito por el usuario
- el runtime de misión síncrono
- la plataforma concreta de ejecución a través del adapter

Esto convierte al intérprete en el núcleo semántico del lenguaje:
entiende la sintaxis del programa, mantiene sus variables y despacha
las acciones de misión hacia el runtime.

---

## Rol del intérprete en la arquitectura SARL

Dentro del proyecto SARL, el intérprete ocupa una posición intermedia entre:

- la **gramática** (`grammar/SARL.g4`)
- el **runtime de misión** (`SyncMissionRuntime`)
- el **backend de ejecución** (por ejemplo Webots, a través de `MissionAdapter`)

Su responsabilidad principal es:

- recorrer el AST del programa SARL
- mantener la memoria de variables del programa
- evaluar expresiones aritméticas
- evaluar condiciones booleanas
- ejecutar estructuras de control
- validar semánticamente los comandos SARL
- delegar en el runtime la ejecución operativa de la misión

En otras palabras:

- la **gramática** define qué se puede escribir
- el **intérprete** define qué significa
- el **runtime** decide cómo se ejecuta dentro de una misión
- el **adapter** conecta esa misión con una plataforma concreta

---

## Estructura actual del directorio

    interpreter/
    ├── Interpreter.java
    ├── Value.java
    └── README.md

En versiones anteriores existían clases auxiliares de entrada o pruebas
como `MainDryRun` o `Runner`, pero en la arquitectura actual el núcleo
del paquete `interpreter/` queda centrado en dos piezas:

- `Interpreter.java`
- `Value.java`

La ejecución de pruebas o integración completa se realiza ahora
desde otros puntos de entrada (`main/` o `runtime/`), manteniendo
mejor separadas las responsabilidades.

---

## Clases principales

### `Interpreter.java`

Es el núcleo del intérprete SARL V3.

Características principales:

- Extiende `SARLBaseVisitor<Void>`
- Implementa un Visitor semántico explícito
- Mantiene la memoria de variables del programa:

      Map<String, Value> variables

- Ejecuta instrucciones secuencialmente
- Evalúa expresiones aritméticas y condiciones booleanas
- Interpreta llamadas a comandos SARL
- Despacha dichos comandos hacia `SyncMissionRuntime`

A diferencia de la versión anterior, el intérprete ya no llama
directamente a un adapter genérico ni se limita a imprimir por consola,
sino que se apoya en el runtime de misión.

Esto supone un cambio arquitectónico importante:

- **Interpreter** → entiende el lenguaje
- **Runtime** → entiende la misión
- **Adapter** → entiende la plataforma

---

### `Value.java`

`Value` es la representación interna unificada de los valores
del lenguaje SARL dentro del intérprete.

Su objetivo es permitir que la memoria del programa y la evaluación
de expresiones trabajen con un único tipo contenedor,
independiente de los tipos concretos de Java.

Actualmente soporta tres tipos lógicos:

- `NUMBER`
- `BOOLEAN`
- `STRING`

Esto permite:

- almacenar variables del programa de forma uniforme
- evaluar expresiones devolviendo siempre un `Value`
- validar errores de tipo de manera explícita
- desacoplar la semántica del lenguaje de los tipos nativos de Java

Ejemplos conceptuales:

    Value.ofNumber(10)
    Value.ofBoolean(true)
    Value.ofString("home")

---

## Evolución respecto a versiones anteriores

La evolución principal del intérprete en SARL V3 respecto a V2
se puede resumir en cuatro cambios importantes.

### 1. De dry-run a ejecución real sobre runtime

En versiones anteriores, las llamadas a comandos se utilizaban sobre todo
para pruebas de semántica y dry-run.

En SARL V3, el intérprete ya no se limita a imprimir llamadas,
sino que traduce comandos del lenguaje a acciones reales sobre el runtime.

Ejemplo conceptual:

    ALTITUD 10;
    DESPEGAR;
    AREA_RECT (0,0) (20,10);
    PLAN_CORTACESPED 5;

Estas instrucciones ya no son únicamente sintaxis válida:
tienen efecto real en la ejecución de la misión.

---

### 2. Soporte para argumentos estructurados

El intérprete se ha adaptado a la nueva gramática de llamadas
introducida en SARL V3.

Ahora los argumentos de un comando pueden ser:

- expresiones numéricas
- puntos 2D
- puntos 3D

Esto obliga al intérprete a distinguir semánticamente
qué tipo de argumento espera cada comando.

Para ello incorpora helpers como:

- `evalExprArg(...)`
- `evalPoint2DArg(...)`
- `evalPoint3DArg(...)`
- `extractVariableName(...)`

Esta separación hace el sistema más claro y más robusto
frente a usos incorrectos del lenguaje.

---

### 3. Introducción de `Value` como modelo unificado de tipado

En versiones anteriores, la gestión de valores era más simple
y estaba centrada sobre todo en números.

SARL V3 introduce explícitamente la clase `Value`,
que actúa como contenedor tipado dinámico.

Gracias a ello:

- las variables del programa usan una representación uniforme
- las expresiones devuelven siempre un `Value`
- la semántica de tipos queda centralizada
- el intérprete es más fácil de extender en el futuro

---

### 4. Separación clara entre programa y misión

Una decisión arquitectónica importante en SARL V3 es separar:

- **estado del programa** → gestionado por `Interpreter`
- **estado de la misión** → gestionado por `MissionState`

Esto significa que:

- variables como `i`, `puntos`, `x`, `contador` viven en el intérprete
- datos como altitud, área, plan, home o waypoint actual viven en el runtime

Esta separación evita que `Interpreter` se convierta en una clase gigante
mezclando sintaxis, semántica de lenguaje y lógica operativa de misión.

---

## Instrucciones actualmente soportadas

El intérprete soporta actualmente:

### Asignación de variables

    x = 5;
    y = x + 3;

### Llamadas a comandos SARL

Ejemplos soportados actualmente por el `switch` semántico del intérprete:

    ALTITUD 10;
    VELOCIDAD 3;
    CASA (0,0,0);
    AREA_RECT (0,0) (20,10);
    PLAN_CORTACESPED 5;
    DESPEGAR;
    IR_SIGUIENTE_PUNTO;
    ESPERAR 1;
    ATERRIZAR;
    PUNTOS_RESTANTES puntos;

### Bloques

    {
        ESPERAR 1;
        ESPERAR 2;
    }

### Condicionales

    SI puntos > 0 {
        IR_SIGUIENTE_PUNTO;
    } SINO {
        ATERRIZAR;
    }

### Bucles

    MIENTRAS puntos > 0 {
        IR_SIGUIENTE_PUNTO;
        PUNTOS_RESTANTES puntos;
    }

---

## Evaluación de expresiones

El intérprete respeta estrictamente la precedencia definida en la gramática:

- `expr` → suma y resta
- `term` → multiplicación y división
- `factor` → literales, variables, paréntesis, negación unaria

Solo se permiten operaciones aritméticas entre valores numéricos.

Ejemplo:

    x = (2 + 3) * 5;

Internamente, la evaluación se apoya en:

- `evalExpr(...)`
- `evalTerm(...)`
- `evalFactor(...)`
- `applyBinaryOp(...)`

La salida de estas funciones ya no es un tipo Java primitivo,
sino un objeto `Value`.

---

## Evaluación de condiciones

El intérprete implementa un sistema completo de evaluación booleana
con precedencia:

    NO  >  Y  >  O

Cada nivel sintáctico tiene su método correspondiente:

- `evalCondition`
- `evalCondOr`
- `evalCondAnd`
- `evalCondNot`
- `evalCondAtom`
- `evalComparison`

Esto garantiza:

- correspondencia 1:1 entre gramática y semántica
- evaluación predecible
- facilidad de depuración
- facilidad de extensión futura

Ejemplo conceptual:

    SI x > 0 O y > 0 Y z > 0 {
        ESPERAR 1;
    }

---

## Comandos con semántica especial

Aunque muchas instrucciones siguen el patrón “leer argumentos → delegar al runtime”,
algunos comandos tienen una semántica especial.

### `PUNTOS_RESTANTES`

Ejemplo:

    PUNTOS_RESTANTES puntos;

En este caso, el argumento no se evalúa como expresión,
sino que se interpreta como **nombre de variable destino**.

Flujo semántico:

1. Se valida que el argumento tiene forma de identificador
2. Se consulta al runtime cuántos puntos quedan
3. Se almacena el resultado en la memoria del programa como `Value`

Esto permite integrar en el programa SARL datos obtenidos
de la misión en ejecución.

---

## Modelo de tipado del intérprete (V3)

El intérprete utiliza tipado dinámico en tiempo de ejecución.

Actualmente soporta:

- `NUMBER`
- `BOOLEAN`
- `STRING`

Restricciones actuales:

- las operaciones aritméticas solo aceptan números
- las comparaciones actuales se aplican sobre valores numéricos
- no existe tipado estático
- no existen todavía estructuras de datos complejas

El objetivo de este diseño es mantener la implementación sencilla
y suficientemente expresiva para la fase actual del proyecto.

---

## Relación del intérprete con la gramática

El intérprete está diseñado en paralelo con la gramática SARL.

Se mantiene una relación muy directa entre ambas capas:

- cada regla relevante del parser tiene una interpretación semántica clara
- no existe lógica “oculta” por fuera del árbol sintáctico
- la ejecución sigue la estructura del AST generado por ANTLR

Esto convierte al intérprete en una herramienta muy adecuada para:

- validar la evolución del lenguaje
- detectar ambigüedades semánticas
- probar nuevas construcciones antes de ampliar la arquitectura

---

## Relación con el runtime

Uno de los cambios más importantes de SARL V3 es la introducción
de una capa de runtime de misión.

El intérprete ya no ejecuta directamente la misión sobre una plataforma,
sino que delega en `SyncMissionRuntime`.

Esto aporta varias ventajas:

- desacopla lenguaje y lógica de misión
- evita sobrecargar el intérprete con estado operativo
- prepara la arquitectura para futuras extensiones reactivas o asíncronas

En el estado actual:

- el intérprete mantiene variables
- el runtime mantiene el estado de misión
- el adapter conecta con la plataforma concreta

---

## Estado actual del intérprete

Actualmente, el intérprete SARL V3 soporta:

- variables y asignaciones
- evaluación aritmética con precedencia
- evaluación booleana completa
- bloques
- condicionales
- bucles
- llamadas a comandos con argumentos estructurados
- integración con el runtime síncrono
- trazabilidad mediante logs

Esto lo convierte en una base sólida para:

- ejecutar misiones SARL completas
- validar la semántica actual del lenguaje
- servir como punto de partida para futuras extensiones con eventos

---

## Próxima evolución

Las siguientes iteraciones del intérprete podrán estudiar, entre otros aspectos:

- registro de manejadores de eventos
- integración con un motor de misión reactivo
- nuevos tipos de datos
- nuevos comandos de misión
- selección más flexible de servicios de planificación

SARL V3 ya proporciona una base suficientemente madura
como para separar el trabajo en dos líneas:

- consolidación del lenguaje y la arquitectura actual
- evolución futura hacia ejecución reactiva y eventos