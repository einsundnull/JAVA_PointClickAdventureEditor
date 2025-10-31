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
        System.out.println("=== LOADING ITEM ACTIONS ===");
        System.out.println("File: " + actionsFile.getAbsolutePath());
        System.out.println("Action: " + actionName);

        if (!actionsFile.exists()) {
            System.out.println("File does not exist - creating empty field");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(actionsFile.toPath());
            System.out.println("Loaded " + lines.size() + " lines from file");

            // Clear existing fields
            for (ConditionsFieldPanel panel : conditionsFieldPanels) {
                conditionsFieldsContainer.remove(panel);
            }
            conditionsFieldPanels.clear();

            // Parse new format
            int lineNumber = 0;
            ConditionsFieldPanel currentField = null;
            String currentSection = "";
            String processName = "";

            for (String line : lines) {
                lineNumber++;
                String trimmedLine = line.trim();
                System.out.println("Line " + lineNumber + ": " + trimmedLine);

                if (trimmedLine.startsWith("#" + actionName + ":")) {
                    System.out.println("  -> Found action header!");
                    // Start a new ConditionsField
                    currentField = new ConditionsFieldPanel();
                    currentSection = "header";
                } else if (trimmedLine.equals("-conditions")) {
                    System.out.println("  -> Conditions section");
                    currentSection = "conditions";
                } else if (trimmedLine.equals("-process")) {
                    System.out.println("  -> Process section");
                    currentSection = "process";
                } else if (trimmedLine.startsWith("--") && "conditions".equals(currentSection) && currentField != null) {
                    // Parse condition line: --conditionName=ifValue -> =resultValue
                    String condLine = trimmedLine.substring(2); // Remove "--"
                    System.out.println("  -> Parsing condition: " + condLine);

                    // Split by "->"
                    String[] parts = condLine.split("->");
                    if (parts.length >= 1) {
                        String condPart = parts[0].trim(); // e.g. "lookedAtCupAtBeach=false"
                        String resultPart = parts.length > 1 ? parts[1].trim() : ""; // e.g. "=true"

                        // Parse condition part
                        String[] condSplit = condPart.split("=");
                        if (condSplit.length == 2) {
                            String condName = condSplit[0].trim();
                            String ifValue = condSplit[1].trim();
                            System.out.println("    Condition: " + condName + ", ifValue: " + ifValue);

                            // Create row
                            ConditionRow row = new ConditionRow(condName);
                            row.use = true;
                            row.ifCurrentValue = ifValue;
                            row.processName = processName;

                            currentField.tableModel.addRow(row);
                            System.out.println("    -> Added to table");

                            // Parse result part
                            if (resultPart.startsWith("=") && resultPart.length() > 1) {
                                String resultValue = resultPart.substring(1).trim();
                                boolean boolResult = Boolean.parseBoolean(resultValue);
                                currentField.resultValues.put(condName, boolResult);
                                System.out.println("    Result value: " + boolResult);
                            }
                        }
                    }
                } else if ("process".equals(currentSection) && !trimmedLine.isEmpty() && currentField != null) {
                    // Process name
                    processName = trimmedLine;
                    System.out.println("  -> Process name: " + processName);
                    // Set process name on all rows
                    for (int i = 0; i < currentField.tableModel.getRowCount(); i++) {
                        currentField.tableModel.getRow(i).processName = processName;
                    }
                } else if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    // End of current action or start of new one
                    if (currentField != null && currentField.tableModel.getRowCount() > 0) {
                        conditionsFieldPanels.add(currentField);
                        conditionsFieldsContainer.add(currentField);
                        System.out.println("  -> ConditionsField added with " + currentField.tableModel.getRowCount() + " rows");
                        currentField = null;
                        currentSection = "";
                        processName = "";
                    }
                }
            }

            // Add last field if any
            if (currentField != null && currentField.tableModel.getRowCount() > 0) {
                conditionsFieldPanels.add(currentField);
                conditionsFieldsContainer.add(currentField);
                System.out.println("  -> Last ConditionsField added with " + currentField.tableModel.getRowCount() + " rows");
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveActions() {
        File actionsFile = new File("resources/actions/" + item.getName() + ".txt");
        System.out.println("=== SAVING ITEM ACTIONS ===");
        System.out.println("File: " + actionsFile.getAbsolutePath());
        System.out.println("Action: " + actionName);
        System.out.println("Number of ConditionsFields: " + conditionsFieldPanels.size());

        try {
            List<String> lines = new ArrayList<>();

            // Read existing lines
            if (actionsFile.exists()) {
                lines = new ArrayList<>(Files.readAllLines(actionsFile.toPath()));
                System.out.println("Loaded " + lines.size() + " existing lines");
            } else {
                System.out.println("File does not exist, creating new");
            }

            // Remove old action lines for this action
            int removedLines = 0;
            for (int i = lines.size() - 1; i >= 0; i--) {
                if (lines.get(i).trim().startsWith("#" + actionName + ":")) {
                    lines.remove(i);
                    removedLines++;
                }
            }
            System.out.println("Removed " + removedLines + " old action lines");

            // Build new action lines from ConditionsFields
            int fieldIndex = 0;
            for (ConditionsFieldPanel fieldPanel : conditionsFieldPanels) {
                fieldIndex++;
                System.out.println("\n--- Processing ConditionsField #" + fieldIndex + " ---");
                System.out.println("Table has " + fieldPanel.tableModel.getRowCount() + " rows");

                List<ConditionRow> usedRows = new ArrayList<>();
                for (int i = 0; i < fieldPanel.tableModel.getRowCount(); i++) {
                    ConditionRow row = fieldPanel.tableModel.getRow(i);
                    System.out.println("  Row " + i + ": " + row.conditionName +
                                     ", use=" + row.use +
                                     ", ifCurrentValue=" + row.ifCurrentValue +
                                     ", processName=" + row.processName);
                    if (row.use) {
                        usedRows.add(row);
                    }
                }

                System.out.println("Used rows: " + usedRows.size());
                System.out.println("Result values: " + fieldPanel.resultValues.size());
                for (Map.Entry<String, Boolean> entry : fieldPanel.resultValues.entrySet()) {
                    System.out.println("  " + entry.getKey() + " = " + entry.getValue());
                }

                if (usedRows.isEmpty()) {
                    System.out.println("Skipping field (no used rows)");
                    continue; // Skip empty fields
                }

                // Build new format
                System.out.println("Building new format...");
                List<String> actionLines = new ArrayList<>();

                // Add header
                actionLines.add("#" + actionName + ":");
                actionLines.add("-conditions");

                String processName = "";

                // Add condition lines
                for (ConditionRow row : usedRows) {
                    System.out.println("  Processing row: " + row.conditionName + ", ifCurrentValue=" + row.ifCurrentValue);

                    // Skip conditions with "ignore"
                    if (!"ignore".equals(row.ifCurrentValue)) {
                        StringBuilder condLine = new StringBuilder();
                        condLine.append("--").append(row.conditionName).append("=").append(row.ifCurrentValue);

                        // Add result value if exists in resultValues map
                        if (fieldPanel.resultValues.containsKey(row.conditionName)) {
                            Boolean resultValue = fieldPanel.resultValues.get(row.conditionName);
                            condLine.append(" -> =").append(resultValue);
                            System.out.println("    Added with result: " + resultValue);
                        } else {
                            condLine.append(" ->");
                            System.out.println("    Added without result");
                        }

                        actionLines.add(condLine.toString());
                    } else {
                        System.out.println("    Skipped (ignore)");
                    }

                    // Get process name (from first row that has one)
                    if (!row.processName.isEmpty() && processName.isEmpty()) {
                        processName = row.processName;
                        System.out.println("    Process name: " + processName);
                    }
                }

                // Add process section
                actionLines.add("-process");
                if (!processName.isEmpty()) {
                    actionLines.add(processName);
                }

                // Add all lines
                if (actionLines.size() > 2) { // More than just header and -conditions
                    for (String line : actionLines) {
                        lines.add(line);
                        System.out.println("Added line: " + line);
                    }
                }
            }

            // Write back to file
            actionsFile.getParentFile().mkdirs();
            Files.write(actionsFile.toPath(), lines);

            System.out.println("File saved successfully with " + lines.size() + " total lines");
            System.out.println("=== SAVE COMPLETE ===");

            JOptionPane.showMessageDialog(this, "Actions saved successfully!\n\nFile: " + actionsFile.getAbsolutePath(), "Saved",
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
            // Get the owner of the main dialog
            Frame owner = null;
            if (NewItemActionsEditorDialog.this.getOwner() instanceof Frame) {
                owner = (Frame) NewItemActionsEditorDialog.this.getOwner();
            }

            ResultValuesEditorDialog dialog = new ResultValuesEditorDialog(owner, resultValues);
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
