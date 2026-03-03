package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Loads Process definitions from text files
 */
public class ProcessLoader {

    /**
     * Load a process by name from resources/processes/
     *
     * @param processName Name of the process file (without .txt extension)
     * @return Loaded Process object, or null if not found
     */
    public static Process loadProcess(String processName) {
        String fileName = "resources/processes/" + processName + ".txt";
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("ProcessLoader: Process file not found: " + fileName);
            return null;
        }

        try {
            return loadProcessFromFile(file);
        } catch (Exception e) {
            System.err.println("ProcessLoader: Error loading process '" + processName + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load a process from a file
     */
    private static Process loadProcessFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Process process = new Process("");

        String currentSection = "";
        boolean inConditionsIf = false;
        boolean inConditionsThen = false;
        Process.ProcessAction currentAction = null;

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }

            // Section headers
            if (line.startsWith("#ProcessName:")) {
                currentSection = "PROCESSNAME";
            } else if (line.startsWith("#Description:")) {
                currentSection = "DESCRIPTION";
            } else if (line.startsWith("#Conditions:")) {
                currentSection = "CONDITIONS";
                inConditionsIf = false;
                inConditionsThen = false;
            } else if (line.startsWith("#Actions:")) {
                currentSection = "ACTIONS";
                inConditionsIf = false;
                inConditionsThen = false;
            } else if (line.startsWith("#End")) {
                break;
            }
            // Conditions IF/THEN markers
            else if (line.equals("IF") && currentSection.equals("CONDITIONS")) {
                inConditionsIf = true;
                inConditionsThen = false;
            } else if (line.equals("THEN") && currentSection.equals("CONDITIONS")) {
                inConditionsIf = false;
                inConditionsThen = true;
            }
            // Parse content
            else if (line.startsWith("-")) {
                // Single dash = main entry
                String content = line.substring(1).trim();

                if (currentSection.equals("PROCESSNAME")) {
                    process.setProcessName(content);
                    System.out.println("ProcessLoader: Loading process '" + content + "'");
                }
                else if (currentSection.equals("DESCRIPTION")) {
                    process.setDescription(content);
                }
                else if (currentSection.equals("CONDITIONS") && inConditionsIf) {
                    // Parse condition: conditionName: value or conditionName = value
                    parseCondition(content, process);
                }
                else if (currentSection.equals("ACTIONS")) {
                    // New action - detect type and create appropriate action object
                    if (content.endsWith(":")) {
                        content = content.substring(0, content.length() - 1);
                    }

                    // Finalize previous action
                    if (currentAction != null) {
                        process.addAction(currentAction);
                    }

                    // Create new action based on type
                    currentAction = createAction(content);
                    System.out.println("ProcessLoader: Found action '" + content + "'");
                }
            }
            else if (line.startsWith("--")) {
                // Double dash = sub-entry (action parameters)
                if (currentAction != null && currentSection.equals("ACTIONS")) {
                    parseActionParameter(line.substring(2).trim(), currentAction);
                }
            }
        }

        // Finalize last action
        if (currentAction != null) {
            process.addAction(currentAction);
        }

        reader.close();

        System.out.println("ProcessLoader: Loaded process with " + process.getConditions().size() +
                         " conditions and " + process.getActions().size() + " actions");
        return process;
    }

    /**
     * Parse a condition line: "conditionName: value" or "conditionName = value"
     */
    private static void parseCondition(String line, Process process) {
        String conditionName;
        boolean value;

        if (line.contains(":")) {
            String[] parts = line.split(":");
            conditionName = parts[0].trim();
            value = Boolean.parseBoolean(parts[1].trim());
        } else if (line.contains("=")) {
            String[] parts = line.split("=");
            conditionName = parts[0].trim();
            value = Boolean.parseBoolean(parts[1].trim());
        } else if (line.contains("→")) {
            String[] parts = line.split("→");
            conditionName = parts[0].trim();
            value = Boolean.parseBoolean(parts[1].trim());
        } else {
            System.err.println("ProcessLoader: Invalid condition format: " + line);
            return;
        }

        process.addCondition(conditionName, value);
        System.out.println("ProcessLoader: Added condition '" + conditionName + "' = " + value);
    }

    /**
     * Create appropriate ProcessAction based on action type
     */
    private static Process.ProcessAction createAction(String actionType) {
        switch (actionType) {
            case "Movement":
                return new Process.MovementAction();
            case "Conditions":
                return new Process.ConditionsAction();
            case "Dialog":
                return new Process.DialogAction();
            case "Text":
                return new Process.TextAction();
            case "SceneChange":
                return new Process.SceneChangeAction();
            case "Sound":
                return new Process.SoundAction();
            case "AddToInventory":
                return new Process.AddToInventoryAction();
            case "RemoveFromInventory":
                return new Process.RemoveFromInventoryAction();
            case "Wait":
                return new Process.WaitAction();
            case "ItemVisibility":
                return new Process.ItemVisibilityAction();
            default:
                System.err.println("ProcessLoader: Unknown action type: " + actionType);
                return new Process.ConditionsAction(); // Fallback
        }
    }

    /**
     * Parse action parameter line: "key: value"
     */
    private static void parseActionParameter(String line, Process.ProcessAction action) {
        // Handle different formats: "key: value", "key = value", "key→value"
        String key;
        String value;

        if (line.contains(":")) {
            int colonIndex = line.indexOf(":");
            key = line.substring(0, colonIndex).trim();
            value = line.substring(colonIndex + 1).trim();
        } else if (line.contains("=")) {
            String[] parts = line.split("=", 2);
            key = parts[0].trim();
            value = parts[1].trim();
        } else if (line.contains("→")) {
            String[] parts = line.split("→", 2);
            key = parts[0].trim();
            value = parts[1].trim();
        } else {
            // Standalone parameter (like "GoTo")
            key = "type";
            value = line;
        }

        action.setParameter(key, value);
        System.out.println("ProcessLoader:   Parameter '" + key + "' = '" + value + "'");
    }

    /**
     * Check if a process file exists
     */
    public static boolean processExists(String processName) {
        String fileName = "resources/processes/" + processName + ".txt";
        return new File(fileName).exists();
    }

    /**
     * Get process file name for an item and action
     */
    public static String getProcessFileName(String itemName, String actionName) {
        return itemName + "_" + actionName;
    }
}
