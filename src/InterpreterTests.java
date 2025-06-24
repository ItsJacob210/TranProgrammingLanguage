import AST.BuiltInMethodDeclarationNode;
import AST.TranNode;
import Interpreter.Interpreter;
import Interpreter.ConsoleWrite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class InterpreterTests {
    @Test
    public void SimpleRun() {
        String program = """
                class SimpleRun
                    shared start()
                        number x
                        x = 6
                        console.write(x)
                """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("6.0",c.getFirst());
    }

    @Test
    public void SimpleRun2() {
        String program = """
                class SimpleRun
                    shared start()
                        number result
                        result = 5 * 10
                        console.write(result + 1)
                        console.write("hi")
                """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(2,c.size());
        Assertions.assertEquals("51.0",c.getFirst());
        Assertions.assertEquals("hi",c.getLast());
    }

    @Test
    public void SimpleAdd() {
        String program = """
                class SimpleAdd
                    shared start()
                        number x
                        number y
                        number z
                        x = 6
                        y = 6
                        z = x + y
                        console.write(z)
               """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void SimpleAddInstantiate() {
        String program = """
                class SimpleAdd
                    number x
                    number y
                    construct()
                        x = 6
                        y = 6
                    add()
                        number z
                        z = x + y
                        console.write(z)
                    shared start()
                        SimpleAdd t
                        t = new SimpleAdd()
                        t.add()
               """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void SimpleAddInstantiateAndPrint() {
        String program = """
                class SimpleAdd
                    number x
                    number y
                    construct()
                        x = 6
                        y = 6
                    add()
                        number z
                        z = x + y
                        console.write(z)
                    shared start()
                        SimpleAdd t
                        t = new SimpleAdd()
                        t.add()
               """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("12.0",c.getFirst());
    }

    @Test
    public void Loop1() {
        String program = """
                class LoopOne
                    shared start()
                        boolean keepGoing
                        number n
                        n = 0
                        keepGoing = true
                        loop keepGoing
                        	  if n >= 15
                                keepGoing = false
                            else
                                n = n + 1
                                console.write(n)
                """;
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(15,c.size());
        Assertions.assertEquals("1.0",c.getFirst());
        Assertions.assertEquals("15.0",c.getLast());
    }

    @Test
    public void student() {
        String program = "class student\n" +
                "    number gradea\n" +
                "    number gradeb\n" +
                "    number gradec\n" +
                "    string firstname\n" +
                "    string lastname\n" +
                "    construct (string fname, string lname, number ga, number gb, number gc)\n" +
                "        firstname = fname\n" +
                "        lastname = lname\n" +
                "        gradea = ga\n" +
                "        gradeb = gb\n" +
                "        gradec = gc\n" +
                "    getAverage() : number avg \n" +
                "        avg = (gradea + gradeb + gradec)/3\n" +
                "    print() \n" +
                "        console.write(firstname, \" \", lastname, \" \", getAverage())\n" +
                "    shared start()\n" +
                "        student sa\n" +
                "        student sb\n" +
                "        student sc\n" +
                "        sa = new student(\"michael\",\"phipps\",100,99,98)\n" +
                "        sb = new student(\"tom\",\"johnson\",80,75,83)\n" +
                "        sc = new student(\"bart\",\"simpson\",32,25,33)\n" +
                "        sa.print()\n" +
                "        sb.print()\n" +
                "        sc.print()\n";
        var tranNode = run(program);
        var c = getConsole(tranNode);
        Assertions.assertEquals(3,c.size());
        Assertions.assertEquals("michael phipps 99.0",c.getFirst());
        Assertions.assertEquals("bart simpson 30.0",c.getLast());
    }

    private static List<String> getConsole(TranNode tn) {
        for (var c : tn.Classes)
            if (c.name.equals("console")) {
                for (var m : c.methods)  {
                    if (m.name.equals("write")) {
                        return ((ConsoleWrite)m).console;
                    }
                }
            }
        throw new RuntimeException("Unable to find console");
    }

    private static TranNode run(String program) {
        var l  = new Lexer(program);
        try {
            var tokens = l.Lex();
            var tran = new TranNode();
            var p = new Parser(tran,tokens);
            p.Tran();
            System.out.println(tran.toString());
            var i = new Interpreter(tran);
            i.start();
            return tran;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
