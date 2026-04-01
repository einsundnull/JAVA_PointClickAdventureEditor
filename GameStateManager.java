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
            File conditionsProgress = ResourcePathHelper.resolve("conditions-progress.txt");
            if (!conditionsProgress.exists()) {
                System.out.println("⚠️  conditions-progress.txt missing, creating from defaults...");
                copyFile(ResourcePathHelper.resolvePath("conditions/conditions.txt"), ResourcePathHelper.resolvePath("conditions-progress.txt"));
            }

            // Ensure progress directories exist
            ResourcePathHelper.resolve("scenes-progress").mkdirs();
            ResourcePathHelper.resolve("items-progress").mkdirs();

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
        copyFile(ResourcePathHelper.resolvePath("conditions/conditions.txt"), ResourcePathHelper.resolvePath("conditions-progress.txt"));

        // 2. Copy all scenes/*.txt → scenes-progress/*.txt
        File scenesDir = ResourcePathHelper.resolve("scenes");
        File scenesProgressDir = ResourcePathHelper.resolve("scenes-progress");
        scenesProgressDir.mkdirs();

        if (scenesDir.exists()) {
            File[] sceneFiles = scenesDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (sceneFiles != null) {
                for (File sceneFile : sceneFiles) {
                    String filename = sceneFile.getName();
                    copyFile(sceneFile.getPath(), ResourcePathHelper.resolvePath("scenes-progress/" + filename));
                }
            }
        }

        // 3. Copy all items/*.txt → items-progress/*.txt
        File itemsDir = ResourcePathHelper.resolve("items");
        File itemsProgressDir = ResourcePathHelper.resolve("items-progress");
        itemsProgressDir.mkdirs();

        if (itemsDir.exists()) {
            File[] itemFiles = itemsDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (itemFiles != null) {
                for (File itemFile : itemFiles) {
                    String filename = itemFile.getName();
                    copyFile(itemFile.getPath(), ResourcePathHelper.resolvePath("items-progress/" + filename));
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
        File progressConditions = ResourcePathHelper.resolve("conditions-progress.txt");
        File scenesProgressDir = ResourcePathHelper.resolve("scenes-progress");

        return progressConditions.exists() && scenesProgressDir.exists() && scenesProgressDir.list().length > 0;
    }
}
