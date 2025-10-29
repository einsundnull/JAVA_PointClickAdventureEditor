package main2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Saves items to .txt files in resources/items/
 */
public class ItemSaver {

    public static void saveItem(Item item, String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs(); // Create directories if needed

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        // Name
        writer.write("#Name:\n");
        writer.write("-" + item.getName() + "\n\n");

        // Image File
        writer.write("#ImageFile:\n");
        writer.write("-" + item.getImageFileName() + "\n\n");

        // Image Path
        writer.write("#ImagePath:\n");
        writer.write("-" + item.getImageFilePath() + "\n\n");

        // Position
        writer.write("#Position:\n");
        writer.write("-x = " + item.getPosition().x + ";\n");
        writer.write("-y = " + item.getPosition().y + ";\n\n");

        // Size
        writer.write("#Size:\n");
        writer.write("-width = " + item.getWidth() + ";\n");
        writer.write("-height = " + item.getHeight() + ";\n\n");

        // Conditions
        writer.write("#Conditions:\n");
        Map<String, Boolean> conditions = item.getConditions();
        if (conditions.isEmpty()) {
            writer.write("-none\n");
        } else {
            for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                writer.write("-" + entry.getKey() + " = " + entry.getValue() + ";\n");
            }
        }
        writer.write("\n");

        // IsInInventory
        writer.write("#IsInInventory:\n");
        writer.write("-" + item.isInInventory() + "\n\n");

        // MouseHover (similar to KeyArea)
        writer.write("#MouseHover:\n");
        writer.write("--conditions\n");
        Map<String, String> hoverConditions = item.getHoverDisplayConditions();
        if (hoverConditions != null && !hoverConditions.isEmpty()) {
            for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
                String condition = entry.getKey();
                String displayText = entry.getValue();
                writer.write("---" + condition + ";\n");
                writer.write("----Display:\n");
                writer.write("------\"" + displayText + "\"\n");
            }
        } else {
            writer.write("---none\n");
            writer.write("----Display:\n");
            writer.write("------\"" + item.getName() + "\"\n");
        }
        writer.write("\n");

        // Image Conditions
        writer.write("#ImageConditions:\n");
        writer.write("--conditions:\n");
        Map<String, String> imageConditions = item.getImageConditions();
        if (imageConditions != null && !imageConditions.isEmpty()) {
            for (Map.Entry<String, String> entry : imageConditions.entrySet()) {
                String condition = entry.getKey();
                String imagePath = entry.getValue();
                writer.write("---" + condition + ";\n");
                writer.write("----Image: " + imagePath + ";\n");
            }
        } else {
            writer.write("---none\n");
            writer.write("----Image: " + item.getImageFilePath() + ";\n");
        }
        writer.write("\n");

        // Click Area (Polygon points)
        writer.write("#ClickArea:\n");
        java.util.List<java.awt.Point> clickPoints = item.getClickAreaPoints();
        if (clickPoints != null && !clickPoints.isEmpty()) {
            for (java.awt.Point p : clickPoints) {
                writer.write("###\n");
                writer.write("--x = " + p.x + ";\n");
                writer.write("--y = " + p.y + ";\n");
            }
        } else {
            // Default rectangular click area
            writer.write("###\n");
            writer.write("--x = " + item.getPosition().x + ";\n");
            writer.write("--y = " + item.getPosition().y + ";\n");
            writer.write("###\n");
            writer.write("--x = " + (item.getPosition().x + item.getWidth()) + ";\n");
            writer.write("--y = " + item.getPosition().y + ";\n");
            writer.write("###\n");
            writer.write("--x = " + (item.getPosition().x + item.getWidth()) + ";\n");
            writer.write("--y = " + (item.getPosition().y + item.getHeight()) + ";\n");
            writer.write("###\n");
            writer.write("--x = " + item.getPosition().x + ";\n");
            writer.write("--y = " + (item.getPosition().y + item.getHeight()) + ";\n");
        }
        writer.write("\n");

        // Actions
        writer.write("#Actions:\n");
        Map<String, KeyArea.ActionHandler> actions = item.getActions();
        if (actions != null && !actions.isEmpty()) {
            for (Map.Entry<String, KeyArea.ActionHandler> actionEntry : actions.entrySet()) {
                String actionName = actionEntry.getKey();
                KeyArea.ActionHandler handler = actionEntry.getValue();

                writer.write("-" + actionName + "\n");
                writer.write("--conditions\n");

                Map<String, String> actionConditions = handler.getConditionalResults();
                if (actionConditions != null && !actionConditions.isEmpty()) {
                    for (Map.Entry<String, String> condEntry : actionConditions.entrySet()) {
                        String condition = condEntry.getKey();
                        String result = condEntry.getValue();

                        writer.write("---" + condition + ";\n");

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
                    writer.write("---none\n");
                    writer.write("----#Dialog:\n");
                    writer.write("------dialog-" + item.getName().toLowerCase() + "-" + actionName.toLowerCase() + "\n");
                }
            }
        } else {
            // Default action
            writer.write("-Anschauen\n");
            writer.write("--conditions\n");
            writer.write("---none\n");
            writer.write("----#Dialog:\n");
            writer.write("------dialog-" + item.getName().toLowerCase() + "-anschauen\n");
        }

        writer.close();
        System.out.println("Saved item: " + item.getName() + " to " + filename);
    }

    /**
     * Save item to resources/items/[ItemName].txt
     */
    public static void saveItemByName(Item item) throws IOException {
        String filename = "resources/items/" + item.getName() + ".txt";
        saveItem(item, filename);
    }
}
