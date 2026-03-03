package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import main.ui.components.button.AppButton;
import main.ui.theme.Spacing;
// New UI imports
import main.ui.theme.ThemeManager;

/**
 * Dialog zum Editieren einer Scene mit Unterstützung für mehrere bedingte Bilder
 */
public class SceneEditorDialog extends JDialog {
    private EditorMain parent;
    private String sceneName;
    private JTextField sceneNameField;
    private JPanel imagesContainer;
    private List<ConditionalImagePanel> imagePanels;
    private Scene scene;

    public SceneEditorDialog(EditorMain parent, String sceneName) {
        super(parent, "Scene Editor Backgrounds - " + sceneName, true);
        this.parent = parent;
        this.sceneName = sceneName;
        this.imagePanels = new ArrayList<>();

        setSize(900, 700);
        setLocationRelativeTo(parent);

        initUI();
        loadSceneData();
    }

    private void initUI() {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();

        setLayout(new BorderLayout(Spacing.SM, Spacing.SM));

        // Title Panel with Theme Toggle
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(c.getBackgroundPanel());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.XS, Spacing.SM));

        JLabel titleLabel = new JLabel("Edit Scene: " + sceneName);
        titleLabel.setFont(t.semiboldLg());
        titleLabel.setForeground(c.getTextPrimary());
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // Theme Toggle Button
        AppButton themeToggleBtn = new AppButton("🌓", AppButton.Variant.GHOST, AppButton.Size.SMALL);
        themeToggleBtn.setToolTipText("Toggle Light/Dark Theme");
        themeToggleBtn.addActionListener(e -> {
            ThemeManager.toggleTheme();
            ThemeManager.updateAllWindows();
        });
        titlePanel.add(themeToggleBtn, BorderLayout.EAST);

        add(titlePanel, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(Spacing.SM, Spacing.SM));
        mainPanel.setBorder(new EmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM));
        mainPanel.setBackground(c.getBackgroundRoot());

        // Scene Name Panel
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.SM));
        namePanel.setBackground(c.getBackgroundRoot());
        JLabel nameLabel = new JLabel("Scene Name:");
        nameLabel.setFont(t.sm());
        nameLabel.setForeground(c.getTextSecondary());
        namePanel.add(nameLabel);
        sceneNameField = new JTextField(sceneName, 30);
        namePanel.add(sceneNameField);
        mainPanel.add(namePanel, BorderLayout.NORTH);

        // Images container with scroll
        imagesContainer = new JPanel();
        imagesContainer.setLayout(new BoxLayout(imagesContainer, BoxLayout.Y_AXIS));
        imagesContainer.setBackground(c.getBackgroundRoot());
        JScrollPane scrollPane = new JScrollPane(imagesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom control panel - Use AppButton
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, Spacing.SM, Spacing.SM));
        controlPanel.setBackground(c.getBackgroundElevated());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, c.getBorderDefault()),
            BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
        ));

        AppButton addImageBtn = new AppButton("Add new Image field", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
        addImageBtn.addActionListener(e -> addNewImageField());
        controlPanel.add(addImageBtn);

        AppButton manageConditionsBtn = new AppButton("Manage Conditions", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
        manageConditionsBtn.addActionListener(e -> openConditionManager());
        controlPanel.add(manageConditionsBtn);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Bottom panel with save/cancel - Use AppButton
        var c = ThemeManager.colors();
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, Spacing.SM));
        bottomPanel.setBackground(c.getBackgroundElevated());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, c.getBorderDefault()),
            BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
        ));

        AppButton saveBtn = new AppButton("Save Changes", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
        saveBtn.addActionListener(e -> saveChanges());
        bottomPanel.add(saveBtn);

        AppButton cancelBtn = new AppButton("Cancel", AppButton.Variant.GHOST, AppButton.Size.SMALL);
        cancelBtn.addActionListener(e -> dispose());
        bottomPanel.add(cancelBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadSceneData() {
        try {
            // Load scene from file
            scene = SceneLoader.loadScene(sceneName);

            // Load background images
            List<ConditionalImage> bgImages = scene.getBackgroundImages();

            if (bgImages.isEmpty()) {
                // Create a default image entry if none exist
                ConditionalImage defaultImg = new ConditionalImage("", "Default");
                addImagePanel(defaultImg);
            } else {
                for (ConditionalImage img : bgImages) {
                    addImagePanel(img);
                }
            }

        } catch (Exception e) {
            parent.log("Error loading scene data: " + e.getMessage());
            e.printStackTrace();
            // Add empty image panel
            addImagePanel(new ConditionalImage("", "Image 1"));
        }
    }

    private void addNewImageField() {
        ConditionalImage newImage = new ConditionalImage("", "Image " + (imagePanels.size() + 1));
        addImagePanel(newImage);
        revalidateImagesContainer();
    }

    private void addImagePanel(ConditionalImage image) {
        ConditionalImagePanel panel = new ConditionalImagePanel(image, this);
        imagePanels.add(panel);
        imagesContainer.add(panel);
        imagesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void removeImagePanel(ConditionalImagePanel panel) {
        imagePanels.remove(panel);

        // Clear all components and rebuild to avoid orphaned spacers
        imagesContainer.removeAll();
        for (ConditionalImagePanel p : imagePanels) {
            imagesContainer.add(p);
            imagesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        revalidateImagesContainer();
    }

    private void revalidateImagesContainer() {
        imagesContainer.revalidate();
        imagesContainer.repaint();
    }

    private void openConditionManager() {
        ConditionsManagerDialog dialog = new ConditionsManagerDialog((JFrame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        // Refresh all image panels if conditions changed
        if (dialog.changesMade()) {
            for (ConditionalImagePanel panel : imagePanels) {
                panel.refreshConditions();
            }
        }
    }

    private void saveChanges() {
        String newName = sceneNameField.getText().trim();

        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Scene name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean renamed = false;

            // Check if name changed
            if (!newName.equals(sceneName)) {
                // Check if new name already exists
                File newFile = new File("resources/scenes/" + newName + ".txt");
                if (newFile.exists()) {
                    JOptionPane.showMessageDialog(this, "A scene with name '" + newName + "' already exists!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Confirm rename
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Rename scene from '" + sceneName + "' to '" + newName + "'?\n\n" +
                        "This will update all references in .txt files.",
                        "Confirm Rename", JOptionPane.YES_NO_OPTION);

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Rename references in all .txt files
                parent.log("Renaming scene references from '" + sceneName + "' to '" + newName + "'...");
                List<String> modifiedFiles = SceneReferenceManager.renameSceneReferences(sceneName, newName);
                parent.log("Updated " + modifiedFiles.size() + " file(s)");

                // Rename the scene file itself
                if (SceneReferenceManager.renameSceneFile(sceneName, newName)) {
                    parent.log("Scene file renamed successfully");
                    sceneName = newName;
                    renamed = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to rename scene file!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Collect all background images from panels
            List<ConditionalImage> backgroundImages = new ArrayList<>();
            for (ConditionalImagePanel panel : imagePanels) {
                ConditionalImage img = panel.getConditionalImage();
                if (!img.getImagePath().trim().isEmpty()) {
                    backgroundImages.add(img);
                }
            }

            // Update scene with new images
            scene.setBackgroundImages(backgroundImages);

            // Save scene to file
            SceneSaver.saveScene(scene);
            parent.log("Scene saved with " + backgroundImages.size() + " background image(s)");

            // Reload scene in game
            parent.getGame().loadScene(sceneName);

            parent.log("✓ Scene saved successfully!");

            String message = "✓ Scene saved successfully!";
            if (renamed) {
                message += "\n\nScene renamed and all references updated.";
            }

            JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);

            // Don't auto-close - let user close manually

        } catch (Exception e) {
            parent.log("Error saving scene: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving scene: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Panel for a single conditional image with all its settings
     */
    private class ConditionalImagePanel extends JPanel {
        private ConditionalImage image;
        private SceneEditorDialog parent;
        private JLabel imagePreviewLabel;
        private JTextField imagePathField;
        private JTextField nameField;
        private JPanel conditionsListPanel;
        private Map<String, JCheckBox> conditionCheckboxes;

        public ConditionalImagePanel(ConditionalImage image, SceneEditorDialog parent) {
            this.image = image;
            this.parent = parent;
            this.conditionCheckboxes = new LinkedHashMap<>();

            initUI();
        }

        private void initUI() {
            setLayout(new BorderLayout(10, 10));

            // Create titled border with image name
            String panelTitle = "Scene Editor Image: " + image.getName();
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 2),
                    panelTitle,
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("Arial", Font.BOLD, 12)
                ),
                new EmptyBorder(10, 10, 10, 10)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 500)); // Increased for buttons at bottom

            // Top panel with image controls
            JPanel topPanel = new JPanel(new BorderLayout(5, 5));

            // Image preview and drop zone
            JPanel imagePanel = new JPanel(new BorderLayout());
            imagePanel.setPreferredSize(new Dimension(200, 150));

            imagePreviewLabel = new JLabel("<html><center>Drag & Drop<br>Image Here</center></html>");
            imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imagePreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
            imagePreviewLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 255), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            imagePreviewLabel.setBackground(new Color(240, 240, 255));
            imagePreviewLabel.setOpaque(true);
            imagePreviewLabel.setPreferredSize(new Dimension(200, 120));

            // Enable drag & drop
            imagePreviewLabel.setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent evt) {
                    handleImageDrop(evt);
                }
            });

            imagePanel.add(imagePreviewLabel, BorderLayout.CENTER);

            topPanel.add(imagePanel, BorderLayout.WEST);

            // Conditions panel (right side)
            JPanel conditionsPanel = new JPanel(new BorderLayout(5, 5));
            conditionsPanel.setBorder(BorderFactory.createTitledBorder("Conditions (show image if all match)"));

            // Conditions list
            conditionsListPanel = new JPanel();
            conditionsListPanel.setLayout(new BoxLayout(conditionsListPanel, BoxLayout.Y_AXIS));
            JScrollPane conditionsScroll = new JScrollPane(conditionsListPanel);
            conditionsScroll.setPreferredSize(new Dimension(300, 100));
            conditionsPanel.add(conditionsScroll, BorderLayout.CENTER);

            // Condition control buttons
            JPanel conditionControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addConditionBtn = new JButton("Add Condition");
            addConditionBtn.addActionListener(e -> addCondition());
            conditionControlPanel.add(addConditionBtn);

            JButton removeConditionBtn = new JButton("Remove Selected Condition");
            removeConditionBtn.addActionListener(e -> removeSelectedConditions());
            conditionControlPanel.add(removeConditionBtn);

            conditionsPanel.add(conditionControlPanel, BorderLayout.SOUTH);

            topPanel.add(conditionsPanel, BorderLayout.CENTER);

            add(topPanel, BorderLayout.CENTER);

            // Bottom: name and path
            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            namePanel.add(new JLabel("Name:"));
            nameField = new JTextField(image.getName(), 20);
            namePanel.add(nameField);
            bottomPanel.add(namePanel);

            JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            pathPanel.add(new JLabel("Image Path:"));
            imagePathField = new JTextField(image.getImagePath(), 30);
            imagePathField.setEditable(false);
            pathPanel.add(imagePathField);
            bottomPanel.add(pathPanel);

            // Image control buttons at bottom
            JPanel buttonControlPanel = new JPanel();
            buttonControlPanel.setLayout(new BoxLayout(buttonControlPanel, BoxLayout.Y_AXIS));

            // First row: Rename and Remove buttons
            JPanel topButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
            JButton renameBtn = new JButton("Rename");
            renameBtn.addActionListener(e -> renameImage());
            topButtonsPanel.add(renameBtn);

            JButton removeBtn = new JButton("Remove this Image field");
            removeBtn.addActionListener(e -> removeThisImage());
            topButtonsPanel.add(removeBtn);
            buttonControlPanel.add(topButtonsPanel);

            // Second row: Flip buttons
            JPanel flipButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
            JButton flipHBtn = new JButton("⇄ Flip H");
            flipHBtn.setToolTipText("Flip image horizontally");
            flipHBtn.addActionListener(e -> flipImage(true));
            flipButtonsPanel.add(flipHBtn);

            JButton flipVBtn = new JButton("⇅ Flip V");
            flipVBtn.setToolTipText("Flip image vertically");
            flipVBtn.addActionListener(e -> flipImage(false));
            flipButtonsPanel.add(flipVBtn);
            buttonControlPanel.add(flipButtonsPanel);

            bottomPanel.add(buttonControlPanel);

            add(bottomPanel, BorderLayout.SOUTH);

            // Load initial conditions
            refreshConditions();
            updateImagePreview();
        }

        private void handleImageDrop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                @SuppressWarnings("unchecked")
                List<File> droppedFiles = (List<File>) evt.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                if (!droppedFiles.isEmpty()) {
                    File file = droppedFiles.get(0);
                    if (isImageFile(file)) {
                        // Copy file to resources/images
                        File imagesDir = new File("resources/images");
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs();
                        }

                        File targetFile = new File(imagesDir, file.getName());
                        if (!targetFile.exists() || !file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                            try {
                                java.nio.file.Files.copy(file.toPath(), targetFile.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                parent.parent.log("Copied image to: " + targetFile.getPath());
                            } catch (Exception copyEx) {
                                parent.parent.log("ERROR copying image: " + copyEx.getMessage());
                            }
                        }

                        image.setImagePath(file.getName());
                        imagePathField.setText(file.getName());
                        updateImagePreview();
                        parent.parent.log("Image selected: " + file.getName());
                    } else {
                        JOptionPane.showMessageDialog(ConditionalImagePanel.this,
                                "Please drop an image file (PNG, JPG)", "Invalid File",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void browseForImage() {
            File imagesDir = new File("resources/images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String filename = JOptionPane.showInputDialog(this,
                    "Enter image filename (e.g., 'background.png'):",
                    imagePathField.getText());

            if (filename != null && !filename.trim().isEmpty()) {
                image.setImagePath(filename.trim());
                imagePathField.setText(filename.trim());
                updateImagePreview();
            }
        }

        private void renameImage() {
            String newName = JOptionPane.showInputDialog(this,
                    "Enter new name for this image:",
                    nameField.getText());

            if (newName != null && !newName.trim().isEmpty()) {
                image.setName(newName.trim());
                nameField.setText(newName.trim());

                // Update panel title
                updatePanelTitle();
            }
        }

        private void updatePanelTitle() {
            String panelTitle = "Scene Editor Image: " + image.getName();
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 2),
                    panelTitle,
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("Arial", Font.BOLD, 12)
                ),
                new EmptyBorder(10, 10, 10, 10)
            ));
            revalidate();
            repaint();
        }

        private void removeThisImage() {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove this image field?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                parent.removeImagePanel(this);
            }
        }

        private void flipImage(boolean horizontal) {
            String imagePath = image.getImagePath();
            if (imagePath == null || imagePath.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No image selected!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Load image from resources/images
                File imageFile = new File("resources/images/" + imagePath);
                if (!imageFile.exists()) {
                    JOptionPane.showMessageDialog(this,
                            "Image file not found: " + imageFile.getPath(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(imageFile);
                int width = original.getWidth();
                int height = original.getHeight();

                java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(
                        width, height, original.getType());

                Graphics2D g2d = flipped.createGraphics();

                if (horizontal) {
                    // Flip horizontally
                    g2d.drawImage(original, width, 0, -width, height, null);
                } else {
                    // Flip vertically
                    g2d.drawImage(original, 0, height, width, -height, null);
                }

                g2d.dispose();

                // Save back to same file
                String format = imagePath.substring(imagePath.lastIndexOf('.') + 1);
                javax.imageio.ImageIO.write(flipped, format, imageFile);

                // Update preview
                updateImagePreview();

                String direction = horizontal ? "horizontally" : "vertically";
                parent.parent.log("Image flipped " + direction + ": " + imagePath);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error flipping image: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        private void addCondition() {
            Map<String, Boolean> allConditions = Conditions.getAllConditions();
            List<String> availableConditions = new ArrayList<>();

            for (String condName : allConditions.keySet()) {
                if (!condName.startsWith("isInInventory_") && !conditionCheckboxes.containsKey(condName)) {
                    availableConditions.add(condName);
                }
            }

            if (availableConditions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No more conditions available. Use 'Manage Conditions' to create new ones.",
                        "No Conditions",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select condition to add:",
                    "Add Condition",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    availableConditions.toArray(),
                    availableConditions.get(0));

            if (selected != null) {
                image.addCondition(selected, true);
                refreshConditions();
            }
        }

        private void removeSelectedConditions() {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : conditionCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String condName : toRemove) {
                image.removeCondition(condName);
            }

            refreshConditions();
        }

        private void refreshConditions() {
            conditionsListPanel.removeAll();
            conditionCheckboxes.clear();

            Map<String, Boolean> imageConditions = image.getConditions();

            if (imageConditions.isEmpty()) {
                JLabel noCondLabel = new JLabel("No conditions set (always show)");
                noCondLabel.setForeground(Color.GRAY);
                conditionsListPanel.add(noCondLabel);
            } else {
                for (Map.Entry<String, Boolean> entry : imageConditions.entrySet()) {
                    String condName = entry.getKey();
                    boolean requiredValue = entry.getValue();

                    JPanel condPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

                    // Checkbox for selection (for removal)
                    JCheckBox checkbox = new JCheckBox(condName);
                    conditionCheckboxes.put(condName, checkbox);
                    condPanel.add(checkbox);

                    // Label "show Image if"
                    condPanel.add(new JLabel("="));

                    // Radio buttons for true/false
                    JRadioButton trueRadio = new JRadioButton("true", requiredValue);
                    JRadioButton falseRadio = new JRadioButton("false", !requiredValue);

                    ButtonGroup radioGroup = new ButtonGroup();
                    radioGroup.add(trueRadio);
                    radioGroup.add(falseRadio);

                    trueRadio.addActionListener(e -> {
                        image.addCondition(condName, true);
                    });

                    falseRadio.addActionListener(e -> {
                        image.addCondition(condName, false);
                    });

                    condPanel.add(trueRadio);
                    condPanel.add(falseRadio);

                    conditionsListPanel.add(condPanel);
                }
            }

            conditionsListPanel.revalidate();
            conditionsListPanel.repaint();
        }

        private void updateImagePreview() {
            try {
                String imagePath = image.getImagePath();
                if (imagePath != null && !imagePath.trim().isEmpty()) {
                    File imageFile = new File("resources/images/" + imagePath);
                    if (imageFile.exists()) {
                        ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                        Image img = icon.getImage().getScaledInstance(180, 100, Image.SCALE_SMOOTH);
                        imagePreviewLabel.setIcon(new ImageIcon(img));
                        imagePreviewLabel.setText("");
                    } else {
                        imagePreviewLabel.setIcon(null);
                        imagePreviewLabel.setText("Not found");
                    }
                } else {
                    imagePreviewLabel.setIcon(null);
                    imagePreviewLabel.setText("<html><center>Drag & Drop<br>Image Here</center></html>");
                }
            } catch (Exception e) {
                imagePreviewLabel.setIcon(null);
                imagePreviewLabel.setText("Error");
            }
        }

        private boolean isImageFile(File file) {
            String name = file.getName().toLowerCase();
            return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
        }

        public ConditionalImage getConditionalImage() {
            // Update image from UI fields
            image.setName(nameField.getText().trim());
            // Condition values are already updated via radio button listeners
            return image;
        }
    }
}
