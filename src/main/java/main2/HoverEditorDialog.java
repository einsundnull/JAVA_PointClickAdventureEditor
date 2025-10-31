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
 * Works with both KeyArea and Item
 */
public class HoverEditorDialog extends JDialog {
	private KeyArea keyArea;
	private Item item;
	private EditorWindow parent;
	private JTable hoverTable;
	private DefaultTableModel tableModel;

	public HoverEditorDialog(EditorWindow parent, KeyArea keyArea) {
		super(parent, "Hover Text Editor - " + keyArea.getName(), false); // Non-modal
		this.parent = parent;
		this.keyArea = keyArea;
		this.item = null;

		setSize(700, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadHoverData();
	}

	public HoverEditorDialog(EditorWindow parent, Item item) {
		super(parent, "Hover Text Editor - " + item.getName(), false); // Non-modal
		this.parent = parent;
		this.item = item;
		this.keyArea = null;

		setSize(700, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadHoverData();
	}

	 public String getName() {
		return keyArea != null ? keyArea.getName() : item.getName();
	}

	private Map<String, String> getHoverDisplayConditions() {
		return keyArea != null ? keyArea.getHoverDisplayConditions() : item.getHoverDisplayConditions();
	}

	private void addHoverDisplayCondition(String condition, String text) {
		if (keyArea != null) {
			keyArea.addHoverDisplayCondition(condition, text);
		} else {
			item.addHoverDisplayCondition(condition, text);
		}
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("Hover Text Editor: " + getName());
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
				return true; // All columns editable (including delete button)
			}
		};

		hoverTable = new JTable(tableModel);
		hoverTable.setRowHeight(30);
		hoverTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		hoverTable.getColumnModel().getColumn(1).setPreferredWidth(400);
		hoverTable.getColumnModel().getColumn(2).setPreferredWidth(80);

		// Condition dropdown - using custom editor that loads conditions dynamically
		hoverTable.getColumnModel().getColumn(0).setCellEditor(new DynamicConditionComboBoxEditor());

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

		JButton manageConditionsBtn = new JButton("Manage Conditions");
		manageConditionsBtn.addActionListener(e -> openConditionsManager());
		addPanel.add(manageConditionsBtn);

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

		Map<String, String> hoverConditions = getHoverDisplayConditions();
		if (hoverConditions != null && !hoverConditions.isEmpty()) {
			for (Map.Entry<String, String> entry : hoverConditions.entrySet()) {
				tableModel.addRow(new Object[] { entry.getKey(), entry.getValue(), "Delete" });
			}
		} else {
			// Add default row
			tableModel.addRow(new Object[] { "none", getName(), "Delete" });
		}
	}

	private void saveHoverData() {
		parent.log("=== SAVING HOVER DATA ===");
		parent.log("Name: " + getName());
		parent.log("Type: " + (keyArea != null ? "KeyArea" : "Item"));

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
		getHoverDisplayConditions().clear();

		// Save from table
		parent.log("Saving from table...");
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String condition = (String) tableModel.getValueAt(row, 0);
			String text = (String) tableModel.getValueAt(row, 1);

			if (text != null && !text.trim().isEmpty()) {
				// Remove quotes if user entered them
				String cleanText = text.trim();
				if (cleanText.startsWith("\"") && cleanText.endsWith("\"")) {
					cleanText = cleanText.substring(1, cleanText.length() - 1);
				}
				addHoverDisplayCondition(condition, cleanText);
				parent.log("  Added: " + condition + " → \"" + cleanText + "\"");
			}
		}

		// Debug: Show what was saved
		parent.log("Hover conditions now: " + getHoverDisplayConditions());

		// CRITICAL: Update scene objects
		Scene currentScene = parent.getGame().getCurrentScene();
		boolean found = false;

		if (keyArea != null) {
			// Update KeyArea in scene
			for (KeyArea area : currentScene.getKeyAreas()) {
				if (area.getName().equals(keyArea.getName())) {
					if (area == keyArea) {
						found = true;
						parent.log("KeyArea found in scene (same instance) - already updated");
					} else {
						// Different instance - copy hover conditions
						parent.log("Updating scene KeyArea (different instance)...");
						area.getHoverDisplayConditions().clear();
						for (Map.Entry<String, String> entry : keyArea.getHoverDisplayConditions().entrySet()) {
							area.addHoverDisplayCondition(entry.getKey(), entry.getValue());
						}
						parent.log("  Copied hover conditions to scene KeyArea");
						found = true;
					}
				}
			}
		} else {
			// Update Item in scene
			for (Item sceneItem : currentScene.getItems()) {
				if (sceneItem.getName().equals(item.getName())) {
					if (sceneItem == item) {
						found = true;
						parent.log("Item found in scene (same instance) - already updated");
					} else {
						// Different instance - copy hover conditions
						parent.log("Updating scene Item (different instance)...");
						sceneItem.getHoverDisplayConditions().clear();
						for (Map.Entry<String, String> entry : item.getHoverDisplayConditions().entrySet()) {
							sceneItem.addHoverDisplayCondition(entry.getKey(), entry.getValue());
						}
						parent.log("  Copied hover conditions to scene Item");
						found = true;
					}
				}
			}
		}

		if (!found) {
			parent.log("ERROR: Object NOT found in current scene!");
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
		private int currentRow;

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
			currentRow = row;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				// Delete the row
				tableModel.removeRow(currentRow);
				parent.log("Deleted hover condition from row " + currentRow);

				// Autosave after deletion
				javax.swing.SwingUtilities.invokeLater(() -> {
					saveHoverData();
				});
			}
			isPushed = false;
			return "Delete";
		}

		@Override
		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}

	/**
	 * Dynamic ComboBox editor that reloads conditions every time a cell is edited
	 */
	class DynamicConditionComboBoxEditor extends DefaultCellEditor {
		private JComboBox<String> comboBox;

		public DynamicConditionComboBoxEditor() {
			super(new JComboBox<>());
			comboBox = (JComboBox<String>) getComponent();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			// Reload conditions every time the editor is opened
			comboBox.removeAllItems();
			comboBox.addItem("none");

			// Add all current conditions from Conditions system (dynamically loaded!)
			for (String conditionName : Conditions.getAllConditionNames()) {
				comboBox.addItem(conditionName + " = true");
				comboBox.addItem(conditionName + " = false");
			}

			// Set current value if exists
			if (value != null) {
				comboBox.setSelectedItem(value);
			}

			return comboBox;
		}
	}

	private void openConditionsManager() {
		ConditionsManagerDialog dialog = new ConditionsManagerDialog(parent);
		dialog.setVisible(true);
		// Reload table to show new conditions
		loadHoverData();
	}
}
