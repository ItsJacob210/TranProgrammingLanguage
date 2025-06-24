import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Lexer1Tests {

    @Test
    public void SimpleLexerTest() {
        var l = new Lexer("ab cd ef gh");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            for (var result : res)
                Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest() {
        var l = new Lexer("ab cd ef gh\nasdjkdsajkl\ndsajkdsa asdjksald dsajhkl\n");
        try {
            var res = l.Lex();
            Assertions.assertEquals(11, res.size());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals("gh", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals("asdjkdsajkl", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals("dsajkdsa", res.get(7).getValue());
            Assertions.assertEquals("asdjksald", res.get(8).getValue());
            Assertions.assertEquals("dsajhkl", res.get(9).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(10).getType());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void NotEqualsTest() {
        var l = new Lexer("!=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void ANDORTest() {
        var l = new Lexer("&& ||");
        try {
            var res = l.Lex();
            Assertions.assertEquals(2, res.size());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "    if n >= 15\n" +
                        "        keepGoing = false\n" +
                        "1");
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }
    @Test
    public void IndentTest2() {
        var l = new Lexer(
                "loop myLoop\n" +
                        "    if n == 10\n" +
                        "        keepGoing = false\n" +
                        "Hi, this is new line"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(21, res.size());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }


    @Test
    public void TwoCharacterTest() {
        var l = new Lexer(">= > <= < = == !=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(7, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHAN, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHAN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(6).getType());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void MixedTest() {
        var l = new Lexer("word 1.2 : ( )");
        try {
            var res = l.Lex();
            Assertions.assertEquals(5, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("word", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(1).getType());
            Assertions.assertEquals("1.2", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(4).getType());
        } catch (Exception e) {
            Assertions.fail("exception occurred: " + e.getMessage());
        }
    }
}
