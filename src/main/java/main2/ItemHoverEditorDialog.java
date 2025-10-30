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
 * Dialog zum Editieren von Hover-Text mit Conditions für Items
 */
public class ItemHoverEditorDialog extends JDialog {
	private Item item;
	private EditorWindow parent;
	private JTable hoverTable;
	private DefaultTableModel tableModel;

	public ItemHoverEditorDialog(EditorWindow parent, Item item) {
		super(parent, "Hover Text Editor - " + item.getName(), false); // Non-modal
		this.parent = parent;
		this.item = item;

		setSize(700, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadHoverData();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("Hover Text Editor: " + item.getName());
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
		JButton addButton = new JButton("Add Hover Text");
		addButton.addActionListener(e -> {
			tableModel.addRow(new Object[] { "none", "", "Delete" });
		});
		addPanel.add(addButton);
		bottomPanel.add(addPanel, BorderLayout.WEST);

		JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> saveHoverData());
		savePanel.add(saveButton);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());
		savePanel.add(closeButton);

		bottomPanel.add(savePanel, BorderLayout.EAST);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadHoverData() {
		tableModel.setRowCount(0);

		Map<String, String> hoverConditions = item.getHoverDisplayConditions();
		if (hoverConditions != null && !hoverConditions.isEmpty()) {
			for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
				tableModel.addRow(new Object[] { entry.getKey(), entry.getValue(), "Delete" });
			}
		} else {
			// Add default row
			tableModel.addRow(new Object[] { "none", item.getName(), "Delete" });
		}
	}

	private void saveHoverData() {
		parent.log("=== SAVING HOVER DATA FOR ITEM ===");
		parent.log("Item Name: " + item.getName());
		parent.log("Item Instance: " + System.identityHashCode(item));

		// CRITICAL: Stop any active cell editing to commit changes
		if (hoverTable.isEditing()) {
			hoverTable.getCellEditor().stopCellEditing();
			parent.log("Stopped active cell editing");
		}

		// Debug: Show what's in the table before saving
		parent.log("Table data:");
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String condition = (String) tableModel.getValueAt(row, 0);
			String text = (String) tableModel.getValueAt(row, 1);
			parent.log("  Row " + row + ": " + condition + " -> " + text);
		}

		// Clear existing hover conditions
		parent.log("Clearing existing hover conditions...");
		item.getHoverDisplayConditions().clear();

		// Save from table
		parent.log("Saving from table to Item...");
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String condition = (String) tableModel.getValueAt(row, 0);
			String text = (String) tableModel.getValueAt(row, 1);

			if (text != null && !text.trim().isEmpty()) {
				// Remove quotes if user entered them
				String cleanText = text.trim();
				if (cleanText.startsWith("\"") && cleanText.endsWith("\"")) {
					cleanText = cleanText.substring(1, cleanText.length() - 1);
				}
				item.addHoverDisplayCondition(condition, cleanText);
				parent.log("  Added: " + condition + " → \"" + cleanText + "\"");
			}
		}

		// Debug: Show what was saved
		parent.log("Hover conditions now in Item: " + item.getHoverDisplayConditions());

		// Verify the Item is in the current scene
		Scene currentScene = parent.getGame().getCurrentScene();
		boolean found = false;
		for (Item sceneItem : currentScene.getItems()) {
			if (sceneItem == item) {
				found = true;
				parent.log("Item found in scene (same instance)");
				break;
			}
			if (sceneItem.getName().equals(item.getName())) {
				parent.log("WARNING: Item with same name found but DIFFERENT instance!");
				parent.log("  Scene Item instance: " + System.identityHashCode(sceneItem));
				parent.log("  Editor Item instance: " + System.identityHashCode(item));
			}
		}
		if (!found) {
			parent.log("ERROR: Item NOT found in current scene!");
		}

		// Save item to file
		try {
			ItemSaver.saveItemByName(item);
			parent.log("Item saved to file!");
		} catch (Exception e) {
			parent.log("ERROR saving item: " + e.getMessage());
		}

		// Auto-save scene
		parent.log("Calling autoSaveCurrentScene()...");
		parent.autoSaveCurrentScene();

		// Repaint game panel to show changes immediately
		parent.getGame().repaintGamePanel();

		parent.log("Hover text save process completed!");

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
			setText("Delete");
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
			button.setText("Delete");
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
