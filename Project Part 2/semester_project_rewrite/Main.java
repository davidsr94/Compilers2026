/*
 * Semester Project Driver
 * Compile:
 *   javac Main.java Lexer.java Token.java Parser.java SemanticAnalyzer.java CodeGenerator.java ASTBuilder.java ASTOptimizer.java SourceRewriter.java JavaCodeGenerator.java TypeScriptCodeGenerator.java LLVMIRGenerator.java
 * Run:
 *   java Main test.txt
 */

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <filename>");
            return;
        }

        String code;
        try {
            code = Files.readString(Paths.get(args[0]));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        int currentPos = 0;
        int programCounter = 1;
        while (currentPos < code.length()) {
            int eopIndex = code.indexOf('$', currentPos);
            if (eopIndex == -1) break;

            String program = code.substring(currentPos, eopIndex + 1);
            System.out.println("\n--- Program " + programCounter + " ---");

            SourceRewriter rewriter = new SourceRewriter();
            SourceRewriter.Result rewriteResult = rewriter.rewrite(program, programCounter);
            if (rewriteResult.changed()) {
                System.out.println("\nREWRITER: Safe source rewrites applied:");
                for (String change : rewriteResult.changes) System.out.println("REWRITER: " + change);
            }

            Lexer lexer = new Lexer(rewriteResult.source, programCounter);
            List<Token> tokens = lexer.tokenize();
            if (lexer.getErrorCount() > 0) {
                System.out.println("ERROR: Lexing failed for Program " + programCounter + ". Parsing aborted for this program.\n");
                programCounter++;
                currentPos = eopIndex + 1;
                continue;
            }

            Parser parser = new Parser(tokens, programCounter);
            parser.parse();
            if (parser.hasSyntaxError()) {
                System.out.println("ERROR: AST, semantic analysis, optimization, and code generation skipped for Program " + programCounter + ".\n");
                programCounter++;
                currentPos = eopIndex + 1;
                continue;
            }

            System.out.println("AST for program " + programCounter + "...");
            ASTBuilder astBuilder = new ASTBuilder(tokens, programCounter);
            ASTNode ast = astBuilder.buildAST();
            ast.print("");

            SemanticAnalyzer analyzer = new SemanticAnalyzer(ast, programCounter);
            analyzer.analyze();
            if (analyzer.hasErrors()) {
                System.out.println("CODEGEN: Skipped because semantic analysis failed.");
                programCounter++;
                currentPos = eopIndex + 1;
                continue;
            }

            ASTOptimizer optimizer = new ASTOptimizer();
            ASTNode optimizedAst = optimizer.optimize(ast);
            System.out.println("\nOPTIMIZER: Changes for Program " + programCounter + ":");
            if (optimizer.getChanges().isEmpty()) {
                System.out.println("OPTIMIZER: No safe optimizations found.");
            } else {
                for (String change : optimizer.getChanges()) System.out.println("OPTIMIZER: " + change);
            }
            System.out.println("Optimized AST for program " + programCounter + "...");
            optimizedAst.print("");

            CodeGenerator generator = new CodeGenerator(optimizedAst, programCounter);
            generator.generate();

            writeGeneratedFile("GeneratedProgram" + programCounter + ".java", new JavaCodeGenerator(optimizedAst, programCounter).generate());
            writeGeneratedFile("GeneratedProgram" + programCounter + ".ts", new TypeScriptCodeGenerator(optimizedAst, programCounter).generate());
            writeGeneratedFile("GeneratedProgram" + programCounter + ".ll", new LLVMIRGenerator(optimizedAst, programCounter).generate());

            programCounter++;
            currentPos = eopIndex + 1;
        }
    }

    private static void writeGeneratedFile(String filename, String content) {
        try {
            Files.writeString(Paths.get(filename), content);
            System.out.println("OUTPUT: Wrote " + filename);
        } catch (IOException e) {
            System.out.println("OUTPUT ERROR: Could not write " + filename + ": " + e.getMessage());
        }
    }
}
