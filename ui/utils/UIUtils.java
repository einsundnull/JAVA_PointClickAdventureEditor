package main.ui.utils;

import main.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Utility methods for UI operations.
 */
public class UIUtils {

    /**
     * Set the preferred, minimum and maximum size of a component.
     */
    public static void setSize(JComponent comp, int width, int height) {
        Dimension dim = new Dimension(width, height);
        comp.setPreferredSize(dim);
        comp.setMinimumSize(dim);
        comp.setMaximumSize(dim);
    }

    /**
     * Set the preferred and minimum width of a component.
     */
    public static void setWidth(JComponent comp, int width) {
        comp.setPreferredSize(new Dimension(width, comp.getPreferredSize().height));
        comp.setMinimumSize(new Dimension(width, comp.getMinimumSize().height));
    }

    /**
     * Set the preferred and minimum height of a component.
     */
    public static void setHeight(JComponent comp, int height) {
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, height));
        comp.setMinimumSize(new Dimension(comp.getMinimumSize().width, height));
    }

    /**
     * Set the maximum width of a component.
     */
    public static void setMaxWidth(JComponent comp, int width) {
        comp.setMaximumSize(new Dimension(width, comp.getMaximumSize().height));
    }

    /**
     * Set the maximum height of a component.
     */
    public static void setMaxHeight(JComponent comp, int height) {
        comp.setMaximumSize(new Dimension(comp.getMaximumSize().width, height));
    }

    /**
     * Apply consistent margin to a component.
     */
    public static void setMargin(JComponent comp, int top, int left, int bottom, int right) {
        comp.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    /**
     * Apply consistent padding to a component.
     */
    public static void setPadding(JComponent comp, int padding) {
        comp.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    /**
     * Add a titled border to a component.
     */
    public static void setTitledBorder(JComponent comp, String title) {
        comp.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ThemeManager.colors().getBorderDefault()),
            title
        ));
    }

    /**
     * Make a component transparent.
     */
    public static void setTransparent(JComponent comp) {
        comp.setOpaque(false);
    }

    /**
     * Apply a background color to a component.
     */
    public static void setBackground(JComponent comp, java.awt.Color color) {
        comp.setOpaque(true);
        comp.setBackground(color);
    }

    /**
     * Apply foreground (text) color to a component.
     */
    public static void setForeground(JComponent comp, java.awt.Color color) {
        comp.setForeground(color);
    }

    /**
     * Enable or disable a component.
     */
    public static void setEnabled(JComponent comp, boolean enabled) {
        comp.setEnabled(enabled);
    }

    /**
     * Set the cursor for a component.
     */
    public static void setCursor(JComponent comp, int cursorType) {
        comp.setCursor(new Cursor(cursorType));
    }

    /**
     * Set hand cursor (clickable).
     */
    public static void setHandCursor(JComponent comp) {
        comp.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Set default cursor.
     */
    public static void setDefaultCursor(JComponent comp) {
        comp.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Make a component fill available width (for BoxLayout).
     */
    public static void fillWidth(JComponent comp) {
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, comp.getMaximumSize().height));
    }

    /**
     * Make a component fill available height (for BoxLayout).
     */
    public static void fillHeight(JComponent comp) {
        comp.setMaximumSize(new Dimension(comp.getMaximumSize().width, Integer.MAX_VALUE));
    }

    /**
     * Make a component fill both dimensions (for BoxLayout).
     */
    public static void fillBoth(JComponent comp) {
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * Center a window on screen.
     */
    public static void centerWindow(Window window) {
        window.setLocationRelativeTo(null);
    }

    /**
     * Show an error dialog.
     */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a warning dialog.
     */
    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Show an info dialog.
     */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show a confirmation dialog.
     * @return true if user confirmed (Yes/OK)
     */
    public static boolean showConfirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * Show a confirmation dialog with custom title.
     */
    public static boolean showConfirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * Show a confirmation dialog with Yes/No/Cancel options.
     * @return JOptionPane.YES_OPTION, NO_OPTION, or CANCEL_OPTION
     */
    public static int showConfirmCancel(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    /**
     * Create an icon from a unicode character.
     */
    public static Icon iconFromChar(String ch, int size, java.awt.Color color) {
        return main.ui.icons.IconLoader.fromText(ch, size, color);
    }

    /**
     * Get current theme color palette.
     */
    public static main.ui.theme.ColorPalette colors() {
        return ThemeManager.colors();
    }

    /**
     * Get current theme typography.
     */
    public static main.ui.theme.Typography fonts() {
        return ThemeManager.typography();
    }

    /**
     * Get the Spacing utility class (contains only static constants).
     */
    public static Class<main.ui.theme.Spacing> spacing() {
        return ThemeManager.spacing();
    }

    /**
     * Toggle between light and dark theme.
     */
    public static void toggleTheme() {
        ThemeManager.toggleTheme();
    }

    /**
     * Update all windows with current theme.
     */
    public static void updateAllUI() {
        ThemeManager.updateAllWindows();
    }
}
