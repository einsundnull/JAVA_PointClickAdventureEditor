package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import main.ui.components.button.AppButton;
import main.ui.theme.Spacing;
// New UI imports
import main.ui.theme.ThemeManager;

/**
 * Dialog zum Verwalten aller Conditions
 * Kann von EditorWindow oder anderen Dialogen (Scene/Item Editor) aufgerufen werden
 */
public class ConditionsManagerDialog extends JDialog {
	private JTable conditionsTable;
	private DefaultTableModel tableModel;
	private Map<String, Boolean> conditions;
	private EditorMain parent; // Can be null if called from other dialogs
	private boolean changesMade = false;
	private AdventureGame game; // Reference to game for mode checking

	/**
	 * Constructor for EditorWindow (non-modal)
	 */
	public ConditionsManagerDialog(EditorMain parent) {
		super(parent, "Conditions Manager", false); // Non-modal
		this.parent = parent;
		this.game = parent.getGame();
		this.conditions = new LinkedHashMap<>();

		setSize(600, 400);
		setLocationRelativeTo(parent);

		initUI();
		loadConditions();
	}

	/**
	 * Constructor for other dialogs like Scene/Item Editor (modal)
	 */
	public ConditionsManagerDialog(java.awt.Frame parentFrame) {
		super(parentFrame, "Manage Conditions", true); // Modal
		this.parent = null; // No EditorWindow parent
		this.game = null; // Will need to get it another way
		this.conditions = new LinkedHashMap<>();

		setSize(600, 400);
		setLocationRelativeTo(parentFrame);

		initUI();
		loadConditions();
	}

