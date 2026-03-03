package main;

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
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

// New UI imports
import main.ui.theme.ThemeManager;
import main.ui.theme.Spacing;
import main.ui.components.button.AppButton;

/**
 * New Actions Editor with multiple ConditionsFields
 * Each ConditionsField is a group of conditions with their own result values and process
 */
public class NewActionsEditorDialog extends JDialog {

    private KeyArea keyArea;
    private String actionName;
    private JPanel conditionsFieldsContainer;
    private List<ConditionsFieldPanel> conditionsFieldPanels = new ArrayList<>();

    public NewActionsEditorDialog(Frame owner, KeyArea keyArea, String actionName) {
        super(owner, "KeyArea Actions Editor - " + actionName, true);
        this.keyArea = keyArea;
        this.actionName = actionName;

        initUI();
        loadExistingActions();

        setSize(1000, 700);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();

        setLayout(new BorderLayout(Spacing.SM, Spacing.SM));

        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(c.getBackgroundPanel());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.XS, Spacing.SM));

        JLabel titleLabel = new JLabel("⚙️ Actions Editor: " + actionName);
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

        // Top panel with manage conditions button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.SM));
        topPanel.setBackground(c.getBackgroundRoot());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, Spacing.SM, 0, Spacing.SM));

        AppButton manageConditionsBtn = new AppButton("🎛️ Manage Conditions", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
        manageConditionsBtn.addActionListener(e -> openConditionsManager());
        topPanel.add(manageConditionsBtn);

        add(topPanel, BorderLayout.NORTH);

        // Center panel with ConditionsFields
        conditionsFieldsContainer = new JPanel();
        conditionsFieldsContainer.setLayout(new BoxLayout(conditionsFieldsContainer, BoxLayout.Y_AXIS));
        conditionsFieldsContainer.setBackground(c.getBackgroundRoot());

        JScrollPane scrollPane = new JScrollPane(conditionsFieldsContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with buttons - Use AppButton
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(c.getBackgroundElevated());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, c.getBorderDefault()),
            BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
        ));

        // Add New ConditionsField button on the left
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.SM));
        leftPanel.setBackground(c.getBackgroundElevated());
        AppButton addFieldBtn = new AppButton("Add New Conditions Field", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
        addFieldBtn.addActionListener(e -> addNewConditionsField());
        leftPanel.add(addFieldBtn);
        bottomPanel.add(leftPanel, BorderLayout.WEST);

        // Save/Cancel buttons on the right
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.XS, Spacing.SM));
        rightPanel.setBackground(c.getBackgroundElevated());
        AppButton saveBtn = new AppButton("Save", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
        saveBtn.addActionListener(e -> saveActions());
        rightPanel.add(saveBtn);

        AppButton cancelBtn = new AppButton("Cancel", AppButton.Variant.GHOST, AppButton.Size.SMALL);
        cancelBtn.addActionListener(e -> dispose());
        rightPanel.add(cancelBtn);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

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
        File actionsFile = new File("resources/actions/" + keyArea.getName() + ".txt");
        System.out.println("=== LOADING ACTIONS ===");
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
                            row.ifCurrentValue = ifValue;

                            currentField.tableModel.addRow(row);
                            System.out.println("    -> Added to table");

                            // Parse result part
                            if (resultPart.startsWith("=") && resultPart.length() > 1) {
                                String resultValue = resultPart.substring(1).trim();
                                boolean boolResult = Boolean.parseBoolean(resultValue);

                                // Add to result values table
                                ResultValueRow rvRow = new ResultValueRow(condName);
                                rvRow.targetValue = boolResult;
                                currentField.resultValuesTableModel.addRow(rvRow);
                                System.out.println("    Result value: " + boolResult);
                            }
                        }
                    }
                } else if ("process".equals(currentSection) && !trimmedLine.isEmpty() && currentField != null) {
                    // Process name
                    processName = trimmedLine;
                    System.out.println("  -> Process name (ignored): " + processName);
                    // Process names are no longer stored in conditions
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
            System.out.println("ERROR loading: " + e.getMessage());
        }
    }

    private void saveActions() {
        File actionsFile = new File("resources/actions/" + keyArea.getName() + ".txt");
        System.out.println("=== SAVING ACTIONS ===");
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

                // All rows are used (no "use" checkbox anymore)
                List<ConditionRow> usedRows = new ArrayList<>();
                for (int i = 0; i < fieldPanel.tableModel.getRowCount(); i++) {
                    ConditionRow row = fieldPanel.tableModel.getRow(i);
                    System.out.println("  Row " + i + ": " + row.conditionName +
                                     ", ifCurrentValue=" + row.ifCurrentValue);
                    usedRows.add(row);
                }

                System.out.println("Total rows: " + usedRows.size());
                System.out.println("Result values: " + fieldPanel.resultValuesTableModel.getRowCount());
                for (int i = 0; i < fieldPanel.resultValuesTableModel.getRowCount(); i++) {
                    ResultValueRow rvRow = fieldPanel.resultValuesTableModel.getRow(i);
                    System.out.println("  " + rvRow.conditionName + " = " + rvRow.targetValue);
                }

                if (usedRows.isEmpty()) {
                    System.out.println("Skipping field (no rows)");
                    continue; // Skip empty fields
                }

                // Build result values map for quick lookup
                Map<String, Boolean> resultValuesMap = new HashMap<>();
                for (int i = 0; i < fieldPanel.resultValuesTableModel.getRowCount(); i++) {
                    ResultValueRow rvRow = fieldPanel.resultValuesTableModel.getRow(i);
                    resultValuesMap.put(rvRow.conditionName, rvRow.targetValue);
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

                        // Add result value if exists in result values table
                        if (resultValuesMap.containsKey(row.conditionName)) {
                            Boolean resultValue = resultValuesMap.get(row.conditionName);
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

                }

                // No longer using -process section

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
        if (getOwner() instanceof EditorMain) {
            EditorMain editorWindow = (EditorMain) getOwner();
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
            var c = ThemeManager.colors();
            var t = ThemeManager.typography();

            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c.getBorderDefault(), 2),
                BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
            ));
            setLayout(new BorderLayout(Spacing.XS, Spacing.XS));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
            setBackground(c.getBackgroundPanel());

            // Title
            JLabel titleLabel = new JLabel("Conditions Field");
            titleLabel.setFont(t.semiboldBase());
            titleLabel.setForeground(c.getTextPrimary());
            add(titleLabel, BorderLayout.NORTH);

            // Center panel with both tables
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setBackground(c.getBackgroundPanel());

            // Conditions Table
            JLabel conditionsLabel = new JLabel("Conditions:");
            conditionsLabel.setFont(t.semiboldSm());
            conditionsLabel.setForeground(c.getTextSecondary());
            centerPanel.add(conditionsLabel);

            tableModel = new ConditionsTableModel();
            conditionsTable = new JTable(tableModel);
            conditionsTable.setRowHeight(Spacing.TABLE_ROW_HEIGHT - 4);

            // Set up column widths
            conditionsTable.getColumnModel().getColumn(0).setPreferredWidth(400); // Conditions
            conditionsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // if Current Value

            // Set up custom renderers and editors
            conditionsTable.getColumnModel().getColumn(1).setCellRenderer(new IfCurrentValueRenderer());
            conditionsTable.getColumnModel().getColumn(1).setCellEditor(new IfCurrentValueEditor());

            JScrollPane conditionsScrollPane = new JScrollPane(conditionsTable);
            conditionsScrollPane.setPreferredSize(new Dimension(900, 120));
            conditionsScrollPane.setBorder(null);
            centerPanel.add(conditionsScrollPane);

            // Spacing
            centerPanel.add(javax.swing.Box.createVerticalStrut(Spacing.SM));

            // Result Values Table
            JLabel resultValuesLabel = new JLabel("Result Values:");
            resultValuesLabel.setFont(t.semiboldSm());
            resultValuesLabel.setForeground(c.getTextSecondary());
            centerPanel.add(resultValuesLabel);

            resultValuesTableModel = new ResultValuesTableModel();
            resultValuesTable = new JTable(resultValuesTableModel);
            resultValuesTable.setRowHeight(Spacing.TABLE_ROW_HEIGHT - 4);

            // Set up column widths
            resultValuesTable.getColumnModel().getColumn(0).setPreferredWidth(300); // Condition
            resultValuesTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Target Value

            // Set up custom renderers and editors
            resultValuesTable.getColumnModel().getColumn(1).setCellRenderer(new RadioButtonRenderer());
            resultValuesTable.getColumnModel().getColumn(1).setCellEditor(new RadioButtonEditor());

            JScrollPane resultValuesScrollPane = new JScrollPane(resultValuesTable);
            resultValuesScrollPane.setPreferredSize(new Dimension(900, 100));
            resultValuesScrollPane.setBorder(null);
            centerPanel.add(resultValuesScrollPane);

            add(centerPanel, BorderLayout.CENTER);

            // Bottom buttons panel
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
            buttonsPanel.setBackground(c.getBackgroundPanel());

            // Conditions buttons - Use AppButton
            JPanel conditionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.XS));
            conditionsButtonsPanel.setBackground(c.getBackgroundPanel());
            addConditionCombo = new JComboBox<>();
            refreshAvailableConditions();
            addConditionCombo.setPreferredSize(new Dimension(150, Spacing.INPUT_HEIGHT));
            conditionsButtonsPanel.add(new JLabel("Add Condition:"));
            conditionsButtonsPanel.add(addConditionCombo);

            AppButton addConditionBtn = new AppButton("Add", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
            addConditionBtn.addActionListener(e -> addCondition());
            conditionsButtonsPanel.add(addConditionBtn);

            AppButton removeConditionBtn = new AppButton("Remove Selected", AppButton.Variant.DANGER, AppButton.Size.SMALL);
            removeConditionBtn.addActionListener(e -> removeSelectedCondition());
            conditionsButtonsPanel.add(removeConditionBtn);

            buttonsPanel.add(conditionsButtonsPanel);

            // Result Values buttons - Use AppButton
            JPanel resultValuesButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.XS));
            resultValuesButtonsPanel.setBackground(c.getBackgroundPanel());
            addResultValueCombo = new JComboBox<>();
            refreshAvailableResultValues();
            addResultValueCombo.setPreferredSize(new Dimension(150, Spacing.INPUT_HEIGHT));
            resultValuesButtonsPanel.add(new JLabel("Add Result Value:"));
            resultValuesButtonsPanel.add(addResultValueCombo);

            AppButton addResultValueBtn = new AppButton("Add", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
            addResultValueBtn.addActionListener(e -> addResultValue());
            resultValuesButtonsPanel.add(addResultValueBtn);

            AppButton removeResultValueBtn = new AppButton("Remove Selected", AppButton.Variant.DANGER, AppButton.Size.SMALL);
            removeResultValueBtn.addActionListener(e -> removeSelectedResultValue());
            resultValuesButtonsPanel.add(removeResultValueBtn);

            buttonsPanel.add(resultValuesButtonsPanel);

            // Field control button - Use AppButton
            JPanel fieldButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.XS));
            fieldButtonsPanel.setBackground(c.getBackgroundPanel());
            AppButton removeFieldBtn = new AppButton("Remove this Conditions Field", AppButton.Variant.DANGER, AppButton.Size.SMALL);
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

                if (columnIndex == 1) {
                    row.ifCurrentValue = (String) value;
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
