import java.util.*;

public class ASTOptimizer {
    private final List<String> changes = new ArrayList<>();
    public List<String> getChanges() { return changes; }

    public ASTNode optimize(ASTNode root) {
        changes.clear();
        ASTNode folded = fold(root);
        return eliminateDeadCode(propagateConstants(folded, new HashMap<>()));
    }

    private ASTNode fold(ASTNode node) {
        ASTNode copy = new ASTNode(node.getType(), node.getValue());
        for (ASTNode child : node.getChildren()) copy.addChild(fold(child));
        if ("Operator".equals(copy.getType()) && copy.getChildren().size() == 2) {
            ASTNode l = copy.getChildren().get(0), r = copy.getChildren().get(1);
            if ("Literal".equals(l.getType()) && "Literal".equals(r.getType())) {
                String op = copy.getValue();
                String a = l.getValue(), b = r.getValue();
                try {
                    if (op.equals("+") && isInt(a) && isInt(b)) {
                        changes.add("Constant folded " + a + " + " + b + ".");
                        return new ASTNode("Literal", String.valueOf(Integer.parseInt(a) + Integer.parseInt(b)));
                    }
                    if (op.equals("==")) { changes.add("Constant folded equality comparison."); return new ASTNode("Literal", String.valueOf(a.equals(b))); }
                    if (op.equals("!=")) { changes.add("Constant folded inequality comparison."); return new ASTNode("Literal", String.valueOf(!a.equals(b))); }
                } catch (NumberFormatException ignored) { }
            }
        }
        return copy;
    }

    private ASTNode propagateConstants(ASTNode node, Map<String, String> constants) {
        ASTNode copy = new ASTNode(node.getType(), node.getValue());
        if ("Identifier".equals(node.getType()) && constants.containsKey(node.getValue())) {
            changes.add("Constant propagated variable '" + node.getValue() + "'.");
            return new ASTNode("Literal", constants.get(node.getValue()));
        }
        if ("Assignment".equals(node.getType()) && node.getChildren().size() == 2) {
            String name = node.getChildren().get(0).getValue();
            ASTNode expr = propagateConstants(node.getChildren().get(1), constants);
            copy.addChild(new ASTNode("Identifier", name));
            copy.addChild(expr);
            if ("Literal".equals(expr.getType())) constants.put(name, expr.getValue()); else constants.remove(name);
            return copy;
        }
        if ("Block".equals(node.getType()) || "Program".equals(node.getType())) {
            Map<String, String> local = new HashMap<>(constants);
            for (ASTNode child : node.getChildren()) copy.addChild(propagateConstants(child, local));
            return copy;
        }
        for (ASTNode child : node.getChildren()) copy.addChild(propagateConstants(child, constants));
        return copy;
    }

    private ASTNode eliminateDeadCode(ASTNode node) {
        ASTNode copy = new ASTNode(node.getType(), node.getValue());
        if (("If".equals(node.getType()) || "While".equals(node.getType())) && node.getChildren().size() >= 2) {
            ASTNode cond = node.getChildren().get(0);
            if ("Literal".equals(cond.getType()) && "false".equals(cond.getValue())) {
                changes.add("Dead code eliminated false " + node.getType() + " block.");
                return new ASTNode("Block", null);
            }
            if ("While".equals(node.getType()) && "Literal".equals(cond.getType()) && "true".equals(cond.getValue())) {
                changes.add("Loop unrolling skipped for infinite while(true); preserved loop.");
            }
        }
        for (ASTNode child : node.getChildren()) copy.addChild(eliminateDeadCode(child));
        return copy;
    }

    private boolean isInt(String s) { return s.matches("\\d+"); }
}
