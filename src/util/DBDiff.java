package util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.text.diff.CommandVisitor;
import org.apache.commons.text.diff.StringsComparator;

import java.io.File;
import java.io.IOException;

public class DBDiff {

    public static void getDBDiff(String path) throws IOException {
        // Read both files with iterator
        LineIterator file1 = FileUtils.lineIterator(new File(path + "BW012.map_DBA-STAR_G16_N1_C250.dat"),"utf-8");
        LineIterator file2 = FileUtils.lineIterator(new File(path + "AW012.map_DBA-STAR_G16_N1_C250.dat"), "utf-8");

        // Initialize visitor
        FileCommandsVisitor fileCommandsVisitor = new FileCommandsVisitor(path);

        // Read line by line for line by line comparison
        while (file1.hasNext() || file2.hasNext()) {
            /*
             * In case both files have different number of lines, fill in with empty
             * strings. Also append newline char at end so next line comparison moves to
             * next line.
             */
            String left = (file1.hasNext() ? file1.nextLine():  "") + "\n";
            String right = (file2.hasNext() ? file2.nextLine() : "") + "\n";

            // Prepare diff comparator with lines from both files
            StringsComparator comparator = new StringsComparator(left, right);

            if (comparator.getScript().getLCSLength() > (Integer.max(left.length(), right.length()) * 0.4)) {
                /*
                 * If both lines have at least 40% commonality then only compare with each other
                 * so that they are aligned with each other in final diff HTML.
                 */
                comparator.getScript().visit(fileCommandsVisitor);
            } else {
                /*
                 * If both lines do not have 40% commonality then compare each with empty line so
                 * that they are not aligned to each other in final diff instead they show up on
                 * separate lines.
                 */
                StringsComparator leftComparator = new StringsComparator(left, "\n");
                leftComparator.getScript().visit(fileCommandsVisitor);
                StringsComparator rightComparator = new StringsComparator("\n", right);
                rightComparator.getScript().visit(fileCommandsVisitor);
            }
        }
        file1.close();
        file2.close();
        fileCommandsVisitor.generateHTML();
    }
}

/*
 * Custom visitor for file comparison which stores comparison & also generates
 * HTML in the end.
 */
class FileCommandsVisitor implements CommandVisitor<Character> {

    private static final String DELETION = "<span style=\"background-color: #FB504B\">${text}</span>";
    private static final String INSERTION = "<span style=\"background-color: #45EA85\">${text}</span>";

    private String left = "";
    private String right = "";

    private final String path;

    public FileCommandsVisitor(String path) {
        this.path = path;
    }

    public void visitDeleteCommand(Character c) {
        // Use <br/> for new lines
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // Character is present in left file, but not right. Show with red highlight on left.
        left = left + DELETION.replace("${text}", "" + toAppend);
    }

    public void visitInsertCommand(Character c) {
        // Use <br/> for new lines
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // Character is present in right file, but not left. Show with green highlight on right.
        right = right + INSERTION.replace("${text}", "" + toAppend);
    }

    public void visitKeepCommand(Character c) {
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // Character is present in both left and right, add to both, no highlight
        left = left + toAppend;
        right = right + toAppend;
    }

    public void generateHTML() throws IOException {
        // Get template & replace placeholders with actual comparison
        String template = FileUtils.readFileToString(new File("resources/difftemplate.html"), "utf-8");
        String out1 = template.replace("${left}", left);
        String output = out1.replace("${right}", right);
        // Write file to disk
        FileUtils.write(new File( path + "finaldiff.html"), output, "utf-8");
        System.out.println("HTML diff generated.");
    }
}
