Semester Project Rewrite

What changed:
1. SourceRewriter.java
   - Applies safe source rewriting before lexing.
   - Lowercases capital letters and attempts simple brace repair.

2. ASTOptimizer.java
   - Adds constant folding.
   - Adds constant propagation.
   - Adds simple dead-code elimination for if(false) / while(false).
   - Notes when loop unrolling is not safe.

3. Main.java
   - Runs the compiler pipeline as:
     rewrite -> lexer -> parser/CST -> AST -> semantic analysis -> optimizer -> 6502 codegen -> Java/TypeScript/LLVM-like IR output.
   - Skips later phases if lexing/parsing/semantic analysis fails.

4. JavaCodeGenerator.java
   - Generates readable Java source from the optimized AST.

5. TypeScriptCodeGenerator.java
   - Generates readable TypeScript source from the optimized AST.

6. LLVMIRGenerator.java
   - Generates a simple LLVM-like IR file from the optimized AST.
   - This is a starter IR generator. Full JVM bytecode generation through LLVM tools still requires external LLVM/JVM tooling.

Compile:
  javac Main.java Lexer.java Token.java Parser.java SemanticAnalyzer.java CodeGenerator.java ASTBuilder.java ASTOptimizer.java SourceRewriter.java JavaCodeGenerator.java TypeScriptCodeGenerator.java LLVMIRGenerator.java

Run:
  java Main test.txt

Generated outputs per program:
  GeneratedProgramN.java
  GeneratedProgramN.ts
  GeneratedProgramN.ll
