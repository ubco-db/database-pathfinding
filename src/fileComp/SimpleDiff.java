package fileComp;

import org.apache.commons.text.diff.CommandVisitor;
import org.apache.commons.text.diff.StringsComparator;

public class SimpleDiff {
    public static void main(String[] args) {
        // Create a diff comparator with two input strings
        StringsComparator comparator = new StringsComparator("Its all Binary.", "Its all fun.");
        // Initialize custom visitor and visit char by char // TODO: potentially change to token by token
        MyCommandsVisitor myCommandsVisitor = new MyCommandsVisitor();
        comparator.getScript().visit(myCommandsVisitor);
        System.out.println("FINAL DIFF = " + myCommandsVisitor.left + " | " + myCommandsVisitor.right);
    }
}

/**
 * Custom visitor
 */
class MyCommandsVisitor implements CommandVisitor<Character> {
    String left = "";
    String right = "";

    public void visitDeleteCommand(Character c) {
        // Character present in left but not right
        left = left + "{" + c + "}";
    }

    public void visitInsertCommand(Character c) {
        // Character present in right but not left
        right = right + "(" + c + ")";
    }

    public void visitKeepCommand(Character c) {
        // Character is present in both files
        left = left + c;
        right = right + c;
    }
}
