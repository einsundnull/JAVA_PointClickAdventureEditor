package main2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 * Dialog zum Verwalten aller Boolean-Conditions
 */
public class ConditionsManagerDialog extends JDialog {
	private JTable conditionsTable;
	private DefaultTableModel tableModel;
	private Map<String, Boolean> conditions;
	private EditorWindow parent;

	public ConditionsManagerDialog(EditorWindow parent) {
		super(parent, "Conditions Manager", false); // Non-modal
		this.parent = parent;
		this.conditions = new LinkedHashMap<>();

		setSize(600, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadConditions();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("🎛️ Boolean Conditions Manager");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Table
		String[] columns = { "Condition Name", "Current Value", "Default Value" };
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public Class<?> getColumnClass(int column) {
				return column == 0 ? String.class : Boolean.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1 || column == 2; // Value and default editable
			}
		};

		conditionsTable = new JTable(tableModel);
		conditionsTable.setRowHeight(25);
		conditionsTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		conditionsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		conditionsTable.getColumnModel().getColumn(2).setPreferredWidth(100);

		// Listen for changes
		conditionsTable.getModel().addTableModelListener(e -> {
			if (e.getType() == TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				int column = e.getColumn();
				String name = (String) tableModel.getValueAt(row, 0);

				if (column == 1) {
					// Current value changed
					Boolean value = (Boolean) tableModel.getValueAt(row, 1);
					conditions.put(name, value);
					Conditions.setCondition(name, value);
				}
				// Default value is saved separately
			}
		});

		JScrollPane scrollPane = new JScrollPane(conditionsTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		add(scrollPane, BorderLayout.CENTER);

		// Bottom panel with buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton addButton = new JButton("➕ Add New");
		addButton.addActionListener(e -> addNewCondition());
		bottomPanel.add(addButton);

		JButton deleteButton = new JButton("🗑️ Delete Selected");
		deleteButton.addActionListener(e -> deleteSelectedCondition());
		bottomPanel.add(deleteButton);

		JButton saveButton = new JButton("💾 Save to File");
		saveButton.addActionListener(e -> saveConditions());
		bottomPanel.add(saveButton);

		JButton closeButton = new JButton("✓ Close");
		closeButton.addActionListener(e -> dispose());
		bottomPanel.add(closeButton);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadConditions() {
		conditions.clear();
		tableModel.setRowCount(0);

		// Load defaults from separate map
		Map<String, Boolean> defaults = loadDefaultValues();

		// Load from Conditions class using reflection
		try {
			java.lang.reflect.Field[] fields = Conditions.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				if (field.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())
						&& java.lang.reflect.Modifier.isPublic(field.getModifiers())) {

					String name = field.getName();
					boolean currentValue = field.getBoolean(null);
					boolean defaultValue = defaults.getOrDefault(name, false);

					conditions.put(name, currentValue);
					tableModel.addRow(new Object[] { name, currentValue, defaultValue });
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error loading conditions: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addNewCondition() {
		String name = JOptionPane.showInputDialog(this, "Enter new condition name (camelCase):", "Add Condition",
				JOptionPane.PLAIN_MESSAGE);

		if (name != null && !name.trim().isEmpty()) {
			name = name.trim();

			// Check if already exists
			if (conditions.containsKey(name)) {
				JOptionPane.showMessageDialog(this, "Condition '" + name + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Add to table
			conditions.put(name, false);
			tableModel.addRow(new Object[] { name, false });

			parent.log("Added new condition: " + name);

			JOptionPane.showMessageDialog(this,
					"Condition added!\n\n" + "⚠️ IMPORTANT: You must manually add this to Conditions.java:\n\n"
							+ "public static boolean " + name + " = false;\n\n"
							+ "And update the switch statements in:\n" + "- setCondition()\n" + "- getCondition()",
					"Manual Step Required", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void deleteSelectedCondition() {
		int row = conditionsTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Please select a condition to delete", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String name = (String) tableModel.getValueAt(row, 0);

		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete condition '" + name + "'?\n\n" + "⚠️ This only removes it from the table.\n"
						+ "You must manually remove it from Conditions.java!",
				"Confirm Delete", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			conditions.remove(name);
			tableModel.removeRow(row);
			parent.log("Deleted condition: " + name);
		}
	}

	private Map<String, Boolean> loadDefaultValues() {
		Map<String, Boolean> defaults = new HashMap<>();
		File file = new File("resources/conditions-defaults.txt");

		if (!file.exists()) {
			return defaults;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				if (line.contains("=")) {
					String[] parts = line.split("=", 2);
					String name = parts[0].trim();
					boolean value = Boolean.parseBoolean(parts[1].trim());
					defaults.put(name, value);
				}
			}
			reader.close();
		} catch (Exception e) {
			parent.log("Could not load defaults: " + e.getMessage());
		}

		return defaults;
	}

	private void saveConditions() {
		try {
			// Save current values to progress.txt
			String filename = "resources/progress.txt";
			parent.log("Saving conditions to: " + filename);

			Scene currentScene = parent.getGame().getCurrentScene();
			String sceneName = currentScene != null ? currentScene.getName() : "sceneBeach";

			Conditions.saveToProgress(filename, sceneName);

			// Save default values to conditions-defaults.txt
			String defaultsFile = "resources/conditions-defaults.txt";
			FileWriter writer = new FileWriter(defaultsFile);
			writer.write("# Default Condition Values\n");
			writer.write("# These are the starting values when game resets\n\n");

			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean defaultValue = (Boolean) tableModel.getValueAt(row, 2);
				writer.write(name + " = " + defaultValue + "\n");
			}
			writer.close();

			parent.log("✓ Conditions and defaults saved!");

			JOptionPane.showMessageDialog(this, "Conditions saved to:\n" + "- progress.txt (current values)\n"
					+ "- conditions-defaults.txt (default values)", "Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public Map<String, Boolean> getConditions() {
		return new LinkedHashMap<>(conditions);
	}
}