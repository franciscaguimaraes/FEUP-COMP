grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;
COMMENT : ('//' ~[\r\n]*) -> skip;
MULTI_COMMENT: ('/*' .*? '*/') -> skip;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' names+=ID ( '.' names+=ID )* ';' #Import
    ;

classDeclaration
    : 'class' names+=ID ( 'extends' names+=ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}' #Class
    ;

varDeclaration
    : type value=ID ';'
    ;

methodDeclaration locals[boolean isStatic=false]
    : ('public')? ('static' {$isStatic=true;})? type name=ID '(' ( parameter ( ',' parameter )* )? ')'
        '{' ( varDeclaration | statement )* 'return' expression ';' '}' #Method
    | ('public')? 'static' 'void' 'main' '(' parameter ')' '{' ( varDeclaration)* ( statement )* '}' {$isStatic=true;} #MainMethod
    ;

parameter
    : type value=ID
    ;

type locals[boolean isArray=false]
    : value='int' ('[' ']' {$isArray=true;})? #TypeInt
    | value='String' ('[' ']' {$isArray=true;})? #TypeStr
    | value='boolean' #TypeBool
    | value=ID #TypeId
    ;

statement
    : '{' ( statement )* '}' #Stmt
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'if' '(' expression ')' statement #IfStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt
    | name=ID '=' expression ';' #AssignmentStmt
    | name=ID '[' expression ']' '=' expression ';' #ArrayAssignmentStmt
    ;


expression
    : '(' expression ')' #Brackets
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #Length
    | expression '.' name=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | '!' expression #Denial
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BoolOp
    | expression op='&&' expression #BoolOp
    | 'new' 'int' '[' expression ']' #IntDeclaration
    | 'new' name=ID '(' ')' #NewObject
    | value=INTEGER #Integer
    | value='true' #Boolean
    | value='false' #Boolean
    | value=ID #Identifier
    | value='this' #This
    ;
