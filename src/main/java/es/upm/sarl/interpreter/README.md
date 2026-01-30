# interpreter/ — Intérprete SARL en Java (V2)

Este directorio contiene la implementación del intérprete del lenguaje SARL
correspondiente a la versión SARL V2.

El intérprete es el componente encargado de ejecutar semánticamente un programa .sarl
a partir del AST generado por ANTLR, recorriéndolo mediante el patrón Visitor.

En esta versión, el intérprete funciona en modo dry-run:
no controla todavía un dron real, pero permite validar completamente
la semántica del lenguaje, el control de flujo y la evaluación de expresiones y condiciones.

## Rol del intérprete en la arquitectura SARL

Dentro del proyecto SARL, el intérprete cumple el papel de cerebro intermedio entre:

- La gramática (parser / lexer generados con ANTLR)
- El controlador del dron (Webots, aún no integrado)

Su responsabilidad principal es:

- Recorrer el AST del programa SARL
- Mantener el estado de ejecución (variables)
- Evaluar expresiones y condiciones
- Ejecutar instrucciones en orden
-Delegar llamadas de alto nivel a un Adapter (capa de abstracción)

## Estructura del directorio
interpreter/
├── Interpreter.java
├── MainDryRun.java
├── Runner.java
└── README.md

### Clases principales
#### Interpreter.java

Es el núcleo del intérprete SARL V2.

Características principales:

- Extiende SARLBaseVisitor<Void>
- Implementa un Visitor semántico explícito
- Mantiene un estado interno de variables:

    Map<String, Object> variables

- Ejecuta instrucciones secuencialmente
- Evalúa expresiones aritméticas y condiciones booleanas
- Imprime por consola el resultado de la ejecución (dry-run)

##### Instrucciones soportadas

- Declaración de variables (let)
- Asignación de variables
- Llamadas a funciones
- Bloques { ... }
- Condicionales if / else
- Bucles while

##### Evaluación de expresiones

El intérprete respeta estrictamente la precedencia definida en la gramática:

- expr → suma y resta
- term → multiplicación y división
- factor → literales, variables, paréntesis, negación unaria

Solo se permiten operaciones aritméticas entre valores numéricos.

##### Evaluación de condiciones (SARL V2)

El intérprete implementa un sistema completo de evaluación booleana con precedencia:

    NOT  >  AND  >  OR

Cada nivel sintáctico (condOr, condAnd, condNot, condAtom)
tiene su método correspondiente:

- evalCondOr
- evalCondAnd
- evalCondNot
- evalCondAtom

Esto garantiza:

- Correspondencia 1:1 entre gramática y semántica
- Evaluación predecible
- Facilidad de depuración y extensión

### MainDryRun.java

Clase de entrada para pruebas locales del intérprete.

Responsabilidades:

- Cargar un archivo .sarl
- Inicializar el lexer y parser generados por ANTLR
- Construir el AST
- Ejecutar el intérprete sobre dicho AST
- Mostrar por consola el resultado de la ejecución

Se utiliza para:

- Validar cambios en la gramática
- Probar nuevas construcciones del lenguaje
- Verificar la semántica del intérprete sin integrar Webots

Ejemplo de uso:

    java -cp target/classes;antlr-4.13.2-complete.jar \
    es.upm.sarl.interpreter.MainDryRun examples/06_condiciones_precedencia.sarl

### Runner.java

Clase reservada para futuras versiones del proyecto.

Su propósito previsto es:

- Actuar como punto de entrada en ejecución real
- Coordinar:

    Intérprete SARL
    Controlador del dron
    Sistema de eventos / sensores

- Sustituir progresivamente a MainDryRun

Actualmente:

No se utiliza activamente. Se mantiene como esqueleto conceptual para versiones futuras.

### Modelo de tipado en el intérprete (V2)

El intérprete utiliza un modelo de tipado dinámico en tiempo de ejecución:

- Las variables almacenan valores numéricos
- Los valores booleanos existen como resultado de condiciones
- Los strings pueden existir como literales o argumentos, pero sin semántica operativa

Restricciones actuales:
- No se permiten operaciones aritméticas con booleanos o strings
- No se permiten comparaciones entre strings
- No existen variables booleanas (let x = true no está soportado)

Estas decisiones son coherentes con el alcance de SARL V2
y se consideran candidatas a ampliación en SARL V3.

### Relación del Interpreter con la gramática

El intérprete está diseñado en paralelo con la gramática SARL V2:

- Cada regla relevante del parser tiene una traducción semántica directa
- No existe lógica “oculta” fuera del AST
- La semántica sigue estrictamente la estructura sintáctica

Esto convierte al intérprete en una herramienta ideal para:

- Experimentar con el diseño del lenguaje
- Detectar ambigüedades
- Validar decisiones antes de integrar el entorno físico (Webots)

### Estado actual Interpreter

- Control de flujo completo (if, else, while)
- Evaluación aritmética con precedencia
- Evaluación booleana completa
- Ejecución secuencial y trazable
- Modo dry-run funcional

El intérprete de SARL V2 constituye una base sólida y estable
sobre la que evolucionar hacia SARL V3 y la integración con Webots.