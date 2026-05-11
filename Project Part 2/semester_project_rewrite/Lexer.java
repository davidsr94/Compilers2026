/*
 * This class is responsible for tokenizing any input file that is given through the command line.
 *
 */

import java.util.*;
import java.util.regex.*;

public class Lexer {
    String sourceCode; // source code to be tokenized
    int curPos; // current position
    char curChar; // current character
    int line = 1; // tracks the current line
    int column = 1; // tracks the current column
    int errors = 0; // error count
    int programNumber; // program number
    boolean currentProgramHasErrors = false; // flags to check if the current program has errors

    // initialzes the lexer with the source code and program number
    public Lexer(String sourceCode, int programNumber) {
        this.sourceCode = sourceCode;
        this.curPos = 0;
        this.programNumber = programNumber;
        System.out.println("INFO LEXER - Lexing program " + programNumber + "..");
    }

    // tokenizes the entire input and returns a list of tokens
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        currentProgramHasErrors = false;

        // loops through the entire source code character by character
        while (curPos < sourceCode.length()) {
            curChar = sourceCode.charAt(curPos);

            // skips whitespace and handles new line
            if (Character.isWhitespace(curChar)) {
                if (curChar == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                curPos++;
                continue;
            }

            // reports error for uppercase characters
            if (Character.isUpperCase(curChar)) {
                System.out.println(
                        "ERROR LEXER - Capital Letter '" + curChar + "' found at (" + line + ":" + column + ")");
                currentProgramHasErrors = true;
                errors++;
                curPos++;
                column++;
                continue;
            }

            // handles single line comments
            if (sourceCode.startsWith("//", curPos)) {
                curPos = sourceCode.indexOf("\n", curPos);
                if (curPos == -1)
                    curPos = sourceCode.length();
                continue;
            }

            // handles multi line comments
            if (sourceCode.startsWith("/*", curPos)) {
                int endComment = sourceCode.indexOf("*/", curPos + 2);
                curPos = (endComment == -1) ? sourceCode.length() : endComment + 2;
                continue;
            }

            // generates next token
            Token token = nextToken();
            if (token != null && token.getType() != Token.Type.EOF) {
                if (token.getType() == Token.Type.EOP) {
                    System.out.println("DEBUG Lexer - EOP [" + token.getValue() + "] found at (" + token.getLine() + ":"
                            + token.getColumn() + ")");
                    System.out.println("INFO Lexer - Lex completed with " + errors + " errors");
                    break;
                } else if (token.getType() != Token.Type.COMMENT && token.getType() != Token.Type.WHITESPACE) {
                    System.out.println("DEBUG Lexer - " + token.getFormattedType() + " [ " + token.getValue()
                            + " ] found at (" + token.getLine() + ":" + token.getColumn() + ")");
                    tokens.add(token);
                }
            } else {
                curPos++;
                column++;
            }
        }

        return tokens;
    }

