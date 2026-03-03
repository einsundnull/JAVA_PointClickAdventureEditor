package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Scene Point Editor with tabbed interface.
 * Shows all items from scene with visibility checkboxes.
 * Has tabs for CustomClickArea, MovingRange, and Path.
 */
public class ScenePointEditor extends JDialog {
    private EditorMainSimple parent;
    private AdventureGame game;
    private Scene scene;

    // Item selection
    private JList<ItemWrapper> itemList;
    private List<ItemWrapper> itemWrappers;
    private ItemWrapper currentItemWrapper;

    // Tabbed pane
    private JTabbedPane tabbedPane;

    // Point containers for each tab
    private JPanel customClickAreaContainer;
    private JPanel movingRangeContainer;
    private JPanel pathContainer;

    // Point panels
    private List<PointPanelNew> customClickAreaPanels = new ArrayList<>();
    private List<PointPanelNew> movingRangePanels = new ArrayList<>();
    private List<PointPanelNew> pathPanels = new ArrayList<>();

    // Selected point index per tab
    private int selectedCustomClickIndex = -1;
    private int selectedMovingRangeIndex = -1;
    private int selectedPathIndex = -1;

    // Add by Click mode
    private JButton addByClickBtn;
    private boolean addByClickModeActive = false;

    // Track minimized windows
    private List<java.awt.Window> minimizedWindows;

    /**
     * Wrapper class for Item with visibility and movement state
     */
    private static class ItemWrapper {
        Item item;
        JCheckBox showCheckbox;
        JCheckBox followCheckbox;
        JCheckBox clickCheckbox;

        ItemWrapper(Item item) {
            this.item = item;
            this.showCheckbox = new JCheckBox("", item.isVisibleInEditor());
            this.followCheckbox = new JCheckBox("", item.isFollowingMouse());
            this.clickCheckbox = new JCheckBox("", item.isFollowingOnMouseClick());
        }

        @Override
        public String toString() {
            return item.getName();
        }
    }

