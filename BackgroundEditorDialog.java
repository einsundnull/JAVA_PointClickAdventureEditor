package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Dialog zum Bearbeiten eines einzelnen Background-Bildes (ConditionalImage)
 */
public class BackgroundEditorDialog extends JDialog {
    private ConditionalImage background;
    private Scene scene;

    private JTextField nameField;
    private JTextField imagePathField;
    private JLabel imagePreviewLabel;
    private JPanel conditionsPanel;
    private Map<String, JCheckBox> conditionCheckboxes;
    private Map<String, ButtonGroup> conditionRadioGroups;

    public BackgroundEditorDialog(JDialog parent, ConditionalImage background, Scene scene) {
        super(parent, "Background Editor - " + background.getName(), true);
        this.background = background;
        this.scene = scene;
        this.conditionCheckboxes = new LinkedHashMap<>();
        this.conditionRadioGroups = new LinkedHashMap<>();

        setSize(700, 600);
        setLocationRelativeTo(parent);

        initUI();
        loadBackgroundData();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Name field
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Name:"));
        nameField = new JTextField(background.getName(), 30);
        namePanel.add(nameField);
        namePanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(namePanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Image preview and drop zone
        JPanel imagePanel = new JPanel(new BorderLayout(10, 10));
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));
        imagePanel.setAlignmentX(LEFT_ALIGNMENT);
        imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        imagePreviewLabel = new JLabel("<html><center>Drag & Drop<br>Image Here</center></html>");
        imagePreviewLabel.setHorizontalAlignment(JLabel.CENTER);
        imagePreviewLabel.setVerticalAlignment(JLabel.CENTER);
        imagePreviewLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 255), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        imagePreviewLabel.setBackground(new Color(240, 240, 255));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setPreferredSize(new Dimension(180, 120));

        // Enable drag & drop
        imagePreviewLabel.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                handleImageDrop(evt);
            }
        });

        imagePanel.add(imagePreviewLabel, BorderLayout.CENTER);

        // Image path field and buttons
        JPanel imageControlPanel = new JPanel();
        imageControlPanel.setLayout(new BoxLayout(imageControlPanel, BoxLayout.Y_AXIS));

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(new JLabel("Image Path:"));
        imagePathField = new JTextField(background.getImagePath(), 25);
        pathPanel.add(imagePathField);

        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> browseImage());
        pathPanel.add(browseBtn);
        imageControlPanel.add(pathPanel);

        // Flip buttons
        JPanel flipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton flipHBtn = new JButton("⇄ Flip Horizontally");
        flipHBtn.addActionListener(e -> flipImage(true));
        flipPanel.add(flipHBtn);

        JButton flipVBtn = new JButton("⇅ Flip Vertically");
        flipVBtn.addActionListener(e -> flipImage(false));
        flipPanel.add(flipVBtn);
        imageControlPanel.add(flipPanel);

        imagePanel.add(imageControlPanel, BorderLayout.SOUTH);
        mainPanel.add(imagePanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Conditions section
        JPanel conditionsSection = new JPanel(new BorderLayout());
        conditionsSection.setBorder(BorderFactory.createTitledBorder("Conditions (show image if all match)"));
        conditionsSection.setAlignmentX(LEFT_ALIGNMENT);
        conditionsSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        JScrollPane conditionsScroll = new JScrollPane(conditionsPanel);
        conditionsScroll.setPreferredSize(new Dimension(0, 150));
        conditionsSection.add(conditionsScroll, BorderLayout.CENTER);

        JPanel condButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addCondBtn = new JButton("Add Condition");
        addCondBtn.addActionListener(e -> addCondition());
        condButtonPanel.add(addCondBtn);

        JButton removeCondBtn = new JButton("Remove Selected");
        removeCondBtn.addActionListener(e -> removeSelectedConditions());
        condButtonPanel.add(removeCondBtn);

        JButton manageCondBtn = new JButton("Manage Conditions");
        manageCondBtn.addActionListener(e -> openConditionManager());
        condButtonPanel.add(manageCondBtn);

        conditionsSection.add(condButtonPanel, BorderLayout.SOUTH);
        mainPanel.add(conditionsSection);

        JScrollPane mainScroll = new JScrollPane(mainPanel);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveBackground());
        bottomPanel.add(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        bottomPanel.add(cancelBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadBackgroundData() {
        nameField.setText(background.getName());
        imagePathField.setText(background.getImagePath());
        updateImagePreview();
        refreshConditions();
    }

    private void updateImagePreview() {
        try {
            String imagePath = background.getImagePath();
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                File imageFile = ResourcePathHelper.resolve("images/" + imagePath);
                if (imageFile.exists()) {
                    ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                    Image img = icon.getImage().getScaledInstance(180, 120, Image.SCALE_SMOOTH);
                    imagePreviewLabel.setIcon(new ImageIcon(img));
                    imagePreviewLabel.setText("");
                } else {
                    imagePreviewLabel.setIcon(null);
                    imagePreviewLabel.setText("Image not found");
                }
            } else {
                imagePreviewLabel.setIcon(null);
                imagePreviewLabel.setText("<html><center>Drag & Drop<br>Image Here</center></html>");
            }
        } catch (Exception e) {
            imagePreviewLabel.setIcon(null);
            imagePreviewLabel.setText("Error loading image");
        }
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
                    File imagesDir = ResourcePathHelper.resolve("images");
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs();
                    }

                    File targetFile = new File(imagesDir, file.getName());
                    if (!targetFile.exists() || !file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                        try {
                            java.nio.file.Files.copy(file.toPath(), targetFile.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception copyEx) {
                            System.err.println("ERROR copying image: " + copyEx.getMessage());
                        }
                    }

                    background.setImagePath(file.getName());
                    imagePathField.setText(file.getName());
                    updateImagePreview();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Please drop an image file (PNG, JPG, GIF)", "Invalid File",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") ||
               name.endsWith(".jpeg") || name.endsWith(".gif") ||
               name.endsWith(".bmp");
    }

    private void browseImage() {
        String filename = JOptionPane.showInputDialog(this,
                "Enter image filename (e.g., 'background1.png'):",
                imagePathField.getText());
        if (filename != null && !filename.trim().isEmpty()) {
            background.setImagePath(filename);
            imagePathField.setText(filename);
            updateImagePreview();
        }
    }

    private void flipImage(boolean horizontal) {
        String imagePath = background.getImagePath();
        if (imagePath == null || imagePath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No image selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            File imageFile = ResourcePathHelper.resolve("images/" + imagePath);
            if (!imageFile.exists()) {
                JOptionPane.showMessageDialog(this, "Image file not found: " + imageFile.getPath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(imageFile);
            int width = original.getWidth();
            int height = original.getHeight();

            java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(width, height,
                    original.getType());

            Graphics2D g2d = flipped.createGraphics();

            if (horizontal) {
                g2d.drawImage(original, width, 0, -width, height, null);
            } else {
                g2d.drawImage(original, 0, height, width, -height, null);
            }

            g2d.dispose();

            String format = imagePath.substring(imagePath.lastIndexOf('.') + 1);
            javax.imageio.ImageIO.write(flipped, format, imageFile);

            updateImagePreview();

            String direction = horizontal ? "horizontally" : "vertically";
            JOptionPane.showMessageDialog(this, "Image flipped " + direction + " successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error flipping image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshConditions() {
        conditionsPanel.removeAll();
        conditionCheckboxes.clear();
        conditionRadioGroups.clear();

        Map<String, Boolean> bgConditions = background.getConditions();

        if (bgConditions.isEmpty()) {
            JLabel noCondLabel = new JLabel("No conditions set (always show)");
            noCondLabel.setForeground(Color.GRAY);
            conditionsPanel.add(noCondLabel);
        } else {
            for (Map.Entry<String, Boolean> entry : bgConditions.entrySet()) {
                String condName = entry.getKey();
                boolean requiredValue = entry.getValue();

                JPanel condPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

                // Checkbox for selection (for removal)
                JCheckBox checkbox = new JCheckBox(condName);
                conditionCheckboxes.put(condName, checkbox);
                condPanel.add(checkbox);

                condPanel.add(new JLabel("="));

                // Radio buttons for true/false
                JRadioButton trueRadio = new JRadioButton("true", requiredValue);
                JRadioButton falseRadio = new JRadioButton("false", !requiredValue);

                ButtonGroup radioGroup = new ButtonGroup();
                radioGroup.add(trueRadio);
                radioGroup.add(falseRadio);
                conditionRadioGroups.put(condName, radioGroup);

                trueRadio.addActionListener(e -> background.addCondition(condName, true));
                falseRadio.addActionListener(e -> background.addCondition(condName, false));

                condPanel.add(trueRadio);
                condPanel.add(falseRadio);

                conditionsPanel.add(condPanel);
            }
        }

        conditionsPanel.revalidate();
        conditionsPanel.repaint();
    }

    private void addCondition() {
        Map<String, Boolean> allConditions = Conditions.getAllConditions();
        List<String> availableConditions = new ArrayList<>();

        for (String condName : allConditions.keySet()) {
            if (!condName.startsWith("isInInventory_") && !background.getConditions().containsKey(condName)) {
                availableConditions.add(condName);
            }
        }

        if (availableConditions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No more conditions available. Use 'Manage Conditions' to create new ones.",
                    "No Conditions", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String selected = (String) JOptionPane.showInputDialog(this,
                "Select condition to add:", "Add Condition",
                JOptionPane.PLAIN_MESSAGE, null,
                availableConditions.toArray(), availableConditions.get(0));

        if (selected != null) {
            background.addCondition(selected, true);
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
            background.removeCondition(condName);
        }

        refreshConditions();
    }

    private void openConditionManager() {
        ConditionsManagerDialog dialog = new ConditionsManagerDialog(
                (javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        // Refresh conditions if changed
        refreshConditions();
    }

    private void saveBackground() {
        // Update background from fields
        background.setName(nameField.getText().trim());
        background.setImagePath(imagePathField.getText().trim());

        // Save scene
        try {
            SceneSaver.saveSceneToDefault(scene);
            JOptionPane.showMessageDialog(this, "Background saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving background: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
