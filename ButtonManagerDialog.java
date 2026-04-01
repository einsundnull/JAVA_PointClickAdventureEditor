package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Button Manager Dialog - nach "Schema Manager Buttons.txt"
 *
 * Tabelle mit:
 * - Image (Thumbnail)
 * - Text on button (editierbar)
 * - Hover Text (editierbar)
 * - Use Text (checkbox)
 * - Use Hover Text (checkbox)
 * - Use Image (checkbox)
 */
public class ButtonManagerDialog extends JDialog {
    private JTable buttonsTable;
    private ButtonsTableModel tableModel;
    private JTextField widthField;
    private JTextField heightField;
    private ButtonData selectedButton;

    public ButtonManagerDialog(Frame owner) {
        super(owner, "Button Manager", true);

        initUI();
        loadButtons();

        setSize(1000, 600);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Button Manager");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f).deriveFont(java.awt.Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Center: Table
        tableModel = new ButtonsTableModel();
        buttonsTable = new JTable(tableModel);
        buttonsTable.setRowHeight(60);
        buttonsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        buttonsTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Image
        buttonsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Text on button
        buttonsTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Hover Text
        buttonsTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Use Text
        buttonsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Use Hover Text
        buttonsTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Use Image

        // Set custom renderers and editors
        buttonsTable.getColumnModel().getColumn(0).setCellRenderer(new ImageRenderer());
        buttonsTable.getColumnModel().getColumn(3).setCellRenderer(new CheckBoxRenderer());
        buttonsTable.getColumnModel().getColumn(3).setCellEditor(new CheckBoxEditor());
        buttonsTable.getColumnModel().getColumn(4).setCellRenderer(new CheckBoxRenderer());
        buttonsTable.getColumnModel().getColumn(4).setCellEditor(new CheckBoxEditor());
        buttonsTable.getColumnModel().getColumn(5).setCellRenderer(new CheckBoxRenderer());
        buttonsTable.getColumnModel().getColumn(5).setCellEditor(new CheckBoxEditor());

        // Selection listener
        buttonsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onButtonSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(buttonsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Table control buttons (↑↓ New Delete Save Cancel)
        JPanel tableButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JButton upBtn = new JButton("↑");
        upBtn.setToolTipText("Move button up");
        upBtn.addActionListener(e -> moveButtonUp());
        tableButtonsPanel.add(upBtn);

        JButton downBtn = new JButton("↓");
        downBtn.setToolTipText("Move button down");
        downBtn.addActionListener(e -> moveButtonDown());
        tableButtonsPanel.add(downBtn);

        JButton newBtn = new JButton("New");
        newBtn.addActionListener(e -> createNewButton());
        tableButtonsPanel.add(newBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteButton());
        tableButtonsPanel.add(deleteBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveButtons());
        tableButtonsPanel.add(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        tableButtonsPanel.add(cancelBtn);

        bottomPanel.add(tableButtonsPanel, BorderLayout.NORTH);

        // Size panel
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sizePanel.add(new JLabel("Size:"));
        sizePanel.add(new JLabel("Width:"));
        widthField = new JTextField("100", 5);
        sizePanel.add(widthField);
        sizePanel.add(new JLabel("px"));
        sizePanel.add(new JLabel("Height:"));
        heightField = new JTextField("30", 5);
        sizePanel.add(heightField);
        sizePanel.add(new JLabel("px"));

        JButton applySizeBtn = new JButton("Apply Size to Selected");
        applySizeBtn.addActionListener(e -> applySizeToSelected());
        sizePanel.add(applySizeBtn);

        bottomPanel.add(sizePanel, BorderLayout.CENTER);

        // Global buttons
        JPanel globalButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton openResourcesBtn = new JButton("Open Resources");
        openResourcesBtn.addActionListener(e -> openResourcesDirectory());
        globalButtonsPanel.add(openResourcesBtn);

        JButton openImageResourcesBtn = new JButton("Open Image Resources");
        openImageResourcesBtn.addActionListener(e -> openImageResourcesDirectory());
        globalButtonsPanel.add(openImageResourcesBtn);

        JButton openButtonResourcesBtn = new JButton("Open Button Resources");
        openButtonResourcesBtn.addActionListener(e -> openButtonResourcesDirectory());
        globalButtonsPanel.add(openButtonResourcesBtn);

        JButton manageConditionsBtn = new JButton("Manage Conditions");
        manageConditionsBtn.addActionListener(e -> openConditionsManager());
        globalButtonsPanel.add(manageConditionsBtn);

        JButton setImageBtn = new JButton("Set Image for Selected");
        setImageBtn.addActionListener(e -> setImageForSelected());
        globalButtonsPanel.add(setImageBtn);

        bottomPanel.add(globalButtonsPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadButtons() {
        tableModel.loadFromManager();
    }

    private void onButtonSelected() {
        int selectedRow = buttonsTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedButton = tableModel.getButtonAt(selectedRow);
            if (selectedButton != null) {
                widthField.setText(String.valueOf(selectedButton.getWidth()));
                heightField.setText(String.valueOf(selectedButton.getHeight()));
            }
        } else {
            selectedButton = null;
        }
    }

    private void moveButtonUp() {
        int selectedRow = buttonsTable.getSelectedRow();
        if (selectedRow <= 0) {
            JOptionPane.showMessageDialog(this, "Cannot move up", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ButtonData button = tableModel.getButtonAt(selectedRow);
        ButtonsDataManager.moveButtonUp(button.getName());
        loadButtons();
        buttonsTable.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
    }

    private void moveButtonDown() {
        int selectedRow = buttonsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount() - 1) {
            JOptionPane.showMessageDialog(this, "Cannot move down", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ButtonData button = tableModel.getButtonAt(selectedRow);
        ButtonsDataManager.moveButtonDown(button.getName());
        loadButtons();
        buttonsTable.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
    }

    private void createNewButton() {
        String buttonName = JOptionPane.showInputDialog(this, "Enter button name:", "New Button", JOptionPane.PLAIN_MESSAGE);

        if (buttonName == null || buttonName.trim().isEmpty()) {
            return;
        }

        buttonName = buttonName.trim();

        if (ButtonsDataManager.getButton(buttonName) != null) {
            JOptionPane.showMessageDialog(this, "Button '" + buttonName + "' already exists!", "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ButtonData newButton = new ButtonData(buttonName);
        ButtonsDataManager.addButton(newButton);
        loadButtons();

        JOptionPane.showMessageDialog(this, "Button '" + buttonName + "' created!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteButton() {
        int selectedRow = buttonsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a button to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ButtonData button = tableModel.getButtonAt(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete button '" + button.getName() + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        ButtonsDataManager.removeButton(button.getName());
        loadButtons();

        JOptionPane.showMessageDialog(this, "Button deleted!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveButtons() {
        // Stop any cell editing
        if (buttonsTable.isEditing()) {
            buttonsTable.getCellEditor().stopCellEditing();
        }

        ButtonsDataManager.saveButtons();
        JOptionPane.showMessageDialog(this, "Buttons saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applySizeToSelected() {
        if (selectedButton == null) {
            JOptionPane.showMessageDialog(this, "Please select a button first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int width = Integer.parseInt(widthField.getText().trim());
            int height = Integer.parseInt(heightField.getText().trim());

            selectedButton.setWidth(width);
            selectedButton.setHeight(height);
            ButtonsDataManager.updateButton(selectedButton);

            JOptionPane.showMessageDialog(this, "Size applied to '" + selectedButton.getName() + "'", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid size values!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setImageForSelected() {
        if (selectedButton == null) {
            JOptionPane.showMessageDialog(this, "Please select a button first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(ResourcePathHelper.resolve("buttons"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String imagePath = selectedFile.getAbsolutePath();

            selectedButton.setImagePath(imagePath);
            ButtonsDataManager.updateButton(selectedButton);
            loadButtons();

            JOptionPane.showMessageDialog(this, "Image set for '" + selectedButton.getName() + "'", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openResourcesDirectory() {
        try {
            java.awt.Desktop.getDesktop().open(ResourcePathHelper.resolve(""));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openImageResourcesDirectory() {
        try {
            File dir = ResourcePathHelper.resolve("images");
            if (!dir.exists()) dir.mkdirs();
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openButtonResourcesDirectory() {
        try {
            File dir = ResourcePathHelper.resolve("buttons");
            if (!dir.exists()) dir.mkdirs();
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openConditionsManager() {
        if (getOwner() instanceof Frame) {
            ConditionsManagerDialog dialog = new ConditionsManagerDialog((Frame) getOwner());
            dialog.setVisible(true);
        }
    }

    // Table Model
    class ButtonsTableModel extends AbstractTableModel {
        private java.util.List<ButtonData> buttons = new java.util.ArrayList<>();
        private String[] columnNames = {"Image", "Text on Button", "Hover Text", "Use Text", "Use Hover Text", "Use Image"};

        public void loadFromManager() {
            buttons = ButtonsDataManager.getButtons();
            fireTableDataChanged();
        }

        public ButtonData getButtonAt(int row) {
            return buttons.get(row);
        }

        @Override
        public int getRowCount() {
            return buttons.size();
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
            ButtonData button = buttons.get(rowIndex);

            switch (columnIndex) {
                case 0: return button.getImagePath(); // Image
                case 1: return button.getTextOnButton();
                case 2: return button.getHoverText();
                case 3: return button.isUseText();
                case 4: return button.isUseHoverText();
                case 5: return button.isUseImage();
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ButtonData button = buttons.get(rowIndex);

            switch (columnIndex) {
                case 1:
                    button.setTextOnButton((String) value);
                    ButtonsDataManager.updateButton(button);
                    break;
                case 2:
                    button.setHoverText((String) value);
                    ButtonsDataManager.updateButton(button);
                    break;
                case 3:
                    button.setUseText((Boolean) value);
                    ButtonsDataManager.updateButton(button);
                    break;
                case 4:
                    button.setUseHoverText((Boolean) value);
                    ButtonsDataManager.updateButton(button);
                    break;
                case 5:
                    button.setUseImage((Boolean) value);
                    ButtonsDataManager.updateButton(button);
                    break;
            }

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Image column is not editable (use "Set Image" button)
            // Columns 1, 2, 3, 4, 5 are editable
            return columnIndex >= 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class; // Image path
            if (columnIndex >= 3 && columnIndex <= 5) return Boolean.class;
            return String.class;
        }
    }

    // Image Renderer
    class ImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);

            if (value != null && !value.toString().isEmpty()) {
                try {
                    File imgFile = new File(value.toString());
                    if (imgFile.exists()) {
                        Image img = ImageIO.read(imgFile);
                        if (img != null) {
                            Image scaledImg = img.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                            label.setIcon(new ImageIcon(scaledImg));
                        }
                    } else {
                        label.setText("No Image");
                    }
                } catch (Exception e) {
                    label.setText("Error");
                }
            } else {
                label.setText("No Image");
            }

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
                label.setOpaque(true);
            }

            return label;
        }
    }

    // CheckBox Renderer
    class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(value != null && (Boolean) value);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    // CheckBox Editor
    class CheckBoxEditor extends javax.swing.DefaultCellEditor {
        public CheckBoxEditor() {
            super(new JCheckBox());
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }
}
