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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Dialog zum Editieren aller Actions eines Items
 */
public class ItemActionsEditorDialog extends JDialog {
	private Item item;
	private EditorWindow parent;

	private static final String[] ALL_ACTIONS = { "Nimm", "Gib", "Ziehe", "Drücke", "Drehe", "Hebe", "Anschauen",
			"Sprich", "Benutze" };

	private JTabbedPane tabbedPane;

	public ItemActionsEditorDialog(EditorWindow parent, Item item) {
		super(parent, "Actions Editor - " + item.getName(), false); // Non-modal
		this.parent = parent;
		this.item = item;

		setSize(900, 600);
		setLocationRelativeTo(parent);

		initUI();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("⚙️ Actions Editor: " + item.getName());
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Tabbed Pane - One tab per action
		tabbedPane = new JTabbedPane();

		for (String action : ALL_ACTIONS) {
			JPanel actionPanel = createActionPanel(action);
			tabbedPane.addTab(action, actionPanel);
		}

		add(tabbedPane, BorderLayout.CENTER);

		// Bottom buttons
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		// Left buttons
		JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton manageConditionsBtn = new JButton("🎛️ Manage Conditions");
		manageConditionsBtn.addActionListener(e -> openConditionsManager());
		leftButtons.add(manageConditionsBtn);

		JButton editHoverBtn = new JButton("💬 Edit Hover");
		editHoverBtn.addActionListener(e -> openHoverEditor());
		leftButtons.add(editHoverBtn);

		JButton manageDialogsBtn = new JButton("📝 Manage Dialogs");
		manageDialogsBtn.addActionListener(e -> openDialogManager());
		leftButtons.add(manageDialogsBtn);

		bottomPanel.add(leftButtons, BorderLayout.WEST);

		// Right buttons
		JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveButton = new JButton("💾 Save All");
		saveButton.addActionListener(e -> saveAllActions());
		rightButtons.add(saveButton);

		JButton closeButton = new JButton("✓ Close");
		closeButton.addActionListener(e -> dispose());
		rightButtons.add(closeButton);

		bottomPanel.add(rightButtons, BorderLayout.EAST);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private JPanel createActionPanel(String actionName) {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Info label
		JLabel infoLabel = new JLabel("Configure conditions and results for: " + actionName);
		infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
		panel.add(infoLabel, BorderLayout.NORTH);

		// Table for conditions
		String[] columns = { "Condition", "Result Type", "Result Value", "Delete" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return true; // All columns editable
			}
		};

		JTable table = new JTable(model);
		table.setRowHeight(30);

		// Set column widths
		table.getColumnModel().getColumn(0).setPreferredWidth(200); // Condition
		table.getColumnModel().getColumn(1).setPreferredWidth(120); // Result Type
		table.getColumnModel().getColumn(2).setPreferredWidth(250); // Result Value
		table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Delete
		table.getColumnModel().getColumn(3).setMaxWidth(80);

		// Condition column with dropdown
		JComboBox<String> conditionCombo = new JComboBox<>();
		conditionCombo.addItem("none");
		// Add all conditions from Conditions system (dynamic!)
		for (String conditionName : Conditions.getAllConditionNames()) {
			conditionCombo.addItem(conditionName + " = true");
			conditionCombo.addItem(conditionName + " = false");
		}
		table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(conditionCombo));

