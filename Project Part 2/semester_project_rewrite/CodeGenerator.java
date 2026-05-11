import java.util.*;

public class CodeGenerator {
    private ASTNode root;
    private int programNumber;

    // code bytes emitted left-to-right
    private List<String> code = new ArrayList<>();

    // scope stack
    private Deque<Map<String, StaticVar>> scopes = new ArrayDeque<>();

    // all static variables and temps that must be backpatched
    private List<StaticVar> staticTable = new ArrayList<>();

    // string heap contents written directly into final memory image
    private Map<Integer, String> heapBytes = new HashMap<>();
    private Map<String, Integer> stringTable = new LinkedHashMap<>();
    private int heapPtr = 0xFF;

    // counters
    private int tempCounter = 0;
    private int scratchCounter = 0;

    // reusable constant temps
    private StaticVar constZero = null;
    private StaticVar constOne = null;

    public CodeGenerator(ASTNode root, int programNumber) {
        this.root = root;
        this.programNumber = programNumber;
    }

    public void generate() {
        System.out.println("CODEGEN: Generating code for Program " + programNumber + "...");
        traverse(root);

        // halt
        emit("00");

        // backpatch statics
        Map<String, Integer> staticAddresses = backpatchStatics();

        // build final 256-byte memory image
        String[] memory = buildMemoryImage(staticAddresses);

        System.out.println("\nMachine Code Output:");
        System.out.println(String.join(" ", memory));
    }

    // =========================================================
    // Traversal
    // =========================================================

    private void traverse(ASTNode node) {
        switch (node.getType()) {
            case "Program":
            case "Block":
                enterScope();
                for (ASTNode child : node.getChildren()) {
                    traverse(child);
                }
                exitScope();
                break;

            case "Declaration": {
                String type = node.getChildren().get(0).getValue();
                String name = node.getChildren().get(1).getValue();
                System.out.println("CODEGEN: Declaring variable '" + name + "'");
                declareVariable(name, type);
                break;
            }

            case "Assignment": {
                String targetName = node.getChildren().get(0).getValue();
                ASTNode expr = node.getChildren().get(1);
                generateAssignment(targetName, expr);
                break;
            }

            case "Print":
                generatePrint(node.getChildren().get(0));
                break;

            case "If":
                generateIf(node);
                break;

            case "While":
                generateWhile(node);
                break;

            default:
                for (ASTNode child : node.getChildren()) {
                    traverse(child);
                }
                break;
        }
    }

    // =========================================================
    // Scope / static helpers
    // =========================================================

    private static class StaticVar {
        String name;
        String type;
        String tempLabel;
        int scopeId;

        StaticVar(String name, String type, String tempLabel, int scopeId) {
            this.name = name;
            this.type = type;
            this.tempLabel = tempLabel;
            this.scopeId = scopeId;
        }
    }

    private int currentScopeId = 0;
    private int nextScopeId = 0;

    private void enterScope() {
        scopes.push(new LinkedHashMap<>());
        currentScopeId = nextScopeId++;
    }

    private void exitScope() {
        scopes.pop();
        currentScopeId = scopes.isEmpty() ? 0 : currentScopeId;
    }

    private void declareVariable(String name, String type) {
        StaticVar var = new StaticVar(name, type, newTempLabel(), currentScopeId);
        scopes.peek().put(name, var);
        staticTable.add(var);

        // initialize declared variables to 0 / false / null pointer
        emit("A9", "00", "8D", var.tempLabel, "XX");
    }

