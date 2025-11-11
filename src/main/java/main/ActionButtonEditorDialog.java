package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Dialog zum Bearbeiten der Action Buttons
 */
public class ActionButtonEditorDialog extends JDialog {
    private EditorMain parent;
    private JTable buttonsTable;
    private DefaultTableModel tableModel;
    private List<ActionButton> actionButtons;

    // Edit fields
    private JTextField textField;
    private JTextField hoverTextField;
    private JCheckBox showHoverCheckbox;
    private JTextField imagePathField;
    private JCheckBox useImageCheckbox;
    private JTextField hoverFormatField;
    private JComboBox<String> cursorTypeCombo;
    private JTextField customCursorPathField;

    private int selectedRow = -1;

    public ActionButtonEditorDialog(EditorMain parent) {
        super(parent, "Action Buttons Editor", false); // Non-modal
        this.parent = parent;

        setSize(900, 700);
        setLocationRelativeTo(parent);

        initUI();
        loadActionButtons();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("⚙️ Action Buttons Editor");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Table
        String[] columns = {"Order", "Text", "Cursor", "Hover Format"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Not directly editable
            }
        };

        buttonsTable = new JTable(tableModel);
        buttonsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        buttonsTable.setRowHeight(25);
        buttonsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        buttonsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        buttonsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        buttonsTable.getColumnModel().getColumn(3).setPreferredWidth(200);

        buttonsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onButtonSelected();
            }
        });

        JScrollPane tableScroll = new JScrollPane(buttonsTable);
        tableScroll.setPreferredSize(new Dimension(0, 200));

        // Control panel (add, remove, reorder)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("➕ Add Button");
        addBtn.addActionListener(e -> addNewButton());
        controlPanel.add(addBtn);

        JButton removeBtn = new JButton("🗑️ Remove Selected");
        removeBtn.addActionListener(e -> removeSelectedButton());
        controlPanel.add(removeBtn);

        JButton upBtn = new JButton("⬆ Move Up");
        upBtn.addActionListener(e -> moveButtonUp());
        controlPanel.add(upBtn);

        JButton downBtn = new JButton("⬇ Move Down");
        downBtn.addActionListener(e -> moveButtonDown());
        controlPanel.add(downBtn);

        // Top section
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(tableScroll, BorderLayout.CENTER);
        topSection.add(controlPanel, BorderLayout.SOUTH);

        mainPanel.add(topSection, BorderLayout.NORTH);

        // Edit panel
        JPanel editPanel = new JPanel(new BorderLayout());
        editPanel.setBorder(BorderFactory.createTitledBorder("Edit Selected Button"));

        JPanel fieldsPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Text
        fieldsPanel.add(new JLabel("Button Text:"));
        textField = new JTextField();
        fieldsPanel.add(textField);

        // Cursor Type
        fieldsPanel.add(new JLabel("Cursor Type:"));
        cursorTypeCombo = new JComboBox<>(new String[]{"HAND", "CROSSHAIR", "DEFAULT", "TEXT", "MOVE", "WAIT", "CUSTOM"});
        fieldsPanel.add(cursorTypeCombo);

        // Custom Cursor Path
        fieldsPanel.add(new JLabel("Custom Cursor Path:"));
        customCursorPathField = new JTextField();
        fieldsPanel.add(customCursorPathField);

        // Hover Format
        fieldsPanel.add(new JLabel("Hover Format:"));
        hoverFormatField = new JTextField();
        fieldsPanel.add(hoverFormatField);

        // Hover Text
        fieldsPanel.add(new JLabel("Hover Text:"));
        hoverTextField = new JTextField();
        fieldsPanel.add(hoverTextField);

        // Show Hover Text
        fieldsPanel.add(new JLabel("Show Hover Text:"));
        showHoverCheckbox = new JCheckBox();
        fieldsPanel.add(showHoverCheckbox);

        // Image Path
        fieldsPanel.add(new JLabel("Image Path:"));
        imagePathField = new JTextField();
        fieldsPanel.add(imagePathField);

        // Use Image
        fieldsPanel.add(new JLabel("Use Image:"));
        useImageCheckbox = new JCheckBox();
        fieldsPanel.add(useImageCheckbox);

        editPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Save button for edit panel
        JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveEditBtn = new JButton("💾 Apply Changes");
        saveEditBtn.addActionListener(e -> applyChangesToSelected());
        editButtonPanel.add(saveEditBtn);
        editPanel.add(editButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(editPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Combined bottom panel (info + buttons)
        JPanel combinedBottomPanel = new JPanel();
        combinedBottomPanel.setLayout(new BoxLayout(combinedBottomPanel, BoxLayout.Y_AXIS));

        // Info label
        JLabel infoLabel = new JLabel("<html><b>Hover Format placeholders:</b> {action} {target} {item}</html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        infoLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        combinedBottomPanel.add(infoLabel);

        // Button panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottomPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JButton saveAllBtn = new JButton("💾 Save All to File");
        saveAllBtn.addActionListener(e -> saveAllButtons());
        bottomPanel.add(saveAllBtn);

        JButton resetBtn = new JButton("🔄 Reset to Defaults");
        resetBtn.addActionListener(e -> resetToDefaults());
        bottomPanel.add(resetBtn);

        JButton closeBtn = new JButton("✓ Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);

        combinedBottomPanel.add(bottomPanel);

        add(combinedBottomPanel, BorderLayout.SOUTH);
    }

    private void loadActionButtons() {
        actionButtons = ActionButtonLoader.loadActionButtons();
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);

        // Sort by order
        actionButtons.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        for (ActionButton btn : actionButtons) {
            tableModel.addRow(new Object[]{
                btn.getOrder(),
                btn.getText(),
                btn.getCursorType(),
                btn.getHoverFormat()
            });
        }
    }

    private void onButtonSelected() {
        selectedRow = buttonsTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < actionButtons.size()) {
            ActionButton btn = actionButtons.get(selectedRow);

            textField.setText(btn.getText());
            hoverTextField.setText(btn.getHoverText());
            showHoverCheckbox.setSelected(btn.isShowHoverText());
            imagePathField.setText(btn.getImagePath());
            useImageCheckbox.setSelected(btn.isUseImage());
            hoverFormatField.setText(btn.getHoverFormat());
            cursorTypeCombo.setSelectedItem(btn.getCursorType());
            customCursorPathField.setText(btn.getCustomCursorPath());
        }
    }

    private void applyChangesToSelected() {
        if (selectedRow < 0 || selectedRow >= actionButtons.size()) {
            JOptionPane.showMessageDialog(this, "Please select a button first!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ActionButton btn = actionButtons.get(selectedRow);
        btn.setText(textField.getText());
        btn.setHoverText(hoverTextField.getText());
        btn.setShowHoverText(showHoverCheckbox.isSelected());
        btn.setImagePath(imagePathField.getText());
        btn.setUseImage(useImageCheckbox.isSelected());
        btn.setHoverFormat(hoverFormatField.getText());
        btn.setCursorType((String) cursorTypeCombo.getSelectedItem());
        btn.setCustomCursorPath(customCursorPathField.getText());

        refreshTable();
        buttonsTable.setRowSelectionInterval(selectedRow, selectedRow);

        parent.log("Applied changes to button: " + btn.getText());
    }

    private void addNewButton() {
        ActionButton newBtn = new ActionButton("New Action");
        newBtn.setOrder(actionButtons.size());
        newBtn.setCursorType("HAND");
        newBtn.setHoverFormat("{action}");

        actionButtons.add(newBtn);
        refreshTable();

        // Select the new button
        buttonsTable.setRowSelectionInterval(actionButtons.size() - 1, actionButtons.size() - 1);
        parent.log("Added new button");
    }

    private void removeSelectedButton() {
        if (selectedRow < 0 || selectedRow >= actionButtons.size()) {
            JOptionPane.showMessageDialog(this, "Please select a button first!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove button '" + actionButtons.get(selectedRow).getText() + "'?",
            "Confirm Remove", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            actionButtons.remove(selectedRow);

            // Reorder remaining buttons
            for (int i = 0; i < actionButtons.size(); i++) {
                actionButtons.get(i).setOrder(i);
            }

            refreshTable();
            parent.log("Removed button");
        }
    }

    private void moveButtonUp() {
        if (selectedRow <= 0 || selectedRow >= actionButtons.size()) {
            return;
        }

        // Swap with previous
        ActionButton current = actionButtons.get(selectedRow);
        ActionButton previous = actionButtons.get(selectedRow - 1);

        int tempOrder = current.getOrder();
        current.setOrder(previous.getOrder());
        previous.setOrder(tempOrder);

        actionButtons.set(selectedRow, previous);
        actionButtons.set(selectedRow - 1, current);

        refreshTable();
        buttonsTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
    }

    private void moveButtonDown() {
        if (selectedRow < 0 || selectedRow >= actionButtons.size() - 1) {
            return;
        }

        // Swap with next
        ActionButton current = actionButtons.get(selectedRow);
        ActionButton next = actionButtons.get(selectedRow + 1);

        int tempOrder = current.getOrder();
        current.setOrder(next.getOrder());
        next.setOrder(tempOrder);

        actionButtons.set(selectedRow, next);
        actionButtons.set(selectedRow + 1, current);

        refreshTable();
        buttonsTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
    }

    private void saveAllButtons() {
        try {
            ActionButtonSaver.saveActionButtons(actionButtons);
            parent.log("✓ Action buttons saved to file!");
            JOptionPane.showMessageDialog(this, "Action buttons saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            parent.log("ERROR saving action buttons: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetToDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reset to default action buttons? This will overwrite all changes!",
            "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ActionButtonSaver.createDefaultFile();
                loadActionButtons();
                parent.log("✓ Reset to default action buttons");
            } catch (Exception e) {
                parent.log("ERROR resetting: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
