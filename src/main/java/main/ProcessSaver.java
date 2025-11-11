package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Saves Process definitions to text files
 */
public class ProcessSaver {

    /**
     * Save a process to resources/processes/
     *
     * @param process The process to save
     * @return true if successful, false otherwise
     */
    public static boolean saveProcess(Process process) {
        String fileName = "resources/processes/" + process.getProcessName() + ".txt";
        File file = new File(fileName);

        // Create directory if needed
        file.getParentFile().mkdirs();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            // Process Name
            writer.write("#ProcessName:\n");
            writer.write("-" + process.getProcessName() + "\n\n");

            // Description
            writer.write("#Description:\n");
            writer.write("-" + process.getDescription() + "\n\n");

            // Conditions
            writer.write("#Conditions:\n");
            if (!process.getConditions().isEmpty()) {
                writer.write("IF\n");
                for (Map.Entry<String, Boolean> entry : process.getConditions().entrySet()) {
                    writer.write("-" + entry.getKey() + ": " + entry.getValue() + "\n");
                }
                writer.write("\n");
            }

            // Actions
            writer.write("#Actions:\n");
            for (Process.ProcessAction action : process.getActions()) {
                writer.write("-" + action.getActionType() + "\n");

                // Write parameters
                for (Map.Entry<String, String> param : action.parameters.entrySet()) {
                    writer.write("--" + param.getKey() + ": " + param.getValue() + "\n");
                }
                writer.write("\n");
            }

            writer.write("#End\n");
            writer.close();

            System.out.println("✓ Process saved to: " + fileName);
            return true;
        } catch (IOException e) {
            System.err.println("ERROR saving process: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get process file name for an item and action
     */
    public static String getProcessFileName(String itemName, String actionName) {
        return itemName + "_" + actionName;
    }
}
