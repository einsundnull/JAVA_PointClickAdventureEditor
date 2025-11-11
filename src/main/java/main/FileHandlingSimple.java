package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * File handling for Scene/SubScene hierarchy (Simple Schema)
 *
 * Directory structure:
 * resources/scenes/
 *   ├─ SceneName1/         (Scene directory)
 *   │   ├─ SubScene1.txt   (SubScene file)
 *   │   ├─ SubScene2.txt
 *   │   └─ SubScene_progress.txt  (Runtime progress - not loaded by editor)
 *   ├─ SceneName2/
 *   │   ├─ SubSceneA.txt
 *   │   └─ SubSceneB.txt
 */
public class FileHandlingSimple {

    private static final String SCENES_BASE_DIR = "resources/scenes";

    // ==================== Scene Operations ====================

    /**
     * Gets all Scene names (directories in resources/scenes/)
     * @return List of scene names (directory names)
     */
    public static List<String> getAllScenes() {
        List<String> sceneNames = new ArrayList<>();

        File scenesDir = new File(SCENES_BASE_DIR);
        if (!scenesDir.exists() || !scenesDir.isDirectory()) {
            System.out.println("FileHandlingSimple: Scenes directory does not exist");
            return sceneNames;
        }

        File[] sceneDirs = scenesDir.listFiles(File::isDirectory);
        if (sceneDirs != null) {
            for (File sceneDir : sceneDirs) {
                sceneNames.add(sceneDir.getName());
            }
        }

        System.out.println("FileHandlingSimple: Found " + sceneNames.size() + " scenes");
        return sceneNames;
    }

    /**
     * Gets all SubScene names for a given Scene
     * @param sceneName The scene directory name
     * @return List of subscene names (without .txt extension)
     */
    public static List<String> getSubScenes(String sceneName) {
        List<String> subSceneNames = new ArrayList<>();

        File sceneDir = new File(SCENES_BASE_DIR, sceneName);
        if (!sceneDir.exists() || !sceneDir.isDirectory()) {
            System.out.println("FileHandlingSimple: Scene directory does not exist: " + sceneName);
            return subSceneNames;
        }

        // Get all .txt files, excluding _progress files
        File[] subSceneFiles = sceneDir.listFiles((dir, name) ->
            name.endsWith(".txt") && !name.endsWith("_progress.txt"));

        if (subSceneFiles != null) {
            for (File subSceneFile : subSceneFiles) {
                String subSceneName = subSceneFile.getName().replace(".txt", "");
                subSceneNames.add(subSceneName);
            }
        }

        System.out.println("FileHandlingSimple: Found " + subSceneNames.size() + " subscenes in " + sceneName);
        return subSceneNames;
    }

    /**
     * Creates a new Scene directory
     * @param sceneName The scene name
     * @return true if created successfully
     */
    public static boolean createScene(String sceneName) {
        if (sceneName == null || sceneName.trim().isEmpty()) {
            System.err.println("FileHandlingSimple: Invalid scene name");
            return false;
        }

        File sceneDir = new File(SCENES_BASE_DIR, sceneName);

        if (sceneDir.exists()) {
            System.err.println("FileHandlingSimple: Scene already exists: " + sceneName);
            return false;
        }

        boolean created = sceneDir.mkdirs();
        if (created) {
            System.out.println("FileHandlingSimple: Created scene directory: " + sceneName);
        } else {
            System.err.println("FileHandlingSimple: Failed to create scene directory: " + sceneName);
        }

        return created;
    }

