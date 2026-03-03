package main;

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
    private String imageFilePath; // Legacy field, kept for backwards compatibility
    private List<ConditionalImage> conditionalImages; // New: multiple conditional images
    private Map<String, Boolean> conditions;
    private boolean isInInventory;
    private boolean isFollowingMouse; // Item follows mouse cursor
    private boolean isFollowingOnMouseClick; // Item follows mouse only when clicked
    private int width;  // Image display width
    private int height; // Image display height

    // Orientation-based images (only used when isFollowingMouse or isFollowingOnMouseClick is true)
    private String imagePathTopLeft;
    private String imagePathTop;
    private String imagePathTopRight;
    private String imagePathLeft;
    private String imagePathMiddle;
    private String imagePathRight;
    private String imagePathBottomLeft;
    private String imagePathBottom;
    private String imagePathBottomRight;

    // Current orientation (runtime only, not saved)
    private String currentOrientation = "Middle";

    // Click detection (similar to KeyArea)
    private List<Point> clickAreaPoints; // Polygon points for click detection
    private Polygon clickAreaPolygon;
    private boolean hasCustomClickArea = false; // True if points were manually edited or loaded from file

    // New: Custom Click Areas and Moving Ranges
    private List<CustomClickArea> customClickAreas; // Multiple custom click areas with conditions
    private List<String> movingRangeNames; // MovingRange references (names) - loaded from MovingRangeManager
    private List<Path> paths; // MovingRange paths
    

    // KeyArea-like properties
    private Map<String, String> imageConditions; // condition -> image path (old system)
    private Map<String, KeyArea.ActionHandler> actions; // action name -> handler
    private Map<String, String> hoverDisplayConditions; // condition -> display text

    // Visibility control
    private boolean visible = true; // Direct visibility control (used by Process system)

    // Editor visibility flags (not saved to file, runtime only)
    private boolean visibleInEditor = true; // Show/hide item image in editor
    private boolean customClickAreaVisibleInEditor = true; // Show/hide custom click area in editor
    private boolean movingRangeVisibleInEditor = true; // Show/hide moving range in editor
    private boolean pathVisibleInEditor = true; // Show/hide path in editor

    public Item(String name) {
        this.name = name;
        this.position = new Point(0, 0);
        this.imageFileName = "";
        this.imageFilePath = "";
        this.conditionalImages = new ArrayList<>();
        this.conditions = new HashMap<>();
        this.isInInventory = false;
        this.isFollowingMouse = false;
        this.isFollowingOnMouseClick = false;
        this.width = 100;  // Default width
        this.height = 100; // Default height
        this.clickAreaPoints = new ArrayList<>();
        this.customClickAreas = new ArrayList<>();
        this.movingRangeNames = new ArrayList<>();  // Only names, not objects
        this.paths = new ArrayList<>();
        this.imageConditions = new HashMap<>();
        this.actions = new HashMap<>();
        this.hoverDisplayConditions = new HashMap<>();

        // Initialize orientation image paths
        this.imagePathTopLeft = "";
        this.imagePathTop = "";
        this.imagePathTopRight = "";
        this.imagePathLeft = "";
        this.imagePathMiddle = "";
        this.imagePathRight = "";
        this.imagePathBottomLeft = "";
        this.imagePathBottom = "";
        this.imagePathBottomRight = "";
        this.currentOrientation = "Middle";

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

        // Auto-save to progress if in game mode
        AutoSaveManager.saveItem(this);
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

    /**
     * @deprecated Use {@link #getConditionalImages()} and {@link ConditionalImage#getImagePath()} instead.
     * This legacy field will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String getImageFilePath() {
        return imageFilePath;
    }

    /**
     * @deprecated Use {@link #addConditionalImage(ConditionalImage)} instead.
     * This legacy field will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
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

        // Auto-save item to progress
        AutoSaveManager.saveItem(this);
    }

    public boolean isFollowingMouse() {
        return isFollowingMouse;
    }

    public void setFollowingMouse(boolean followingMouse) {
        this.isFollowingMouse = followingMouse;
    }

    public boolean isFollowingOnMouseClick() {
        return isFollowingOnMouseClick;
    }

    public void setFollowingOnMouseClick(boolean followingOnMouseClick) {
        this.isFollowingOnMouseClick = followingOnMouseClick;
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
        // auto-update click area - polygons should not change when resizing items
    }

    // ========== MIGRATION: Legacy ImageConditions (deprecated) ==========
    // Use ConditionalImage system instead

    /**
     * @deprecated Use {@link #getConditionalImages()} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Map<String, String> getImageConditions() {
        return imageConditions;
    }

    /**
     * @deprecated Use {@link #addConditionalImage(ConditionalImage)} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void addImageCondition(String condition, String imagePath) {
        imageConditions.put(condition, imagePath);
    }

    public String getCurrentImagePath() {
        System.out.println("DEBUG getCurrentImagePath() for item: " + name);
        System.out.println("  conditionalImages size: " + conditionalImages.size());

        // New system: Check conditional images first
        for (ConditionalImage img : conditionalImages) {
            System.out.println("  Checking ConditionalImage: " + img.getName() + " at path: " + img.getImagePath());
            if (img.shouldDisplay(null)) { // Item doesn't need GameProgress
                System.out.println("    -> Matched! Returning: " + img.getImagePath());
                return img.getImagePath();
            } else {
                System.out.println("    -> Conditions not met");
            }
        }

        // Old system: Check image conditions similar to KeyArea
        System.out.println("  Checking old imageConditions, size: " + imageConditions.size());
        for (Map.Entry<String, String> entry : imageConditions.entrySet()) {
            String condition = entry.getKey();
            String imagePath = entry.getValue();

            if (condition.equals("none") || evaluateCondition(condition)) {
                System.out.println("    -> Old system matched: " + imagePath);
                return imagePath;
            }
        }

        // Fallback to default image path
        System.out.println("  Fallback to imageFilePath: " + imageFilePath);
        return imageFilePath;
    }

    // ========== MIGRATION: Legacy HoverDisplayConditions (deprecated) ==========
    // Use CustomClickArea.hoverText instead

    /**
     * @deprecated Use {@link #getCustomClickAreas()} and {@link CustomClickArea#getHoverText()} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Map<String, String> getHoverDisplayConditions() {
        return hoverDisplayConditions;
    }

    /**
     * @deprecated Use {@link CustomClickArea#setHoverText(String)} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void addHoverDisplayCondition(String condition, String displayText) {
        hoverDisplayConditions.put(condition, displayText);
    }

    /**
     * @deprecated Use {@link #getHoverDisplayText(Point)} instead.
     * This method without point parameter will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
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

    /**
     * Gets the hover display text for this item at a specific point.
     * Checks CustomClickAreas first (new system), then falls back to old system.
     * @param point The mouse position to check
     * @return The hover text to display
     */
    public String getHoverDisplayText(Point point) {
        // NEW SYSTEM: Check CustomClickAreas with conditions
        CustomClickArea activeArea = getCustomClickAreaAt(point);
        if (activeArea != null && activeArea.getHoverText() != null && !activeArea.getHoverText().isEmpty()) {
            return activeArea.getHoverText();
        }

        // FALLBACK: Use old system (hoverDisplayConditions)
        for (Map.Entry<String, String> entry : hoverDisplayConditions.entrySet()) {
            String condition = entry.getKey();
            String displayText = entry.getValue();

            if (condition.equals("none") || evaluateCondition(condition)) {
                return displayText;
            }
        }

        // Default to item name
        return name;
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
        // Check direct visibility flag first
        if (!visible) {
            return false;
        }

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

    /**
     * Set item visibility directly (used by Process system)
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // ========== MIGRATION: Legacy Click Area methods (deprecated) ==========
    // Use CustomClickArea system instead

    /**
     * @deprecated Use {@link #getCustomClickAreas()} and {@link CustomClickArea#getPoints()} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public List<Point> getClickAreaPoints() {
        return clickAreaPoints;
    }

    /**
     * @deprecated Use {@link #addCustomClickArea(CustomClickArea)} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void addClickAreaPoint(Point p) {
        clickAreaPoints.add(p);
        updateClickAreaPolygon();
        // Mark as custom when user adds points
        hasCustomClickArea = true;
    }

    /**
     * @deprecated Use {@link #addCustomClickArea(CustomClickArea)} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void addClickAreaPoint(int x, int y) {
        addClickAreaPoint(new Point(x, y));
    }

    /**
     * @deprecated This flag is no longer needed with CustomClickArea system.
     * Use {@link #getCustomClickAreas()} and check {@code !isEmpty()} instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public boolean hasCustomClickArea() {
        return hasCustomClickArea;
    }

    /**
     * @deprecated This flag is no longer needed with CustomClickArea system.
     * Use {@link #addCustomClickArea(CustomClickArea)} instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setHasCustomClickArea(boolean hasCustom) {
        this.hasCustomClickArea = hasCustom;
    }

    /**
     * @deprecated Use {@link CustomClickArea#updatePolygon()} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
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

    /**
     * @deprecated Use {@link CustomClickArea#getPolygon()} instead.
     * This legacy system will be removed in a future version.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Polygon getClickAreaPolygon() {
        return clickAreaPolygon;
    }

    /**
     * Check if a point is inside this item's click area.
     * MIGRATION: Priority system for backward compatibility.
     * After migration, only CustomClickAreas will be checked.
     */
    public boolean containsPoint(Point point) {
        // PRIORITY 1: Check new CustomClickAreas system first (with polygons)
        if (customClickAreas != null && !customClickAreas.isEmpty()) {
            // Check each CustomClickArea to see if the point is inside
            for (CustomClickArea area : customClickAreas) {
                if (area.containsPoint(point)) {
                    return true;
                }
            }
            // Point is not in any CustomClickArea
            return false;
        }

        // PRIORITY 2: Fallback to old clickAreaPolygon system (backward compatibility)
        // TODO: Remove after migration is complete
        if (clickAreaPolygon != null) {
            return clickAreaPolygon.contains(point);
        }

        // PRIORITY 3: Final fallback to simple bounds check
        int x = position.x;
        int y = position.y;
        return point.x >= x && point.x <= x + width &&
               point.y >= y && point.y <= y + height;
    }

    // ConditionalImage management methods
    public List<ConditionalImage> getConditionalImages() {
        return conditionalImages;
    }

    public void setConditionalImages(List<ConditionalImage> images) {
        this.conditionalImages = images;
    }

    public void addConditionalImage(ConditionalImage image) {
        this.conditionalImages.add(image);
    }

    public void removeConditionalImage(ConditionalImage image) {
        this.conditionalImages.remove(image);
    }

    // === Custom Click Areas ===
    public List<CustomClickArea> getCustomClickAreas() {
        return customClickAreas;
    }

    public void setCustomClickAreas(List<CustomClickArea> customClickAreas) {
        this.customClickAreas = customClickAreas;
    }

    public void addCustomClickArea(CustomClickArea area) {
        this.customClickAreas.add(area);
    }

    public void removeCustomClickArea(CustomClickArea area) {
        this.customClickAreas.remove(area);
    }

    public CustomClickArea getPrimaryCustomClickArea() {
        if (customClickAreas == null || customClickAreas.isEmpty()) {
            return null;
        }
        return customClickAreas.get(0);
    }

    public CustomClickArea ensurePrimaryCustomClickArea() {
        if (customClickAreas == null) {
            customClickAreas = new ArrayList<>();
        }
        if (customClickAreas.isEmpty() || customClickAreas.get(0) == null) {
            CustomClickArea area = new CustomClickArea();
            if (customClickAreas.isEmpty()) {
                customClickAreas.add(area);
            } else {
                customClickAreas.set(0, area);
            }
        }
        return customClickAreas.get(0);
    }

    // === Moving Ranges (REFACTOR: Now stored as name references) ===

    /**
     * Gets the MovingRange names (references)
     */
    public List<String> getMovingRangeNames() {
        return movingRangeNames;
    }

    /**
     * Sets the MovingRange names
     */
    public void setMovingRangeNames(List<String> names) {
        this.movingRangeNames = names;
    }

    /**
     * Adds a MovingRange reference by name
     */
    public void addMovingRangeName(String name) {
        if (name != null && !name.trim().isEmpty() && !movingRangeNames.contains(name)) {
            movingRangeNames.add(name);
        }
    }

    /**
     * Removes a MovingRange reference by name
     */
    public void removeMovingRangeName(String name) {
        movingRangeNames.remove(name);
    }

    /**
     * Gets the actual MovingRange objects (lazy-loaded from MovingRangeManager)
     * This loads the MovingRanges from their files based on the stored names
     */
    public List<MovingRange> getMovingRanges() {
        List<MovingRange> ranges = new ArrayList<>();
        for (String name : movingRangeNames) {
            MovingRange range = MovingRangeManager.get(name);
            if (range != null) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    /**
     * @deprecated Use getMovingRangeNames() instead
     */
    @Deprecated
    public void setMovingRanges(List<MovingRange> movingRanges) {
        // Legacy support - extract names and use new system
        this.movingRangeNames = new ArrayList<>();
        if (movingRanges != null) {
            for (MovingRange range : movingRanges) {
                if (range != null && range.getName() != null) {
                    movingRangeNames.add(range.getName());
                    // Ensure the MovingRange is saved to a file
                    MovingRangeManager.save(range);
                }
            }
        }
    }

    /**
     * @deprecated Use addMovingRangeName() instead
     */
    @Deprecated
    public void addMovingRange(MovingRange range) {
        if (range != null && range.getName() != null) {
            addMovingRangeName(range.getName());
            MovingRangeManager.save(range);
        }
    }

    /**
     * Gets the primary (first) MovingRange
     * Loads from MovingRangeManager based on stored names
     */
    public MovingRange getPrimaryMovingRange() {
        if (movingRangeNames == null || movingRangeNames.isEmpty()) {
            return null;
        }
        return MovingRangeManager.get(movingRangeNames.get(0));
    }

    /**
     * Ensures a primary MovingRange exists
     * Creates a new named MovingRange if needed
     */
    public MovingRange ensurePrimaryMovingRange() {
        if (movingRangeNames == null) {
            movingRangeNames = new ArrayList<>();
        }

        if (movingRangeNames.isEmpty()) {
            // Create a new MovingRange named after the item
            String rangeName = this.getName() + "_range";
            MovingRange range = MovingRangeManager.create(rangeName);
            movingRangeNames.add(rangeName);
            return range;
        }

        return MovingRangeManager.get(movingRangeNames.get(0));
    }

    // === Paths ===
    public List<Path> getPaths() {
        return paths;
    }

    public void setPaths(List<Path> paths) {
        this.paths = paths;
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    
    
    /**
     * Removes a MovingRange reference by name
     * @deprecated Use removeMovingRangeName() instead
     */
    @Deprecated
    public void removeMovingRange(MovingRange range) {
        if (range != null && range.getName() != null) {
            removeMovingRangeName(range.getName());
        }
    }

    public void removePath(Path path) {
        this.paths.remove(path);
    }
    

    public Path getPrimaryPath() {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        return paths.get(0);
    }

    public Path ensurePrimaryPath() {
        if (paths == null) {
            paths = new ArrayList<>();
        }
        if (paths.isEmpty() || paths.get(0) == null) {
            Path path = new Path();
            if (paths.isEmpty()) {
                paths.add(path);
            } else {
                paths.set(0, path);
            }
        }
        return paths.get(0);
    }

    public void consolidatePointContainers() {
        if (customClickAreas != null && customClickAreas.size() > 1) {
            CustomClickArea primary = ensurePrimaryCustomClickArea();
            for (int i = 1; i < customClickAreas.size(); i++) {
                CustomClickArea extra = customClickAreas.get(i);
                mergePointData(primary, extra);
            }
            customClickAreas.subList(1, customClickAreas.size()).clear();
        }

        // NOTE: MovingRanges are now stored externally in MovingRangeManager
        // and referenced by name only. Consolidation is handled at the Manager level.
        // If multiple MovingRange names exist, we keep only the first one.
        if (movingRangeNames != null && movingRangeNames.size() > 1) {
            System.out.println("Item: Multiple MovingRanges found for " + getName() + ", keeping only: " + movingRangeNames.get(0));
            movingRangeNames.subList(1, movingRangeNames.size()).clear();
        }

        if (paths != null && paths.size() > 1) {
            Path primaryPath = ensurePrimaryPath();
            for (int i = 1; i < paths.size(); i++) {
                Path extra = paths.get(i);
                mergePointData(primaryPath, extra);
            }
            paths.subList(1, paths.size()).clear();
        }
    }

    private void mergePointData(CustomClickArea target, CustomClickArea source) {
        if (target == null || source == null) {
            return;
        }

        List<Point> targetPoints = target.getPoints();
        List<Point> sourcePoints = source.getPoints();

        if (sourcePoints == null || sourcePoints.isEmpty()) {
            return;
        }

        if (targetPoints == null) {
            target.setPoints(new ArrayList<>());
            target.updatePolygon();
        }

        for (Point p : sourcePoints) {
            if (!containsPoint(targetPoints, p)) {
                target.getPoints().add(new Point(p.x, p.y));
            }
        }
        target.updatePolygon();

        // Merge basic metadata if target is missing it
        if (target.getHoverText() == null || target.getHoverText().isEmpty()) {
            target.setHoverText(source.getHoverText());
        }
        if (target.getConditions().isEmpty() && !source.getConditions().isEmpty()) {
            target.setConditions(new HashMap<>(source.getConditions()));
        }
    }

    private boolean containsPoint(List<Point> points, Point candidate) {
        if (points == null || candidate == null) {
            return false;
        }
        for (Point existing : points) {
            if (existing.x == candidate.x && existing.y == candidate.y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the active CustomClickArea at the given point.
     * Checks conditions and polygon containment.
     * @param point The point to check
     * @return The first active CustomClickArea containing the point, or null if none found
     */
    public CustomClickArea getCustomClickAreaAt(Point point) {
        for (CustomClickArea area : customClickAreas) {
            // Check if area's conditions are met
            if (area.shouldBeActive(null)) {
                // Check if point is inside the area's polygon
                if (area.containsPoint(point)) {
                    return area;
                }
            }
        }
        return null;
    }

    // ==================== Editor Visibility Methods ====================

    public boolean isVisibleInEditor() {
        return visibleInEditor;
    }

    public void setVisibleInEditor(boolean visible) {
        this.visibleInEditor = visible;
    }

    public boolean isCustomClickAreaVisibleInEditor() {
        return customClickAreaVisibleInEditor;
    }

    public void setCustomClickAreaVisibleInEditor(boolean visible) {
        this.customClickAreaVisibleInEditor = visible;
    }

    public boolean isMovingRangeVisibleInEditor() {
        return movingRangeVisibleInEditor;
    }

    public void setMovingRangeVisibleInEditor(boolean visible) {
        this.movingRangeVisibleInEditor = visible;
    }

    public boolean isPathVisibleInEditor() {
        return pathVisibleInEditor;
    }

    public void setPathVisibleInEditor(boolean visible) {
        this.pathVisibleInEditor = visible;
    }

    // ==================== Orientation Image Paths ====================

    public String getImagePathTopLeft() {
        return imagePathTopLeft;
    }

    public void setImagePathTopLeft(String path) {
        this.imagePathTopLeft = path;
    }

    public String getImagePathTop() {
        return imagePathTop;
    }

    public void setImagePathTop(String path) {
        this.imagePathTop = path;
    }

    public String getImagePathTopRight() {
        return imagePathTopRight;
    }

    public void setImagePathTopRight(String path) {
        this.imagePathTopRight = path;
    }

    public String getImagePathLeft() {
        return imagePathLeft;
    }

    public void setImagePathLeft(String path) {
        this.imagePathLeft = path;
    }

    public String getImagePathMiddle() {
        return imagePathMiddle;
    }

    public void setImagePathMiddle(String path) {
        this.imagePathMiddle = path;
    }

    public String getImagePathRight() {
        return imagePathRight;
    }

    public void setImagePathRight(String path) {
        this.imagePathRight = path;
    }

    public String getImagePathBottomLeft() {
        return imagePathBottomLeft;
    }

    public void setImagePathBottomLeft(String path) {
        this.imagePathBottomLeft = path;
    }

    public String getImagePathBottom() {
        return imagePathBottom;
    }

    public void setImagePathBottom(String path) {
        this.imagePathBottom = path;
    }

    public String getImagePathBottomRight() {
        return imagePathBottomRight;
    }

    public void setImagePathBottomRight(String path) {
        this.imagePathBottomRight = path;
    }

    public String getCurrentOrientation() {
        return currentOrientation;
    }

    /**
     * Calculates and updates the current orientation based on cursor position relative to item center.
     * The orientation is calculated in a star-shaped pattern with 8 directions + middle.
     *
     * @param cursorX Cursor X position
     * @param cursorY Cursor Y position
     */
    public void updateOrientationBasedOnCursor(int cursorX, int cursorY) {
        // Calculate vector from item center to cursor
        int deltaX = cursorX - position.x;
        int deltaY = cursorY - position.y;

        // Calculate distance from center
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // If cursor is very close to center (within 20px), use Middle
        if (distance < 20) {
            if (!currentOrientation.equals("Middle")) {
                currentOrientation = "Middle";
                System.out.println("Item '" + name + "' Orientation: " + currentOrientation);
            }
            return;
        }

        // Calculate angle in degrees (0° = right, 90° = down, 180° = left, 270° = up)
        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
        if (angle < 0) angle += 360; // Normalize to 0-360

        // Determine orientation based on angle (8 sectors of 45° each)
        // Right: 337.5° - 22.5°
        // BottomRight: 22.5° - 67.5°
        // Bottom: 67.5° - 112.5°
        // BottomLeft: 112.5° - 157.5°
        // Left: 157.5° - 202.5°
        // TopLeft: 202.5° - 247.5°
        // Top: 247.5° - 292.5°
        // TopRight: 292.5° - 337.5°

        String newOrientation;
        if (angle >= 337.5 || angle < 22.5) {
            newOrientation = "Right";
        } else if (angle >= 22.5 && angle < 67.5) {
            newOrientation = "BottomRight";
        } else if (angle >= 67.5 && angle < 112.5) {
            newOrientation = "Bottom";
        } else if (angle >= 112.5 && angle < 157.5) {
            newOrientation = "BottomLeft";
        } else if (angle >= 157.5 && angle < 202.5) {
            newOrientation = "Left";
        } else if (angle >= 202.5 && angle < 247.5) {
            newOrientation = "TopLeft";
        } else if (angle >= 247.5 && angle < 292.5) {
            newOrientation = "Top";
        } else { // 292.5 - 337.5
            newOrientation = "TopRight";
        }

        // Only log when orientation changes
        if (!currentOrientation.equals(newOrientation)) {
            currentOrientation = newOrientation;
            System.out.println("Item '" + name + "' Orientation: " + currentOrientation);
        }
    }

    /**
     * Gets the appropriate image path based on current orientation.
     * Returns null if orientation-specific image is not set.
     * Only applies when isFollowingMouse or isFollowingOnMouseClick is true.
     *
     * @return The orientation-specific image path, or null if not available
     */
    public String getOrientationImage() {
        // Only use orientation images when following mouse
        if (!isFollowingMouse && !isFollowingOnMouseClick) {
            return null;
        }

        switch (currentOrientation) {
            case "TopLeft": return imagePathTopLeft.isEmpty() ? null : imagePathTopLeft;
            case "Top": return imagePathTop.isEmpty() ? null : imagePathTop;
            case "TopRight": return imagePathTopRight.isEmpty() ? null : imagePathTopRight;
            case "Left": return imagePathLeft.isEmpty() ? null : imagePathLeft;
            case "Middle": return imagePathMiddle.isEmpty() ? null : imagePathMiddle;
            case "Right": return imagePathRight.isEmpty() ? null : imagePathRight;
            case "BottomLeft": return imagePathBottomLeft.isEmpty() ? null : imagePathBottomLeft;
            case "Bottom": return imagePathBottom.isEmpty() ? null : imagePathBottom;
            case "BottomRight": return imagePathBottomRight.isEmpty() ? null : imagePathBottomRight;
            default: return null;
        }
    }

    @Override
    public String toString() {
        return name + " (x=" + position.x + ", y=" + position.y + ")";
    }
}
