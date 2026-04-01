package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

/**
 * Hybrid Scene Editor - Combines visual overview with powerful editing capabilities
 * Implements the best of both old (functional) and new (UX) design
 *
 * Structure:
 * - Scene Name Input
 * - Backgrounds Section (collapseable) -> List with Edit/Delete
 * - Items Section (collapseable) -> List with Edit Details/Edit Actions/Delete
 * - KeyAreas Section (collapseable) -> List with Edit/Edit Points/Edit Actions/Delete
 * - Dialogs Section (collapseable) -> List with Edit/Delete
 * - Bottom: [Manage Conditions] [Save] [Cancel]
 */
public class SceneEditorDialogHybrid extends JDialog {
    private Scene scene;
    private EditorMain editorMain;
    private AdventureGame game;

    // UI Components
    private JTextField sceneNameField;

    // Backgrounds Section
    private JPanel backgroundsContentPanel;
    private DefaultListModel<ConditionalImage> backgroundsListModel;
    private JList<ConditionalImage> backgroundsList;

    // Items Section
    private JPanel itemsContentPanel;
    private DefaultListModel<Item> itemsListModel;
    private JList<Item> itemsList;

    // KeyAreas Section
    private JPanel keyAreasContentPanel;
    private DefaultListModel<KeyArea> keyAreasListModel;
    private JList<KeyArea> keyAreasList;

    // Dialogs Section
    private JPanel dialogsContentPanel;
    private DefaultListModel<String> dialogsListModel;
    private JList<String> dialogsList;

