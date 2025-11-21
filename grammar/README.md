**grammar/ - Lenguaje SARL con ANTLR (en desarrollo)**

Este directorio está destinado a contener la definición formal de la gramática del lenguaje SARL (Search and Rescue Language), un lenguaje de alto nivel diseñado para programar misiones de drones en escenarios de búsqueda y rescate (SAR).

---

## Estado Actual

Actualmente esta carpeta contiene un ejemplo básico de gramática estilo *Hello World* (no funcional ni vinculada al diseño real del lenguaje). Este archivo sirve solo como demostración inicial de integración con ANTLR, y será reemplazado por la gramática real conforme se avance en el desarrollo.

---

## Propósito de la Carpeta

* Contener los archivos `.g4` (gramáticas ANTLR) que definirán la sintaxis y estructura del lenguaje SARL.
* Servir como base para generar el parser y lexer automáticamente mediante ANTLR4.
* Permitir pruebas de parsing independientes mediante los ficheros de ejemplo `.sarl` definidos en la carpeta `examples/`.

---

## Objetivo a Futuro

Diseñar una gramática que permita:

* Definir misiones mediante comandos de alto nivel para drones SAR.
* Incorporar estructuras de control de flujo (bucles, condicionales).
* Incluir operaciones comunes como `goto()`, `recordDetection()`, `setAltitude()`, etc.
* Ser legible para operadores humanos, en principio con una sintaxis similar a Python.

Este lenguaje será posteriormente enlazado con el controlador Java mediante un intérprete, que ejecutará las acciones en Webots.

---

## Recomendaciones

Mientras el lenguaje no esté implementado, **no modificar esta carpeta** sin seguir una estrategia definida de diseño. El desarrollo de la gramática debe basarse en los ejemplos `.sarl` conceptuales y en las necesidades funcionales extraídas de nuestro proyecto.
