package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileWritingUtil {
    private static final Logger logger = LogManager.getLogger(FileWritingUtil.class);

    public static void writeResultToFile(String filePath, String result) {
        try {
            // Create a FileWriter object to write to the file
            FileWriter fileWriter = new FileWriter(filePath, true);

            // Wrap the FileWriter with BufferedWriter for efficient writing
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Write the number to the file
            bufferedWriter.write(result);

            // Close the BufferedWriter to flush and close the underlying FileWriter
            bufferedWriter.close();

            logger.info("Result has been written to " + filePath);
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
