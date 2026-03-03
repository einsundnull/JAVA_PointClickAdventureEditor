package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Manages game state: copying defaults to progress, loading game states
 */
public class GameStateManager {

    /**
     * Ensures all required progress files exist.
     * If they don't exist, they are created from defaults.
     * This is called on game startup.
     */
    public static void ensureProgressFilesExist() {
        try {
            File conditionsProgress = new File("resources/conditions-progress.txt");
            if (!conditionsProgress.exists()) {
                System.out.println("⚠️  conditions-progress.txt missing, creating from defaults...");
                copyFile("resources/conditions/conditions.txt", "resources/conditions-progress.txt");
            }

            // Ensure progress directories exist
            new File("resources/scenes-progress").mkdirs();
            new File("resources/items-progress").mkdirs();

            System.out.println("✓ Progress file structure verified");
        } catch (Exception e) {
            System.err.println("ERROR ensuring progress files: " + e.getMessage());
        }
    }

    /**
     * NEW GAME: Copy all DEFAULT files to PROGRESS files
     * This creates a fresh game state from the defaults
     */
    public static void copyDefaultsToProgress() throws IOException {
        System.out.println("========================================");
        System.out.println("NEW GAME: Copying all DEFAULT files to PROGRESS...");
        System.out.println("========================================");

        // 1. Copy conditions/conditions.txt → conditions-progress.txt
        copyFile("resources/conditions/conditions.txt", "resources/conditions-progress.txt");

        // 2. Copy all scenes/*.txt → scenes-progress/*.txt
        File scenesDir = new File("resources/scenes");
        File scenesProgressDir = new File("resources/scenes-progress");
        scenesProgressDir.mkdirs();

        if (scenesDir.exists()) {
            File[] sceneFiles = scenesDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (sceneFiles != null) {
                for (File sceneFile : sceneFiles) {
                    String filename = sceneFile.getName();
                    copyFile(sceneFile.getPath(), "resources/scenes-progress/" + filename);
                }
            }
        }

        // 3. Copy all items/*.txt → items-progress/*.txt
        File itemsDir = new File("resources/items");
        File itemsProgressDir = new File("resources/items-progress");
        itemsProgressDir.mkdirs();

        if (itemsDir.exists()) {
            File[] itemFiles = itemsDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (itemFiles != null) {
                for (File itemFile : itemFiles) {
                    String filename = itemFile.getName();
                    copyFile(itemFile.getPath(), "resources/items-progress/" + filename);
                }
            }
        }

        System.out.println("✓ NEW GAME: All defaults copied to progress!");
    }

    /**
     * Helper to copy a single file
     */
    private static void copyFile(String sourcePath, String destPath) throws IOException {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);

        if (sourceFile.exists()) {
            destFile.getParentFile().mkdirs();
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("  Copied: " + sourcePath + " → " + destPath);
        } else {
            System.out.println("  ⚠️  Source not found: " + sourcePath);
        }
    }

    /**
     * Get all scene names from scenes directory
     */
    public static List<String> getAllSceneNames() {
        return SceneReferenceManager.getAllSceneNames();
    }

    /**
     * Check if progress files exist
     */
    public static boolean progressFilesExist() {
        File progressConditions = new File("resources/conditions-progress.txt");
        File scenesProgressDir = new File("resources/scenes-progress");

        return progressConditions.exists() && scenesProgressDir.exists() && scenesProgressDir.list().length > 0;
    }
}
