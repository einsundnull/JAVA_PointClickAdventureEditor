package main;

/**
 * Represents a configurable action button for the adventure game
 * Allows dynamic configuration of button text, cursor, hover behavior, etc.
 */
public class ActionButton {
    private String text;              // Button label text
    private String hoverText;         // Optional hover text when button is active
    private boolean showHoverText;    // Whether to show hover text
    private String imagePath;         // Optional button image path
    private boolean useImage;         // Use image instead of text
    private String hoverFormat;       // Format string for dynamic hover (e.g., "{action} {target}")
    private String cursorType;        // Cursor type: HAND, CROSSHAIR, DEFAULT, TEXT, MOVE, WAIT, CUSTOM
    private String customCursorPath;  // Path to custom cursor image if cursorType is CUSTOM
    private int order;                // Display order (0-based)

    public ActionButton() {
        this.text = "";
        this.hoverText = "";
        this.showHoverText = false;
        this.imagePath = "";
        this.useImage = false;
        this.hoverFormat = "{action}";
        this.cursorType = "CROSSHAIR";
        this.customCursorPath = "";
        this.order = 0;
    }

    public ActionButton(String text) {
        this();
        this.text = text;
    }

    // Getters and setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHoverText() {
        return hoverText;
    }

    public void setHoverText(String hoverText) {
        this.hoverText = hoverText;
    }

    public boolean isShowHoverText() {
        return showHoverText;
    }

    public void setShowHoverText(boolean showHoverText) {
        this.showHoverText = showHoverText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isUseImage() {
        return useImage;
    }

    public void setUseImage(boolean useImage) {
        this.useImage = useImage;
    }

    public String getHoverFormat() {
        return hoverFormat;
    }

    public void setHoverFormat(String hoverFormat) {
        this.hoverFormat = hoverFormat;
    }

    public String getCursorType() {
        return cursorType;
    }

    public void setCursorType(String cursorType) {
        this.cursorType = cursorType;
    }

    public String getCustomCursorPath() {
        return customCursorPath;
    }

    public void setCustomCursorPath(String customCursorPath) {
        this.customCursorPath = customCursorPath;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "ActionButton{" +
                "text='" + text + '\'' +
                ", cursorType='" + cursorType + '\'' +
                ", order=" + order +
                '}';
    }
}
