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

//instrucciones (3 tipos de momento)
//- llamada a funcion
//- declaracion de variable
//- asignacion
stmt 
  : callStmt//llamar a algo
  | letStmt //variable
  | assignStmt //asignacion de variable
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

//Expresiones con precedencia
//- expr: suma/resta (nivel mas bajo)
//- term: mul/div
//- factor: numero, id, string, ...
expr
  : term ((PLUS | MINUS) term)*
  ;

term
  : factor ((STAR | SLASH) factor)*
  ;

factor
  : MINUS factor //para permitir negativos
  | LPAREN expr RPAREN //agrupar entre parentesis (2+3)*5
  | NUMBER //literal numero
  | STRING //literal string
  | ID //variable
  ;   


// LEXER (tokens)

MISSION : 'mission'; //palabra reservada

LET     : 'let'; //declaracion de variables

ID      : [a-zA-Z_][a-zA-Z_0-9]* ; //identificadores

NUMBER  : [0-9]+ ('.' [0-9]+)? ; //entero o decimal 

STRING  : '"' ( '\\' . | ~["\\\r\n] )* '"' ; //texto entre comillas, uno o mas elementos de cualquier caracter (\\ .) ó cualquier caracter que no sea ni ", inicio de escape (\) o salto de linea (\r y \n)

LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;
SEMI    : ';' ;

//operadores
ASSIGN  : '=' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;


WS      : [ \t\r\n]+ -> skip ; //ignorar espacios y saltos
COMMENT : '#' ~[\r\n]* -> skip ; //ignora comentarios estilo python con #
