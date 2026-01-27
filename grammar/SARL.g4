grammar SARL;//nombre gramatica

@header {
package es.upm.sarl.gen;
}

// PARSER (estructura)

program
: mission EOF //un fichero SARL es una mision y se acaba (EOF)
;

mission
  : MISSION ID SEMI stmt*   // "mission Demo;" seguido de 0..N instrucciones
  ;

// INSTRUCCIONES:
// Representa cualquier sentencia ejecutable del lenguaje SARL
// En V2 se amplía para incluir estructuras de control (if / while)
stmt 
  : callStmt//llamar a algo
  | letStmt //variable
  | assignStmt //asignacion de variable
  | ifStmt //condicion (if)
  | whileStmt //bucle (while)
  ;

//Llamada a funcion
callStmt //por ejemplo setHome(0,0,0)
  : ID LPAREN argList? RPAREN SEMI //asrList? es una lista de argumentos opcional
  ;

//Declaracion de variable: 
// Permite introducir una nueva variable en el programa SARL
// Sintaxis: let nombre = expresión;
letStmt
  : LET ID ASSIGN expr SEMI
  ; 

// ASIGNACION:
// Modifica el valor de una variable previamente declarada.
// Sintaxis: nombre = expresión;
assignStmt
  : ID ASSIGN expr SEMI
  ;

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
  | TRUE
  | FALSE
  | ID //variable
  ;   

// BLOQUE DE INSTRUCCIONES:
// Define un bloque delimitado por llaves { } que contiene
// cero o más sentencias SARL.
// Se utiliza como cuerpo de estructuras de control (if / while)
block 
  : LBRACE stmt* RBRACE
  ;

// ESTRUCTURA CONDICIONAL IF / ELSE:
// Evalúa una expresión entre paréntesis y ejecuta el bloque
// asociado si la condición es verdadera
// Opcionalmente permite un bloque ELSE si la condición no se cumple.
// De momento la condición es una expresión numérica (no booleana)
ifStmt
  : IF LPAREN condition RPAREN block (ELSE block)? 
  ;

// ESTRUCTURA DE BUCLE WHILE:
// Evalúa repetidamente una expresión entre paréntesis
// Mientras la condición sea verdadera, se ejecuta el bloque asociado
// En esta versión la condición es una expresión numérica
whileStmt
  : WHILE LPAREN condition RPAREN block
  ;

condition
  : expr (compOp expr)?   // permite: (x) o (x > 0) o (x == y)
  ;

compOp
  : LT | GT | LE | GE | EQ | NE
  ;

// LEXER (tokens)

//palabras reservadas:
MISSION : 'mission'; //palabra reservada para nombre mision
LET     : 'let'; //declaracion de variables
IF      : 'if'; //condicion if
ELSE    : 'else'; //condicion si no se cumple if
WHILE   : 'while'; //bucle
TRUE  : 'true'; 
FALSE : 'false';

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
