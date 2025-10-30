package main2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

		// Load all conditions from Conditions class (now dynamic!)
		Map<String, Boolean> allConditions = Conditions.getAllConditions();

		for (Map.Entry<String, Boolean> entry : allConditions.entrySet()) {
			String name = entry.getKey();
			boolean currentValue = entry.getValue();
			boolean defaultValue = entry.getValue(); // Current = default from conditions.txt

			conditions.put(name, currentValue);
			tableModel.addRow(new Object[] { name, currentValue, defaultValue });
		}

		parent.log("Loaded " + conditions.size() + " conditions dynamically");
	}

	private void addNewCondition() {
		String name = JOptionPane.showInputDialog(this, "Enter new condition name (camelCase):", "Add Condition",
				JOptionPane.PLAIN_MESSAGE);

		if (name != null && !name.trim().isEmpty()) {
			name = name.trim();

			// Check if already exists
			if (Conditions.conditionExists(name)) {
				JOptionPane.showMessageDialog(this, "Condition '" + name + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Add to Conditions system (automatically saves to conditions.txt!)
			Conditions.addCondition(name, false);

			// Add to table
			conditions.put(name, false);
			tableModel.addRow(new Object[] { name, false, false });

			parent.log("✓ Added new condition: " + name);

			JOptionPane.showMessageDialog(this,
					"✓ Condition added successfully!\n\n" + "The condition has been saved to:\n"
							+ "resources/conditions/conditions.txt\n\n" + "No source code changes needed!",
					"Success", JOptionPane.INFORMATION_MESSAGE);
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

		// Find all references first
		try {
			parent.log("Searching for references to condition: " + name);
			Map<String, List<Integer>> references = ConditionReferenceManager.findConditionReferences(name);

			// Build confirmation message
			StringBuilder message = new StringBuilder();
			message.append("Delete condition '").append(name).append("'?\n\n");

			if (references.isEmpty()) {
				message.append("✓ No references found in .txt files.\n\n");
			} else {
				message.append("⚠️ Found references in ").append(references.size()).append(" file(s):\n\n");
				int count = 0;
				for (Map.Entry<String, List<Integer>> entry : references.entrySet()) {
					String file = entry.getKey().replace("resources\\", "").replace("resources/", "");
					message.append("  • ").append(file).append(" (").append(entry.getValue().size()).append(" refs)\n");
					count++;
					if (count >= 5 && references.size() > 5) {
						message.append("  • ... and ").append(references.size() - 5).append(" more\n");
						break;
					}
				}
				message.append("\nAll references will be removed!\n\n");
			}

			message.append("This will:\n");
			message.append("✓ Remove from conditions-defaults.txt\n");
			message.append("✓ Remove from progress files\n");
			message.append("✓ Remove from all scene/item .txt files\n");
			message.append("✓ Reload all scenes and items (live update)\n");
			message.append("⚠️ Manual: Remove from Conditions.java\n");

			int confirm = JOptionPane.showConfirmDialog(this, message.toString(), "Confirm Cascade Delete",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}

			// Perform cascade deletion
			parent.log("Starting cascade deletion for: " + name);

			// 1. Remove from .txt files
			List<String> modifiedFiles = ConditionReferenceManager.removeConditionReferences(name);
			parent.log("Modified " + modifiedFiles.size() + " file(s):");
			for (String file : modifiedFiles) {
				parent.log("  - " + file);
			}

			// 2. Remove from defaults
			if (ConditionReferenceManager.removeFromDefaults(name)) {
				parent.log("Removed from conditions-defaults.txt");
			}

			// 3. Remove from progress files
			ConditionReferenceManager.removeFromProgressFiles(name);
			parent.log("Removed from progress files");

			// 4. Remove from Conditions system (automatically updates conditions.txt!)
			Conditions.removeCondition(name);
			parent.log("✓ Removed from conditions.txt");

			// 5. Remove from table
			conditions.remove(name);
			tableModel.removeRow(row);

			// 6. Trigger live update of scenes and items
			triggerLiveUpdate();

			parent.log("✓ Cascade deletion complete for: " + name);

			JOptionPane.showMessageDialog(this,
					"✓ Condition deleted successfully!\n\n" + "Removed from:\n" + "• conditions.txt\n"
							+ "• All .txt resource files\n" + "• Progress files\n\n" + "No source code changes needed!",
					"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error during deletion: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			parent.log("Error: " + e.getMessage());
		}
	}

	/**
	 * Triggers live update of all loaded scenes and items
	 */
	private void triggerLiveUpdate() {
		try {
			parent.log("Triggering live update of scenes and items...");

			// 1. Reload Conditions from file (in case they were deleted)
			parent.log("Reloading conditions from file...");
			Conditions.reloadConditions();

			// Get the current game instance
			AdventureGame game = parent.getGame();
			if (game == null) {
				parent.log("Warning: No game instance found, skipping live update");
				return;
			}

			// Store current scene name
			Scene currentScene = game.getCurrentScene();
			String currentSceneName = currentScene != null ? currentScene.getName() : null;

			// 2. Reload all scenes
			parent.log("Reloading scenes...");
			game.reloadAllScenes();

			// 3. Reload inventory items
			parent.log("Reloading inventory...");
			game.reloadInventory();

			// 4. Restore current scene
			if (currentSceneName != null) {
				game.loadScene(currentSceneName);
				parent.log("Restored scene: " + currentSceneName);
			}

			// 5. Clean up invalid condition references in loaded items
			parent.log("Cleaning up invalid condition references...");
			cleanupInvalidConditionReferences();

			// 6. Force repaint
			game.repaint();

			parent.log("✓ Live update complete");

		} catch (Exception e) {
			parent.log("Warning during live update: " + e.getMessage());
		}
	}

	/**
	 * Removes references to non-existent conditions from all loaded items and
	 * keyareas
	 */
	private void cleanupInvalidConditionReferences() {
		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null) {
			return;
		}

		Set<String> validConditions = Conditions.getAllConditionNames();
		int removedCount = 0;

		// Clean up Items
		for (Item item : currentScene.getItems()) {
			// Clean conditions map
			Map<String, Boolean> itemConditions = item.getConditions();
			List<String> toRemove = new ArrayList<>();
			for (String condName : itemConditions.keySet()) {
				if (!validConditions.contains(condName)) {
					toRemove.add(condName);
				}
			}
			for (String condName : toRemove) {
				itemConditions.remove(condName);
				removedCount++;
				parent.log("  Removed invalid condition '" + condName + "' from item: " + item.getName());
			}

			// Clean hover display conditions
			Map<String, String> hoverConditions = item.getHoverDisplayConditions();
			toRemove.clear();
			for (String condName : hoverConditions.keySet()) {
				// Extract actual condition name (format: "condName = true")
				String actualName = condName.split("=")[0].trim();
				if (!actualName.equals("none") && !validConditions.contains(actualName)) {
					toRemove.add(condName);
				}
			}
			for (String condName : toRemove) {
				hoverConditions.remove(condName);
				removedCount++;
				parent.log("  Removed invalid hover condition '" + condName + "' from item: " + item.getName());
			}
		}

		// Clean up KeyAreas
		for (KeyArea keyArea : currentScene.getKeyAreas()) {
			// Clean hover display conditions
			Map<String, String> hoverConditions = keyArea.getHoverDisplayConditions();
			List<String> toRemove = new ArrayList<>();
			for (String condName : hoverConditions.keySet()) {
				// Extract actual condition name (format: "condName = true")
				String actualName = condName.split("=")[0].trim();
				if (!actualName.equals("none") && !validConditions.contains(actualName)) {
					toRemove.add(condName);
				}
			}
			for (String condName : toRemove) {
				hoverConditions.remove(condName);
				removedCount++;
				parent.log("  Removed invalid hover condition '" + condName + "' from keyArea: " + keyArea.getName());
			}

			// Clean action conditions
			Map<String, KeyArea.ActionHandler> actions = keyArea.getActions();
			for (Map.Entry<String, KeyArea.ActionHandler> entry : actions.entrySet()) {
				KeyArea.ActionHandler handler = entry.getValue();
				Map<String, String> conditionalResults = handler.getConditionalResults();
				toRemove.clear();
				for (String condName : conditionalResults.keySet()) {
					String actualName = condName.split("=")[0].trim();
					if (!actualName.equals("none") && !validConditions.contains(actualName)) {
						toRemove.add(condName);
					}
				}
				for (String condName : toRemove) {
					conditionalResults.remove(condName);
					removedCount++;
					parent.log(
							"  Removed invalid action condition '" + condName + "' from keyArea: " + keyArea.getName());
				}
			}
		}

		parent.log("✓ Cleaned up " + removedCount + " invalid condition references");
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
			// Update Conditions from table
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean currentValue = (Boolean) tableModel.getValueAt(row, 1);
				Boolean defaultValue = (Boolean) tableModel.getValueAt(row, 2);

				// Set current value
				Conditions.setCondition(name, currentValue);
			}

			// Save current values to progress.txt
			String filename = "resources/progress.txt";
			parent.log("Saving current conditions to: " + filename);

			Scene currentScene = parent.getGame().getCurrentScene();
			String sceneName = currentScene != null ? currentScene.getName() : "sceneBeach";

			Conditions.saveToProgress(filename, sceneName);

			// Save default values to conditions.txt
			parent.log("Saving default values to conditions.txt");
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean defaultValue = (Boolean) tableModel.getValueAt(row, 2);

				// Update the condition with default value temporarily
				Conditions.setCondition(name, defaultValue);
			}
			Conditions.saveConditionsToFile();

			// Restore current values
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean currentValue = (Boolean) tableModel.getValueAt(row, 1);
				Conditions.setCondition(name, currentValue);
			}

			parent.log("✓ Conditions saved!");

			JOptionPane.showMessageDialog(this,
					"✓ Conditions saved successfully!\n\n" + "Saved to:\n" + "• conditions.txt (default values)\n"
							+ "• progress.txt (current values)\n\n" + "No source code changes needed!",
					"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public Map<String, Boolean> getConditions() {
		return new LinkedHashMap<>(conditions);
	}
}