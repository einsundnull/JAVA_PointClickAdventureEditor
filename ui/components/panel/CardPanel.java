package main.ui.components.panel;

import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Modern panel with card styling.
 * Features:
 * - Subtle border and shadow effect
 * - Optional title
 * - Optional header
 * - Configurable padding
 */
public class CardPanel extends JPanel {

    public enum Style {
        DEFAULT,    // White with border
        ELEVATED,   // With shadow effect
        OUTLINED,   // Stronger border
        FLAT        // No border, background only
    }

    private final Style style;
    private JLabel titleLabel;
    private JPanel headerPanel;
    private JPanel contentPanel;

    /**
     * Create a default card panel.
     */
    public CardPanel() {
        this(Style.DEFAULT);
    }

    /**
     * Create a card panel with specific style.
     */
    public CardPanel(Style style) {
        this(style, Spacing.PANEL_PADDING);
    }

    /**
     * Create a card panel with style and custom padding.
     */
    public CardPanel(Style style, int padding) {
        super(new BorderLayout());
        this.style = style;
        init(padding);
    }

    /**
     * Create a card panel with a title.
     */
    public CardPanel(String title) {
        this(title, Style.DEFAULT);
    }

    /**
     * Create a card panel with title and style.
     */
    public CardPanel(String title, Style style) {
        this(title, style, Spacing.PANEL_PADDING);
    }

    /**
     * Create a card panel with title, style and padding.
     */
    public CardPanel(String title, Style style, int padding) {
        super(new BorderLayout());
        this.style = style;
        init(padding);
        setTitle(title);
    }

    private void init(int padding) {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();

        // Background
        setBackground(c.getBackgroundPanel());

        // Border based on style
        Border border;
        switch (style) {
            case ELEVATED:
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c.getBorderDefault(), 1),
                    BorderFactory.createEmptyBorder(padding, padding, padding, padding)
                );
                break;
            case OUTLINED:
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c.getBorderStrong(), 1),
                    BorderFactory.createEmptyBorder(padding, padding, padding, padding)
                );
                break;
            case FLAT:
                border = BorderFactory.createEmptyBorder(padding, padding, padding, padding);
                break;
            case DEFAULT:
            default:
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c.getBorderDefault(), 1),
                    BorderFactory.createEmptyBorder(padding, padding, padding, padding)
                );
                break;
        }
        setBorder(border);

        // Content panel for adding components
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        super.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Set the card title (creates header if needed).
     */
    public void setTitle(String title) {
        if (titleLabel == null) {
            createHeader();
        }
        titleLabel.setText(title);
    }

    /**
     * Get the title text.
     */
    public String getTitle() {
        return titleLabel != null ? titleLabel.getText() : null;
    }

    /**
     * Create the header panel.
     */
    private void createHeader() {
        var t = ThemeManager.typography();
        var c = ThemeManager.colors();

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, Spacing.SM, 0));

        titleLabel = new JLabel();
        titleLabel.setFont(t.semiboldMd());
        titleLabel.setForeground(c.getTextPrimary());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     * Add a component to the header (right side).
     */
    public void addHeaderComponent(Component comp) {
        if (headerPanel == null) {
            createHeader();
        }
        headerPanel.add(comp, BorderLayout.EAST);
    }

    /**
     * Add a component to the content area.
     */
    @Override
    public Component add(Component comp) {
        return contentPanel.add(comp);
    }

    /**
     * Add a component to the content area with constraints.
     */
    @Override
    public void add(Component comp, Object constraints) {
        contentPanel.add(comp, constraints);
    }

    /**
     * Get the content panel for direct manipulation.
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * Get the style.
     */
    public Style getStyle() {
        return style;
    }
}
