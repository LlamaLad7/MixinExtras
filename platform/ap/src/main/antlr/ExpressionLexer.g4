lexer grammar ExpressionLexer;

NewLine
   : '\r\n'
   | '\n'
   ;

WS
   : [\t ]+ -> skip
   ;

StringLit
   : '\'' (~ ["\\\r\n] | '\\\'')* '\''
   { setText(getText().replace("\\'", "'")); }
   ;

Wildcard
   : '?'
   ;

New
   : 'new'
   ;

Instanceof
   : 'instanceof'
   ;

BoolLit
   : 'true'
   | 'false'
   ;

NullLit
   : 'null'
   ;

Identifier
   : [A-Za-z_] [A-Za-z0-9_]*
   ;

IntLit
   : [0-9]+
   | '0x' [0-9a-fA-F]+
   { setText(String.valueOf(Integer.parseInt(getText().substring(2), 16))); }
   ;

DecLit
   : [0-9]* Dot [0-9]+
   ;

Plus
   : '+'
   ;

Minus
   : '-'
   ;

Mult
   : '*'
   ;

Div
   : '/'
   ;

Mod
   : '%'
   ;

BitwiseNot
   : '~'
   ;

Dot
   : '.'
   ;

Comma
   : ','
   ;

LeftParen
   : '('
   ;

RightParen
   : ')'
   ;

LeftBracket
   : '['
   ;

RightBracket
   : ']'
   ;

At
   : '@'
   ;

Shl
   : '<<'
   ;

Shr
   : '>>'
   ;

Ushr
   : '>>>'
   ;

Lt
   : '<'
   ;

Le
   : '<='
   ;

Gt
   : '>'
   ;

Ge
   : '>='
   ;

Eq
   : '=='
   ;

Ne
   : '!='
   ;

BitwiseAnd
   : '&'
   ;

BitwiseXor
   : '^'
   ;

BitwiseOr
   : '|'
   ;

Assign
   : '='
   ;