    private StaticVar resolveVariable(String name) {
        for (Map<String, StaticVar> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        throw new RuntimeException("CODEGEN ERROR - Variable not found: " + name);
    }

    private StaticVar newScratchTemp(String type) {
        StaticVar temp = new StaticVar("_T" + scratchCounter, type, newTempLabel(), -1);
        scratchCounter++;
        staticTable.add(temp);
        return temp;
    }

    private String newTempLabel() {
        return "T" + (tempCounter++);
    }

    // =========================================================
    // Assignment
    // =========================================================

    private void generateAssignment(String targetName, ASTNode expr) {
        StaticVar target = resolveVariable(targetName);

        if (isIntExpression(expr)) {
            generateIntExpressionToAccumulator(expr);
            emit("8D", target.tempLabel, "XX");
            return;
        }

        if (isBooleanExpression(expr)) {
            generateBooleanExpressionToAccumulator(expr);
            emit("8D", target.tempLabel, "XX");
            return;
        }

        if (isStringExpression(expr)) {
            if (expr.getType().equals("Literal")) {
                int ptr = storeStringInHeap(expr.getValue());
                emit("A9", hex(ptr), "8D", target.tempLabel, "XX");
            } else if (expr.getType().equals("Identifier")) {
                StaticVar source = resolveVariable(expr.getValue());
                emit("AD", source.tempLabel, "XX", "8D", target.tempLabel, "XX");
            } else {
                throw new RuntimeException("CODEGEN ERROR - Unsupported string assignment.");
            }
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported assignment expression: " + expr.getType());
    }

    // =========================================================
    // Print
    // =========================================================

    private void generatePrint(ASTNode expr) {
        if (isIntExpression(expr)) {
            if (expr.getType().equals("Identifier")) {
                StaticVar var = resolveVariable(expr.getValue());
                emit("AC", var.tempLabel, "XX", "A2", "01", "FF");
            } else {
                StaticVar temp = newScratchTemp("int");
                generateIntExpressionToAccumulator(expr);
                emit("8D", temp.tempLabel, "XX");
                emit("AC", temp.tempLabel, "XX", "A2", "01", "FF");
            }
            return;
        }

        if (isStringExpression(expr)) {
            if (expr.getType().equals("Identifier")) {
                StaticVar var = resolveVariable(expr.getValue());
                emit("AC", var.tempLabel, "XX", "A2", "02", "FF");
            } else if (expr.getType().equals("Literal")) {
                int ptr = storeStringInHeap(expr.getValue());
                emit("A0", hex(ptr), "A2", "02", "FF");
            } else {
                throw new RuntimeException("CODEGEN ERROR - Unsupported string print expression.");
            }
            return;
        }

        if (isBooleanExpression(expr)) {
            StaticVar temp = newScratchTemp("boolean");
            generateBooleanExpressionToAccumulator(expr);
            emit("8D", temp.tempLabel, "XX");
            generateBooleanPrint(temp);
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported print expression.");
    }

    private void generateBooleanPrint(StaticVar boolVar) {
        int truePtr = storeStringInHeap("\"true\"");
        int falsePtr = storeStringInHeap("\"false\"");

        StaticVar one = getConstOne();

        emit("A2", "01");
        emit("EC", boolVar.tempLabel, "XX");
        emit("D0", "JFALSE");
        int falsePatch = code.size() - 1;

        emit("A0", hex(truePtr), "A2", "02", "FF");

        emit("D0", "JEND");
        int endPatch = code.size() - 1;

        int falseStart = code.size();
        patchJump(falsePatch, falseStart);

        emit("A0", hex(falsePtr), "A2", "02", "FF");

        int end = code.size();
        patchJump(endPatch, end);

        // keep constOne emitted so it exists in static area
        one.toString();
    }

    // =========================================================
    // If / While
    // =========================================================

    private void generateIf(ASTNode node) {
        ASTNode condition = node.getChildren().get(0);
        ASTNode block = node.getChildren().get(1);

        if (!condition.getType().equals("Operator")) {
            throw new RuntimeException("CODEGEN ERROR - If condition must be comparison.");
        }

        String op = condition.getValue();
        ASTNode left = condition.getChildren().get(0);
        ASTNode right = condition.getChildren().get(1);

        if (op.equals("==")) {
            generateDirectCompare(left, right);
            emit("D0", "JUMP");
            int skipPatch = code.size() - 1;

            traverse(block);

            int afterBlock = code.size();
            patchJump(skipPatch, afterBlock);
            return;
        }

        if (op.equals("!=")) {
            generateDirectCompare(left, right);

            // If not equal, skip over the unconditional jump sequence and execute block.
            emit("D0", "07");

            // Otherwise equal => unconditional jump over the block.
            emitUnconditionalBranchPlaceholder();
            int skipBlockPatch = code.size() - 1;

            traverse(block);

            int afterBlock = code.size();
            patchJump(skipBlockPatch, afterBlock);
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported if operator: " + op);
    }

    private void generateWhile(ASTNode node) {
        ASTNode condition = node.getChildren().get(0);
        ASTNode block = node.getChildren().get(1);

        if (!condition.getType().equals("Operator")) {
            throw new RuntimeException("CODEGEN ERROR - While condition must be comparison.");
        }

        String op = condition.getValue();
        ASTNode left = condition.getChildren().get(0);
        ASTNode right = condition.getChildren().get(1);

        int loopStart = code.size();

        if (op.equals("==")) {
            generateDirectCompare(left, right);
            emit("D0", "JEXIT");
            int exitPatch = code.size() - 1;

            traverse(block);

            emitUnconditionalBranch(loopStart);

            int afterLoop = code.size();
            patchJump(exitPatch, afterLoop);
            return;
        }

        if (op.equals("!=")) {
            generateDirectCompare(left, right);

            // If not equal, skip over the unconditional exit jump and run body.
            emit("D0", "07");

            // Equal => unconditional jump out of loop.
            emitUnconditionalBranchPlaceholder();
            int exitPatch = code.size() - 1;

            traverse(block);

            emitUnconditionalBranch(loopStart);

            int afterLoop = code.size();
            patchJump(exitPatch, afterLoop);
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported while operator: " + op);
    }

    // =========================================================
    // Direct compare generation
    // =========================================================

    private void generateDirectCompare(ASTNode left, ASTNode right) {
        // CPX compares X to a memory location, so:
        // 1) load X from right operand
        // 2) compare X to left operand in memory
        loadXFromOperand(right);
        compareXToOperand(left);
    }

    private void loadXFromOperand(ASTNode operand) {
        if (operand.getType().equals("Identifier")) {
            StaticVar var = resolveVariable(operand.getValue());
            emit("AE", var.tempLabel, "XX");
            return;
        }

        if (operand.getType().equals("Literal")) {
            String value = operand.getValue();

            if (value.equals("true")) {
                emit("A2", "01");
                return;
            }
            if (value.equals("false")) {
                emit("A2", "00");
                return;
            }
            if (value.startsWith("\"")) {
                emit("A2", hex(storeStringInHeap(value)));
                return;
            }

            emit("A2", hex(Integer.parseInt(value)));
            return;
        }

        if (operand.getType().equals("Operator") && operand.getValue().equals("+")) {
            StaticVar temp = newScratchTemp("int");
            generateIntExpressionToAccumulator(operand);
            emit("8D", temp.tempLabel, "XX");
            emit("AE", temp.tempLabel, "XX");
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported compare operand for X load.");
    }

    private void compareXToOperand(ASTNode operand) {
        if (operand.getType().equals("Identifier")) {
            StaticVar var = resolveVariable(operand.getValue());
            emit("EC", var.tempLabel, "XX");
            return;
        }

        if (operand.getType().equals("Literal")) {
            StaticVar temp;
            String value = operand.getValue();

            if (value.equals("true") || value.equals("false")) {
                temp = newScratchTemp("boolean");
                emit("A9", value.equals("true") ? "01" : "00", "8D", temp.tempLabel, "XX");
                emit("EC", temp.tempLabel, "XX");
                return;
            }

            if (value.startsWith("\"")) {
                temp = newScratchTemp("string");
                emit("A9", hex(storeStringInHeap(value)), "8D", temp.tempLabel, "XX");
                emit("EC", temp.tempLabel, "XX");
                return;
            }

            temp = newScratchTemp("int");
            emit("A9", hex(Integer.parseInt(value)), "8D", temp.tempLabel, "XX");
            emit("EC", temp.tempLabel, "XX");
            return;
        }

        if (operand.getType().equals("Operator") && operand.getValue().equals("+")) {
            StaticVar temp = newScratchTemp("int");
            generateIntExpressionToAccumulator(operand);
            emit("8D", temp.tempLabel, "XX");
            emit("EC", temp.tempLabel, "XX");
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported compare operand for EC.");
    }

    // =========================================================
    // Integer expressions
    // =========================================================

    private void generateIntExpressionToAccumulator(ASTNode expr) {
        if (expr.getType().equals("Literal")) {
            emit("A9", hex(Integer.parseInt(expr.getValue())));
            return;
        }

        if (expr.getType().equals("Identifier")) {
            StaticVar var = resolveVariable(expr.getValue());
            emit("AD", var.tempLabel, "XX");
            return;
        }

        if (expr.getType().equals("Operator") && expr.getValue().equals("+")) {
            ASTNode left = expr.getChildren().get(0);
            ASTNode right = expr.getChildren().get(1);

            StaticVar leftTemp = newScratchTemp("int");

            generateIntExpressionToAccumulator(left);
            emit("8D", leftTemp.tempLabel, "XX");

            generateIntExpressionToAccumulator(right);
            emit("6D", leftTemp.tempLabel, "XX");
            return;
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported int expression.");
    }

    // =========================================================
    // Boolean expressions
    // =========================================================

    private void generateBooleanExpressionToAccumulator(ASTNode expr) {
        if (expr.getType().equals("Literal")) {
            if (expr.getValue().equals("true")) {
                emit("A9", "01");
                return;
            }
            if (expr.getValue().equals("false")) {
                emit("A9", "00");
                return;
            }
        }

        if (expr.getType().equals("Identifier")) {
            StaticVar var = resolveVariable(expr.getValue());
            emit("AD", var.tempLabel, "XX");
            return;
        }

        if (expr.getType().equals("Operator")) {
            String op = expr.getValue();
            ASTNode left = expr.getChildren().get(0);
            ASTNode right = expr.getChildren().get(1);

            StaticVar resultTemp = newScratchTemp("boolean");

            if (op.equals("==")) {
                emit("A9", "00", "8D", resultTemp.tempLabel, "XX");
                generateDirectCompare(left, right);
                emit("D0", "JBOOL");
                int skipPatch = code.size() - 1;
                emit("A9", "01", "8D", resultTemp.tempLabel, "XX");
                int after = code.size();
                patchJump(skipPatch, after);
                emit("AD", resultTemp.tempLabel, "XX");
                return;
            }

            if (op.equals("!=")) {
                emit("A9", "01", "8D", resultTemp.tempLabel, "XX");
                generateDirectCompare(left, right);
                emit("D0", "JBOOLNE");
                int skipPatch = code.size() - 1;
                emit("A9", "00", "8D", resultTemp.tempLabel, "XX");
                int after = code.size();
                patchJump(skipPatch, after);
                emit("AD", resultTemp.tempLabel, "XX");
                return;
            }
        }

        throw new RuntimeException("CODEGEN ERROR - Unsupported boolean expression.");
    }

    // =========================================================
    // Type helpers
    // =========================================================

    private boolean isIntExpression(ASTNode node) {
        if (node.getType().equals("Identifier")) {
            return resolveVariable(node.getValue()).type.equals("int");
        }
        if (node.getType().equals("Literal")) {
            String v = node.getValue();
            return !v.equals("true") && !v.equals("false") && !v.startsWith("\"");
        }
        return node.getType().equals("Operator") && node.getValue().equals("+");
    }

    private boolean isStringExpression(ASTNode node) {
        if (node.getType().equals("Identifier")) {
            return resolveVariable(node.getValue()).type.equals("string");
        }
        return node.getType().equals("Literal") && node.getValue().startsWith("\"");
    }

    private boolean isBooleanExpression(ASTNode node) {
        if (node.getType().equals("Identifier")) {
            return resolveVariable(node.getValue()).type.equals("boolean");
        }
        if (node.getType().equals("Literal")) {
            return node.getValue().equals("true") || node.getValue().equals("false");
        }
        return node.getType().equals("Operator") &&
                (node.getValue().equals("==") || node.getValue().equals("!="));
    }

    // =========================================================
    // Constants
    // =========================================================

    private StaticVar getConstZero() {
        if (constZero == null) {
            constZero = newScratchTemp("int");
            emit("A9", "00", "8D", constZero.tempLabel, "XX");
        }
        return constZero;
    }

    private StaticVar getConstOne() {
        if (constOne == null) {
            constOne = newScratchTemp("int");
            emit("A9", "01", "8D", constOne.tempLabel, "XX");
        }
        return constOne;
    }

    // =========================================================
    // Heap strings
    // =========================================================

    private int storeStringInHeap(String literal) {
        if (stringTable.containsKey(literal)) {
            return stringTable.get(literal);
        }

        String raw = literal.substring(1, literal.length() - 1);
        int startAddr = heapPtr - raw.length();

        for (int i = 0; i < raw.length(); i++) {
            heapBytes.put(startAddr + i, hex((int) raw.charAt(i)));
        }
        heapBytes.put(startAddr + raw.length(), "00");

        stringTable.put(literal, startAddr);
        heapPtr = startAddr - 1;

        return startAddr;
    }

    // =========================================================
    // Unconditional branching
    // =========================================================

    private void emitUnconditionalBranchPlaceholder() {
        // Use X=00 and compare with constOne so Z=0, then D0 always branches.
        StaticVar one = getConstOne();
        emit("A2", "00");
        emit("EC", one.tempLabel, "XX");
        emit("D0", "JUMP");
    }

    private void emitUnconditionalBranch(int targetIndex) {
        StaticVar one = getConstOne();
        emit("A2", "00");
        emit("EC", one.tempLabel, "XX");
        emit("D0", "JUMP");
        int patchIndex = code.size() - 1;
        patchJump(patchIndex, targetIndex);
    }

    // =========================================================
    // Emit / patch
    // =========================================================

    private void emit(String... bytes) {
        code.addAll(Arrays.asList(bytes));
    }

    private void patchJump(int operandIndex, int targetIndex) {
        int offset = targetIndex - (operandIndex + 1);
        code.set(operandIndex, hex(offset));
    }

    private Map<String, Integer> backpatchStatics() {
        Map<String, Integer> addresses = new HashMap<>();

        int staticStart = code.size();
        int nextAddr = staticStart;

        for (StaticVar var : staticTable) {
            if (!addresses.containsKey(var.tempLabel)) {
                addresses.put(var.tempLabel, nextAddr++);
            }
        }

        return addresses;
    }

    private String[] buildMemoryImage(Map<String, Integer> staticAddresses) {
        String[] memory = new String[256];
        Arrays.fill(memory, "00");

        if (code.size() > 256) {
            throw new RuntimeException("CODEGEN ERROR - Program exceeds 256 bytes before static/heap placement.");
        }

        // patch code bytes into final memory
        for (int i = 0; i < code.size(); i++) {
            String token = code.get(i);

            if (staticAddresses.containsKey(token)) {
                memory[i] = hex(staticAddresses.get(token));
            } else if (token.equals("XX")) {
                memory[i] = "00";
            } else {
                memory[i] = token;
            }
        }

        // write heap bytes
        for (Map.Entry<Integer, String> entry : heapBytes.entrySet()) {
            memory[entry.getKey()] = entry.getValue();
        }

        // collision check
        int highestStatic = code.size() + staticAddresses.size() - 1;
        if (highestStatic >= heapPtr + 1) {
            throw new RuntimeException("CODEGEN ERROR - Static area collided with heap.");
        }

        return memory;
    }

    private String hex(int value) {
        return String.format("%02X", value & 0xFF);
    }
}