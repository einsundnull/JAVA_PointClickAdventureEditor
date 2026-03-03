package main.ui.theme;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarculaLaf;

/**
 * Central theme manager for the application.
 * Manages theme switching and applies design tokens to Swing components.
 */
public class ThemeManager {

    private static AppTheme currentTheme;
    private static final Map<String, Consumer<JComponent>> componentStylers = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize the theme system with saved preference or default (light) theme.
     * Call this once at application startup, before creating any Swing components.
     */
    public static void init() {
        boolean savedDarkMode = ThemeStorage.loadTheme();
        init(savedDarkMode ? AppTheme.dark() : AppTheme.light());
    }

    /**
     * Initialize the theme system with a specific theme.
     */
    public static void init(AppTheme theme) {
        if (initialized) {
            throw new IllegalStateException("ThemeManager already initialized. Call setTheme() to change themes.");
        }
        setTheme(theme);
        initialized = true;
    }

    /**
     * Set the current theme and update all existing windows.
     */
    public static void setTheme(AppTheme theme) {
        currentTheme = theme;

        // Apply FlatLaf Look and Feel
        applyLookAndFeel(theme);

        // Apply custom color overrides
        applyColorOverrides(theme);
    }

    /**
     * Get the current theme.
     */
    public static AppTheme getTheme() {
        if (currentTheme == null) {
            throw new IllegalStateException("ThemeManager not initialized. Call init() first.");
        }
        return currentTheme;
    }

    /**
     * Get the current color palette.
     */
    public static ColorPalette colors() {
        return getTheme().colors();
    }

    /**
     * Get the current typography.
     */
    public static Typography typography() {
        return getTheme().typography();
    }

    /**
     * Get the Spacing utility class (contains only static constants).
     */
    public static Class<Spacing> spacing() {
        return getTheme().spacing();
    }

    /**
     * Apply FlatLaf Look and Feel based on theme.
     */
    private static void applyLookAndFeel(AppTheme theme) {
        try {
            if (theme.isDark()) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception e) {
            System.err.println("Failed to apply Look and Feel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apply custom color overrides to UIManager.
     * This extends FlatLaf with our custom design tokens.
     */
    private static void applyColorOverrides(AppTheme theme) {
        ColorPalette c = theme.colors();

        // === COMPONENT SPECIFIC OVERRIDES ===

        // Button
        UIManager.put("Button.arc", Spacing.RADIUS_MD);
        UIManager.put("Button.margin", new Insets(Spacing.BUTTON_PADDING_V, Spacing.BUTTON_PADDING_H, Spacing.BUTTON_PADDING_V, Spacing.BUTTON_PADDING_H));
        UIManager.put("Button.focusWidth", 0);

        // TextField
        UIManager.put("TextField.arc", Spacing.RADIUS_SM);
        UIManager.put("TextField.margin", new Insets(Spacing.INPUT_PADDING_V, Spacing.INPUT_PADDING_H, Spacing.INPUT_PADDING_V, Spacing.INPUT_PADDING_H));
        UIManager.put("TextField.background", new ColorUIResource(c.getBackgroundInput()));
        UIManager.put("TextField.foreground", new ColorUIResource(c.getTextPrimary()));

        // TextArea
        UIManager.put("TextArea.arc", Spacing.RADIUS_SM);
        UIManager.put("TextArea.margin", new Insets(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM));
        UIManager.put("TextArea.background", new ColorUIResource(c.getBackgroundInput()));

        // CheckBox
        UIManager.put("CheckBox.arc", Spacing.RADIUS_SM);
        UIManager.put("CheckBox.icon.arc", Spacing.RADIUS_SM);

        // Panel
        UIManager.put("Panel.background", new ColorUIResource(c.getBackgroundRoot()));
        UIManager.put("Panel.foreground", new ColorUIResource(c.getTextPrimary()));

        // SplitPane
        UIManager.put("SplitPaneDivider.draggingColor", new ColorUIResource(c.getPrimary()));
        UIManager.put("SplitPaneDivider.borderColor", new ColorUIResource(c.getBorderDefault()));

        // ScrollBar
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.track", new ColorUIResource(c.getBackgroundElevated()));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(c.getBorderStrong()));
        UIManager.put("ScrollBar.thumbHover", new ColorUIResource(c.getTextTertiary()));
        UIManager.put("ScrollBar.thumbArc", Spacing.RADIUS_SM);

        // Tree
        UIManager.put("Tree.rowHeight", Spacing.TREE_ROW_HEIGHT);
        UIManager.put("Tree.background", new ColorUIResource(c.getBackgroundPanel()));
        UIManager.put("Tree.foreground", new ColorUIResource(c.getTextPrimary()));
        UIManager.put("Tree.selectionBackground", new ColorUIResource(c.getBackgroundSelected()));
        UIManager.put("Tree.selectionForeground", new ColorUIResource(c.getPrimary()));

        // Table
        UIManager.put("Table.rowHeight", Spacing.TABLE_ROW_HEIGHT);
        UIManager.put("Table.background", new ColorUIResource(c.getBackgroundPanel()));
        UIManager.put("Table.foreground", new ColorUIResource(c.getTextPrimary()));
        UIManager.put("Table.selectionBackground", new ColorUIResource(c.getBackgroundSelected()));
        UIManager.put("Table.selectionForeground", new ColorUIResource(c.getPrimary()));
        UIManager.put("Table.headerBackground", new ColorUIResource(c.getBackgroundElevated()));
        UIManager.put("Table.headerForeground", new ColorUIResource(c.getTextSecondary()));

        // Label
        UIManager.put("Label.foreground", new ColorUIResource(c.getTextPrimary()));

        // ToolTip
        UIManager.put("ToolTip.background", new ColorUIResource(c.getTextPrimary()));
        UIManager.put("ToolTip.foreground", new ColorUIResource(c.getBackgroundPanel()));

        // TabbedPane
        UIManager.put("TabbedPane.selectedBackground", new ColorUIResource(c.getBackgroundPanel()));
        UIManager.put("TabbedPane.selectedForeground", new ColorUIResource(c.getPrimary()));
        UIManager.put("TabbedPane.underlineColor", new ColorUIResource(c.getPrimary()));

        // Progress Bar
        UIManager.put("ProgressBar.foreground", new ColorUIResource(c.getPrimary()));
        UIManager.put("ProgressBar.background", new ColorUIResource(c.getBackgroundElevated()));
        UIManager.put("ProgressBar.arc", Spacing.RADIUS_SM);
    }

    /**
     * Update all existing windows with the current theme.
     */
    public static void updateAllWindows() {
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    /**
     * Register a custom component styler.
     * The styler will be applied to components when applyStyle() is called.
     */
    public static void registerComponentStyler(String key, Consumer<JComponent> styler) {
        componentStylers.put(key, styler);
    }

    /**
     * Apply a registered style to a component.
     */
    public static void applyStyle(String key, JComponent component) {
        Consumer<JComponent> styler = componentStylers.get(key);
        if (styler != null) {
            styler.accept(component);
        }
    }

    /**
     * Check if theme manager is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Toggle between light and dark theme.
     * Saves the preference to storage.
     */
    public static void toggleTheme() {
        AppTheme newTheme;
        if (currentTheme == null) {
            newTheme = AppTheme.light();
        } else if (currentTheme.isDark()) {
            newTheme = AppTheme.light();
        } else {
            newTheme = AppTheme.dark();
        }
        setTheme(newTheme);
        ThemeStorage.saveTheme(newTheme.isDark());
        updateAllWindows();
    }
}
