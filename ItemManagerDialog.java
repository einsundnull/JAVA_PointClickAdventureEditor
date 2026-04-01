package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Global Item Manager Dialog
 * Manages all items in the game (not just items in the current SubScene)
 * Provides CRUD operations: Create, Read, Update, Delete, Copy
 */
public class ItemManagerDialog extends JDialog {
    private EditorMainSimple parent;
    private JPanel itemListPanel; // Panel containing checkboxes
    private List<String> allItemNames;
    private Map<String, JCheckBox> itemCheckBoxes; // Map: itemName -> checkbox
    private Map<String, JLabel> itemLabels; // Map: itemName -> label
    private Scene currentSubScene; // Current SubScene for Add/Remove operations
    private String selectedItemName; // Currently selected item

    public ItemManagerDialog(EditorMainSimple parent, Scene currentSubScene) {
        super(parent, "Item Manager - All Items", false); // Non-modal
        this.parent = parent;
        this.currentSubScene = currentSubScene;
        this.allItemNames = new ArrayList<>();
        this.itemCheckBoxes = new HashMap<>();
        this.itemLabels = new HashMap<>();
        this.selectedItemName = null;

        setSize(800, 600);
        setLocationRelativeTo(parent);

        initUI();
        loadAllItems();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Item Manager - All Items");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Center: Panel with checkboxes for all items
        itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));
        itemListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(itemListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("All Items (from /resources/items/) | Show: [x] = Hide in Editor"));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton newBtn = new JButton("New Item");
        newBtn.setToolTipText("Create a new item file");
        newBtn.addActionListener(e -> createNewItem());
        buttonPanel.add(newBtn);

        JButton editBtn = new JButton("Edit Item");
        editBtn.setToolTipText("Edit the selected item");
        editBtn.addActionListener(e -> editSelectedItem());
        buttonPanel.add(editBtn);

        JButton copyBtn = new JButton("Copy Item");
        copyBtn.setToolTipText("Duplicate the selected item");
        copyBtn.addActionListener(e -> copySelectedItem());
        buttonPanel.add(copyBtn);

        JButton deleteBtn = new JButton("Delete Item");
        deleteBtn.setToolTipText("Delete the selected item file");
        deleteBtn.addActionListener(e -> deleteSelectedItem());
        buttonPanel.add(deleteBtn);

        buttonPanel.add(Box.createHorizontalStrut(20)); // Spacer

        JButton addToSceneBtn = new JButton("Add to Scene");
        addToSceneBtn.setToolTipText("Add selected item to current SubScene");
        addToSceneBtn.addActionListener(e -> addItemToScene());
        buttonPanel.add(addToSceneBtn);

        JButton removeFromSceneBtn = new JButton("Remove from Scene");
        removeFromSceneBtn.setToolTipText("Remove selected item from current SubScene");
        removeFromSceneBtn.addActionListener(e -> removeItemFromScene());
        buttonPanel.add(removeFromSceneBtn);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setToolTipText("Reload all items");
        refreshBtn.addActionListener(e -> loadAllItems());
        buttonPanel.add(refreshBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.setToolTipText("Close Item Manager");
        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Window close listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    /**
     * Load all items from /resources/items/ directory
     */
    private void loadAllItems() {
        itemListPanel.removeAll();
        allItemNames.clear();
        itemCheckBoxes.clear();
        itemLabels.clear();

        File itemsDir = ResourcePathHelper.resolve("items");
        if (!itemsDir.exists() || !itemsDir.isDirectory()) {
            parent.log("Items directory not found: resources/items");
            return;
        }

        File[] itemFiles = itemsDir.listFiles((dir, name) ->
            name.endsWith(".txt") && !name.endsWith("_progress.txt")
        );

        if (itemFiles == null || itemFiles.length == 0) {
            parent.log("No items found in resources/items");
            return;
        }

        // Sort alphabetically
        Arrays.sort(itemFiles, Comparator.comparing(File::getName));

        for (File file : itemFiles) {
            String itemName = file.getName().replace(".txt", "");
            allItemNames.add(itemName);

            // Check if item is in current SubScene
            boolean inCurrentScene = false;
            if (currentSubScene != null && currentSubScene.getItems() != null) {
                inCurrentScene = currentSubScene.getItems().stream()
                    .anyMatch(item -> item.getName().equals(itemName));
            }

            // Create row panel with checkbox and label
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            rowPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

            // CheckBox for show/hide in editor
            JCheckBox showCheckBox = new JCheckBox();
            showCheckBox.setToolTipText("Check to hide this item in Edit Mode");
            showCheckBox.addActionListener(e -> {
                boolean hideInEditor = showCheckBox.isSelected();
                if (hideInEditor) {
                    parent.log("Hiding item in editor: " + itemName);
                    parent.getGame().hideItemInEditor(itemName);
                } else {
                    parent.log("Showing item in editor: " + itemName);
                    parent.getGame().showItemInEditor(itemName);
                }
            });
            itemCheckBoxes.put(itemName, showCheckBox);

            // Label with item name and [IN SCENE] marker
            String displayText = inCurrentScene ? itemName + " [IN SCENE]" : itemName;
            JLabel itemLabel = new JLabel(displayText);
            itemLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
            itemLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectItem(itemName);
                }
            });
            itemLabels.put(itemName, itemLabel);

            rowPanel.add(showCheckBox);
            rowPanel.add(itemLabel);
            itemListPanel.add(rowPanel);
        }

        itemListPanel.revalidate();
        itemListPanel.repaint();

        parent.log("Loaded " + allItemNames.size() + " items");
    }

    /**
     * Select an item in the list
     */
    private void selectItem(String itemName) {
        // Deselect previous
        if (selectedItemName != null && itemLabels.containsKey(selectedItemName)) {
            itemLabels.get(selectedItemName).setForeground(null); // Reset color
        }

        // Select new
        selectedItemName = itemName;
        if (itemLabels.containsKey(selectedItemName)) {
            itemLabels.get(selectedItemName).setForeground(java.awt.Color.BLUE);
        }
    }

    /**
     * Create a new item
     */
    private void createNewItem() {
        String itemName = JOptionPane.showInputDialog(this,
            "Enter name for new item:",
            "New Item",
            JOptionPane.PLAIN_MESSAGE);

        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        itemName = itemName.trim();

        // Check if item already exists
        File itemFile = ResourcePathHelper.resolve("items/" + itemName + ".txt");
        if (itemFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Item '" + itemName + "' already exists!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create new item with default values
        try {
            Item newItem = new Item(itemName);
            newItem.setImageFilePath(ResourcePathHelper.resolvePath("images/items/default.png"));
            newItem.setPosition(new Point(100, 100));
            newItem.setSize(100, 100);
            newItem.setInInventory(false);

            // Save to file
            ItemSaver.saveItemToDefault(newItem);
            parent.log("Created new item: " + itemName);

            // Reload list
            loadAllItems();

            // Select the new item
            selectItem(itemName);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to create item: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR creating item: " + e.getMessage());
        }
    }

    /**
     * Edit the selected item
     */
    private void editSelectedItem() {
        if (selectedItemName == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an item to edit!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = selectedItemName;

        try {
            // Load the item
            Item item = ItemLoader.loadItemFromDefault(itemName);

            // Open ItemEditorDialog
            // Note: ItemEditorDialog might need to be adapted for EditorMainSimple
            parent.log("Opening editor for item: " + itemName);

            // TODO: Open item editor (might need to create a simple item editor dialog)
            JOptionPane.showMessageDialog(this,
                "Item editor not yet implemented.\nItem: " + itemName,
                "TODO",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load item: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR loading item: " + e.getMessage());
        }
    }

    /**
     * Copy the selected item
     */
    private void copySelectedItem() {
        if (selectedItemName == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an item to copy!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sourceItemName = selectedItemName;

        String newItemName = JOptionPane.showInputDialog(this,
            "Enter name for copied item:",
            sourceItemName + "_copy");

        if (newItemName == null || newItemName.trim().isEmpty()) {
            return;
        }

        newItemName = newItemName.trim();

        // Check if item already exists
        File itemFile = ResourcePathHelper.resolve("items/" + newItemName + ".txt");
        if (itemFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Item '" + newItemName + "' already exists!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Load source item
            Item sourceItem = ItemLoader.loadItemFromDefault(sourceItemName);

            // Create copy with new name
            Item copiedItem = new Item(newItemName);
            copiedItem.setImageFilePath(sourceItem.getImageFilePath());
            copiedItem.setPosition(new Point(sourceItem.getPosition()));
            copiedItem.setSize(sourceItem.getWidth(), sourceItem.getHeight());
            copiedItem.setInInventory(sourceItem.isInInventory());

            // Copy points
            if (sourceItem.getClickAreaPoints() != null) {
                copiedItem.getClickAreaPoints().addAll(sourceItem.getClickAreaPoints());
            }

            // Copy conditional images
            if (sourceItem.getConditionalImages() != null) {
                for (ConditionalImage img : sourceItem.getConditionalImages()) {
                    copiedItem.addConditionalImage(img.copy());
                }
            }

            // Copy hover display conditions
            if (sourceItem.getHoverDisplayConditions() != null) {
                for (Map.Entry<String, String> entry : sourceItem.getHoverDisplayConditions().entrySet()) {
                    copiedItem.addHoverDisplayCondition(entry.getKey(), entry.getValue());
                }
            }

            // Save to file
            ItemSaver.saveItemToDefault(copiedItem);
            parent.log("Copied item: " + sourceItemName + " -> " + newItemName);

            // Reload list
            loadAllItems();

            // Select the new item
            selectItem(newItemName);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to copy item: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR copying item: " + e.getMessage());
        }
    }

    /**
     * Delete the selected item
     */
    private void deleteSelectedItem() {
        if (selectedItemName == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an item to delete!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = selectedItemName;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete item '" + itemName + "'?\nThis will delete the file and cannot be undone!",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        File itemFile = ResourcePathHelper.resolve("items/" + itemName + ".txt");
        if (itemFile.delete()) {
            parent.log("Deleted item: " + itemName);
            loadAllItems();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to delete item file!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR deleting item: " + itemName);
        }
    }

    /**
     * Add selected item to current SubScene
     */
    private void addItemToScene() {
        if (currentSubScene == null) {
            JOptionPane.showMessageDialog(this,
                "No SubScene is currently selected!",
                "No SubScene",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedItemName == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an item to add!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = selectedItemName;

        // Check if already in scene
        if (currentSubScene.getItems().stream().anyMatch(item -> item.getName().equals(itemName))) {
            JOptionPane.showMessageDialog(this,
                "Item '" + itemName + "' is already in this SubScene!",
                "Already in Scene",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // Load the item
            Item item = ItemLoader.loadItemFromDefault(itemName);

            // Add to scene
            currentSubScene.addItem(item);

            // Save scene
            FileHandlingSimple.saveSubSceneToDefault(
                currentSubScene,
                parent.getSelectedSceneName()
            );

            parent.log("Added item '" + itemName + "' to SubScene: " + currentSubScene.getName());

            // Reload list to update [IN SCENE] markers
            loadAllItems();

            // Reload items in parent editor
            parent.loadItemsIntoEditor();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to add item to scene: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR adding item to scene: " + e.getMessage());
        }
    }

    /**
     * Remove selected item from current SubScene
     */
    private void removeItemFromScene() {
        if (currentSubScene == null) {
            JOptionPane.showMessageDialog(this,
                "No SubScene is currently selected!",
                "No SubScene",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedItemName == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an item to remove!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String itemName = selectedItemName;

        // Find item in scene
        Item itemToRemove = currentSubScene.getItems().stream()
            .filter(item -> item.getName().equals(itemName))
            .findFirst()
            .orElse(null);

        if (itemToRemove == null) {
            JOptionPane.showMessageDialog(this,
                "Item '" + itemName + "' is not in this SubScene!",
                "Not in Scene",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // Remove from scene
            currentSubScene.getItems().remove(itemToRemove);

            // Save scene
            FileHandlingSimple.saveSubSceneToDefault(
                currentSubScene,
                parent.getSelectedSceneName()
            );

            parent.log("Removed item '" + itemName + "' from SubScene: " + currentSubScene.getName());

            // Reload list to update [IN SCENE] markers
            loadAllItems();

            // Reload items in parent editor
            parent.loadItemsIntoEditor();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to remove item from scene: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            parent.log("ERROR removing item from scene: " + e.getMessage());
        }
    }

    /**
     * Update the current SubScene (called from parent editor)
     */
    public void setCurrentSubScene(Scene subScene) {
        this.currentSubScene = subScene;
        loadAllItems(); // Reload to update [IN SCENE] markers
    }
}
