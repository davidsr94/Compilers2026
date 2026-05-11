public class TypeScriptCodeGenerator {
    private final ASTNode root;
    private final int programNumber;
    private final StringBuilder out = new StringBuilder();
    public TypeScriptCodeGenerator(ASTNode root, int programNumber) { this.root = root; this.programNumber = programNumber; }
    public String generate() { out.setLength(0); out.append("// Generated TypeScript for Program ").append(programNumber).append("\n"); for (ASTNode c: root.getChildren()) emit(c,0); return out.toString(); }
    private void emit(ASTNode n, int d) { String ind="  ".repeat(d); switch(n.getType()) { case "Program": case "Block": out.append(ind).append("{\n"); for(ASTNode c:n.getChildren()) emit(c,d+1); out.append(ind).append("}\n"); break; case "Declaration": out.append(ind).append("let ").append(n.getChildren().get(1).getValue()).append(": ").append(type(n.getChildren().get(0).getValue())).append(" = ").append(def(n.getChildren().get(0).getValue())).append(";\n"); break; case "Assignment": out.append(ind).append(n.getChildren().get(0).getValue()).append(" = ").append(expr(n.getChildren().get(1))).append(";\n"); break; case "Print": out.append(ind).append("console.log(").append(expr(n.getChildren().get(0))).append(");\n"); break; case "If": out.append(ind).append("if (").append(expr(n.getChildren().get(0))).append(") "); emit(n.getChildren().get(1), d); break; case "While": out.append(ind).append("while (").append(expr(n.getChildren().get(0))).append(") "); emit(n.getChildren().get(1), d); break; } }
    private String expr(ASTNode n) { if("Literal".equals(n.getType())||"Identifier".equals(n.getType())) return n.getValue(); if("Operator".equals(n.getType())) return "("+expr(n.getChildren().get(0))+" "+n.getValue()+" "+expr(n.getChildren().get(1))+")"; return "undefined"; }
    private String type(String t){ return t.equals("int") ? "number" : t.equals("string") ? "string" : "boolean"; }
    private String def(String t){ return t.equals("int") ? "0" : t.equals("string") ? "\"\"" : "false"; }
}
