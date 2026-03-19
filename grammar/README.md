# grammar/ — Lenguaje SARL con ANTLR (V3)

Este directorio contiene la definición formal de la gramática del lenguaje **SARL (Search And Rescue Language)**  
en su **tercera versión evolutiva (SARL V3)**.

SARL es un **lenguaje específico de dominio (DSL)** diseñado para describir misiones de drones
en escenarios de **búsqueda y rescate (Search And Rescue, SAR)**.

La gramática se implementa mediante **ANTLR4** y constituye el núcleo sintáctico del lenguaje,
a partir del cual se generan el lexer y el parser utilizados por el intérprete en Java.

---

## Estado actual del lenguaje

La gramática incluida en este directorio corresponde a **SARL V3**, una versión que mantiene
la base de **SARL V2** y la amplía hacia una sintaxis más natural para la definición de misiones.

Esta versión:

- Conserva la base sintáctica establecida en **SARL V2**
- Mantiene el soporte de:
  - variables
  - expresiones aritméticas
  - estructuras de control (`SI`, `SINO`, `MIENTRAS`)
  - condiciones booleanas completas
- Introduce una nueva forma de expresar **llamadas a comandos con argumentos estructurados**
- Añade soporte nativo en la gramática para **puntos 2D y 3D**
- Sirve como base del modo de ejecución síncrono integrado ya con el runtime y Webots

SARL V3 ya no es solo una gramática experimental, sino una base funcional del lenguaje
capaz de describir misiones completas y ejecutarlas mediante el intérprete y el runtime.

---

## Propósito de la carpeta `grammar/`

Los objetivos principales de este directorio en SARL V3 son:

- Mantener una gramática **no ambigua, legible y extensible**
- Servir como **contrato sintáctico claro** entre el lenguaje y el intérprete
- Permitir expresar misiones de forma cada vez más natural
- Facilitar pruebas mediante programas `.sarl` ubicados en la carpeta `examples/`

---

## Principales características de SARL V3

SARL V3 conserva la base expresiva de V2 y añade una sintaxis más adecuada
para describir comandos de misión.

---

### 1. Estructura general de misión

Un fichero SARL representa una misión completa y sigue la forma general:


MISION Demo;

ALTITUD 10;
DESPEGAR;
ATERRIZAR;


Toda misión:

- comienza con la declaración `MISION nombre;`
- contiene una secuencia de sentencias
- finaliza al llegar al final del fichero

---

### 2. Variables, expresiones y asignación

SARL permite definir y reutilizar variables dentro del programa:


x = 5;
y = x + 3;


Actualmente se soportan:

- números
- strings
- uso de variables en expresiones

Las expresiones aritméticas mantienen precedencia clásica:

- `*` y `/` tienen más precedencia que `+` y `-`
- se pueden usar paréntesis para agrupar

Ejemplo:


x = (2 + 3) * 5;


---

### 3. Control de flujo

SARL V3 mantiene las estructuras de control introducidas en V2:

#### Condicional `SI / SINO`


SI x > 0 {
DESPEGAR;
} SINO {
ATERRIZAR;
}


#### Bucle `MIENTRAS`


MIENTRAS puntos > 0 {
IR_SIGUIENTE_PUNTO;
ESPERAR 1;
}


Estas estructuras permiten expresar lógica de misión no trivial
y son una de las bases que hacen que SARL deje de ser una simple secuencia lineal de comandos.

---

### 4. Condiciones booleanas completas

SARL soporta condiciones lógicas con:

#### Literales booleanos


VERDADERO
FALSO


#### Comparaciones


x > 0
a == b
x + 8 < y + 9


#### Operadores lógicos

- `NO`
- `Y`
- `O`

La precedencia se mantiene explícita:


NO > Y > O


Ejemplo:


SI x > 0 O y > 0 Y z > 0 {
ESPERAR 1;
}


Se interpreta como:


(x > 0) O ((y > 0) Y (z > 0))


---

### 5. Nueva sintaxis de llamadas a comandos

Una de las principales novedades de SARL V3 es la evolución de la sintaxis
de llamada a comandos.

En versiones anteriores, las llamadas se apoyaban en una lista de expresiones
separadas por comas (`argList`).

En SARL V3 se introduce una sintaxis más flexible y natural basada en:

- `callStmt`
- `callArgs`
- `callArg`

Esto permite escribir comandos de misión de forma más cercana a un lenguaje operativo:


ALTITUD 10;
ESPERAR 2;
CASA (0,0,0);
AREA_RECT (0,0) (20,10);


Ventajas de este cambio:

- hace el lenguaje más legible
- acerca la sintaxis a un DSL de misión real
- permite mezclar expresiones y estructuras geométricas
- prepara mejor la integración con comandos de misión de más alto nivel

---

### 6. Soporte nativo para puntos 2D y 3D

SARL V3 añade en la gramática dos nuevas construcciones:

- `point2D`
- `point3D`

#### Punto 2D


(0,0)
(x,y)
(2+3, distancia)


#### Punto 3D


(0,0,10)
(homeX, homeY, altitud)
(x+1, y-2, 5)


Esto permite modelar de forma natural:

- posiciones de misión
- áreas de trabajo
- coordenadas espaciales completas

---

### 7. Separación clara entre expresiones, geometría y condiciones

Una decisión importante de diseño en SARL V3 es mantener separadas tres cosas:

- **expresiones aritméticas**
- **condiciones booleanas**
- **argumentos geométricos**

Esto mejora la claridad del lenguaje y facilita su interpretación.

---

## Ejemplo completo SARL V3


MISION BarridoSimple;

CASA (0,0,0);
ALTITUD 10;
VELOCIDAD 3;

AREA_RECT (0,0) (20,10);
PLAN_CORTACESPED 5;

DESPEGAR;

PUNTOS_RESTANTES puntos;

MIENTRAS puntos > 0 {
IR_SIGUIENTE_PUNTO;
ESPERAR 1;
PUNTOS_RESTANTES puntos;
}

ATERRIZAR;


---

## Limitaciones actuales de SARL V3

- Tipado dinámico básico
- Sin funciones definidas por el usuario
- Sin scopes locales
- Catálogo de comandos limitado

---

## Relación con el intérprete

La gramática define la sintaxis, pero:

- el **Interpreter** define la semántica
- el **Runtime** ejecuta la misión
- el **Adapter** conecta con la plataforma

---

## Próxima evolución

- eventos
- ejecución reactiva
- selección de algoritmos de planificación
- mayor desacoplamiento entre lenguaje y lógica de misión