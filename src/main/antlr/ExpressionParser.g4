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
   | Super Dot memberName = name LeftParen args = arguments RightParen # SuperCallExpression
   | receiver = expression Dot memberName = name LeftParen args = arguments RightParen # MethodCallExpression
   | memberName = name LeftParen args = arguments RightParen # StaticMethodCallExpression
   | arr = expression LeftBracket index = expression RightBracket # ArrayAccessExpression
   | type = nameWithDims Dot Class # ClassConstantExpression
   | receiver = expression Dot memberName = name # MemberAccessExpression
   | lit = Minus? DecLit # DecimalLitExpression
   | lit = Minus? IntLit # IntLitExpression
   | New innerType = name (LeftBracket dims += expression RightBracket)+ (blankDims += LeftBracket RightBracket)* # NewArrayExpression
   | op = (Minus | BitwiseNot) expr = expression # UnaryExpression
   | < assoc = right > LeftParen type = nameWithDims RightParen expr = expression # CastExpression
   | < assoc = right > New type = name LeftParen args = arguments RightParen # InstantiationExpression
   | left = expression op = (Mult | Div | Mod) right = expression # MultiplicativeExpression
   | left = expression op = (Plus | Minus) right = expression # AdditiveExpression
   | left = expression op = (Shl | Shr | Ushr) right = expression # ShiftExpression
   | left = expression op = (Lt | Le | Gt | Ge) right = expression # ComparisonExpression
   | expr = expression Instanceof type = nameWithDims # InstanceofExpression
   | left = expression op = (Eq | Ne) right = expression # EqualityExpression
   | left = expression BitwiseAnd right = expression # BitwiseAndExpression
   | left = expression BitwiseXor right = expression # BitwiseXorExpression
   | left = expression BitwiseOr right = expression # BitwiseOrExpression
   | lit = StringLit # StringLitExpression
   | lit = BoolLit # BoolLitExpression
   | lit = NullLit # NullExpression
   | Wildcard # WildcardExpression
   | This # ThisExpression
   | id = Identifier # IdentifierExpression
   ;

name
   : Identifier # IdentifierName
   | Wildcard # WildcardName
   ;

nameWithDims
   : name (dims += LeftBracket RightBracket)*
   ;

arguments
   : ((expression Comma)* expression)?
   ;