		// Result Type column
		JComboBox<String> resultTypeCombo = new JComboBox<>(new String[] { "Dialog", "LoadScene", "SetBoolean" });
		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(resultTypeCombo));

		// Delete button column
		table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
		table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox(), table, model));

		// Load existing actions
		loadActionData(actionName, model);

		JScrollPane scrollPane = new JScrollPane(table);
		panel.add(scrollPane, BorderLayout.CENTER);

		// Add row button
		JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addButton = new JButton("➕ Add Condition");
		addButton.addActionListener(e -> {
			model.addRow(new Object[] { "none", "Dialog", "", "[X]" });
		});
		addPanel.add(addButton);

		// Quick add dialog button
		JButton quickAddButton = new JButton("📝 Quick Add Dialog");
		quickAddButton.addActionListener(e -> quickAddDialog(actionName, model));
		addPanel.add(quickAddButton);

		panel.add(addPanel, BorderLayout.SOUTH);

		// Store model for later retrieval
		table.putClientProperty("actionName", actionName);

		return panel;
	}

	private void loadActionData(String actionName, DefaultTableModel model) {
		// Get existing action handler
		KeyArea.ActionHandler handler = item.getActions().get(actionName);
		if (handler != null && handler.getConditionalResults() != null) {
			for (Map.Entry<String, String> entry : handler.getConditionalResults().entrySet()) {
				String condition = entry.getKey();
				String result = entry.getValue();

				// Parse result
				String resultType = "Dialog";
				String resultValue = result;

				if (result.startsWith("##load")) {
					resultType = "LoadScene";
					resultValue = result.substring(6).trim();
				} else if (result.startsWith("#SetBoolean:")) {
					resultType = "SetBoolean";
					resultValue = result.substring(12).trim();
				} else if (result.startsWith("#Dialog:")) {
					resultType = "Dialog";
					resultValue = result.substring(8).trim();
				}

				model.addRow(new Object[] { condition, resultType, resultValue, "[X]" });
			}
		}
	}

	private void quickAddDialog(String actionName, DefaultTableModel model) {
		String dialogName = JOptionPane.showInputDialog(this, "Enter dialog name for " + actionName + ":",
				"dialog-" + item.getName().toLowerCase() + "-" + actionName.toLowerCase());

		if (dialogName != null && !dialogName.trim().isEmpty()) {
			model.addRow(new Object[] { "none", "Dialog", dialogName.trim(), "[X]" });
		}
	}

	private void saveAllActions() {
		parent.log("Saving actions for item " + item.getName() + "...");

		// Clear existing actions
		item.getActions().clear();

		// Save each tab
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			String actionName = tabbedPane.getTitleAt(i);

			if (comp instanceof JPanel) {
				JPanel panel = (JPanel) comp;
				JScrollPane scrollPane = (JScrollPane) panel.getComponent(1);
				JTable table = (JTable) scrollPane.getViewport().getView();
				DefaultTableModel model = (DefaultTableModel) table.getModel();

				if (model.getRowCount() > 0) {
					KeyArea.ActionHandler handler = new KeyArea.ActionHandler();

					for (int row = 0; row < model.getRowCount(); row++) {
						String condition = (String) model.getValueAt(row, 0);
						String resultType = (String) model.getValueAt(row, 1);
						String resultValue = (String) model.getValueAt(row, 2);

						if (resultValue != null && !resultValue.trim().isEmpty()) {
							String result;
							switch (resultType) {
							case "Dialog":
								result = "#Dialog:" + resultValue.trim();
								break;
							case "LoadScene":
								result = "##load" + resultValue.trim();
								break;
							case "SetBoolean":
								result = "#SetBoolean:" + resultValue.trim();
								break;
							default:
								result = resultValue.trim();
							}

							handler.addConditionalResult(condition, result);
							parent.log("  " + actionName + ": " + condition + " → " + result);
						}
					}

					item.addAction(actionName, handler);
				}
			}
		}

		// Save item to file
		try {
			ItemSaver.saveItemByName(item);
			parent.log("✓ Actions saved to item file!");
		} catch (Exception e) {
			parent.log("ERROR saving item: " + e.getMessage());
		}

		// Also update scene
		parent.autoSaveCurrentScene();

		parent.log("✓ Actions saved!");

		JOptionPane.showMessageDialog(this, "Actions saved successfully!\n\n" + "Saved to item file.", "Success",
				JOptionPane.INFORMATION_MESSAGE);
	}

	// Button Renderer for Delete column
	class ButtonRenderer extends JButton implements TableCellRenderer {
		public ButtonRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText("[X]");
			setToolTipText("Remove this condition");
			return this;
		}
	}

	// Button Editor for Delete column
	class ButtonEditor extends DefaultCellEditor {
		private JButton button;
		private boolean isPushed;
		private int currentRow;
		private JTable table;
		private DefaultTableModel model;

		public ButtonEditor(JCheckBox checkBox, JTable table, DefaultTableModel model) {
			super(checkBox);
			this.table = table;
			this.model = model;
			button = new JButton();
			button.setOpaque(true);
			button.addActionListener(e -> fireEditingStopped());
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			button.setText("[X]");
			isPushed = true;
			currentRow = row;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				// Delete the row
				model.removeRow(currentRow);
				parent.log("Deleted action condition from row " + currentRow);

				// Autosave after deletion
				javax.swing.SwingUtilities.invokeLater(() -> {
					saveAllActions();
				});
			}
			isPushed = false;
			return "[X]";
		}

		@Override
		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}

	private void openConditionsManager() {
		parent.log("Opening Conditions Manager from Item Actions Editor...");
		ConditionsManagerDialog dialog = new ConditionsManagerDialog(parent);
		dialog.setVisible(true);
		parent.log("Conditions Manager closed");
	}

	private void openHoverEditor() {
		parent.log("Opening Hover Editor from Item Actions Editor...");
		ItemHoverEditorDialog dialog = new ItemHoverEditorDialog(parent, item);
		dialog.setVisible(true);
		parent.log("Hover Editor closed");
	}

	private void openDialogManager() {
		parent.log("Opening Dialog Manager from Item Actions Editor...");
		DialogManagerDialog dialog = new DialogManagerDialog(parent);
		dialog.setVisible(true);
		parent.log("Dialog Manager opened");
	}
}
