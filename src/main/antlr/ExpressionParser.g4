parser grammar ExpressionParser;


options { tokenVocab = ExpressionLexer; }
root
   : statement EOF
   ;

statement
   : receiver = expression Dot memberName = name Assign value = expression # MemberAssignmentStatement
   | arr = expression LeftBracket index = expression RightBracket Assign value = expression # ArrayStoreStatement
   | identifier = name Assign value = expression # IdentifierAssignmentStatement
   | Return value = expression # ReturnStatement
   | Throw value = expression # ThrowStatement
   | expression # ExpressionStatement
   ;

expression
   : At LeftParen expr = expression RightParen # CapturingExpression
   | LeftParen expr = expression RightParen # ParenthesizedExpression
   | receiver = expression Dot memberName = name LeftParen args = arguments RightParen # MethodCallExpression
   | memberName = name LeftParen args = arguments RightParen # StaticMethodCallExpression
   | arr = expression LeftBracket index = expression RightBracket # ArrayAccessExpression
   | receiver = expression Dot memberName = name # MemberAccessExpression
   | op = (Minus | BitwiseNot) expr = expression # UnaryExpression
   | < assoc = right > LeftParen type = name RightParen expr = expression # CastExpression
   | < assoc = right > New type = name LeftParen args = arguments RightParen # InstantiationExpression
   | left = expression op = (Mult | Div | Mod) right = expression # MultiplicativeExpression
   | left = expression op = (Plus | Minus) right = expression # AdditiveExpression
   | left = expression op = (Shl | Shr | Ushr) right = expression # ShiftExpression
   | left = expression op = (Lt | Le | Gt | Ge) right = expression # ComparisonExpression
   | expr = expression Instanceof type = name # InstanceofExpression
   | left = expression op = (Eq | Ne) right = expression # EqualityExpression
   | left = expression BitwiseAnd right = expression # BitwiseAndExpression
   | left = expression BitwiseXor right = expression # BitwiseXorExpression
   | left = expression BitwiseOr right = expression # BitwiseOrExpression
   | lit = DecLit # DecimalLitExpression
   | lit = IntLit # IntLitExpression
   | lit = StringLit # StringLitExpression
   | lit = BoolLit # BoolLitExpression
   | lit = NullLit # NullExpression
   | Wildcard # WildcardExpression
   | id = Identifier # IdentifierExpression
   ;

name
   : Identifier # IdentifierName
   | Wildcard # WildcardName
   ;

arguments
   : ((expression Comma)* expression)?
   ;

