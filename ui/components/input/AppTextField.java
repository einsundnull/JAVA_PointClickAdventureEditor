package main.ui.components.input;

import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Modern text field with consistent styling.
 */
public class AppTextField extends JTextField {

    public enum Variant {
        DEFAULT,
        ERROR,
        SUCCESS
    }

    private Variant variant = Variant.DEFAULT;
    private Border defaultBorder;
    private Border focusBorder;

    /**
     * Create a default text field.
     */
    public AppTextField() {
        this(0);
    }

    /**
     * Create a text field with columns.
     */
    public AppTextField(int columns) {
        super(columns);
        init();
    }

    /**
     * Create a text field with initial text.
     */
    public AppTextField(String text) {
        this(text, 0);
    }

    /**
     * Create a text field with initial text and columns.
     */
    public AppTextField(String text, int columns) {
        super(text, columns);
        init();
    }

    private void init() {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();
        var s = Spacing.INPUT_HEIGHT;

        // Font
        setFont(t.sm());

        // Size
        setPreferredSize(new Dimension(getPreferredSize().width, s));
        setMinimumSize(new Dimension(50, s));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, s));

        // Colors
        setBackground(c.getBackgroundInput());
        setForeground(c.getTextPrimary());
        setSelectionColor(c.getBackgroundSelected());
        setSelectedTextColor(c.getPrimary());

        // Border
        createBorders();
        setBorder(defaultBorder);

        // Padding
        setMargin(new Insets(
            Spacing.INPUT_PADDING_V,
            Spacing.INPUT_PADDING_H,
            Spacing.INPUT_PADDING_V,
            Spacing.INPUT_PADDING_H
        ));

        // Focus listener
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                setBorder(focusBorder);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                setBorder(defaultBorder);
            }
        });
    }

    private void createBorders() {
        var c = ThemeManager.colors();
        int radius = Spacing.RADIUS_SM;

        defaultBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c.getBorderDefault(), 1),
            BorderFactory.createEmptyBorder(
                Spacing.INPUT_PADDING_V - 1,
                Spacing.INPUT_PADDING_H - 1,
                Spacing.INPUT_PADDING_V - 1,
                Spacing.INPUT_PADDING_H - 1
            )
        );

        focusBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c.getBorderFocus(), 2),
            BorderFactory.createEmptyBorder(
                Spacing.INPUT_PADDING_V - 2,
                Spacing.INPUT_PADDING_H - 2,
                Spacing.INPUT_PADDING_V - 2,
                Spacing.INPUT_PADDING_H - 2
            )
        );
    }

    /**
     * Set the variant (error, success, etc.).
     */
    public void setVariant(Variant variant) {
        this.variant = variant;
        updateBorder();
    }

    /**
     * Get the current variant.
     */
    public Variant getVariant() {
        return variant;
    }

    private void updateBorder() {
        var c = ThemeManager.colors();

        if (variant == Variant.ERROR) {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c.getDanger(), 2),
                BorderFactory.createEmptyBorder(
                    Spacing.INPUT_PADDING_V - 2,
                    Spacing.INPUT_PADDING_H - 2,
                    Spacing.INPUT_PADDING_V - 2,
                    Spacing.INPUT_PADDING_H - 2
                )
            ));
        } else if (variant == Variant.SUCCESS) {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c.getSuccess(), 2),
                BorderFactory.createEmptyBorder(
                    Spacing.INPUT_PADDING_V - 2,
                    Spacing.INPUT_PADDING_H - 2,
                    Spacing.INPUT_PADDING_V - 2,
                    Spacing.INPUT_PADDING_H - 2
                )
            ));
        } else {
            if (hasFocus()) {
                setBorder(focusBorder);
            } else {
                setBorder(defaultBorder);
            }
        }
    }

    /**
     * Set placeholder text (hint).
     */
    public void setPlaceholder(String placeholder) {
        // Simple placeholder using focus listener
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (getText().isEmpty()) {
                    setText(placeholder);
                    setForeground(ThemeManager.colors().getTextTertiary());
                }
            }

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (getText().equals(placeholder)) {
                    setText("");
                    setForeground(ThemeManager.colors().getTextPrimary());
                }
            }
        });
    }
}
