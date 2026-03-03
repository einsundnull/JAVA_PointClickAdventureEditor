package main;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Migration tool to update progress.txt to Simple format
 *
 * Old: currentScene=sceneBeach
 * New: currentScene=Beach/MainBeach
 */
public class MigrateToSimpleFormat {

    public static void main(String[] args) {
        System.out.println("=== Migrating to Simple Format ===\n");

        try {
            // Update progress.txt
            updateProgressFile("resources/progress.txt");

            // Update progress_default.txt if it exists
            java.io.File defaultFile = new java.io.File("resources/progress_default.txt");
            if (defaultFile.exists()) {
                updateProgressFile("resources/progress_default.txt");
            }

            System.out.println("\n✓ Migration complete!");
            System.out.println("You can now start the game with the new Simple format.");

        } catch (Exception e) {
            System.err.println("ERROR during migration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void updateProgressFile(String filename) throws Exception {
        System.out.println("→ Updating " + filename);

        // Read current content
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(filename));
        StringBuilder content = new StringBuilder();
        String line;
        boolean updated = false;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("currentScene=")) {
                String oldScene = line.substring(13).trim();
                String newScene = migrateSceneName(oldScene);

                content.append("currentScene=").append(newScene).append("\n");
                System.out.println("  ✓ Updated: " + oldScene + " → " + newScene);
                updated = true;
            } else {
                content.append(line).append("\n");
            }
        }
        reader.close();

        // Write updated content
        if (updated) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(content.toString());
            writer.close();
            System.out.println("  ✓ Saved " + filename);
        } else {
            System.out.println("  - No changes needed");
        }
    }

    private static String migrateSceneName(String oldSceneName) {
        // Map old scene names to new format: SceneName/MainSceneName
        switch (oldSceneName) {
            case "sceneBeach":
                return "Beach/MainBeach";
            case "sceneWood":
                return "Wood/MainWood";
            case "sceneTown":
                return "Town/MainTown";
            default:
                // Already in new format or unknown
                return oldSceneName;
        }
    }
}
