package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * Simplified Editor Main Window - Based on "Schema Editor Simple.txt"
 *
 * Structure:
 * - Scenes Section: TreeView (Scene → SubScene) + Buttons
 * - Items Section: ListView + Inline Properties Panel + Buttons
 * - No separate dialog windows for editing
 */
public class EditorMainSimple extends JFrame {
    private AdventureGame game;
    private JTextArea outputArea;
    private EditorSettings settings;

    // Track unsaved changes
    private boolean hasUnsavedChanges = false;

    // Split panes (for saving divider positions)
    private JSplitPane mainSplitPane;
    private JSplitPane itemsSplitPane;

    // Scenes Section (JTree for hierarchy)
    private JTree scenesTree;
    private DefaultMutableTreeNode scenesRootNode;
    private DefaultTreeModel scenesTreeModel;
    private JPanel scenesPanel;
    private JPanel scenesContentPanel;

    // Items Section
    private JPanel itemsListPanel; // Panel containing checkboxes + labels
    private Map<String, JCheckBox> itemCheckBoxes; // Map: itemName -> show checkbox
    private Map<String, JCheckBox> itemFollowingMouseCheckBoxes; // Map: itemName -> following mouse checkbox
    private Map<String, JCheckBox> itemFollowingOnClickCheckBoxes; // Map: itemName -> following on click checkbox
    private Map<String, JLabel> itemLabels; // Map: itemName -> label
    private JPanel itemsPanel;
    private JPanel itemsContentPanel;
    private JPanel itemPropertiesPanel;
    private Item selectedItem;
    private String selectedItemName; // Currently selected item name

    // Item Properties UI Components
    private JLabel itemNameLabel;
    private JLabel itemImagePreview;
    private JTextField itemImagePathField;
    private JCheckBox itemImageShowCheckBox; // Show checkbox for hiding image in edit mode
    private JTextField itemHoverTextField;
    private JPanel itemConditionsPanel; // Legacy - will be replaced
    private javax.swing.JTable itemConditionsTable; // NEW: Selectable table
    private ConditionsTableModel conditionsTableModel; // NEW: Table model
    private JList<String> customClickAreaPointsList;
    private DefaultListModel<String> customClickAreaPointsModel;
    private JCheckBox customClickAreaShowCheckBox; // Show checkbox for hiding click area in edit mode
    private JList<String> movingRangePointsList;
    private DefaultListModel<String> movingRangePointsModel;
    private JCheckBox movingRangeShowCheckBox; // Show checkbox for hiding moving range in edit mode
    private JList<String> pathPointsList;
    private DefaultListModel<String> pathPointsModel;
    private JCheckBox pathShowCheckBox; // Show checkbox for hiding path in edit mode

    // Selected objects
    private Scene selectedScene;
    private Scene selectedSubScene;
    private String currentSceneName; // Parent scene name of currently selected SubScene

