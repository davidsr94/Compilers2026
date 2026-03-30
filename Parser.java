import java.util.*;

public class Parser {
    private List<Token> tokens; // list of tokens to be parsed
    private int current; // current token index
    private int programNumber; // prog number for reporting errors
    private StringBuilder cst; // CST
    private boolean hasSyntaxError = false; // Syntax errors

    // used to initialize parser w/ program and program number
    public Parser(List<Token> tokens, int programNumber) {
        this.tokens = tokens;
        this.current = 0;
        this.programNumber = programNumber;
    }

    // entry point for parsing
    public void parse() {
        cst = new StringBuilder();
        parseProgram();

        // checks for syntax errors and prints them
        if (hasSyntaxError) {
            System.out.println("ERROR: Parsing failed for Program " + programNumber + " due to syntax errors.\n");
        } else {
            System.out.println("PARSER: Parse completed successfully\n");
            System.out.println("CST for program " + programNumber + "...");
            System.out.println(cst.toString());
        }
    }

    // parse the program
    private void parseProgram() {
        cst.append("<Program>\n");
        parseBlock(1); // parse main block of the program

        // check for remaining tokens after prog ends
        if (!isAtEnd()) {
            reportSyntaxError("Unexpected token after program end.");
        }
    }

    // parse a code block that has {}
    private void parseBlock(int depth) {
        if (match(Token.Type.PUNCTUATION, "{", depth)) {
            cst.append(getIndent(depth) + "-<Block>\n");
            cst.append(getIndent(depth + 1) + "-[{]\n");
            parseStatementList(depth + 1); // parse list of statements indside the block

            // checks for closing brace
            if (!match(Token.Type.PUNCTUATION, "}", depth + 1)) {
                reportSyntaxError("Missing closing '}' for block.");
            } else {
                cst.append(getIndent(depth + 1) + "-[}]\n");
            }
        } else {
            reportSyntaxError("Expected '{' to start block.");
        }
    }

    // parse a list of statements
    private void parseStatementList(int depth) {
        cst.append(getIndent(depth) + "-<Statement List>\n");

        // check for various statement types and parse accordingly
        while (!check(Token.Type.PUNCTUATION, "}") && !check(Token.Type.EOP, null) && !isAtEnd()) {
            parseStatement(depth + 1);
        }
    }

    // parses individual statements IE declarations, assignments, and print
    // statements
    private void parseStatement(int depth) {
        cst.append(getIndent(depth) + "-<Statement>\n");

        if (check(Token.Type.KEYWORD, "int") || check(Token.Type.KEYWORD, "string")
                || check(Token.Type.KEYWORD, "boolean")) {
            parseDeclaration(depth + 1);
        } else if (check(Token.Type.PUNCTUATION, "{")) {
            parseBlock(depth + 1);
        } else if (check(Token.Type.IDENTIFIER, null)) {
            parseAssignment(depth + 1);
        } else if (check(Token.Type.KEYWORD, "print")) {
            parsePrintStatement(depth + 1);
        } else if (check(Token.Type.KEYWORD, "if")) {
            parseIfStatement(depth + 1);
        } else if (check(Token.Type.KEYWORD, "while")) {
            parseWhileStatement(depth + 1);
        } else {
            reportSyntaxError("Unexpected token: " + peek().getValue());
            advance();
        }
    }

    // parses an if statement
    private void parseIfStatement(int depth) {
        cst.append(getIndent(depth) + "-<If Statement>\n");
        match(Token.Type.KEYWORD, "if", depth + 1);
        match(Token.Type.PUNCTUATION, "(", depth + 1);
        parseExpression(depth + 1);
        match(Token.Type.PUNCTUATION, ")", depth + 1);
        parseBlock(depth + 1); // Ensures block is correctly handled
    }

    // parses a while statement
    private void parseWhileStatement(int depth) {
        cst.append(getIndent(depth) + "-<While Statement>\n");
        match(Token.Type.KEYWORD, "while", depth + 1);
        match(Token.Type.PUNCTUATION, "(", depth + 1);
        parseExpression(depth + 1);
        match(Token.Type.PUNCTUATION, ")", depth + 1);
        parseBlock(depth + 1);
    }

    // parses variable declaration statement
    private void parseDeclaration(int depth) {
        cst.append(getIndent(depth) + "-<Declaration>\n");

        // matches the variable type
        match(Token.Type.KEYWORD, null, depth + 1);
        if (!match(Token.Type.IDENTIFIER, null, depth + 1)) {
            reportSyntaxError("Expected single-letter identifier after type declaration.");
        }
    }

    // parses an assignment statement
    private void parseAssignment(int depth) {
        cst.append(getIndent(depth) + "-<Assignment>\n");

        // match the identifier and assignment operator
        if (!match(Token.Type.IDENTIFIER, null, depth + 1)) {
            reportSyntaxError("Invalid assignment target. Expected an identifier.");
            return;
        }

        if (!match(Token.Type.OPERATOR, "=", depth + 1)) {
            reportSyntaxError("Missing '=' in assignment.");
            return;
        }

        // parses the expression being assigned
        parseExpression(depth + 1);
    }