    // generates and returns the next token from source code
    public Token nextToken() {
        if (curPos >= sourceCode.length()) {
            return new Token(Token.Type.EOF, "", line, column);
        }

        curChar = sourceCode.charAt(curPos);

        // Handle End of Program Symbol ($)
        if (curChar == '$') {
            curPos++;
            column++;
            return new Token(Token.Type.EOP, "$", line, column);
        }

        // Handle Operators (Assignment and Boolean Ops)
        // Handle Operators (==, !=, =, +)
        if (curChar == '!' || curChar == '=') {
            if (curPos + 1 < sourceCode.length() && sourceCode.charAt(curPos + 1) == '=') {
                String doubleOp = "" + curChar + "=";
                curPos += 2;
                column += 2;
                return new Token(Token.Type.OPERATOR, doubleOp, line, column);
            } else {
                curPos++;
                column++;
                return new Token(Token.Type.OPERATOR, String.valueOf(curChar), line, column);
            }
        }

        // Handle Addition Operator '+'
        if (curChar == '+') {
            curPos++;
            column++;
            return new Token(Token.Type.OPERATOR, "+", line, column);
        }

        /*
         * if (sourceCode.startsWith("==", curPos)) {
         * curPos += 2;
         * column += 2;
         * return new Token(Token.Type.OPERATOR, "==", line, column);
         * }
         * 
         * if (sourceCode.startsWith("!=", curPos)) {
         * curPos += 2;
         * column += 2;
         * return new Token(Token.Type.OPERATOR, "!=", line, column);
         * }
         */

        // Handle Assignment Operator '='

        // Handle String Literals
        if (curChar == '"') {
            int start = curPos++;
            StringBuilder stringContent = new StringBuilder();

            while (curPos < sourceCode.length() && sourceCode.charAt(curPos) != '"') {
                char nextChar = sourceCode.charAt(curPos);

                if (Character.isUpperCase(nextChar)) {
                    System.out.println("ERROR LEXER - Capital Letter '" + nextChar +
                            "' found in string at (" + line + ":" + column + ")");
                    currentProgramHasErrors = true;
                    errors++;
                    return null;
                }
                if (!Character.isLetter(nextChar) && nextChar != ' ') {
                    System.out.println("ERROR LEXER - Invalid character in string at (" + line + ":" + column + ")");
                    currentProgramHasErrors = true;
                    errors++;
                    return null;
                }
                stringContent.append(nextChar);
                curPos++;
            }

            if (curPos < sourceCode.length()) {
                curPos++; // Consume closing "
                return new Token(Token.Type.LITERAL, "\"" + stringContent.toString() + "\"", line, column);
            } else {
                System.out.println("ERROR LEXER - Unclosed string literal found at (" + line + ":" + column + ")");
                currentProgramHasErrors = true;
                errors++;
                return null;
            }
        }

        // Handle Boolean Literals (true, false)
        if (sourceCode.startsWith("true", curPos) || sourceCode.startsWith("false", curPos)) {
            String boolValue = sourceCode.startsWith("true", curPos) ? "true" : "false";
            curPos += boolValue.length();
            column += boolValue.length();
            return new Token(Token.Type.LITERAL, boolValue, line, column);
        }

        // Handle Keywords (int, string, boolean, print, if, while, true, false)
        String[] keywords = { "int", "string", "boolean", "print", "if", "while", "true", "false" };
        for (String keyword : keywords) {
            if (sourceCode.startsWith(keyword, curPos) &&
                    (curPos + keyword.length() == sourceCode.length() ||
                            !Character.isLetterOrDigit(sourceCode.charAt(curPos + keyword.length())))) {
                curPos += keyword.length();
                column += keyword.length();
                return new Token(Token.Type.KEYWORD, keyword, line, column);
            }
        }

        // Handle Single-Character Identifiers (a-z)
        if (Character.isLowerCase(curChar)) {
            StringBuilder identifier = new StringBuilder();
            while (curPos < sourceCode.length() && Character.isLowerCase(curChar)) {
                identifier.append(curChar);
                curPos++;
                if (curPos < sourceCode.length()) {
                    curChar = sourceCode.charAt(curPos);
                }
            }
            column += identifier.length();
            return new Token(Token.Type.IDENTIFIER, identifier.toString(), line, column);
        }

        // Handle Numbers (digits)
        if (Character.isDigit(curChar)) {
            int startColumn = column;
            StringBuilder number = new StringBuilder();

            while (curPos < sourceCode.length() && Character.isDigit(sourceCode.charAt(curPos))) {
                number.append(sourceCode.charAt(curPos));
                curPos++;
                column++;
            }

            return new Token(Token.Type.LITERAL, number.toString(), line, startColumn);
        }

        // Handle Punctuation ( { } ( ) )
        if ("{}()".indexOf(curChar) != -1) {
            curPos++;
            column++;
            return new Token(Token.Type.PUNCTUATION, String.valueOf(curChar), line, column);
        }

        // Error Handling for Unknown Tokens
        System.out.println("ERROR LEXER - Unrecognized Token '" + curChar + "' found at (" + line + ":" + column + ")");
        currentProgramHasErrors = true;
        errors++;

        curPos++;
        column++;
        return new Token(Token.Type.EOF, "", line, column);
    }

    // returns the number of errors encountered
    public int getErrorCount() {
        return errors;
    }
}