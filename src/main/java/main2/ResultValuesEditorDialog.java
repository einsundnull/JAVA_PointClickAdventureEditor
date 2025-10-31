package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Dialog zum Bearbeiten der Result Values
 * Ermöglicht Auswahl mehrerer Conditions und deren Zielwerte (true/false)
 */
public class ResultValuesEditorDialog extends JDialog {

    private JTable resultValuesTable;
    private ResultValuesTableModel tableModel;
    private Map<String, Boolean> resultValues; // conditionName -> targetValue
    private boolean cancelled = false;

    public ResultValuesEditorDialog(Frame owner, Map<String, Boolean> initialValues) {
        super(owner, "Result Values Editor", true);
        this.resultValues = new HashMap<>(initialValues);

        initUI();
        loadData();

        setSize(600, 400);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Info label
        JLabel infoLabel = new JLabel("Select conditions and their target values:");
        infoLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(infoLabel, BorderLayout.NORTH);

        // Table with all available conditions
        tableModel = new ResultValuesTableModel();
        resultValuesTable = new JTable(tableModel);
        resultValuesTable.setRowHeight(30);

        // Set up column widths
        resultValuesTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Use
        resultValuesTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Condition
        resultValuesTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Target Value

        // Set up custom renderers and editors
        resultValuesTable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        resultValuesTable.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor());

        resultValuesTable.getColumnModel().getColumn(2).setCellRenderer(new RadioButtonRenderer());
        resultValuesTable.getColumnModel().getColumn(2).setCellEditor(new RadioButtonEditor());

        JScrollPane scrollPane = new JScrollPane(resultValuesTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> {
            saveData();
            cancelled = false;
            dispose();
        });
        buttonPanel.add(okBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            cancelled = true;
            dispose();
        });
        buttonPanel.add(cancelBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadData() {
        // Load all available conditions
        Set<String> allConditions = Conditions.getAllConditionNames();

        for (String conditionName : allConditions) {
            ResultValueRow row = new ResultValueRow(conditionName);

            // Check if this condition is in the initial values
            if (resultValues.containsKey(conditionName)) {
                row.use = true;
                row.targetValue = resultValues.get(conditionName);
            }

            tableModel.addRow(row);
        }
    }

    private void saveData() {
        resultValues.clear();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ResultValueRow row = tableModel.getRow(i);
            if (row.use && row.targetValue != null) {
                resultValues.put(row.conditionName, row.targetValue);
            }
        }
    }

    public Map<String, Boolean> getResultValues() {
        return cancelled ? null : new HashMap<>(resultValues);
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    // Table Model
    class ResultValuesTableModel extends AbstractTableModel {
        private List<ResultValueRow> rows = new ArrayList<>();
        private String[] columnNames = {"Use", "Condition", "Target Value"};

        public void addRow(ResultValueRow row) {
            rows.add(row);
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
                case 0: return row.use;
                case 1: return row.conditionName;
                case 2: return row.targetValue;
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ResultValueRow row = rows.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    row.use = (Boolean) value;
                    break;
                case 2:
                    row.targetValue = (Boolean) value;
                    break;
            }

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Columns 0 (use) and 2 (target value) are editable
            return columnIndex == 0 || columnIndex == 2;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: return Boolean.class;
                case 2: return Boolean.class;
                default: return String.class;
            }
        }
    }

    // Data class for table rows
    class ResultValueRow {
        String conditionName;
        boolean use = false;
        Boolean targetValue = null;

        public ResultValueRow(String conditionName) {
            this.conditionName = conditionName;
        }
    }

    // Custom cell renderers
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
                trueButton.setSelected(false);
                falseButton.setSelected(false);
            } else {
                boolean boolValue = (Boolean) value;
                trueButton.setSelected(boolValue);
                falseButton.setSelected(!boolValue);
            }

            return this;
        }
    }

    // Custom cell editors
    class CheckBoxEditor extends javax.swing.DefaultCellEditor {
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
                trueButton.setSelected(false);
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
            if (trueButton.isSelected()) {
                return true;
            } else if (falseButton.isSelected()) {
                return false;
            }
            return null;
        }
    }
}
