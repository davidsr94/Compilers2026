import java.util.*;

public class LLVMIRGenerator {
    private final ASTNode root;
    private final int programNumber;
    private final StringBuilder out = new StringBuilder();
    private int reg = 1;
    private final Map<String, String> vars = new HashMap<>();

    public LLVMIRGenerator(ASTNode root, int programNumber) { this.root = root; this.programNumber = programNumber; }

    public String generate() {
        out.setLength(0);
        out.append("; LLVM-like IR for Program ").append(programNumber).append("\n");
        out.append("declare i32 @printf(i8*, ...)\n");
        out.append("@.intfmt = private constant [4 x i8] c\"%d\\0A\\00\"\n");
        out.append("define i32 @main() {\n");
        for (ASTNode c : root.getChildren()) emit(c);
        out.append("  ret i32 0\n}\n");
        return out.toString();
    }

    private void emit(ASTNode n) {
        switch(n.getType()) {
            case "Program": case "Block": for(ASTNode c:n.getChildren()) emit(c); break;
            case "Declaration": String name=n.getChildren().get(1).getValue(); vars.put(name, "%"+name); out.append("  %").append(name).append(" = alloca ").append(type(n.getChildren().get(0).getValue())).append("\n"); break;
            case "Assignment": out.append("  store i32 ").append(value(n.getChildren().get(1))).append(", i32* ").append(vars.getOrDefault(n.getChildren().get(0).getValue(), "%"+n.getChildren().get(0).getValue())).append("\n"); break;
            case "Print": out.append("  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 ").append(value(n.getChildren().get(0))).append(")\n"); break;
        }
    }
    private String value(ASTNode n) { if("Literal".equals(n.getType()) && n.getValue().matches("\\d+")) return n.getValue(); if("Identifier".equals(n.getType())) { String r="%r"+(reg++); out.append("  ").append(r).append(" = load i32, i32* ").append(vars.getOrDefault(n.getValue(), "%"+n.getValue())).append("\n"); return r; } if("Operator".equals(n.getType()) && "+".equals(n.getValue())) { String r="%r"+(reg++); out.append("  ").append(r).append(" = add i32 ").append(value(n.getChildren().get(0))).append(", ").append(value(n.getChildren().get(1))).append("\n"); return r; } return "0"; }
    private String type(String t) { return "i32"; }
}
