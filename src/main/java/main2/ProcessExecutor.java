package main2;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import javax.swing.SwingWorker;

/**
 * Executes processes defined in resources/processes/*.txt files
 * 
 * Process file format:
 * condition: <conditionName> = <true|false>  - Check condition before continuing
 * pause: <seconds>                           - Wait for specified seconds
 * dialog: <dialogName>                       - Show dialog
 * result condition: <conditionName> = <value> - Set condition value
 */
public class ProcessExecutor {
    
    private AdventureGame game;
    
    public ProcessExecutor(AdventureGame game) {
        this.game = game;
    }
    
    /**
     * Executes a process from file
     * @param processName Name of the process file (without .txt)
     */
    public void executeProcess(String processName) {
        File processFile = new File("resources/processes/" + processName + ".txt");
        
        if (!processFile.exists()) {
            System.err.println("Process file not found: " + processFile.getPath());
            return;
        }
        
        // Execute in background thread to allow pauses
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> lines = Files.readAllLines(processFile.toPath());
                
                for (String line : lines) {
                    line = line.trim();
                    
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue; // Skip empty lines and comments
                    }
                    
                    executeProcessLine(line);
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                System.out.println("Process completed: " + processName);
            }
        };
        
        worker.execute();
    }
    
    /**
     * Executes a single line from the process file
     */
    private void executeProcessLine(String line) throws InterruptedException {
        if (line.startsWith("condition:")) {
            // Check condition
            String conditionStr = line.substring(10).trim();
            if (!evaluateCondition(conditionStr)) {
                System.out.println("Process stopped - condition not met: " + conditionStr);
                throw new InterruptedException("Condition not met");
            }
            System.out.println("Process: Condition met - " + conditionStr);
            
        } else if (line.startsWith("pause:")) {
            // Pause execution
            String secondsStr = line.substring(6).trim();
            try {
                int seconds = Integer.parseInt(secondsStr);
                System.out.println("Process: Pausing for " + seconds + " seconds...");
                Thread.sleep(seconds * 1000);
            } catch (NumberFormatException e) {
                System.err.println("Invalid pause value: " + secondsStr);
            }
            
        } else if (line.startsWith("dialog:")) {
            // Show dialog
            String dialogName = line.substring(7).trim();
            // Remove .txt extension if present
            if (dialogName.endsWith(".txt")) {
                dialogName = dialogName.substring(0, dialogName.length() - 4);
            }
            final String finalDialogName = dialogName;
            System.out.println("Process: Showing dialog - " + finalDialogName);
            
            // Execute dialog on EDT
            javax.swing.SwingUtilities.invokeLater(() -> {
                game.showDialogByName(finalDialogName);
            });
            
        } else if (line.startsWith("result condition:")) {
            // Set condition result
            String conditionStr = line.substring(17).trim();
            String[] parts = conditionStr.split("=");
            if (parts.length == 2) {
                String conditionName = parts[0].trim();
                boolean value = Boolean.parseBoolean(parts[1].trim());
                
                System.out.println("Process: Setting condition - " + conditionName + " = " + value);
                
                // Execute on EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    Conditions.setCondition(conditionName, value);
                });
            }
        }
    }
    
    /**
     * Evaluates a condition string like "conditionName = true"
     */
    private boolean evaluateCondition(String conditionStr) {
        String[] parts = conditionStr.split("=");
        if (parts.length == 2) {
            String conditionName = parts[0].trim();
            boolean expectedValue = Boolean.parseBoolean(parts[1].trim());
            boolean actualValue = Conditions.getCondition(conditionName);
            return actualValue == expectedValue;
        }
        return false;
    }
}
