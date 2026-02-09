grammar SARL;//nombre gramatica

@header {
package es.upm.sarl.gen;
}

// PARSER (estructura)

// PROGRAMA:
// Un fichero SARL completo representa una misión
// Debe comenzar con la declaración de misión y finalizar en EOF
program
: mission EOF //un fichero SARL es una mision y se acaba (EOF)
;

// MISIÓN:
// Define el nombre de la misión y contiene una secuencia de sentencias
mission
  : MISSION ID SEMI stmt*   // "mission Demo;" seguido de 0..N instrucciones
  ;

// INSTRUCCIONES:
// Representa cualquier sentencia ejecutable del lenguaje SARL
// En V2 se amplía para incluir estructuras de control (if / while)
stmt 
  : callStmt//llamar a algo
  | assignStmt //asignacion de variable
  | ifStmt //condicion (if)
  | whileStmt //bucle (while)
  ;

//LLAMADA A FUNCION (comando)
// por ejemplo setHome 0, 0, 0;
callStmt
  : ID SEMI
  | ID argList SEMI
  ;


// ASIGNACION:
// Modifica el valor de una variable previamente declarada.
// Sintaxis: nombre = expresión;
assignStmt
  : ID ASSIGN expr SEMI
  ;

// LISTA DE ARGUMENTOS:
// Una o más expresiones separadas por comas
argList
  : expr (COMMA expr)* //una expresion seguido de 0...N expresiones más separados por coma
  ;

// EXPRESIONES ARITMÉTICAS CON PRECEDENCIA:
// La gramática separa los distintos niveles de precedencia
// para evitar ambigüedades en la evaluación
// - expr   : suma y resta (menor precedencia)
// - term   : multiplicación y división
// - factor : elementos básicos (literales, variables, paréntesis)
expr
  : term ((PLUS | MINUS) term)*
  ;

// TÉRMINO ARITMÉTICO:
// Representa operaciones de multiplicación y división
// Tiene mayor precedencia que expr, por lo que se evalúa antes
// en expresiones compuestas
term
  : factor ((STAR | SLASH) factor)*
  ;

// FACTOR:
// Unidad básica de una expresión
// Puede ser un literal, una variable, una expresión agrupada
// entre paréntesis o una expresión negada
factor
  : MINUS factor //para permitir negativos
  | LPAREN expr RPAREN //agrupar entre parentesis (2+3)*5
  | NUMBER //literal numero
  | STRING //literal string
  | ID //variable
  ;   

// BLOQUE DE INSTRUCCIONES:
// Define un bloque delimitado por llaves { } que contiene
// cero o más sentencias SARL
// Se utiliza como cuerpo de estructuras de control (if / while)
block 
  : LBRACE stmt* RBRACE
  ;

// ESTRUCTURA CONDICIONAL IF / ELSE:
// Evalúa una condición booleana y ejecuta el bloque correspondiente.
ifStmt
  : IF condition block (ELSE block)? 
  ;

// ESTRUCTURA DE BUCLE WHILE:
// Evalúa repetidamente una condición booleana
// Mientras la condición sea verdadera, ejecuta el bloque
whileStmt
  : WHILE condition block
  ;

// CONDICIÓN:
// Entrada principal para expresiones booleanas
// Implementa precedencia lógica completa:
// NOT > AND > OR
condition
  : condOr
  ;

// OR
// Operador lógico de menor precedencia.
condOr
  : condAnd (OR condAnd)*
  ;

// AND 
// Operador lógico de precedencia intermedia.
condAnd
  : condNot (AND condNot)*
  ;

// NOT 
// Operador lógico de mayor precedencia.
condNot
  : NOT condNot
  | condAtom
  ;

// ÁTOMOS DE CONDICIÓN:
// Unidades básicas de una condición lógica
condAtom
  : LPAREN condition RPAREN // subcondición agrupada entre parentesis
  | TRUE // literal booleano verdadero
  | FALSE // literal booleano falso
  | comparison // comparación relacional
  ;

// COMPARACIÓN:
// Comparación explícita entre dos expresiones aritméticas
// Siempre produce un valor booleano
comparison
  : expr compOp expr   // permite: x>0, x==y, x+8<y+9
  ;

// OPERADORES DE COMPARACIÓN:
// Permiten evaluar relaciones entre dos expresiones numéricas
// El resultado es siempre booleano
compOp
  : LT | GT | LE | GE | EQ | NE
  ;

// LEXER (tokens)

//palabras reservadas:
MISSION : 'MISION'; //palabra reservada para nombre mision
IF      : 'SI'; //condicion if
ELSE    : 'SINO'; //condicion si no se cumple if
WHILE   : 'MIENTRAS'; //bucle
TRUE    : 'VERDADERO'; //literal booleano verdadero 
FALSE   : 'FALSO'; //literal booleano falso 
AND   : 'Y';  
OR    : 'O'; 
NOT   : 'NO';

ID      : [a-zA-Z_][a-zA-Z_0-9]* ; //identificadores

NUMBER  : [0-9]+ ('.' [0-9]+)? ; //entero o decimal 

STRING  : '"' ( '\\' . | ~["\\\r\n] )* '"' ; //texto entre comillas, uno o mas elementos de cualquier caracter (\\ .) ó cualquier caracter que no sea ni ", inicio de escape (\) o salto de linea (\r y \n)

LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;
SEMI    : ';' ;
LBRACE  : '{' ;
RBRACE  : '}' ;

//operadores
ASSIGN  : '=' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;

//comparadores
LE : '<=' ; //less than or equal
LT : '<' ; //less than
GE : '>=' ; //greater than or equal
GT : '>' ; // Greater than
EQ : '==' ; //equal
NE : '!=' ; //not equal

WS      : [ \t\r\n]+ -> skip ; //ignorar espacios y saltos
COMMENT : '#' ~[\r\n]* -> skip ; //ignora comentarios estilo python con #
