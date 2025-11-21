**Carpeta `examples/` – Ejemplos de Misiones SAR en Lenguaje Imaginario `.sarl`**

Este directorio contiene una colección de archivos `.sarl` que representan ejemplos conceptuales de misiones SAR (Search and Rescue) definidas con un lenguaje de alto nivel inventado.

---

## Importante

* **El lenguaje usado no está implementado aún.**
* Su estilo está inspirado en Python, pero **no sigue ninguna gramática real**.
* El propósito es **explorar y diseñar conceptualmente** cómo debería comportarse nuestro lenguaje futuro basado en ANTLR, orientado a la programación de misiones para drones en escenarios SAR.

---

## Objetivos de estos ejemplos

* Ilustrar **distintos modos de operación**: búsqueda rápida, exhaustiva, híbrida, revisita de detecciones, etc.
* Visualizar **estructuras comunes en misiones**: definición de áreas, control de flujo con bucles y condiciones, gestión de detecciones, etc.
* Facilitar el diseño posterior del parser y la semántica del lenguaje SARL que se implementará con ANTLR y se conectará a un controlador Java para drones en Webots.

---

## Sobre el contenido

* Los comandos como `gotoDetection()`, `followStep()`, `recordDetection()` son **propuestas imaginarias**, que podrán o no existir en la versión final.
* El diseño se inspira en **estrategias de misiones reales** extraídas de literatura sobre UAVs en SAR, pero los ejemplos **no siguen un estándar formal**.
* Cada `.sarl` representa un **modo de misión diferente**, útil para validar diferentes posibilidades del lenguaje antes de implementar nada en código.

---

## Recomendación

Este material **no debe interpretarse como código ejecutable**. Es **documentación funcional y de diseño** que acompaña el desarrollo del TFG para justificar y anticipar futuras decisiones de implementación.
