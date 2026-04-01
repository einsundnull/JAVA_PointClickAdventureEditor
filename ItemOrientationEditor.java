package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Editor for setting orientation-based images for items.
 * Allows setting different images based on cursor position relative to item.
 */
public class ItemOrientationEditor extends JFrame {

    private AdventureGame game;
    private EditorMainSimple parent;

    // UI Components
    private JComboBox<ItemWrapper> itemComboBox;
    private JPanel imageGridPanel;

    // Image drop panels (z + a-i)
    private ImageDropPanel defaultImagePanel;  // z - fallback
    private ImageDropPanel topLeftPanel;       // a
    private ImageDropPanel topPanel;           // b
    private ImageDropPanel topRightPanel;      // c
    private ImageDropPanel leftPanel;          // d
    private ImageDropPanel middlePanel;        // e
    private ImageDropPanel rightPanel;         // f
    private ImageDropPanel bottomLeftPanel;    // g
    private ImageDropPanel bottomPanel;        // h
    private ImageDropPanel bottomRightPanel;   // i

    private Item currentItem;

    /**
     * Wrapper class for ComboBox to display item name with thumbnail
     */
    private static class ItemWrapper {
        Item item;

        ItemWrapper(Item item) {
            this.item = item;
        }

        @Override
        public String toString() {
            return item.getName();
        }
    }

