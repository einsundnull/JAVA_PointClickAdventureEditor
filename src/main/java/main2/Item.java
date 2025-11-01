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
    private boolean hasCustomClickArea = false; // True if points were manually edited or loaded from file

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

        // Create default isInInventory condition for this item (runtime only, not saved to conditions.txt)
        String inventoryConditionName = "isInInventory_" + name;
        if (!Conditions.conditionExists(inventoryConditionName)) {
            Conditions.addConditionRuntimeOnly(inventoryConditionName, false);
            System.out.println("Item constructor: Created runtime condition " + inventoryConditionName);
        }

        // Don't create default click area yet - wait until image is loaded
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

    /**
     * Updates existing click area points to match current position and size
     * Only used for non-custom click areas (when user drags item or resizes)
     */
    private void updateClickAreaToMatchBounds() {
        if (clickAreaPoints.size() != 4) {
            // If not a simple rectangle, don't update
            return;
        }

        // Get center position
        int centerX = position.x;
        int centerY = position.y;
        int halfWidth = width / 2 + 10;  // +10px padding
        int halfHeight = height / 2 + 10; // +10px padding

        // Update the 4 corner points
        clickAreaPoints.get(0).setLocation(centerX - halfWidth, centerY - halfHeight);
        clickAreaPoints.get(1).setLocation(centerX + halfWidth, centerY - halfHeight);
        clickAreaPoints.get(2).setLocation(centerX + halfWidth, centerY + halfHeight);
        clickAreaPoints.get(3).setLocation(centerX - halfWidth, centerY + halfHeight);

        updateClickAreaPolygon();
    }

    /**
     * Creates a default click area based on actual image dimensions + 10px padding
     * Only creates if user hasn't customized the click area
     */
    public void createClickAreaFromImage() {
        System.out.println("createClickAreaFromImage called for: " + name +
                         ", current points: " + clickAreaPoints.size() +
                         ", hasCustomClickArea: " + hasCustomClickArea);

        // Only create if user hasn't customized the click area
        if (hasCustomClickArea) {
            System.out.println("  Skipping - user has custom click area");
            return;
        }

        clickAreaPoints.clear();

        // Get image dimensions
        int imgWidth = width;
        int imgHeight = height;

        // Try to load actual image to get real dimensions
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            try {
                java.io.File imageFile = new java.io.File(imageFilePath);
                if (imageFile.exists()) {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(imageFile);
                    if (img != null) {
                        imgWidth = img.getWidth();
                        imgHeight = img.getHeight();
                        // Update item size to match image
                        this.width = imgWidth;
                        this.height = imgHeight;
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load image for click area: " + e.getMessage());
            }
        }

        // Create click area with 10px padding around image
        int centerX = position.x;
        int centerY = position.y;
        int halfWidth = imgWidth / 2 + 10;  // +10px padding
        int halfHeight = imgHeight / 2 + 10; // +10px padding

        clickAreaPoints.add(new Point(centerX - halfWidth, centerY - halfHeight));
        clickAreaPoints.add(new Point(centerX + halfWidth, centerY - halfHeight));
        clickAreaPoints.add(new Point(centerX + halfWidth, centerY + halfHeight));
        clickAreaPoints.add(new Point(centerX - halfWidth, centerY + halfHeight));

        updateClickAreaPolygon();
        System.out.println("Created click area for " + name + " with dimensions: " +
                         (halfWidth*2) + "x" + (halfHeight*2));
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
        // Don't auto-update click area - polygons should not change when moving items
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
        // Don't auto-create here - let caller decide when to create click area
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
        // Always read from condition to ensure consistency
        String inventoryConditionName = "isInInventory_" + name;
        if (Conditions.conditionExists(inventoryConditionName)) {
            return Conditions.getCondition(inventoryConditionName);
        }
        return isInInventory; // Fallback to field if condition doesn't exist
    }

    public void setInInventory(boolean inInventory) {
        this.isInInventory = inInventory;
        // Also update the condition
        String inventoryConditionName = "isInInventory_" + name;
        if (Conditions.conditionExists(inventoryConditionName)) {
            Conditions.setCondition(inventoryConditionName, inInventory);
        }
    }

    /**
     * Synchronizes the isInInventory field with its corresponding condition
     */
    public void syncIsInInventoryFromCondition() {
        String inventoryConditionName = "isInInventory_" + name;
        if (Conditions.conditionExists(inventoryConditionName)) {
            this.isInInventory = Conditions.getCondition(inventoryConditionName);
        }
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
        // Don't auto-update click area - polygons should not change when resizing items
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
        System.out.println("DEBUG getHoverDisplayText() for item: " + name);
        System.out.println("  hoverDisplayConditions size: " + hoverDisplayConditions.size());
        for (Map.Entry<String, String> entry : hoverDisplayConditions.entrySet()) {
            String condition = entry.getKey();
            String displayText = entry.getValue();
            System.out.println("  Checking condition: '" + condition + "' -> '" + displayText + "'");

            if (condition.equals("none") || evaluateCondition(condition)) {
                System.out.println("  -> Condition matched! Returning: " + displayText);
                return displayText;
            }
        }
        System.out.println("  -> No condition matched, returning name: " + name);
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
        // Use getter method to read from condition
        if (isInInventory()) {
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
        // Mark as custom when user adds points
        hasCustomClickArea = true;
    }

    public void addClickAreaPoint(int x, int y) {
        addClickAreaPoint(new Point(x, y));
    }

    public boolean hasCustomClickArea() {
        return hasCustomClickArea;
    }

    public void setHasCustomClickArea(boolean hasCustom) {
        this.hasCustomClickArea = hasCustom;
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
