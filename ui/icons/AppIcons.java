package main.ui.icons;

import javax.swing.*;
import java.awt.Color;

/**
 * Centralized icon constants for the application.
 * Icons are loaded or generated on first access.
 */
public class AppIcons {

    // === NAVIGATION ===
    public static final Icon ARROW_UP = IconLoader.arrow(IconLoader.Direction.UP, 16, new Color(100, 100, 100));
    public static final Icon ARROW_DOWN = IconLoader.arrow(IconLoader.Direction.DOWN, 16, new Color(100, 100, 100));
    public static final Icon ARROW_LEFT = IconLoader.arrow(IconLoader.Direction.LEFT, 16, new Color(100, 100, 100));
    public static final Icon ARROW_RIGHT = IconLoader.arrow(IconLoader.Direction.RIGHT, 16, new Color(100, 100, 100));

    // === ACTIONS ===
    public static final Icon ADD = IconLoader.fromText("+", 20, new Color(16, 185, 129));
    public static final Icon REMOVE = IconLoader.fromText("×", 20, new Color(239, 68, 68));
    public static final Icon EDIT = IconLoader.fromText("✎", 18, new Color(100, 100, 100));
    public static final Icon DELETE = IconLoader.fromText("🗑", 18, new Color(239, 68, 68));
    public static final Icon SAVE = IconLoader.fromText("💾", 18, new Color(59, 130, 246));
    public static final Icon COPY = IconLoader.fromText("⧉", 18, new Color(100, 100, 100));

    // === COMMON ===
    public static final Icon SEARCH = IconLoader.fromText("🔍", 18, new Color(100, 100, 100));
    public static final Icon CHECK = IconLoader.fromText("✓", 18, new Color(16, 185, 129));
    public static final Icon CLOSE = IconLoader.fromText("×", 16, new Color(100, 100, 100));
    public static final Icon SETTINGS = IconLoader.fromText("⚙", 18, new Color(100, 100, 100));
    public static final Icon INFO = IconLoader.fromText("ⓘ", 18, new Color(6, 182, 212));
    public static final Icon WARNING = IconLoader.fromText("⚠", 18, new Color(245, 158, 11));

    // === PANELS ===
    public static final Icon EXPAND = IconLoader.fromText("⬍", 16, new Color(100, 100, 100));
    public static final Icon COLLAPSE = IconLoader.fromText("⬌", 16, new Color(100, 100, 100));

    // === SECTIONS ===
    public static final Icon SCENES = IconLoader.fromText("📋", 20, new Color(59, 130, 246));
    public static final Icon ITEMS = IconLoader.fromText("🎨", 20, new Color(99, 102, 241));
    public static final Icon ACTIONS = IconLoader.fromText("⚡", 20, new Color(245, 158, 11));
    public static final Icon CONDITIONS = IconLoader.fromText("❓", 20, new Color(6, 182, 212));

    // === FILES ===
    public static final Icon FOLDER = IconLoader.fromText("📁", 20, new Color(245, 158, 11));
    public static final Icon FILE = IconLoader.fromText("📄", 20, new Color(100, 100, 100));

    // === COLOR INDICATORS ===
    public static final Icon DOT_SUCCESS = IconLoader.coloredCircle(new Color(16, 185, 129), 8);
    public static final Icon DOT_WARNING = IconLoader.coloredCircle(new Color(245, 158, 11), 8);
    public static final Icon DOT_ERROR = IconLoader.coloredCircle(new Color(239, 68, 68), 8);
    public static final Icon DOT_INFO = IconLoader.coloredCircle(new Color(6, 182, 212), 8);

    // Prevent instantiation
    private AppIcons() {}
}