    public ItemOrientationEditor(EditorMainSimple parent) {
        this.parent = parent;
        this.game = parent.getGame();

        setTitle("Item Orientation Editor");
        setSize(800, 700);
        setLocationRelativeTo(parent);

        initUI();
        loadItems();

        // Add ComboBox listener AFTER UI is fully initialized and items are loaded
        itemComboBox.addActionListener(e -> onItemSelected());

        // Load the first item's images if available
        if (itemComboBox.getItemCount() > 0) {
            onItemSelected();
        }

        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top selection panel: Item ComboBox
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        headerPanel.add(new JLabel("Select Item:"));

        itemComboBox = new JComboBox<>();
        itemComboBox.setPreferredSize(new Dimension(250, 30));
        // ActionListener will be added after UI is fully initialized
        headerPanel.add(itemComboBox);

        add(headerPanel, BorderLayout.NORTH);

        // Center panel: Image grid (3x3 + 1 default)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Default image at top
        JPanel defaultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        defaultPanel.setBorder(BorderFactory.createTitledBorder("Default/Fallback Image (z)"));
        defaultImagePanel = new ImageDropPanel("Default", this::onImageDropped);
        defaultPanel.add(defaultImagePanel);
        centerPanel.add(defaultPanel, BorderLayout.NORTH);

        // 3x3 Grid for orientation images
        imageGridPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        imageGridPanel.setBorder(BorderFactory.createTitledBorder("Orientation Images"));

        // Row 1: TopLeft, Top, TopRight
        topLeftPanel = new ImageDropPanel("a - TopLeft", this::onImageDropped);
        topPanel = new ImageDropPanel("b - Top", this::onImageDropped);
        topRightPanel = new ImageDropPanel("c - TopRight", this::onImageDropped);

        imageGridPanel.add(topLeftPanel);
        imageGridPanel.add(topPanel);
        imageGridPanel.add(topRightPanel);

        // Row 2: Left, Middle, Right
        leftPanel = new ImageDropPanel("d - Left", this::onImageDropped);
        middlePanel = new ImageDropPanel("e - Middle", this::onImageDropped);
        rightPanel = new ImageDropPanel("f - Right", this::onImageDropped);

        imageGridPanel.add(leftPanel);
        imageGridPanel.add(middlePanel);
        imageGridPanel.add(rightPanel);

        // Row 3: BottomLeft, Bottom, BottomRight
        bottomLeftPanel = new ImageDropPanel("g - BottomLeft", this::onImageDropped);
        bottomPanel = new ImageDropPanel("h - Bottom", this::onImageDropped);
        bottomRightPanel = new ImageDropPanel("i - BottomRight", this::onImageDropped);

        imageGridPanel.add(bottomLeftPanel);
        imageGridPanel.add(bottomPanel);
        imageGridPanel.add(bottomRightPanel);

        centerPanel.add(imageGridPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton openResourcesBtn = new JButton("Open Resources");
        openResourcesBtn.addActionListener(e -> openResourcesFolder());
        buttonPanel.add(openResourcesBtn);

        JButton clearAllBtn = new JButton("Clear All");
        clearAllBtn.addActionListener(e -> clearAllImages());
        buttonPanel.add(clearAllBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveCurrentItem());
        buttonPanel.add(saveBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Load all items from current scene into ComboBox
     */
    private void loadItems() {
        itemComboBox.removeAllItems();

        Scene currentScene = game.getCurrentScene();
        if (currentScene == null || currentScene.getItems() == null) {
            return;
        }

        for (Item item : currentScene.getItems()) {
            itemComboBox.addItem(new ItemWrapper(item));
        }

        if (itemComboBox.getItemCount() > 0) {
            itemComboBox.setSelectedIndex(0);
        }
    }

    /**
     * Called when user selects an item from ComboBox
     */
    private void onItemSelected() {
        ItemWrapper selected = (ItemWrapper) itemComboBox.getSelectedItem();
        if (selected == null) return;

        currentItem = selected.item;
        loadItemImages();
    }

    /**
     * Load current item's images into drop panels
     */
    private void loadItemImages() {
        if (currentItem == null) return;

        // Default image
        defaultImagePanel.setImagePath(currentItem.getImageFilePath());

        // Orientation images
        topLeftPanel.setImagePath(currentItem.getImagePathTopLeft());
        topPanel.setImagePath(currentItem.getImagePathTop());
        topRightPanel.setImagePath(currentItem.getImagePathTopRight());
        leftPanel.setImagePath(currentItem.getImagePathLeft());
        middlePanel.setImagePath(currentItem.getImagePathMiddle());
        rightPanel.setImagePath(currentItem.getImagePathRight());
        bottomLeftPanel.setImagePath(currentItem.getImagePathBottomLeft());
        bottomPanel.setImagePath(currentItem.getImagePathBottom());
        bottomRightPanel.setImagePath(currentItem.getImagePathBottomRight());
    }

    /**
     * Called when user drops an image on a panel
     */
    private void onImageDropped(ImageDropPanel panel, File imageFile) {
        if (currentItem == null) return;

        try {
            // Determine target directory and filename
            String targetDir;
            String targetFilename;

            if (panel == defaultImagePanel) {
                // Default image goes to resources/images/items/
                targetDir = ResourcePathHelper.resolvePath("images/items");
                targetFilename = currentItem.getName() + getFileExtension(imageFile);
            } else {
                // Orientation images go to resources/images/itemsOrientation/
                targetDir = ResourcePathHelper.resolvePath("images/itemsOrientation");

                // Determine orientation suffix
                String suffix = "";
                if (panel == topLeftPanel) suffix = "_topLeft";
                else if (panel == topPanel) suffix = "_top";
                else if (panel == topRightPanel) suffix = "_topRight";
                else if (panel == leftPanel) suffix = "_left";
                else if (panel == middlePanel) suffix = "_middle";
                else if (panel == rightPanel) suffix = "_right";
                else if (panel == bottomLeftPanel) suffix = "_bottomLeft";
                else if (panel == bottomPanel) suffix = "_bottom";
                else if (panel == bottomRightPanel) suffix = "_bottomRight";

                targetFilename = currentItem.getName() + suffix + getFileExtension(imageFile);
            }

            // Create target directory if it doesn't exist
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
                parent.log("Created directory: " + targetDir);
            }

            // Copy image to target location
            File targetFile = new File(targetDirFile, targetFilename);
            copyFile(imageFile, targetFile);

            // Get relative path for storage
            String relativePath = targetDir + "/" + targetFilename;

            // Update item based on which panel received the drop
            if (panel == defaultImagePanel) {
                currentItem.setImageFilePath(relativePath);
            } else if (panel == topLeftPanel) {
                currentItem.setImagePathTopLeft(relativePath);
            } else if (panel == topPanel) {
                currentItem.setImagePathTop(relativePath);
            } else if (panel == topRightPanel) {
                currentItem.setImagePathTopRight(relativePath);
            } else if (panel == leftPanel) {
                currentItem.setImagePathLeft(relativePath);
            } else if (panel == middlePanel) {
                currentItem.setImagePathMiddle(relativePath);
            } else if (panel == rightPanel) {
                currentItem.setImagePathRight(relativePath);
            } else if (panel == bottomLeftPanel) {
                currentItem.setImagePathBottomLeft(relativePath);
            } else if (panel == bottomPanel) {
                currentItem.setImagePathBottom(relativePath);
            } else if (panel == bottomRightPanel) {
                currentItem.setImagePathBottomRight(relativePath);
            }

            // Update panel display
            panel.setImagePath(relativePath);

            // Auto-save
            saveCurrentItem();

            parent.log("✓ Copied to: " + relativePath);
        } catch (Exception e) {
            parent.log("ERROR copying image: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Failed to copy image: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get file extension including the dot
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot);
        }
        return ".png"; // Default extension
    }

    /**
     * Copy file from source to destination
     */
    private void copyFile(File source, File dest) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(source);
        java.io.OutputStream out = new java.io.FileOutputStream(dest);

        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }

        in.close();
        out.close();
    }