    public SceneEditorDialogHybrid(JFrame parent, Scene scene, AdventureGame game, EditorMain editorMain) {
        super(parent, "Scene Editor: " + scene.getName(), false); // Non-modal to allow access to other editors
        this.scene = scene;
        this.game = game;
        this.editorMain = editorMain;

        setSize(800, 900);
        setLocationRelativeTo(parent);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Scene Name Field
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        namePanel.add(new JLabel("Scene Name:"));
        sceneNameField = new JTextField(scene.getName(), 30);
        namePanel.add(sceneNameField);
        mainPanel.add(namePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Backgrounds Section
        mainPanel.add(createBackgroundsSection());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Items Section
        mainPanel.add(createItemsSection());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // KeyAreas Section
        mainPanel.add(createKeyAreasSection());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Dialogs Section
        mainPanel.add(createDialogsSection());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Bottom Buttons
        mainPanel.add(createBottomButtons());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createBackgroundsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        panel.setBorder(BorderFactory.createTitledBorder("Backgrounds"));

        // Toggle button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleBtn = new JButton("▼");
        toggleBtn.setPreferredSize(new Dimension(40, 25));
        headerPanel.add(toggleBtn);

        backgroundsContentPanel = new JPanel(new BorderLayout());

        // List
        backgroundsListModel = new DefaultListModel<>();
        backgroundsList = new JList<>(backgroundsListModel);
        backgroundsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        backgroundsList.setCellRenderer(new BackgroundCellRenderer());

        // Load backgrounds
        if (scene.getBackgroundImages() != null) {
            for (ConditionalImage img : scene.getBackgroundImages()) {
                backgroundsListModel.addElement(img);
            }
        }

        JScrollPane listScroll = new JScrollPane(backgroundsList);
        listScroll.setPreferredSize(new Dimension(0, 150));
        backgroundsContentPanel.add(listScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        JButton addBtn = new JButton("Add");
        addBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        addBtn.setPreferredSize(new Dimension(70, 28));
        addBtn.addActionListener(e -> addBackground());
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        editBtn.setPreferredSize(new Dimension(70, 28));
        editBtn.addActionListener(e -> editBackground());
        buttonPanel.add(editBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        deleteBtn.setPreferredSize(new Dimension(75, 28));
        deleteBtn.addActionListener(e -> deleteBackground());
        buttonPanel.add(deleteBtn);

        backgroundsContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(backgroundsContentPanel, BorderLayout.CENTER);

        // Toggle collapse/expand
        toggleBtn.addActionListener(e -> {
            boolean isVisible = backgroundsContentPanel.isVisible();
            backgroundsContentPanel.setVisible(!isVisible);
            toggleBtn.setText(isVisible ? "►" : "▼");
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 250));
            panel.revalidate();
            panel.repaint();
        });

        return panel;
    }

    private JPanel createItemsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        panel.setBorder(BorderFactory.createTitledBorder("Items"));

        // Toggle button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleBtn = new JButton("▼");
        toggleBtn.setPreferredSize(new Dimension(40, 25));
        headerPanel.add(toggleBtn);

        itemsContentPanel = new JPanel(new BorderLayout());

        // List
        itemsListModel = new DefaultListModel<>();
        itemsList = new JList<>(itemsListModel);
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemsList.setCellRenderer(new ItemCellRenderer());

        // Load items
        if (scene.getItems() != null) {
            for (Item item : scene.getItems()) {
                itemsListModel.addElement(item);
            }
        }

        JScrollPane listScroll = new JScrollPane(itemsList);
        listScroll.setPreferredSize(new Dimension(0, 150));
        itemsContentPanel.add(listScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        JButton addBtn = new JButton("Add");
        addBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        addBtn.setPreferredSize(new Dimension(70, 28));
        addBtn.addActionListener(e -> addItem());
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Edit Details");
        editBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        editBtn.setPreferredSize(new Dimension(95, 28));
        editBtn.addActionListener(e -> editItem());
        buttonPanel.add(editBtn);

        JButton actionsBtn = new JButton("Actions");
        actionsBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        actionsBtn.setPreferredSize(new Dimension(75, 28));
        actionsBtn.addActionListener(e -> editItemActions());
        buttonPanel.add(actionsBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        deleteBtn.setPreferredSize(new Dimension(75, 28));
        deleteBtn.addActionListener(e -> deleteItem());
        buttonPanel.add(deleteBtn);

        itemsContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(itemsContentPanel, BorderLayout.CENTER);

        // Toggle collapse/expand
        toggleBtn.addActionListener(e -> {
            boolean isVisible = itemsContentPanel.isVisible();
            itemsContentPanel.setVisible(!isVisible);
            toggleBtn.setText(isVisible ? "►" : "▼");
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 250));
            panel.revalidate();
            panel.repaint();
        });

        return panel;
    }

    private JPanel createKeyAreasSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        panel.setBorder(BorderFactory.createTitledBorder("KeyAreas"));

        // Toggle button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleBtn = new JButton("▼");
        toggleBtn.setPreferredSize(new Dimension(40, 25));
        headerPanel.add(toggleBtn);

        keyAreasContentPanel = new JPanel(new BorderLayout());

        // List
        keyAreasListModel = new DefaultListModel<>();
        keyAreasList = new JList<>(keyAreasListModel);
        keyAreasList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyAreasList.setCellRenderer(new KeyAreaCellRenderer());

        // Load keyareas
        if (scene.getKeyAreas() != null) {
            for (KeyArea area : scene.getKeyAreas()) {
                keyAreasListModel.addElement(area);
            }
        }