    public EditorMainSimple(AdventureGame game) {
        this.game = game;
        this.itemCheckBoxes = new HashMap<>();
        this.itemFollowingMouseCheckBoxes = new HashMap<>();
        this.itemFollowingOnClickCheckBoxes = new HashMap<>();
        this.itemLabels = new HashMap<>();
        this.selectedItemName = null;

        // Disable auto-save in editor mode
        AutoSaveManager.setEnabled(false);

        // Load settings
        settings = new EditorSettings();
        settings.load();

        setTitle("Main Editor (Simple) - ALT+E to toggle");

        // Apply saved window bounds or use defaults
        java.awt.Rectangle defaultBounds = new java.awt.Rectangle(100, 100, 1200, 800);
        java.awt.Rectangle bounds = settings.getWindowBounds("editor.main", defaultBounds);
        setBounds(bounds);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setResizable(true);

        // Window listeners
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                log("✓ Editor opened - AUTO-SAVE DISABLED (Editor Mode)");
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Save settings before closing
                saveSettings();
                game.switchToGamingMode();
                log("✓ Editor closed - Switched to GAMING MODE");
            }
        });

        initUI();
        ensureDirectoriesExist();

        // Setup F3 key binding for refresh
        setupKeyBindings();

        // Don't center if we loaded saved position
        if (!settings.has("editor.main.x")) {
            setLocationRelativeTo(null);
        }

        setVisible(true);

        log("EditorMainSimple initialized");
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // === MAIN SPLIT PANE: Left (Editors) vs Right (Output/Rendering) ===
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6);
        mainSplitPane.setDividerSize(8);

        // Apply saved divider location
        int mainDivider = settings.getSplitPaneDivider("main", 720);
        mainSplitPane.setDividerLocation(mainDivider);

        // Listen for divider changes
        mainSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            settings.setSplitPaneDivider("main", mainSplitPane.getDividerLocation());
        });

        // === LEFT SIDE: Editors Panel ===
        JPanel editorsPanel = new JPanel();
        editorsPanel.setLayout(new BoxLayout(editorsPanel, BoxLayout.Y_AXIS));
        editorsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("🎮 Scene & Item Editor");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        editorsPanel.add(titleLabel);
        editorsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // === SCENES SECTION ===
        scenesPanel = createScenesSection();
        editorsPanel.add(scenesPanel);
        editorsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // === ITEMS SECTION ===
        itemsPanel = createItemsSection();
        editorsPanel.add(itemsPanel);
        editorsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JScrollPane editorsScrollPane = new JScrollPane(editorsPanel);
        editorsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // === LEFT SIDE CONTAINER: Scrollable content + Sticky bottom buttons ===
        JPanel leftSideContainer = new JPanel(new BorderLayout());
        leftSideContainer.add(editorsScrollPane, BorderLayout.CENTER);

        // === STICKY BOTTOM: Global Buttons (always visible, doesn't scroll) ===
        JPanel globalButtonsPanel = createGlobalButtonsPanel();
        globalButtonsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        leftSideContainer.add(globalButtonsPanel, BorderLayout.SOUTH);

        mainSplitPane.setLeftComponent(leftSideContainer);

        // === RIGHT SIDE: Output Area with Clear Button ===
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Log"));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        // Clear button for log
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.addActionListener(e -> {
            outputArea.setText("");
            log("Log cleared");
        });
        logButtonPanel.add(clearLogBtn);
        outputPanel.add(logButtonPanel, BorderLayout.SOUTH);

        mainSplitPane.setRightComponent(outputPanel);

        add(mainSplitPane, BorderLayout.CENTER);
    }

    /**
     * Creates the Scenes Section with JTree (Scene / SubScene with thumbnails)
     * According to "Schema Editor Item Simple Scenes Segment.txt"
     */
    private JPanel createScenesSection() {
        CollapsablePanel panel = new CollapsablePanel("📋 Scenes", false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // === CONTENT: JTree + Buttons ===
        scenesContentPanel = new JPanel(new BorderLayout());

        // Create tree with root node
        scenesRootNode = new DefaultMutableTreeNode("Scenes");
        scenesTreeModel = new DefaultTreeModel(scenesRootNode);
        scenesTree = new JTree(scenesTreeModel);
        scenesTree.setRootVisible(false); // Hide root node
        scenesTree.setShowsRootHandles(true);
        scenesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scenesTree.setCellRenderer(new SceneTreeCellRenderer());
        scenesTree.setRowHeight(50); // Fixed height for thumbnails

        // Mouse listener for clicks on tree
        scenesTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                javax.swing.tree.TreePath path = scenesTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node == null || !(node.getUserObject() instanceof SceneListItem)) {
                    return;
                }

                SceneListItem item = (SceneListItem) node.getUserObject();

                // Double-click behavior
                if (e.getClickCount() == 2) {
                    if (item.isSubScene()) {
                        // SubScene: Load the scene
                        // Check for unsaved changes
                        if (hasUnsavedChanges()) {
                            int result = JOptionPane.showConfirmDialog(
                                EditorMainSimple.this,
                                "You have unsaved changes. Load scene anyway?",
                                "Unsaved Changes",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );
                            if (result != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        // Load the scene
                        openSelectedSceneByNode(node);
                    } else if (item.isScene() && !node.isLeaf()) {
                        // Scene (parent node): Toggle expand/collapse on double-click
                        if (scenesTree.isExpanded(path)) {
                            scenesTree.collapsePath(path);
                        } else {
                            scenesTree.expandPath(path);
                        }
                    }
                }
                // Single click on Scene node (not SubScene) - toggle expand/collapse on thumbnail area
                else if (e.getClickCount() == 1 && item.isScene() && !node.isLeaf()) {
                    // Get the tree cell bounds
                    java.awt.Rectangle bounds = scenesTree.getPathBounds(path);
                    if (bounds != null) {
                        // Check if click is on the icon/thumbnail area (first ~50 pixels)
                        int clickX = e.getX() - bounds.x;
                        if (clickX <= 50) { // Icon is 40px + some padding
                            // Toggle expand/collapse
                            if (scenesTree.isExpanded(path)) {
                                scenesTree.collapsePath(path);
                            } else {
                                scenesTree.expandPath(path);
                            }
                        }
                    }
                }
            }
        });

        // Key listener for F2 to rename
        scenesTree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof SceneListItem) {
                        SceneListItem item = (SceneListItem) node.getUserObject();
                        renameSceneItem(item);
                    }
                }
            }
        });

        JScrollPane treeScrollPane = new JScrollPane(scenesTree);
        treeScrollPane.setPreferredSize(new Dimension(0, 300));

        // Buttons: [New] [Add] [↑] [↓] [Open] [Delete]
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        JButton newBtn = new JButton("New");
        newBtn.setToolTipText("Create new Scene");
        newBtn.addActionListener(e -> createNewSceneSimple());
        buttonsPanel.add(newBtn);

        JButton addBtn = new JButton("Add");
        addBtn.setToolTipText("Add new SubScene to selected Scene");
        addBtn.addActionListener(e -> addNewSubScene());
        buttonsPanel.add(addBtn);

        JButton upBtn = new JButton("↑");
        upBtn.setToolTipText("Move selected scene/subscene up");
        upBtn.addActionListener(e -> moveSceneUp());
        buttonsPanel.add(upBtn);

        JButton downBtn = new JButton("↓");
        downBtn.setToolTipText("Move selected scene/subscene down");
        downBtn.addActionListener(e -> moveSceneDown());
        buttonsPanel.add(downBtn);

        JButton openBtn = new JButton("Open");
        openBtn.setToolTipText("Load selected SubScene into editor");
        openBtn.addActionListener(e -> openSelectedScene());
        buttonsPanel.add(openBtn);

        JButton renameBtn = new JButton("Rename");
        renameBtn.setToolTipText("Rename selected scene/subscene (also F2 or double-click)");
        renameBtn.addActionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof SceneListItem) {
                SceneListItem selected = (SceneListItem) node.getUserObject();
                renameSceneItem(selected);
            } else {
                log("No scene/subscene selected");
            }
        });
        buttonsPanel.add(renameBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setToolTipText("Delete selected scene/subscene");
        deleteBtn.addActionListener(e -> deleteSelectedScene());
        buttonsPanel.add(deleteBtn);

        // Create JSplitPane for resizable tree view
        JSplitPane scenesSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, buttonsPanel);
        scenesSplitPane.setResizeWeight(1.0); // Give all extra space to tree
        scenesSplitPane.setDividerSize(5);
        scenesSplitPane.setDividerLocation(250);
        scenesContentPanel.add(scenesSplitPane, BorderLayout.CENTER);

        // Add header buttons for Expand All / Collapse All
        JButton expandAllBtn = new JButton("⬍");
        expandAllBtn.setToolTipText("Expand all scenes in tree");
        expandAllBtn.addActionListener(e -> expandAllScenes());
        expandAllBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
        panel.addHeaderButton(expandAllBtn);

        JButton collapseAllBtn = new JButton("⬌");
        collapseAllBtn.setToolTipText("Collapse all scenes in tree");
        collapseAllBtn.addActionListener(e -> collapseAllScenes());
        collapseAllBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
        panel.addHeaderButton(collapseAllBtn);

        // Set content
        panel.setContent(scenesContentPanel);

        // Load scenes
        loadScenesSimple();

        return panel;
    }

    /**
     * Creates the Items Section with ListView + Inline Properties Panel
     */
    private JPanel createItemsSection() {
        CollapsablePanel panel = new CollapsablePanel("🎨 Items", false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // === CONTENT: SplitPane (List + Properties) + Buttons ===
        itemsContentPanel = new JPanel(new BorderLayout());

        // Split: Items List (left) vs Properties Panel (right)
        itemsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        itemsSplitPane.setResizeWeight(0.4);
        itemsSplitPane.setDividerSize(6);

        // Apply saved divider location
        int itemsDivider = settings.getSplitPaneDivider("items", 200);
        itemsSplitPane.setDividerLocation(itemsDivider);

        // Listen for divider changes
        itemsSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            settings.setSplitPaneDivider("items", itemsSplitPane.getDividerLocation());
        });

        // === LEFT: Items List Panel with CheckBoxes ===
        itemsListPanel = new JPanel();
        itemsListPanel.setLayout(new BoxLayout(itemsListPanel, BoxLayout.Y_AXIS));
        itemsListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane itemsListScrollPane = new JScrollPane(itemsListPanel);
        itemsListScrollPane.setBorder(BorderFactory.createTitledBorder("Items in SubScene | Show: [x] = Hide in Editor"));
        itemsSplitPane.setLeftComponent(itemsListScrollPane);

        // === RIGHT: Properties Panel ===
        itemPropertiesPanel = createItemPropertiesPanel();
        itemsSplitPane.setRightComponent(itemPropertiesPanel);

        itemsContentPanel.add(itemsSplitPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton newBtn = new JButton("New");
        newBtn.setToolTipText("Create new Item");
        newBtn.addActionListener(e -> createNewItem());
        buttonsPanel.add(newBtn);

        JButton copyBtn = new JButton("Copy");
        copyBtn.setToolTipText("Copy selected Item");
        copyBtn.addActionListener(e -> copyItem());
        buttonsPanel.add(copyBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save selected Item");
        saveBtn.addActionListener(e -> saveItem());
        buttonsPanel.add(saveBtn);

        JButton removeBtn = new JButton("Remove");
        removeBtn.setToolTipText("Remove Item from current scene");
        removeBtn.addActionListener(e -> removeItemFromScene());
        buttonsPanel.add(removeBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setToolTipText("Delete Item from resources");
        deleteBtn.addActionListener(e -> deleteItem());
        buttonsPanel.add(deleteBtn);

        itemsContentPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Set content
        panel.setContent(itemsContentPanel);

        // Load items
        loadItems();

        return panel;
    }

    /**
     * Creates the inline Properties Panel for selected Item
     */
    private JPanel createItemPropertiesPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createTitledBorder("Item Properties"));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JScrollPane propertiesScrollPane = new JScrollPane(panel);
        propertiesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        propertiesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        outerPanel.add(propertiesScrollPane, BorderLayout.CENTER);

        // === NAME ===
        itemNameLabel = new JLabel("No item selected");
        itemNameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        itemNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(itemNameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // === IMAGE ===
        CollapsablePanel imageCollapsablePanel = new CollapsablePanel("🖼️ Image", false);
        imageCollapsablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        imageCollapsablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JPanel imageContent = new JPanel(new BorderLayout());

        // Image Preview
        itemImagePreview = new JLabel("<html><center>Drag & Drop<br>Image Here</center></html>");
        itemImagePreview.setPreferredSize(new Dimension(200, 100));
        itemImagePreview.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        itemImagePreview.setHorizontalAlignment(JLabel.CENTER);
        setupImageDropTarget(itemImagePreview);
        imageContent.add(itemImagePreview, BorderLayout.CENTER);

        // Bottom panel with Path and Show checkbox
        JPanel imageBottomPanel = new JPanel(new BorderLayout());

        // Image Path Field
        JPanel imagePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imagePathPanel.add(new JLabel("Path:"));
        itemImagePathField = new JTextField(20);
        itemImagePathField.setEditable(false);
        imagePathPanel.add(itemImagePathField);
        imageBottomPanel.add(imagePathPanel, BorderLayout.CENTER);

        // Show checkbox (right aligned)
        JPanel imageShowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        itemImageShowCheckBox = new JCheckBox("Show", true);
        itemImageShowCheckBox.setToolTipText("Hide/show item image in edit mode");
        itemImageShowCheckBox.addActionListener(e -> updateItemImageVisibility());
        imageShowPanel.add(itemImageShowCheckBox);
        imageBottomPanel.add(imageShowPanel, BorderLayout.EAST);

        imageContent.add(imageBottomPanel, BorderLayout.SOUTH);

        imageCollapsablePanel.setContent(imageContent);
        panel.add(imageCollapsablePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === HOVER TEXT ===
        CollapsablePanel hoverCollapsablePanel = new CollapsablePanel("💬 Hover Text", false);
        hoverCollapsablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hoverCollapsablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel hoverContent = new JPanel(new BorderLayout());
        hoverContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        itemHoverTextField = new JTextField(20);
        itemHoverTextField.addActionListener(e -> updateItemHoverText());
        hoverContent.add(itemHoverTextField, BorderLayout.CENTER);

        hoverCollapsablePanel.setContent(hoverContent);
        panel.add(hoverCollapsablePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === CONDITIONS TABLE (NEW - Selectable) ===
        CollapsablePanel conditionsCollapsablePanel = new CollapsablePanel("⚙️ Conditions", false);
        conditionsCollapsablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        conditionsCollapsablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JPanel conditionsContent = new JPanel(new BorderLayout());

        // Create table model and table
        conditionsTableModel = new ConditionsTableModel();
        itemConditionsTable = new javax.swing.JTable(conditionsTableModel);
        itemConditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemConditionsTable.setRowHeight(25);

        // Set column widths
        itemConditionsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Condition
        itemConditionsTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // true

        // Center the checkbox
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        itemConditionsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        JScrollPane conditionsScrollPane = new JScrollPane(itemConditionsTable);
        conditionsScrollPane.setPreferredSize(new Dimension(0, 120));

        // Buttons (according to schema: [Add] [Rename] [Delete] [Save to Progress] [Save as Default] [Close])
        JPanel conditionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton addConditionBtn = new JButton("Add");
        addConditionBtn.addActionListener(e -> addConditionToItem());
        conditionsButtonsPanel.add(addConditionBtn);

        JButton renameConditionBtn = new JButton("Rename");
        renameConditionBtn.addActionListener(e -> renameConditionInItem());
        conditionsButtonsPanel.add(renameConditionBtn);

        JButton deleteConditionBtn = new JButton("Delete");
        deleteConditionBtn.addActionListener(e -> removeConditionFromItem());
        conditionsButtonsPanel.add(deleteConditionBtn);

        JButton saveToProgressBtn = new JButton("Save to Progress");
        saveToProgressBtn.setToolTipText("Save changes to progress file only");
        saveToProgressBtn.addActionListener(e -> saveItemToProgress());
        conditionsButtonsPanel.add(saveToProgressBtn);

        JButton saveAsDefaultBtn = new JButton("Save as Default");
        saveAsDefaultBtn.setToolTipText("Save changes to default file only");
        saveAsDefaultBtn.addActionListener(e -> saveItemAsDefault());
        conditionsButtonsPanel.add(saveAsDefaultBtn);

        // Create JSplitPane for resizable table view
        JSplitPane conditionsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, conditionsScrollPane, conditionsButtonsPanel);
        conditionsSplitPane.setResizeWeight(1.0);
        conditionsSplitPane.setDividerSize(3);
        conditionsSplitPane.setDividerLocation(120);
        conditionsContent.add(conditionsSplitPane, BorderLayout.CENTER);

        conditionsCollapsablePanel.setContent(conditionsContent);
        panel.add(conditionsCollapsablePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === CUSTOM CLICK AREA POINTS ===
        CollapsablePanel customClickPanel = new CollapsablePanel("🖱️ CustomClickArea Points", true);
        customClickPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add Show checkbox to header
        customClickAreaShowCheckBox = new JCheckBox("Show", true);
        customClickAreaShowCheckBox.setToolTipText("Hide/show custom click area in edit mode");
        customClickAreaShowCheckBox.addActionListener(e -> updateCustomClickAreaVisibility());
        customClickPanel.addHeaderComponent(customClickAreaShowCheckBox);

        JPanel customClickContent = new JPanel(new BorderLayout());
        customClickAreaPointsModel = new DefaultListModel<>();
        customClickAreaPointsList = new JList<>(customClickAreaPointsModel);
        customClickAreaPointsList.setVisibleRowCount(4);
        customClickAreaPointsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectedItem != null) {
                int selectedIndex = customClickAreaPointsList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    game.setHighlightedPoint(selectedItem, "CustomClickArea", selectedIndex);
                } else {
                    game.clearHighlightedPoint();
                }
            }
        });
        JScrollPane customClickScroll = new JScrollPane(customClickAreaPointsList);

        // No edit buttons - use "Open Scene Point Editor" button instead
        customClickContent.add(customClickScroll, BorderLayout.CENTER);

        customClickPanel.setContent(customClickContent);
        panel.add(customClickPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === MOVING RANGE POINTS ===
        CollapsablePanel movingRangePanel = new CollapsablePanel("📏 MovingRange Points", true);
        movingRangePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add Show checkbox to header
        movingRangeShowCheckBox = new JCheckBox("Show", true);
        movingRangeShowCheckBox.setToolTipText("Hide/show moving range in edit mode");
        movingRangeShowCheckBox.addActionListener(e -> updateMovingRangeVisibility());
        movingRangePanel.addHeaderComponent(movingRangeShowCheckBox);

        JPanel movingRangeContent = new JPanel(new BorderLayout());
        movingRangePointsModel = new DefaultListModel<>();
        movingRangePointsList = new JList<>(movingRangePointsModel);
        movingRangePointsList.setVisibleRowCount(4);
        JScrollPane movingRangeScroll = new JScrollPane(movingRangePointsList);

        // No edit buttons - use "Open Scene Point Editor" button instead
        movingRangeContent.add(movingRangeScroll, BorderLayout.CENTER);

        movingRangePanel.setContent(movingRangeContent);
        panel.add(movingRangePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === PATH POINTS ===
        CollapsablePanel pathPanel = new CollapsablePanel("🛤️ Path Points", true);
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add Show checkbox to header
        pathShowCheckBox = new JCheckBox("Show", true);
        pathShowCheckBox.setToolTipText("Hide/show path in edit mode");
        pathShowCheckBox.addActionListener(e -> updatePathVisibility());
        pathPanel.addHeaderComponent(pathShowCheckBox);

        JPanel pathContent = new JPanel(new BorderLayout());
        pathPointsModel = new DefaultListModel<>();
        pathPointsList = new JList<>(pathPointsModel);
        pathPointsList.setVisibleRowCount(4);
        JScrollPane pathScroll = new JScrollPane(pathPointsList);

        // No edit buttons - use "Open Scene Point Editor" button instead
        pathContent.add(pathScroll, BorderLayout.CENTER);

        pathPanel.setContent(pathContent);
        panel.add(pathPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === ACTIONS BUTTON ===
        JButton actionsBtn = new JButton("Edit Actions");
        actionsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsBtn.addActionListener(e -> editItemActions());
        panel.add(actionsBtn);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === OPEN SCENE POINT EDITOR ===
        JButton openPointEditorBtn = new JButton("Open Scene Point Editor");
        openPointEditorBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        openPointEditorBtn.setToolTipText("Open the universal point editor for all items");
        openPointEditorBtn.addActionListener(e -> openScenePointEditor());
        panel.add(openPointEditorBtn);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        // === OPEN ORIENTATION EDITOR ===
        JButton openOrientationEditorBtn = new JButton("Open Orientation Editor");
        openOrientationEditorBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        openOrientationEditorBtn.setToolTipText("Edit orientation-based images for items");
        openOrientationEditorBtn.addActionListener(e -> openOrientationEditor());
        panel.add(openOrientationEditorBtn);

        return outerPanel;
    }

    /**
     * Creates the Global Buttons Panel (Sticky at bottom)
     */
    private JPanel createGlobalButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(new Color(240, 240, 240)); // Light gray background

        JLabel globalLabel = new JLabel("Global Actions:");
        globalLabel.setFont(new Font("Arial", Font.BOLD, 10));
        panel.add(globalLabel);

        JButton openResourceBtn = new JButton("Open Resource");
        openResourceBtn.setToolTipText("Open selected resource .txt file");
        openResourceBtn.addActionListener(e -> openSelectedResource());
        panel.add(openResourceBtn);

        JButton openResourcesBtn = new JButton("Open Resources");
        openResourcesBtn.setToolTipText("Open resources directory");
        openResourcesBtn.addActionListener(e -> openResourcesDirectory());
        panel.add(openResourcesBtn);

        JButton manageConditionsBtn = new JButton("Manage Conditions");
        manageConditionsBtn.setToolTipText("Open Condition Manager");
        manageConditionsBtn.addActionListener(e -> openConditionManager());
        panel.add(manageConditionsBtn);

        JButton manageDialogsBtn = new JButton("Manage Dialogs");
        manageDialogsBtn.setToolTipText("Open Dialogs Manager");
        manageDialogsBtn.addActionListener(e -> openDialogManager());
        panel.add(manageDialogsBtn);

        JButton manageButtonsBtn = new JButton("Manage Buttons");
        manageButtonsBtn.setToolTipText("Open Button Manager");
        manageButtonsBtn.addActionListener(e -> openButtonManager());
        panel.add(manageButtonsBtn);

        JButton manageItemsBtn = new JButton("Manage Items");
        manageItemsBtn.setToolTipText("Open Item Manager - Global CRUD for all items");
        manageItemsBtn.addActionListener(e -> openItemManager());
        panel.add(manageItemsBtn);

        // Add separator
        panel.add(new JLabel("  |  "));

        // Always on Top checkbox
        JCheckBox alwaysOnTopCheckBox = new JCheckBox("Always on Top", isAlwaysOnTop());
        alwaysOnTopCheckBox.setToolTipText("Keep editor window always on top of other windows");
        alwaysOnTopCheckBox.setBackground(panel.getBackground());
        alwaysOnTopCheckBox.addActionListener(e -> {
            boolean selected = alwaysOnTopCheckBox.isSelected();
            setAlwaysOnTop(selected);
            log(selected ? "✓ Always on Top enabled" : "✓ Always on Top disabled");
        });
        panel.add(alwaysOnTopCheckBox);

        return panel;
    }

    // ==================== Event Handlers ====================

    // OLD METHOD - Removed (JTree replaced by JList)

    private void onItemSelected() {
        // Clear any point highlighting when switching items
        game.clearHighlightedPoint();

        if (selectedItemName == null) {
            selectedItem = null;
            clearItemPropertiesPanel();
            return;
        }

        // Load item
        try {
            selectedItem = ItemLoader.loadItemByName(selectedItemName);
            log("Selected item: " + selectedItemName);
            updateItemPropertiesPanel();
        } catch (Exception ex) {
            log("ERROR loading item: " + ex.getMessage());
            clearItemPropertiesPanel();
        }
    }

    // ==================== Scene Actions ====================

    private void createNewScene() {
        log("TODO: Create new Scene");
    }

    private void copyScene() {
        log("TODO: Copy Scene");
    }

    private void editScene() {
        log("TODO: Edit Scene");
    }

    private void deleteScene() {
        log("TODO: Delete Scene");
    }

    // OLD METHOD - Removed (replaced by loadScenesSimple())

    /**
     * Load scenes into JTree using FileHandlingSimple (Scene/SubScene hierarchy)
     */
    private void loadScenesSimple() {
        scenesRootNode.removeAllChildren();

        // Load saved scene order
        Map<String, List<String>> savedOrder = loadSceneOrder();

        // Get all Scene directories
        List<String> sceneNames = FileHandlingSimple.getAllScenes();

        if (sceneNames.isEmpty()) {
            log("No scenes found (no scene directories in resources/scenes/)");
            scenesTreeModel.reload();
            return;
        }

        // Apply saved scene order to top-level scenes
        List<String> sceneOrder = new ArrayList<>(savedOrder.keySet());
        sceneNames = SceneOrderManager.applySavedOrder(sceneNames, sceneOrder);

        for (String sceneName : sceneNames) {
            try {
                // Get SubScenes first
                List<String> subSceneNames = FileHandlingSimple.getSubScenes(sceneName);

                // Apply saved order to subscenes
                if (savedOrder.containsKey(sceneName)) {
                    subSceneNames = SceneOrderManager.applySavedOrder(subSceneNames, savedOrder.get(sceneName));
                }

                // Load first SubScene to get its Scene object for thumbnail
                Scene firstSubScene = null;
                if (!subSceneNames.isEmpty()) {
                    try {
                        firstSubScene = FileHandlingSimple.loadSubScene(sceneName, subSceneNames.get(0), null);
                    } catch (Exception e) {
                        log("Warning: Could not load first subscene for thumbnail: " + e.getMessage());
                    }
                }

                // Create Scene node with first SubScene for thumbnail
                SceneListItem sceneItem = new SceneListItem(sceneName, firstSubScene);
                DefaultMutableTreeNode sceneNode = new DefaultMutableTreeNode(sceneItem);

                // Add all SubScenes
                for (String subSceneName : subSceneNames) {
                    try {
                        // Load SubScene (reload first one if needed)
                        Scene subScene = FileHandlingSimple.loadSubScene(sceneName, subSceneName, null);

                        // Create SubScene node
                        SceneListItem subSceneItem = new SceneListItem(subSceneName, subScene, sceneName);
                        DefaultMutableTreeNode subSceneNode = new DefaultMutableTreeNode(subSceneItem);

                        // Add SubScene to Scene
                        sceneNode.add(subSceneNode);
                    } catch (Exception subEx) {
                        log("Warning: Could not load subscene " + subSceneName + ": " + subEx.getMessage());
                    }
                }

                // Add Scene to root
                scenesRootNode.add(sceneNode);
            } catch (Exception ex) {
                log("Warning: Could not process scene " + sceneName + ": " + ex.getMessage());
            }
        }

        scenesTreeModel.reload();

        // Expand all scenes by default
        for (int i = 0; i < scenesTree.getRowCount(); i++) {
            scenesTree.expandRow(i);
        }

        log("✓ Loaded " + sceneNames.size() + " scenes into tree");
    }

    /**
     * Refresh the scenes tree (after modifications)
     */
    private void refreshScenesTree() {
        // Save current selection
        javax.swing.tree.TreePath selectedPath = scenesTree.getSelectionPath();
        String selectedName = null;
        if (selectedPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (node.getUserObject() instanceof SceneListItem) {
                selectedName = ((SceneListItem) node.getUserObject()).getName();
            }
        }

        // Reload tree
        loadScenesSimple();

        // Restore selection
        if (selectedName != null) {
            selectNodeByName(selectedName);
        }
    }

    /**
     * Helper to select a tree node by scene/subscene name
     */
    private void selectNodeByName(String name) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) scenesTreeModel.getRoot();
        java.util.Enumeration<?> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof SceneListItem) {
                SceneListItem item = (SceneListItem) node.getUserObject();
                if (item.getName().equals(name)) {
                    javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
                    scenesTree.setSelectionPath(path);
                    scenesTree.scrollPathToVisible(path);
                    break;
                }
            }
        }
    }

    private void expandAllScenes() {
        for (int i = 0; i < scenesTree.getRowCount(); i++) {
            scenesTree.expandRow(i);
        }
        log("✓ Expanded all scenes");
    }

    private void collapseAllScenes() {
        for (int i = 0; i < scenesTree.getRowCount(); i++) {
            scenesTree.collapseRow(i);
        }
        log("✓ Collapsed all scenes");
    }

    private void createNewSceneSimple() {
        String sceneName = JOptionPane.showInputDialog(this, "Enter Scene name:", "New Scene", JOptionPane.PLAIN_MESSAGE);

        if (sceneName == null || sceneName.trim().isEmpty()) {
            log("Scene creation cancelled");
            return;
        }

        sceneName = sceneName.trim();

        if (FileHandlingSimple.sceneExists(sceneName)) {
            JOptionPane.showMessageDialog(this, "Scene already exists: " + sceneName, "Error", JOptionPane.ERROR_MESSAGE);
            log("✗ Scene already exists: " + sceneName);
            return;
        }

        boolean created = FileHandlingSimple.createScene(sceneName);
        if (created) {
            log("✓ Created Scene: " + sceneName);
            refreshScenesTree();
        } else {
            log("✗ Failed to create Scene: " + sceneName);
            JOptionPane.showMessageDialog(this, "Failed to create Scene", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addNewSubScene() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
        if (node == null) {
            log("No scene selected. Please select a Scene first.");
            JOptionPane.showMessageDialog(this, "Please select a Scene first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        SceneListItem selected = (SceneListItem) node.getUserObject();

        // Determine parent scene
        String parentSceneName = null;
        if (selected == null) {
            log("No scene selected. Please select a Scene first.");
            JOptionPane.showMessageDialog(this, "Please select a Scene first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selected.isScene()) {
            parentSceneName = selected.getName();
        } else {
            // SubScene selected - get parent
            parentSceneName = selected.getParentSceneName();
        }

        String subSceneName = JOptionPane.showInputDialog(this, "Enter SubScene name:", "New SubScene", JOptionPane.PLAIN_MESSAGE);

        if (subSceneName == null || subSceneName.trim().isEmpty()) {
            log("SubScene creation cancelled");
            return;
        }

        subSceneName = subSceneName.trim();

        if (FileHandlingSimple.subSceneExists(parentSceneName, subSceneName)) {
            JOptionPane.showMessageDialog(this, "SubScene already exists: " + subSceneName, "Error", JOptionPane.ERROR_MESSAGE);
            log("✗ SubScene already exists: " + subSceneName);
            return;
        }

        boolean created = FileHandlingSimple.createSubScene(parentSceneName, subSceneName);
        if (created) {
            log("✓ Created SubScene: " + subSceneName + " in " + parentSceneName);
            refreshScenesTree();
        } else {
            log("✗ Failed to create SubScene: " + subSceneName);
            JOptionPane.showMessageDialog(this, "Failed to create SubScene", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void moveSceneUp() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
        if (node == null) {
            log("No scene/subscene selected");
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            log("Cannot move root node");
            return;
        }

        int index = parent.getIndex(node);
        if (index <= 0) {
            log("Already at the top");
            return;
        }

        // Remove from parent
        scenesTreeModel.removeNodeFromParent(node);

        // Insert at new position
        scenesTreeModel.insertNodeInto(node, parent, index - 1);

        // Reselect the node
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        scenesTree.setSelectionPath(path);
        scenesTree.scrollPathToVisible(path);

        log("✓ Moved up: " + ((SceneListItem) node.getUserObject()).getName());

        // Save the new order to file
        saveSceneOrder();
        markAsChanged();
    }

    private void moveSceneDown() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
        if (node == null) {
            log("No scene/subscene selected");
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent == null) {
            log("Cannot move root node");
            return;
        }

        int index = parent.getIndex(node);
        if (index >= parent.getChildCount() - 1) {
            log("Already at the bottom");
            return;
        }

        // Remove from parent
        scenesTreeModel.removeNodeFromParent(node);

        // Insert at new position
        scenesTreeModel.insertNodeInto(node, parent, index + 1);

        // Reselect the node
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        scenesTree.setSelectionPath(path);
        scenesTree.scrollPathToVisible(path);

        log("✓ Moved down: " + ((SceneListItem) node.getUserObject()).getName());

        // Save the new order to file
        saveSceneOrder();
        markAsChanged();
    }

    private void openSelectedScene() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
        if (node == null) {
            log("No scene selected");
            return;
        }

        if (!(node.getUserObject() instanceof SceneListItem)) {
            log("Invalid selection");
            return;
        }

        SceneListItem selected = (SceneListItem) node.getUserObject();

        if (selected.isSubScene()) {
            try {
                // Load the SubScene
                Scene subScene = FileHandlingSimple.loadSubScene(
                    selected.getParentSceneName(),
                    selected.getName(),
                    game.getGameProgress()
                );

                // Store as currently selected SubScene
                selectedSubScene = subScene;
                selectedScene = null;
                currentSceneName = selected.getParentSceneName();

                log("✓ Opened SubScene: " + selected.getName() + " from Scene: " + selected.getParentSceneName());
                log("  Background: " + subScene.getBackgroundImagePath());
                log("  Items: " + subScene.getItems().size());

                // Load the scene into the game for preview
                String fullSceneName = selected.getParentSceneName() + "/" + selected.getName();
                game.loadSceneFromDefault(fullSceneName);

                // Reload items list to show items from this scene
                loadItems();

                // Update UI
                revalidate();
                repaint();

                log("✓ Scene loaded into editor and game preview");

            } catch (Exception e) {
                log("✗ Failed to open SubScene: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to open SubScene: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            log("Please select a SubScene to open (Scenes cannot be opened directly)");
        }
    }

    private void deleteSelectedScene() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scenesTree.getLastSelectedPathComponent();
        if (node == null) {
            log("No scene/subscene selected");
            return;
        }

        if (!(node.getUserObject() instanceof SceneListItem)) {
            log("Invalid selection");
            return;
        }

        SceneListItem selected = (SceneListItem) node.getUserObject();

        String message;
        if (selected.isScene()) {
            message = "Delete Scene '" + selected.getName() + "' and all its SubScenes?\nThis cannot be undone!";
        } else {
            message = "Delete SubScene '" + selected.getName() + "'?\nThis cannot be undone!";
        }

        int confirm = JOptionPane.showConfirmDialog(this, message, "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            log("Deletion cancelled");
            return;
        }

        boolean deleted = false;
        if (selected.isScene()) {
            deleted = FileHandlingSimple.deleteScene(selected.getName());
            if (deleted) {
                log("✓ Deleted Scene: " + selected.getName());
            } else {
                log("✗ Failed to delete Scene: " + selected.getName());
            }
        } else {
            deleted = FileHandlingSimple.deleteSubScene(selected.getParentSceneName(), selected.getName());
            if (deleted) {
                log("✓ Deleted SubScene: " + selected.getName());
            } else {
                log("✗ Failed to delete SubScene: " + selected.getName());
            }
        }

        if (deleted) {
            refreshScenesTree();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Renames a Scene or SubScene (F2 or double-click)
     */
    private void renameSceneItem(SceneListItem item) {
        if (item == null) return;

        String oldName = item.getName();
        String dialogTitle = item.isScene() ? "Rename Scene" : "Rename SubScene";
        String prompt = item.isScene() ? "Enter new Scene name:" : "Enter new SubScene name:";

        Object result = JOptionPane.showInputDialog(this, prompt, dialogTitle, JOptionPane.PLAIN_MESSAGE, null, null, oldName);

        if (result == null) {
            log("Rename cancelled");
            return;
        }

        String newName = result.toString().trim();

        if (newName.isEmpty()) {
            log("Rename cancelled - empty name");
            return;
        }

        if (newName.equals(oldName)) {
            log("Name unchanged");
            return;
        }

        boolean renamed = false;
        if (item.isScene()) {
            if (FileHandlingSimple.sceneExists(newName)) {
                JOptionPane.showMessageDialog(this, "Scene already exists: " + newName, "Error", JOptionPane.ERROR_MESSAGE);
                log("✗ Scene already exists: " + newName);
                return;
            }
            renamed = FileHandlingSimple.renameScene(oldName, newName);
            if (renamed) {
                log("✓ Renamed Scene: " + oldName + " → " + newName);
            } else {
                log("✗ Failed to rename Scene");
            }
        } else {
            String parentScene = item.getParentSceneName();
            if (FileHandlingSimple.subSceneExists(parentScene, newName)) {
                JOptionPane.showMessageDialog(this, "SubScene already exists: " + newName, "Error", JOptionPane.ERROR_MESSAGE);
                log("✗ SubScene already exists: " + newName);
                return;
            }
            renamed = FileHandlingSimple.renameSubScene(parentScene, oldName, newName);
            if (renamed) {
                log("✓ Renamed SubScene: " + oldName + " → " + newName);
            } else {
                log("✗ Failed to rename SubScene");
            }
        }

        if (renamed) {
            refreshScenesTree();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to rename", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== Item Actions ====================

    private void loadItems() {
        itemsListPanel.removeAll();
        itemCheckBoxes.clear();
        itemFollowingMouseCheckBoxes.clear();
        itemFollowingOnClickCheckBoxes.clear();
        itemLabels.clear();

        // Only load items from the currently selected SubScene
        if (selectedSubScene == null) {
            log("No SubScene selected - no items to load");
            return;
        }

        List<Item> itemsInSubScene = selectedSubScene.getItems();
        if (itemsInSubScene == null || itemsInSubScene.isEmpty()) {
            log("✓ No items in current SubScene");
            return;
        }

        // Add items from the current SubScene with checkboxes
        int itemCount = 0;
        for (Item item : itemsInSubScene) {
            if (item != null && item.getName() != null) {
                String itemName = item.getName();

                // Create row panel with vertical layout as per schema
                JPanel rowPanel = new JPanel();
                rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
                rowPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
                rowPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

                // Left side: Thumbnail and name stacked vertically
                JPanel leftPanel = new JPanel();
                leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
                leftPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);

                // Thumbnail - Load item image and scale to small icon
                JLabel thumbnailLabel = new JLabel();
                thumbnailLabel.setPreferredSize(new Dimension(50, 50));
                thumbnailLabel.setMinimumSize(new Dimension(50, 50));
                thumbnailLabel.setMaximumSize(new Dimension(50, 50));
                thumbnailLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                thumbnailLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

                try {
                    String imagePath = item.getImageFilePath();
                    if (imagePath != null && !imagePath.isEmpty()) {
                        // Use ResourcePathHelper to find image
                        File imageFile = ResourcePathHelper.findImageFile(imagePath);
                        if (imageFile == null) {
                            // Fallback: try direct path
                            imageFile = new File(imagePath);
                        }

                        if (imageFile != null && imageFile.exists()) {
                            javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imageFile.getAbsolutePath());
                            java.awt.Image img = icon.getImage().getScaledInstance(48, 48, java.awt.Image.SCALE_SMOOTH);
                            thumbnailLabel.setIcon(new javax.swing.ImageIcon(img));
                        } else {
                            // Placeholder if image not found
                            thumbnailLabel.setText("?");
                            thumbnailLabel.setHorizontalAlignment(JLabel.CENTER);
                        }
                    } else {
                        // No image path
                        thumbnailLabel.setText("?");
                        thumbnailLabel.setHorizontalAlignment(JLabel.CENTER);
                    }
                } catch (Exception e) {
                    // Error loading thumbnail
                    thumbnailLabel.setText("!");
                    thumbnailLabel.setHorizontalAlignment(JLabel.CENTER);
                }

                // Label with item name - below thumbnail
                JLabel itemLabel = new JLabel(itemName);
                itemLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
                itemLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
                itemLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectItemByName(itemName);
                    }
                });
                itemLabels.put(itemName, itemLabel);

                leftPanel.add(thumbnailLabel);
                leftPanel.add(Box.createRigidArea(new Dimension(0, 2))); // Small gap
                leftPanel.add(itemLabel);

                // Right side: Checkboxes stacked vertically
                JPanel checkBoxPanel = new JPanel();
                checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
                checkBoxPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);

                // Get the actual Item object to read current values
                Item itemObj = item;

                // CheckBox for show/hide in editor
                JCheckBox showCheckBox = new JCheckBox("Show");
                showCheckBox.setToolTipText("Uncheck to hide this item in Edit Mode");
                showCheckBox.setSelected(itemObj.isVisibleInEditor());
                showCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
                showCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
                showCheckBox.addActionListener(e -> {
                    boolean visible = showCheckBox.isSelected();
                    game.syncVisibleInEditorCheckbox(itemObj, visible);
                    log((visible ? "Showing" : "Hiding") + " item in editor: " + itemName);
                });
                itemCheckBoxes.put(itemName, showCheckBox);

                // CheckBox for Following Mouse
                JCheckBox followingMouseCheckBox = new JCheckBox("Follow");
                followingMouseCheckBox.setToolTipText("Item follows mouse cursor");
                followingMouseCheckBox.setSelected(itemObj.isFollowingMouse());
                followingMouseCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
                followingMouseCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
                followingMouseCheckBox.addActionListener(e -> {
                    boolean newState = followingMouseCheckBox.isSelected();
                    game.syncFollowingMouseCheckbox(itemObj, newState);
                    log((newState ? "Enabled" : "Disabled") + " following mouse for: " + itemName);
                });
                itemFollowingMouseCheckBoxes.put(itemName, followingMouseCheckBox);

                // CheckBox for Following On Mouse Click
                JCheckBox followingOnClickCheckBox = new JCheckBox("Click");
                followingOnClickCheckBox.setToolTipText("Item follows mouse when clicked");
                followingOnClickCheckBox.setSelected(itemObj.isFollowingOnMouseClick());
                followingOnClickCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 10));
                followingOnClickCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
                followingOnClickCheckBox.addActionListener(e -> {
                    boolean newState = followingOnClickCheckBox.isSelected();
                    game.syncFollowingOnClickCheckbox(itemObj, newState);
                    log((newState ? "Enabled" : "Disabled") + " following on click for: " + itemName);
                });
                itemFollowingOnClickCheckBoxes.put(itemName, followingOnClickCheckBox);

                // Add checkboxes with minimal spacing
                checkBoxPanel.add(showCheckBox);
                checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 1)));
                checkBoxPanel.add(followingMouseCheckBox);
                checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 1)));
                checkBoxPanel.add(followingOnClickCheckBox);

                // Assemble row: left panel + small gap + checkbox panel
                rowPanel.add(leftPanel);
                rowPanel.add(Box.createRigidArea(new Dimension(5, 0))); // Small gap between thumbnail and checkboxes
                rowPanel.add(checkBoxPanel);
                rowPanel.add(Box.createHorizontalGlue()); // Push everything to the left

                itemsListPanel.add(rowPanel);

                itemCount++;
            }
        }

        itemsListPanel.revalidate();
        itemsListPanel.repaint();

        log("✓ Loaded " + itemCount + " items from SubScene: " + selectedSubScene.getName());

        // Auto-select first item if any items exist
        if (itemCount > 0 && !itemLabels.isEmpty()) {
            String firstItemName = itemLabels.keySet().iterator().next();
            selectItemByName(firstItemName);
        }
    }

    /**
     * Select an item by name in the items list
     */
    private void selectItemByName(String itemName) {
        // Deselect previous
        if (selectedItemName != null && itemLabels.containsKey(selectedItemName)) {
            itemLabels.get(selectedItemName).setForeground(null); // Reset color
        }

        // Select new
        selectedItemName = itemName;
        if (itemLabels.containsKey(selectedItemName)) {
            itemLabels.get(selectedItemName).setForeground(java.awt.Color.BLUE);
        }

        // Load item properties
        onItemSelected();
    }

    private void createNewItem() {
        String itemName = JOptionPane.showInputDialog(this, "Enter new Item name:");
        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        itemName = itemName.trim();

        // Check if item already exists
        File itemFile = new File("resources/items/" + itemName + ".txt");
        if (itemFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Item '" + itemName + "' already exists!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Create new item
            Item newItem = new Item(itemName);
            newItem.setPosition(100, 100);

            // Initialize empty lists
            newItem.setConditionalImages(new java.util.ArrayList<>());
            newItem.setCustomClickAreas(new java.util.ArrayList<>());
            newItem.setMovingRanges(new java.util.ArrayList<>());

            // Save to file
            ItemSaver.saveItemByName(newItem);

            // Refresh list and select new item
            loadItems();
            selectItemByName(itemName);

            log("✓ Created new item: " + itemName);
        } catch (Exception ex) {
            log("ERROR creating item: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void copyItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        String newName = JOptionPane.showInputDialog(this,
            "Enter name for copied item:",
            selectedItem.getName() + "_copy");

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        newName = newName.trim();

        // Check if item already exists
        File itemFile = new File("resources/items/" + newName + ".txt");
        if (itemFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Item '" + newName + "' already exists!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Load original item fresh from file
            Item original = ItemLoader.loadItemByName(selectedItem.getName());

            // Change name
            original.setName(newName);

            // Save as new item
            ItemSaver.saveItemByName(original);

            // Refresh list and select new item
            loadItems();
            selectItemByName(newName);

            log("✓ Copied item: " + selectedItem.getName() + " → " + newName);
        } catch (Exception ex) {
            log("ERROR copying item: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void saveItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        try {
            ItemSaver.saveItemByName(selectedItem);
            log("✓ Saved item: " + selectedItem.getName());
        } catch (Exception ex) {
            log("ERROR saving item: " + ex.getMessage());
        }
    }

    private void removeItemFromScene() {
        if (selectedItem == null || selectedScene == null) {
            log("No item or scene selected");
            return;
        }

        try {
            // Remove item from current scene's item list
            if (selectedScene.getItems() != null) {
                selectedScene.getItems().removeIf(item -> item.getName().equals(selectedItem.getName()));
                SceneSaver.saveScene(selectedScene);
                log("✓ Removed item '" + selectedItem.getName() + "' from scene '" + selectedScene.getName() + "'");
            }
        } catch (Exception ex) {
            log("ERROR removing item from scene: " + ex.getMessage());
        }
    }

    private void deleteItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete item '" + selectedItem.getName() + "' from resources?\nThis cannot be undone!",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String itemName = selectedItem.getName();

            // Delete file
            File itemFile = new File("resources/items/" + itemName + ".txt");
            if (itemFile.exists()) {
                itemFile.delete();
            }

            // Delete progress file if exists
            File progressFile = new File("resources/items/" + itemName + "_progress.txt");
            if (progressFile.exists()) {
                progressFile.delete();
            }

            // Clear selection
            selectedItem = null;
            clearItemPropertiesPanel();

            // Refresh list
            loadItems();

            log("✓ Deleted item: " + itemName);

            // TODO: Check for references in scene files and warn user
        } catch (Exception ex) {
            log("ERROR deleting item: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void editItemActions() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        try {
            // Open ItemActionsListDialog - shows all actions for the item
            ItemActionsListDialog actionsListDialog = new ItemActionsListDialog(this, selectedItem);
            actionsListDialog.setVisible(true);

            // Reload item after editing (in case it was modified)
            Item updatedItem = actionsListDialog.getItem();
            if (updatedItem != null) {
                selectedItem = updatedItem;
                updateItemPropertiesPanel();
            }

            log("✓ Opened Actions Manager for item: " + selectedItem.getName());
        } catch (Exception ex) {
            log("ERROR opening Actions Manager: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ==================== Global Actions ====================

    private void openSelectedResource() {
        log("TODO: Open selected resource");
    }

    private void openResourcesDirectory() {
        try {
            java.awt.Desktop.getDesktop().open(new File("resources"));
            log("✓ Opened resources directory");
        } catch (Exception ex) {
            log("ERROR opening resources: " + ex.getMessage());
        }
    }

    private void openConditionManager() {
        ConditionsManagerDialog dialog = new ConditionsManagerDialog(this);
        dialog.setVisible(true);
        log("Opened Condition Manager");
    }

    private void openDialogManager() {
        log("TODO: Open Dialog Manager");
    }

    private void openButtonManager() {
        ButtonManagerDialog dialog = new ButtonManagerDialog(this);
        dialog.setVisible(true);
        log("Opened Button Manager");
    }

    private void openItemManager() {
        if (selectedSubScene == null) {
            log("WARNING: No SubScene selected. Please select a SubScene first.");
            JOptionPane.showMessageDialog(this,
                "Please select a SubScene first!",
                "No SubScene Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        ItemManagerDialog dialog = new ItemManagerDialog(this, selectedSubScene);
        dialog.setVisible(true);
        log("Opened Item Manager");
    }

    // ==================== Utilities ====================

    private void ensureDirectoriesExist() {
        String[] dirs = {
            "resources/scenes",
            "resources/items",
            "resources/images/items",
            "resources/keyAreas",
            "resources/dialogs",
            "resources/conditions"
        };

        for (String dir : dirs) {
            File directory = new File(dir);
            if (!directory.exists()) {
                directory.mkdirs();
                log("✓ Created directory: " + dir);
            }
        }
    }

    /**
     * Setup keyboard shortcuts
     */
    private void setupKeyBindings() {
        // Get root pane for key bindings
        javax.swing.JRootPane rootPane = getRootPane();

        // F3 = Refresh everything
        rootPane.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "refresh");
        rootPane.getActionMap().put("refresh", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refreshAll();
            }
        });
    }

    /**
     * Refresh everything: scenes, items, UI, game canvas
     */
    private void refreshAll() {
        log("🔄 Refreshing all...");

        // Refresh scenes tree
        loadScenesSimple();

        // Refresh items list
        loadItems();

        // Refresh item properties if an item is selected
        if (selectedItem != null) {
            try {
                String itemName = selectedItem.getName();
                selectedItem = ItemLoader.loadItemByName(itemName);
                updateItemPropertiesPanel();
            } catch (Exception ex) {
                log("ERROR refreshing selected item: " + ex.getMessage());
            }
        }

        // Reload current scene in game window
        if (selectedSubScene != null) {
            try {
                game.reloadCurrentScene();

                // Re-select item in game window if one was selected
                if (selectedItem != null) {
                    Scene reloadedScene = game.getCurrentScene();
                    if (reloadedScene != null) {
                        for (Item item : reloadedScene.getItems()) {
                            if (item.getName().equals(selectedItem.getName())) {
                                reloadedScene.setSelectedItem(item);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log("ERROR reloading scene: " + ex.getMessage());
            }
        }

        // Repaint game canvas
        game.repaint();

        log("✓ Refresh complete [F3]");
    }

    public void log(String message) {
        System.out.println("[EditorMainSimple] " + message);
        if (outputArea != null) {
            outputArea.append(message + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    }

    // ==================== Unsaved Changes Tracking ====================

    private boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    private void markAsChanged() {
        hasUnsavedChanges = true;
        setTitle("Main Editor (Simple) - ALT+S to toggle [UNSAVED]");
    }

    private void markAsSaved() {
        hasUnsavedChanges = false;
        setTitle("Main Editor (Simple) - ALT+S to toggle");
    }

    private void openSelectedSceneByNode(DefaultMutableTreeNode node) {
        if (!(node.getUserObject() instanceof SceneListItem)) {
            return;
        }

        SceneListItem selected = (SceneListItem) node.getUserObject();

        if (selected.isSubScene()) {
            try {
                // Load the SubScene
                Scene subScene = FileHandlingSimple.loadSubScene(
                    selected.getParentSceneName(),
                    selected.getName(),
                    game.getGameProgress()
                );

                // Store as currently selected SubScene
                selectedSubScene = subScene;
                selectedScene = null;
                currentSceneName = selected.getParentSceneName();

                log("✓ Loaded SubScene: " + selected.getName() + " from Scene: " + selected.getParentSceneName());

                // Load the scene into the game for preview
                String fullSceneName = selected.getParentSceneName() + "/" + selected.getName();
                game.loadSceneFromDefault(fullSceneName);

                // Reload items list to show items from this scene
                loadItems();

                // Mark as saved (freshly loaded)
                markAsSaved();

                // Update UI
                revalidate();
                repaint();

                log("✓ Scene loaded into editor and game preview");

            } catch (Exception e) {
                log("✗ Failed to load SubScene: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load SubScene: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }



    // ==================== Conditions Table Model ====================

    /**
     * Table model for Conditions - shows condition name and true checkbox
     * Rows are selectable
     */
    private class ConditionsTableModel extends javax.swing.table.AbstractTableModel {
        private java.util.List<ConditionRow> rows = new java.util.ArrayList<>();
        private String[] columnNames = {"Condition", "true"};

        public void clear() {
            rows.clear();
            fireTableDataChanged();
        }

        public void addRow(String conditionName, boolean isTrue) {
            rows.add(new ConditionRow(conditionName, isTrue));
            fireTableDataChanged();
        }

        public void removeRow(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                rows.remove(rowIndex);
                fireTableDataChanged();
            }
        }

        public ConditionRow getRow(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < rows.size()) {
                return rows.get(rowIndex);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ConditionRow row = rows.get(rowIndex);

            switch (columnIndex) {
                case 0: return row.conditionName;
                case 1: return row.isTrue;
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ConditionRow row = rows.get(rowIndex);

            if (columnIndex == 1) {
                row.isTrue = (Boolean) value;

                // Update item's condition
                if (selectedItem != null && selectedItem.getConditions() != null) {
                    selectedItem.getConditions().put(row.conditionName, row.isTrue);
                    log("Condition " + row.conditionName + " = " + row.isTrue);
                }

                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Only the "true" column is editable
            return columnIndex == 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1) {
                return Boolean.class; // Renders as checkbox
            }
            return String.class;
        }

        private class ConditionRow {
            String conditionName;
            boolean isTrue;

            ConditionRow(String conditionName, boolean isTrue) {
                this.conditionName = conditionName;
                this.isTrue = isTrue;
            }
        }
    }

    // ==================== Custom Renderer for Items List ====================

    /**
     * Custom ListCellRenderer that displays item thumbnails
     */
    private class ItemListCellRenderer extends JPanel implements javax.swing.ListCellRenderer<String> {
        private JLabel iconLabel;
        private JLabel textLabel;

        public ItemListCellRenderer() {
            setLayout(new BorderLayout(5, 0));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // Icon/Thumbnail on the left
            iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(50, 50));
            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            iconLabel.setVerticalAlignment(JLabel.CENTER);
            iconLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            add(iconLabel, BorderLayout.WEST);

            // Text on the right
            textLabel = new JLabel();
            textLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            add(textLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String itemName,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            textLabel.setText(itemName);

            // Load thumbnail
            try {
                Item item = ItemLoader.loadItemByName(itemName);
                String imagePath = null;

                // Get image path from conditional images
                if (item.getConditionalImages() != null && !item.getConditionalImages().isEmpty()) {
                    imagePath = item.getConditionalImages().get(0).getImagePath();
                }

                if (imagePath != null && !imagePath.trim().isEmpty()) {
                    // Try to load from resources/images/items/
                    File imageFile = new File("resources/images/items/" + imagePath);

                    // Fallback to resources/images/
                    if (!imageFile.exists()) {
                        imageFile = new File("resources/images/" + imagePath);
                    }

                    if (imageFile.exists()) {
                        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imageFile.getAbsolutePath());

                        // Scale to fit 50x50 while preserving aspect ratio
                        java.awt.Image scaledImage = scaleImagePreserveAspectRatio(
                            icon.getImage(), 50, 50);

                        iconLabel.setIcon(new javax.swing.ImageIcon(scaledImage));
                    } else {
                        iconLabel.setIcon(null);
                        iconLabel.setText("?");
                    }
                } else {
                    iconLabel.setIcon(null);
                    iconLabel.setText("?");
                }
            } catch (Exception ex) {
                iconLabel.setIcon(null);
                iconLabel.setText("!");
            }

            // Selection highlighting
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                textLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                textLabel.setForeground(list.getForeground());
            }

            setOpaque(true);
            return this;
        }
    }

    // ==================== Custom Renderer for Scenes Tree ====================

    /**
     * Custom TreeCellRenderer for Scenes hierarchy with thumbnails
     * Shows Scenes and SubScenes with thumbnails from background images
     */
    private class SceneTreeCellRenderer extends JPanel implements javax.swing.tree.TreeCellRenderer {
        private JLabel iconLabel;
        private JLabel textLabel;

        public SceneTreeCellRenderer() {
            setLayout(new BorderLayout(5, 0));
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            // Icon/Thumbnail on the left
            iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            iconLabel.setVerticalAlignment(JLabel.CENTER);
            iconLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            add(iconLabel, BorderLayout.WEST);

            // Text label on the right
            textLabel = new JLabel();
            textLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            add(textLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            if (!(value instanceof DefaultMutableTreeNode)) {
                textLabel.setText(value.toString());
                iconLabel.setIcon(null);
                iconLabel.setText("");
                return this;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (!(userObject instanceof SceneListItem)) {
                textLabel.setText(userObject.toString());
                iconLabel.setIcon(null);
                iconLabel.setText("");
                return this;
            }

            SceneListItem item = (SceneListItem) userObject;

            // Set text
            if (item.isScene()) {
                textLabel.setText(item.getName());
                textLabel.setFont(new Font("Arial", Font.BOLD, 11));
            } else {
                textLabel.setText(item.getName());
                textLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            }

            // Load thumbnail
            String thumbnailPath = item.getThumbnailPath();
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                try {
                    // Try multiple paths:
                    // 1. resources/images/scenes/[filename]
                    // 2. resources/images/[filename]
                    // 3. [full path]
                    File imgFile = new File("resources/images/scenes/" + thumbnailPath);
                    if (!imgFile.exists()) {
                        imgFile = new File("resources/images/" + thumbnailPath);
                    }
                    if (!imgFile.exists()) {
                        imgFile = new File(thumbnailPath);
                    }

                    if (imgFile.exists()) {
                        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imgFile.getAbsolutePath());
                        java.awt.Image scaledImg = icon.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH);
                        iconLabel.setIcon(new javax.swing.ImageIcon(scaledImg));
                        iconLabel.setText("");
                    } else {
                        iconLabel.setIcon(null);
                        iconLabel.setText("?");
                    }
                } catch (Exception ex) {
                    iconLabel.setIcon(null);
                    iconLabel.setText("!");
                }
            } else {
                iconLabel.setIcon(null);
                iconLabel.setText("?");
            }

            // Selection highlighting
            if (selected) {
                setBackground(javax.swing.UIManager.getColor("Tree.selectionBackground"));
                setForeground(javax.swing.UIManager.getColor("Tree.selectionForeground"));
                textLabel.setForeground(javax.swing.UIManager.getColor("Tree.selectionForeground"));
            } else {
                setBackground(tree.getBackground());
                setForeground(tree.getForeground());
                textLabel.setForeground(tree.getForeground());
            }

            setOpaque(true);
            return this;
        }
    }

    // ==================== Item Properties Panel Helpers ====================

    private void updateItemPropertiesPanel() {
        if (selectedItem == null) {
            clearItemPropertiesPanel();
            return;
        }

        // Update Name
        itemNameLabel.setText("Item: " + selectedItem.getName());

        // Update Image
        updateItemImagePreview();

        // Update Hover Text
        // Note: Items might have conditional hover text, for now we show a placeholder
        itemHoverTextField.setText(selectedItem.getName());

        // Update Conditions
        updateConditionsPanel();

        // Update CustomClickArea Points
        customClickAreaPointsModel.clear();
        if (selectedItem.getCustomClickAreas() != null && !selectedItem.getCustomClickAreas().isEmpty()) {
            for (CustomClickArea area : selectedItem.getCustomClickAreas()) {
                if (area.getPoints() != null) {
                    for (int i = 0; i < area.getPoints().size(); i++) {
                        java.awt.Point p = area.getPoints().get(i);
                        customClickAreaPointsModel.addElement("Point " + (i + 1) + ": (" + p.x + ", " + p.y + ")");
                    }
                }
            }
        }

        // Update MovingRange Points
        movingRangePointsModel.clear();
        System.out.println("DEBUG MovingRange: selectedItem.getMovingRanges() = " + selectedItem.getMovingRanges());
        if (selectedItem.getMovingRanges() != null && !selectedItem.getMovingRanges().isEmpty()) {
            System.out.println("DEBUG MovingRange: Found " + selectedItem.getMovingRanges().size() + " ranges");
            for (MovingRange range : selectedItem.getMovingRanges()) {
                if (range.getPoints() != null) {
                    System.out.println("DEBUG MovingRange: Range has " + range.getPoints().size() + " points");
                    for (int i = 0; i < range.getPoints().size(); i++) {
                        java.awt.Point p = range.getPoints().get(i);
                        movingRangePointsModel.addElement("Point " + (i + 1) + ": (" + p.x + ", " + p.y + ")");
                    }
                }
            }
        } else {
            System.out.println("DEBUG MovingRange: No ranges or empty list");
        }

        // Update Path Points
        pathPointsModel.clear();
        System.out.println("DEBUG Path: selectedItem.getPaths() = " + selectedItem.getPaths());
        if (selectedItem.getPaths() != null && !selectedItem.getPaths().isEmpty()) {
            System.out.println("DEBUG Path: Found " + selectedItem.getPaths().size() + " paths");
            int pointCounter = 1;
            for (Path path : selectedItem.getPaths()) {
                if (path.getPoints() != null) {
                    System.out.println("DEBUG Path: Path has " + path.getPoints().size() + " points");
                    for (int i = 0; i < path.getPoints().size(); i++) {
                        java.awt.Point p = path.getPoints().get(i);
                        pathPointsModel.addElement("Point " + pointCounter + ": (" + p.x + ", " + p.y + ")");
                        pointCounter++;
                    }
                }
            }
        } else {
            System.out.println("DEBUG Path: No paths or empty list");
        }

        log("✓ Updated properties panel for item: " + selectedItem.getName());
    }

    private void clearItemPropertiesPanel() {
        itemNameLabel.setText("No item selected");
        itemImagePreview.setIcon(null);
        itemImagePreview.setText("<html><center>Drag & Drop<br>Image Here</center></html>");
        itemImagePathField.setText("");
        itemHoverTextField.setText("");
        conditionsTableModel.clear();
        customClickAreaPointsModel.clear();
        movingRangePointsModel.clear();
        pathPointsModel.clear();
    }

    private void updateConditionsPanel() {
        conditionsTableModel.clear();

        if (selectedItem == null) {
            return;
        }

        // Get item's current conditions
        java.util.Map<String, Boolean> itemConditions = selectedItem.getConditions();
        if (itemConditions == null || itemConditions.isEmpty()) {
            return;
        }

        // Add each condition to table
        for (java.util.Map.Entry<String, Boolean> entry : itemConditions.entrySet()) {
            String conditionName = entry.getKey();
            Boolean isTrue = entry.getValue();

            // Skip runtime-only conditions
            if (conditionName.startsWith("isInInventory_")) {
                continue;
            }

            conditionsTableModel.addRow(conditionName, isTrue != null ? isTrue : false);
        }
    }

    private void addConditionToItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        // Get all available conditions
        java.util.Map<String, Boolean> allConditions = Conditions.getAllConditions();
        if (allConditions == null || allConditions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No conditions available", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Create list of available conditions (excluding runtime-only)
        java.util.List<String> availableConditions = new java.util.ArrayList<>();
        for (String condName : allConditions.keySet()) {
            if (!condName.startsWith("isInInventory_")) {
                availableConditions.add(condName);
            }
        }

        if (availableConditions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No conditions available", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show selection dialog
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select condition to add:",
            "Add Condition",
            JOptionPane.PLAIN_MESSAGE,
            null,
            availableConditions.toArray(),
            availableConditions.get(0)
        );

        if (selected != null) {
            java.util.Map<String, Boolean> itemConditions = selectedItem.getConditions();
            if (itemConditions == null) {
                log("ERROR: Item has no conditions map");
                return;
            }

            // Add with default value true
            itemConditions.put(selected, true);
            updateConditionsPanel();
            log("✓ Added condition: " + selected);
        }
    }

    private void removeConditionFromItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        // Get selected row from table
        int selectedRow = itemConditionsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a condition to delete",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get condition name from selected row
        ConditionsTableModel.ConditionRow row = conditionsTableModel.getRow(selectedRow);
        if (row == null) {
            return;
        }

        String conditionName = row.conditionName;

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete condition '" + conditionName + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove from item
        java.util.Map<String, Boolean> itemConditions = selectedItem.getConditions();
        if (itemConditions != null) {
            itemConditions.remove(conditionName);
            updateConditionsPanel();
            log("✓ Removed condition: " + conditionName);
        }
    }

    private void renameConditionInItem() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        // Get selected row from table
        int selectedRow = itemConditionsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a condition to rename",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get condition name from selected row
        ConditionsTableModel.ConditionRow row = conditionsTableModel.getRow(selectedRow);
        if (row == null) {
            return;
        }

        String oldName = row.conditionName;
        boolean oldValue = row.isTrue;

        // Ask for new name
        String newName = JOptionPane.showInputDialog(this,
            "Enter new name for condition '" + oldName + "':",
            oldName);

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        newName = newName.trim();

        if (newName.equals(oldName)) {
            return; // No change
        }

        // Check if new name already exists
        java.util.Map<String, Boolean> itemConditions = selectedItem.getConditions();
        if (itemConditions != null && itemConditions.containsKey(newName)) {
            JOptionPane.showMessageDialog(this,
                "Condition '" + newName + "' already exists!",
                "Duplicate",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Rename: remove old, add new
        if (itemConditions != null) {
            itemConditions.remove(oldName);
            itemConditions.put(newName, oldValue);
            updateConditionsPanel();
            log("✓ Renamed condition: " + oldName + " → " + newName);
        }
    }

    private void saveItemToProgress() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        try {
            ItemSaver.saveItemToProgress(selectedItem);
            log("✓ Saved to PROGRESS: " + selectedItem.getName());
            JOptionPane.showMessageDialog(this,
                "Item saved to progress file!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            log("ERROR saving to progress: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                "Error saving to progress: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveItemAsDefault() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        try {
            ItemSaver.saveItemToDefault(selectedItem);
            log("✓ Saved to DEFAULT: " + selectedItem.getName());
            JOptionPane.showMessageDialog(this,
                "Item saved to default file!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            log("ERROR saving to default: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                "Error saving to default: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateItemImagePreview() {
        if (selectedItem == null) {
            return;
        }

        // Get image path from Item (as per Schema: #ImagePath:)
        String imagePath = selectedItem.getImageFilePath();

        if (imagePath != null && !imagePath.trim().isEmpty()) {
            itemImagePathField.setText(imagePath);

            // Try to load from resources/images/items/
            File imageFile = new File("resources/images/items/" + imagePath);

            // Fallback to resources/images/
            if (!imageFile.exists()) {
                imageFile = new File("resources/images/" + imagePath);
            }

            if (imageFile.exists()) {
                try {
                    javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imageFile.getAbsolutePath());

                    // Scale to fit 200x100 while preserving aspect ratio
                    java.awt.Image scaledImage = scaleImagePreserveAspectRatio(
                        icon.getImage(), 200, 100);

                    itemImagePreview.setIcon(new javax.swing.ImageIcon(scaledImage));
                    itemImagePreview.setText("");
                } catch (Exception ex) {
                    log("Warning: Could not load image: " + ex.getMessage());
                }
            } else {
                log("Warning: Image file not found: " + imagePath);
            }
        } else {
            itemImagePathField.setText("");
            itemImagePreview.setIcon(null);
            itemImagePreview.setText("<html><center>Drag & Drop<br>Image Here</center></html>");
        }
    }

    /**
     * Scales an image to fit within target dimensions while preserving aspect ratio
     */
    private java.awt.Image scaleImagePreserveAspectRatio(java.awt.Image original, int targetWidth, int targetHeight) {
        int originalWidth = original.getWidth(null);
        int originalHeight = original.getHeight(null);

        // Calculate scaling factor to fit within target dimensions
        double scaleX = (double) targetWidth / originalWidth;
        double scaleY = (double) targetHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);

        // Calculate new dimensions
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        // Scale the image
        return original.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);
    }

    private void setupImageDropTarget(JLabel label) {
        label.setTransferHandler(new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(javax.swing.TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(javax.swing.TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    java.awt.datatransfer.Transferable transferable = support.getTransferable();
                    @SuppressWarnings("unchecked")
                    java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(
                        java.awt.datatransfer.DataFlavor.javaFileListFlavor);

                    if (!files.isEmpty()) {
                        File file = files.get(0);

                        // Copy to resources/images/items/
                        File itemsImagesDir = new File("resources/images/items");
                        itemsImagesDir.mkdirs();

                        File targetFile = new File(itemsImagesDir, file.getName());
                        java.nio.file.Files.copy(file.toPath(), targetFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        // Update item (as per Schema: #ImagePath:)
                        if (selectedItem != null) {
                            selectedItem.setImageFilePath(file.getName());

                            // Update UI
                            updateItemImagePreview();

                            // Auto-save
                            saveItem();

                            log("✓ Image dropped and saved: " + file.getName());
                        }

                        return true;
                    }
                } catch (Exception ex) {
                    log("ERROR dropping image: " + ex.getMessage());
                    ex.printStackTrace();
                }

                return false;
            }
        });
    }

    private void updateItemHoverText() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        String newHoverText = itemHoverTextField.getText();
        // TODO: Update item's hover text (might need to adjust Item class structure)
        log("TODO: Update hover text to: " + newHoverText);
    }

    private void addCustomClickAreaPoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        CustomClickArea area = selectedItem.ensurePrimaryCustomClickArea();
        area.addPoint(100, 100);

        // Save item
        try {
            ItemSaver.saveItemByName(selectedItem);
            log("✓ Added point to CustomClickArea at (100, 100)");
        } catch (java.io.IOException ex) {
            log("✗ Failed to save item: " + ex.getMessage());
        }

        // Refresh UI
        updateItemPropertiesPanel();
    }

    private void removeCustomClickAreaPoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = customClickAreaPointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        CustomClickArea area = selectedItem.getPrimaryCustomClickArea();
        if (area != null && area.getPoints() != null && selectedIndex < area.getPoints().size()) {
            area.getPoints().remove(selectedIndex);
            area.updatePolygon();

            updateItemPropertiesPanel();
            log("✓ Removed point at index " + selectedIndex);
        }
    }

    private void moveCustomClickAreaPointUp() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = customClickAreaPointsList.getSelectedIndex();
        if (selectedIndex <= 0) {
            log("Cannot move point up");
            return;
        }

        CustomClickArea area = selectedItem.getPrimaryCustomClickArea();
        if (area != null) {
            java.util.List<java.awt.Point> points = area.getPoints();
            if (points != null && selectedIndex < points.size()) {
                // Swap with previous point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex - 1));
                points.set(selectedIndex - 1, temp);
                area.updatePolygon();

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                customClickAreaPointsList.setSelectedIndex(selectedIndex - 1);
                log("✓ Moved point up");
            }
        }
    }

    private void moveCustomClickAreaPointDown() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = customClickAreaPointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        CustomClickArea area = selectedItem.getPrimaryCustomClickArea();
        if (area != null) {
            java.util.List<java.awt.Point> points = area.getPoints();
            if (points != null && selectedIndex < points.size() - 1) {
                // Swap with next point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex + 1));
                points.set(selectedIndex + 1, temp);
                area.updatePolygon();

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                customClickAreaPointsList.setSelectedIndex(selectedIndex + 1);
                log("✓ Moved point down");
            } else {
                log("Cannot move point down");
            }
        }
    }

    private void startAddPointByClickMode(String pointType) {
        System.out.println("=== startAddPointByClickMode called ===");
        System.out.println("  pointType: " + pointType);
        System.out.println("  selectedItem: " + (selectedItem != null ? selectedItem.getName() : "NULL"));

        if (selectedItem == null) {
            log("No item selected");
            System.out.println("  ABORTED: No item selected");
            return;
        }

        // Set the item as selected in the scene (for rendering)
        if (selectedSubScene != null) {
            System.out.println("  Setting selected item in scene...");
            selectedSubScene.setSelectedItem(selectedItem);
        } else {
            System.out.println("  WARNING: selectedSubScene is null");
        }

        // Enable path visualization FIRST to show points immediately
        System.out.println("  Enabling path visualization...");
        game.setShowPaths(true);

        // CRITICAL: Set visibility flags for the point type BEFORE opening editor
        if (pointType.equals("CustomClickArea")) {
            selectedItem.setCustomClickAreaVisibleInEditor(true);
            System.out.println("  Set CustomClickArea visible");
        } else if (pointType.equals("MovingRange")) {
            selectedItem.setMovingRangeVisibleInEditor(true);
            System.out.println("  Set MovingRange visible");
        } else if (pointType.equals("Path")) {
            selectedItem.setPathVisibleInEditor(true);
            System.out.println("  Set Path visible");
        }

        game.repaint();

        // Get the points list for this type
        List<java.awt.Point> points = getPointsForType(selectedItem, pointType);

        log("✓ Opening Point Editor for " + pointType + " with " + points.size() + " points");
        System.out.println("  Points count: " + points.size());

        System.out.println("  Creating PointEditorDialog...");

        // Create Point Editor Dialog (this will open the window)
        try {
            PointEditorDialog pointEditor = new PointEditorDialog(
                this,
                points,
                updatedPoints -> {
                    // Callback: Update the item's points
                    setPointsForType(selectedItem, pointType, updatedPoints);

                    // Auto-save
                    try {
                        ItemSaver.saveItemToDefault(selectedItem);
                        log("   Auto-saved: " + selectedItem.getName());
                    } catch (Exception e) {
                        log("   ERROR saving: " + e.getMessage());
                    }

                    // CRITICAL: Reload scene in game to show updated points
                    try {
                        FileHandlingSimple.saveSubSceneToDefault(selectedSubScene, currentSceneName);

                        // Store item name before reload
                        String itemName = selectedItem.getName();

                        // Reload scene in game window
                        game.reloadCurrentScene();

                        // Re-select the item after reload
                        Scene reloadedScene = game.getCurrentScene();
                        if (reloadedScene != null) {
                            // Update selectedSubScene to point to the newly loaded scene
                            selectedSubScene = reloadedScene;

                            for (Item item : reloadedScene.getItems()) {
                                if (item.getName().equals(itemName)) {
                                    reloadedScene.setSelectedItem(item);
                                    // CRITICAL: Update selectedItem to point to the newly loaded item
                                    selectedItem = item;
                                    // Restore visibility flags for points
                                    if (pointType.equals("CustomClickArea")) {
                                        item.setCustomClickAreaVisibleInEditor(true);
                                    } else if (pointType.equals("MovingRange")) {
                                        item.setMovingRangeVisibleInEditor(true);
                                    } else if (pointType.equals("Path")) {
                                        item.setPathVisibleInEditor(true);
                                    }
                                    break;
                                }
                            }
                        }

                        log("   Reloaded scene in game window and re-selected item");
                    } catch (Exception e) {
                        log("   ERROR reloading scene: " + e.getMessage());
                    }

                    // Update UI and re-render
                    updateItemPropertiesPanel();
                    game.setShowPaths(true); // Ensure paths are visible
                    game.repaint();
                },
                selectedItem.getName(),
                pointType
            );

            System.out.println("  PointEditorDialog created successfully");
            System.out.println("  Dialog is showing: " + pointEditor.isShowing());
            System.out.println("  Dialog is visible: " + pointEditor.isVisible());

            log("✓ Point Editor opened - Click on game canvas to add points");
        } catch (Exception e) {
            System.err.println("  ERROR creating PointEditorDialog: " + e.getMessage());
            e.printStackTrace();
            log("✗ ERROR opening Point Editor: " + e.getMessage());
        }

        System.out.println("=== startAddPointByClickMode complete ===");
    }

    /**
     * Get points list for a specific type.
     */
    private List<java.awt.Point> getPointsForType(Item item, String pointType) {
        switch (pointType) {
            case "CustomClickArea":
                CustomClickArea area = item.ensurePrimaryCustomClickArea();
                if (area.getHoverText() == null || area.getHoverText().isEmpty()) {
                    area.setHoverText(item.getName());
                }
                return area.getPoints();

            case "MovingRange":
                return item.ensurePrimaryMovingRange().getPoints();

            case "Path":
                return item.ensurePrimaryPath().getPoints();

            case "Item":
            default:
                return item.getClickAreaPoints();
        }
    }

    /**
     * Set points list for a specific type.
     */
    private void setPointsForType(Item item, String pointType, List<java.awt.Point> newPoints) {
        switch (pointType) {
            case "CustomClickArea":
                CustomClickArea area = item.ensurePrimaryCustomClickArea();
                area.getPoints().clear();
                area.getPoints().addAll(newPoints);
                area.updatePolygon();
                break;

            case "MovingRange":
                MovingRange range = item.ensurePrimaryMovingRange();
                range.getPoints().clear();
                range.getPoints().addAll(newPoints);
                range.updatePolygon();
                break;

            case "Path":
                Path path = item.ensurePrimaryPath();
                path.getPoints().clear();
                path.getPoints().addAll(newPoints);
                path.updatePolygon();
                break;

            case "Item":
            default:
                item.getClickAreaPoints().clear();
                item.getClickAreaPoints().addAll(newPoints);
                break;
        }
    }

    public void addPointFromCanvas(int x, int y, String pointType) {
        if (selectedItem == null) {
            log("ERROR: No item selected");
            return;
        }

        java.awt.Point newPoint = new java.awt.Point(x, y);

        switch (pointType) {
            case "CustomClickArea":
                CustomClickArea area = selectedItem.ensurePrimaryCustomClickArea();
                area.getPoints().add(newPoint);
                area.updatePolygon();
                log("✓ Added point #" + area.getPoints().size() + " at (" + x + ", " + y + ") to CustomClickArea");
                break;

            case "MovingRange":
                MovingRange range = selectedItem.ensurePrimaryMovingRange();
                range.getPoints().add(newPoint);
                range.updatePolygon();
                log("✓ Added point #" + range.getPoints().size() + " at (" + x + ", " + y + ") to MovingRange");
                break;

            case "Path":
                Path path = selectedItem.ensurePrimaryPath();
                path.getPoints().add(newPoint);
                path.updatePolygon();
                log("✓ Added point #" + path.getPoints().size() + " at (" + x + ", " + y + ") to Path");
                break;
        }

        // Auto-save
        try {
            ItemSaver.saveItemByName(selectedItem);
            log("   Auto-saved: " + selectedItem.getName());
        } catch (Exception e) {
            log("   ERROR saving: " + e.getMessage());
        }

        // Update UI
        updateItemPropertiesPanel();
        revalidate();
        repaint();

        // Repaint game canvas to show new point
        game.repaint();
    }

    private void editCustomClickAreaPoints() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        CustomClickArea area = selectedItem.getPrimaryCustomClickArea();
        if (area == null) {
            log("No CustomClickArea defined");
            return;
        }
        if (area.getPoints() == null || area.getPoints().isEmpty()) {
            log("No points defined in CustomClickArea");
            return;
        }

        log("Edit mode: Use Add/Remove buttons to modify points");
        log("(Full Point Editor dialog requires refactoring to work with JFrame)");
    }

    private void addMovingRangePoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        System.out.println("DEBUG addMovingRangePoint: Starting...");

        MovingRange range = selectedItem.ensurePrimaryMovingRange();
        range.addPoint(100, 100);
        System.out.println("DEBUG addMovingRangePoint: Added point, now has " + range.getPoints().size() + " points");
        System.out.println("DEBUG addMovingRangePoint: selectedItem.getMovingRanges().size() = " + selectedItem.getMovingRanges().size());

        // Save item
        try {
            ItemSaver.saveItemByName(selectedItem);
            log("✓ Added point to MovingRange at (100, 100)");
        } catch (java.io.IOException ex) {
            log("✗ Failed to save item: " + ex.getMessage());
        }

        // Refresh UI
        System.out.println("DEBUG addMovingRangePoint: Calling updateItemPropertiesPanel()...");
        updateItemPropertiesPanel();
    }

    private void removeMovingRangePoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = movingRangePointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        MovingRange range = selectedItem.getPrimaryMovingRange();
        if (range != null && range.getPoints() != null && selectedIndex < range.getPoints().size()) {
            range.getPoints().remove(selectedIndex);

            // Save and refresh
            try {
                ItemSaver.saveItemByName(selectedItem);
            } catch (java.io.IOException ex) {
                log("✗ Failed to save item: " + ex.getMessage());
            }
            updateItemPropertiesPanel();
            log("✓ Removed point at index " + selectedIndex);
        }
    }

    private void moveMovingRangePointUp() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = movingRangePointsList.getSelectedIndex();
        if (selectedIndex <= 0) {
            log("Cannot move point up");
            return;
        }

        MovingRange range = selectedItem.getPrimaryMovingRange();
        if (range != null) {
            java.util.List<java.awt.Point> points = range.getPoints();
            if (points != null && selectedIndex < points.size()) {
                // Swap with previous point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex - 1));
                points.set(selectedIndex - 1, temp);

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                movingRangePointsList.setSelectedIndex(selectedIndex - 1);
                log("✓ Moved point up");
            }
        }
    }

    private void moveMovingRangePointDown() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = movingRangePointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        MovingRange range = selectedItem.getPrimaryMovingRange();
        if (range != null) {
            java.util.List<java.awt.Point> points = range.getPoints();
            if (points != null && selectedIndex < points.size() - 1) {
                // Swap with next point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex + 1));
                points.set(selectedIndex + 1, temp);

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                movingRangePointsList.setSelectedIndex(selectedIndex + 1);
                log("✓ Moved point down");
            } else {
                log("Cannot move point down");
            }
        }
    }

    private void addPathPoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        System.out.println("DEBUG addPathPoint: Starting...");

        Path path = selectedItem.ensurePrimaryPath();
        path.addPoint(100, 100);
        System.out.println("DEBUG addPathPoint: Added point, now has " + path.getPoints().size() + " points");
        System.out.println("DEBUG addPathPoint: selectedItem.getPaths().size() = " + selectedItem.getPaths().size());

        // Save item
        try {
            ItemSaver.saveItemByName(selectedItem);
            log("✓ Added point to Path at (100, 100)");
        } catch (java.io.IOException ex) {
            log("✗ Failed to save item: " + ex.getMessage());
        }

        // Refresh UI
        System.out.println("DEBUG addPathPoint: Calling updateItemPropertiesPanel()...");
        updateItemPropertiesPanel();
    }

    private void removePathPoint() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = pathPointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        Path path = selectedItem.getPrimaryPath();
        if (path != null && path.getPoints() != null && selectedIndex < path.getPoints().size()) {
            path.getPoints().remove(selectedIndex);

            // Save and refresh
            try {
                ItemSaver.saveItemByName(selectedItem);
            } catch (java.io.IOException ex) {
                log("✗ Failed to save item: " + ex.getMessage());
            }
            updateItemPropertiesPanel();
            log("✓ Removed point at index " + selectedIndex);
        }
    }

    private void movePathPointUp() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = pathPointsList.getSelectedIndex();
        if (selectedIndex <= 0) {
            log("Cannot move point up");
            return;
        }

        Path path = selectedItem.getPrimaryPath();
        if (path != null) {
            java.util.List<java.awt.Point> points = path.getPoints();
            if (points != null && selectedIndex < points.size()) {
                // Swap with previous point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex - 1));
                points.set(selectedIndex - 1, temp);

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                pathPointsList.setSelectedIndex(selectedIndex - 1);
                log("✓ Moved point up");
            }
        }
    }

    private void movePathPointDown() {
        if (selectedItem == null) {
            log("No item selected");
            return;
        }

        int selectedIndex = pathPointsList.getSelectedIndex();
        if (selectedIndex < 0) {
            log("No point selected");
            return;
        }

        Path path = selectedItem.getPrimaryPath();
        if (path != null) {
            java.util.List<java.awt.Point> points = path.getPoints();
            if (points != null && selectedIndex < points.size() - 1) {
                // Swap with next point
                java.awt.Point temp = points.get(selectedIndex);
                points.set(selectedIndex, points.get(selectedIndex + 1));
                points.set(selectedIndex + 1, temp);

                // Save and refresh
                try {
                    ItemSaver.saveItemByName(selectedItem);
                } catch (java.io.IOException ex) {
                    log("✗ Failed to save item: " + ex.getMessage());
                }
                updateItemPropertiesPanel();
                pathPointsList.setSelectedIndex(selectedIndex + 1);
                log("✓ Moved point down");
            } else {
                log("Cannot move point down");
            }
        }
    }

    /**
     * Update item image visibility in edit mode based on Show checkbox
     */
    private void updateItemImageVisibility() {
        if (selectedItem == null || game == null) {
            return;
        }

        boolean showImage = itemImageShowCheckBox.isSelected();
        game.setItemImageVisibleInEditor(selectedItem, showImage);
        log((showImage ? "✓ Showing" : "✓ Hiding") + " item image in editor");
    }

    /**
     * Update custom click area visibility in edit mode based on Show checkbox
     */
    private void updateCustomClickAreaVisibility() {
        if (selectedItem == null || game == null) {
            return;
        }

        boolean show = customClickAreaShowCheckBox.isSelected();
        game.setCustomClickAreaVisibleInEditor(selectedItem, show);
        log((show ? "✓ Showing" : "✓ Hiding") + " custom click area in editor");
    }

    /**
     * Update moving range visibility in edit mode based on Show checkbox
     */
    private void updateMovingRangeVisibility() {
        if (selectedItem == null || game == null) {
            return;
        }

        boolean show = movingRangeShowCheckBox.isSelected();
        game.setMovingRangeVisibleInEditor(selectedItem, show);
        log((show ? "✓ Showing" : "✓ Hiding") + " moving range in editor");
    }

    /**
     * Update path visibility in edit mode based on Show checkbox
     */
    private void updatePathVisibility() {
        if (selectedItem == null || game == null) {
            return;
        }

        boolean show = pathShowCheckBox.isSelected();
        game.setPathVisibleInEditor(selectedItem, show);
        log((show ? "✓ Showing" : "✓ Hiding") + " path in editor");
    }

    /**
     * Save the current scene order from the tree to file.
     */
    private void saveSceneOrder() {
        Map<String, List<String>> order = SceneOrderManager.extractOrderFromTree(scenesRootNode);
        SceneOrderManager.saveOrder(order);
        log("✓ Scene order saved");
    }

    /**
     * Load saved scene order and apply it when populating the tree.
     * Call this before adding scenes to the tree.
     */
    private Map<String, List<String>> loadSceneOrder() {
        Map<String, List<String>> order = SceneOrderManager.loadOrder();
        if (!order.isEmpty()) {
            log("✓ Loaded saved scene order");
        }
        return order;
    }

    /**
     * Open the Scene Point Editor
     */
    private void openScenePointEditor() {
        if (selectedSubScene == null) {
            log("No scene selected");
            JOptionPane.showMessageDialog(this, "Please load a scene first", "No Scene", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open the new Scene Point Editor
        ScenePointEditor editor = new ScenePointEditor(this, selectedSubScene, selectedItem);
        log("✓ Opened Scene Point Editor");
    }

    /**
     * Opens the Orientation Editor for setting orientation-based images
     */
    private void openOrientationEditor() {
        if (selectedSubScene == null) {
            log("No scene selected");
            JOptionPane.showMessageDialog(this, "Please load a scene first", "No Scene", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open the Orientation Editor
        ItemOrientationEditor editor = new ItemOrientationEditor(this);
        log("✓ Opened Orientation Editor");
    }

    /**
     * Save editor settings (window position, size, split pane positions, etc.)
     */
    private void saveSettings() {
        // Save window bounds
        settings.setWindowBounds("editor.main", getBounds());

        // Split pane dividers are already saved via listeners

        // Save the currently loaded scene (if any)
        if (selectedSubScene != null && selectedScene != null) {
            String fullSceneName = selectedScene.getName() + "/" + selectedSubScene.getName();
            settings.setString("editor.lastScene", fullSceneName);
        }

        // Save Always on Top setting
        settings.setBoolean("editor.alwaysOnTop", isAlwaysOnTop());

        // Persist to file
        settings.save();
        log("✓ Editor settings saved");
    }

    /**
     * Get the name of the currently selected parent Scene
     * @return Parent scene name, or null if no SubScene is selected
     */
    public String getSelectedSceneName() {
        return currentSceneName;
    }

    /**
     * Public method to reload items from current SubScene
     * Used by ItemManagerDialog after Add/Remove operations
     */
    public void loadItemsIntoEditor() {
        loadItems();
    }

    /**
     * Get the current AdventureGame instance
     */
    public AdventureGame getGame() {
        return game;
    }

    // ==================== BIDIRECTIONAL CHECKBOX REFRESH METHODS ====================

    /**
     * Refresh "Show" checkbox for an item (called from AdventureGame when checkbox changes in ScenePointEditor)
     */
    public void refreshShowCheckbox(Item item, boolean visible) {
        if (item == null || item.getName() == null) return;

        JCheckBox checkbox = itemCheckBoxes.get(item.getName());
        if (checkbox != null) {
            checkbox.setSelected(visible);
        }
    }

    /**
     * Refresh "Follow" checkbox for an item (called from AdventureGame when checkbox changes in ScenePointEditor)
     */
    public void refreshFollowingMouseCheckbox(Item item, boolean following) {
        if (item == null || item.getName() == null) return;

        JCheckBox checkbox = itemFollowingMouseCheckBoxes.get(item.getName());
        if (checkbox != null) {
            checkbox.setSelected(following);
        }
    }

    /**
     * Refresh "Click" checkbox for an item (called from AdventureGame when checkbox changes in ScenePointEditor)
     */
    public void refreshFollowingOnClickCheckbox(Item item, boolean followingOnClick) {
        if (item == null || item.getName() == null) return;

        JCheckBox checkbox = itemFollowingOnClickCheckBoxes.get(item.getName());
        if (checkbox != null) {
            checkbox.setSelected(followingOnClick);
        }
    }
}
