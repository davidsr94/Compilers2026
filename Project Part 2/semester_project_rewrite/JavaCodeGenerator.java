public class JavaCodeGenerator {
    private final ASTNode root;
    private final int programNumber;
    private final StringBuilder out = new StringBuilder();
    private int tempCounter = 0;

    public JavaCodeGenerator(ASTNode root, int programNumber) { this.root = root; this.programNumber = programNumber; }

    public String generate() {
        out.setLength(0);
        out.append("public class GeneratedProgram").append(programNumber).append(" {\n");
        out.append("  public static void main(String[] args) {\n");
        for (ASTNode child : root.getChildren()) emitNode(child, 2);
        out.append("  }\n}\n");
        return out.toString();
    }

    private void emitNode(ASTNode node, int depth) {
        String ind = "  ".repeat(depth);
        switch (node.getType()) {
            case "Program": case "Block":
                out.append(ind).append("{\n");
                for (ASTNode child : node.getChildren()) emitNode(child, depth + 1);
                out.append(ind).append("}\n");
                break;
            case "Declaration":
                out.append(ind).append(mapType(node.getChildren().get(0).getValue())).append(" ")
                   .append(node.getChildren().get(1).getValue()).append(" = ").append(defaultValue(node.getChildren().get(0).getValue())).append(";\n");
                break;
            case "Assignment":
                out.append(ind).append(node.getChildren().get(0).getValue()).append(" = ").append(expr(node.getChildren().get(1))).append(";\n");
                break;
            case "Print":
                out.append(ind).append("System.out.println(").append(expr(node.getChildren().get(0))).append(");\n");
                break;
            case "If":
                out.append(ind).append("if (").append(expr(node.getChildren().get(0))).append(") "); emitNode(node.getChildren().get(1), depth); break;
            case "While":
                out.append(ind).append("while (").append(expr(node.getChildren().get(0))).append(") "); emitNode(node.getChildren().get(1), depth); break;
        }
    }

    private String expr(ASTNode n) {
        if ("Literal".equals(n.getType())) return n.getValue();
        if ("Identifier".equals(n.getType())) return n.getValue();
        if ("Operator".equals(n.getType())) {
            String op = n.getValue();
            return "(" + expr(n.getChildren().get(0)) + " " + op + " " + expr(n.getChildren().get(1)) + ")";
        }
        return "/*unsupported" + (tempCounter++) + "*/0";
    }
    private String mapType(String t) { return t.equals("string") ? "String" : t; }
    private String defaultValue(String t) { return t.equals("string") ? "\"\"" : t.equals("boolean") ? "false" : "0"; }
}
