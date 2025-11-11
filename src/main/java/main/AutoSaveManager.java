package main;

import java.io.IOException;

/**
 * Manages automatic saving to progress files during gameplay
 * Enable in GAME mode, disable in EDITOR mode
 */
public class AutoSaveManager {

    private static boolean enabled = false;
    private static Scene currentScene = null;

    /**
     * Enable/disable auto-save
     * Call this when switching between GAME and EDITOR mode
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
        System.out.println("AutoSaveManager: " + (enable ? "ENABLED (GAME MODE)" : "DISABLED (EDITOR MODE)"));

        // Also update Conditions auto-save
        Conditions.setAutoSaveToProgress(enable);
    }

    /**
     * Set the current scene for auto-saving
     */
    public static void setCurrentScene(Scene scene) {
        currentScene = scene;
    }

    /**
     * Auto-save an item to progress
     * Called when item position changes, isInInventory changes, etc.
     */
    public static void saveItem(Item item) {
        if (!enabled) {
            return; // Don't save in editor mode
        }

        try {
            ItemSaver.saveItemToProgress(item);
            System.out.println("  → Auto-saved item to progress: " + item.getName());
        } catch (IOException e) {
            System.err.println("ERROR auto-saving item: " + e.getMessage());
        }
    }

    /**
     * Auto-save the current scene to progress
     * Called when scene state changes
     */
    public static void saveScene(Scene scene) {
        if (!enabled) {
            return; // Don't save in editor mode
        }

        try {
            SceneSaver.saveSceneToProgress(scene);
            System.out.println("  → Auto-saved scene to progress: " + scene.getName());
        } catch (IOException e) {
            System.err.println("ERROR auto-saving scene: " + e.getMessage());
        }
    }

    /**
     * Auto-save the current scene (uses stored currentScene)
     */
    public static void saveCurrentScene() {
        if (currentScene != null) {
            saveScene(currentScene);
        }
    }

    /**
     * Check if auto-save is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
}
