package main2;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // Click detection (similar to KeyArea)
    private List<Point> clickAreaPoints; // Polygon points for click detection
    private Polygon clickAreaPolygon;

    // KeyArea-like properties
    private Map<String, String> imageConditions; // condition -> image path
    private Map<String, KeyArea.ActionHandler> actions; // action name -> handler
    private Map<String, String> hoverDisplayConditions; // condition -> display text

    public Item(String name) {
        this.name = name;
        this.position = new Point(0, 0);
        this.imageFileName = "";
        this.imageFilePath = "";
        this.conditions = new HashMap<>();
        this.isInInventory = false;
        this.width = 100;  // Default width
        this.height = 100; // Default height
        this.clickAreaPoints = new ArrayList<>();
        this.imageConditions = new HashMap<>();
        this.actions = new HashMap<>();
        this.hoverDisplayConditions = new HashMap<>();

        // Create default rectangular click area
        createDefaultClickArea();
    }

    /**
     * Creates a default rectangular click area based on position and size
     */
    private void createDefaultClickArea() {
        clickAreaPoints.clear();
        int x = position.x;
        int y = position.y;
        clickAreaPoints.add(new Point(x, y));
        clickAreaPoints.add(new Point(x + width, y));
        clickAreaPoints.add(new Point(x + width, y + height));
        clickAreaPoints.add(new Point(x, y + height));
        updateClickAreaPolygon();
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
        if (clickAreaPoints.isEmpty() || clickAreaPoints.size() == 4) {
            // Update default rectangular click area
            createDefaultClickArea();
        }
    }

    public void setPosition(int x, int y) {
        setPosition(new Point(x, y));
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
        if (clickAreaPoints.isEmpty() || clickAreaPoints.size() == 4) {
            // Update default rectangular click area
            createDefaultClickArea();
        }
    }

    // ImageConditions methods
    public Map<String, String> getImageConditions() {
        return imageConditions;
    }

    public void addImageCondition(String condition, String imagePath) {
        imageConditions.put(condition, imagePath);
    }

    public String getCurrentImagePath() {
        // Check image conditions similar to KeyArea
        for (Map.Entry<String, String> entry : imageConditions.entrySet()) {
            String condition = entry.getKey();
            String imagePath = entry.getValue();

            if (condition.equals("none") || evaluateCondition(condition)) {
                return imagePath;
            }
        }
        // Fallback to default image path
        return imageFilePath;
    }

    // HoverDisplayConditions methods
    public Map<String, String> getHoverDisplayConditions() {
        return hoverDisplayConditions;
    }

    public void addHoverDisplayCondition(String condition, String displayText) {
        hoverDisplayConditions.put(condition, displayText);
    }

    public String getHoverDisplayText() {
        for (Map.Entry<String, String> entry : hoverDisplayConditions.entrySet()) {
            String condition = entry.getKey();
            String displayText = entry.getValue();

            if (condition.equals("none") || evaluateCondition(condition)) {
                return displayText;
            }
        }
        return name; // Default to item name
    }

    // Actions methods
    public Map<String, KeyArea.ActionHandler> getActions() {
        return actions;
    }

    public void addAction(String actionName, KeyArea.ActionHandler handler) {
        actions.put(actionName, handler);
    }

    public String performAction(String actionName, GameProgress progress) {
        KeyArea.ActionHandler handler = actions.get(actionName);
        if (handler != null) {
            return handler.execute(progress);
        }
        return null;
    }

    // Evaluate condition helper
    private boolean evaluateCondition(String condition) {
        if (condition.equals("none")) {
            return true;
        }

        // Parse "fieldName = true/false"
        String[] parts = condition.split("=");
        if (parts.length == 2) {
            String fieldName = parts[0].trim();
            boolean expectedValue = Boolean.parseBoolean(parts[1].trim());
            boolean actualValue = Conditions.getCondition(fieldName);
            return actualValue == expectedValue;
        }

        return false;
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

    // Click Area methods
    public List<Point> getClickAreaPoints() {
        return clickAreaPoints;
    }

    public void addClickAreaPoint(Point p) {
        clickAreaPoints.add(p);
        updateClickAreaPolygon();
    }

    public void addClickAreaPoint(int x, int y) {
        addClickAreaPoint(new Point(x, y));
    }

    public void updateClickAreaPolygon() {
        if (clickAreaPoints.isEmpty()) {
            clickAreaPolygon = null;
            return;
        }

        int[] xPoints = new int[clickAreaPoints.size()];
        int[] yPoints = new int[clickAreaPoints.size()];

        for (int i = 0; i < clickAreaPoints.size(); i++) {
            xPoints[i] = clickAreaPoints.get(i).x;
            yPoints[i] = clickAreaPoints.get(i).y;
        }

        clickAreaPolygon = new Polygon(xPoints, yPoints, clickAreaPoints.size());
    }

    public Polygon getClickAreaPolygon() {
        return clickAreaPolygon;
    }

    public boolean containsPoint(Point point) {
        if (clickAreaPolygon == null) {
            // Fallback to simple bounds check
            int x = position.x;
            int y = position.y;
            return point.x >= x && point.x <= x + width &&
                   point.y >= y && point.y <= y + height;
        }
        return clickAreaPolygon.contains(point);
    }

    @Override
    public String toString() {
        return name + " (x=" + position.x + ", y=" + position.y + ")";
    }
}
