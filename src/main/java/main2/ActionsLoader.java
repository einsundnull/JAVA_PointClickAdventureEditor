package main2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads actions from resources/actions/<name>.txt files
 */
public class ActionsLoader {

    /**
     * Loads actions for a KeyArea or Item from resources/actions/<name>.txt
     * Returns a map of actionName -> List of ConditionsField
     */
    public static Map<String, List<ConditionsField>> loadActions(String entityName) {
        Map<String, List<ConditionsField>> actionsMap = new HashMap<>();
        File actionsFile = new File("resources/actions/" + entityName + ".txt");

        if (!actionsFile.exists()) {
            System.out.println("No actions file found for: " + entityName);
            return actionsMap;
        }

        System.out.println("Loading actions from: " + actionsFile.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(actionsFile))) {
            String line;
            String currentAction = null;
            String currentSection = "";
            ConditionsField currentField = null;
            List<ConditionsField> currentFieldsList = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                    continue;
                }

                // Action header: #ActionName:
                if (trimmed.startsWith("#") && trimmed.endsWith(":")) {
                    currentAction = trimmed.substring(1, trimmed.length() - 1);
                    currentFieldsList = new ArrayList<>();
                    actionsMap.put(currentAction, currentFieldsList);
                    currentField = null;
                    currentSection = "";
                    System.out.println("  Found action: " + currentAction);
                }
                // Section markers
                else if (trimmed.equals("-conditions")) {
                    currentSection = "conditions";
                    // Start new ConditionsField only if we have an action
                    if (currentFieldsList != null) {
                        currentField = new ConditionsField();
                        // Don't add yet - will add when we have actual data
                    }
                }
                else if (trimmed.equals("-process")) {
                    currentSection = "process";
                    // Add currentField if it has conditions
                    if (currentField != null && !currentField.conditions.isEmpty() && currentFieldsList != null) {
                        currentFieldsList.add(currentField);
                        System.out.println("    Added ConditionsField with " + currentField.conditions.size() + " conditions");
                    }
                    currentField = null; // Reset for next field
                }
                // Condition line: --conditionName=ifValue -> =resultValue
                else if (trimmed.startsWith("--") && currentSection.equals("conditions") && currentField != null) {
                    String condLine = trimmed.substring(2);

                    // Split by ->
                    String[] parts = condLine.split("->");
                    String condPart = parts[0].trim();
                    String resultPart = parts.length > 1 ? parts[1].trim() : "";

                    // Parse condition part: conditionName=ifValue
                    String[] condSplit = condPart.split("=");
                    if (condSplit.length >= 2) {
                        String condName = condSplit[0].trim();
                        String ifValue = condSplit[1].trim();

                        // Add to conditions
                        currentField.conditions.put(condName, ifValue);
                        System.out.println("    Condition: " + condName + " = " + ifValue);

                        // Parse result part: =resultValue
                        if (resultPart.startsWith("=") && resultPart.length() > 1) {
                            String resultValue = resultPart.substring(1).trim();
                            boolean boolResult = Boolean.parseBoolean(resultValue);
                            currentField.resultValues.put(condName, boolResult);
                            System.out.println("      -> Result: " + condName + " = " + boolResult);
                        }
                    }
                }
                // Process name (currently not used in new format, but could be extended)
                else if (currentSection.equals("process") && !trimmed.isEmpty() && currentField != null) {
                    currentField.processName = trimmed;
                    System.out.println("    Process: " + trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading actions from " + actionsFile + ": " + e.getMessage());
            e.printStackTrace();
        }

        return actionsMap;
    }

    /**
     * Applies loaded actions to a KeyArea
     */
    public static void applyActionsToKeyArea(KeyArea keyArea, Map<String, List<ConditionsField>> actionsMap) {
        for (Map.Entry<String, List<ConditionsField>> entry : actionsMap.entrySet()) {
            String actionName = entry.getKey();
            List<ConditionsField> fieldsList = entry.getValue();

            KeyArea.ActionHandler handler = new KeyArea.ActionHandler();
            keyArea.addAction(actionName, handler);

            // Process all ConditionsFields
            for (ConditionsField field : fieldsList) {
                // Build condition string for this field
                StringBuilder conditionBuilder = new StringBuilder();
                boolean first = true;

                for (Map.Entry<String, String> condEntry : field.conditions.entrySet()) {
                    String condName = condEntry.getKey();
                    String ifValue = condEntry.getValue();

                    if (!first) {
                        conditionBuilder.append(" AND ");
                    }
                    conditionBuilder.append(condName).append(" = ").append(ifValue);
                    first = false;
                }

                String condition = conditionBuilder.toString();
                if (condition.isEmpty()) {
                    condition = "none";
                }

                // Build result - apply each result value separately
                if (!field.resultValues.isEmpty()) {
                    for (Map.Entry<String, Boolean> resultEntry : field.resultValues.entrySet()) {
                        String setBooleanResult = "#SetBoolean:" + resultEntry.getKey() + "=" + resultEntry.getValue();
                        handler.addConditionalResult(condition, setBooleanResult);
                        System.out.println("    Applied result: " + setBooleanResult);
                    }
                } else {
                    // Default result if no result values
                    String result = "#Dialog:dialog-" + keyArea.getName().toLowerCase() + "-" + actionName.toLowerCase();
                    handler.addConditionalResult(condition, result);
                }

                System.out.println("  Applied: " + actionName + " with condition: " + condition);
            }
        }
    }

    /**
     * Applies loaded actions to an Item
     */
    public static void applyActionsToItem(Item item, Map<String, List<ConditionsField>> actionsMap) {
        for (Map.Entry<String, List<ConditionsField>> entry : actionsMap.entrySet()) {
            String actionName = entry.getKey();
            List<ConditionsField> fieldsList = entry.getValue();

            KeyArea.ActionHandler handler = new KeyArea.ActionHandler();
            item.addAction(actionName, handler);

            // Process all ConditionsFields
            for (ConditionsField field : fieldsList) {
                // Build condition string for this field
                StringBuilder conditionBuilder = new StringBuilder();
                boolean first = true;

                for (Map.Entry<String, String> condEntry : field.conditions.entrySet()) {
                    String condName = condEntry.getKey();
                    String ifValue = condEntry.getValue();

                    if (!first) {
                        conditionBuilder.append(" AND ");
                    }
                    conditionBuilder.append(condName).append(" = ").append(ifValue);
                    first = false;
                }

                String condition = conditionBuilder.toString();
                if (condition.isEmpty()) {
                    condition = "none";
                }

                // Build result - apply each result value separately
                if (!field.resultValues.isEmpty()) {
                    for (Map.Entry<String, Boolean> resultEntry : field.resultValues.entrySet()) {
                        String setBooleanResult = "#SetBoolean:" + resultEntry.getKey() + "=" + resultEntry.getValue();
                        handler.addConditionalResult(condition, setBooleanResult);
                        System.out.println("    Applied result: " + setBooleanResult);
                    }
                } else {
                    // Default result if no result values
                    String result = "#Dialog:dialog-" + item.getName().toLowerCase() + "-" + actionName.toLowerCase();
                    handler.addConditionalResult(condition, result);
                }

                System.out.println("  Applied: " + actionName + " with condition: " + condition);
            }
        }
    }

    /**
     * Helper class to represent a ConditionsField
     */
    public static class ConditionsField {
        public Map<String, String> conditions = new HashMap<>(); // conditionName -> ifValue (true/false/ignore)
        public Map<String, Boolean> resultValues = new HashMap<>(); // conditionName -> targetValue
        public String processName = "";
    }
}
