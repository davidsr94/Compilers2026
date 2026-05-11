import java.util.*;

// class representing a node in the AST
class ASTNode {
    private String type; // type of node
    private String value; // value
    private List<ASTNode> children; // list of child nodes

    // constructor to create a node
    public ASTNode(String type, String value) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }

    // adds a child node
    public void addChild(ASTNode child) {
        children.add(child);
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    // recursively print the AST structure
    public void print(String indent) {
        System.out.println(indent + "<" + type + "> " + (value != null ? "[" + value + "]" : ""));
        for (ASTNode child : children) {
            child.print(indent + "  ");
        }
    }
}

// AST Builder class
public class ASTBuilder {
    private List<Token> tokens;
    private int current;
    private int programNumber;

    public ASTBuilder(List<Token> tokens, int programNumber) {
        this.tokens = tokens;
        this.current = 0;
        this.programNumber = programNumber;
    }

    // builds AST for a program
    public ASTNode buildAST() {
        ASTNode program = new ASTNode("Program", null);
        program.addChild(parseBlock());
        return program;
    }

    // parses a block {}
    private ASTNode parseBlock() {
        // Accept multiple open braces
        while (check(Token.Type.PUNCTUATION, "{")) {
            advance();
        }

        ASTNode block = new ASTNode("Block", null);
        // parse statements until a closing brace is found
        while (!check(Token.Type.PUNCTUATION, "}") && !isAtEnd()) {
            block.addChild(parseStatement());
        }

        // hanndles closing braces
        while (check(Token.Type.PUNCTUATION, "}")) {
            advance();
        }

        return block;
    }

    // parses a single statement
    private ASTNode parseStatement() {
        Token token = peek();

        // variable declaration
        if (match(Token.Type.KEYWORD, "int") || match(Token.Type.KEYWORD, "string")
                || match(Token.Type.KEYWORD, "boolean")) {
            Token typeToken = previous();
            Token id = consume(Token.Type.IDENTIFIER, null);
            ASTNode decl = new ASTNode("Declaration", null);
            decl.addChild(new ASTNode("Type", typeToken.getValue()));
            decl.addChild(new ASTNode("Identifier", id.getValue()));
            return decl;
        } else if (check(Token.Type.IDENTIFIER, null)) {
            return parseAssignment();
        } else if (match(Token.Type.KEYWORD, "print")) {
            ASTNode print = new ASTNode("Print", null);
            consume(Token.Type.PUNCTUATION, "(");
            print.addChild(parseExpression());
            consume(Token.Type.PUNCTUATION, ")");
            return print;
        } else if (match(Token.Type.KEYWORD, "if")) {
            ASTNode ifNode = new ASTNode("If", null);
            consume(Token.Type.PUNCTUATION, "(");
            ifNode.addChild(parseExpression());
            consume(Token.Type.PUNCTUATION, ")");
            ifNode.addChild(parseBlock());
            return ifNode;
        } else if (match(Token.Type.KEYWORD, "while")) {
            ASTNode whileNode = new ASTNode("While", null);
            consume(Token.Type.PUNCTUATION, "(");
            whileNode.addChild(parseExpression());
            consume(Token.Type.PUNCTUATION, ")");
            whileNode.addChild(parseBlock());
            return whileNode;
        } else if (match(Token.Type.PUNCTUATION, "{")) {
            return parseBlock();
        } else {
            error("Unknown statement start: " + token.getValue());
            advance();
            return new ASTNode("Error", token.getValue());
        }
    }

    // parses assignment statement
    private ASTNode parseAssignment() {
        Token id = consume(Token.Type.IDENTIFIER, null);
        consume(Token.Type.OPERATOR, "=");
        ASTNode assign = new ASTNode("Assignment", null);
        assign.addChild(new ASTNode("Identifier", id.getValue()));
        assign.addChild(parseExpression());
        return assign;
    }

    // parses expression
    private ASTNode parseExpression() {
        ASTNode left = parsePrimary();
        while (check(Token.Type.OPERATOR, "==") || check(Token.Type.OPERATOR, "!=")
                || check(Token.Type.OPERATOR, "+")) {
            Token op = advance();
            ASTNode opNode = new ASTNode("Operator", op.getValue());
            opNode.addChild(left);
            opNode.addChild(parsePrimary());
            left = opNode;
        }
        return left;
    }

    // parses literals, identifiers, and parenthesis
    private ASTNode parsePrimary() {
        if (match(Token.Type.LITERAL, null)) {
            return new ASTNode("Literal", previous().getValue());
        } else if (match(Token.Type.IDENTIFIER, null)) {
            return new ASTNode("Identifier", previous().getValue());
        } else if (match(Token.Type.PUNCTUATION, "(")) {
            ASTNode expr = parseExpression();
            consume(Token.Type.PUNCTUATION, ")");
            return expr;
        } else {
            error("Invalid expression");
            return new ASTNode("Error", peek().getValue());
        }
    }

    // Helper methods
    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean check(Token.Type type, String value) {
        if (isAtEnd())
            return false;
        Token token = peek();
        return token.getType() == type && (value == null || token.getValue().equals(value));
    }

    private boolean match(Token.Type type, String value) {
        if (check(type, value)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(Token.Type type, String value) {
        if (check(type, value))
            return advance();
        error("Expected token: " + type + (value != null ? " " + value : ""));
        return new Token(Token.Type.EOF, "", -1, -1);
    }

    private void error(String message) {
        System.out.println("ERROR ASTBuilder - Program " + programNumber + ": " + message);
    }
}