        JScrollPane listScroll = new JScrollPane(keyAreasList);
        listScroll.setPreferredSize(new Dimension(0, 150));
        keyAreasContentPanel.add(listScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        JButton addBtn = new JButton("Add");
        addBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        addBtn.setPreferredSize(new Dimension(70, 28));
        addBtn.addActionListener(e -> addKeyArea());
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        editBtn.setPreferredSize(new Dimension(70, 28));
        editBtn.addActionListener(e -> editKeyArea());
        buttonPanel.add(editBtn);

        JButton pointsBtn = new JButton("Points");
        pointsBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        pointsBtn.setPreferredSize(new Dimension(70, 28));
        pointsBtn.addActionListener(e -> editKeyAreaPoints());
        buttonPanel.add(pointsBtn);

        JButton actionsBtn = new JButton("Actions");
        actionsBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        actionsBtn.setPreferredSize(new Dimension(75, 28));
        actionsBtn.addActionListener(e -> editKeyAreaActions());
        buttonPanel.add(actionsBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        deleteBtn.setPreferredSize(new Dimension(75, 28));
        deleteBtn.addActionListener(e -> deleteKeyArea());
        buttonPanel.add(deleteBtn);

        keyAreasContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(keyAreasContentPanel, BorderLayout.CENTER);

        // Toggle collapse/expand
        toggleBtn.addActionListener(e -> {
            boolean isVisible = keyAreasContentPanel.isVisible();
            keyAreasContentPanel.setVisible(!isVisible);
            toggleBtn.setText(isVisible ? "►" : "▼");
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 250));
            panel.revalidate();
            panel.repaint();
        });

