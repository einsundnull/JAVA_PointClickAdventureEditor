package main;

/**
 * Represents a button with all its properties:
 * - Image (path to button image)
 * - Text on button
 * - Hover text
 * - Flags for which parts to use
 * - Size (width/height)
 */
public class ButtonData {
    private String name; // Internal identifier (e.g., "Anschauen")
    private String imagePath; // Path to button image
    private String textOnButton; // Text displayed on button
    private String hoverText; // Hover text for button
    private boolean useText; // Use text on button?
    private boolean useHoverText; // Use hover text?
    private boolean useImage; // Use image?
    private int width; // Button width in pixels
    private int height; // Button height in pixels

    public ButtonData(String name) {
        this.name = name;
        this.imagePath = "";
        this.textOnButton = name; // Default to name
        this.hoverText = name; // Default to name
        this.useText = true;
        this.useHoverText = true;
        this.useImage = false;
        this.width = 100;
        this.height = 30;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTextOnButton() {
        return textOnButton;
    }

    public void setTextOnButton(String textOnButton) {
        this.textOnButton = textOnButton;
    }

    public String getHoverText() {
        return hoverText;
    }

    public void setHoverText(String hoverText) {
        this.hoverText = hoverText;
    }

    public boolean isUseText() {
        return useText;
    }

    public void setUseText(boolean useText) {
        this.useText = useText;
    }

    public boolean isUseHoverText() {
        return useHoverText;
    }

    public void setUseHoverText(boolean useHoverText) {
        this.useHoverText = useHoverText;
    }

    public boolean isUseImage() {
        return useImage;
    }

    public void setUseImage(boolean useImage) {
        this.useImage = useImage;
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

    @Override
    public String toString() {
        return name;
    }
}
