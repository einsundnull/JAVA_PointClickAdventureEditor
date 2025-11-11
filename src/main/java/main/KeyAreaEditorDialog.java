package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Consolidated KeyArea Editor Dialog
 * Combines functionality from:
 * - Hover Editor
 * - Points Manager
 * - Rename
 * - Type selection
 */
public class KeyAreaEditorDialog extends JDialog {
    private KeyArea keyArea;
    private Scene scene;
    private EditorMain parent;

    // UI Components
    private JTextField nameField;
    private JComboBox<KeyArea.Type> typeComboBox;
    private JTextArea hoverTextArea;
    private JButton editPointsButton;

    public KeyAreaEditorDialog(EditorMain parent, KeyArea keyArea, Scene scene) {
        super(parent, "KeyArea Editor - " + keyArea.getName(), true);
        this.parent = parent;
        this.keyArea = keyArea;
        this.scene = scene;

        setSize(500, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        initUI();
    }

    private void initUI() {
        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== NAME SECTION =====
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBorder(BorderFactory.createTitledBorder("Name"));
        nameField = new JTextField(keyArea.getName());
        namePanel.add(nameField, BorderLayout.CENTER);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        contentPanel.add(namePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // ===== TYPE SECTION =====
        JPanel typePanel = new JPanel(new BorderLayout());
        typePanel.setBorder(BorderFactory.createTitledBorder("Type"));
        typeComboBox = new JComboBox<>(KeyArea.Type.values());
        typeComboBox.setSelectedItem(keyArea.getType());
        typeComboBox.addActionListener(e -> showTypeInfo());
        typePanel.add(typeComboBox, BorderLayout.CENTER);
        typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        contentPanel.add(typePanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // ===== HOVER TEXT SECTION =====
        JPanel hoverPanel = new JPanel(new BorderLayout());
        hoverPanel.setBorder(BorderFactory.createTitledBorder("Hover Text (Simple Mode)"));
        hoverTextArea = new JTextArea(3, 40);
        // Get current hover text (uses "none" condition by default)
        String currentHoverText = keyArea.getHoverDisplayText();
        hoverTextArea.setText(currentHoverText != null ? currentHoverText : "");
        hoverTextArea.setLineWrap(true);
        hoverTextArea.setWrapStyleWord(true);
        JScrollPane hoverScroll = new JScrollPane(hoverTextArea);
        hoverPanel.add(hoverScroll, BorderLayout.CENTER);

        // Add button for advanced hover editor
        JButton advancedHoverBtn = new JButton("Advanced Hover Editor");
        advancedHoverBtn.addActionListener(e -> openAdvancedHoverEditor());
        hoverPanel.add(advancedHoverBtn, BorderLayout.SOUTH);

        hoverPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        contentPanel.add(hoverPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // ===== POINTS SECTION =====
        JPanel pointsPanel = new JPanel(new BorderLayout());
        pointsPanel.setBorder(BorderFactory.createTitledBorder("Click Area Points"));

        JLabel pointsLabel = new JLabel(keyArea.getPoints().size() + " points defined");
        pointsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pointsPanel.add(pointsLabel, BorderLayout.CENTER);

        editPointsButton = new JButton("Edit Points");
        editPointsButton.addActionListener(e -> openPointsEditor());
        pointsPanel.add(editPointsButton, BorderLayout.SOUTH);

        pointsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        contentPanel.add(pointsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // ===== INFO PANEL =====
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Type Information"));
        JTextArea infoArea = new JTextArea(4, 40);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText(getTypeInfoText(keyArea.getType()));
        infoPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        contentPanel.add(infoPanel);

        // Store reference to info area for updates
        typeComboBox.addActionListener(e -> {
            KeyArea.Type selected = (KeyArea.Type) typeComboBox.getSelectedItem();
            infoArea.setText(getTypeInfoText(selected));
        });

        add(contentPanel, BorderLayout.CENTER);

        // ===== BUTTON PANEL =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAndClose());
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private String getTypeInfoText(KeyArea.Type type) {
        switch (type) {
            case TRANSITION:
                return "TRANSITION: Use Actions → LoadScene to define which scene to load when clicked.";
            case INTERACTION:
                return "INTERACTION: Use Actions to define dialogs or other interactions.";
            case MOVEMENT_BOUNDS:
                return "MOVEMENT_BOUNDS: Defines the walkable area for character movement.";
            case CHARACTER_RANGE:
                return "CHARACTER_RANGE: Defines movement area for NPCs/characters.";
            default:
                return "Select a type for this KeyArea.";
        }
    }

    private void showTypeInfo() {
        KeyArea.Type selected = (KeyArea.Type) typeComboBox.getSelectedItem();
        if (selected != null) {
            // Info is already displayed in the info panel
        }
    }

    private void openPointsEditor() {
        UniversalPointEditorDialog pointEditor = new UniversalPointEditorDialog(parent, keyArea);
        pointEditor.setVisible(true);

        // Update points count after editing
        editPointsButton.setText("Edit Points (" + keyArea.getPoints().size() + ")");
    }

    private void openAdvancedHoverEditor() {
        HoverEditorDialog hoverEditor = new HoverEditorDialog(parent, keyArea);
        hoverEditor.setVisible(true);

        // Refresh the simple hover text display after editing
        String currentHoverText = keyArea.getHoverDisplayText();
        hoverTextArea.setText(currentHoverText != null ? currentHoverText : "");
    }

    private void saveAndClose() {
        try {
            // Validate name
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Name cannot be empty!",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if name already exists (for other KeyAreas)
            String oldName = keyArea.getName();
            if (!newName.equals(oldName)) {
                for (KeyArea area : scene.getKeyAreas()) {
                    if (area != keyArea && area.getName().equals(newName)) {
                        JOptionPane.showMessageDialog(this,
                            "A KeyArea with name '" + newName + "' already exists!",
                            "Name Conflict",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Apply changes
            keyArea.setName(newName);
            keyArea.setType((KeyArea.Type) typeComboBox.getSelectedItem());

            // Update hover text (simplified mode - uses "none" condition)
            String hoverText = hoverTextArea.getText().trim();
            keyArea.getHoverDisplayConditions().clear(); // Clear existing conditions
            if (!hoverText.isEmpty()) {
                keyArea.addHoverDisplayCondition("none", hoverText);
            }

            // Save scene to DEFAULT file
            SceneSaver.saveSceneToDefault(scene);

            parent.log("✓ KeyArea updated: " + newName);

            JOptionPane.showMessageDialog(this,
                "KeyArea saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving KeyArea: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