    public ScenePointEditor(EditorMainSimple parent, Scene scene, Item initialItem) {
        super(parent, "Scene Point Editor", false);

        this.parent = parent;
        this.game = parent.getGame();
        this.scene = scene;
        this.itemWrappers = new ArrayList<>();
        this.minimizedWindows = new ArrayList<>();

        // Load items from scene
        if (scene != null && scene.getItems() != null) {
            for (Item item : scene.getItems()) {
                ItemWrapper wrapper = new ItemWrapper(item);
                itemWrappers.add(wrapper);
                if (item == initialItem) {
                    currentItemWrapper = wrapper;
                }
            }
        }

        setSize(900, 700);
        setLocationRelativeTo(parent);
        setAlwaysOnTop(true);

        // CRITICAL: Enable path visualization to render all point types
        System.out.println("🟢 ScenePointEditor: Enabling showPaths...");
        game.setShowPaths(true);
        System.out.println("🟢 ScenePointEditor: showPaths enabled. Current value: " + game.isShowPaths());

        // CRITICAL: Register this editor with game for bidirectional live-update
        System.out.println("🟢 ScenePointEditor: Registering editor with game...");
        game.setScenePointEditor(this);
        System.out.println("🟢 ScenePointEditor: Editor registered successfully.");

        initUI();

        // Select initial item, or select first item if no initial item was provided
        if (currentItemWrapper != null) {
            itemList.setSelectedValue(currentItemWrapper, true);
        } else if (!itemWrappers.isEmpty()) {
            // Auto-select first item if no initial item was specified
            itemList.setSelectedIndex(0);
            currentItemWrapper = itemWrappers.get(0);
            parent.log("Auto-selected first item: " + currentItemWrapper.item.getName());
        } else {
            parent.log("No items in scene - editor opened without selection");
        }

        minimizeOtherWindows();

        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
            requestFocus();
        });
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Scene Point Editor");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Left panel: Item list with checkboxes
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 5));
        leftPanel.setPreferredSize(new Dimension(220, 0));

        JLabel itemsLabel = new JLabel("Items in Scene:");
        itemsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        itemsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        leftPanel.add(itemsLabel, BorderLayout.NORTH);

        // Item list with custom renderer
        itemList = new JList<>(itemWrappers.toArray(new ItemWrapper[0]));
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setCellRenderer(new ItemCellRenderer());
        itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onItemSelected();
            }
        });

        // Add mouse listener for checkbox clicks
        itemList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int index = itemList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    ItemWrapper wrapper = itemList.getModel().getElementAt(index);

                    // Get the cell bounds
                    java.awt.Rectangle cellBounds = itemList.getCellBounds(index, index);

                    // New vertical layout calculation:
                    // Left panel: [Thumbnail 50px + name] - approximately 60-70px total width
                    // Spacing: 5px
                    // Right panel: Checkboxes start at approximately 65-75px from left
                    int checkboxPanelStartX = 65;
                    int relativeClickX = e.getX() - cellBounds.x;
                    int relativeClickY = e.getY() - cellBounds.y;

                    // Check if click was in checkbox panel area
                    if (relativeClickX >= checkboxPanelStartX) {
                        // Determine which checkbox was clicked based on Y position
                        // Each checkbox is approximately 15-20px high with 1px spacing
                        // Show: 0-16px, Follow: 17-33px, Click: 34-50px

                        if (relativeClickY < 17) {
                            // Show checkbox - use synchronized method
                            boolean newState = !wrapper.showCheckbox.isSelected();
                            game.syncVisibleInEditorCheckbox(wrapper.item, newState);
                            parent.log((newState ? "✓ Showing" : "✗ Hiding") + " item: " + wrapper.item.getName());
                        } else if (relativeClickY < 34) {
                            // Follow checkbox - use synchronized method
                            boolean newState = !wrapper.followCheckbox.isSelected();
                            game.syncFollowingMouseCheckbox(wrapper.item, newState);
                            parent.log((newState ? "✓ Enabled" : "✗ Disabled") + " following mouse for: " + wrapper.item.getName());
                        } else if (relativeClickY < 51) {
                            // Click checkbox - use synchronized method
                            boolean newState = !wrapper.clickCheckbox.isSelected();
                            game.syncFollowingOnClickCheckbox(wrapper.item, newState);
                            parent.log((newState ? "✓ Enabled" : "✗ Disabled") + " following on click for: " + wrapper.item.getName());
                        }

                        game.repaint();
                        itemList.repaint();
                    }
                }
            }
        });

        JScrollPane itemScrollPane = new JScrollPane(itemList);
        leftPanel.add(itemScrollPane, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);

        // Center panel: Tabbed pane with point editors
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 10));

        // Tab 1: CustomClickArea
        JPanel tab1 = createTabPanel("CustomClickArea");
        customClickAreaContainer = (JPanel) tab1.getClientProperty("container");
        tabbedPane.addTab("CustomClickArea", tab1);

        // Tab 2: MovingRange
        JPanel tab2 = createTabPanel("MovingRange");
        movingRangeContainer = (JPanel) tab2.getClientProperty("container");
        tabbedPane.addTab("MovingRange", tab2);

        // Tab 3: Path
        JPanel tab3 = createTabPanel("Path");
        pathContainer = (JPanel) tab3.getClientProperty("container");
        tabbedPane.addTab("Path", tab3);

        // Listen to tab changes
        tabbedPane.addChangeListener(e -> onTabChanged());

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel: Close button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> closeEditor());
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Window close listener
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeEditor();
            }
        });

        // Key listener for DELETE
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedPointInCurrentTab();
                }
            }
        });

        setFocusable(true);
    }

    /**
     * Create a tab panel with points list and buttons
     */
    private JPanel createTabPanel(String pointType) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Points container
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addPointInCurrentTab());
        buttonsPanel.add(addBtn);

        addByClickBtn = new JButton("Add by Click");
        addByClickBtn.addActionListener(e -> toggleAddByClickMode());
        buttonsPanel.add(addByClickBtn);

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeSelectedPointInCurrentTab());
        buttonsPanel.add(removeBtn);

        JButton upBtn = new JButton("↑");
        upBtn.addActionListener(e -> movePointUpInCurrentTab());
        buttonsPanel.add(upBtn);

        JButton downBtn = new JButton("↓");
        downBtn.addActionListener(e -> movePointDownInCurrentTab());
        buttonsPanel.add(downBtn);

        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Store container reference
        mainPanel.putClientProperty("container", container);

        return mainPanel;
    }

    /**
     * Custom cell renderer for item list with checkboxes
     */
    private class ItemCellRenderer extends JPanel implements javax.swing.ListCellRenderer<ItemWrapper> {
        private JLabel thumbnailLabel;
        private JLabel nameLabel;
        private JCheckBox showCheckBox;
        private JCheckBox followCheckBox;
        private JCheckBox clickCheckBox;

        public ItemCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // Left panel: Thumbnail and name stacked vertically
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setOpaque(false);

            // Thumbnail
            thumbnailLabel = new JLabel();
            thumbnailLabel.setPreferredSize(new Dimension(50, 50));
            thumbnailLabel.setMinimumSize(new Dimension(50, 50));
            thumbnailLabel.setMaximumSize(new Dimension(50, 50));
            thumbnailLabel.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY, 1));
            thumbnailLabel.setHorizontalAlignment(JLabel.CENTER);
            thumbnailLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

            // Item name - below thumbnail
            nameLabel = new JLabel();
            nameLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
            nameLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

            leftPanel.add(thumbnailLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            leftPanel.add(nameLabel);

            // Right panel: Checkboxes stacked vertically
            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
            checkBoxPanel.setOpaque(false);

            // Show checkbox
            showCheckBox = new JCheckBox("Show");
            showCheckBox.setOpaque(false);
            showCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
            showCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

            // Follow checkbox
            followCheckBox = new JCheckBox("Follow");
            followCheckBox.setOpaque(false);
            followCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
            followCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

            // Click checkbox
            clickCheckBox = new JCheckBox("Click");
            clickCheckBox.setOpaque(false);
            clickCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
            clickCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

            checkBoxPanel.add(showCheckBox);
            checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 1)));
            checkBoxPanel.add(followCheckBox);
            checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 1)));
            checkBoxPanel.add(clickCheckBox);

            // Assemble the cell
            add(leftPanel);
            add(Box.createRigidArea(new Dimension(5, 0)));
            add(checkBoxPanel);
            add(Box.createHorizontalGlue());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ItemWrapper> list, ItemWrapper value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                // Load thumbnail
                try {
                    String imagePath = value.item.getImageFilePath();
                    if (imagePath != null && !imagePath.isEmpty()) {
                        File imageFile = ResourcePathHelper.findImageFile(imagePath);
                        if (imageFile == null) {
                            imageFile = new File(imagePath);
                        }

                        if (imageFile != null && imageFile.exists()) {
                            javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imageFile.getAbsolutePath());
                            java.awt.Image img = icon.getImage().getScaledInstance(48, 48, java.awt.Image.SCALE_SMOOTH);
                            thumbnailLabel.setIcon(new javax.swing.ImageIcon(img));
                            thumbnailLabel.setText("");
                        } else {
                            thumbnailLabel.setIcon(null);
                            thumbnailLabel.setText("?");
                        }
                    } else {
                        thumbnailLabel.setIcon(null);
                        thumbnailLabel.setText("?");
                    }
                } catch (Exception e) {
                    thumbnailLabel.setIcon(null);
                    thumbnailLabel.setText("!");
                }

                nameLabel.setText(value.item.getName());
                showCheckBox.setSelected(value.showCheckbox.isSelected());
                followCheckBox.setSelected(value.followCheckbox.isSelected());
                clickCheckBox.setSelected(value.clickCheckbox.isSelected());
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                nameLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                nameLabel.setForeground(list.getForeground());
            }

            return this;
        }
    }

    private void onItemSelected() {
        ItemWrapper selected = itemList.getSelectedValue();
        if (selected != null) {
            currentItemWrapper = selected;

            // Set as selected in scene
            if (scene != null) {
                scene.setSelectedItem(selected.item);
            }

            // Refresh all point panels
            refreshAllPoints();

            // Enable visibility for current tab type
            onTabChanged();

            game.repaint();
        }
    }

    private void onTabChanged() {
        if (currentItemWrapper == null) return;

        // Deactivate Add by Click mode when switching tabs to prevent confusion
        if (addByClickModeActive) {
            addByClickModeActive = false;
            addByClickBtn.setText("Add by Click");
            addByClickBtn.setBackground(null);
            addByClickBtn.setOpaque(false);
            game.setAddPointModeSimple(false, "", null);
            parent.log("✓ Add by Click disabled (tab changed)");
        }

        // Make all point types visible - they should all be rendered simultaneously
        // The tab only determines which type we're editing, not which is visible
        Item item = currentItemWrapper.item;
        item.setCustomClickAreaVisibleInEditor(true);
        item.setMovingRangeVisibleInEditor(true);
        item.setPathVisibleInEditor(true);

        game.repaint();
    }

    private void refreshAllPoints() {
        if (currentItemWrapper == null) return;
        Item item = currentItemWrapper.item;

        // Refresh CustomClickArea points
        customClickAreaPanels.clear();
        customClickAreaContainer.removeAll();
        CustomClickArea customArea = item.getPrimaryCustomClickArea();
        if (customArea != null && customArea.getPoints() != null) {
            for (int i = 0; i < customArea.getPoints().size(); i++) {
                Point p = customArea.getPoints().get(i);
                    PointPanelNew panel = new PointPanelNew(i, p, "CustomClickArea");
                    customClickAreaPanels.add(panel);
                    customClickAreaContainer.add(panel);
                    customClickAreaContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        customClickAreaContainer.revalidate();
        customClickAreaContainer.repaint();

        // Refresh MovingRange points
        movingRangePanels.clear();
        movingRangeContainer.removeAll();
        MovingRange movingRange = item.getPrimaryMovingRange();
        if (movingRange != null && movingRange.getPoints() != null) {
            for (int i = 0; i < movingRange.getPoints().size(); i++) {
                Point p = movingRange.getPoints().get(i);
                    PointPanelNew panel = new PointPanelNew(i, p, "MovingRange");
                    movingRangePanels.add(panel);
                    movingRangeContainer.add(panel);
                    movingRangeContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        movingRangeContainer.revalidate();
        movingRangeContainer.repaint();

        // Refresh Path points
        pathPanels.clear();
        pathContainer.removeAll();
        Path primaryPath = item.getPrimaryPath();
        if (primaryPath != null && primaryPath.getPoints() != null) {
            for (int i = 0; i < primaryPath.getPoints().size(); i++) {
                Point p = primaryPath.getPoints().get(i);
                    PointPanelNew panel = new PointPanelNew(i, p, "Path");
                    pathPanels.add(panel);
                    pathContainer.add(panel);
                    pathContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
        pathContainer.revalidate();
        pathContainer.repaint();
    }

    /**
     * Inner class for a single point panel
     */
    private class PointPanelNew extends JPanel {
        private int index;
        private Point point;
        private String type;
        private JTextField xField;
        private JTextField yField;
        private boolean selected = false;

        public PointPanelNew(int index, Point point, String type) {
            this.index = index;
            this.point = point;
            this.type = type;

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            // Click to select
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectThis();
                    requestFocusInWindow();
                }
            });

            // Key listener for DELETE
            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                        removeSelectedPointInCurrentTab();
                    }
                }
            });

            setFocusable(true);
            initPanel();
        }

        private void initPanel() {
            // Left: Point number
            JLabel numberLabel = new JLabel("Point " + (index + 1) + ":");
            numberLabel.setFont(new Font("Arial", Font.BOLD, 12));
            numberLabel.setPreferredSize(new Dimension(80, 25));
            add(numberLabel, BorderLayout.WEST);

            // Center: X, Y fields
            JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

            fieldsPanel.add(new JLabel("x:"));
            xField = new JTextField(String.valueOf(point.x), 6);
            xField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    updatePoint();
                }
            });
            xField.addActionListener(e -> updatePoint());
            fieldsPanel.add(xField);

            fieldsPanel.add(new JLabel("y:"));
            yField = new JTextField(String.valueOf(point.y), 6);
            yField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    updatePoint();
                }
            });
            yField.addActionListener(e -> updatePoint());
            fieldsPanel.add(yField);

            add(fieldsPanel, BorderLayout.CENTER);
        }

        private void updatePoint() {
            try {
                int newX = Integer.parseInt(xField.getText());
                int newY = Integer.parseInt(yField.getText());
                point.x = newX;
                point.y = newY;

                // Auto-save item and MovingRange
                saveCurrentItem();
                if (type.equals("MovingRange")) {
                    saveMovingRange();
                }
                game.repaint();
            } catch (NumberFormatException e) {
                // Reset to current values
                xField.setText(String.valueOf(point.x));
                yField.setText(String.valueOf(point.y));
            }
        }

        private void selectThis() {
            // Update selection index based on type
            if (type.equals("CustomClickArea")) {
                selectedCustomClickIndex = index;
                for (PointPanelNew panel : customClickAreaPanels) {
                    panel.setSelected(panel == this);
                }
            } else if (type.equals("MovingRange")) {
                selectedMovingRangeIndex = index;
                for (PointPanelNew panel : movingRangePanels) {
                    panel.setSelected(panel == this);
                }
            } else if (type.equals("Path")) {
                selectedPathIndex = index;
                for (PointPanelNew panel : pathPanels) {
                    panel.setSelected(panel == this);
                }
            }

            // Highlight in scene
            if (currentItemWrapper != null) {
                game.setHighlightedPoint(currentItemWrapper.item, type, index);
                game.repaint();
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                setBackground(new Color(200, 230, 255));
            } else {
                setBackground(null);
            }
        }

        public void updateFields() {
            xField.setText(String.valueOf(point.x));
            yField.setText(String.valueOf(point.y));
        }
    }

    // Button actions
    private void addPointInCurrentTab() {
        if (currentItemWrapper == null) return;

        int tab = tabbedPane.getSelectedIndex();
        Point newPoint = new Point(100, 100);

        if (tab == 0) { // CustomClickArea
            CustomClickArea area = currentItemWrapper.item.ensurePrimaryCustomClickArea();
            area.addPoint(newPoint);
        } else if (tab == 1) { // MovingRange
            MovingRange range = currentItemWrapper.item.ensurePrimaryMovingRange();
            range.addPoint(newPoint);
            saveMovingRange();
        } else if (tab == 2) { // Path
            Path path = currentItemWrapper.item.ensurePrimaryPath();
            path.addPoint(newPoint);
        }

        saveCurrentItem();
        refreshAllPoints();
        game.repaint();
    }

    /**
     * Toggle Add by Click mode on/off
     * Changes button text between "Add by Click" and "Stop Adding"
     * Provides visual feedback with button color
     */
    private void toggleAddByClickMode() {
        addByClickModeActive = !addByClickModeActive;

        if (addByClickModeActive) {
            enableAddByClickForCurrentTab();
            addByClickBtn.setText("Stop Adding");
            addByClickBtn.setBackground(new Color(100, 200, 100)); // Light green
            addByClickBtn.setOpaque(true);
        } else {
            disableAddByClickMode();
            addByClickBtn.setText("Add by Click");
            addByClickBtn.setBackground(null);
            addByClickBtn.setOpaque(false);
        }
    }

    /**
     * Enable Add by Click mode for current tab
     */
    private void enableAddByClickForCurrentTab() {
        if (currentItemWrapper == null) {
            System.out.println("❌ enableAddByClickForCurrentTab: currentItemWrapper is NULL!");
            addByClickModeActive = false; // Reset state
            addByClickBtn.setText("Add by Click");
            addByClickBtn.setBackground(null);
            addByClickBtn.setOpaque(false);
            return;
        }

        int tab = tabbedPane.getSelectedIndex();
        String pointType = "";

        if (tab == 0) pointType = "CustomClickArea";
        else if (tab == 1) pointType = "MovingRange";
        else if (tab == 2) pointType = "Path";

        System.out.println("🎯 Enabling Add by Click mode for: " + pointType + " (item: " + currentItemWrapper.item.getName() + ")");
        game.setAddPointModeSimple(true, pointType, currentItemWrapper.item);
        game.setScenePointEditor(this);
        System.out.println("✅ Add by Click mode enabled!");

        parent.log("✓ Add by Click enabled for " + pointType);
    }

    /**
     * Disable Add by Click mode
     */
    private void disableAddByClickMode() {
        System.out.println("🛑 Disabling Add by Click mode");
        game.setAddPointModeSimple(false, "", null);
        game.setCursor(java.awt.Cursor.getDefaultCursor());
        System.out.println("✅ Add by Click mode disabled!");

        parent.log("✓ Add by Click disabled");
    }

    private void removeSelectedPointInCurrentTab() {
        if (currentItemWrapper == null) return;

        int tab = tabbedPane.getSelectedIndex();

        if (tab == 0 && selectedCustomClickIndex >= 0) {
            CustomClickArea area = currentItemWrapper.item.getPrimaryCustomClickArea();
            if (area != null) {
                area.removePoint(selectedCustomClickIndex);
                selectedCustomClickIndex = -1;
            }
        } else if (tab == 1 && selectedMovingRangeIndex >= 0) {
            MovingRange range = currentItemWrapper.item.getPrimaryMovingRange();
            if (range != null) {
                range.removePoint(selectedMovingRangeIndex);
                selectedMovingRangeIndex = -1;
                saveMovingRange();
            }
        } else if (tab == 2 && selectedPathIndex >= 0) {
            Path path = currentItemWrapper.item.getPrimaryPath();
            if (path != null) {
                path.removePoint(selectedPathIndex);
                selectedPathIndex = -1;
            }
        }

        saveCurrentItem();
        refreshAllPoints();
        game.repaint();
    }

    private void movePointUpInCurrentTab() {
        if (currentItemWrapper == null) return;

        int tab = tabbedPane.getSelectedIndex();

        if (tab == 0 && selectedCustomClickIndex > 0) {
            CustomClickArea area = currentItemWrapper.item.getPrimaryCustomClickArea();
            if (area != null) {
                List<Point> points = area.getPoints();
                Point temp = points.get(selectedCustomClickIndex);
                points.set(selectedCustomClickIndex, points.get(selectedCustomClickIndex - 1));
                points.set(selectedCustomClickIndex - 1, temp);
                selectedCustomClickIndex--;
            }
        } else if (tab == 1 && selectedMovingRangeIndex > 0) {
            MovingRange range = currentItemWrapper.item.getPrimaryMovingRange();
            if (range != null) {
                List<Point> points = range.getPoints();
                Point temp = points.get(selectedMovingRangeIndex);
                points.set(selectedMovingRangeIndex, points.get(selectedMovingRangeIndex - 1));
                points.set(selectedMovingRangeIndex - 1, temp);
                selectedMovingRangeIndex--;
                saveMovingRange();
            }
        } else if (tab == 2 && selectedPathIndex > 0) {
            Path path = currentItemWrapper.item.getPrimaryPath();
            if (path != null) {
                List<Point> points = path.getPoints();
                Point temp = points.get(selectedPathIndex);
                points.set(selectedPathIndex, points.get(selectedPathIndex - 1));
                points.set(selectedPathIndex - 1, temp);
                selectedPathIndex--;
            }
        }

        saveCurrentItem();
        refreshAllPoints();
        game.repaint();
    }

    private void movePointDownInCurrentTab() {
        if (currentItemWrapper == null) return;

        int tab = tabbedPane.getSelectedIndex();

        if (tab == 0 && selectedCustomClickIndex >= 0) {
            CustomClickArea area = currentItemWrapper.item.getPrimaryCustomClickArea();
            if (area != null) {
                List<Point> points = area.getPoints();
                if (selectedCustomClickIndex < points.size() - 1) {
                    Point temp = points.get(selectedCustomClickIndex);
                    points.set(selectedCustomClickIndex, points.get(selectedCustomClickIndex + 1));
                    points.set(selectedCustomClickIndex + 1, temp);
                    selectedCustomClickIndex++;
                }
            }
        } else if (tab == 1 && selectedMovingRangeIndex >= 0) {
            MovingRange range = currentItemWrapper.item.getPrimaryMovingRange();
            if (range != null) {
                List<Point> points = range.getPoints();
                if (selectedMovingRangeIndex < points.size() - 1) {
                    Point temp = points.get(selectedMovingRangeIndex);
                    points.set(selectedMovingRangeIndex, points.get(selectedMovingRangeIndex + 1));
                    points.set(selectedMovingRangeIndex + 1, temp);
                    selectedMovingRangeIndex++;
                    saveMovingRange();
                }
            }
        } else if (tab == 2 && selectedPathIndex >= 0) {
            Path path = currentItemWrapper.item.getPrimaryPath();
            if (path != null) {
                List<Point> points = path.getPoints();
                if (selectedPathIndex < points.size() - 1) {
                    Point temp = points.get(selectedPathIndex);
                    points.set(selectedPathIndex, points.get(selectedPathIndex + 1));
                    points.set(selectedPathIndex + 1, temp);
                    selectedPathIndex++;
                }
            }
        }

        saveCurrentItem();
        refreshAllPoints();
        game.repaint();
    }

    /**
     * Called when a point is added by clicking on canvas
     */
    public void addPointAtPosition(int x, int y, String pointType) {
        if (currentItemWrapper == null) {
            System.out.println("❌ addPointAtPosition: currentItemWrapper is NULL!");
            return;
        }

        System.out.println("🔵 addPointAtPosition: Adding " + pointType + " point at (" + x + ", " + y + ") to item: " + currentItemWrapper.item.getName());
        Point newPoint = new Point(x, y);

        if (pointType.equals("CustomClickArea")) {
            CustomClickArea area = currentItemWrapper.item.ensurePrimaryCustomClickArea();
            area.addPoint(newPoint);
            System.out.println("✅ CustomClickArea now has " + area.getPoints().size() + " points");
        } else if (pointType.equals("MovingRange")) {
            MovingRange range = currentItemWrapper.item.ensurePrimaryMovingRange();
            range.addPoint(newPoint);
            saveMovingRange();
            System.out.println("✅ MovingRange now has " + range.getPoints().size() + " points");
        } else if (pointType.equals("Path")) {
            Path path = currentItemWrapper.item.ensurePrimaryPath();
            path.addPoint(newPoint);
            System.out.println("✅ Path now has " + path.getPoints().size() + " points");
        }

        System.out.println("🔵 Saving item and updating scene...");
        saveCurrentItem(); // This now also updates the scene reference
        System.out.println("🔵 Refreshing all points in UI...");
        refreshAllPoints();
        System.out.println("✅ Point added successfully!");
        System.out.println("✅ showPaths is currently: " + game.isShowPaths());

        parent.log("Added point at (" + x + ", " + y + ") for " + pointType);
    }

    /**
     * Refresh all point fields for the currently selected item (called during item drag)
     */
    public void refreshPointFieldsForCurrentItem() {
        refreshPointFieldsForItem(null);
    }

    /**
     * Refresh point fields for a specific item or the currently selected one
     */
    public void refreshPointFieldsForItem(Item item) {
        System.out.println("🔄 DEBUG: refreshPointFieldsForItem() CALLED with item=" +
            (item != null ? item.getName() : "null"));

        // If no item specified, use current item
        if (item == null && currentItemWrapper != null) {
            item = currentItemWrapper.item;
        }

        // Check if the item is the currently selected one
        if (currentItemWrapper == null || item == null || currentItemWrapper.item != item) {
            // Debug log
            if (currentItemWrapper != null && item != null) {
                System.out.println("❌ DEBUG: refreshPointFieldsForItem - Item mismatch! " +
                    "draggedItem=" + item.getName() + " vs currentItem=" + currentItemWrapper.item.getName() +
                    " (same object? " + (currentItemWrapper.item == item) + ")");
            } else if (currentItemWrapper == null) {
                System.out.println("❌ DEBUG: currentItemWrapper is NULL");
            } else if (item == null) {
                System.out.println("❌ DEBUG: item parameter is NULL");
            }
            return; // Only update if this is the currently displayed item
        }

        // Debug log
        System.out.println("DEBUG: refreshPointFieldsForItem - Updating fields for item: " + item.getName() +
            " | CustomClickArea panels: " + customClickAreaPanels.size() +
            " | MovingRange panels: " + movingRangePanels.size() +
            " | Path panels: " + pathPanels.size());

        // Update all CustomClickArea panels
        for (PointPanelNew panel : customClickAreaPanels) {
            panel.updateFields();
        }

        // Update all MovingRange panels
        for (PointPanelNew panel : movingRangePanels) {
            panel.updateFields();
        }

        // Update all Path panels
        for (PointPanelNew panel : pathPanels) {
            panel.updateFields();
        }
    }

    /**
     * Update a point's coordinates (called during drag)
     */
    public void updatePointInList(int index, int newX, int newY, String pointType) {
        if (currentItemWrapper == null) return;

        int tab = tabbedPane.getSelectedIndex();
        String currentType = tab == 0 ? "CustomClickArea" : (tab == 1 ? "MovingRange" : "Path");

        if (!currentType.equals(pointType)) return;

        List<PointPanelNew> panels = null;
        if (pointType.equals("CustomClickArea")) panels = customClickAreaPanels;
        else if (pointType.equals("MovingRange")) panels = movingRangePanels;
        else if (pointType.equals("Path")) panels = pathPanels;

        if (panels != null && index >= 0 && index < panels.size()) {
            PointPanelNew panel = panels.get(index);
            panel.point.x = newX;
            panel.point.y = newY;
            panel.updateFields();
            panel.selectThis();

            SwingUtilities.invokeLater(() -> {
                panel.scrollRectToVisible(panel.getBounds());
            });
        }
    }

    /**
     * Select a point by clicking in scene (called when user clicks point in scene)
     */
    public void selectPointFromScene(Item item, int index, String pointType) {
        if (currentItemWrapper == null || currentItemWrapper.item != item) {
            // Find and select the item
            for (int i = 0; i < itemWrappers.size(); i++) {
                if (itemWrappers.get(i).item == item) {
                    itemList.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Switch to appropriate tab
        int targetTab = -1;
        if (pointType.equals("CustomClickArea")) targetTab = 0;
        else if (pointType.equals("MovingRange")) targetTab = 1;
        else if (pointType.equals("Path")) targetTab = 2;

        if (targetTab >= 0 && tabbedPane.getSelectedIndex() != targetTab) {
            tabbedPane.setSelectedIndex(targetTab);
        }

        // Select the point
        List<PointPanelNew> panels = null;
        if (pointType.equals("CustomClickArea")) panels = customClickAreaPanels;
        else if (pointType.equals("MovingRange")) panels = movingRangePanels;
        else if (pointType.equals("Path")) panels = pathPanels;

        if (panels != null && index >= 0 && index < panels.size()) {
            PointPanelNew panel = panels.get(index);
            panel.selectThis();

            SwingUtilities.invokeLater(() -> {
                panel.scrollRectToVisible(panel.getBounds());
                panel.requestFocusInWindow();
            });
        }
    }

    private void saveCurrentItem() {
        if (currentItemWrapper == null) return;

        try {
            ItemSaver.saveItemByName(currentItemWrapper.item);
            parent.log("✓ Auto-saved: " + currentItemWrapper.item.getName());

            // CRITICAL: Update item reference in scene!
            // Without this, the scene keeps the old item reference and doesn't show changes
            Scene currentScene = game.getCurrentScene();
            if (currentScene != null) {
                List<Item> items = currentScene.getItems();
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).getName().equals(currentItemWrapper.item.getName())) {
                        items.set(i, currentItemWrapper.item);
                        break;
                    }
                }
            }

            // CRITICAL: Repaint game to show changes immediately
            game.repaint();
        } catch (Exception e) {
            parent.log("ERROR saving: " + e.getMessage());
        }
    }

    /**
     * Saves the current item's MovingRange to MovingRangeManager.
     * This ensures that MovingRange changes are immediately persisted to the file system.
     */
    private void saveMovingRange() {
        if (currentItemWrapper == null) return;

        MovingRange range = currentItemWrapper.item.getPrimaryMovingRange();
        if (range != null && range.getName() != null) {
            MovingRangeManager.save(range);
            parent.log("✓ MovingRange saved: " + range.getName());
        }
    }

    private void minimizeOtherWindows() {
        java.awt.Window[] windows = java.awt.Window.getWindows();
        for (java.awt.Window window : windows) {
            if (window == this || window == game) {
                continue;
            }
            if (window.isVisible() && (window instanceof javax.swing.JFrame || window instanceof javax.swing.JDialog)) {
                minimizedWindows.add(window);
                window.setVisible(false);
            }
        }
    }

    private void restoreWindows() {
        for (java.awt.Window window : minimizedWindows) {
            window.setVisible(true);
        }
        minimizedWindows.clear();
    }

    private void closeEditor() {
        // Disable add by click mode
        if (addByClickModeActive) {
            addByClickModeActive = false;
            game.setAddPointModeSimple(false, "", null);
        }
        game.clearHighlightedPoint();

        // Disable path visualization when closing
        game.setShowPaths(false);

        // CRITICAL: Unregister this editor from game
        game.setScenePointEditor(null);

        restoreWindows();
        dispose();

        parent.log("✓ Scene Point Editor closed");
    }

    // ==================== BIDIRECTIONAL CHECKBOX REFRESH METHODS ====================

    /**
     * Refresh "Show" checkbox for an item (called from AdventureGame when checkbox changes in EditorMainSimple)
     */
    public void refreshShowCheckbox(Item item, boolean visible) {
        if (item == null) return;

        // Find the wrapper for this item and update its checkbox
        for (ItemWrapper wrapper : itemWrappers) {
            if (wrapper.item == item) {
                wrapper.showCheckbox.setSelected(visible);
                itemList.repaint();
                return;
            }
        }
    }

    /**
     * Refresh "Follow" checkbox for an item (called from AdventureGame when checkbox changes in EditorMainSimple)
     */
    public void refreshFollowingMouseCheckbox(Item item, boolean following) {
        if (item == null) return;

        // Find the wrapper for this item and update its checkbox
        for (ItemWrapper wrapper : itemWrappers) {
            if (wrapper.item == item) {
                wrapper.followCheckbox.setSelected(following);
                itemList.repaint();
                return;
            }
        }
    }

    /**
     * Refresh "Click" checkbox for an item (called from AdventureGame when checkbox changes in EditorMainSimple)
     */
    public void refreshFollowingOnClickCheckbox(Item item, boolean followingOnClick) {
        if (item == null) return;

        // Find the wrapper for this item and update its checkbox
        for (ItemWrapper wrapper : itemWrappers) {
            if (wrapper.item == item) {
                wrapper.clickCheckbox.setSelected(followingOnClick);
                itemList.repaint();
                return;
            }
        }
    }
}
