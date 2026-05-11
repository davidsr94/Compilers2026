import java.util.*;

//performs semantic analysis on the AST + builds symbol table
public class SemanticAnalyzer {
    private ASTNode root;
    private int currentScope = 0;
    private int scopeCounter = 0;
    private int programNumber;

    // stack of symbol tables
    private Deque<Map<String, Symbol>> scopeStack = new ArrayDeque<>();

    // keeps track of all scopes
    private Map<Integer, Map<String, Symbol>> allScopes = new HashMap<>();

    private boolean hasErrors = false;

    public SemanticAnalyzer(ASTNode root, int programNumber) {
        this.root = root;
        this.programNumber = programNumber;
    }

    // entry point for semantic analysis
    public void analyze() {
        enterScope();
        analyzeNode(root);
        exitScope();

        if (!hasErrors) {
            System.out.println("\nSEMANTIC ANALYSIS: Passed with no errors.");
            printSymbolTables();
        } else {
            System.out.println("\nSEMANTIC ANALYSIS: Errors found. No symbol table printed");
        }
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    // recursive AST traversal
    private void analyzeNode(ASTNode node) {
        switch (node.getType()) {
            case "Block":
                enterScope();
                for (ASTNode child : node.getChildren()) {
                    analyzeNode(child);
                }
                exitScope();
                break;

            case "Declaration":
                String varType = node.getChildren().get(0).getValue();
                String varName = node.getChildren().get(1).getValue();
                declareVariable(varName, varType);
                break;

            case "Assignment":
                String assignVar = node.getChildren().get(0).getValue();
                ASTNode valueExpr = node.getChildren().get(1);
                String declaredType = resolveVariable(assignVar);
                String valueType = evaluateExpression(valueExpr);

                Symbol sym = getSymbol(assignVar);
                if (sym != null) {
                    sym.isInitialized = true;
                }

                if (declaredType != null && valueType != null && !declaredType.equals(valueType)) {
                    error("Type mismatch: Cannot assign " + valueType + " to " + declaredType + " [" + assignVar + "]");
                }
                break;

            case "Print":
                evaluateExpression(node.getChildren().get(0));
                break;

            case "If":
            case "While":
                String conditionType = evaluateExpression(node.getChildren().get(0));
                if (!"boolean".equals(conditionType)) {
                    error(node.getType() + " condition must be boolean");
                }
                analyzeNode(node.getChildren().get(1)); // Block
                break;

            default:
                for (ASTNode child : node.getChildren()) {
                    analyzeNode(child);
                }
        }
    }

    // recursively evaluates the type of an expression
    private String evaluateExpression(ASTNode node) {
        switch (node.getType()) {
            case "Literal":
                String val = node.getValue();
                if (val.equals("true") || val.equals("false"))
                    return "boolean";
                if (val.startsWith("\""))
                    return "string";
                return "int";

            case "Identifier":
                return resolveVariable(node.getValue());

            case "Operator":
                String op = node.getValue();
                String left = evaluateExpression(node.getChildren().get(0));
                String right = evaluateExpression(node.getChildren().get(1));

                if (op.equals("+")) {
                    if ("int".equals(left) && "int".equals(right))
                        return "int";
                    if ("string".equals(left) || "string".equals(right))
                        return "string";
                    error("Invalid operands for '+': " + left + ", " + right);
                    return null;
                } else if (op.equals("==") || op.equals("!=")) {
                    if (left != null && left.equals(right))
                        return "boolean";
                    error("Incompatible types for comparison: " + left + ", " + right);
                    return null;
                }
                error("Unknown operator: " + op);
                return null;

            default:
                error("Invalid expression node: " + node.getType());
                return null;
        }
    }

    // declares a new variable in the current scope
    private void declareVariable(String name, String type) {
        Map<String, Symbol> currentTable = scopeStack.peek();
        if (currentTable.containsKey(name)) {
            error("Variable '" + name + "' already declared in current scope");
        } else {
            Symbol sym = new Symbol(name, type, currentScope);
            currentTable.put(name, sym);
        }
    }

    // searches for variable in all scopes and returns the type
    private String resolveVariable(String name) {
        for (Map<String, Symbol> table : scopeStack) {
            if (table.containsKey(name)) {
                Symbol sym = table.get(name);
                sym.isUsed = true;
                return sym.type;
            }
        }
        error("Variable '" + name + "' not declared in accessible scope");
        return null;
    }

    // gets symbol from the stack
    private Symbol getSymbol(String name) {
        for (Map<String, Symbol> table : scopeStack) {
            if (table.containsKey(name)) {
                return table.get(name);
            }
        }
        return null;
    }

    private void enterScope() {
        Map<String, Symbol> newScope = new HashMap<>();
        scopeStack.push(newScope);
        allScopes.put(scopeCounter, newScope);
        currentScope = scopeCounter;
        scopeCounter++;
    }

    private void exitScope() {
        scopeStack.pop();
        currentScope = scopeStack.isEmpty() ? 0 : currentScope - 1;
    }

    private void error(String message) {
        System.out.println("SEMANTIC ERROR - " + message);
        hasErrors = true;
    }

    // prints all symbol tables per scope
    private void printSymbolTables() {
        System.out.println("\nSYMB: Printing Symbol Table for Program " + programNumber + ":");
        System.out.println("NAME TYPE isINIT? isUSED? SCOPE");
        for (Map.Entry<Integer, Map<String, Symbol>> entry : allScopes.entrySet()) {
            for (Symbol sym : entry.getValue().values()) {
                System.out.println(sym.toString());
            }
        }
    }

    // inner class to represent symbol
    private static class Symbol {
        String name;
        String type;
        boolean isInitialized = false;
        boolean isUsed = false;
        int scope;

        public Symbol(String name, String type, int scope) {
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "[" + name + " " + type + " " + isInitialized + " " + isUsed + " " + scope + "]";
        }
    }
}