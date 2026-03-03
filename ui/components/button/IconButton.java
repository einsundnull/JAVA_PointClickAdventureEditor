package main.ui.components.button;

import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import java.awt.*;

/**
 * Icon-only button with consistent sizing.
 * Used for toolbar buttons, action buttons, etc.
 */
public class IconButton extends JButton {

    public enum Size {
        SMALL(20),
        DEFAULT(24),
        LARGE(32);

        private final int size;

        Size(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    private final Size iconSize;

    /**
     * Create an icon button with default size.
     */
    public IconButton(Icon icon) {
        this(icon, Size.DEFAULT);
    }

    /**
     * Create an icon button with specified size.
     */
    public IconButton(Icon icon, Size iconSize) {
        super(icon);
        this.iconSize = iconSize;
        init();
    }

    private void init() {
        var c = ThemeManager.colors();
        var s = Spacing.BUTTON_HEIGHT;

        setPreferredSize(new Dimension(s, s));
        setMinimumSize(new Dimension(s, s));
        setMaximumSize(new Dimension(s, s));

        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);

        setBackground(new Color(0, 0, 0, 0));
        setForeground(c.getTextSecondary());
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Scale icon
        scaleIcon(iconSize.getSize());
    }

    private void scaleIcon(int size) {
        Icon original = getIcon();
        if (original != null && original instanceof ImageIcon) {
            ImageIcon imgIcon = (ImageIcon) original;
            java.awt.Image img = imgIcon.getImage();
            java.awt.Image scaledImg = img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
            setIcon(new ImageIcon(scaledImg));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        var c = ThemeManager.colors();

        if (isEnabled()) {
            if (getModel().isRollover()) {
                setBackground(c.getBackgroundHover());
                setForeground(c.getPrimary());
            } else if (getModel().isPressed()) {
                setBackground(c.getBackgroundSelected());
            } else {
                setBackground(new Color(0, 0, 0, 0));
                setForeground(c.getTextSecondary());
            }
        } else {
            setBackground(new Color(0, 0, 0, 0));
            setForeground(c.getTextDisabled());
        }

        super.paintComponent(g);
    }

    public Size getIconSize() {
        return iconSize;
    }
}