    // parses a print statement
    private void parsePrintStatement(int depth) {
        cst.append(getIndent(depth) + "-<Print Statement>\n");

        // match the print keyword and parses the expression
        match(Token.Type.KEYWORD, "print", depth + 1);
        match(Token.Type.PUNCTUATION, "(", depth + 1);
        parseExpression(depth + 1);
        match(Token.Type.PUNCTUATION, ")", depth + 1);
    }

    // parses the expression and handles different types
    private void parseExpression(int depth) {
        cst.append(getIndent(depth) + "-<Expression>\n");

        // Handle parentheses
        if (match(Token.Type.PUNCTUATION, "(", depth + 1)) {
            parseExpression(depth + 1);
            if (!match(Token.Type.PUNCTUATION, ")", depth + 1)) {
                reportSyntaxError("Expected closing parenthesis");
            }
            return;
        }

        // Handle literals and identifiers
        if (match(Token.Type.LITERAL, null, depth + 1) ||
                match(Token.Type.IDENTIFIER, null, depth + 1)) {
            // Handle operators that might follow
            while (check(Token.Type.OPERATOR, "==") ||
                    check(Token.Type.OPERATOR, "!=") ||
                    check(Token.Type.OPERATOR, "+") ||
                    check(Token.Type.OPERATOR, "=")) {
                match(Token.Type.OPERATOR, null, depth + 1);
                parseExpression(depth + 1);
            }
            return;
        }

        // Handle boolean literals
        if (match(Token.Type.LITERAL, "true", depth + 1) ||
                match(Token.Type.LITERAL, "false", depth + 1)) {
            return;
        }

        reportSyntaxError("Invalid expression");
    }

    /*
     * private void parseExpression(int depth) {
     * cst.append(getIndent(depth) + "-<Expression>\n");
     * 
     * if (check(Token.Type.LITERAL, null) || check(Token.Type.IDENTIFIER, null)) {
     * cst.append(getIndent(depth + 1) + "-<Term>\n");
     * advance();
     * 
     * // Handle additional operators in a single expression
     * while (check(Token.Type.OPERATOR, "+") || check(Token.Type.OPERATOR, "==")
     * || check(Token.Type.OPERATOR, "!=")) {
     * cst.append(getIndent(depth + 1) + "-<Operator>\n");
     * advance();
     * if (check(Token.Type.LITERAL, null) || check(Token.Type.IDENTIFIER, null)) {
     * cst.append(getIndent(depth + 1) + "-<Term>\n");
     * advance();
     * } else {
     * reportSyntaxError("Expected literal or identifier after operator.");
     * }
     * }
     * return;
     * }
     * 
     * // Handle nested expressions inside parentheses
     * if (match(Token.Type.PUNCTUATION, "(", depth + 1)) {
     * parseExpression(depth + 1);
     * if (!match(Token.Type.OPERATOR, "==", depth + 1) &&
     * !match(Token.Type.OPERATOR, "!=", depth + 1)) {
     * reportSyntaxError("Expected boolean operator (== or !=).");
     * }
     * parseExpression(depth + 1);
     * if (!match(Token.Type.PUNCTUATION, ")", depth + 1)) {
     * reportSyntaxError("Expected closing parenthesis for boolean expression.");
     * }
     * return;
     * }
     * 
     * if (match(Token.Type.LITERAL, "true", depth + 1) || match(Token.Type.LITERAL,
     * "false", depth + 1)) {
     * return;
     * }
     * 
     * reportSyntaxError("Unexpected expression.");
     * }
     */

    // matches the current token with the given type
    private boolean match(Token.Type type, String value, int depth) {
        if (check(type, value)) {
            cst.append(getIndent(depth) + "-[" + peek().getValue() + "]\n");
            advance();
            return true;
        }
        return false;
    }

    // checks if the current token matches the expected type and value
    private boolean check(Token.Type type, String value) {
        return !isAtEnd() && peek().getType() == type && (value == null || peek().getValue().equals(value));
    }

    // checks if the end of the token list is reached
    private boolean isAtEnd() {
        return current >= tokens.size() || peek().getType() == Token.Type.EOF;
    }

    // returns current token w/o moving forward
    private Token peek() {
        return tokens.get(current);
    }

    // advances to the next token and returns the current one
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return tokens.get(current - 1);
    }

    // used for CST printing
    private String getIndent(int depth) {
        return "-".repeat(depth);
    }

    // reports syntax errors
    private void reportSyntaxError(String message) {
        System.out.println("ERROR PARSER - Program " + programNumber + ": " + message);
        hasSyntaxError = true;
    }
}