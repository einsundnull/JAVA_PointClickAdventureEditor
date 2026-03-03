package main.ui.components.button;

import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import java.awt.*;

/**
 * Modern button with consistent styling.
 *
 * Variants:
 * - PRIMARY: Main action button
 * - SECONDARY: Secondary action
 * - DANGER: Destructive actions
 * - SUCCESS: Success actions
 * - GHOST: Transparent with border
 * - ICON: Square icon button
 */
public class AppButton extends JButton {

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS,
        WARNING,
        GHOST,
        ICON
    }

    public enum Size {
        SMALL(Spacing.BUTTON_HEIGHT_SM),
        DEFAULT(Spacing.BUTTON_HEIGHT),
        LARGE(Spacing.BUTTON_HEIGHT_LG);

        private final int height;

        Size(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }
    }

    private final Variant variant;
    private final Size size;

    /**
     * Create a default primary button.
     */
    public AppButton(String text) {
        this(text, Variant.PRIMARY, Size.DEFAULT);
    }

    /**
     * Create a button with text and variant.
     */
    public AppButton(String text, Variant variant) {
        this(text, variant, Size.DEFAULT);
    }

    /**
     * Create a button with text, variant and size.
     */
    public AppButton(String text, Variant variant, Size size) {
        super(text);
        this.variant = variant;
        this.size = size;
        init();
    }

    /**
     * Create an icon button.
     */
    public AppButton(Icon icon) {
        this(icon, Variant.ICON, Size.DEFAULT);
    }

    /**
     * Create an icon button with size.
     */
    public AppButton(Icon icon, Size size) {
        this(icon, Variant.ICON, size);
    }

    /**
     * Create a button with icon and text.
     */
    public AppButton(Icon icon, String text) {
        this(icon, text, Variant.PRIMARY, Size.DEFAULT);
    }

    /**
     * Create a button with icon, text and variant.
     */
    public AppButton(Icon icon, String text, Variant variant) {
        this(icon, text, variant, Size.DEFAULT);
    }

    /**
     * Create a button with icon, text, variant and size.
     */
    public AppButton(Icon icon, String text, Variant variant, Size size) {
        super(text, icon);
        this.variant = variant;
        this.size = size;
        init();
    }

    private void init() {
        setFont(ThemeManager.typography().semiboldSm());
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);

        // Set size
        setPreferredSize(new Dimension(getPreferredWidth(), size.getHeight()));
        setMinimumSize(new Dimension(getPreferredWidth(), size.getHeight()));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, size.getHeight()));

        // Apply variant colors
        applyVariant();

        // Cursor
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private int getPreferredWidth() {
        if (variant == Variant.ICON) {
            return size.getHeight();
        }
        // For text buttons, let the text determine width with padding
        return -1;
    }

    private void applyVariant() {
        var c = ThemeManager.colors();

        switch (variant) {
            case PRIMARY:
                setBackground(c.getPrimary());
                setForeground(c.getPrimaryForeground());
                break;
            case SECONDARY:
                setBackground(c.getBackgroundElevated());
                setForeground(c.getTextPrimary());
                setBorder(BorderFactory.createLineBorder(c.getBorderDefault(), 1));
                setBorderPainted(true);
                break;
            case DANGER:
                setBackground(c.getDanger());
                setForeground(Color.WHITE);
                break;
            case SUCCESS:
                setBackground(c.getSuccess());
                setForeground(Color.WHITE);
                break;
            case WARNING:
                setBackground(c.getWarning());
                setForeground(Color.WHITE);
                break;
            case GHOST:
                setBackground(new Color(0, 0, 0, 0));
                setForeground(c.getPrimary());
                setBorder(BorderFactory.createLineBorder(c.getPrimary(), 1));
                setBorderPainted(true);
                setOpaque(false);
                break;
            case ICON:
                setBackground(c.getBackgroundElevated());
                setForeground(c.getTextPrimary());
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        var c = ThemeManager.colors();

        // Hover effect
        if (getModel().isRollover() && isEnabled()) {
            switch (variant) {
                case PRIMARY:
                    setBackground(c.getPrimaryHover());
                    break;
                case SECONDARY:
                case ICON:
                    setBackground(c.getBackgroundHover());
                    break;
                case DANGER:
                    setBackground(c.getDanger().darker());
                    break;
                case SUCCESS:
                    setBackground(c.getSuccess().darker());
                    break;
                case WARNING:
                    setBackground(c.getWarning().darker());
                    break;
                case GHOST:
                    setBackground(c.getBackgroundSelected());
                    break;
            }
        } else if (!getModel().isRollover() && isEnabled()) {
            // Reset to base color
            applyVariant();
        }

        // Pressed effect
        if (getModel().isPressed() && isEnabled()) {
            switch (variant) {
                case PRIMARY:
                    setBackground(c.getPrimaryActive());
                    break;
            }
        }

        // Disabled state
        if (!isEnabled()) {
            setBackground(c.getBackgroundElevated());
            setForeground(c.getTextDisabled());
        }

        super.paintComponent(g);
    }

    public Variant getVariant() {
        return variant;
    }

    public Size getSizeType() {
        return size;
    }
}
