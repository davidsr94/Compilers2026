/*README:
 * in order to compile:
 *      Javac Main.java Lexer.java Token.java Parser.java
 * in order to execute text file:
 *      java Main test.txt(any .txt)
 *
 * Description:
 * This class reads a text file and sends it to the Lexer and Parser class and then prints it to terminal
 */

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <filename>");
            return;
        }

        String filename = args[0];
        StringBuilder sourceCode = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceCode.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        int currentPos = 0;
        int programCounter = 1;
        String code = sourceCode.toString();

        while (currentPos < code.length()) {
            int eopIndex = code.indexOf('$', currentPos);
            if (eopIndex == -1) {
                break;
            }

            String program = code.substring(currentPos, eopIndex + 1); // Include the $
            System.out.println("\n--- Program " + programCounter + " ---");

            Lexer lexer = new Lexer(program, programCounter);
            List<Token> tokens = lexer.tokenize();

            if (lexer.getErrorCount() > 0) {
                System.out.println(
                        "ERROR: Lexing failed for Program " + programCounter + ". Parsing aborted for this program.\n");
            } else {
                System.out.println("PARSER: Parsing program " + programCounter + "...");
                Parser parser = new Parser(tokens, programCounter);
                parser.parse();
            }

            programCounter++;
            currentPos = eopIndex + 1;

        }

    }
}