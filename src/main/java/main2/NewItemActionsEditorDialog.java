package main2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * New Actions Editor for Items with multiple ConditionsFields
 * Each ConditionsField is a group of conditions with their own result values and process
 */
public class NewItemActionsEditorDialog extends JDialog {

    private Item item;
    private String actionName;
    private JPanel conditionsFieldsContainer;
    private List<ConditionsFieldPanel> conditionsFieldPanels = new ArrayList<>();

    public NewItemActionsEditorDialog(Frame owner, Item item, String actionName) {
        super(owner, "Actions Editor - " + actionName, true);
        this.item = item;
        this.actionName = actionName;

        initUI();
        loadExistingActions();

        setSize(1000, 700);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top panel with manage conditions button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton manageConditionsBtn = new JButton("Manage Conditions");
        manageConditionsBtn.addActionListener(e -> openConditionsManager());
        topPanel.add(manageConditionsBtn);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with ConditionsFields
        conditionsFieldsContainer = new JPanel();
        conditionsFieldsContainer.setLayout(new BoxLayout(conditionsFieldsContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(conditionsFieldsContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Add New ConditionsField button on the left
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFieldBtn = new JButton("Add New Conditions Field");
        addFieldBtn.addActionListener(e -> addNewConditionsField());
        leftPanel.add(addFieldBtn);
        bottomPanel.add(leftPanel, BorderLayout.WEST);

        // Save/Cancel buttons on the right
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveActions());
        rightPanel.add(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        rightPanel.add(cancelBtn);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Add initial ConditionsField
        addNewConditionsField();
    }

    private void addNewConditionsField() {
        ConditionsFieldPanel panel = new ConditionsFieldPanel();
        conditionsFieldPanels.add(panel);
        conditionsFieldsContainer.add(panel);
        conditionsFieldsContainer.revalidate();
        conditionsFieldsContainer.repaint();
    }

    private void removeConditionsField(ConditionsFieldPanel panel) {
        conditionsFieldPanels.remove(panel);
        conditionsFieldsContainer.remove(panel);
        conditionsFieldsContainer.revalidate();
        conditionsFieldsContainer.repaint();
    }

    private void loadExistingActions() {
        File actionsFile = new File("resources/actions/" + item.getName() + ".txt");
        if (!actionsFile.exists()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(actionsFile.toPath());

            // Clear existing fields
            for (ConditionsFieldPanel panel : conditionsFieldPanels) {
                conditionsFieldsContainer.remove(panel);
            }
            conditionsFieldPanels.clear();

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("#" + actionName + ":")) {
                    // Parse action line
                    String content = line.substring(actionName.length() + 2).trim();

                    // Split into condition and result parts
                    String[] parts = content.split("->");
                    if (parts.length == 2) {
                        String conditionsStr = parts[0].trim();
                        String resultsStr = parts[1].trim();

                        // Create a new ConditionsField for this line
                        ConditionsFieldPanel fieldPanel = new ConditionsFieldPanel();

                        // Parse conditions (could be multiple with AND)
                        String[] conditions = conditionsStr.split("&&");

                        // Parse results
                        String processName = "";
                        Map<String, Boolean> resultValues = new HashMap<>();

                        String[] results = resultsStr.split("\\|\\|");
                        for (String result : results) {
                            result = result.trim();

                            if (result.startsWith("#SetBoolean:")) {
                                String boolStr = result.substring(12).trim();
                                String[] boolParts = boolStr.split("=");
                                if (boolParts.length == 2) {
                                    String condName = boolParts[0].trim();
                                    boolean value = Boolean.parseBoolean(boolParts[1].trim());
                                    resultValues.put(condName, value);
                                }
                            } else if (result.startsWith("#Process:")) {
                                processName = result.substring(9).trim();
                            }
                        }

                        // Add conditions to the field
                        for (String cond : conditions) {
                            cond = cond.trim();
                            String[] condParts = cond.split("=");
                            if (condParts.length == 2) {
                                String condName = condParts[0].trim();
                                String condValue = condParts[1].trim();

                                ConditionRow row = new ConditionRow(condName);
                                row.use = true;
                                row.ifCurrentValue = condValue; // "true" or "false"
                                row.processName = processName;

                                fieldPanel.tableModel.addRow(row);
                            }
                        }

                        // Set result values for the field
                        fieldPanel.resultValues = resultValues;

                        conditionsFieldPanels.add(fieldPanel);
                        conditionsFieldsContainer.add(fieldPanel);
                    }
                }
            }

            // If no fields were loaded, add an empty one
            if (conditionsFieldPanels.isEmpty()) {
                addNewConditionsField();
            }

            conditionsFieldsContainer.revalidate();
            conditionsFieldsContainer.repaint();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveActions() {
        File actionsFile = new File("resources/actions/" + item.getName() + ".txt");

        try {
            List<String> lines = new ArrayList<>();

            // Read existing lines
            if (actionsFile.exists()) {
                lines = new ArrayList<>(Files.readAllLines(actionsFile.toPath()));
            }

            // Remove old action lines for this action
            lines.removeIf(line -> line.trim().startsWith("#" + actionName + ":"));

            // Build new action lines from ConditionsFields
            for (ConditionsFieldPanel fieldPanel : conditionsFieldPanels) {
                List<ConditionRow> usedRows = new ArrayList<>();
                for (int i = 0; i < fieldPanel.tableModel.getRowCount(); i++) {
                    ConditionRow row = fieldPanel.tableModel.getRow(i);
                    if (row.use) {
                        usedRows.add(row);
                    }
                }

                if (usedRows.isEmpty()) {
                    continue; // Skip empty fields
                }

                StringBuilder conditionsStr = new StringBuilder();
                StringBuilder resultsStr = new StringBuilder();
                String processName = "";

                // Build conditions string
                for (ConditionRow row : usedRows) {
                    // Skip conditions with "ignore"
                    if (!"ignore".equals(row.ifCurrentValue)) {
                        if (conditionsStr.length() > 0) {
                            conditionsStr.append(" && ");
                        }
                        conditionsStr.append(row.conditionName).append("=").append(row.ifCurrentValue);
                    }

                    // Get process name (from first row that has one)
                    if (!row.processName.isEmpty() && processName.isEmpty()) {
                        processName = row.processName;
                    }
                }

                // Build results string from result values
                for (Map.Entry<String, Boolean> entry : fieldPanel.resultValues.entrySet()) {
                    if (resultsStr.length() > 0) {
                        resultsStr.append(" || ");
                    }
                    resultsStr.append("#SetBoolean:").append(entry.getKey()).append("=").append(entry.getValue());
                }

                // Add process to results
                if (!processName.isEmpty()) {
                    if (resultsStr.length() > 0) {
                        resultsStr.append(" || ");
                    }
                    resultsStr.append("#Process:").append(processName);
                }

                // Only save if we have conditions or results
                if (conditionsStr.length() > 0 || resultsStr.length() > 0) {
                    String actionLine = "#" + actionName + ":";
                    if (conditionsStr.length() > 0) {
                        actionLine += conditionsStr.toString();
                    } else {
                        actionLine += "true"; // Default condition if no conditions specified
                    }
                    actionLine += " -> " + resultsStr.toString();
                    lines.add(actionLine);
                }
            }

            // Write back to file
            actionsFile.getParentFile().mkdirs();
            Files.write(actionsFile.toPath(), lines);

            JOptionPane.showMessageDialog(this, "Actions saved successfully!", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving actions: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openConditionsManager() {
        if (getOwner() instanceof EditorWindow) {
            EditorWindow editorWindow = (EditorWindow) getOwner();
            ConditionsManagerDialog dialog = new ConditionsManagerDialog(editorWindow);
            dialog.setVisible(true);

            // Reload all fields to show new conditions
            for (ConditionsFieldPanel panel : conditionsFieldPanels) {
                panel.refreshAvailableConditions();
            }
        }
    }

    // Inner class representing one ConditionsField panel
    class ConditionsFieldPanel extends JPanel {
        private JTable conditionsTable;
        private ConditionsTableModel tableModel;
        private Map<String, Boolean> resultValues = new HashMap<>();
        private JComboBox<String> addConditionCombo;

        public ConditionsFieldPanel() {
            initUI();
        }

        private void initUI() {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setLayout(new BorderLayout(5, 5));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

            // Title
            JLabel titleLabel = new JLabel("Conditions Field");
            titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(java.awt.Font.BOLD));
            add(titleLabel, BorderLayout.NORTH);

            // Table
            tableModel = new ConditionsTableModel();
            conditionsTable = new JTable(tableModel);
            conditionsTable.setRowHeight(30);

            // Set up column widths
            conditionsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Conditions
            conditionsTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // use
            conditionsTable.getColumnModel().getColumn(2).setPreferredWidth(200); // if Current Value
            conditionsTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Result Values
            conditionsTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Result Process

            // Set up custom renderers and editors
            conditionsTable.getColumnModel().getColumn(1).setCellRenderer(new CheckBoxRenderer());
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new CheckBoxEditor());

            conditionsTable.getColumnModel().getColumn(2).setCellRenderer(new IfCurrentValueRenderer());
            conditionsTable.getColumnModel().getColumn(2).setCellEditor(new IfCurrentValueEditor());

            conditionsTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
            conditionsTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor());

            conditionsTable.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JTextField()));

            JScrollPane scrollPane = new JScrollPane(conditionsTable);
            scrollPane.setPreferredSize(new Dimension(900, 150));
            add(scrollPane, BorderLayout.CENTER);

            // Bottom buttons panel
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            // Add Condition combo and button
            addConditionCombo = new JComboBox<>();
            refreshAvailableConditions();
            addConditionCombo.setPreferredSize(new Dimension(150, 25));
            buttonsPanel.add(new JLabel("Add:"));
            buttonsPanel.add(addConditionCombo);

            JButton addConditionBtn = new JButton("Add Condition");
            addConditionBtn.addActionListener(e -> addCondition());
            buttonsPanel.add(addConditionBtn);

            JButton removeConditionBtn = new JButton("Remove Selected Condition");
            removeConditionBtn.addActionListener(e -> removeSelectedCondition());
            buttonsPanel.add(removeConditionBtn);

            JButton removeFieldBtn = new JButton("Remove this Conditions Field");
            removeFieldBtn.addActionListener(e -> removeThisField());
            buttonsPanel.add(removeFieldBtn);

            add(buttonsPanel, BorderLayout.SOUTH);
        }

        private void refreshAvailableConditions() {
            addConditionCombo.removeAllItems();
            Set<String> allConditions = Conditions.getAllConditionNames();
            for (String conditionName : allConditions) {
                addConditionCombo.addItem(conditionName);
            }
        }

        private void addCondition() {
            String selectedCondition = (String) addConditionCombo.getSelectedItem();
            if (selectedCondition != null && !selectedCondition.trim().isEmpty()) {
                // Check if condition already exists in this field
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getRow(i).conditionName.equals(selectedCondition)) {
                        JOptionPane.showMessageDialog(this,
                            "Condition already exists in this field",
                            "Duplicate", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                ConditionRow row = new ConditionRow(selectedCondition);
                row.use = true;
                tableModel.addRow(row);
            }
        }

        private void removeSelectedCondition() {
            int selectedRow = conditionsTable.getSelectedRow();
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a condition to remove",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void removeThisField() {
            int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this Conditions Field?",
                "Confirm", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                removeConditionsField(this);
            }
        }

        private void openResultValuesEditor() {
            ResultValuesEditorDialog dialog = new ResultValuesEditorDialog(
                (Frame) SwingConstants.getWindowAncestor(this),
                resultValues
            );
            dialog.setVisible(true);

            if (!dialog.wasCancelled()) {
                resultValues = dialog.getResultValues();
            }
        }

        // Table Model
        class ConditionsTableModel extends AbstractTableModel {
            private List<ConditionRow> rows = new ArrayList<>();
            private String[] columnNames = {"Conditions", "use", "if Current Value", "Result Values", "Result Process"};

            public void addRow(ConditionRow row) {
                rows.add(row);
                fireTableDataChanged();
            }

            public void removeRow(int rowIndex) {
                rows.remove(rowIndex);
                fireTableDataChanged();
            }

            public ConditionRow getRow(int rowIndex) {
                return rows.get(rowIndex);
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
                    case 1: return row.use;
                    case 2: return row.ifCurrentValue;
                    case 3: return "Edit...";
                    case 4: return row.processName;
                    default: return null;
                }
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                ConditionRow row = rows.get(rowIndex);

                switch (columnIndex) {
                    case 1:
                        row.use = (Boolean) value;
                        break;
                    case 2:
                        row.ifCurrentValue = (String) value;
                        break;
                    case 4:
                        row.processName = (String) value;
                        break;
                }

                fireTableCellUpdated(rowIndex, columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                // Columns 1 (use), 2 (if current value), 3 (result values button), and 4 (process) are editable
                return columnIndex == 1 || columnIndex == 2 || columnIndex == 3 || columnIndex == 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 1: return Boolean.class;
                    case 2: return String.class;
                    case 3: return String.class;
                    default: return String.class;
                }
            }
        }

        // Button Renderer for Result Values column
        class ButtonRenderer extends JButton implements TableCellRenderer {
            public ButtonRenderer() {
                setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                setText(value != null ? value.toString() : "Edit...");
                return this;
            }
        }

        // Button Editor for Result Values column
        class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
            private JButton button;
            private boolean isPushed;

            public ButtonEditor() {
                button = new JButton();
                button.setOpaque(true);
                button.addActionListener(e -> {
                    openResultValuesEditor();
                    fireEditingStopped();
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                button.setText("Edit...");
                isPushed = true;
                return button;
            }

            @Override
            public Object getCellEditorValue() {
                isPushed = false;
                return "Edit...";
            }
        }
    }

    // Data class for table rows
    class ConditionRow {
        String conditionName;
        boolean use = false;
        String ifCurrentValue = "ignore"; // "true", "false", or "ignore"
        String processName = "";

        public ConditionRow(String conditionName) {
            this.conditionName = conditionName;
        }

        public boolean getCurrentValue() {
            return Conditions.getCondition(conditionName);
        }
    }

    // Custom cell renderers (shared across all ConditionsFields)
    class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(value != null && (Boolean) value);
            return this;
        }
    }

    class IfCurrentValueRenderer extends JPanel implements TableCellRenderer {
        private JRadioButton trueButton;
        private JRadioButton falseButton;
        private JRadioButton ignoreButton;

        public IfCurrentValueRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");
            ignoreButton = new JRadioButton("ignore");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);
            group.add(ignoreButton);

            add(trueButton);
            add(falseButton);
            add(ignoreButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            String strValue = value != null ? value.toString() : "ignore";

            trueButton.setSelected("true".equals(strValue));
            falseButton.setSelected("false".equals(strValue));
            ignoreButton.setSelected("ignore".equals(strValue));

            return this;
        }
    }

    // Custom cell editors (shared across all ConditionsFields)
    class IfCurrentValueEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JRadioButton trueButton;
        private JRadioButton falseButton;
        private JRadioButton ignoreButton;

        public IfCurrentValueEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");
            ignoreButton = new JRadioButton("ignore");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);
            group.add(ignoreButton);

            panel.add(trueButton);
            panel.add(falseButton);
            panel.add(ignoreButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            String strValue = value != null ? value.toString() : "ignore";

            trueButton.setSelected("true".equals(strValue));
            falseButton.setSelected("false".equals(strValue));
            ignoreButton.setSelected("ignore".equals(strValue));

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            if (trueButton.isSelected()) {
                return "true";
            } else if (falseButton.isSelected()) {
                return "false";
            } else {
                return "ignore";
            }
        }
    }

    class CheckBoxEditor extends DefaultCellEditor {
        private JCheckBox checkBox;

        public CheckBoxEditor() {
            super(new JCheckBox());
            checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            checkBox.setSelected(value != null && (Boolean) value);
            return checkBox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }
}
