import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private final TextManager text;
    private static final HashMap<Character, Token.TokenTypes> puncMap = new HashMap<>();
    private static final HashMap<String, Token.TokenTypes> doublepuncMap = new HashMap<>();
    private static final HashMap<String, Token.TokenTypes> keyMap = new HashMap<>();
    static {

        //accepted punctuation
        puncMap.put('=', Token.TokenTypes.ASSIGN);
        puncMap.put('(', Token.TokenTypes.LPAREN);
        puncMap.put(')', Token.TokenTypes.RPAREN);
        puncMap.put('+', Token.TokenTypes.PLUS);
        puncMap.put('-', Token.TokenTypes.MINUS);
        puncMap.put('*', Token.TokenTypes.TIMES);
        puncMap.put('/', Token.TokenTypes.DIVIDE);
        puncMap.put('%', Token.TokenTypes.MODULO);
        puncMap.put('.', Token.TokenTypes.DOT);
        puncMap.put(',', Token.TokenTypes.COMMA);
        puncMap.put(':', Token.TokenTypes.COLON);
        puncMap.put('<', Token.TokenTypes.LESSTHAN);
        puncMap.put('>', Token.TokenTypes.GREATERTHAN);
        puncMap.put('!', Token.TokenTypes.NOT);

        //accepted punctuation with two char
        doublepuncMap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        doublepuncMap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        doublepuncMap.put("==", Token.TokenTypes.EQUAL);
        doublepuncMap.put("!=", Token.TokenTypes.NOTEQUAL);
        doublepuncMap.put("&&", Token.TokenTypes.AND);
        doublepuncMap.put("||", Token.TokenTypes.OR);

        //accepted keywords
        keyMap.put("accessor", Token.TokenTypes.ACCESSOR);
        keyMap.put("mutator", Token.TokenTypes.MUTATOR);
        keyMap.put("implements", Token.TokenTypes.IMPLEMENTS);
        keyMap.put("class", Token.TokenTypes.CLASS);
        keyMap.put("interface", Token.TokenTypes.INTERFACE);
        keyMap.put("loop", Token.TokenTypes.LOOP);
        keyMap.put("if", Token.TokenTypes.IF);
        keyMap.put("else", Token.TokenTypes.ELSE);
        keyMap.put("true", Token.TokenTypes.TRUE);
        keyMap.put("false", Token.TokenTypes.FALSE);
        keyMap.put("new", Token.TokenTypes.NEW);
        keyMap.put("shared", Token.TokenTypes.SHARED);
        keyMap.put("private", Token.TokenTypes.PRIVATE);
        keyMap.put("construct", Token.TokenTypes.CONSTRUCT);
    }

    private int lineNumber = 0; //tracks line number (y)
    private int columnNumber = 0; //tracks column number (x)
    private int previousIndent = 0; //tracks indents for dedents

    public Lexer(String input) {
        this.text = new TextManager(input);
    }

    public List<Token> Lex() throws SyntaxErrorException { //cycles through all methods to tokenize text
        var tokens = new LinkedList<Token>();
        while (!text.isAtEnd()) {
            char c = text.peekCharacter();
            if (Character.isLetter(c)) { //words
                tokens.add(parseWord());
            } else if (Character.isDigit(c)) { //numbers
                tokens.add(parseNumber());
            } else if ((puncMap.containsKey(c)) || c == '&' || c == '|') { //punctuation
                tokens.add(parsePunctuation());
            } else if (c == '\'') { //single character quotes
                tokens.add(parseQuotedChar());
            } else if (c== '"') { //quoted strings
                tokens.add(parseQuotedString());
            } else if (c == '\n') { //newlines, indents & dedents
                tokens.add(handleNewline());
                Token indent = handleIndentation();
                if (indent != null) {
                    tokens.add(indent);
                }
            }else if (c == '{') { //comments
                comment();
            } else if (Character.isWhitespace(c)) { //spaces
                text.getCharacter();
                columnNumber++;
            }else { //throws error for unknown characters
                throw new SyntaxErrorException("Unknown char", lineNumber, columnNumber);
            }
        }
        return tokens; //returns tokens collected
    }

    private Token handleNewline() { //handles newlines '\n'
        text.getCharacter();
        lineNumber++;
        columnNumber = 0;
        System.out.println("newline"); //tracking
        return new Token(Token.TokenTypes.NEWLINE, lineNumber, columnNumber); //newline token
    }

    private Token handleIndentation() {
        int indent = 0;
        int space = 0;

        while (!text.isAtEnd()) {
            char c = text.peekCharacter();
            if (c == '\t') { //tab == new indentation
                indent++;
                text.getCharacter();
                columnNumber++;
            }else if (c == ' ') {
                space++;
                if (space == 4) { //4 spaces == new indentation
                    indent++;
                    space = 0;
                }
                text.getCharacter();
                columnNumber++;
            } else {
                break;
            }
        }
        if (indent > previousIndent) { //creates indent token if current indent level > previous level
            previousIndent = indent;
            System.out.println("indent"); //tracking
            return new Token(Token.TokenTypes.INDENT, lineNumber, columnNumber); //indent token
        }
        else if (indent < previousIndent) { //creates dedent token if current indent level < previous level & is not origin
            if (previousIndent != 0) {
                previousIndent = indent;
                System.out.println("dedent"); //tracking
                return new Token(Token.TokenTypes.DEDENT, lineNumber, columnNumber); //dedent token
            }
        }
        return null;
    }

    private Token parseWord() {
        StringBuilder word = new StringBuilder();
        while (!text.isAtEnd() && Character.isLetter(text.peekCharacter())) {
            word.append(text.getCharacter());
            columnNumber++;
        }
        if (keyMap.containsKey(word.toString())) { //determines if string is a keyword by comparing it to key hash map
            Token.TokenTypes type = keyMap.get(word.toString());
            System.out.println("key:" + type); //tracking
            return new Token(type, lineNumber, columnNumber - word.length(), word.toString()); //keyword token
        }
        System.out.println("word:" + word); //tracking
        return new Token(Token.TokenTypes.WORD, lineNumber, columnNumber - word.length(), word.toString()); //word token
    }

    private Token parseNumber() {
        StringBuilder number = new StringBuilder();
        while (!text.isAtEnd() && Character.isDigit(text.peekCharacter()) || text.peekCharacter() == '.') {
            number.append(text.getCharacter());
            columnNumber++;
        }
        System.out.println("number:" + number); //tracking
        return new Token(Token.TokenTypes.NUMBER, lineNumber,columnNumber - number.length(), number.toString()); //number token
    }

    private Token parsePunctuation() {
        if (text.isAtEnd()) {
            return null;
        }
        char first = text.peekCharacter();
        if (!text.isAtEnd() && text.peekNextCharacter() != '\0') { //reads two characters and returns as double punctuation if found in "double punc" hash map
            String doubleChar = "" + first + text.peekNextCharacter();
            Token.TokenTypes type = doublepuncMap.get(doubleChar);
            if (type != null) {
                text.getCharacter();
                text.getCharacter();
                columnNumber += 2;
                System.out.println("double punc:" + doubleChar); //tracking
                return new Token(type, lineNumber, columnNumber - 2, doubleChar); //double punctuation token
            }
        }
        Token.TokenTypes type = puncMap.get(first);
        if (type != null) { //reads first character and returns as punctuation if it's found in "punc" hash map
            text.getCharacter();
            columnNumber++;
            System.out.println("punc:" + type); //tracking
            return new Token(type, lineNumber, columnNumber - 1, String.valueOf(first)); //single punctuation token
        }
        return null;
    }

    private Token parseQuotedChar() throws SyntaxErrorException { //handles single characters in quotes
        text.getCharacter();
        if (text.isAtEnd() || text.getCharacter() != '\'') { //throws error if quote isn't ended
            throw new SyntaxErrorException("Quote not ended", lineNumber, columnNumber);
        }
        char c = text.getCharacter();
        columnNumber +=3; //2 for ' 1 for character
        System.out.println("quotedc:" + c); //tracking
        return new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, columnNumber - 3, String.valueOf(c)); //quoted character token
    }

    private Token parseQuotedString() throws SyntaxErrorException { //handles strings in quotes
        StringBuilder string = new StringBuilder();
        text.getCharacter();
        while (!text.isAtEnd()) {
            char c = text.getCharacter();
            if (c == '"') {
                columnNumber += string.length() + 2;
                System.out.println("quoteds"); //tracking
                return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, columnNumber - string.length() - 2, string.toString()); //quoted string token
            }
            string.append(c);
        }
        throw new SyntaxErrorException("Quote not ended", lineNumber, columnNumber); //throws error if quote isn't ended.
    }

    private void comment() throws SyntaxErrorException { //handles comments '{}'
        text.getCharacter();
        columnNumber++;
        int braceCount = 1; //tracks amount of braces '{' used (starts at one)
        while (!text.isAtEnd()) {
            char c = text.getCharacter();
            columnNumber++;
            if (c == '{') {
                braceCount++;
            }else if (c == '}') {
                braceCount--;
                if (braceCount == 0) { //only return a comment if braces are closed
                    System.out.println("comment");
                    return;
                }
            }
            if (text.isAtEnd() && braceCount > 0) { //throws error if braces are never closed
                throw new SyntaxErrorException("Comment not ended", lineNumber, columnNumber);
            }
        }
    }

}
