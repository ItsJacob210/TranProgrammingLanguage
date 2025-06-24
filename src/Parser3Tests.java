import AST.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Parser3Tests
{
    @Test
    public void testDisambiguate() throws Exception {

        var l = new Lexer("class Tran\n" +
                "\thelloWorld() : number a, number b, number avg\n" +
                "\t\ta=b\n" +
                "\t\tb=a\n"+
                "\t\tavg=b\n" +
                "\tnumber z\n" +
                "\tnumber x\n" +
                "\tnumber y\n" );
        var tokens= l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);
        p.Tran();
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(3, myMethod.statements.size());

        Assertions.assertEquals("b = a\n", ((AssignmentNode)myMethod.statements.get(1)).toString());
        Assertions.assertInstanceOf(AssignmentNode.class, myMethod.statements.get(2));

    }

    @Test
    public void testVariableReference() throws Exception {
        var l = new Lexer("class Tran\n" +
                "\thelloWorld() : number a, number b, number avg\n" +
                "\t\ta=b\n" +
                "\t\tb=avg\n"+
                "\t\tavg=a\n" +
                "\tnumber z\n" +
                "\tnumber x\n" +
                "\tnumber y\n" );
        var tokens = l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);
        p.Tran();
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.get(0);

        Assertions.assertEquals("b", ((VariableReferenceNode)((AssignmentNode)myMethod.statements.get(1)).target).toString());
        Assertions.assertEquals("avg", ((VariableReferenceNode)((AssignmentNode)myMethod.statements.get(2)).target).toString());

        Assertions.assertEquals("[number a, number b, number avg]", ((myMethod.returns)).toString());

    }
   @Test
    public void testassign() throws Exception {


        var l = new Lexer("class Tran\n" +
                "\thelloWorld()\n" +
                "\t\tnumber a\n" +
                "\t\tnumber b\n" +
                "\t\tnumber avg\n" +
                "\t\ta=b\n" +
                "\t\tb=a\n" );
        var tokens = l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);
        p.Tran();
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(2, myMethod.statements.size());
        Assertions.assertEquals("a = b\n", (((AssignmentNode) myMethod.statements.get(0)).toString()));
        Assertions.assertEquals("b = a\n", (((AssignmentNode) myMethod.statements.get(1)).toString()));
    }



    @Test
    public void testBooleanExp_term() throws Exception {

        var l = new Lexer("class Tran\n" +
                "\thelloWorld()\n" +
                "\t\tif n>b && n!=a || n==b\n" +
                "\t\t\tn = a\n");
        var tokens = l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);
        p.Tran();
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertInstanceOf(IfNode.class, myMethod.statements.getFirst());
        Assertions.assertEquals("n > b and n != a or n == b", ((IfNode) myMethod.statements.getFirst()).condition.toString());

        Assertions.assertTrue(((IfNode) (myMethod.statements.getFirst())).elseStatement.isEmpty());
    }
    @Test
    public void testBooleanTerm_Factor() throws Exception {
        var l = new Lexer("class Tran\n" +
                "\thelloWorld()\n" +
                "\t\tif n>b && n!=a || n==b\n" +
                "\t\t\tn = a\n");
        var tokens = l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);
        p.Tran();
        Assertions.assertEquals(1, t.Classes.size());
        var myClass = t.Classes.getFirst();
        Assertions.assertEquals(1, myClass.methods.size());
        var myMethod = myClass.methods.getFirst();
        Assertions.assertEquals(1, myMethod.statements.size());
        Assertions.assertInstanceOf(IfNode.class, myMethod.statements.getFirst());
        Assertions.assertEquals("n > b", ((BooleanOpNode) ((BooleanOpNode) ((IfNode) myMethod.statements.getFirst()).condition).left).left.toString());
        Assertions.assertEquals("n != a", ((BooleanOpNode) ((BooleanOpNode) ((IfNode) myMethod.statements.getFirst()).condition).left).right.toString());
        Assertions.assertTrue(((IfNode) (myMethod.statements.getFirst())).elseStatement.isEmpty());
    }

    @Test
    public void testParseBooleanExpressions() throws Exception {
        // Create a test input with a variety of boolean expressions
        var l = new Lexer("class TestClass\n" +
                "\tmyMethod()\n" +
                "\t\tif a == b && c != d || e > f\n" +
                "\t\t\tg = h\n");
        var tokens = l.Lex();
        TranNode t = new TranNode();
        Parser p = new Parser(t, tokens);

        // Parse the input
        p.Tran();

        // Assertions to validate the structure of the parsed AST
        Assertions.assertEquals(1, t.Classes.size()); // Ensure one class was parsed
        var parsedClass = t.Classes.getFirst();
        Assertions.assertEquals("TestClass", parsedClass.name); // Ensure the class name matches

        Assertions.assertEquals(1, parsedClass.methods.size()); // Ensure one method was parsed
        var parsedMethod = parsedClass.methods.getFirst();
        Assertions.assertEquals("myMethod", parsedMethod.name); // Ensure the method name matches

        Assertions.assertEquals(1, parsedMethod.statements.size()); // Ensure one statement (the 'if') was parsed
        var ifStatement = parsedMethod.statements.getFirst();
        Assertions.assertInstanceOf(IfNode.class, ifStatement); // Ensure the statement is an 'if' node

        // Validate the condition of the 'if' statement
        var ifNode = (IfNode) ifStatement;

        // Ensure the top-level condition is an OR operation
        Assertions.assertInstanceOf(BooleanOpNode.class, ifNode.condition);
        var orCondition = (BooleanOpNode) ifNode.condition;
        Assertions.assertEquals(BooleanOpNode.BooleanOperations.or, orCondition.op);

        // Validate the left side of the OR operation
        Assertions.assertInstanceOf(BooleanOpNode.class, orCondition.left);
        var andCondition = (BooleanOpNode) orCondition.left;
        Assertions.assertEquals(BooleanOpNode.BooleanOperations.and, andCondition.op);

        // Validate the left side of the AND operation (a == b)
        Assertions.assertInstanceOf(CompareNode.class, andCondition.left);
        var compareLeft = (CompareNode) andCondition.left;
        Assertions.assertEquals(CompareNode.CompareOperations.eq, compareLeft.op);
        Assertions.assertEquals("a", compareLeft.left.toString());
        Assertions.assertEquals("b", compareLeft.right.toString());

        // Validate the right side of the AND operation (c != d)
        Assertions.assertInstanceOf(CompareNode.class, andCondition.right);
        var compareRight = (CompareNode) andCondition.right;
        Assertions.assertEquals(CompareNode.CompareOperations.ne, compareRight.op);
        Assertions.assertEquals("c", compareRight.left.toString());
        Assertions.assertEquals("d", compareRight.right.toString());

        // Validate the right side of the OR operation (e > f)
        Assertions.assertInstanceOf(CompareNode.class, orCondition.right);
        var greaterCondition = (CompareNode) orCondition.right;
        Assertions.assertEquals(CompareNode.CompareOperations.gt, greaterCondition.op);
        Assertions.assertEquals("e", greaterCondition.left.toString());
        Assertions.assertEquals("f", greaterCondition.right.toString());

        // Validate the statement inside the 'if' block (g = h)
        Assertions.assertEquals(1, ifNode.statements.size());
        var innerStatement = ifNode.statements.get(0);
        Assertions.assertInstanceOf(AssignmentNode.class, innerStatement);
        var assignment = (AssignmentNode) innerStatement;
        Assertions.assertEquals("g", assignment.target.name);
        Assertions.assertEquals("h", assignment.expression.toString());
    }
}