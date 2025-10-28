package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Dialog zum Editieren von Hover-Text mit Conditions
 */
public class HoverEditorDialog extends JDialog {
	private KeyArea keyArea;
	private EditorWindow parent;
	private JTable hoverTable;
	private DefaultTableModel tableModel;

	public HoverEditorDialog(EditorWindow parent, KeyArea keyArea) {
		super(parent, "Hover Text Editor - " + keyArea.getName(), false); // Non-modal
		this.parent = parent;
		this.keyArea = keyArea;

		setSize(700, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadHoverData();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("💬 Hover Text Editor: " + keyArea.getName());
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Info
		JLabel infoLabel = new JLabel("Set different hover texts based on conditions");
		infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
		add(infoLabel, BorderLayout.NORTH);

		// Table
		String[] columns = { "Condition", "Hover Text", "" };
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column != 2; // Button column not editable
			}
		};

		hoverTable = new JTable(tableModel);
		hoverTable.setRowHeight(30);
		hoverTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		hoverTable.getColumnModel().getColumn(1).setPreferredWidth(400);
		hoverTable.getColumnModel().getColumn(2).setPreferredWidth(80);

		// Condition dropdown
		JComboBox<String> conditionCombo = new JComboBox<>();
		conditionCombo.addItem("none");
		try {
			java.lang.reflect.Field[] fields = Conditions.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				if (field.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					conditionCombo.addItem(field.getName() + " = true");
					conditionCombo.addItem(field.getName() + " = false");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		hoverTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(conditionCombo));

		// Delete button column
		hoverTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());
		hoverTable.getColumnModel().getColumn(2).setCellEditor(new ButtonEditor(new JCheckBox()));

		JScrollPane scrollPane = new JScrollPane(hoverTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		add(scrollPane, BorderLayout.CENTER);

		// Bottom panel
		JPanel bottomPanel = new JPanel(new BorderLayout());

		JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addButton = new JButton("➕ Add Hover Text");
		addButton.addActionListener(e -> {
			tableModel.addRow(new Object[] { "none", "", "Delete" });
		});
		addPanel.add(addButton);
		bottomPanel.add(addPanel, BorderLayout.WEST);

		JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveButton = new JButton("💾 Save");
		saveButton.addActionListener(e -> saveHoverData());
		savePanel.add(saveButton);

		JButton closeButton = new JButton("✓ Close");
		closeButton.addActionListener(e -> dispose());
		savePanel.add(closeButton);

		bottomPanel.add(savePanel, BorderLayout.EAST);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadHoverData() {
		tableModel.setRowCount(0);

		Map<String, String> hoverConditions = keyArea.getHoverDisplayConditions();
		if (hoverConditions != null && !hoverConditions.isEmpty()) {
			for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
				tableModel.addRow(new Object[] { entry.getKey(), entry.getValue(), "Delete" });
			}
		} else {
			// Add default row
			tableModel.addRow(new Object[] { "none", keyArea.getName(), "Delete" });
		}
	}

	private void saveHoverData() {
		parent.log("Saving hover text for " + keyArea.getName() + "...");

		// Clear existing hover conditions
		keyArea.getHoverDisplayConditions().clear();

		// Save from table
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String condition = (String) tableModel.getValueAt(row, 0);
			String text = (String) tableModel.getValueAt(row, 1);

			if (text != null && !text.trim().isEmpty()) {
				keyArea.addHoverDisplayCondition(condition, text.trim());
				parent.log("  " + condition + " → \"" + text.trim() + "\"");
			}
		}

		// Auto-save scene
		parent.autoSaveCurrentScene();

		parent.log("✓ Hover text saved!");

		JOptionPane.showMessageDialog(this, "Hover text saved successfully!", "Success",
				JOptionPane.INFORMATION_MESSAGE);
	}

	// Button Renderer
	class ButtonRenderer extends JButton implements TableCellRenderer {
		public ButtonRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText("🗑️");
			return this;
		}
	}

	// Button Editor
	class ButtonEditor extends DefaultCellEditor {
		private JButton button;
		private boolean isPushed;

		public ButtonEditor(JCheckBox checkBox) {
			super(checkBox);
			button = new JButton();
			button.setOpaque(true);
			button.addActionListener(e -> fireEditingStopped());
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			button.setText("🗑️");
			isPushed = true;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				int row = hoverTable.getSelectedRow();
				if (row >= 0) {
					tableModel.removeRow(row);
				}
			}
			isPushed = false;
			return "Delete";
		}
	}
}