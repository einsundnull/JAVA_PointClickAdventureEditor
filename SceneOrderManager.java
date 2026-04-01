package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the display order of Scenes and SubScenes in the editor.
 * Stores order in resources/scenes/scene_order.txt
 */
public class SceneOrderManager {

    private static final String ORDER_FILE = ResourcePathHelper.resolvePath("scenes/scene_order.txt");

    /**
     * Load the scene order from file.
     * Returns a map: SceneName -> List of SubScene names in order
     * Top-level scenes are in a special entry with key "__ROOT__"
     */
    public static Map<String, List<String>> loadOrder() {
        Map<String, List<String>> order = new LinkedHashMap<>();

        File orderFile = new File(ORDER_FILE);
        if (!orderFile.exists()) {
            return order; // Return empty map if no order file exists
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(orderFile))) {
            String line;
            String currentScene = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                if (line.startsWith("Scene:")) {
                    // New scene section
                    currentScene = line.substring(6).trim();
                    order.put(currentScene, new ArrayList<>());
                } else if (currentScene != null && line.startsWith("  -")) {
                    // SubScene entry
                    String subSceneName = line.substring(3).trim();
                    order.get(currentScene).add(subSceneName);
                }
            }

            System.out.println("✓ Loaded scene order from: " + ORDER_FILE);
        } catch (IOException e) {
            System.err.println("Error loading scene order: " + e.getMessage());
        }

        return order;
    }

    /**
     * Save the scene order to file.
     *
     * @param sceneOrder Map: SceneName -> List of SubScene names in order
     */
    public static void saveOrder(Map<String, List<String>> sceneOrder) {
        File orderFile = new File(ORDER_FILE);

        // Create parent directory if it doesn't exist
        orderFile.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(orderFile))) {
            writer.println("# Scene and SubScene display order");
            writer.println("# This file controls the order of scenes in the editor tree view");
            writer.println();

            // Write scene order
            for (Map.Entry<String, List<String>> entry : sceneOrder.entrySet()) {
                String sceneName = entry.getKey();
                List<String> subScenes = entry.getValue();

                writer.println("Scene:" + sceneName);

                // Write subscene order
                for (String subSceneName : subScenes) {
                    writer.println("  -" + subSceneName);
                }

                writer.println(); // Empty line between scenes
            }

            System.out.println("✓ Saved scene order to: " + ORDER_FILE);
        } catch (IOException e) {
            System.err.println("Error saving scene order: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract the current order from a JTree model.
     *
     * @param rootNode The root node of the tree
     * @return Map of scene order
     */
    public static Map<String, List<String>> extractOrderFromTree(javax.swing.tree.DefaultMutableTreeNode rootNode) {
        Map<String, List<String>> order = new LinkedHashMap<>();

        // Iterate through all scene nodes
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            javax.swing.tree.DefaultMutableTreeNode sceneNode =
                (javax.swing.tree.DefaultMutableTreeNode) rootNode.getChildAt(i);

            Object userObj = sceneNode.getUserObject();
            if (!(userObj instanceof SceneListItem)) {
                continue;
            }

            SceneListItem sceneItem = (SceneListItem) userObj;
            if (!sceneItem.isSubScene()) {
                // This is a Scene node
                String sceneName = sceneItem.getName();
                List<String> subScenes = new ArrayList<>();

                // Get all subscene children
                for (int j = 0; j < sceneNode.getChildCount(); j++) {
                    javax.swing.tree.DefaultMutableTreeNode subSceneNode =
                        (javax.swing.tree.DefaultMutableTreeNode) sceneNode.getChildAt(j);

                    Object subObj = subSceneNode.getUserObject();
                    if (subObj instanceof SceneListItem) {
                        SceneListItem subSceneItem = (SceneListItem) subObj;
                        if (subSceneItem.isSubScene()) {
                            subScenes.add(subSceneItem.getName());
                        }
                    }
                }

                order.put(sceneName, subScenes);
            }
        }

        return order;
    }

    /**
     * Apply saved order to a list of scene names.
     * Returns the list sorted according to saved order, with unsorted items at the end.
     */
    public static List<String> applySavedOrder(List<String> items, List<String> savedOrder) {
        if (savedOrder == null || savedOrder.isEmpty()) {
            return items; // No saved order, return as-is
        }

        List<String> result = new ArrayList<>();
        Set<String> addedItems = new HashSet<>();

        // Add items in saved order
        for (String orderedItem : savedOrder) {
            if (items.contains(orderedItem)) {
                result.add(orderedItem);
                addedItems.add(orderedItem);
            }
        }

        // Add any remaining items that weren't in saved order
        for (String item : items) {
            if (!addedItems.contains(item)) {
                result.add(item);
            }
        }

        return result;
    }
}
