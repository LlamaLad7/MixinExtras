lexer grammar ExpressionLexer;

NewLine
   : ('\r\n' | '\n') -> skip
   ;

WS
   : [\t ]+ -> skip
   ;

StringLit
   : '\'' (~ ["\\\r\n] | '\\\'' | '\\\\')* '\''
   { setText(getText().replace("\\'", "'").replace("\\\\", "\\")); }
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

Return
   : 'return'
   ;

Throw
   : 'throw'
   ;

Identifier
   : [A-Za-z_] [A-Za-z0-9_]*
   ;

IntLit
   : '-'? [0-9]+
   | '-'? '0x' [0-9a-fA-F]+
   { setText(String.valueOf(Integer.parseInt(getText().replace("0x", ""), 16))); }
   ;

DecLit
   : '-'? [0-9]* Dot [0-9]+
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