    /**
     * Deletes a Scene directory and all its SubScenes
     * @param sceneName The scene name
     * @return true if deleted successfully
     */
    public static boolean deleteScene(String sceneName) {
        if (sceneName == null || sceneName.trim().isEmpty()) {
            System.err.println("FileHandlingSimple: Invalid scene name");
            return false;
        }

        File sceneDir = new File(SCENES_BASE_DIR, sceneName);

        if (!sceneDir.exists()) {
            System.err.println("FileHandlingSimple: Scene does not exist: " + sceneName);
            return false;
        }

        // Delete all files in directory first
        File[] files = sceneDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    System.err.println("FileHandlingSimple: Failed to delete file: " + file.getName());
                    return false;
                }
            }
        }

        // Delete the directory itself
        boolean deleted = sceneDir.delete();
        if (deleted) {
            System.out.println("FileHandlingSimple: Deleted scene: " + sceneName);
        } else {
            System.err.println("FileHandlingSimple: Failed to delete scene directory: " + sceneName);
        }

        return deleted;
    }

    /**
     * Renames a Scene directory
     * @param oldName Current scene name
     * @param newName New scene name
     * @return true if renamed successfully
     */
    public static boolean renameScene(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.trim().isEmpty() || newName.trim().isEmpty()) {
            System.err.println("FileHandlingSimple: Invalid scene names");
            return false;
        }

        File oldDir = new File(SCENES_BASE_DIR, oldName);
        File newDir = new File(SCENES_BASE_DIR, newName);

        if (!oldDir.exists()) {
            System.err.println("FileHandlingSimple: Scene does not exist: " + oldName);
            return false;
        }

        if (newDir.exists()) {
            System.err.println("FileHandlingSimple: Scene already exists: " + newName);
            return false;
        }

        boolean renamed = oldDir.renameTo(newDir);
        if (renamed) {
            System.out.println("FileHandlingSimple: Renamed scene: " + oldName + " → " + newName);
        } else {
            System.err.println("FileHandlingSimple: Failed to rename scene");
        }

        return renamed;
    }

    // ==================== SubScene Operations ====================

    /**
     * Loads a SubScene from file
     * @param sceneName The scene directory name
     * @param subSceneName The subscene file name (without .txt)
     * @param gameProgress Optional game progress (can be null)
     * @return Loaded Scene object
     * @throws IOException If loading fails
     */
    public static Scene loadSubScene(String sceneName, String subSceneName, GameProgress gameProgress) throws IOException {
        String filePath = SCENES_BASE_DIR + "/" + sceneName + "/" + subSceneName + ".txt";

        System.out.println("FileHandlingSimple: Loading subscene from: " + filePath);

        // Use SceneLoader with relative path (without resources/scenes prefix)
        // SceneLoader expects: sceneName -> adds "resources/scenes/" + sceneName + ".txt"
        // We need to pass: "Beach/MainBeach" so it becomes "resources/scenes/Beach/MainBeach.txt"
        String relativeScenePath = sceneName + "/" + subSceneName;
        return SceneLoader.loadScene(relativeScenePath, gameProgress);
    }

    /**
     * Saves a SubScene to DEFAULT file (editor version)
     * @param subScene The scene object to save
     * @param sceneName The parent scene directory name
     * @throws IOException If saving fails
     */
    public static void saveSubSceneToDefault(Scene subScene, String sceneName) throws IOException {
        if (subScene == null || subScene.getName() == null) {
            throw new IOException("Invalid subscene");
        }

        // Ensure scene directory exists
        File sceneDir = new File(SCENES_BASE_DIR, sceneName);
        if (!sceneDir.exists()) {
            sceneDir.mkdirs();
        }

        String filePath = SCENES_BASE_DIR + "/" + sceneName + "/" + subScene.getName() + ".txt";

        System.out.println("FileHandlingSimple: Saving subscene to DEFAULT: " + filePath);

        // Use existing SceneSaver
        SceneSaver.saveScene(subScene, filePath);
    }

    /**
     * @deprecated Progress is now managed differently - _progress.txt files are no longer used
     */
    @Deprecated
    public static void saveSubSceneToProgress(Scene subScene, String sceneName) throws IOException {
        System.out.println("⚠️  WARNING: saveSubSceneToProgress() is deprecated - Progress is managed differently now");
        // Do nothing - progress files are no longer used
    }

    /**
     * Creates a new SubScene file with default content
     * @param sceneName The parent scene directory name
     * @param subSceneName The new subscene name
     * @return true if created successfully
     */
    public static boolean createSubScene(String sceneName, String subSceneName) {
        if (sceneName == null || subSceneName == null || sceneName.trim().isEmpty() || subSceneName.trim().isEmpty()) {
            System.err.println("FileHandlingSimple: Invalid names");
            return false;
        }

        // Ensure scene directory exists
        File sceneDir = new File(SCENES_BASE_DIR, sceneName);
        if (!sceneDir.exists()) {
            sceneDir.mkdirs();
        }

        File subSceneFile = new File(sceneDir, subSceneName + ".txt");

        if (subSceneFile.exists()) {
            System.err.println("FileHandlingSimple: SubScene already exists: " + subSceneName);
            return false;
        }

        try {
            // Create empty scene object
            Scene newSubScene = new Scene(subSceneName);
            newSubScene.setBackgroundImagePath("default.png");

            // Save to file
            saveSubSceneToDefault(newSubScene, sceneName);

            System.out.println("FileHandlingSimple: Created subscene: " + subSceneName + " in " + sceneName);
            return true;
        } catch (IOException e) {
            System.err.println("FileHandlingSimple: Failed to create subscene: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a SubScene file
     * @param sceneName The parent scene directory name
     * @param subSceneName The subscene name
     * @return true if deleted successfully
     */
    public static boolean deleteSubScene(String sceneName, String subSceneName) {
        if (sceneName == null || subSceneName == null) {
            System.err.println("FileHandlingSimple: Invalid names");
            return false;
        }

        File subSceneFile = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + subSceneName + ".txt");
        File progressFile = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + subSceneName + "_progress.txt");

        boolean deleted = false;

        // Delete DEFAULT file
        if (subSceneFile.exists()) {
            deleted = subSceneFile.delete();
            if (deleted) {
                System.out.println("FileHandlingSimple: Deleted subscene: " + subSceneName);
            }
        }

        // Delete PROGRESS file if exists
        if (progressFile.exists()) {
            progressFile.delete();
        }

        return deleted;
    }

    /**
     * Renames a SubScene file
     * @param sceneName The parent scene directory name
     * @param oldName Current subscene name
     * @param newName New subscene name
     * @return true if renamed successfully
     */
    public static boolean renameSubScene(String sceneName, String oldName, String newName) {
        if (sceneName == null || oldName == null || newName == null) {
            System.err.println("FileHandlingSimple: Invalid names");
            return false;
        }

        File oldFile = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + oldName + ".txt");
        File newFile = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + newName + ".txt");

        if (!oldFile.exists()) {
            System.err.println("FileHandlingSimple: SubScene does not exist: " + oldName);
            return false;
        }

        if (newFile.exists()) {
            System.err.println("FileHandlingSimple: SubScene already exists: " + newName);
            return false;
        }

        boolean renamed = oldFile.renameTo(newFile);
        if (renamed) {
            System.out.println("FileHandlingSimple: Renamed subscene: " + oldName + " → " + newName);

            // Also rename progress file if exists
            File oldProgress = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + oldName + "_progress.txt");
            File newProgress = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + newName + "_progress.txt");
            if (oldProgress.exists()) {
                oldProgress.renameTo(newProgress);
            }
        } else {
            System.err.println("FileHandlingSimple: Failed to rename subscene");
        }

        return renamed;
    }

    /**
     * Checks if a Scene directory exists
     * @param sceneName The scene name
     * @return true if exists
     */
    public static boolean sceneExists(String sceneName) {
        File sceneDir = new File(SCENES_BASE_DIR, sceneName);
        return sceneDir.exists() && sceneDir.isDirectory();
    }

    /**
     * Checks if a SubScene file exists
     * @param sceneName The scene directory name
     * @param subSceneName The subscene name
     * @return true if exists
     */
    public static boolean subSceneExists(String sceneName, String subSceneName) {
        File subSceneFile = new File(SCENES_BASE_DIR + "/" + sceneName + "/" + subSceneName + ".txt");
        return subSceneFile.exists();
    }
}
