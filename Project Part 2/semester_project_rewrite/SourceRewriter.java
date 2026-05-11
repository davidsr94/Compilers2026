import java.util.*;

public class SourceRewriter {
    public static class Result {
        public final String source;
        public final List<String> changes;
        public Result(String source, List<String> changes) { this.source = source; this.changes = changes; }
        public boolean changed() { return !changes.isEmpty(); }
    }

    public Result rewrite(String source, int programNumber) {
        List<String> changes = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') { inString = !inString; out.append(c); continue; }
            if (inString) {
                if (Character.isUpperCase(c)) {
                    char fixed = Character.toLowerCase(c);
                    out.append(fixed);
                    changes.add("Program " + programNumber + ": lowercased string character '" + c + "' to '" + fixed + "'.");
                } else if (Character.isLetter(c) || c == ' ') {
                    out.append(c);
                } else {
                    changes.add("Program " + programNumber + ": removed invalid string character '" + c + "'.");
                }
                continue;
            }
            if (Character.isUpperCase(c)) {
                char fixed = Character.toLowerCase(c);
                out.append(fixed);
                changes.add("Program " + programNumber + ": lowercased identifier/keyword character '" + c + "' to '" + fixed + "'.");
            } else if (c == '!' && (i + 1 >= source.length() || source.charAt(i + 1) != '=')) {
                out.append("!=");
                changes.add("Program " + programNumber + ": rewrote lone ! to != false-safe comparison marker.");
            } else {
                out.append(c);
            }
        }
        String fixed = out.toString();
        int balance = 0;
        for (int i = 0; i < fixed.length(); i++) {
            char c = fixed.charAt(i);
            if (c == '{') balance++;
            if (c == '}') balance--;
        }
        StringBuilder balanced = new StringBuilder(fixed);
        while (balance > 0) { balanced.insert(Math.max(0, balanced.lastIndexOf("$")), "}\n"); balance--; changes.add("Program " + programNumber + ": inserted missing closing brace before EOP."); }
        return new Result(balanced.toString(), changes);
    }
}