    /**
     * Save current item to file
     */
    private void saveCurrentItem() {
        if (currentItem == null) return;

        try {
            ItemSaver.saveItemByName(currentItem);
            game.repaint();
            parent.log("✓ Saved orientation images for: " + currentItem.getName());
        } catch (Exception e) {
            parent.log("ERROR saving item: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Failed to save item: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Clear all orientation images
     */
    private void clearAllImages() {
        if (currentItem == null) return;

        int result = JOptionPane.showConfirmDialog(this,
            "Clear all orientation images for " + currentItem.getName() + "?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION);

        if (result != JOptionPane.YES_OPTION) return;

        // Clear all orientation paths (keep default)
        currentItem.setImagePathTopLeft("");
        currentItem.setImagePathTop("");
        currentItem.setImagePathTopRight("");
        currentItem.setImagePathLeft("");
        currentItem.setImagePathMiddle("");
        currentItem.setImagePathRight("");
        currentItem.setImagePathBottomLeft("");
        currentItem.setImagePathBottom("");
        currentItem.setImagePathBottomRight("");

        // Reload display
        loadItemImages();

        // Save
        saveCurrentItem();

        parent.log("Cleared orientation images for: " + currentItem.getName());
    }

    /**
     * Open resources folder in file explorer
     */
    private void openResourcesFolder() {
        try {
            File resourcesDir = ResourcePathHelper.resolve("images/itemsOrientation");
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(resourcesDir);
            }
        } catch (Exception e) {
            parent.log("Could not open resources folder: " + e.getMessage());
        }
    }

    /**
     * Panel for dropping and displaying images
     */
    private static class ImageDropPanel extends JPanel {
        private String label;
        private String imagePath;
        private JLabel imageLabel;
        private JLabel textLabel;
        private ImageDropCallback callback;

        interface ImageDropCallback {
            void onDrop(ImageDropPanel panel, File file);
        }

        public ImageDropPanel(String label, ImageDropCallback callback) {
            this.label = label;
            this.callback = callback;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 150));
            setBorder(BorderFactory.createTitledBorder(label));
            setBackground(new Color(240, 240, 240));

            // Text label for filename
            textLabel = new JLabel("Drop image here", SwingConstants.CENTER);
            textLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            add(textLabel, BorderLayout.SOUTH);

            // Image display
            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            add(imageLabel, BorderLayout.CENTER);

            // Setup drag and drop
            setupDropTarget();
        }

        private void setupDropTarget() {
            new DropTarget(this, new DropTargetAdapter() {
                @Override
                public void drop(DropTargetDropEvent dtde) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);

                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                        if (!files.isEmpty()) {
                            File file = files.get(0);
                            if (isImageFile(file)) {
                                callback.onDrop(ImageDropPanel.this, file);
                            }
                        }

                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        dtde.dropComplete(false);
                    }
                }

                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    setBackground(new Color(200, 220, 255));
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                    setBackground(new Color(240, 240, 240));
                }
            });
        }

        private boolean isImageFile(File file) {
            String name = file.getName().toLowerCase();
            return name.endsWith(".png") || name.endsWith(".jpg") ||
                   name.endsWith(".jpeg") || name.endsWith(".gif");
        }

        public void setImagePath(String path) {
            this.imagePath = path;

            if (path == null || path.isEmpty()) {
                imageLabel.setIcon(null);
                textLabel.setText("Drop image here");
                return;
            }

            // Load and display thumbnail
            try {
                File imageFile = ResourcePathHelper.findImageFile(path);
                if (imageFile == null) {
                    imageFile = new File(path);
                }

                if (imageFile.exists()) {
                    ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                    Image scaled = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaled));
                    textLabel.setText(imageFile.getName());
                } else {
                    imageLabel.setIcon(null);
                    textLabel.setText("Image not found");
                }
            } catch (Exception e) {
                imageLabel.setIcon(null);
                textLabel.setText("Error loading");
            }
        }

        public String getLabel() {
            return label;
        }
    }
}
