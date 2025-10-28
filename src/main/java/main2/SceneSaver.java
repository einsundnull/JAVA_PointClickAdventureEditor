package main2;

import java.awt.Point;
import java.io.*;
import java.util.*;

public class SceneSaver {
    
    public static void saveScene(Scene scene, String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        
        // Background
        writer.write("#Backgroundimage:\n");
        writer.write("-" + scene.getBackgroundImagePath() + "\n\n");
        
        // KeyAreas (in correct schema order!)
        for (KeyArea area : scene.getKeyAreas()) {
            writer.write("#KeyArea:\n");
            writer.write("-Type: " + area.getType() + "\n");
            writer.write("-Name: " + area.getName() + ";\n");
            
            // MouseHover
            writer.write("-MouseHover\n");
            writer.write("--conditions\n");
            
            Map<String, String> hoverConditions = area.getHoverDisplayConditions();
            if (hoverConditions != null && !hoverConditions.isEmpty()) {
                for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
                    String condition = entry.getKey();
                    String displayText = entry.getValue();
                    
                    writer.write("---" + condition + ";\n");
                    writer.write("----Display:\n");
                    writer.write("------\"" + displayText + "\"\n");
                }
            } else {
                // Default
                writer.write("---none\n");
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

                    writer.write("---" + condition + ";\n");
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
            for (Point p : points) {
                writer.write("###\n");
                writer.write("--x = " + p.x + ";\n");
                writer.write("--y = " + p.y + ";\n");
            }
            
            writer.write("\n");
        }
        
        // Dialogs
        Map<String, String> dialogs = scene.getDialogs();
        if (!dialogs.isEmpty()) {
            writer.write("#Dialogs:\n");
            for (Map.Entry<String, String> entry : dialogs.entrySet()) {
                writer.write("-" + entry.getKey() + "\n");
                String[] lines = entry.getValue().split("\n");
                for (String line : lines) {
                    writer.write("--" + line + "\n");
                }
                writer.write("\n");
            }
        }

        // Items
        List<Item> items = scene.getItems();
        if (!items.isEmpty()) {
            writer.write("#Items:\n");
            for (Item item : items) {
                writer.write("-" + item.getName() + "\n");
            }
            writer.write("\n");
        }

        writer.close();
        System.out.println("Scene saved to: " + filename);
        System.out.println("  KeyAreas: " + scene.getKeyAreas().size());
        System.out.println("  Total points: " + scene.getKeyAreas().stream()
            .mapToInt(a -> a.getPoints().size()).sum());
        System.out.println("  Items: " + scene.getItems().size());
    }
}