package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Complete Process Editor for creating/editing process files
 */
public class ProcessEditorDialog extends JDialog {

    private Frame owner;
    private Item item;
    private String actionName;
    private Process process;

    // UI Components
    private JTextField processNameField;
    private JTextField descriptionField;

    // IF Conditions
    private DefaultListModel<ConditionEntry> conditionsListModel;
    private JList<ConditionEntry> conditionsList;

    // THEN Actions
    private DefaultListModel<ActionEntry> actionsListModel;
    private JList<ActionEntry> actionsList;

    // Action types
    private static final String[] ACTION_TYPES = {
        "Movement", "Conditions", "Dialog", "Text", "SceneChange",
        "Sound", "AddToInventory", "RemoveFromInventory", "Wait",
        "ItemVisibility"
    };

    public ProcessEditorDialog(Frame owner, Item item, String actionName) {
        super(owner, "Process Editor - " + actionName, true);
        System.out.println("ProcessEditorDialog: Constructor started");
        this.owner = owner;
        this.item = item;
        this.actionName = actionName;

        // Try to load existing process
        String processFileName = ProcessLoader.getProcessFileName(item.getName(), actionName);
        System.out.println("ProcessEditorDialog: Process file name: " + processFileName);

        this.process = ProcessLoader.loadProcess(processFileName);

        if (this.process == null) {
            System.out.println("ProcessEditorDialog: No existing process found, creating new");
            // Create new process
            this.process = new Process(processFileName);
            this.process.setDescription("Process for " + item.getName() + " - " + actionName);
        } else {
            System.out.println("ProcessEditorDialog: Loaded existing process");
        }

        System.out.println("ProcessEditorDialog: Calling initUI()");
        initUI();

        System.out.println("ProcessEditorDialog: Calling loadProcessData()");
        loadProcessData();

        System.out.println("ProcessEditorDialog: Setting size and location");
        setSize(1200, 800);
        setLocationRelativeTo(owner);

        System.out.println("ProcessEditorDialog: Constructor completed");
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top panel - Process name and description
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Split into IF and THEN
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Left: IF Conditions
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel ifPanel = createIfConditionsPanel();
        centerPanel.add(ifPanel, gbc);

        // Right: THEN Actions
        gbc.gridx = 1;
        gbc.gridy = 0;
        JPanel thenPanel = createThenActionsPanel();
        centerPanel.add(thenPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Buttons
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Title
        JLabel titleLabel = new JLabel("Process Editor: " + item.getName() + " - " + actionName);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(10));

        // Process name field
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Process Name:"));
        processNameField = new JTextField(30);
        namePanel.add(processNameField);
        panel.add(namePanel);

        // Description field
        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField(50);
        descPanel.add(descriptionField);
        panel.add(descPanel);

        return panel;
    }

