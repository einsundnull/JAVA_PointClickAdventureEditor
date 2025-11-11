package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A panel with a clickable header that can collapse/expand its content.
 * Similar to an accordion panel.
 */
public class CollapsablePanel extends JPanel {

    private JPanel headerPanel;
    private JLabel titleLabel;
    private JLabel iconLabel;
    private JPanel contentPanel;
    private boolean collapsed;
    private Dimension expandedSize;

    /**
     * Create a collapsable panel with a title.
     *
     * @param title The title to display in the header
     */
    public CollapsablePanel(String title) {
        this(title, false);
    }

    /**
     * Create a collapsable panel with a title and initial state.
     *
     * @param title The title to display in the header
     * @param startCollapsed Whether to start in collapsed state
     */
    public CollapsablePanel(String title, boolean startCollapsed) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        this.collapsed = false; // Will be set properly by setCollapsed()

        // === HEADER ===
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(220, 220, 220));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Icon (▼ or ▶)
        iconLabel = new JLabel("▼");
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        // Title
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Click listener to toggle
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCollapsed();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBackground(new Color(200, 200, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(new Color(220, 220, 220));
            }
        });

        add(headerPanel, BorderLayout.NORTH);

        // === CONTENT ===
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        // Set initial state
        setCollapsed(startCollapsed);
    }

    /**
     * Set the content of this collapsable panel.
     *
     * @param component The component to display as content
     */
    public void setContent(Component component) {
        contentPanel.removeAll();
        contentPanel.add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Get the content panel (to add components directly).
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * Toggle between collapsed and expanded state.
     */
    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    /**
     * Set the collapsed state.
     *
     * @param collapsed true to collapse, false to expand
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;

        if (collapsed) {
            // Save current size before collapsing
            if (!this.collapsed && getHeight() > 0) {
                expandedSize = getSize();
            }

            // Hide content
            contentPanel.setVisible(false);
            iconLabel.setText("▶");

            // Set both maximum and preferred size to header only
            int collapsedHeight = headerPanel.getPreferredSize().height + 10;
            Dimension collapsedDim = new Dimension(Integer.MAX_VALUE, collapsedHeight);
            setMaximumSize(collapsedDim);
            setPreferredSize(new Dimension(getWidth(), collapsedHeight));
        } else {
            // Show content
            contentPanel.setVisible(true);
            iconLabel.setText("▼");

            // Restore maximum size to unlimited
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Clear preferred size to allow content to determine size
            setPreferredSize(null);
        }

        revalidate();
        repaint();

        // Notify parent to revalidate recursively up the hierarchy
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent.repaint();
            parent = parent.getParent();
        }
    }

    /**
     * Check if currently collapsed.
     */
    public boolean isCollapsed() {
        return collapsed;
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
     * Add a button to the header (right side).
     */
    public void addHeaderButton(JButton button) {
        addHeaderComponent(button);
    }

    /**
     * Add a component (button, checkbox, etc.) to the header (right side).
     */
    public void addHeaderComponent(JComponent component) {
        // Get existing button panel or create new one
        Component[] components = headerPanel.getComponents();
        JPanel buttonPanel = null;

        for (Component comp : components) {
            if (comp instanceof JPanel && headerPanel.getLayout() instanceof BorderLayout) {
                Object constraints = ((BorderLayout) headerPanel.getLayout()).getConstraints(comp);
                if (BorderLayout.EAST.equals(constraints)) {
                    buttonPanel = (JPanel) comp;
                    break;
                }
            }
        }

        if (buttonPanel == null) {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            buttonPanel.setOpaque(false);
            headerPanel.add(buttonPanel, BorderLayout.EAST);
        }

        buttonPanel.add(component);
        revalidate();
    }
}
