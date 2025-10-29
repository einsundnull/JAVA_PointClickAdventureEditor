package main2;

import java.io.*;
import java.util.*;

public class SceneLoader {
    
    public static Scene loadScene(String sceneName, GameProgress progress) throws IOException {
        String filename = "resources/scenes/" + sceneName + ".txt";
        File file = new File(filename);
        
        if (!file.exists()) {
            throw new IOException("Scene file not found: " + filename);
        }
        
        Scene scene = new Scene(sceneName);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        
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
        String currentDialogName = null;
        StringBuilder currentDialogText = null;
        
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Main sections
            if (trimmed.startsWith("#Backgroundimage:")) {
                currentMainSection = "Backgroundimage";
                inDialogs = false;
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
                continue;
            } else if (trimmed.equals("#Dialogs:")) {
                currentMainSection = "Dialogs";
                inDialogs = true;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                continue;
            } else if (trimmed.equals("#Items:")) {
                currentMainSection = "Items";
                inDialogs = false;
                inLocation = false;
                inImage = false;
                inActions = false;
                inMouseHover = false;
                continue;
            }
            
            // Parse Backgroundimage
            if ("Backgroundimage".equals(currentMainSection) && !inDialogs) {
                if (trimmed.startsWith("-")) {
                    String bgPath = trimmed.substring(1).trim();
                    scene.setBackgroundImagePath(bgPath);
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
                    continue;
                }
                else if (inMouseHover && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    pendingCondition = trimmed.substring(3).trim().replace(";", "");
                }
                else if (inMouseHover && trimmed.startsWith("----Display:")) {
                    continue;
                }
                else if (inMouseHover && trimmed.startsWith("------")) {
                    String displayText = trimmed.substring(6).trim().replace("\"", "");
                    if (currentKeyArea != null && pendingCondition != null) {
                        currentKeyArea.addHoverDisplayCondition(pendingCondition, displayText);
                    }
                    pendingCondition = null;
                }
                // Image section
                else if (trimmed.equals("-Image")) {
                    inImage = true;
                    inLocation = false;
                    inActions = false;
                    inMouseHover = false;
                }
                else if (inImage && trimmed.startsWith("--conditions:")) {
                    continue;
                }
                else if (inImage && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    pendingCondition = trimmed.substring(3).trim().replace(";", "");
                }
                else if (inImage && trimmed.startsWith("----Image:")) {
                    String imagePath = trimmed.substring(10).trim().replace(";", "");
                    if (currentKeyArea != null && pendingCondition != null) {
                        currentKeyArea.addImageCondition(pendingCondition, imagePath);
                    }
                    pendingCondition = null;
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
                    currentAction = trimmed.substring(1).trim();
                    currentActionHandler = new KeyArea.ActionHandler();
                    if (currentKeyArea != null) {
                        currentKeyArea.addAction(currentAction, currentActionHandler);
                    }
                }
                // Conditions for action
                else if (inActions && trimmed.startsWith("--conditions")) {
                    continue;
                }
                else if (inActions && trimmed.startsWith("---") && !trimmed.startsWith("----")) {
                    pendingCondition = trimmed.substring(3).trim().replace(";", "");
                }
                else if (inActions && trimmed.startsWith("----#Dialog:")) {
                    continue;
                }
                else if (inActions && trimmed.startsWith("------")) {
                    String result = trimmed.substring(6).trim();
                    if (currentActionHandler != null && pendingCondition != null) {
                        currentActionHandler.addConditionalResult(pendingCondition, "#Dialog:" + result);
                    }
                    pendingCondition = null;
                }
                else if (inActions && trimmed.startsWith("----##load")) {
                    String result = trimmed.substring(4).trim();
                    if (currentActionHandler != null && pendingCondition != null) {
                        currentActionHandler.addConditionalResult(pendingCondition, result);
                    }
                    pendingCondition = null;
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
                if (trimmed.startsWith("-")) {
                    String itemName = trimmed.substring(1).trim();
                    try {
                        Item item = ItemLoader.loadItemByName(itemName);
                        scene.addItem(item);
                        System.out.println("  Loaded item: " + itemName);
                    } catch (Exception e) {
                        System.err.println("WARNING: Could not load item: " + itemName + " - " + e.getMessage());
                    }
                }
            }
        }

        // Add last dialog
        if (currentDialogName != null && currentDialogText != null) {
            scene.addDialog(currentDialogName, currentDialogText.toString().trim());
        }

        reader.close();

        System.out.println("Loaded scene: " + sceneName);
        System.out.println("  KeyAreas: " + scene.getKeyAreas().size());
        System.out.println("  Dialogs: " + scene.getDialogs().size());
        System.out.println("  Items: " + scene.getItems().size());

        return scene;
    }
    
    private static int parseCoordinate(String line) {
        String[] parts = line.split("=");
        if (parts.length == 2) {
            return Integer.parseInt(parts[1].trim().replace(";", ""));
        }
        return 0;
    }
}