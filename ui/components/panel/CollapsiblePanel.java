package main.ui.components.panel;

import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern collapsible panel (accordion style).
 * Features:
 * - Clickable header to toggle
 * - Smooth transition effect (via repaint)
 * - Icon indicator (expanded/collapsed)
 * - Optional header buttons
 */
public class CollapsiblePanel extends JPanel {

    private JPanel headerPanel;
    private JLabel titleLabel;
    private JLabel iconLabel;
    private JPanel contentPanel;
    private JPanel buttonPanel;

    private boolean collapsed = false;

    /**
     * Create a collapsible panel with a title.
     */
    public CollapsiblePanel(String title) {
        this(title, false);
    }

    /**
     * Create a collapsible panel with title and initial state.
     */
    public CollapsiblePanel(String title, boolean startCollapsed) {
        super(new BorderLayout());
        init(title, startCollapsed);
    }

    private void init(String title, boolean startCollapsed) {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();

        setOpaque(false);

        // === HEADER ===
        headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw bottom border
                g.setColor(c.getBorderDefault());
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        headerPanel.setOpaque(true);
        headerPanel.setBackground(c.getBackgroundElevated());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(
            Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM
        ));
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon (▼ or ▶)
        iconLabel = new JLabel("▼");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        iconLabel.setForeground(c.getTextSecondary());
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Spacing.SM));

        // Title
        titleLabel = new JLabel(title);
        titleLabel.setFont(t.semiboldBase());
        titleLabel.setForeground(c.getTextPrimary());

        // Left panel for icon + title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(iconLabel);
        leftPanel.add(titleLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // Click listener to toggle
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCollapsed();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBackground(c.getBackgroundHover());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(c.getBackgroundElevated());
            }
        });

        add(headerPanel, BorderLayout.NORTH);

        // === CONTENT ===
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        add(contentPanel, BorderLayout.CENTER);

        // Set initial state
        setCollapsed(startCollapsed);
    }

    /**
     * Set the content of this collapsible panel.
     */
    public void setContent(Component component) {
        contentPanel.removeAll();
        contentPanel.add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Get the content panel for adding components directly.
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * Set the title text.
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * Get the title text.
     */
    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * Toggle between collapsed and expanded state.
     */
    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    /**
     * Set the collapsed state.
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;

        if (collapsed) {
            contentPanel.setVisible(false);
            iconLabel.setText("▶");
        } else {
            contentPanel.setVisible(true);
            iconLabel.setText("▼");
        }

        revalidate();
        repaint();
    }

    /**
     * Check if currently collapsed.
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Add a button to the header (right side).
     */
    public void addHeaderButton(JButton button) {
        addHeaderComponent(button);
    }

    /**
     * Add a component to the header (right side).
     */
    public void addHeaderComponent(JComponent component) {
        if (buttonPanel == null) {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.XS, 0));
            buttonPanel.setOpaque(false);
            headerPanel.add(buttonPanel, BorderLayout.EAST);
        }
        buttonPanel.add(component);
        revalidate();
        repaint();
    }
}
