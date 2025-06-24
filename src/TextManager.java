public class TextManager {

    private final String text;
    private int position;

    public TextManager(String text) {
        this.position = 0;
        this.text = text;
    }
    public boolean isAtEnd() { //tracks if text is at end
        return position >= text.length();
    }
    public char getCharacter() { //returns next character
        if (isAtEnd()) {
            return '\0';
        }
        return text.charAt(position++);
    }
    public char peekCharacter() { //tracks next character
        if (isAtEnd()) {
            return '\0';
        } else return text.charAt(position);
    }
    public char peekNextCharacter() { //tracks next.next character for punctuation
        if (position + 1 >= text.length()) {
            return '\0';
        } return text.charAt(position + 1);
    }
}
