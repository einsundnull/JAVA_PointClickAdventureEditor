package main.ui.theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Storage for theme preferences.
 * Persists theme choice (light/dark) across sessions.
 */
public class ThemeStorage {

    private static final String SETTINGS_FILE = "editor_theme.properties";
    private static final String THEME_KEY = "theme";

    /**
     * Save theme preference.
     */
    public static void saveTheme(boolean isDark) {
        try {
            Properties props = new Properties();
            props.setProperty(THEME_KEY, isDark ? "dark" : "light");

            File file = new File(SETTINGS_FILE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "PointClick Adventure Editor - Theme Settings");
            }
        } catch (Exception e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }

    /**
     * Load theme preference.
     * @return true if dark mode was saved, false otherwise (default: light)
     */
    public static boolean loadTheme() {
        try {
            File file = new File(SETTINGS_FILE);
            if (!file.exists()) {
                return false; // Default to light theme
            }

            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }

            String theme = props.getProperty(THEME_KEY, "light");
            return "dark".equalsIgnoreCase(theme);
        } catch (Exception e) {
            System.err.println("Failed to load theme preference: " + e.getMessage());
            return false; // Default to light theme on error
        }
    }

    /**
     * Delete theme storage (reset to default).
     */
    public static void reset() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
