package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class SceneLoader {
    
    public static Scene loadScene(String sceneName, GameProgress progress) throws IOException {
        return loadScene(sceneName, progress, "resources/scenes/" + sceneName + ".txt", false);
    }

    /**
     * Core load method with custom file path
     */
    private static Scene loadScene(String sceneName, GameProgress progress, String filename) throws IOException {
        return loadScene(sceneName, progress, filename, false);
    }

    /**
     * Core load method with custom file path and progress flag
     */
    private static Scene loadScene(String sceneName, GameProgress progress, String filename, boolean loadFromProgress) throws IOException {
        System.out.println("====================================================");
        System.out.println("SCENELOADER.LOADSCENE() CALLED!");
        System.out.println("  sceneName: " + sceneName);
        System.out.println("  filename: " + filename);
        System.out.println("====================================================");

        File file = new File(filename);

        if (!file.exists()) {
            throw new IOException("Scene file not found: " + filename);
        }

        if (!file.canRead()) {
            throw new IOException("Scene file cannot be read: " + filename);
        }

        // Extract only the filename (without directory path) for Scene name
        // This prevents "Beach/MainBeach" from becoming the scene name (should be just "MainBeach")
        String actualSceneName = sceneName;
        if (sceneName.contains("/")) {
            actualSceneName = sceneName.substring(sceneName.lastIndexOf("/") + 1);
        }

        System.out.println("Scene file exists: " + filename);
        System.out.println("  Using scene name: " + actualSceneName + " (extracted from: " + sceneName + ")");
        Scene scene = new Scene(actualSceneName);

        // Use try-with-resources to ensure reader is always closed
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            loadSceneContent(scene, reader, loadFromProgress);
        }

        // Load actions from separate files for all KeyAreas
        System.out.println("Loading actions from separate files...");
        if (scene.getKeyAreas() != null) {
            for (KeyArea keyArea : scene.getKeyAreas()) {
                if (keyArea == null || keyArea.getName() == null) continue;

                try {
                    Map<String, java.util.List<ActionsLoader.ConditionsField>> actionsMap =
                        ActionsLoader.loadActions(keyArea.getName());
                    if (actionsMap != null && !actionsMap.isEmpty()) {
                        ActionsLoader.applyActionsToKeyArea(keyArea, actionsMap);
                        System.out.println("  Loaded actions for KeyArea: " + keyArea.getName());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to load actions for KeyArea: " + keyArea.getName() + " - " + e.getMessage());
                }
            }
        }

        // Load actions from separate files for all Items
        if (scene.getItems() != null) {
            for (Item item : scene.getItems()) {
                if (item == null || item.getName() == null) continue;

                try {
                    Map<String, java.util.List<ActionsLoader.ConditionsField>> actionsMap =
                        ActionsLoader.loadActions(item.getName());
                    if (actionsMap != null && !actionsMap.isEmpty()) {
                        ActionsLoader.applyActionsToItem(item, actionsMap);
                        System.out.println("  Loaded actions for Item: " + item.getName());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Failed to load actions for Item: " + item.getName() + " - " + e.getMessage());
                }
            }
        }

        System.out.println("Loaded scene: " + sceneName);
        System.out.println("  KeyAreas: " + (scene.getKeyAreas() != null ? scene.getKeyAreas().size() : 0));
        System.out.println("  Dialogs: " + (scene.getDialogs() != null ? scene.getDialogs().size() : 0));
        System.out.println("  Items: " + (scene.getItems() != null ? scene.getItems().size() : 0));

        return scene;
    }

    private static void loadSceneContent(Scene scene, BufferedReader reader, boolean loadFromProgress) throws IOException {
        String line;
        String currentMainSection = null;
        KeyArea.Type currentType = null;
        String currentName = null;
        int x1 = 0, y1 = 0;
        KeyArea currentKeyArea = null;
        String currentAction = null;
        KeyArea.ActionHandler currentActionHandler = null;
        boolean inLocation = false;
        boolean inImage = false;
        boolean inActions = false;
        boolean inDialogs = false;
        boolean inMouseHover = false;
        String pendingCondition = null;
        java.util.List<String> pendingConditions = new java.util.ArrayList<>(); // Collect multiple --- lines
        String currentDialogName = null;
        StringBuilder currentDialogText = null;

        // New: For loading multiple conditional images
        ConditionalImage currentConditionalImage = null;
        boolean inBackgroundImageSection = false;
        boolean inConditionsSection = false;
        boolean inSubSceneConditions = false; // For top-level SubScene conditions

        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();

            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }

            // Main sections
            if (trimmed.equals("#Conditions:")) {
                // SubScene-level conditions (determines when this SubScene is loaded)
                currentMainSection = "SubSceneConditions";
                inSubSceneConditions = true;
                inDialogs = false;
                inBackgroundImageSection = false;
                continue;
            } else if (trimmed.startsWith("#Backgroundimage:")) {
                // Old format: single background image
                currentMainSection = "Backgroundimage";
                inDialogs = false;
                inBackgroundImageSection = false;
                inSubSceneConditions = false;
                continue;
            } else if (trimmed.equals("#BackgroundImages:")) {
                // New format: multiple conditional images
                currentMainSection = "BackgroundImages";
                inDialogs = false;
                inBackgroundImageSection = false;
                inSubSceneConditions = false;
                continue;
            } else if (trimmed.equals("#KeyArea:")) {
                currentMainSection = "KeyArea";
                currentType = null;
                currentName = null;
                currentKeyArea = null;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                inDialogs = false;
                inSubSceneConditions = false;
                continue;
            } else if (trimmed.equals("#Dialogs:")) {
                currentMainSection = "Dialogs";
                inDialogs = true;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                inSubSceneConditions = false;
                continue;
            } else if (trimmed.equals("#Items:")) {
                System.out.println("=====> Found #Items: section! <=====");
                currentMainSection = "Items";
                inDialogs = false;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                inSubSceneConditions = false;
                continue;
            } else if (trimmed.equals("#SelectedItem:")) {
                System.out.println("=====> Found #SelectedItem: section! <=====");
                currentMainSection = "SelectedItem";
                inDialogs = false;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                inSubSceneConditions = false;
                continue;
            }

            // Parse SubScene Conditions (determines when this SubScene is loaded)
            if ("SubSceneConditions".equals(currentMainSection) && inSubSceneConditions) {
                if (trimmed.startsWith("-")) {
                    // Parse condition: "-hasLighter = true"
                    String conditionLine = trimmed.substring(1).trim();
                    if (conditionLine.contains("=")) {
                        String[] parts = conditionLine.split("=");
                        if (parts.length == 2) {
                            String condName = parts[0].trim();
                            boolean condValue = Boolean.parseBoolean(parts[1].trim());
                            scene.addSubSceneCondition(condName, condValue);
                            System.out.println("  SubScene Condition: " + condName + " = " + condValue);
                        }
                    }
                }
            }

            // Parse Backgroundimage (old format - backwards compatibility)
            // MIGRATION: Convert to ConditionalImage
            else if ("Backgroundimage".equals(currentMainSection) && !inDialogs) {
                if (trimmed.startsWith("-")) {
                    String bgPath = trimmed.substring(1).trim();
                    // Create ConditionalImage from legacy format
                    ConditionalImage defaultImage = new ConditionalImage(bgPath, "Default");
                    scene.addBackgroundImage(defaultImage);
                    // Also set legacy field for backward compatibility during transition
                    scene.setBackgroundImagePath(bgPath);
                }
            }

            // Parse BackgroundImages (new format - multiple conditional images)
            else if ("BackgroundImages".equals(currentMainSection) && !inDialogs) {
                if (trimmed.equals("-Image:")) {
                    // Save previous image if exists
                    if (currentConditionalImage != null) {
                        scene.addBackgroundImage(currentConditionalImage);
                    }
                    // Start new image
                    currentConditionalImage = new ConditionalImage();
                    inBackgroundImageSection = true;
                    inConditionsSection = false;
                } else if (inBackgroundImageSection && currentConditionalImage != null) {
                    if (trimmed.startsWith("--Name:")) {
                        String name = trimmed.substring(7).trim();
                        currentConditionalImage.setName(name);
                    } else if (trimmed.startsWith("--Path:")) {
                        String path = trimmed.substring(7).trim();
                        currentConditionalImage.setImagePath(path);
                    } else if (trimmed.startsWith("--ShowIfTrue:")) {
                        String showIfStr = trimmed.substring(13).trim();
                        currentConditionalImage.setShowIfTrue(Boolean.parseBoolean(showIfStr));
                    } else if (trimmed.startsWith("--FlipHorizontally:") || trimmed.startsWith("---FlipHorizontally")) {
                        // Support both formats: "--FlipHorizontally:" and "---FlipHorizontally = true"
                        String flipStr;
                        if (trimmed.contains("=")) {
                            flipStr = trimmed.substring(trimmed.indexOf("=") + 1).trim();
                        } else {
                            flipStr = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                        }
                        currentConditionalImage.setFlipHorizontally(Boolean.parseBoolean(flipStr));
                    } else if (trimmed.startsWith("--FlipVertically:") || trimmed.startsWith("---FlipVertically")) {
                        // Support both formats: "--FlipVertically:" and "---FlipVertically = true"
                        String flipStr;
                        if (trimmed.contains("=")) {
                            flipStr = trimmed.substring(trimmed.indexOf("=") + 1).trim();
                        } else {
                            flipStr = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                        }
                        currentConditionalImage.setFlipVertically(Boolean.parseBoolean(flipStr));
                    } else if (trimmed.equals("--Conditions:")) {
                        inConditionsSection = true;
                    } else if (inConditionsSection && trimmed.startsWith("---")) {
                        // Skip flip settings in conditions section
                        if (trimmed.startsWith("---FlipHorizontally") || trimmed.startsWith("---FlipVertically")) {
                            // Already handled above
                        } else {
                            // Parse condition: "---hasLighter = true"
                            String conditionLine = trimmed.substring(3).trim();
                            if (conditionLine.contains("=")) {
                                String[] parts = conditionLine.split("=");
                                if (parts.length == 2) {
                                    String condName = parts[0].trim();
                                    boolean condValue = Boolean.parseBoolean(parts[1].trim());
                                    currentConditionalImage.addCondition(condName, condValue);
                                }
                            }
                        }
                    }
                }
            }
            // Parse KeyArea
            else if ("KeyArea".equals(currentMainSection) && !inDialogs) {
                // Type
                if (trimmed.startsWith("-Type:")) {
                    String typeStr = trimmed.substring(6).trim();
                    switch (typeStr) {
                        case "TRANSITION":
                        case "Transition":
                            currentType = KeyArea.Type.TRANSITION;
                            break;
                        case "INTERACTION":
                        case "Interaction":
                            currentType = KeyArea.Type.INTERACTION;
                            break;
                        case "MOVEMENT_BOUNDS":
                        case "Movement_Bounds":
                            currentType = KeyArea.Type.MOVEMENT_BOUNDS;
                            break;
                        case "CHARACTER_RANGE":
                        case "Character_Range":
                            currentType = KeyArea.Type.CHARACTER_RANGE;
                            break;
                        default:
                            currentType = KeyArea.Type.INTERACTION;
                            break;
                    }
                }
                // Name
                else if (trimmed.startsWith("-Name:")) {
                    currentName = trimmed.substring(6).trim().replace(";", "");
                    // Create KeyArea immediately
                    if (currentType != null && currentName != null) {
                        currentKeyArea = new KeyArea(currentType, currentName);
                        scene.addKeyArea(currentKeyArea);
                        System.out.println("Created KeyArea: " + currentName);
                    }
                }
                // MouseHover section
                else if (trimmed.equals("-MouseHover")) {
                    inMouseHover = true;
                    inImage = false;
                    inLocation = false;
                    inActions = false;
                }
                else if (inMouseHover && trimmed.startsWith("--conditions")) {
                    // Clear previous conditions when starting new conditions block
                    pendingConditions.clear();
                    continue;
                }
                else if (inMouseHover && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    String conditionLine = trimmed.substring(3).trim().replace(";", "");
                    pendingConditions.add(conditionLine);
                }
                else if (inMouseHover && trimmed.startsWith("----Display:")) {
                    continue;
                }
                else if (inMouseHover && trimmed.startsWith("------")) {
                    String displayText = trimmed.substring(6).trim().replace("\"", "");
                    // Combine all pending conditions with AND
                    if (currentKeyArea != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentKeyArea.addHoverDisplayCondition(pendingCondition, displayText);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
                // Image section
                else if (trimmed.equals("-Image")) {
                    inImage = true;
                    inLocation = false;
                    inActions = false;
                    inMouseHover = false;
                }
                else if (inImage && trimmed.startsWith("--conditions:")) {
                    // Clear previous conditions when starting new conditions block
                    pendingConditions.clear();
                    continue;
                }
                else if (inImage && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    String conditionLine = trimmed.substring(3).trim().replace(";", "");
                    pendingConditions.add(conditionLine);
                }
                else if (inImage && trimmed.startsWith("----Image:")) {
                    String imagePath = trimmed.substring(10).trim().replace(";", "");
                    // Combine all pending conditions with AND
                    if (currentKeyArea != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentKeyArea.addImageCondition(pendingCondition, imagePath);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
                // Actions section (with #Actions:)
                else if (trimmed.equals("#Actions:")) {
                    inActions = true;
                    inImage = false;
                    inLocation = false;
                    inMouseHover = false;
                }
                // Action name (starts with single -)
                else if (inActions && trimmed.startsWith("-") && !trimmed.startsWith("--")) {
                    // Clear pending conditions from previous action
                    pendingConditions.clear();
                    currentAction = trimmed.substring(1).trim();
                    currentActionHandler = new KeyArea.ActionHandler();
                    if (currentKeyArea != null) {
                        currentKeyArea.addAction(currentAction, currentActionHandler);
                    }
                }
                // Conditions for action
                else if (inActions && trimmed.startsWith("--conditions")) {
                    // Clear previous conditions when starting new conditions block
                    pendingConditions.clear();
                    continue;
                }
                else if (inActions && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    String conditionLine = trimmed.substring(3).trim().replace(";", "");
                    pendingConditions.add(conditionLine);
                }
                else if (inActions && trimmed.startsWith("----#Dialog:")) {
                    continue;
                }
                else if (inActions && trimmed.startsWith("------")) {
                    String result = trimmed.substring(6).trim();
                    // Combine all pending conditions with AND
                    if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentActionHandler.addConditionalResult(pendingCondition, "#Dialog:" + result);
                        System.out.println("SceneLoader: Added Dialog result for condition: " + pendingCondition);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
                else if (inActions && trimmed.startsWith("----##load")) {
                    String result = trimmed.substring(4).trim();
                    // Combine all pending conditions with AND
                    if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentActionHandler.addConditionalResult(pendingCondition, result);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
                else if (inActions && trimmed.startsWith("----#SetBoolean:")) {
                    String result = trimmed.substring(4).trim();
                    // Combine all pending conditions with AND
                    if (currentActionHandler != null && !pendingConditions.isEmpty()) {
                        pendingCondition = String.join(" AND ", pendingConditions);
                        currentActionHandler.addConditionalResult(pendingCondition, result);
                        System.out.println("SceneLoader: Added SetBoolean result: " + result + " for condition: " + pendingCondition);
                    }
                    pendingCondition = null;
                    // Don't clear pendingConditions here - may have multiple results for same conditions
                }
                // Location section (comes AFTER Actions, starts with ##Location:)
                else if (trimmed.equals("##Location:")) {
                    inLocation = true;
                    inImage = false;
                    inActions = false;
                    inMouseHover = false;
                }
                else if (inLocation && trimmed.startsWith("###")) {
                    continue;
                }
                else if (inLocation && trimmed.startsWith("--x")) {
                    x1 = parseCoordinate(trimmed);
                }
                else if (inLocation && trimmed.startsWith("--y")) {
                    y1 = parseCoordinate(trimmed);
                    if (currentKeyArea != null) {
                        currentKeyArea.addPoint(x1, y1);
                        System.out.println("  Added point: (" + x1 + ", " + y1 + ")");
                    }
                }
            }
            // Parse Dialogs
            else if ("Dialogs".equals(currentMainSection)) {
                if (trimmed.startsWith("-") && !trimmed.startsWith("--")) {
                    // Save previous dialog
                    if (currentDialogName != null && currentDialogText != null) {
                        scene.addDialog(currentDialogName, currentDialogText.toString().trim());
                    }
                    // Start new dialog
                    currentDialogName = trimmed.substring(1).trim();
                    currentDialogText = new StringBuilder();
                } else if (trimmed.startsWith("--")) {
                    // Dialog line
                    String dialogLine = trimmed.substring(2).trim();
                    if (currentDialogText != null) {
                        if (currentDialogText.length() > 0) {
                            currentDialogText.append("\n");
                        }
                        currentDialogText.append(dialogLine);
                    }
                }
            }
            // Parse Items
            else if ("Items".equals(currentMainSection)) {
                System.out.println("DEBUG SceneLoader: In Items section!");
                if (trimmed.startsWith("-")) {
                    String itemName = trimmed.substring(1).trim();
                    System.out.println("DEBUG SceneLoader: Found item line: " + itemName);
                    try {
                        Item item;
                        if (loadFromProgress) {
                            System.out.println("DEBUG SceneLoader: Calling ItemLoader.loadItemFromProgress(" + itemName + ")");
                            item = ItemLoader.loadItemFromProgress(itemName);
                        } else {
                            System.out.println("DEBUG SceneLoader: Calling ItemLoader.loadItemFromDefault(" + itemName + ")");
                            item = ItemLoader.loadItemFromDefault(itemName);
                        }
                        scene.addItem(item);
                        System.out.println("  Loaded item: " + itemName);
                        System.out.println("  Item hoverDisplayConditions size: " + item.getHoverDisplayConditions().size());
                    } catch (Exception e) {
                        System.err.println("WARNING: Could not load item: " + itemName + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            // Parse SelectedItem (editor selection - persists across sessions)
            else if ("SelectedItem".equals(currentMainSection)) {
                if (trimmed.startsWith("-")) {
                    String selectedItemName = trimmed.substring(1).trim();
                    System.out.println("  Selected item: " + selectedItemName);
                    // Find and select the item in the scene
                    if (scene.getItems() != null) {
                        for (Item item : scene.getItems()) {
                            if (item != null && item.getName().equals(selectedItemName)) {
                                scene.setSelectedItem(item);
                                System.out.println("  ✓ Restored selected item: " + selectedItemName);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Add last conditional image if exists
        if (currentConditionalImage != null) {
            scene.addBackgroundImage(currentConditionalImage);
        }

        // Add last dialog
        if (currentDialogName != null && currentDialogText != null) {
            scene.addDialog(currentDialogName, currentDialogText.toString().trim());
        }
    }

    /**
     * Overloaded method for loading scene without GameProgress (for editor use)
     */
    public static Scene loadScene(String sceneName) throws IOException {
        return loadScene(sceneName, null);
    }

    private static int parseCoordinate(String line) {
        String[] parts = line.split("=");
        if (parts.length == 2) {
            return Integer.parseInt(parts[1].trim().replace(";", ""));
        }
        return 0;
    }

    /**
     * @deprecated Use loadScene() instead - <name>.txt is now the DEFAULT
     */
    @Deprecated
    public static Scene loadSceneFromDefault(String sceneName, GameProgress progress) throws IOException {
        System.out.println("✓ Loading scene from DEFAULT: resources/scenes/" + sceneName + ".txt");
        return loadScene(sceneName, progress);
    }

    /**
     * @deprecated Progress is now managed differently - _progress.txt files are no longer used
     */
    @Deprecated
    public static Scene loadSceneFromProgress(String sceneName, GameProgress progress) throws IOException {
        System.out.println("⚠️  WARNING: loadSceneFromProgress() is deprecated - Progress is managed differently now");
        // Fall back to loading from default file
        return loadScene(sceneName, progress);
    }

    /**
     * @deprecated This method is no longer used - kept for compatibility
     */
    @Deprecated
    private static Scene loadSceneFromProgressLegacy(String sceneName, GameProgress progress) throws IOException {
        String progressFilename = "resources/scenes/" + sceneName + "_progress.txt";
        File progressFile = new File(progressFilename);

        if (progressFile.exists()) {
            System.out.println("✓ Loading scene from PROGRESS: " + progressFilename);
            return loadScene(sceneName, progress, progressFilename, true);
        } else {
            System.out.println("⚠️  Progress file not found, falling back to DEFAULT: " + progressFilename);
            return loadScene(sceneName, progress);
        }
    }
}