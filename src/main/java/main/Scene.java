package main;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    private String name;
    private String backgroundImagePath; // Legacy field, kept for backwards compatibility
    private List<ConditionalImage> backgroundImages; // New: multiple conditional images
    private List<KeyArea> keyAreas;
    private List<Path> paths;
    private Map<String, String> dialogs; // dialogName -> dialogText
    private List<Item> items; // Items placed in this scene
    private Item selectedItem; // Item currently selected in editor (has mouse priority)
    private Map<String, Boolean> subSceneConditions; // Conditions that determine when this SubScene is loaded

    public Scene(String name) {
        this.name = name;
        this.backgroundImages = new ArrayList<>();
        this.keyAreas = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.dialogs = new HashMap<>();
        this.items = new ArrayList<>();
        this.subSceneConditions = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * @deprecated Use getBackgroundImages() instead. This method is kept for backwards compatibility.
     * Returns the path of the first background image, or null if no images exist.
     */
    @Deprecated
    public String getBackgroundImagePath() {
        // For backwards compatibility, return the first image path if available
        if (backgroundImages != null && !backgroundImages.isEmpty()) {
            return backgroundImages.get(0).getImagePath();
        }
        return backgroundImagePath;
    }

    /**
     * @deprecated Use addBackgroundImage() instead. This method is kept for backwards compatibility.
     */
    @Deprecated
    public void setBackgroundImagePath(String path) {
        this.backgroundImagePath = path;
        // Also update the new system for consistency
        if (backgroundImages.isEmpty()) {
            ConditionalImage img = new ConditionalImage(path, "Default");
            backgroundImages.add(img);
        } else {
            backgroundImages.get(0).setImagePath(path);
        }
    }

    /**
     * Get all conditional background images for this scene.
     */
    public List<ConditionalImage> getBackgroundImages() {
        return backgroundImages;
    }

    /**
     * Set all conditional background images for this scene.
     */
    public void setBackgroundImages(List<ConditionalImage> images) {
        this.backgroundImages = images;
    }

    /**
     * Add a conditional background image to this scene.
     */
    public void addBackgroundImage(ConditionalImage image) {
        this.backgroundImages.add(image);
    }

    /**
     * Remove a conditional background image from this scene.
     */
    public void removeBackgroundImage(ConditionalImage image) {
        this.backgroundImages.remove(image);
    }

    /**
     * Get the background image that should be displayed based on current conditions.
     * @param gameProgress The current game progress containing condition states
     * @return The image path to display, or null if no image matches
     */
    public String getCurrentBackgroundImagePath(GameProgress gameProgress) {
        // Check all conditional images in order
        for (ConditionalImage img : backgroundImages) {
            if (img.shouldDisplay(gameProgress)) {
                return img.getImagePath();
            }
        }

        // Fallback to legacy backgroundImagePath if no conditional images match
        if (backgroundImagePath != null && !backgroundImagePath.isEmpty()) {
            return backgroundImagePath;
        }

        // Last resort: return first image path if available
        if (!backgroundImages.isEmpty()) {
            return backgroundImages.get(0).getImagePath();
        }

        return null;
    }
    
    public void addKeyArea(KeyArea area) {
        keyAreas.add(area);
    }
    
    public void addPath(Path path) {
        paths.add(path);
    }
    
    public void addDialog(String name, String text) {
        dialogs.put(name, text);
    }
    
    public String getDialog(String name) {
        return dialogs.get(name);
    }
    
    public Map<String, String> getDialogs() {
        return dialogs;
    }

    public List<KeyArea> getKeyAreas() {
        return keyAreas;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    /**
     * Find item by name
     */
    public Item getItemByName(String name) {
        for (Item item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get item at specific point using polygon-based click detection
     * Selected item has priority and is checked first
     */
    public Item getItemAt(Point point) {
        // Check selected item first (if it exists and is visible)
        if (selectedItem != null && selectedItem.isVisible() && selectedItem.containsPoint(point)) {
            return selectedItem;
        }

        // Then check other items in reverse order (last added = top layer)
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item != selectedItem && item.isVisible() && item.containsPoint(point)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Set the selected item (for editor - gives mouse priority)
     */
    public void setSelectedItem(Item item) {
        this.selectedItem = item;
    }

    /**
     * Get the currently selected item
     */
    public Item getSelectedItem() {
        return selectedItem;
    }
    
    /**
     * Get KeyArea at specific point
     */
    public KeyArea getKeyAreaAt(Point point) {
        for (KeyArea area : keyAreas) {
            if (area.contains(point)) {
                return area;
            }
        }
        return null;
    }

    /**
     * Get the conditions for this SubScene.
     * The SubScene will only be loaded if all conditions match the game progress.
     */
    public Map<String, Boolean> getSubSceneConditions() {
        return subSceneConditions;
    }

    /**
     * Set the conditions for this SubScene.
     */
    public void setSubSceneConditions(Map<String, Boolean> subSceneConditions) {
        this.subSceneConditions = subSceneConditions;
    }

    /**
     * Add a condition to this SubScene.
     */
    public void addSubSceneCondition(String conditionName, boolean requiredValue) {
        this.subSceneConditions.put(conditionName, requiredValue);
    }

    /**
     * Remove a condition from this SubScene.
     */
    public void removeSubSceneCondition(String conditionName) {
        this.subSceneConditions.remove(conditionName);
    }

    @Override
    public String toString() {
        return "Scene{name='" + name + "', keyAreas=" + keyAreas.size() + ", paths=" + paths.size() + ", dialogs=" + dialogs.size() + ", items=" + items.size() + "}";
    }
}