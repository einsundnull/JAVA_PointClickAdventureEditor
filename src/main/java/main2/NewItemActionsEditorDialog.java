package main2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
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
 * Item Actions Editor with multiple ConditionsFields
 * Each ConditionsField is a group of conditions with their own result values and process
 */
public class NewItemActionsEditorDialog extends JDialog {

    private Item item;
    private String actionName;
    private JPanel conditionsFieldsContainer;
    private List<ConditionsFieldPanel> conditionsFieldPanels = new ArrayList<>();

    public NewItemActionsEditorDialog(Frame owner, Item item, String actionName) {
        super(owner, "Item Actions Editor - " + actionName, true);
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
        System.out.println("=== LOADING ACTIONS FROM ITEM OBJECT ===");
        System.out.println("Item: " + item.getName());
        System.out.println("Action: " + actionName);

        // Get the action handler from the item
        KeyArea.ActionHandler handler = item.getActions().get(actionName);

        if (handler == null || handler.getConditionalResults() == null || handler.getConditionalResults().isEmpty()) {
            System.out.println("No actions found in item - creating empty field");
            return;
        }

        try {
            Map<String, String> conditionalResults = handler.getConditionalResults();
            System.out.println("Found " + conditionalResults.size() + " conditional results");

            // Clear existing fields
            for (ConditionsFieldPanel panel : conditionsFieldPanels) {
                conditionsFieldsContainer.remove(panel);
            }
            conditionsFieldPanels.clear();

            // Process each condition/result pair from the item's action handler
            for (Map.Entry<String, String> entry : conditionalResults.entrySet()) {
                String conditionString = entry.getKey();
                String resultString = entry.getValue();

                System.out.println("\nProcessing: " + conditionString + " -> " + resultString);

                // Create a new ConditionsFieldPanel for each condition/result pair
                ConditionsFieldPanel currentField = new ConditionsFieldPanel();

                // Split condition by AND
                String[] conditions = conditionString.split(" AND ");
                System.out.println("  Found " + conditions.length + " conditions");

                for (String condition : conditions) {
                    condition = condition.trim();

                    // Parse condition: "conditionName = value"
                    String[] parts = condition.split("=");
                    if (parts.length == 2) {
                        String condName = parts[0].trim();
                        String ifValue = parts[1].trim();

                        System.out.println("    Condition: " + condName + " = " + ifValue);

                        // Create row
                        ConditionRow row = new ConditionRow(condName);
                        row.ifCurrentValue = ifValue;
                        currentField.tableModel.addRow(row);
                    }
                }

                // Process results (can be multiple, separated by |||)
                String[] results = resultString.split("\\|\\|\\|");
                System.out.println("  Found " + results.length + " results");

                for (String result : results) {
                    result = result.trim();
                    System.out.println("    Result: " + result);

                    // Extract #SetBoolean results for the Result Values table
                    if (result.startsWith("#SetBoolean:")) {
                        String setBooleanLine = result.substring(12); // Remove "#SetBoolean:"
                        String[] setBoolParts = setBooleanLine.split("=");
                        if (setBoolParts.length == 2) {
                            String condName = setBoolParts[0].trim();
                            boolean targetValue = Boolean.parseBoolean(setBoolParts[1].trim());

                            System.out.println("      SetBoolean: " + condName + " = " + targetValue);

                            ResultValueRow rvRow = new ResultValueRow(condName);
                            rvRow.targetValue = targetValue;
                            currentField.resultValuesTableModel.addRow(rvRow);
                        }
                    }
                }

                // Add the field if it has conditions
                if (currentField.tableModel.getRowCount() > 0) {
                    conditionsFieldPanels.add(currentField);
                    conditionsFieldsContainer.add(currentField);
                    System.out.println("  -> ConditionsField added with " + currentField.tableModel.getRowCount() + " rows");
                }
            }

            // If no fields were loaded, add an empty one
            if (conditionsFieldPanels.isEmpty()) {
                System.out.println("No fields loaded - adding empty field");
                addNewConditionsField();
            } else {
                System.out.println("Loaded " + conditionsFieldPanels.size() + " ConditionsFields");
            }

            conditionsFieldsContainer.revalidate();
            conditionsFieldsContainer.repaint();
            System.out.println("=== LOAD COMPLETE ===\n");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR loading: " + e.getMessage());
        }
    }

