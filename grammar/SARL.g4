grammar SARL;

@header {package es.upm.sarl.gen;}

// Un programa es una o más órdenes terminadas en ';'
program
  : (command ';')+ EOF
  ;

// Comandos admitidos
command
  : TAKEOFF
  | LAND
  | GOTO '(' INT ',' INT ')'
  ;

// LÉXICO 
TAKEOFF : 'takeoff' ;
LAND    : 'land' ;
GOTO    : 'goto' ;

INT     : [0-9]+ ;
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;
