package main.ui.utils;

import main.ui.components.button.AppButton;
import main.ui.components.input.AppTextField;
import main.ui.components.panel.CardPanel;
import main.ui.icons.IconLoader;
import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating styled UI components with minimal code.
 */
public class ComponentFactory {

    /**
     * Create a primary button.
     */
    public static AppButton button(String text) {
        return new AppButton(text, AppButton.Variant.PRIMARY);
    }

    /**
     * Create a button with variant.
     */
    public static AppButton button(String text, AppButton.Variant variant) {
        return new AppButton(text, variant);
    }

    /**
     * Create a small button.
     */
    public static AppButton buttonSmall(String text, AppButton.Variant variant) {
        return new AppButton(text, variant, AppButton.Size.SMALL);
    }

    /**
     * Create an icon button.
     */
    public static JButton iconButton(Icon icon) {
        return new main.ui.components.button.IconButton(icon);
    }

    /**
     * Create a text field.
     */
    public static AppTextField textField() {
        return new AppTextField();
    }

    /**
     * Create a text field with columns.
     */
    public static AppTextField textField(int columns) {
        return new AppTextField(columns);
    }

    /**
     * Create a text field with placeholder.
     */
    public static AppTextField textField(String placeholder) {
        AppTextField field = new AppTextField();
        field.setPlaceholder(placeholder);
        return field;
    }

    /**
     * Create a label with primary text color.
     */
    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.typography().sm());
        label.setForeground(ThemeManager.colors().getTextPrimary());
        return label;
    }

    /**
     * Create a bold label.
     */
    public static JLabel labelBold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.typography().semiboldSm());
        label.setForeground(ThemeManager.colors().getTextPrimary());
        return label;
    }

    /**
     * Create a secondary label (muted color).
     */
    public static JLabel labelSecondary(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.typography().sm());
        label.setForeground(ThemeManager.colors().getTextSecondary());
        return label;
    }

    /**
     * Create a title label.
     */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.typography().semiboldLg());
        label.setForeground(ThemeManager.colors().getTextPrimary());
        return label;
    }

    /**
     * Create a subtitle label.
     */
    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.typography().md());
        label.setForeground(ThemeManager.colors().getTextSecondary());
        return label;
    }

    /**
     * Create a card panel.
     */
    public static CardPanel card() {
        return new CardPanel();
    }

    /**
     * Create a card panel with title.
     */
    public static CardPanel card(String title) {
        return new CardPanel(title);
    }

    /**
     * Create a card panel with title and style.
     */
    public static CardPanel card(String title, CardPanel.Style style) {
        return new CardPanel(title, style);
    }

    /**
     * Create a vertical strut (spacer).
     */
    public static Component vStrut(int height) {
        return Box.createVerticalStrut(height);
    }

    /**
     * Create a horizontal strut (spacer).
     */
    public static Component hStrut(int width) {
        return Box.createHorizontalStrut(width);
    }

    /**
     * Create a vertical glue (expanding spacer).
     */
    public static Component vGlue() {
        return Box.createVerticalGlue();
    }

    /**
     * Create a horizontal glue (expanding spacer).
     */
    public static Component hGlue() {
        return Box.createHorizontalGlue();
    }

    /**
     * Create a panel with vertical layout.
     */
    public static JPanel vPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Create a panel with horizontal layout.
     */
    public static JPanel hPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Create a panel with flow layout (left aligned).
     */
    public static JPanel flowPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.SM));
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Create a separator line.
     */
    public static JSeparator separator() {
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(ThemeManager.colors().getBorderDefault());
        return sep;
    }

    /**
     * Create a checkbox.
     */
    public static JCheckBox checkBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(ThemeManager.typography().sm());
        cb.setForeground(ThemeManager.colors().getTextPrimary());
        cb.setBackground(ThemeManager.colors().getBackgroundPanel());
        return cb;
    }

    /**
     * Create a scroll pane.
     */
    public static JScrollPane scrollPane(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.colors().getBorderDefault(), 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Create a scroll pane with no border.
     */
    public static JScrollPane scrollPanePlain(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
}
