import java.util.List;
import java.util.Optional;

public class TokenManager {
    private List<Token> tokens;
    private int position;
    private int lineNumber;
    private int columnNumber;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.lineNumber = 1;
        this.columnNumber = 1;
    }
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {

        return true;
    }
    public int getCurrentLine() {
        if (done()) {
            System.out.println("Token stream exhausted. Position: " + position + ", Total tokens: " + tokens.size());
            throw new IllegalStateException("No more tokens");
        }
        return tokens.get(position).getLineNumber();
    }
    public int getCurrentColumnNumber() {
        if (done()) {
            throw new IllegalStateException("No more tokens");
        }
        return tokens.get(position).getColumnNumber();
    }
    public boolean done() {
        return position >= tokens.size();
    }
    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        //System.out.println("Matching and removing token: " + t + ", Current token: " + (done() ? "None" : tokens.get(position)));
        if (!done() && tokens.get(position).getType() == t) {
            Token matchedToken = tokens.remove(position);
            //System.out.println("Token matched & removed: " + matchedToken);
            if (matchedToken.getType() == Token.TokenTypes.NEWLINE) {
                lineNumber++;
                columnNumber = 1;
            } else {
                columnNumber += matchedToken.getValue().length();
            }
            return Optional.of(matchedToken);
        }
        //System.out.println("No match for token: " + t);
        return Optional.empty();
    }
    public Optional<Token> peek(int i) {
        //System.out.println("Peeking token at offset " + i + ": " + (position + i < tokens.size() ? tokens.get(position + i) : "None"));
        int target = position + i;
        if (target >= 0 && target < tokens.size()) {
            return Optional.of(tokens.get(target));
       }
        return Optional.empty();
    }
}
