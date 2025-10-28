package main2;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an item that can be placed in a scene
 */
public class Item {
    private String name;
    private Point position;
    private String imageFileName;
    private String imageFilePath;
    private Map<String, Boolean> conditions;
    private boolean isInInventory;
    private int width;  // Image display width
    private int height; // Image display height

    public Item(String name) {
        this.name = name;
        this.position = new Point(0, 0);
        this.imageFileName = "";
        this.imageFilePath = "";
        this.conditions = new HashMap<>();
        this.isInInventory = false;
        this.width = 100;  // Default width
        this.height = 100; // Default height
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setPosition(int x, int y) {
        this.position = new Point(x, y);
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageFilePath() {
        return imageFilePath;
    }

    public void setImageFilePath(String imageFilePath) {
        this.imageFilePath = imageFilePath;
    }

    public Map<String, Boolean> getConditions() {
        return conditions;
    }

    public void addCondition(String conditionName, boolean value) {
        conditions.put(conditionName, value);
    }

    public void removeCondition(String conditionName) {
        conditions.remove(conditionName);
    }

    public boolean isInInventory() {
        return isInInventory;
    }

    public void setInInventory(boolean inInventory) {
        this.isInInventory = inInventory;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Check if item should be visible based on conditions
     */
    public boolean isVisible() {
        if (isInInventory) {
            return false; // Don't show in scene if in inventory
        }

        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            String conditionName = entry.getKey();
            boolean expectedValue = entry.getValue();

            boolean actualValue = Conditions.getCondition(conditionName);
            if (actualValue != expectedValue) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return name + " (x=" + position.x + ", y=" + position.y + ")";
    }
}
