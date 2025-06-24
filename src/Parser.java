import AST.*;

import java.util.*;

public class Parser {
    private final TokenManager tokenManager;
    private final TranNode topNode;

    public Parser(TranNode top, List<Token> tokens) {
        this.topNode = top;
        this.tokenManager = new TokenManager(tokens);
    }

    //helper method, looks for newline & throws error if not found
    void requireNewLine() throws SyntaxErrorException {
        System.out.println("checking newline, current token: " + tokenManager.peek(0));
        if (tokenManager.done()) {
            throw new SyntaxErrorException("EOF", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()) {
            throw new SyntaxErrorException("Newline expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        System.out.println("Newline matched and removed");
    }

    //Tran = {Interface} | [Class]
    public void Tran() throws SyntaxErrorException {
        while (!tokenManager.done()) {
            Optional<InterfaceNode> interfaceNode = parseInterface();
            if (interfaceNode.isPresent()) {
                topNode.Interfaces.add(interfaceNode.get());
                continue;
            }
            Optional<ClassNode> classNode = parseClass();
            if (classNode.isPresent()) {
                topNode.Classes.add(classNode.get());
                continue;
            }
            if (!tokenManager.done()) {
                throw new SyntaxErrorException("Unexpected token after class or interface", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }

    }

    //Interface = "interface" Identifier NEWLINE INDENT {MethodHeader NEWLINE } DEDENT
    private Optional<InterfaceNode> parseInterface() throws SyntaxErrorException {
        // looks for the interface keyword
        if (tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()) {
            return Optional.empty();
        }
        // looks for interface name & throws error if not found
        Optional<Token> interfaceNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (interfaceNameToken.isEmpty()) {
            throw new SyntaxErrorException("No interface name provided.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // newline after the interface declaration
        requireNewLine();

        // looks for indent and throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent expected after interface declaration.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // creates a new InterfaceNode, assigns name to interface
        String interfaceName = interfaceNameToken.get().getValue();
        InterfaceNode interfaceNode = new InterfaceNode();
        interfaceNode.name = interfaceName;

        while (!tokenManager.done()) {
            Optional<Token> nextToken = tokenManager.peek(0);
            //if dedent is found there are no method headers
            if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.DEDENT) {
                tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                break; // End of interface block
            }

            //parses method headers and adds them to interface
            Optional<MethodHeaderNode> methodHeaderNode = parseMethodHeader();
            methodHeaderNode.ifPresent(interfaceNode.methods::add);

            // Require a newline after each method header
            requireNewLine();
        }
        return Optional.of(interfaceNode);
    }

    //MethodHeader = Identifier "(" VariableDeclarations ")" [ ":" VariableDeclaration { "," VariableDeclaration }]
    private Optional<MethodHeaderNode> parseMethodHeader() throws SyntaxErrorException {
        // looks for method name, throws error if not found
        Optional<Token> methodNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (methodNameToken.isEmpty()) {
            throw new SyntaxErrorException("Method name expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // looks for a '(' , throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected '(' after method name.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //parses variable declaration (parameters)
        List<VariableDeclarationNode> parameters = parseVariableDeclarations();

        // looks for a ')' , throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected ')' after method parameters.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // creates method header node and sets name & parameters
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        methodHeaderNode.name = methodNameToken.get().getValue();
        methodHeaderNode.parameters = parameters;

        // looks for : and parses remaining variable declarations to the right of it (returns), throws error if returns not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
            List<VariableDeclarationNode> returnTypes = parseVariableDeclarations();
            if (returnTypes.isEmpty()) {
                throw new SyntaxErrorException("Expected return type(s) after colon.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            methodHeaderNode.returns = returnTypes;
        }
        return Optional.of(methodHeaderNode);
    }

    //Class = "class" Identifier [ "implements" Identifier { "," Identifier } ] NEWLINE INDENT {Constructor NEWLINE | MethodDeclaration NEWLINE | Member NEWLINE } DEDENT
    private Optional<ClassNode> parseClass() throws SyntaxErrorException {
        //looks for class keyword, throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isEmpty()) {
            return Optional.empty();
        }
        // looks for class name, throws error if not found
        Optional<Token> classNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (classNameToken.isEmpty()) {
            throw new SyntaxErrorException("No class name provided.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        // creates a new ClassNode and assigns name to word found
        ClassNode classNode = new ClassNode();
        classNode.name = classNameToken.get().getValue();

        //continue if class implements interface
        if (tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
            //looks for interface name, throws error if not found
            Optional<Token> interfaceToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (interfaceToken.isEmpty()) {
                throw new SyntaxErrorException("Expected interface.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            //adds interface to class
            classNode.interfaces.add(interfaceToken.get().getValue());

            //continue if comma is seen
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                //looks for another interface name, throws error if not found
                Optional<Token> nextInterfaceToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                if (nextInterfaceToken.isEmpty()) {
                    throw new SyntaxErrorException("Expected interface.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                //adds interface to class
                classNode.interfaces.add(nextInterfaceToken.get().getValue());
            }
        }
        //newline required after class and interfaces
        requireNewLine();

        //looks for indent, throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        while (!tokenManager.done()) {
            Optional<Token> nextToken = tokenManager.peek(0);
            //if dedent is found there is no constructor | method | member
            if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.DEDENT) {
                System.out.println("END OF CLASS");
                tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                break;
            }

            //looks for constructor to parse
            Optional<ConstructorNode> constructor = parseConstructor();
            if (constructor.isPresent()) {
                classNode.constructors.add(constructor.get());
                continue;
            }
            //peeks at the next token to determine if it's a method or member
            Optional<Token> mdToken = tokenManager.peek(0);
            if (mdToken.isPresent() && mdToken.get().getType() == Token.TokenTypes.SHARED || mdToken.isPresent() && mdToken.get().getType() == Token.TokenTypes.PRIVATE) {
                Optional<MethodDeclarationNode> method = parseMethodDeclaration();
                if (method.isPresent()) {
                    classNode.methods.add(method.get());
                    continue;
                }
            }
            Optional<Token> firstToken = tokenManager.peek(0);
            if (firstToken.isPresent() && firstToken.get().getType() == Token.TokenTypes.WORD) {
                Optional<Token> nextAfterWord = tokenManager.peek(1);
                if (nextAfterWord.isPresent() && nextAfterWord.get().getType() == Token.TokenTypes.LPAREN) {
                    // it's a method declaration (because '(' follows the identifier)
                    Optional<MethodDeclarationNode> method = parseMethodDeclaration();
                    if (method.isPresent()) {
                        classNode.methods.add(method.get());
                        continue;
                        //requireNewLine();
                    }
                } else {
                    Optional<MemberNode> member = parseMember();
                    if (member.isPresent()) {
                        classNode.members.add(member.get());
                        //requireNewLine();
                        continue;
                    }
                }
            } else {
                throw new SyntaxErrorException("unexpected token in class", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        return Optional.of(classNode);
    }

    //Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    private Optional<ConstructorNode> parseConstructor() throws SyntaxErrorException {
        //looks for construct, throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty()) {
            return Optional.empty();
        }

        System.out.println("Parsing constructor");

        //looks for '(', throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
            throw new SyntaxErrorException("Left Parenthesis expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //parses variable declarations (parameters)
        List<VariableDeclarationNode> parameters = parseVariableDeclarations();

        //looks for ')' , throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Right Parenthesis expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //newline required after construct and variable parameter declarations
        requireNewLine();

        ConstructorNode constructorNode = new ConstructorNode();
        constructorNode.parameters = parameters;

        //looks for indent to parse method body
        Optional<Token> nextToken = tokenManager.peek(0);
        if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.INDENT) {
            parseMethodBody(constructorNode.locals, constructorNode.statements);
        }
        System.out.println(constructorNode);
        return Optional.of(constructorNode);
    }

    //VariableDeclaration = Identifier Identifier
    private Optional<VariableDeclarationNode> parseVariableDeclaration() throws SyntaxErrorException {
        //looks for word (type)
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) {
            return Optional.empty();
        }

        //looks for word (name) , throws error if not found
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) {
            throw new SyntaxErrorException("Variable name expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //assigns type & word
        VariableDeclarationNode variableDeclaration = new VariableDeclarationNode();
        variableDeclaration.type = typeToken.get().getValue();
        variableDeclaration.name = nameToken.get().getValue();

        return Optional.of(variableDeclaration);
    }

    //VariableDeclarations = [ VariableDeclaration ] | VariableDeclaration { "," VariableDeclaration }
    private List<VariableDeclarationNode> parseVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> variableDeclarations = new ArrayList<>();
        Optional<VariableDeclarationNode> firstDeclaration = parseVariableDeclaration();
        firstDeclaration.ifPresent(variableDeclarations::add);
        //checks for repeating variable declarations
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            Optional<VariableDeclarationNode> addedDeclaration = parseVariableDeclaration();
            if (addedDeclaration.isEmpty()) {
                throw new SyntaxErrorException("added variable declaration expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            //adds any added variable declarations if comma is present before
            variableDeclarations.add(addedDeclaration.get());
        }
        return variableDeclarations;
    }

    //Member = VariableDeclaration NEWLINE [INDENT["accessor:" Statements] ["mutator:" Statements]]
    private Optional<MemberNode> parseMember() throws SyntaxErrorException {

        //looks for variable declaration
        Optional<VariableDeclarationNode> variableDeclaration = parseVariableDeclaration();
        if (variableDeclaration.isEmpty()) {
            return Optional.empty();
        }
        //assigns variable declaration to member declaration
        MemberNode memberNode = new MemberNode();
        memberNode.declaration = variableDeclaration.get();
        System.out.println("Variable Declaration:" + memberNode.declaration.name);

        requireNewLine();

        //checks for accessor & ':' and saves its statements
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty()) {
                    throw new SyntaxErrorException("colon after accessor expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                //List<StatementNode> accessorStatements = parseStatements();
                //memberNode.accessor = Optional.of(accessorStatements);
            }


            //checks for mutator & ':' and saves its statements
            if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isEmpty()) {
                    throw new SyntaxErrorException("colon after mutator expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                //List<StatementNode> mutatorStatements = parseStatements();
                //memberNode.mutator = Optional.of(mutatorStatements);
            }
        }
        return Optional.of(memberNode);
    }

    //MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE methodBody
    private Optional<MethodDeclarationNode> parseMethodDeclaration() throws SyntaxErrorException {
       MethodDeclarationNode methodDeclaration = new MethodDeclarationNode();
       //looks for private, returns true if found
       if (tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
           methodDeclaration.isPrivate = true;
       }
        //looks for shared, returns true if found
       if (tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
           methodDeclaration.isShared = true;
       }
       //looks for method header, throws error if not found
       Optional<MethodHeaderNode> methodHeader = parseMethodHeader();
       if (methodHeader.isEmpty()) {
           throw new SyntaxErrorException("method header expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
       }
       //assigns method declarations name parameters and returns
       methodDeclaration.name = methodHeader.get().name;
       methodDeclaration.parameters = methodHeader.get().parameters;
       methodDeclaration.returns = methodHeader.get().returns;

       //newline required after method header
       requireNewLine();

       //parses method body
       parseMethodBody(methodDeclaration.locals, methodDeclaration.statements);

       return Optional.of(methodDeclaration);
    }

    //MethodBody = INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
    private void parseMethodBody(List<VariableDeclarationNode> locals, List<StatementNode> statements) throws SyntaxErrorException {
        // look for indent, throw error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //parse both variable declarations and statements
        while (!tokenManager.done()) {
            Optional<Token> nextToken = tokenManager.peek(0);

            // If dedent found, exit the loop
            if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.DEDENT) {
                tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                break;
            }

            // Check if it's a variable declaration (two WORD tokens in a row)
            if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.WORD) {
                Optional<Token> nextAfterWord = tokenManager.peek(1);
                // If two consecutive WORD tokens, treat it as a variable declaration
                if (nextAfterWord.isPresent() && nextAfterWord.get().getType() == Token.TokenTypes.WORD) {
                    Optional<VariableDeclarationNode> variableDeclaration = parseVariableDeclaration();
                    if (variableDeclaration.isPresent()) {
                        locals.add(variableDeclaration.get());
                        requireNewLine(); // Expect newline after variable declaration
                        continue; // Continue the loop to handle more declarations or statements
                    }
                }
            }

            //if it's not a variable declaration, attempt to parse a statement
            Optional<StatementNode> statement = parseStatement();
            if (statement.isPresent()) {
                statements.add(statement.get());
                if (statement.get() instanceof AssignmentNode || statement.get() instanceof MethodCallStatementNode) {
                    requireNewLine();
                }
                continue; // continue to check for more statements
            }
            // throws error if neither found
            throw new SyntaxErrorException("Expected variable declaration or statement.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    //Statements = INDENT { Statement NEWLINE } DEDENT
    private List<StatementNode> parseStatements() throws SyntaxErrorException {
        List<StatementNode> statements = new ArrayList<>();
        //looks for indent, throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw new SyntaxErrorException("Indent expected.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //if dedent found no statements
        while (!tokenManager.done()) {
            Optional<Token> nextToken = tokenManager.peek(0);
            if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.DEDENT) {
                if (!statements.isEmpty()) {
                    System.out.println("END OF STATEMENT(S) next token:" + nextToken.get());
                    tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                    break;
                } else {
                    throw new SyntaxErrorException("Unexpected DEDENT w/o statements.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
            //parse through statements
            Optional<StatementNode> statementNode = parseStatement();
            if (statementNode.isEmpty()) {
                throw new SyntaxErrorException("Expected statement.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            statements.add(statementNode.get());
            // Consume NEWLINE if it exists
            if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
        }
        return statements;
    }

    //Statement = If | Loop | MethodCall | Assignment
    private Optional<StatementNode> parseStatement() throws SyntaxErrorException {
        Optional<Token> nextToken = tokenManager.peek(0);
        if (nextToken.isEmpty()) {
            return Optional.empty();
        }

        Token token = nextToken.get();
        //parses through statements (if & loop) calls disambiguate if a word is found
        if (token.getType() == Token.TokenTypes.IF) {
            return parseIf();
        } else if (token.getType() == Token.TokenTypes.LOOP) {
            return parseLoop();
        } else if (token.getType() == Token.TokenTypes.WORD) {
            return disambiguate();
        }
        return Optional.empty();
    }

    //If = "if" BoolExpTerm NEWLINE Statements ["else" NEWLINE (Statement | Statements)]
    private Optional<StatementNode> parseIf() throws SyntaxErrorException {
        //looks for if
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) {
            return Optional.empty();
        }
        //parses boolean expression term, throws error if not found
        Optional<ExpressionNode> condition = parseBoolExpTerm();
        if (condition.isEmpty()) {
            throw new SyntaxErrorException("expected boolean expression after if.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        System.out.println("tokens after parsing bool exp" + tokenManager.peek(0));

        //newline required after boolean expression
        requireNewLine();

        //parses statements after newline
        List<StatementNode> ifStatements = parseStatements();

        //assigns if node its conditions and statements
        IfNode ifNode = new IfNode();
        ifNode.condition = condition.get();
        ifNode.statements = ifStatements;

        //loops for else
        if(tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()) {
            requireNewLine();
            List<StatementNode> elseStatements = parseStatements();

            ElseNode elseNode = new ElseNode();
            elseNode.statements = elseStatements;
            ifNode.elseStatement = Optional.of(elseNode);
            System.out.println("else statements: " + elseNode.statements);
        } else {
            System.out.println("No else statements in if statement");
            ifNode.elseStatement = Optional.empty();
        }
        return Optional.of(ifNode);
    }

    //Loop = "loop" [VariableReference "=" ] ( BoolExpTerm ) NEWLINE Statements
    private Optional<StatementNode> parseLoop() throws SyntaxErrorException {

        //looks for loop
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()) {
            return Optional.empty();
        }
        System.out.println("Parsing Loop");
        Optional<VariableReferenceNode> assignment = Optional.empty();
        if (tokenManager.peek(0).isPresent() && tokenManager.peek(1).isPresent() && tokenManager.peek(1).get().getType() == Token.TokenTypes.ASSIGN) {
            Optional<ExpressionNode> varRef = parseVariableReference();
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
            if (varRef.isEmpty()) {
                throw new SyntaxErrorException("Expected variable reference after assignment.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            assignment = Optional.of((VariableReferenceNode) varRef.get());
        }
        Optional<ExpressionNode> condition = parseBoolExpTerm();
        if (condition.isEmpty()) {
            throw new SyntaxErrorException("expected boolean expression after loop.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //newline required after boolean expression term
        requireNewLine();

        //parses loop statements
        List<StatementNode> loopStatements = new ArrayList<>();

        Optional<Token> nextToken = tokenManager.peek(0);
        if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.INDENT) {
            loopStatements = parseStatements();
        }
        LoopNode loopNode = new LoopNode();
        loopNode.statements = loopStatements;
        loopNode.expression = condition.get();
        loopNode.assignment = assignment;
        return Optional.of(loopNode);
    }

    //Assignment = VariableReference "=" Expression
    private Optional<StatementNode> parseAssignment() throws SyntaxErrorException {
        //looks for var ref
        Optional<ExpressionNode> variableRef = parseVariableReference();
        if (variableRef.isEmpty()) {
            return Optional.empty();
        }

        //looks for assignment op '=', throws error if not found
        if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
            throw new SyntaxErrorException(" '=' expected after variable ref.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //parses expressions after '=', throws error if not found
        Optional<ExpressionNode> expression = parseExpression();
        if (expression.isEmpty()) {
            throw new SyntaxErrorException("expression expected on right of '=' .", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.target = (VariableReferenceNode) variableRef.get();
        assignmentNode.expression = expression.get();
        //requireNewLine(); //incorrect I think

        return Optional.of(assignmentNode);
    }

    //MethodCall = [VariableReference { "," VariableReference } "=" MethodCallExpression
    private Optional<StatementNode> parseMethodCall() throws SyntaxErrorException {
        //linked list to store multiple var refs
        LinkedList<VariableReferenceNode> variableRefs = new LinkedList<>();

        //looks for var ref
        Optional<ExpressionNode> firstVariable = parseVariableReference();
        if (firstVariable.isEmpty()) {
            return Optional.empty();
        }
        variableRefs.add((VariableReferenceNode) firstVariable.get());

        //if comma is present parse through following var refs & add to linked list
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            Optional<ExpressionNode> additionalVariable = parseVariableReference();
            if (additionalVariable.isEmpty()) {
                throw new SyntaxErrorException("No variable reference after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            variableRefs.add((VariableReferenceNode) additionalVariable.get());
        }
        //looks for assignment op '=' after var refs
        if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()) {
            throw new SyntaxErrorException("no '=' after variable reference(s)", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //parses method call expression after '=', throws error if not found
        Optional<ExpressionNode> methodCallExpression = parseMethodCallExpression();
        if (methodCallExpression.isEmpty()) {
            throw new SyntaxErrorException("no method call expression after '=' ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        MethodCallExpressionNode methodCallExpressionNode = (MethodCallExpressionNode) methodCallExpression.get();
        MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode(methodCallExpressionNode);
        methodCallStatementNode.returnValues = variableRefs;

        return Optional.of(methodCallStatementNode);
    }

    //BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpTerm} | "not" BoolExpTerm (handle not later)
    private Optional<ExpressionNode> parseBoolExpTerm() throws SyntaxErrorException {
        //parses first factor (left side of expression)
        System.out.println("Parsing Bool term");
        Optional<ExpressionNode> leftOp = parseBoolExpFactor();
        //returns empty if not present
        if (leftOp.isEmpty()) {
            System.out.println("No left side factor found");
            return Optional.empty();
        }

        ExpressionNode left = leftOp.get();

        //peeks for "AND" or "OR" operators
        while(true) {
            Optional<Token> opToken = tokenManager.peek(0);
            if (opToken.isEmpty()) {
                break;
            }
            Token token = opToken.get();
            // Check for AND (higher precedence)
            if (token.getType() == Token.TokenTypes.AND) {
                tokenManager.matchAndRemove(token.getType());
                System.out.println("Middle Operator: AND");

                Optional<ExpressionNode> rightExp = parseBoolExpFactor();
                if (rightExp.isEmpty()) {
                    throw new SyntaxErrorException("Expected right side boolean expression after 'and'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }

                BooleanOpNode andNode = new BooleanOpNode();
                andNode.op = BooleanOpNode.BooleanOperations.and;
                andNode.left = left;
                andNode.right = rightExp.get();

                left = andNode;
                continue;
            }

            // Check for OR (lower precedence)
            if (token.getType() == Token.TokenTypes.OR) {
                tokenManager.matchAndRemove(token.getType());
                System.out.println("Middle Operator: OR");

                Optional<ExpressionNode> rightExp = parseBoolExpTerm(); // Recursive to handle OR grouping
                if (rightExp.isEmpty()) {
                    throw new SyntaxErrorException("Expected right side boolean expression after 'or'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }

                BooleanOpNode orNode = new BooleanOpNode();
                orNode.op = BooleanOpNode.BooleanOperations.or;
                orNode.left = left;
                orNode.right = rightExp.get();

                left = orNode;
                break; // OR has lower precedence; stop parsing further
            }

            break; // Stop if no AND or OR operator
        }
        System.out.println("Final Term " + left);
        return Optional.of(left);
    }

    //BoolExpFactor = MethodCallExpression | (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | VariableReference
    private Optional<ExpressionNode> parseBoolExpFactor() throws SyntaxErrorException {
        Optional<Token> nextNext = tokenManager.peek(  1);
        if (nextNext.isPresent() && nextNext.get().getType() == Token.TokenTypes.LPAREN) {
            Optional<ExpressionNode> methodCallExpression = parseMethodCallExpression();
            if (methodCallExpression.isPresent()) {
                return methodCallExpression;
            }
        }

        Optional<ExpressionNode> leftExp = parseExpression();
        if (leftExp.isEmpty()) {
            return Optional.empty();
        } else {
            System.out.println("Left side factor: " + leftExp);
        }

        Optional<Token> opToken = tokenManager.peek(0);
        if (opToken.isPresent()) {
            Token token = opToken.get();

            // check if the next token is a comparison operator and then consume it
            if (token.getType() == Token.TokenTypes.EQUAL ||
                    token.getType() == Token.TokenTypes.NOTEQUAL ||
                    token.getType() == Token.TokenTypes.LESSTHANEQUAL ||
                    token.getType() == Token.TokenTypes.GREATERTHANEQUAL ||
                    token.getType() == Token.TokenTypes.GREATERTHAN ||
                    token.getType() == Token.TokenTypes.LESSTHAN) {

                tokenManager.matchAndRemove(token.getType());
                System.out.println("Comparison Operator: " + token);

                Optional<ExpressionNode> rightExp = parseExpression();
                if (rightExp.isEmpty()) {
                    throw new SyntaxErrorException("Expected right expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                } else {
                    System.out.println("Right side factor: " + rightExp);
                }

                CompareNode comparison = new CompareNode();
                comparison.left = leftExp.get();
                comparison.right = rightExp.get();

                //find comparison op with matched type
                switch (token.getType()) {
                    case EQUAL:
                        comparison.op = CompareNode.CompareOperations.eq;
                        break;
                    case NOTEQUAL:
                        comparison.op = CompareNode.CompareOperations.ne;
                        break;
                    case GREATERTHAN:
                        comparison.op = CompareNode.CompareOperations.gt;
                        break;
                    case LESSTHAN:
                        comparison.op = CompareNode.CompareOperations.lt;
                        break;
                    case GREATERTHANEQUAL:
                        comparison.op = CompareNode.CompareOperations.ge;
                        break;
                    case LESSTHANEQUAL:
                        comparison.op = CompareNode.CompareOperations.le;
                        break;
                    default:
                        throw new SyntaxErrorException("unknown comparison operator", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                return Optional.of(comparison);
                }
            }
        return leftExp;
    }

    //MethodCallExpression = [Identifier "."] Identifier "(" [Expression {"," Expression }] ")"
    private Optional<ExpressionNode> parseMethodCallExpression() throws SyntaxErrorException {
        Optional<Token> identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (identifier.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> objectName = Optional.empty();
        if (tokenManager.matchAndRemove(Token.TokenTypes.DOT).isPresent()){
            objectName = Optional.of(identifier.get().getValue());
            identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (identifier.isEmpty()) {
                throw new SyntaxErrorException("Expected method name after '.'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        Token methodName = identifier.get();
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected '(' after method name", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        List<ExpressionNode> args = new ArrayList<>();
        do {
            Optional<ExpressionNode> expression = parseExpression();
            expression.ifPresent(args::add);
        } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());

        if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            throw new SyntaxErrorException("Expected ')' after expression(s)", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        MethodCallExpressionNode methodCallExpressionNode = new MethodCallExpressionNode();
        methodCallExpressionNode.objectName = objectName;
        methodCallExpressionNode.methodName = methodName.getValue();
        methodCallExpressionNode.parameters = args;

        return Optional.of(methodCallExpressionNode);
    }

    //VariableReference = Identifier
    private Optional<ExpressionNode> parseVariableReference() throws SyntaxErrorException {
        //looks for identifier and sets var ref name
        Optional<Token> token = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (token.isPresent()) {
            VariableReferenceNode variableRef = new VariableReferenceNode();
            variableRef.name = token.get().getValue();
            return Optional.of(variableRef);
        }
        return Optional.empty();
        }

    //disambiguate between method calls and assignments
    private Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        // looks ahead to see if the next token is a var ref
        Optional<Token> nextToken = tokenManager.peek(0);
        if (nextToken.isEmpty() || nextToken.get().getType() != Token.TokenTypes.WORD) {
            return Optional.empty();
        }

        Optional<Token> nextAfterRef = tokenManager.peek(1);
        if (nextAfterRef.isEmpty()) {
            return Optional.empty();
        }

        System.out.println("Disambiguated token: " + nextAfterRef);

        // If there's a '.' following the word, parse as a method call expression
        if (nextAfterRef.get().getType() == Token.TokenTypes.DOT) {
            Optional<ExpressionNode> methodCallExpression = parseMethodCallExpression();
            if (methodCallExpression.isEmpty()) {
                throw new SyntaxErrorException("Expected valid method call expression after '.'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            MethodCallExpressionNode methodCallExprNode = (MethodCallExpressionNode) methodCallExpression.get();
            MethodCallStatementNode methodCallStmtNode = new MethodCallStatementNode(methodCallExprNode);
            return Optional.of(methodCallStmtNode);
        }

        // if peeked token is a comma, it means this is part of a method call with multiple return values
        if (nextAfterRef.get().getType() == Token.TokenTypes.COMMA) {
            return parseMethodCall(); // Parse as method call
        }
        // if peeked token is a '=' it is a method call or assignment (assignment handles both cases)
        if (nextAfterRef.get().getType() == Token.TokenTypes.ASSIGN) {
            Optional<Token> nextAfterAssign = tokenManager.peek(2);
            if (nextAfterAssign.isPresent() && nextAfterAssign.get().getType() == Token.TokenTypes.WORD) {
                Optional<Token> nextAfterWord = tokenManager.peek(3);
                if (nextAfterWord.isPresent() && nextAfterWord.get().getType() == Token.TokenTypes.LPAREN) {
                    return parseMethodCall();
                }
            }
            return parseAssignment();
        }
        return Optional.empty();
    }

    //Expression = Term { ("+"|"-") Term }
    private Optional<ExpressionNode> parseExpression() throws SyntaxErrorException {
        //parses first term, returns empty if not present
        Optional<ExpressionNode> term = parseTerm();
        if (term.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expression = term.get();

        //loops through remaining +,- operators on remaining terms
        while (true) {
            Optional<Token> opToken = Optional.empty();
            if (tokenManager.matchAndRemove(Token.TokenTypes.PLUS).isPresent()) {
                opToken = Optional.of(new Token(Token.TokenTypes.PLUS, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            } else if (tokenManager.matchAndRemove(Token.TokenTypes.MINUS).isPresent()) {
                opToken = Optional.of(new Token(Token.TokenTypes.MINUS, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            }
            if (opToken.isEmpty()) {
                break;
            }
            Optional<ExpressionNode> nextTerm = parseTerm();
            if (nextTerm.isEmpty()) {
                throw new SyntaxErrorException("Expected term after operators", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = expression;
            mathOpNode.right = nextTerm.get();
            mathOpNode.op = opToken.get().getType() == Token.TokenTypes.PLUS
                    ? MathOpNode.MathOperations.add
                    : MathOpNode.MathOperations.subtract;
            expression = mathOpNode;
        }
        return Optional.of(expression);
    }

    //Term = Factor { ("*"|"/"|"%") Factor }
    private Optional<ExpressionNode> parseTerm() throws SyntaxErrorException {
        //parses first factor, returns empty if not present
        Optional<ExpressionNode> factor = parseFactor();
        if (factor.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode term = factor.get();

        //loops through remaining *,/,& operators and remaining factors
        while (true) {
            Optional<Token> opToken = Optional.empty();
            if (tokenManager.matchAndRemove(Token.TokenTypes.TIMES).isPresent()) {
                opToken = Optional.of(new Token(Token.TokenTypes.TIMES, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            } else if (tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE).isPresent()) {
                opToken = Optional.of(new Token(Token.TokenTypes.DIVIDE, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            } else if (tokenManager.matchAndRemove(Token.TokenTypes.MODULO).isPresent()) {
                opToken = Optional.of(new Token(Token.TokenTypes.MODULO, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            }
            if (opToken.isEmpty()) {
                break;
            }

            //parses factor after operator
            Optional<ExpressionNode> nextFactor = parseFactor();
            if (nextFactor.isEmpty()) {
                throw new SyntaxErrorException("Expected factor after operators", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = term;
            mathOpNode.right = nextFactor.get();
            switch (opToken.get().getType()) {
                case Token.TokenTypes.TIMES:
                    mathOpNode.op = MathOpNode.MathOperations.multiply;
                    break;
                case Token.TokenTypes.DIVIDE:
                    mathOpNode.op = MathOpNode.MathOperations.divide;
                    break;
                case Token.TokenTypes.MODULO:
                    mathOpNode.op = MathOpNode.MathOperations.modulo;
                    break;
            }
            term = mathOpNode;
        }
        return Optional.of(term);
    }

    /* Factor = NumberLiteral | VariableReference | "true" | "false" | StringLiteral | CharacterLiteral
    | MethodCallExpression | "(" Expression ")" | "new" Identifier "(" [Expression {"," Expression }] ")" */
    private Optional<ExpressionNode> parseFactor() throws SyntaxErrorException {
        //checks true & false tokens first
        if (tokenManager.matchAndRemove(Token.TokenTypes.TRUE).isPresent()) {
            return Optional.of(new BooleanLiteralNode(true));
        } else if (tokenManager.matchAndRemove(Token.TokenTypes.FALSE).isPresent()) {
            return Optional.of(new BooleanLiteralNode(false));
        }

        //checks for new tokens
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEW).isPresent()) {
            //looks for new's identifier (className)
            Optional<Token> identifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (identifier.isEmpty()) {
                throw new SyntaxErrorException("Expected class name after 'new'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected '(' after 'new'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            List<ExpressionNode> args = new ArrayList<>();
            do {
                Optional<ExpressionNode> expression = parseExpression();
                expression.ifPresent(args::add);
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());

            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected ')' after 'new'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            //creates new newNode and assigns class name (identifier) & parameter(s) (expressions)
            NewNode newNode = new NewNode();
            newNode.className = identifier.get().getValue();
            newNode.parameters = args;
            return Optional.of(newNode);
        }

        //looks for expressions enclosed in ()
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            //parses expression withing ()
            Optional<ExpressionNode> expression = parseExpression();
            if (expression.isEmpty()) {
                throw new SyntaxErrorException("Expected expression inside '('", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                throw new SyntaxErrorException("Expected ')' after expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return expression;
        }

        //parses number literal
        Optional<Token> numToken = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if (numToken.isPresent()) {
            //converts parsed number to float
            float value = Float.parseFloat(numToken.get().getValue());
            //creates new Numeric Literal and assigns number float value
            NumericLiteralNode numericLiteralNode = new NumericLiteralNode();
            numericLiteralNode.value = value;
            return Optional.of(numericLiteralNode);
        }

        //parses string literal
        Optional<Token> strToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
        if (strToken.isPresent()) {
            //creates new String Literal and assigns quoted string value
            StringLiteralNode stringLiteralNode = new StringLiteralNode();
            stringLiteralNode.value = strToken.get().getValue();
            return Optional.of(stringLiteralNode);
        }

        //parses character literal
        Optional<Token> charToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
        if (charToken.isPresent()) {
            //converts parsed quoted character to char
            char value = charToken.get().getValue().charAt(1);
            //creates new Char Literal and assigns quoted char, char value
            CharLiteralNode charLiteralNode = new CharLiteralNode();
            charLiteralNode.value =  value;
            return Optional.of(charLiteralNode);
        }

        //checks for method call expressions or variable references (both start with identifiers)
        Optional<Token> nextToken = tokenManager.peek(0);
        if (nextToken.isPresent() && nextToken.get().getType() == Token.TokenTypes.WORD) {
            Optional<Token> nextNextToken = tokenManager.peek(1);
            //peeks for a '.' or '(' if found it's a method call exp
            if (nextNextToken.isPresent() && nextNextToken.get().getType() == Token.TokenTypes.DOT || nextNextToken.get().getType() == Token.TokenTypes.LPAREN) {
                return parseMethodCallExpression();
            } else { //var ref if not call exp
                return parseVariableReference();
            }
        }
        return Optional.empty();
    }
}
