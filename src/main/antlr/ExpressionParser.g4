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
   | Wildcard # WildcardExpression
   | This # ThisExpression
   | lit = Minus? IntLit # IntLitExpression
   | lit = Minus? DecLit # DecimalLitExpression
   | lit = BoolLit # BoolLitExpression
   | lit = NullLit # NullExpression
   | lit = StringLit # StringLitExpression
   | id = Identifier # IdentifierExpression
   | type = nameWithDims Dot Class # ClassConstantExpression
   | arr = expression LeftBracket index = expression RightBracket # ArrayAccessExpression
   | receiver = expression Dot memberName = name # MemberAccessExpression
   | Super Dot memberName = name LeftParen args = arguments RightParen # SuperCallExpression
   | receiver = expression Dot memberName = name LeftParen args = arguments RightParen # MethodCallExpression
   | memberName = name LeftParen args = arguments RightParen # StaticMethodCallExpression
   | op = (Minus | BitwiseNot) expr = expression # UnaryExpression
   | < assoc = right > LeftParen type = nameWithDims RightParen expr = expression # CastExpression
   | < assoc = right > New type = name LeftParen args = arguments RightParen # InstantiationExpression
   | New elementType = nameWithDims LeftBracket RightBracket LeftBrace values = nonEmptyArguments RightBrace # ArrayLitExpression
   | New innerType = name (LeftBracket dims += expression RightBracket)+ (blankDims += LeftBracket RightBracket)* # NewArrayExpression
   | left = expression op = (Mult | Div | Mod) right = expression # MultiplicativeExpression
   | left = expression op = (Plus | Minus) right = expression # AdditiveExpression
   | left = expression op = (Shl | Shr | Ushr) right = expression # ShiftExpression
   | left = expression op = (Lt | Le | Gt | Ge) right = expression # ComparisonExpression
   | expr = expression Instanceof type = nameWithDims # InstanceofExpression
   | left = expression op = (Eq | Ne) right = expression # EqualityExpression
   | left = expression BitwiseAnd right = expression # BitwiseAndExpression
   | left = expression BitwiseXor right = expression # BitwiseXorExpression
   | left = expression BitwiseOr right = expression # BitwiseOrExpression
   ;

name
   : Identifier # IdentifierName
   | Wildcard # WildcardName
   ;

nameWithDims
   : name (dims += LeftBracket RightBracket)*
   ;

arguments
   : nonEmptyArguments?
   ;

nonEmptyArguments
   : (expression Comma)* expression
   ;

