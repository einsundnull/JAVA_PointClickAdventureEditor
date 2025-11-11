package main;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages editor settings persistence using a simple key-value format.
 * Stores window sizes, positions, split pane divider locations, etc.
 */
public class EditorSettings {

    private static final String SETTINGS_FILE = "resources/editor_settings.txt";

    private Map<String, String> settings;

    public EditorSettings() {
        settings = new LinkedHashMap<>();
    }

    /**
     * Load settings from file.
     */
    public void load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            System.out.println("No settings file found, using defaults");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    settings.put(key, value);
                }
            }
            System.out.println("✓ Loaded " + settings.size() + " editor settings");
        } catch (IOException e) {
            System.err.println("Error loading editor settings: " + e.getMessage());
        }
    }

    /**
     * Save settings to file.
     */
    public void save() {
        File file = new File(SETTINGS_FILE);
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# Editor Settings");
            writer.println("# Auto-generated - do not edit manually while editor is running");
            writer.println();

            // Write all settings
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }

            System.out.println("✓ Saved " + settings.size() + " editor settings");
        } catch (IOException e) {
            System.err.println("Error saving editor settings: " + e.getMessage());
        }
    }

    // === Generic getters/setters ===

    public String getString(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public void setString(String key, String value) {
        settings.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        String value = settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setInt(String key, int value) {
        settings.put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public void setBoolean(String key, boolean value) {
        settings.put(key, String.valueOf(value));
    }

    // === Window-specific getters/setters ===

    public Dimension getWindowSize(String prefix, Dimension defaultSize) {
        int width = getInt(prefix + ".width", defaultSize.width);
        int height = getInt(prefix + ".height", defaultSize.height);
        return new Dimension(width, height);
    }

    public void setWindowSize(String prefix, Dimension size) {
        setInt(prefix + ".width", size.width);
        setInt(prefix + ".height", size.height);
    }

    public Point getWindowPosition(String prefix, Point defaultPosition) {
        int x = getInt(prefix + ".x", defaultPosition.x);
        int y = getInt(prefix + ".y", defaultPosition.y);
        return new Point(x, y);
    }

    public void setWindowPosition(String prefix, Point position) {
        setInt(prefix + ".x", position.x);
        setInt(prefix + ".y", position.y);
    }

    /**
     * Get window bounds (position + size).
     */
    public java.awt.Rectangle getWindowBounds(String prefix, java.awt.Rectangle defaultBounds) {
        int x = getInt(prefix + ".x", defaultBounds.x);
        int y = getInt(prefix + ".y", defaultBounds.y);
        int width = getInt(prefix + ".width", defaultBounds.width);
        int height = getInt(prefix + ".height", defaultBounds.height);
        return new java.awt.Rectangle(x, y, width, height);
    }

    /**
     * Set window bounds (position + size).
     */
    public void setWindowBounds(String prefix, java.awt.Rectangle bounds) {
        setInt(prefix + ".x", bounds.x);
        setInt(prefix + ".y", bounds.y);
        setInt(prefix + ".width", bounds.width);
        setInt(prefix + ".height", bounds.height);
    }

    /**
     * Get split pane divider location.
     */
    public int getSplitPaneDivider(String name, int defaultLocation) {
        return getInt("splitpane." + name, defaultLocation);
    }

    /**
     * Set split pane divider location.
     */
    public void setSplitPaneDivider(String name, int location) {
        setInt("splitpane." + name, location);
    }

    /**
     * Clear all settings.
     */
    public void clear() {
        settings.clear();
    }

    /**
     * Check if a key exists.
     */
    public boolean has(String key) {
        return settings.containsKey(key);
    }
}