    private void saveActions() {
        System.out.println("=== SAVING ACTIONS TO ITEM ===");
        System.out.println("Item: " + item.getName());
        System.out.println("Action: " + actionName);
        System.out.println("Number of ConditionsFields: " + conditionsFieldPanels.size());

        try {
            // Get existing handler to preserve #Dialog results
            KeyArea.ActionHandler oldHandler = item.getActions().get(actionName);

            // Clear and create new action handler
            item.getActions().remove(actionName);
            System.out.println("Removed old action handler");

            KeyArea.ActionHandler newHandler = new KeyArea.ActionHandler();

            // Build conditions and results from ConditionsFields
            int fieldIndex = 0;
            for (ConditionsFieldPanel fieldPanel : conditionsFieldPanels) {
                fieldIndex++;
                System.out.println("\n--- Processing ConditionsField #" + fieldIndex + " ---");
                System.out.println("Table has " + fieldPanel.tableModel.getRowCount() + " rows");

                // Collect all conditions from this field
                List<String> conditionParts = new ArrayList<>();
                for (int i = 0; i < fieldPanel.tableModel.getRowCount(); i++) {
                    ConditionRow row = fieldPanel.tableModel.getRow(i);
                    String condPart = row.conditionName + " = " + row.ifCurrentValue;
                    conditionParts.add(condPart);
                    System.out.println("  Condition: " + condPart);
                }

                if (conditionParts.isEmpty()) {
                    System.out.println("Skipping field (no conditions)");
                    continue;
                }

                // Combine all conditions with AND
                String combinedCondition = String.join(" AND ", conditionParts);
                System.out.println("Combined condition: " + combinedCondition);

                // Collect all results
                List<String> resultParts = new ArrayList<>();

                // Add #SetBoolean results from Result Values table
                for (int i = 0; i < fieldPanel.resultValuesTableModel.getRowCount(); i++) {
                    ResultValueRow rvRow = fieldPanel.resultValuesTableModel.getRow(i);
                    String setBoolean = "#SetBoolean:" + rvRow.conditionName + "=" + rvRow.targetValue;
                    resultParts.add(setBoolean);
                    System.out.println("  SetBoolean result: " + setBoolean);
                }

                // Preserve #Dialog results from old handler if they match this condition
                if (oldHandler != null && oldHandler.getConditionalResults() != null) {
                    for (Map.Entry<String, String> oldEntry : oldHandler.getConditionalResults().entrySet()) {
                        if (oldEntry.getKey().equals(combinedCondition)) {
                            // This is the same condition - extract #Dialog results
                            String[] oldResults = oldEntry.getValue().split("\\|\\|\\|");
                            for (String oldResult : oldResults) {
                                if (oldResult.trim().startsWith("#Dialog:")) {
                                    resultParts.add(oldResult.trim());
                                    System.out.println("  Preserved Dialog result: " + oldResult.trim());
                                }
                            }
                        }
                    }
                }

                // Combine all results with |||
                String combinedResult;
                if (!resultParts.isEmpty()) {
                    combinedResult = String.join("|||", resultParts);
                } else {
                    combinedResult = ""; // No results
                }
                System.out.println("Combined result: " + combinedResult);

                // Add to action handler
                if (!combinedResult.isEmpty()) {
                    newHandler.addConditionalResult(combinedCondition, combinedResult);
                    System.out.println("Added to action handler");
                }
            }

            // Add the action handler to the item
            item.addAction(actionName, newHandler);
            System.out.println("Action handler added to item");

            // Save the entire item to file
            ItemSaver.saveItemByName(item);
            System.out.println("Item saved to file");

            System.out.println("=== SAVE COMPLETE ===");

            JOptionPane.showMessageDialog(this, "Actions saved successfully to " + item.getName() + ".txt!", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
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
        private JTable resultValuesTable;
        private ResultValuesTableModel resultValuesTableModel;
        private JComboBox<String> addConditionCombo;
        private JComboBox<String> addResultValueCombo;

        public ConditionsFieldPanel() {
            initUI();
        }

        private void initUI() {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setLayout(new BorderLayout(5, 5));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

            // Title
            JLabel titleLabel = new JLabel("Conditions Field");
            titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(java.awt.Font.BOLD));
            add(titleLabel, BorderLayout.NORTH);

            // Center panel with both tables
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

            // Conditions Table
            JLabel conditionsLabel = new JLabel("Conditions:");
            conditionsLabel.setFont(conditionsLabel.getFont().deriveFont(12f).deriveFont(java.awt.Font.BOLD));
            centerPanel.add(conditionsLabel);

            tableModel = new ConditionsTableModel();
            conditionsTable = new JTable(tableModel);
            conditionsTable.setRowHeight(30);

            // Set up column widths
            conditionsTable.getColumnModel().getColumn(0).setPreferredWidth(400); // Conditions
            conditionsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // if Current Value

            // Set up custom renderers and editors
            conditionsTable.getColumnModel().getColumn(1).setCellRenderer(new IfCurrentValueRenderer());
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new IfCurrentValueEditor());

            JScrollPane conditionsScrollPane = new JScrollPane(conditionsTable);
            conditionsScrollPane.setPreferredSize(new Dimension(900, 120));
            centerPanel.add(conditionsScrollPane);

            // Spacing
            centerPanel.add(javax.swing.Box.createVerticalStrut(10));

            // Result Values Table
            JLabel resultValuesLabel = new JLabel("Result Values:");
            resultValuesLabel.setFont(resultValuesLabel.getFont().deriveFont(12f).deriveFont(java.awt.Font.BOLD));
            centerPanel.add(resultValuesLabel);

            resultValuesTableModel = new ResultValuesTableModel();
            resultValuesTable = new JTable(resultValuesTableModel);
            resultValuesTable.setRowHeight(30);

            // Set up column widths
            resultValuesTable.getColumnModel().getColumn(0).setPreferredWidth(300); // Condition
            resultValuesTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Target Value

            // Set up custom renderers and editors
            resultValuesTable.getColumnModel().getColumn(1).setCellRenderer(new RadioButtonRenderer());
            resultValuesTable.getColumnModel().getColumn(1).setCellEditor(new RadioButtonEditor());

            JScrollPane resultValuesScrollPane = new JScrollPane(resultValuesTable);
            resultValuesScrollPane.setPreferredSize(new Dimension(900, 100));
            centerPanel.add(resultValuesScrollPane);

            add(centerPanel, BorderLayout.CENTER);

            // Bottom buttons panel
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

            // Conditions buttons
            JPanel conditionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            addConditionCombo = new JComboBox<>();
            refreshAvailableConditions();
            addConditionCombo.setPreferredSize(new Dimension(150, 25));
            conditionsButtonsPanel.add(new JLabel("Add Condition:"));
            conditionsButtonsPanel.add(addConditionCombo);

            JButton addConditionBtn = new JButton("Add");
            addConditionBtn.addActionListener(e -> addCondition());
            conditionsButtonsPanel.add(addConditionBtn);

            JButton removeConditionBtn = new JButton("Remove Selected Condition");
            removeConditionBtn.addActionListener(e -> removeSelectedCondition());
            conditionsButtonsPanel.add(removeConditionBtn);

            buttonsPanel.add(conditionsButtonsPanel);

            // Result Values buttons
            JPanel resultValuesButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            addResultValueCombo = new JComboBox<>();
            refreshAvailableResultValues();
            addResultValueCombo.setPreferredSize(new Dimension(150, 25));
            resultValuesButtonsPanel.add(new JLabel("Add Result Value:"));
            resultValuesButtonsPanel.add(addResultValueCombo);

            JButton addResultValueBtn = new JButton("Add");
            addResultValueBtn.addActionListener(e -> addResultValue());
            resultValuesButtonsPanel.add(addResultValueBtn);

            JButton removeResultValueBtn = new JButton("Remove Selected Result Value");
            removeResultValueBtn.addActionListener(e -> removeSelectedResultValue());
            resultValuesButtonsPanel.add(removeResultValueBtn);

            buttonsPanel.add(resultValuesButtonsPanel);

            // Field control button
            JPanel fieldButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton removeFieldBtn = new JButton("Remove this Conditions Field");
            removeFieldBtn.addActionListener(e -> removeThisField());
            fieldButtonsPanel.add(removeFieldBtn);

            buttonsPanel.add(fieldButtonsPanel);

            add(buttonsPanel, BorderLayout.SOUTH);
        }

        private void refreshAvailableConditions() {
            addConditionCombo.removeAllItems();
            Set<String> allConditions = Conditions.getAllConditionNames();
            for (String conditionName : allConditions) {
                addConditionCombo.addItem(conditionName);
            }
        }

        private void refreshAvailableResultValues() {
            addResultValueCombo.removeAllItems();

            // Add all conditions from Conditions (includes isInInventory_* automatically)
            Set<String> allConditions = Conditions.getAllConditionNames();
            for (String conditionName : allConditions) {
                addResultValueCombo.addItem(conditionName);
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
                tableModel.addRow(row);
            }
        }

        private void addResultValue() {
            String selectedCondition = (String) addResultValueCombo.getSelectedItem();
            if (selectedCondition != null && !selectedCondition.trim().isEmpty()) {
                // Check if condition already exists in result values
                for (int i = 0; i < resultValuesTableModel.getRowCount(); i++) {
                    if (resultValuesTableModel.getRow(i).conditionName.equals(selectedCondition)) {
                        JOptionPane.showMessageDialog(this,
                            "Result value for this condition already exists",
                            "Duplicate", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                ResultValueRow row = new ResultValueRow(selectedCondition);
                row.targetValue = true; // Default to true
                resultValuesTableModel.addRow(row);
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

        private void removeSelectedResultValue() {
            int selectedRow = resultValuesTable.getSelectedRow();
            if (selectedRow >= 0) {
                resultValuesTableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a result value to remove",
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

        // Table Models
        class ConditionsTableModel extends AbstractTableModel {
            private List<ConditionRow> rows = new ArrayList<>();
            private String[] columnNames = {"Conditions", "if Current Value"};

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
                    case 1: return row.ifCurrentValue;
                    default: return null;
                }
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                ConditionRow row = rows.get(rowIndex);

                switch (columnIndex) {
                    case 1:
                        row.ifCurrentValue = (String) value;
                        break;
                }

                fireTableCellUpdated(rowIndex, columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                // Only column 1 (if current value) is editable
                return columnIndex == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        }

        // Result Values Table Model
        class ResultValuesTableModel extends AbstractTableModel {
            private List<ResultValueRow> rows = new ArrayList<>();
            private String[] columnNames = {"Condition", "Target Value"};

            public void addRow(ResultValueRow row) {
                rows.add(row);
                fireTableDataChanged();
            }

            public void removeRow(int rowIndex) {
                rows.remove(rowIndex);
                fireTableDataChanged();
            }

            public ResultValueRow getRow(int rowIndex) {
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
                ResultValueRow row = rows.get(rowIndex);

                switch (columnIndex) {
                    case 0: return row.conditionName;
                    case 1: return row.targetValue;
                    default: return null;
                }
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                ResultValueRow row = rows.get(rowIndex);

                if (columnIndex == 1) {
                    row.targetValue = (Boolean) value;
                }

                fireTableCellUpdated(rowIndex, columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                // Only column 1 (target value) is editable
                return columnIndex == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                return String.class;
            }
        }
    }

    // Data classes for table rows
    class ConditionRow {
        String conditionName;
        String ifCurrentValue = "true"; // "true" or "false"

        public ConditionRow(String conditionName) {
            this.conditionName = conditionName;
        }

        public boolean getCurrentValue() {
            return Conditions.getCondition(conditionName);
        }
    }

    class ResultValueRow {
        String conditionName;
        Boolean targetValue = true;

        public ResultValueRow(String conditionName) {
            this.conditionName = conditionName;
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

        public IfCurrentValueRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);

            add(trueButton);
            add(falseButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            String strValue = value != null ? value.toString() : "true";

            trueButton.setSelected("true".equals(strValue));
            falseButton.setSelected("false".equals(strValue));

            return this;
        }
    }

    // Custom cell editors (shared across all ConditionsFields)
    class IfCurrentValueEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JRadioButton trueButton;
        private JRadioButton falseButton;

        public IfCurrentValueEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);

            panel.add(trueButton);
            panel.add(falseButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            String strValue = value != null ? value.toString() : "true";

            trueButton.setSelected("true".equals(strValue));
            falseButton.setSelected("false".equals(strValue));

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            if (trueButton.isSelected()) {
                return "true";
            } else {
                return "false";
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

    // RadioButton renderer and editor for Result Values (true/false only)
    class RadioButtonRenderer extends JPanel implements TableCellRenderer {
        private JRadioButton trueButton;
        private JRadioButton falseButton;

        public RadioButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);

            add(trueButton);
            add(falseButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if (value == null) {
                trueButton.setSelected(true);
                falseButton.setSelected(false);
            } else {
                boolean boolValue = (Boolean) value;
                trueButton.setSelected(boolValue);
                falseButton.setSelected(!boolValue);
            }

            return this;
        }
    }

    class RadioButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JRadioButton trueButton;
        private JRadioButton falseButton;

        public RadioButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            trueButton = new JRadioButton("true");
            falseButton = new JRadioButton("false");

            ButtonGroup group = new ButtonGroup();
            group.add(trueButton);
            group.add(falseButton);

            panel.add(trueButton);
            panel.add(falseButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            if (value == null) {
                trueButton.setSelected(true);
                falseButton.setSelected(false);
            } else {
                boolean boolValue = (Boolean) value;
                trueButton.setSelected(boolValue);
                falseButton.setSelected(!boolValue);
            }

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return trueButton.isSelected();
        }
    }
}
