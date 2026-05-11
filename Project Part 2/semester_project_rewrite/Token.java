/*
 * this class represents tokens that is identified by the lexer
 *
 */

public class Token {
    // Enum that defines the different types of tokens the lexer should be able to
    // identify
    public enum Type {
        KEYWORD, IDENTIFIER, LITERAL, OPERATOR, PUNCTUATION, COMMENT, WHITESPACE, EOF, EOP
    }

    Type type; // token type
    String value; // value of the token
    int line; // line number where token is found
    int column; // position of token in the line

    // creates token objects
    public Token(Type type, String value, int line, int column) {
        this.type = type; // category of the token
        this.value = value; // string value of the token
        this.line = line; // line where the token is found
        this.column = column; // column where the token starts
    }

    // returns the type of the token
    public Type getType() {
        return type;
    }

    // returns the value of the token
    public String getValue() {
        return value;
    }

    // returns the line number where the token is found
    public int getLine() {
        return line;
    }

    // returns column number where the token starts
    public int getColumn() {
        return column;
    }

    /*
     * returns a string representation of the token
     * Iden_type = identifier
     *
     */
    public String getFormattedType() {
        switch (type) {
            case PUNCTUATION:
                return value.equals("{") ? "OPEN_BLOCK" : value.equals("}") ? "CLOSE_BLOCK" : type.toString();
            case IDENTIFIER:
                return "Iden_TYPE"; // Identifier type
            case OPERATOR:
                if (value.equals("$")) {
                    return "EOP";
                } else {
                    return "OP";
                }
            case EOP:
                return "EOP";
            default:
                return type.toString();
        }
    }

    /*
     * string representation of the token
     */
    public String toString() {
        return "DEBUG Lexer - " + getFormattedType() + " [ " + value + " ] found at (" + line + ":" + column + ")";
    }
}