    private JPanel createIfConditionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("IF - Conditions"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Info label
        JLabel infoLabel = new JLabel("Conditions that must be met for this process to execute:");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Conditions list
        conditionsListModel = new DefaultListModel<>();
        conditionsList = new JList<>(conditionsListModel);
        conditionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(conditionsList);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("Add Condition");
        addBtn.addActionListener(e -> addCondition());
        buttonsPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editCondition());
        buttonsPanel.add(editBtn);

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeCondition());
        buttonsPanel.add(removeBtn);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createThenActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("THEN - Actions"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Info label
        JLabel infoLabel = new JLabel("Actions to execute when conditions are met:");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Actions list
        actionsListModel = new DefaultListModel<>();
        actionsList = new JList<>(actionsListModel);
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(actionsList);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("Add Action");
        addBtn.addActionListener(e -> addAction());
        buttonsPanel.add(addBtn);

        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editAction());
        buttonsPanel.add(editBtn);

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeAction());
        buttonsPanel.add(removeBtn);

        JButton moveUpBtn = new JButton("↑ Move Up");
        moveUpBtn.addActionListener(e -> moveActionUp());
        buttonsPanel.add(moveUpBtn);

        JButton moveDownBtn = new JButton("↓ Move Down");
        moveDownBtn.addActionListener(e -> moveActionDown());
        buttonsPanel.add(moveDownBtn);

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Left buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton manageConditionsBtn = new JButton("Manage Conditions");
        manageConditionsBtn.addActionListener(e -> openConditionsManager());
        leftPanel.add(manageConditionsBtn);

        JButton testProcessBtn = new JButton("Test Process");
        testProcessBtn.addActionListener(e -> testProcess());
        leftPanel.add(testProcessBtn);

        panel.add(leftPanel, BorderLayout.WEST);

        // Right buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveBtn = new JButton("💾 Save Process");
        saveBtn.addActionListener(e -> saveProcess());
        rightPanel.add(saveBtn);

        JButton saveAndReferenceBtn = new JButton("💾 Save & Add to Action");
        saveAndReferenceBtn.addActionListener(e -> saveAndReference());
        rightPanel.add(saveAndReferenceBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        rightPanel.add(cancelBtn);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private void loadProcessData() {
        // Load process name
        processNameField.setText(process.getProcessName());
        descriptionField.setText(process.getDescription());

        // Load IF conditions
        conditionsListModel.clear();
        for (Map.Entry<String, Boolean> entry : process.getConditions().entrySet()) {
            ConditionEntry ce = new ConditionEntry(entry.getKey(), entry.getValue());
            conditionsListModel.addElement(ce);
        }

        // Load THEN actions
        actionsListModel.clear();
        for (Process.ProcessAction action : process.getActions()) {
            ActionEntry ae = new ActionEntry(action);
            actionsListModel.addElement(ae);
        }
    }

    private void addCondition() {
        // Show dialog to select condition
        Set<String> allConditions = Conditions.getAllConditionNames();
        if (allConditions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No conditions available. Please create conditions first.",
                "No Conditions", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] conditionsArray = allConditions.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(this,
            "Select condition:", "Add Condition",
            JOptionPane.QUESTION_MESSAGE, null, conditionsArray, conditionsArray[0]);

        if (selected != null) {
            // Ask for value (true/false)
            String[] values = {"true", "false"};
            String valueStr = (String) JOptionPane.showInputDialog(this,
                "Select required value for " + selected + ":", "Condition Value",
                JOptionPane.QUESTION_MESSAGE, null, values, "true");

            if (valueStr != null) {
                boolean value = Boolean.parseBoolean(valueStr);
                ConditionEntry ce = new ConditionEntry(selected, value);
                conditionsListModel.addElement(ce);
            }
        }
    }

    private void editCondition() {
        int index = conditionsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a condition to edit",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ConditionEntry ce = conditionsListModel.get(index);
        String[] values = {"true", "false"};
        String valueStr = (String) JOptionPane.showInputDialog(this,
            "Select required value for " + ce.conditionName + ":", "Edit Condition",
            JOptionPane.QUESTION_MESSAGE, null, values, ce.expectedValue ? "true" : "false");

        if (valueStr != null) {
            ce.expectedValue = Boolean.parseBoolean(valueStr);
            conditionsListModel.set(index, ce);
        }
    }

    private void removeCondition() {
        int index = conditionsList.getSelectedIndex();
        if (index >= 0) {
            conditionsListModel.remove(index);
        }
    }

    private void addAction() {
        String selected = (String) JOptionPane.showInputDialog(this,
            "Select action type:", "Add Action",
            JOptionPane.QUESTION_MESSAGE, null, ACTION_TYPES, ACTION_TYPES[0]);

        if (selected != null) {
            Process.ProcessAction action = createActionByType(selected);
            if (action != null) {
                // Open editor for this action
                if (editActionDialog(action, true)) {
                    ActionEntry ae = new ActionEntry(action);
                    actionsListModel.addElement(ae);
                }
            }
        }
    }

    private void editAction() {
        int index = actionsList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select an action to edit",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ActionEntry ae = actionsListModel.get(index);
        if (editActionDialog(ae.action, false)) {
            actionsListModel.set(index, ae);
        }
    }

    private void removeAction() {
        int index = actionsList.getSelectedIndex();
        if (index >= 0) {
            actionsListModel.remove(index);
        }
    }

    private void moveActionUp() {
        int index = actionsList.getSelectedIndex();
        if (index > 0) {
            ActionEntry ae = actionsListModel.remove(index);
            actionsListModel.add(index - 1, ae);
            actionsList.setSelectedIndex(index - 1);
        }
    }

    private void moveActionDown() {
        int index = actionsList.getSelectedIndex();
        if (index >= 0 && index < actionsListModel.size() - 1) {
            ActionEntry ae = actionsListModel.remove(index);
            actionsListModel.add(index + 1, ae);
            actionsList.setSelectedIndex(index + 1);
        }
    }

    private Process.ProcessAction createActionByType(String type) {
        switch (type) {
            case "Movement": return new Process.MovementAction();
            case "Conditions": return new Process.ConditionsAction();
            case "Dialog": return new Process.DialogAction();
            case "Text": return new Process.TextAction();
            case "SceneChange": return new Process.SceneChangeAction();
            case "Sound": return new Process.SoundAction();
            case "AddToInventory": return new Process.AddToInventoryAction();
            case "RemoveFromInventory": return new Process.RemoveFromInventoryAction();
            case "Wait": return new Process.WaitAction();
            case "ItemVisibility": return new Process.ItemVisibilityAction();
            default: return null;
        }
    }

    private boolean editActionDialog(Process.ProcessAction action, boolean isNew) {
        String type = action.getActionType();

        switch (type) {
            case "Movement":
                return editMovementAction((Process.MovementAction) action);
            case "Conditions":
                return editConditionsAction((Process.ConditionsAction) action);
            case "Dialog":
                return editDialogAction((Process.DialogAction) action);
            case "Text":
                return editTextAction((Process.TextAction) action);
            case "SceneChange":
                return editSceneChangeAction((Process.SceneChangeAction) action);
            case "Sound":
                return editSoundAction((Process.SoundAction) action);
            case "AddToInventory":
                return editAddToInventoryAction((Process.AddToInventoryAction) action);
            case "RemoveFromInventory":
                return editRemoveFromInventoryAction((Process.RemoveFromInventoryAction) action);
            case "Wait":
                return editWaitAction((Process.WaitAction) action);
            case "ItemVisibility":
                return editItemVisibilityAction((Process.ItemVisibilityAction) action);
            default:
                return false;
        }
    }

    private boolean editMovementAction(Process.MovementAction action) {
        JDialog dialog = new JDialog(this, "Edit Movement Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Movement type
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Movement Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"GoTo", "GoToItem"});
        typeCombo.setSelectedItem(action.getParameter("type", "GoTo"));
        panel.add(typeCombo, gbc);

        // X coordinate
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("X:"), gbc);
        gbc.gridx = 1;
        JTextField xField = new JTextField(action.getParameter("x", "0"), 10);
        panel.add(xField, gbc);

        // Y coordinate
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Y:"), gbc);
        gbc.gridx = 1;
        JTextField yField = new JTextField(action.getParameter("y", "0"), 10);
        panel.add(yField, gbc);

        // Item name (for GoToItem)
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Item Name (for GoToItem):"), gbc);
        gbc.gridx = 1;
        JTextField itemField = new JTextField(action.getParameter("itemName", ""), 15);
        panel.add(itemField, gbc);

        // Offset X
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Offset X:"), gbc);
        gbc.gridx = 1;
        JTextField offsetXField = new JTextField(action.getParameter("offset_x", "0"), 10);
        panel.add(offsetXField, gbc);

        // Offset Y
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Offset Y:"), gbc);
        gbc.gridx = 1;
        JTextField offsetYField = new JTextField(action.getParameter("offset_y", "0"), 10);
        panel.add(offsetYField, gbc);

        // Wait for completion
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Wait for Completion:"), gbc);
        gbc.gridx = 1;
        JCheckBox waitCheckBox = new JCheckBox("", action.getBooleanParameter("waitForCompletion", true));
        panel.add(waitCheckBox, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("type", (String) typeCombo.getSelectedItem());
            action.setParameter("x", xField.getText());
            action.setParameter("y", yField.getText());
            action.setParameter("itemName", itemField.getText());
            action.setParameter("offset_x", offsetXField.getText());
            action.setParameter("offset_y", offsetYField.getText());
            action.setParameter("waitForCompletion", String.valueOf(waitCheckBox.isSelected()));
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editConditionsAction(Process.ConditionsAction action) {
        JDialog dialog = new JDialog(this, "Edit Conditions Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info
        panel.add(new JLabel("Set condition values when this action executes:"), BorderLayout.NORTH);

        // Conditions list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Map.Entry<String, String> entry : action.getParameters().entrySet()) {
            listModel.addElement(entry.getKey() + " = " + entry.getValue());
        }
        JList<String> condList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(condList);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons for managing conditions
        JPanel condButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addCondBtn = new JButton("Add Condition");
        addCondBtn.addActionListener(e -> {
            Set<String> allConds = Conditions.getAllConditionNames();
            if (allConds.isEmpty()) return;

            String[] condsArray = allConds.toArray(new String[0]);
            String selected = (String) JOptionPane.showInputDialog(dialog, "Select condition:",
                "Add Condition", JOptionPane.QUESTION_MESSAGE, null, condsArray, condsArray[0]);

            if (selected != null) {
                String[] values = {"true", "false"};
                String value = (String) JOptionPane.showInputDialog(dialog, "Select value:",
                    "Condition Value", JOptionPane.QUESTION_MESSAGE, null, values, "true");

                if (value != null) {
                    action.setParameter(selected, value);
                    listModel.addElement(selected + " = " + value);
                }
            }
        });
        condButtonsPanel.add(addCondBtn);

        JButton removeCondBtn = new JButton("Remove Selected");
        removeCondBtn.addActionListener(e -> {
            int index = condList.getSelectedIndex();
            if (index >= 0) {
                String entry = listModel.get(index);
                String condName = entry.split(" = ")[0];
                action.getParameters().remove(condName);
                listModel.remove(index);
            }
        });
        condButtonsPanel.add(removeCondBtn);

        panel.add(condButtonsPanel, BorderLayout.SOUTH);

        dialog.add(panel, BorderLayout.CENTER);

        // OK/Cancel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editDialogAction(Process.DialogAction action) {
        JDialog dialog = new JDialog(this, "Edit Dialog Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Dialog file
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Dialog File:"), gbc);
        gbc.gridx = 1;
        JTextField fileField = new JTextField(action.getParameter("file", ""), 20);
        panel.add(fileField, gbc);

        // Position
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Position:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> posCombo = new JComboBox<>(new String[]{"center", "top", "bottom", "left", "right"});
        posCombo.setSelectedItem(action.getParameter("position", "center"));
        panel.add(posCombo, gbc);

        // Block input
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Block Input:"), gbc);
        gbc.gridx = 1;
        JCheckBox blockCheckBox = new JCheckBox("", action.getBooleanParameter("blockInput", true));
        panel.add(blockCheckBox, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("file", fileField.getText());
            action.setParameter("position", (String) posCombo.getSelectedItem());
            action.setParameter("blockInput", String.valueOf(blockCheckBox.isSelected()));
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editTextAction(Process.TextAction action) {
        JDialog dialog = new JDialog(this, "Edit Text Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Message
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Message:"), gbc);
        gbc.gridx = 1;
        JTextField msgField = new JTextField(action.getParameter("message", ""), 30);
        panel.add(msgField, gbc);

        // Duration
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Duration (ms):"), gbc);
        gbc.gridx = 1;
        JTextField durationField = new JTextField(action.getParameter("duration", "2000"), 10);
        panel.add(durationField, gbc);

        // Position
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Position:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> posCombo = new JComboBox<>(new String[]{"center", "top", "bottom"});
        posCombo.setSelectedItem(action.getParameter("position", "center"));
        panel.add(posCombo, gbc);

        // Color
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Color (R,G,B):"), gbc);
        gbc.gridx = 1;
        JTextField colorField = new JTextField(action.getParameter("color", "255,255,255"), 15);
        panel.add(colorField, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("message", msgField.getText());
            action.setParameter("duration", durationField.getText());
            action.setParameter("position", (String) posCombo.getSelectedItem());
            action.setParameter("color", colorField.getText());
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editSceneChangeAction(Process.SceneChangeAction action) {
        JDialog dialog = new JDialog(this, "Edit Scene Change Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Scene name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Scene Name:"), gbc);
        gbc.gridx = 1;
        JTextField sceneField = new JTextField(action.getParameter("sceneName", ""), 20);
        panel.add(sceneField, gbc);

        // SubScene name
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("SubScene Name:"), gbc);
        gbc.gridx = 1;
        JTextField subSceneField = new JTextField(action.getParameter("subSceneName", ""), 20);
        panel.add(subSceneField, gbc);

        // Spawn point
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Spawn Point:"), gbc);
        gbc.gridx = 1;
        JTextField spawnField = new JTextField(action.getParameter("spawnPoint", ""), 20);
        panel.add(spawnField, gbc);

        // Transition
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Transition:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> transCombo = new JComboBox<>(new String[]{"fade", "none", "slide"});
        transCombo.setSelectedItem(action.getParameter("transition", "fade"));
        panel.add(transCombo, gbc);

        // Transition duration
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Transition Duration (ms):"), gbc);
        gbc.gridx = 1;
        JTextField durationField = new JTextField(action.getParameter("transitionDuration", "500"), 10);
        panel.add(durationField, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("sceneName", sceneField.getText());
            action.setParameter("subSceneName", subSceneField.getText());
            action.setParameter("spawnPoint", spawnField.getText());
            action.setParameter("transition", (String) transCombo.getSelectedItem());
            action.setParameter("transitionDuration", durationField.getText());
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editSoundAction(Process.SoundAction action) {
        JDialog dialog = new JDialog(this, "Edit Sound Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Sound file
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Sound File:"), gbc);
        gbc.gridx = 1;
        JTextField fileField = new JTextField(action.getParameter("file", ""), 20);
        panel.add(fileField, gbc);

        // Type
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"sfx", "music", "ambient"});
        typeCombo.setSelectedItem(action.getParameter("type", "sfx"));
        panel.add(typeCombo, gbc);

        // Volume
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Volume (0.0 - 1.0):"), gbc);
        gbc.gridx = 1;
        JTextField volumeField = new JTextField(action.getParameter("volume", "1.0"), 10);
        panel.add(volumeField, gbc);

        // Loop
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Loop:"), gbc);
        gbc.gridx = 1;
        JCheckBox loopCheckBox = new JCheckBox("", action.getBooleanParameter("loop", false));
        panel.add(loopCheckBox, gbc);

        // Wait for completion
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Wait for Completion:"), gbc);
        gbc.gridx = 1;
        JCheckBox waitCheckBox = new JCheckBox("", action.getBooleanParameter("waitForCompletion", false));
        panel.add(waitCheckBox, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("file", fileField.getText());
            action.setParameter("type", (String) typeCombo.getSelectedItem());
            action.setParameter("volume", volumeField.getText());
            action.setParameter("loop", String.valueOf(loopCheckBox.isSelected()));
            action.setParameter("waitForCompletion", String.valueOf(waitCheckBox.isSelected()));
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editAddToInventoryAction(Process.AddToInventoryAction action) {
        JDialog dialog = new JDialog(this, "Edit Add To Inventory Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Item name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        JTextField itemField = new JTextField(action.getParameter("itemName", ""), 20);
        panel.add(itemField, gbc);

        // Show notification
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Show Notification:"), gbc);
        gbc.gridx = 1;
        JCheckBox notifCheckBox = new JCheckBox("", action.getBooleanParameter("showNotification", true));
        panel.add(notifCheckBox, gbc);

        // Notification text
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Notification Text:"), gbc);
        gbc.gridx = 1;
        JTextField notifTextField = new JTextField(action.getParameter("notificationText", ""), 30);
        panel.add(notifTextField, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("itemName", itemField.getText());
            action.setParameter("showNotification", String.valueOf(notifCheckBox.isSelected()));
            action.setParameter("notificationText", notifTextField.getText());
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editRemoveFromInventoryAction(Process.RemoveFromInventoryAction action) {
        JDialog dialog = new JDialog(this, "Edit Remove From Inventory Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Item name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        JTextField itemField = new JTextField(action.getParameter("itemName", ""), 20);
        panel.add(itemField, gbc);

        // Show notification
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Show Notification:"), gbc);
        gbc.gridx = 1;
        JCheckBox notifCheckBox = new JCheckBox("", action.getBooleanParameter("showNotification", false));
        panel.add(notifCheckBox, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("itemName", itemField.getText());
            action.setParameter("showNotification", String.valueOf(notifCheckBox.isSelected()));
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editWaitAction(Process.WaitAction action) {
        JDialog dialog = new JDialog(this, "Edit Wait Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Duration (ms):"));
        JTextField durationField = new JTextField(action.getParameter("duration", "1000"), 10);
        panel.add(durationField);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("duration", durationField.getText());
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private boolean editItemVisibilityAction(Process.ItemVisibilityAction action) {
        JDialog dialog = new JDialog(this, "Edit Item Visibility Action", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Item name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        JTextField itemField = new JTextField(action.getParameter("itemName", ""), 20);
        panel.add(itemField, gbc);

        // Visible
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Visible:"), gbc);
        gbc.gridx = 1;
        JCheckBox visibleCheckBox = new JCheckBox("", action.getBooleanParameter("visible", true));
        panel.add(visibleCheckBox, gbc);

        // Fade
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Fade:"), gbc);
        gbc.gridx = 1;
        JCheckBox fadeCheckBox = new JCheckBox("", action.getBooleanParameter("fade", false));
        panel.add(fadeCheckBox, gbc);

        // Fade duration
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Fade Duration (ms):"), gbc);
        gbc.gridx = 1;
        JTextField fadeDurationField = new JTextField(action.getParameter("fadeDuration", "500"), 10);
        panel.add(fadeDurationField, gbc);

        dialog.add(panel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        boolean[] result = {false};
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            action.setParameter("itemName", itemField.getText());
            action.setParameter("visible", String.valueOf(visibleCheckBox.isSelected()));
            action.setParameter("fade", String.valueOf(fadeCheckBox.isSelected()));
            action.setParameter("fadeDuration", fadeDurationField.getText());
            result[0] = true;
            dialog.dispose();
        });
        buttonsPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonsPanel.add(cancelBtn);

        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private void openConditionsManager() {
        if (owner instanceof EditorMain) {
            EditorMain editorWindow = (EditorMain) owner;
            ConditionsManagerDialog dialog = new ConditionsManagerDialog(editorWindow);
            dialog.setVisible(true);
        }
    }

    private void testProcess() {
        // Build process and check for errors
        if (buildProcess()) {
            JOptionPane.showMessageDialog(this,
                "Process structure is valid!\n\n" +
                "Process: " + process.getProcessName() + "\n" +
                "Conditions: " + process.getConditions().size() + "\n" +
                "Actions: " + process.getActions().size(),
                "Test Result", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveProcess() {
        if (buildProcess()) {
            if (ProcessSaver.saveProcess(process)) {
                JOptionPane.showMessageDialog(this,
                    "Process saved successfully to:\n" +
                    "resources/processes/" + process.getProcessName() + ".txt",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error saving process!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveAndReference() {
        if (buildProcess()) {
            // Save process file
            if (ProcessSaver.saveProcess(process)) {
                // Add #Process: reference to item action
                KeyArea.ActionHandler handler = item.getActions().get(actionName);
                if (handler == null) {
                    handler = new KeyArea.ActionHandler();
                }

                // Build condition string from IF conditions
                StringBuilder conditionStr = new StringBuilder();
                for (int i = 0; i < conditionsListModel.size(); i++) {
                    if (i > 0) conditionStr.append(" AND ");
                    ConditionEntry ce = conditionsListModel.get(i);
                    conditionStr.append(ce.conditionName).append(" = ").append(ce.expectedValue);
                }

                // If no conditions, use "none"
                String finalCondition = conditionStr.length() > 0 ? conditionStr.toString() : "none";

                // Add process reference
                String processRef = "#Process:" + process.getProcessName();
                handler.addConditionalResult(finalCondition, processRef);

                item.addAction(actionName, handler);

                // Save item
                try {
                    ItemSaver.saveItemToDefault(item);
                } catch (Exception ex) {
                    System.err.println("Error saving item: " + ex.getMessage());
                }

                JOptionPane.showMessageDialog(this,
                    "Process saved and referenced in item action!\n\n" +
                    "Process file: resources/processes/" + process.getProcessName() + ".txt\n" +
                    "Referenced in: " + item.getName() + " - " + actionName,
                    "Success", JOptionPane.INFORMATION_MESSAGE);

                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error saving process!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean buildProcess() {
        // Validate process name
        String name = processNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a process name",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        process.setProcessName(name);
        process.setDescription(descriptionField.getText().trim());

        // Build conditions
        process.getConditions().clear();
        for (int i = 0; i < conditionsListModel.size(); i++) {
            ConditionEntry ce = conditionsListModel.get(i);
            process.addCondition(ce.conditionName, ce.expectedValue);
        }

        // Build actions
        process.getActions().clear();
        for (int i = 0; i < actionsListModel.size(); i++) {
            ActionEntry ae = actionsListModel.get(i);
            process.addAction(ae.action);
        }

        return true;
    }

    // Helper classes
    static class ConditionEntry {
        String conditionName;
        boolean expectedValue;

        ConditionEntry(String name, boolean value) {
            this.conditionName = name;
            this.expectedValue = value;
        }

        @Override
        public String toString() {
            return conditionName + " = " + expectedValue;
        }
    }

    static class ActionEntry {
        Process.ProcessAction action;

        ActionEntry(Process.ProcessAction action) {
            this.action = action;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(action.getActionType());

            // Add key parameters to display
            switch (action.getActionType()) {
                case "Movement":
                    sb.append(" (").append(action.getParameter("type", "GoTo")).append(")");
                    break;
                case "Conditions":
                    sb.append(" (").append(action.getParameters().size()).append(" conditions)");
                    break;
                case "Wait":
                    sb.append(" (").append(action.getParameter("duration", "1000")).append("ms)");
                    break;
                case "ItemVisibility":
                    sb.append(" (").append(action.getParameter("itemName", "")).append(")");
                    break;
                case "Dialog":
                    sb.append(" (").append(action.getParameter("file", "")).append(")");
                    break;
                case "Text":
                    sb.append(" (").append(action.getParameter("message", "")).append(")");
                    break;
                case "SceneChange":
                    sb.append(" (").append(action.getParameter("sceneName", "")).append(")");
                    break;
                case "AddToInventory":
                case "RemoveFromInventory":
                    sb.append(" (").append(action.getParameter("itemName", "")).append(")");
                    break;
            }

            return sb.toString();
        }
    }
}