	private void initUI() {
		var c = ThemeManager.colors();
		var t = ThemeManager.typography();

		setLayout(new BorderLayout(Spacing.SM, Spacing.SM));

		// Title Panel with Theme Toggle
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(c.getBackgroundPanel());
		titlePanel.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.XS, Spacing.SM));

		JLabel titleLabel = new JLabel("🎛️ Boolean Conditions Manager");
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

		// Table
		String[] columns = { "Condition Name", "Progress Value", "Default Value" };
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public Class<?> getColumnClass(int column) {
				return column == 0 ? String.class : Boolean.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1 || column == 2; // Progress and default editable
			}
		};

		conditionsTable = new JTable(tableModel);
		conditionsTable.setRowHeight(Spacing.TABLE_ROW_HEIGHT - 4);
		conditionsTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		conditionsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		conditionsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		conditionsTable.setBackground(c.getBackgroundPanel());
		conditionsTable.setForeground(c.getTextPrimary());

		// Listen for changes
		conditionsTable.getModel().addTableModelListener(e -> {
			if (e.getType() == TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				int column = e.getColumn();
				String name = (String) tableModel.getValueAt(row, 0);

				if (column == 1) {
					// Progress value changed (column 1)
					Boolean value = (Boolean) tableModel.getValueAt(row, 1);
					conditions.put(name, value);
					// Update RAM immediately for live preview
					Conditions.setCondition(name, value);
					changesMade = true;
				}
				// Default value changed (column 2)
				if (column == 2) {
					changesMade = true;
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(conditionsTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, Spacing.SM, Spacing.SM, Spacing.SM));

		add(scrollPane, BorderLayout.CENTER);

		// Bottom panel with buttons - Use AppButton
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, Spacing.SM));
		bottomPanel.setBackground(c.getBackgroundElevated());
		bottomPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, c.getBorderDefault()),
			BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
		));

		AppButton addButton = new AppButton("➕ Add New", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		addButton.addActionListener(e -> addNewCondition());
		bottomPanel.add(addButton);

		AppButton deleteButton = new AppButton("🗑️ Delete Selected", AppButton.Variant.DANGER, AppButton.Size.SMALL);
		deleteButton.addActionListener(e -> deleteSelectedCondition());
		bottomPanel.add(deleteButton);

		AppButton saveDefaultButton = new AppButton("💾 Save to Default", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		saveDefaultButton.setToolTipText("Save current values to conditions/conditions.txt");
		saveDefaultButton.addActionListener(e -> saveToDefault());
		bottomPanel.add(saveDefaultButton);

		AppButton saveProgressButton = new AppButton("📝 Save to Progress", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		saveProgressButton.setToolTipText("Save current values to conditions-progress.txt");
		saveProgressButton.addActionListener(e -> saveToProgress());
		bottomPanel.add(saveProgressButton);

		AppButton closeButton = new AppButton("✓ Close", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		closeButton.addActionListener(e -> dispose());
		bottomPanel.add(closeButton);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadConditions() {
		conditions.clear();
		tableModel.setRowCount(0);

		// Load PROGRESS values from conditions/conditions.txt
		Map<String, Boolean> progressValues = loadConditionsFromFile(ResourcePathHelper.resolvePath("conditions/conditions.txt"));

		// Load DEFAULT values from conditions/conditions_defaults.txt
		Map<String, Boolean> defaultValues = loadConditionsFromFile(ResourcePathHelper.resolvePath("conditions/conditions_defaults.txt"));

		// Combine all condition names from both files
		java.util.Set<String> allNames = new java.util.TreeSet<>();
		allNames.addAll(progressValues.keySet());
		allNames.addAll(defaultValues.keySet());

		// Add rows to table
		for (String name : allNames) {
			boolean progressValue = progressValues.getOrDefault(name, false);
			boolean defaultValue = defaultValues.getOrDefault(name, false);

			conditions.put(name, progressValue);
			tableModel.addRow(new Object[] { name, progressValue, defaultValue });
		}

		log("Loaded " + conditions.size() + " conditions from files");
		log("  Progress: resources/conditions/conditions.txt");
		log("  Defaults: resources/conditions/conditions_defaults.txt");
	}

	/**
	 * Loads conditions from a specific file
	 * @param filename Path to the file
	 * @return Map of condition name -> value
	 */
	private Map<String, Boolean> loadConditionsFromFile(String filename) {
		Map<String, Boolean> result = new java.util.LinkedHashMap<>();

		try {
			java.io.File file = new java.io.File(filename);
			if (!file.exists()) {
				log("Warning: File not found: " + filename);
				return result;
			}

			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				line = line.trim();

				// Skip empty lines and comments
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				// Parse: conditionName = value
				if (line.contains("=")) {
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String name = parts[0].trim();
						boolean value = Boolean.parseBoolean(parts[1].trim());
						result.put(name, value);
					}
				}
			}

			reader.close();
		} catch (Exception e) {
			log("ERROR loading from " + filename + ": " + e.getMessage());
		}

		return result;
	}

	/**
	 * Helper method to log messages (handles null parent)
	 */
	private void log(String message) {
		if (parent != null) {
			parent.log(message);
		} else {
			System.out.println("[ConditionsManager] " + message);
		}
	}

	/**
	 * Returns true if changes were made to conditions
	 */
	public boolean changesMade() {
		return changesMade;
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
			changesMade = true;

			log("✓ Added new condition: " + name);

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
			log("Searching for references to condition: " + name);
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
			log("Starting cascade deletion for: " + name);

			// 1. Remove from .txt files
			List<String> modifiedFiles = ConditionReferenceManager.removeConditionReferences(name);
			log("Modified " + modifiedFiles.size() + " file(s):");
			for (String file : modifiedFiles) {
				log("  - " + file);
			}

			// 2. Remove from defaults
			if (ConditionReferenceManager.removeFromDefaults(name)) {
				log("Removed from conditions-defaults.txt");
			}

			// 3. Remove from progress files
			ConditionReferenceManager.removeFromProgressFiles(name);
			log("Removed from progress files");

			// 4. Remove from Conditions system (automatically updates conditions.txt!)
			Conditions.removeCondition(name);
			log("✓ Removed from conditions.txt");

			// 5. Remove from table
			conditions.remove(name);
			tableModel.removeRow(row);
			changesMade = true;

			// 6. Trigger live update of scenes and items
			triggerLiveUpdate();

			log("✓ Cascade deletion complete for: " + name);

			JOptionPane.showMessageDialog(this,
					"✓ Condition deleted successfully!\n\n" + "Removed from:\n" + "• conditions.txt\n"
							+ "• All .txt resource files\n" + "• Progress files\n\n" + "No source code changes needed!",
					"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error during deletion: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			log("Error: " + e.getMessage());
		}
	}

	/**
	 * Triggers live update of all loaded scenes and items
	 */
	private void triggerLiveUpdate() {
		try {
			log("Triggering live update of scenes and items...");

			// 1. Reload Conditions from file (in case they were deleted)
			log("Reloading conditions from file...");
			Conditions.reloadConditions();

			// Get the current game instance
			AdventureGame game = parent.getGame();
			if (game == null) {
				log("Warning: No game instance found, skipping live update");
				return;
			}

			// Store current scene name
			Scene currentScene = game.getCurrentScene();
			String currentSceneName = currentScene != null ? currentScene.getName() : null;

			// 2. Reload all scenes
			log("Reloading scenes...");
			game.reloadAllScenes();

			// 3. Reload inventory items
			log("Reloading inventory...");
			game.reloadInventory();

			// 4. Restore current scene
			if (currentSceneName != null) {
				game.loadScene(currentSceneName);
				log("Restored scene: " + currentSceneName);
			}

			// 5. Clean up invalid condition references in loaded items
			log("Cleaning up invalid condition references...");
			cleanupInvalidConditionReferences();

			// 6. Force repaint
			game.repaint();

			log("✓ Live update complete");

		} catch (Exception e) {
			log("Warning during live update: " + e.getMessage());
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
				log("  Removed invalid condition '" + condName + "' from item: " + item.getName());
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
				log("  Removed invalid hover condition '" + condName + "' from item: " + item.getName());
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
				log("  Removed invalid hover condition '" + condName + "' from keyArea: " + keyArea.getName());
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
					log(
							"  Removed invalid action condition '" + condName + "' from keyArea: " + keyArea.getName());
				}
			}
		}

		log("✓ Cleaned up " + removedCount + " invalid condition references");
	}

	private Map<String, Boolean> loadDefaultValues() {
		Map<String, Boolean> defaults = new HashMap<>();
		File file = ResourcePathHelper.resolve("conditions-defaults.txt");

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
			log("Could not load defaults: " + e.getMessage());
		}

		return defaults;
	}

	/**
	 * Saves DEFAULT values (column 2) to conditions/conditions_defaults.txt
	 */
	private void saveToDefault() {
		try {
			// Collect default values from column 2
			Map<String, Boolean> defaultValues = new java.util.LinkedHashMap<>();
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean defaultValue = (Boolean) tableModel.getValueAt(row, 2);
				defaultValues.put(name, defaultValue);
			}

			// Save to conditions_defaults.txt
			saveConditionsToFile(ResourcePathHelper.resolvePath("conditions/conditions_defaults.txt"), defaultValues, "DEFAULT TEMPLATE VALUES");
			log("✓ Saved DEFAULT values to conditions/conditions_defaults.txt");

			JOptionPane.showMessageDialog(this,
				"✓ Default conditions saved successfully!\n\n" +
				"Saved to: conditions/conditions_defaults.txt\n" +
				"Type: DEFAULT TEMPLATE VALUES",
				"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving defaults: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			log("ERROR: " + e.getMessage());
		}
	}

	/**
	 * Saves PROGRESS values (column 1) to conditions/conditions.txt
	 */
	private void saveToProgress() {
		try {
			// Collect progress values from column 1
			Map<String, Boolean> progressValues = new java.util.LinkedHashMap<>();
			for (int row = 0; row < tableModel.getRowCount(); row++) {
				String name = (String) tableModel.getValueAt(row, 0);
				Boolean progressValue = (Boolean) tableModel.getValueAt(row, 1);
				progressValues.put(name, progressValue);
			}

			// Save to conditions.txt
			saveConditionsToFile(ResourcePathHelper.resolvePath("conditions/conditions.txt"), progressValues, "CURRENT GAME STATE");
			log("✓ Saved PROGRESS values to conditions/conditions.txt");

			// Also update RAM for immediate effect
			for (Map.Entry<String, Boolean> entry : progressValues.entrySet()) {
				Conditions.setCondition(entry.getKey(), entry.getValue());
			}

			JOptionPane.showMessageDialog(this,
				"✓ Progress conditions saved successfully!\n\n" +
				"Saved to: conditions/conditions.txt\n" +
				"Type: CURRENT GAME STATE\n\n" +
				"Changes are active immediately.",
				"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving progress: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			log("ERROR: " + e.getMessage());
		}
	}

	/**
	 * Saves conditions to a specific file
	 */
	private void saveConditionsToFile(String filename, Map<String, Boolean> values, String description) throws Exception {
		java.io.File file = new java.io.File(filename);
		file.getParentFile().mkdirs();

		java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));
		writer.write("# Conditions File\n");
		writer.write("# Type: " + description + "\n");
		writer.write("# Format: conditionName = value\n\n");

		for (Map.Entry<String, Boolean> entry : values.entrySet()) {
			writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
		}

		writer.close();
	}

	public Map<String, Boolean> getConditions() {
		return new LinkedHashMap<>(conditions);
	}
}