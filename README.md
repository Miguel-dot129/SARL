# SARL
**Raíz del Proyecto**

Este repositorio está organizado para facilitar la lectura, pruebas y revisión del trabajo de definición de un lenguaje de alto nivel para misiones de drones SAR (Search and Rescue) usando ANTLR, con integración futura en Webots. A continuación se describe la estructura del proyecto.

---

## Estructura de Carpetas

```
/
|-- grammar/             # Futuros ficheros .g4 de ANTLR para definir la gramática del lenguaje
|-- examples/            # Archivos .sarl de ejemplo para imaginar misiones escritas con el lenguaje deseado
|   |-- README.md        # Explica el uso imaginativo y no definitivo de los archivos SARL
|-- controller/          # Implementación del controlador en Java para Webots, dentro de la carpeta \src\main\java\es\upm\sarl\
|   |-- Controlador.java      # Versión funcional actual del controlador para Webots
|   |-- Controlador_ejemplo_no_operativo.java       # Versión experimental anterior (descartada)
|   |-- Controlador_ejemplo_no_operativo_2.java   # Otra versión experimental fallida
|-- interpreter/         # Código Java para interpretar comandos del lenguaje definido (ANTLR), dentro de la carpeta \src\main\java\es\upm\sarl\
|   |-- Interpreter.java
|   |-- Runner.java
|   |-- Adapter.java
|   |-- ConsoleAdapter.java
|   |-- WebotsAdapter.java
|   |-- MainDryRun.java
|-- .gitignore           # Ignora archivos generados o no necesarios (builds, IDE, etc.)
|-- README.md            # Este archivo explicando toda la estructura
```

---

## Descripción General

### grammar/

Aquí se incluirán los ficheros `.g4` que definen la gramática de nuestro lenguaje ANTLR. Aún no contiene archivos definitivos, pero será el corazón del proyecto de interpretación.

### examples/

Contiene archivos `.sarl` que representan misiones imaginarias escritas con una sintaxis de alto nivel. Estas misiones **no están pensadas para ser ejecutables**, sino para ayudar a imaginar y definir cómo podría expresarse una misión SAR con un lenguaje simple y legible por humanos. 

### controller/

Implementación en Java de los controladores utilizados en Webots. `Controlador.java` es la versión actual y estable. Los otros archivos son intentos anteriores y se mantienen solo como referencia histórica. 

### interpreter/

Contiene los componentes de interpretación para conectar el lenguaje definido con su ejecución. Incluye adaptadores (“console” y “webots”), un `Runner`, el `Interpreter.java`, y `MainDryRun.java` para pruebas sin Webots.

---

## Proximamente

* Integración de gramática ANTLR
* Adaptación de comandos del lenguaje a métodos del controlador
* Interpretación completa con salida en Webots o consola

---