        return panel;
    }

    private JPanel createDialogsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        panel.setBorder(BorderFactory.createTitledBorder("Dialogs"));

        // Toggle button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleBtn = new JButton("▼");
        toggleBtn.setPreferredSize(new Dimension(40, 25));
        headerPanel.add(toggleBtn);

        dialogsContentPanel = new JPanel(new BorderLayout());

        // List
        dialogsListModel = new DefaultListModel<>();
        dialogsList = new JList<>(dialogsListModel);
        dialogsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Load dialogs
        if (scene.getDialogs() != null) {
            for (String dialogName : scene.getDialogs().keySet()) {
                dialogsListModel.addElement(dialogName);
            }
        }

        JScrollPane listScroll = new JScrollPane(dialogsList);
        listScroll.setPreferredSize(new Dimension(0, 100));
        dialogsContentPanel.add(listScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

        JButton addBtn = new JButton("Add");
        addBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        addBtn.setPreferredSize(new Dimension(70, 28));
        addBtn.addActionListener(e -> addDialog());
        buttonPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        editBtn.setPreferredSize(new Dimension(70, 28));
        editBtn.addActionListener(e -> editDialog());
        buttonPanel.add(editBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        deleteBtn.setPreferredSize(new Dimension(75, 28));
        deleteBtn.addActionListener(e -> deleteDialog());
        buttonPanel.add(deleteBtn);

        dialogsContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(dialogsContentPanel, BorderLayout.CENTER);

        // Toggle collapse/expand
        toggleBtn.addActionListener(e -> {
            boolean isVisible = dialogsContentPanel.isVisible();
            dialogsContentPanel.setVisible(!isVisible);
            toggleBtn.setText(isVisible ? "►" : "▼");
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 200));
            panel.revalidate();
            panel.repaint();
        });

        return panel;
    }

    private JPanel createBottomButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton manageConditionsBtn = new JButton("Manage Conditions");
        manageConditionsBtn.addActionListener(e -> openConditionsManager());
        panel.add(manageConditionsBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveScene());
        panel.add(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        panel.add(cancelBtn);

        return panel;
    }

    // ========================================
    // BACKGROUNDS METHODS
    // ========================================

    private void addBackground() {
        String name = JOptionPane.showInputDialog(this, "Enter background name:");
        if (name != null && !name.trim().isEmpty()) {
            ConditionalImage newBg = new ConditionalImage();
            newBg.setName(name);
            newBg.setImagePath("default.png");
            newBg.setShowIfTrue(true);

            scene.addBackgroundImage(newBg);
            backgroundsListModel.addElement(newBg);
        }
    }

    private void editBackground() {
        int index = backgroundsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a background first!");
            return;
        }

        ConditionalImage selectedBg = backgroundsListModel.getElementAt(index);
        BackgroundEditorDialog bgEditor = new BackgroundEditorDialog(this, selectedBg, scene);
        bgEditor.setVisible(true);

        // Refresh list
        refreshBackgroundsList();
        backgroundsList.repaint();
    }

    private void deleteBackground() {
        int index = backgroundsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a background first!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this background?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ConditionalImage bg = backgroundsListModel.remove(index);
            scene.getBackgroundImages().remove(bg);
        }
    }

    private void refreshBackgroundsList() {
        backgroundsListModel.clear();
        if (scene.getBackgroundImages() != null) {
            for (ConditionalImage img : scene.getBackgroundImages()) {
                backgroundsListModel.addElement(img);
            }
        }
    }

    // ========================================
    // ITEMS METHODS
    // ========================================

    private void addItem() {
        String name = JOptionPane.showInputDialog(this, "Enter item name:");
        if (name != null && !name.trim().isEmpty()) {
            Item newItem = new Item(name);
            scene.addItem(newItem);
            itemsListModel.addElement(newItem);
        }
    }

    private void editItem() {
        int index = itemsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item first!");
            return;
        }

        Item item = itemsListModel.getElementAt(index);
        ItemEditorDialog editor = new ItemEditorDialog(editorMain, item);
        editor.setVisible(true);
        itemsList.repaint();
    }

    private void editItemActions() {
        int index = itemsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item first!");
            return;
        }

        Item item = itemsListModel.getElementAt(index);
        ItemActionsEditorDialog editor = new ItemActionsEditorDialog(editorMain, item);
        editor.setVisible(true);
    }

    private void deleteItem() {
        int index = itemsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item first!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this item?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Item item = itemsListModel.remove(index);
            scene.getItems().remove(item);
        }
    }

    // ========================================
    // KEYAREAS METHODS
    // ========================================

    private void addKeyArea() {
        String name = JOptionPane.showInputDialog(this, "Enter KeyArea name:");
        if (name != null && !name.trim().isEmpty()) {
            String typeStr = (String) JOptionPane.showInputDialog(this, "Select KeyArea type:", "Type",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Interaction", "Transition", "Movement_Bounds", "Character_Range"},
                "Interaction");

            if (typeStr != null) {
                KeyArea.Type type = KeyArea.Type.valueOf(typeStr.toUpperCase());
                KeyArea newArea = new KeyArea(type, name);
                scene.addKeyArea(newArea);
                keyAreasListModel.addElement(newArea);
            }
        }
    }

    private void editKeyArea() {
        int index = keyAreasList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a KeyArea first!");
            return;
        }

        KeyArea area = keyAreasListModel.getElementAt(index);
        KeyAreaEditorDialog editor = new KeyAreaEditorDialog(editorMain, area, scene);
        editor.setVisible(true);
        keyAreasList.repaint();
    }

    private void editKeyAreaPoints() {
        int index = keyAreasList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a KeyArea first!");
            return;
        }

        KeyArea area = keyAreasListModel.getElementAt(index);
        UniversalPointEditorDialog pointEditor = new UniversalPointEditorDialog(editorMain, area);
        pointEditor.setVisible(true);
        keyAreasList.repaint();
    }

    private void editKeyAreaActions() {
        int index = keyAreasList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a KeyArea first!");
            return;
        }

        KeyArea area = keyAreasListModel.getElementAt(index);
        ActionsEditorDialog editor = new ActionsEditorDialog(editorMain, area);
        editor.setVisible(true);
    }

    private void deleteKeyArea() {
        int index = keyAreasList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a KeyArea first!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this KeyArea?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            KeyArea area = keyAreasListModel.remove(index);
            scene.getKeyAreas().remove(area);
        }
    }

    // ========================================
    // DIALOGS METHODS
    // ========================================

    private void addDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter dialog name:");
        if (name != null && !name.trim().isEmpty()) {
            scene.addDialog(name, "");
            dialogsListModel.addElement(name);
        }
    }

    private void editDialog() {
        int index = dialogsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a dialog first!");
            return;
        }

        String dialogName = dialogsListModel.getElementAt(index);
        // Simple text editor for dialog content
        String currentText = scene.getDialogs().get(dialogName);
        String newText = (String) JOptionPane.showInputDialog(
            this,
            "Edit dialog text for: " + dialogName,
            "Edit Dialog",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            currentText
        );
        if (newText != null) {
            scene.getDialogs().put(dialogName, newText);
        }
    }

    private void deleteDialog() {
        int index = dialogsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a dialog first!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this dialog?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String dialogName = dialogsListModel.remove(index);
            scene.getDialogs().remove(dialogName);
        }
    }

    // ========================================
    // GENERAL METHODS
    // ========================================

    private void openConditionsManager() {
        ConditionsManagerDialog dialog = new ConditionsManagerDialog(editorMain);
        dialog.setVisible(true);
    }

    private void saveScene() {
        // Update scene name if changed
        String newName = sceneNameField.getText().trim();
        if (!newName.equals(scene.getName())) {
            // TODO: Scene renaming would require:
            // 1. Renaming the scene file
            // 2. Checking all references in other files
            // 3. Updating those references
            // For now, scene name changes are not supported
            JOptionPane.showMessageDialog(this,
                "Scene renaming is not yet supported.\nPlease use Copy and Delete to rename a scene.",
                "Not Supported", JOptionPane.WARNING_MESSAGE);
            sceneNameField.setText(scene.getName()); // Revert to original name
        }

        try {
            SceneSaver.saveSceneToDefault(scene);
            JOptionPane.showMessageDialog(this, "Scene saved successfully!", "Success",
                JOptionPane.INFORMATION_MESSAGE);

            // Refresh EditorMain
            if (editorMain != null) {
                editorMain.updateSceneInfo();
                editorMain.refreshKeyAreaList();
                editorMain.refreshItemList();
            }

            // Refresh game
            if (game != null) {
                game.loadScene(scene.getName());
            }

            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving scene:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ========================================
    // CELL RENDERERS
    // ========================================

    private class BackgroundCellRenderer extends JLabel implements ListCellRenderer<ConditionalImage> {
        private Map<String, ImageIcon> imageCache = new HashMap<>();

        public BackgroundCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ConditionalImage> list,
                ConditionalImage value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value != null) {
                setText(value.getName() + " - " + value.getImagePath());
                setFont(new Font("Arial", Font.PLAIN, 11));

                // Try to load thumbnail
                String imagePath = value.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imgFile = ResourcePathHelper.resolve("images/" + imagePath);
                    if (imgFile.exists() && !imageCache.containsKey(imagePath)) {
                        try {
                            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                            ImageIcon thumbnail = new ImageIcon(icon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH));
                            imageCache.put(imagePath, thumbnail);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    setIcon(imageCache.get(imagePath));
                }
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    private class ItemCellRenderer extends JLabel implements ListCellRenderer<Item> {
        private Map<String, ImageIcon> imageCache = new HashMap<>();

        public ItemCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list,
                Item value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value != null) {
                setText(value.getName());
                setFont(new Font("Arial", Font.PLAIN, 11));

                // Try to load thumbnail
                String imagePath = value.getImageFilePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imgFile = ResourcePathHelper.resolve("images/" + imagePath);
                    if (imgFile.exists() && !imageCache.containsKey(imagePath)) {
                        try {
                            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                            ImageIcon thumbnail = new ImageIcon(icon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH));
                            imageCache.put(imagePath, thumbnail);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    setIcon(imageCache.get(imagePath));
                }
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    private class KeyAreaCellRenderer extends JLabel implements ListCellRenderer<KeyArea> {
        public KeyAreaCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends KeyArea> list,
                KeyArea value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value != null) {
                int pointCount = value.getPoints() != null ? value.getPoints().size() : 0;
                setText(value.getName() + " (" + value.getType() + ") - " + pointCount + " points");
                setFont(new Font("Arial", Font.PLAIN, 11));
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
