package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {
    private TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;

        ClassNode consoleClass = new ClassNode();
        consoleClass.name = "console";

        ConsoleWrite consoleWrite = new ConsoleWrite();
        consoleWrite.name = "write";
        consoleWrite.isShared = true;
        consoleWrite.isVariadic = true;

        consoleClass.methods.add(consoleWrite);
        top.Classes.add(consoleClass);
    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        for (ClassNode classNode : top.Classes) {
            for (MethodDeclarationNode methodNode : classNode.methods) {
                if (methodNode.isShared && !methodNode.isPrivate && methodNode.name.equals("start") && methodNode.parameters.isEmpty()) {
                    interpretMethodCall(Optional.empty(), methodNode, List.of());
                    return;
                }
            }
        }
        throw new RuntimeException("Start method not found");
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        //eval param's
        List<InterpreterDataType> parameters = getParameters(object, locals, mc);
        //case 1: no object name
        if (mc.objectName.isEmpty()) {
            if (object.isEmpty()) {
                throw new RuntimeException("Cannot call method with no object");
            }
            MethodDeclarationNode methodNode = getMethodFromObject(object.get(), mc ,parameters);
            return interpretMethodCall(object, methodNode, parameters);
        }
        //case 2: object name refers to class
        Optional<ClassNode> classNode = getClassByName(mc.objectName.get());
        if (classNode.isPresent()) {
            for (MethodDeclarationNode methodNode : classNode.get().methods) {
                if (doesMatch(methodNode,mc,parameters)) {
                    return interpretMethodCall(Optional.empty(), methodNode, parameters);
                }
            }
        }
        //case 3: object name refers to var
        if (mc.objectName.isPresent()) {
            InterpreterDataType objectVariable = findVariable(mc.objectName.get(), locals, object);
            if (objectVariable instanceof ObjectIDT obj) {
                MethodDeclarationNode methodNode = getMethodFromObject(obj, mc, parameters);
                return interpretMethodCall(Optional.of(obj), methodNode, parameters);
            }
        }else if (object.isPresent()) {
            MethodDeclarationNode methodNode = getMethodFromObject(object.get(), mc, parameters);
            return interpretMethodCall(object, methodNode, parameters);
        }
        throw new RuntimeException("No matching method found for" + mc.objectName.get());
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        //checks if 'm' is a build-in, calls execute if is
        if (m instanceof BuiltInMethodDeclarationNode builtIn) {
            return builtIn.Execute(values);
        }
        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        //throws error if number of parameters does not match value expectations
        if (m.parameters.size() != values.size()) {
            throw new RuntimeException("Wrong number of parameters");
        }
        //adds all parameters by name to locals
        for (int i = 0; i < m.parameters.size(); i++) {
            locals.put(m.parameters.get(i).name, values.get(i));
        }
        for (VariableDeclarationNode localVar : m.locals) {
            if (!locals.containsKey(localVar.name)) {
                locals.put(localVar.name, instantiate(localVar.type));
            }
        }
        for (VariableDeclarationNode returnVar : m.returns) {
            if (!locals.containsKey(returnVar.name)) {
                locals.put(returnVar.name, instantiate(returnVar.type));
            }
        }
        //calls interpret statement block on locals
        interpretStatementBlock(object, m.statements, locals);

        List<InterpreterDataType> returnValues = new LinkedList<>();
        for (VariableDeclarationNode returnVar : m.returns) {
            returnValues.add(findVariable(returnVar.name,locals,object));
        }
        return returnValues;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        System.out.println("Constructor Call -> obj name:" + mc.objectName.orElse(null));
        //populates a list of IDTs
        List<InterpreterDataType> parameters = getParameters(callerObj, locals, mc);
        String className = mc.objectName.orElseThrow(() -> new RuntimeException("Constructor must have class name"));
        System.out.println("Looking for class: " + className);
        //find class for the constructor, throws error if not found
        Optional<ClassNode> classNode = getClassByName(className);
        if (classNode.isEmpty()) {
            throw new RuntimeException("Class not found" + mc.objectName.get());
        }
        ClassNode classNode1 = classNode.get();

        ConstructorNode constructor = null;
        //finds a constructor that is a good match
        for (ConstructorNode constructorNode : classNode1.constructors) {
            if (doesConstructorMatch(constructorNode,mc,parameters)) {
                constructor = constructorNode;
                break;
            }
        }
        //throws error if doesConstructorMatch didn't find a constructor
        if (constructor == null) {
            throw new RuntimeException("No constructor found for " + mc.objectName.get());
        }
        //interprets match
        interpretConstructorCall(newOne, constructor, parameters);
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        System.out.println("Constructor Parameters -> Expected: " + c.parameters.size() + "Provided: " + values.size());
        //checks passed params & validates count
        if (c.parameters.size() != values.size()) {
            throw new RuntimeException("Number of parameters does not match. Expected: " + c.parameters.size() + ", Actual: " + values.size());
        }
        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        //loops through params & adds them with assigned names to locals
        for (int i = 0; i < c.parameters.size(); i++) {
            locals.put(c.parameters.get(i).name, values.get(i));
            System.out.println("Added to locals: " + c.parameters.get(i).name + " = " + values.get(i));
        }

        System.out.println("Initializing class members...");
        for (MemberNode var : object.astNode.members) {
            InterpreterDataType defaultValue = instantiate(var.declaration.type);
            object.members.put(var.declaration.name, defaultValue);
            System.out.println("Initialized Variables: " + var.declaration.name + " with a value of: " + defaultValue);
        }

        System.out.println("Constructor Statements: " + c.statements);

        interpretStatementBlock(Optional.of(object), c.statements, locals);

        System.out.println("Constructor complete -> Members: " + object.members );
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        //For each statement in statements
        for (StatementNode statement : statements) {
            //Handles assignments
            if (statement instanceof AssignmentNode assignment) {
                System.out.println("Processing Assignment: " + assignment.target.name + " = " + assignment.expression);
                //find the target using findVariable
                InterpreterDataType target = findVariable(assignment.target.name, locals, object);
                System.out.println("Found target: " + assignment.target.name + " -> " + target);
                //evaluates the expression
                InterpreterDataType value = evaluate(locals, object, assignment.expression);
                System.out.println("Evaluated value: " + assignment.expression + " -> " + value);
                //assigns target to the result (found var) to value (evaluated)
                target.Assign(value);
                System.out.println("Assigned value: " + value + " to variable " + assignment.target.name);
            }
            //Handles method calls
            else if (statement instanceof MethodCallStatementNode call) {
                //'doMethodCall' calls findMethodForMethodCall&RunIt
                List<InterpreterDataType> returnValues = findMethodForMethodCallAndRunIt(object, locals, call);
                //loops over returned values and copies into locals
                for (int i = 0; i < returnValues.size(); i++) {
                    if (i < call.returnValues.size()) {
                        String returnName = call.returnValues.get(i).name;
                        InterpreterDataType target = findVariable(returnName, locals, object);
                        target.Assign(returnValues.get(i));
                    }
                }
            }
            //Handles loops
            else if (statement instanceof LoopNode loopNode) {
                boolean isIteratorLoop = loopNode.assignment.isPresent();
                if (isIteratorLoop) {
                    VariableReferenceNode iteratorVar = loopNode.assignment.get();
                    InterpreterDataType iterator = findVariable(iteratorVar.name, locals, object);

                    if (!(iterator instanceof ObjectIDT obj) || !obj.astNode.interfaces.contains(iterator)) {
                        throw new RuntimeException("Loop assignment must be an object implementing 'iterator'");
                    }
                    MethodDeclarationNode getNextMethod = obj.astNode.methods.stream()
                            .filter(method -> method.name.equals("getNext"))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Iterator object must have a 'getNext()' method"));

                    boolean hasNext = true;
                    while (hasNext) {
                        List<InterpreterDataType> results = interpretMethodCall(Optional.of(obj), getNextMethod, List.of());

                        if (results.size() != 2 || !(results.get(0) instanceof BooleanIDT)) {
                            throw new RuntimeException("Iterator 'getNext()' method must return a boolean and a value");
                        }

                        hasNext = ((BooleanIDT) results.get(0)).Value;
                        InterpreterDataType next = (InterpreterDataType) results.get(1);

                        if (hasNext ) {
                            InterpreterDataType loop = findVariable(iteratorVar.name, locals, object);
                            loop.Assign(next);
                            interpretStatementBlock(object, loopNode.statements, locals);
                        }
                    }
                } else {
                    while(true) {
                        InterpreterDataType conditionResult = evaluate(locals, object, loopNode.expression);
                        if (!(conditionResult instanceof BooleanIDT)) {
                            throw new RuntimeException("Loop condition must evaluate to a boolean");
                        }
                        boolean keepGoing = ((BooleanIDT) conditionResult).Value;
                        if (!keepGoing) {
                            break;
                        }
                        interpretStatementBlock(object, loopNode.statements, locals);
                    }
                }

            }


            //Handles if-else
            else if (statement instanceof IfNode ifNode) {
                //evaluate condition
                BooleanIDT condition = (BooleanIDT) evaluate(locals, object, ifNode.condition);
                //if true interpret statement block
                if (condition.Value) {
                    interpretStatementBlock(object, ifNode.statements, locals);
                }
                //if not & there's an else, interpret statement block on else block
                else
                    ifNode.elseStatement.ifPresent(elseNode -> interpretStatementBlock(object, elseNode.statements, locals));
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        if (expression == null) {
            throw new RuntimeException("Expression must not be null");
        }
        System.out.println("Evaluating Expression: " + expression.getClass().getSimpleName());
        //Resolves 4 expression nodes (Bool , String , Number , Char)
        //BooleanLiteralNode
        if (expression instanceof BooleanLiteralNode) {
            return new BooleanIDT(((BooleanLiteralNode)expression).value);
        }
        //StringLiteralNode
        if (expression instanceof StringLiteralNode) {
            return new StringIDT(((StringLiteralNode)expression).value);
        }
        //NumericLiteralNode
        if (expression instanceof NumericLiteralNode) {
            return new NumberIDT(((NumericLiteralNode)expression).value);
        }
        //CharLiteralNode
        if (expression instanceof CharLiteralNode) {
            return new CharIDT(((CharLiteralNode)expression).value);
        }

        //BooleanOpNode
        if (expression instanceof BooleanOpNode boolOp) {
            //eval left & right
            InterpreterDataType l = evaluate(locals, object, boolOp.left);
            InterpreterDataType r = evaluate(locals, object, boolOp.right);

            //throws error if left or right aren't Bool IDT's
            if (!(l instanceof BooleanIDT) || !(r instanceof BooleanIDT)) {
                throw new RuntimeException("Boolean operations require boolean operators");
            }

            //assign l & r to bool IDT l & r values
            boolean lVal = ((BooleanIDT) l).Value;
            boolean rVal = ((BooleanIDT) r).Value;
            //preform AND || OR on the results
            return new BooleanIDT(boolOp.op == BooleanOpNode.BooleanOperations.and
                    ? lVal && rVal
                    : lVal || rVal);
        }
        //CompareNode
        if (expression instanceof CompareNode compare) {
            //evaluate both side
            InterpreterDataType l = evaluate(locals, object, compare.left);
            InterpreterDataType r = evaluate(locals, object, compare.right);

            //assign sides to num IDT values
            if (l instanceof NumberIDT && r instanceof NumberIDT) {
                double lVal = ((NumberIDT) l).Value;
                double rVal = ((NumberIDT) r).Value;

                //switch return case for every operator case (== != <  > <= >=)
                return new BooleanIDT(switch (compare.op) {
                    case eq -> lVal == rVal;
                    case ne -> lVal != rVal;
                    case lt -> lVal < rVal;
                    case gt -> lVal > rVal;
                    case le -> lVal <= rVal;
                    case ge -> lVal >= rVal;
                });
            }
            throw new RuntimeException("Unsupported compare operator between: " + l.getClass().getSimpleName() + " and " + r.getClass().getSimpleName());
        }
        //MathOpNode
        if (expression instanceof MathOpNode mathNode) {
            //evaluate both sides
            InterpreterDataType l = evaluate(locals, object, mathNode.left);
            InterpreterDataType r = evaluate(locals, object, mathNode.right);

            //assign sides to num IDT values
            if (l instanceof NumberIDT && r instanceof NumberIDT) {
                float lVal = ((NumberIDT) l).Value;
                float rVal = ((NumberIDT) r).Value;

                //switch return case for every operator case (+-*/%)
                return switch (mathNode.op) {
                    case add -> new NumberIDT(lVal + rVal);
                    case subtract -> new NumberIDT(lVal - rVal);
                    case multiply -> new NumberIDT(lVal * rVal);
                    case divide -> {
                        //throws error if dividing by 0
                        if (rVal == 0) {
                            throw new RuntimeException("Division by zero");
                        }
                        yield new NumberIDT(lVal / rVal);
                    }
                    case modulo -> new NumberIDT(lVal % rVal);
                };
            }
            //handle adding strings
            if (mathNode.op == MathOpNode.MathOperations.add && l instanceof StringIDT && r instanceof StringIDT) {
                return new StringIDT(((StringIDT) l).Value + ((StringIDT) r).Value);
            }
            throw new RuntimeException("Unsupported math operator: " + mathNode.getClass().getSimpleName());
        }
        //MethodCall
        if (expression instanceof MethodCallExpressionNode call) {
            System.out.println("Eval MethodCallExpression: " + call.methodName);
            System.out.println("Object: " + call.objectName.orElse("None"));
            System.out.println("Parameters: " + call.parameters);

            MethodCallStatementNode temp = new MethodCallStatementNode();
            temp.methodName = call.methodName;
            temp.objectName = call.objectName;
            temp.parameters = call.parameters;

            System.out.println("Temporary MethodCallStatementNode created with:");
            System.out.println("Method Name: " + temp.methodName);
            System.out.println("Object Name: " + temp.objectName.orElse("None"));
            System.out.println("Parameters: " + temp.parameters);

            //find results by doing method call
            List<InterpreterDataType> results = findMethodForMethodCallAndRunIt(object, locals, temp);
            if (results.isEmpty()) {
                throw new RuntimeException("Method call returned no results");
            }
            System.out.println("Returning first result of method call: " + results.getFirst());
            return results.getFirst();
        }
        //VariableReferenceNode
        if (expression instanceof VariableReferenceNode var) {
            //calls & returns var
            return findVariable(var.name, locals, object);
        }
        if (expression instanceof NewNode newNode) {
            Optional<ClassNode> classNodeOpt = getClassByName(newNode.className);
            if (classNodeOpt.isEmpty()) {
                throw new RuntimeException("Class not found: " + newNode.className);
            }
            ClassNode classNode = classNodeOpt.get();

            ObjectIDT newObject = new ObjectIDT(classNode);

            MethodCallStatementNode call = new MethodCallStatementNode();
            call.methodName = "construct";
            call.objectName = Optional.of(newNode.className);
            call.parameters = newNode.parameters;

            findConstructorAndRunIt(object, locals, call, newObject);

            return newObject;
        }
        //throws error if given expression isnt among these
        throw new RuntimeException("Expression type not handled: " + expression.getClass().getSimpleName());
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this methoc call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        //check method name
        System.out.println("Matched method: " + m.name + " with call: " + mc.methodName);
        //checks for name match, returns false if two names dont match
        if (!m.name.equals(mc.methodName)) {
            return false;
        }
        //checks for built-in's & is variadic
        if (m instanceof BuiltInMethodDeclarationNode builtIn && builtIn.isVariadic) {
            return true;
        }
        //check for param match, returns false if two parameter counts are not equal
        if (m.parameters.size() != mc.parameters.size()) {
            System.out.println("Param count mismatch");
            return false;
        }
        //loops through param's and checks types
        for (int i = 0; i < m.parameters.size(); i++) {
            VariableDeclarationNode paramDec = m.parameters.get(i);
            InterpreterDataType parameter = parameters.get(i);
            if (!typeMatchToIDT(paramDec.type, parameter)) {
                System.out.println("Param type mismatch at index: " + i);
                return false;
            }
        }
       //check return count, returns true if no return values are needed, returns false if two return counts are not equal
        if (m.returns.size() != mc.returnValues.size()) {
            if (mc.returnValues.isEmpty()) {
                return true;
            }
            System.out.println("Return count mismatch");
            return false;
        }
        //return true if all checks pass
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        //checks param count, returns false if two counts are not equal
        if (c.parameters.size() != mc.parameters.size()) {
            System.out.println("Constructor parameter count mismatch");
            return false;
        }
        //loops through parameters, returns false if parameters are not equal
        for (int i = 0; i < mc.parameters.size(); i++) {
            VariableDeclarationNode paramDec = c.parameters.get(i);
            InterpreterDataType parameter = parameters.get(i);
            if (!typeMatchToIDT(paramDec.type, parameter)) {
                System.out.println("Constructor parameter type mismatch at index: " + i);
                return false;
            }
        }
        System.out.println("Constructor matched");
        //return true if all constructor checks pass
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> parameters = new LinkedList<>();
        //evaluate each param in mc, gets & adds IDT to list
        for (ExpressionNode param : mc.parameters) {
            parameters.add(evaluate(locals,object,param));
        }
        return parameters;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        //checks primitive types (number,string.boolean,character)
        if (idt instanceof NumberIDT && type.equals("number")) {return true;}
        if (idt instanceof StringIDT && type.equals("string")) {return true;}
        if (idt instanceof BooleanIDT && type.equals("boolean")) {return true;}
        if (idt instanceof NumberIDT && type.equals("character")) {return true;}
        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        ClassNode classNode = object.astNode;
        System.out.println("Looking for method: " + mc.methodName);
        //loops over methods in mc, looks for a method that matches
        for (MethodDeclarationNode method : classNode.methods) {
            System.out.println("Checking method: " + method.name);
            if (doesMatch(method, mc, parameters)) {
                System.out.println("Method matched: " + method.name);
                return method;
            }
        }
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        return top.Classes.stream().filter(c -> c.name.equals(name)).findFirst();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        System.out.println("Looking for variable: " + name);
        System.out.println("Locals: " + locals);
        System.out.println("Object: " + (object.isPresent() ? object.get().members : "No object present"));
        //checks local variable names
        if (locals.containsKey(name)) {
            System.out.println("Variable found: " + name + " in locals: " + locals.get(name));
            return locals.get(name);
        }
        //checks and objects members
        if (object.isPresent() && object.get().members.containsKey(name)) {
            System.out.println("Variable found: " + name + " in object's members: " + object.get().members.get(name));
            return object.get().members.get(name);
        }
        throw new RuntimeException("Unable to find variable: " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        System.out.println("Instantiating type: " + type);
        //switch case to assign primitive types their IDT's
        switch (type) {
            case "number" -> {return new NumberIDT(0);}
            case "string" -> {return new StringIDT("");}
            case "boolean" -> {return new BooleanIDT(false);}
            case "character" -> {return new CharIDT(' ');}
            default -> {
                Optional<ClassNode> classNode = getClassByName(type);
                if (classNode.isPresent()) {
                    System.out.println("Found class: " + classNode.get().name);
                    return new ObjectIDT(classNode.get());
                }
                throw new RuntimeException("Unknown type: " + type);
            }
        }
    }
}
