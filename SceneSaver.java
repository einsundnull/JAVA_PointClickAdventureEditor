package main;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SceneSaver {
    
    public static void saveScene(Scene scene, String filename) throws IOException {
        if (scene == null) {
            throw new IOException("Cannot save null scene");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("Invalid filename");
        }

        File file = new File(filename);

        // Ensure parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created && !parentDir.exists()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // Use try-with-resources to ensure writer is always closed
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            saveSceneContent(scene, writer);
        }

        System.out.println("Scene saved to: " + filename);
        System.out.println("  KeyAreas: " + (scene.getKeyAreas() != null ? scene.getKeyAreas().size() : 0));
        System.out.println("  Total points: " + (scene.getKeyAreas() != null ? scene.getKeyAreas().stream()
            .mapToInt(a -> a.getPoints() != null ? a.getPoints().size() : 0).sum() : 0));
        System.out.println("  Items: " + (scene.getItems() != null ? scene.getItems().size() : 0));
    }

    private static void saveSceneContent(Scene scene, BufferedWriter writer) throws IOException {
        // SubScene Conditions (determines when this SubScene is loaded)
        Map<String, Boolean> subSceneConditions = scene.getSubSceneConditions();
        if (subSceneConditions != null && !subSceneConditions.isEmpty()) {
            writer.write("#Conditions:\n");
            for (Map.Entry<String, Boolean> entry : subSceneConditions.entrySet()) {
                writer.write("-" + entry.getKey() + " = " + entry.getValue() + "\n");
            }
            writer.write("\n");
        }

        // Background Images (new format with conditions)
        List<ConditionalImage> bgImages = scene.getBackgroundImages();
        if (bgImages != null && !bgImages.isEmpty()) {
            writer.write("#BackgroundImages:\n");
            for (ConditionalImage img : bgImages) {
                writer.write("-Image:\n");
                writer.write("--Name: " + img.getName() + "\n");
                writer.write("--Path: " + img.getImagePath() + "\n");
                writer.write("--ShowIfTrue: " + img.isShowIfTrue() + "\n");

                // Write flip settings and conditions according to Schema
                Map<String, Boolean> conditions = img.getConditions();
                if (conditions != null && !conditions.isEmpty()) {
                    writer.write("--Conditions:\n");
                    // Write flip settings INSIDE conditions section as per Schema
                    writer.write("---FlipHorizontally = " + img.isFlipHorizontally() + "\n");
                    writer.write("---FlipVertically = " + img.isFlipVertically() + "\n");
                    for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                        writer.write("---" + entry.getKey() + " = " + entry.getValue() + "\n");
                    }
                } else {
                    // If no conditions, still write flip settings in a conditions block
                    writer.write("--Conditions:\n");
                    writer.write("---FlipHorizontally = " + img.isFlipHorizontally() + "\n");
                    writer.write("---FlipVertically = " + img.isFlipVertically() + "\n");
                }
            }
            writer.write("\n");
        } else {
            // MIGRATION: Fallback to old format for backward compatibility
            // Try to get path from legacy field first, then create default ConditionalImage
            String bgPath = scene.getBackgroundImagePath();
            if (bgPath == null || bgPath.isEmpty()) {
                bgPath = "default.png";
            }
            writer.write("#Backgroundimage:\n");
            writer.write("-" + bgPath + "\n\n");
        }
        
        // KeyAreas (in correct schema order!)
        List<KeyArea> keyAreas = scene.getKeyAreas();
        if (keyAreas != null) {
            for (KeyArea area : keyAreas) {
                if (area == null) continue;

                writer.write("#KeyArea:\n");
                writer.write("-Type: " + (area.getType() != null ? area.getType() : "INTERACTION") + "\n");
                writer.write("-Name: " + (area.getName() != null ? area.getName() : "unnamed") + ";\n");
            
            // MouseHover
            writer.write("-MouseHover\n");
            writer.write("--conditions\n");

            Map<String, String> hoverConditions = area.getHoverDisplayConditions();
            System.out.println("DEBUG: Saving hover conditions for " + area.getName() + ": " + hoverConditions);
            if (hoverConditions != null && !hoverConditions.isEmpty()) {
                for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
                    String condition = entry.getKey();
                    String displayText = entry.getValue();

                    System.out.println("  Writing: " + condition + " -> " + displayText);

                    // Split condition by AND and write each on separate line
                    String[] conditionParts = condition.split(" AND ");
                    for (String condPart : conditionParts) {
                        writer.write("---" + condPart.trim() + ";\n");
                    }

                    writer.write("----Display:\n");
                    writer.write("------\"" + displayText + "\"\n");
                }
            } else {
                // Default
                System.out.println("  No hover conditions, using default: " + area.getName());
                writer.write("---none;\n");
                writer.write("----Display:\n");
                writer.write("------\"" + area.getName() + "\"\n");
            }

            // Image
            writer.write("-Image\n");
            writer.write("--conditions:\n");

            Map<String, String> imageConditions = area.getImageConditions();
            if (imageConditions != null && !imageConditions.isEmpty()) {
                for (Map.Entry<String, String> entry : imageConditions.entrySet()) {
                    String condition = entry.getKey();
                    String imagePath = entry.getValue();

                    // Split condition by AND and write each on separate line
                    String[] conditionParts = condition.split(" AND ");
                    for (String condPart : conditionParts) {
                        writer.write("---" + condPart.trim() + ";\n");
                    }

                    writer.write("----Image: " + imagePath + ";\n");
                }
            } else {
                // Default
                writer.write("---none\n");
                writer.write("----Image: default.png;\n");
            }

            // Actions
            writer.write("#Actions:\n");
            
            Map<String, KeyArea.ActionHandler> actions = area.getActions();
            if (actions != null && !actions.isEmpty()) {
                for (Map.Entry<String, KeyArea.ActionHandler> actionEntry : actions.entrySet()) {
                    String actionName = actionEntry.getKey();
                    KeyArea.ActionHandler handler = actionEntry.getValue();
                    
                    writer.write("-" + actionName + "\n");
                    writer.write("--conditions\n");
                    
                    Map<String, String> conditions = handler.getConditionalResults();
                    if (conditions != null && !conditions.isEmpty()) {
                        for (Map.Entry<String, String> condEntry : conditions.entrySet()) {
                            String condition = condEntry.getKey();
                            String result = condEntry.getValue();

                            // Split condition by AND and write each on separate line
                            String[] conditionParts = condition.split(" AND ");
                            for (String condPart : conditionParts) {
                                writer.write("---" + condPart.trim() + ";\n");
                            }

                            if (result.startsWith("#Dialog:")) {
                                writer.write("----#Dialog:\n");
                                writer.write("------" + result.substring(8).trim() + "\n");
                            } else if (result.startsWith("##load")) {
                                writer.write("----" + result + "\n");
                            } else if (result.startsWith("#SetBoolean:")) {
                                writer.write("----" + result + "\n");
                            } else {
                                writer.write("----" + result + "\n");
                            }
                        }
                    } else {
                        // Default: none condition with dialog
                        writer.write("---none\n");
                        writer.write("----#Dialog:\n");
                        writer.write("------dialog-" + area.getName().toLowerCase() + "-" + actionName.toLowerCase() + "\n");
                    }
                }
            } else {
                // Default: Just "Anschauen"
                writer.write("-Anschauen\n");
                writer.write("--conditions\n");
                writer.write("---none\n");
                writer.write("----#Dialog:\n");
                writer.write("------dialog-" + area.getName().toLowerCase() + "-anschauen\n");
            }
            
            // Location (comes LAST! with ##Location:)
            writer.write("##Location:\n");
            List<Point> points = area.getPoints();
            if (points != null) {
                for (Point p : points) {
                    if (p != null) {
                        writer.write("###\n");
                        writer.write("--x = " + p.x + ";\n");
                        writer.write("--y = " + p.y + ";\n");
                    }
                }
            }

            writer.write("\n");
            }
        }
        
        // Dialogs
        Map<String, String> dialogs = scene.getDialogs();
        if (dialogs != null && !dialogs.isEmpty()) {
            writer.write("#Dialogs:\n");
            for (Map.Entry<String, String> entry : dialogs.entrySet()) {
                if (entry.getKey() != null) {
                    writer.write("-" + entry.getKey() + "\n");
                    String value = entry.getValue();
                    if (value != null) {
                        String[] lines = value.split("\n");
                        for (String line : lines) {
                            writer.write("--" + line + "\n");
                        }
                    }
                    writer.write("\n");
                }
            }
        }

        // Items
        List<Item> items = scene.getItems();
        if (items != null && !items.isEmpty()) {
            writer.write("#Items:\n");
            for (Item item : items) {
                if (item != null && item.getName() != null) {
                    writer.write("-" + item.getName() + "\n");
                }
            }
            writer.write("\n");
        }

        // Selected Item (editor selection - persists across sessions)
        Item selectedItem = scene.getSelectedItem();
        if (selectedItem != null && selectedItem.getName() != null) {
            writer.write("#SelectedItem:\n");
            writer.write("-" + selectedItem.getName() + "\n");
            writer.write("\n");
        }
    }

    /**
     * Overloaded method - saves scene to default location
     */
    /**
     * Save scene to DEFAULT file: resources/scenes/[SceneName].txt
     * Use this in EDITOR MODE when saving scene definitions/templates
     * This is the DEFAULT version that will be loaded by the editor
     */
    public static void saveScene(Scene scene) throws IOException {
        String filename = ResourcePathHelper.resolvePath("scenes/" + scene.getName() + ".txt");
        saveScene(scene, filename);
        System.out.println("✓ Saved to DEFAULT: " + filename);
    }

    /**
     * @deprecated Use saveScene() instead - <name>.txt is now the DEFAULT
     */
    @Deprecated
    public static void saveSceneToDefault(Scene scene) throws IOException {
        saveScene(scene);
    }

    /**
     * @deprecated Progress is now managed differently - _progress.txt files are no longer used
     */
    @Deprecated
    public static void saveSceneToProgress(Scene scene) throws IOException {
        System.out.println("⚠️  WARNING: saveSceneToProgress() is deprecated - Progress is managed differently now");
        // Do nothing - progress files are no longer used
    }
}