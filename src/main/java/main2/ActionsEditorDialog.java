package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Dialog zum Editieren aller Actions einer KeyArea
 */
public class ActionsEditorDialog extends JDialog {
	private KeyArea keyArea;
	private EditorWindow parent;

	private static final String[] ALL_ACTIONS = { "Nimm", "Gib", "Ziehe", "Drücke", "Drehe", "Hebe", "Anschauen",
			"Sprich", "Benutze", "Gehe zu" };

	private JTabbedPane tabbedPane;

	public ActionsEditorDialog(EditorWindow parent, KeyArea keyArea) {
		super(parent, "Actions Editor - " + keyArea.getName(), false); // Non-modal
		this.parent = parent;
		this.keyArea = keyArea;

		setSize(900, 600);
		setLocationRelativeTo(parent);

		initUI();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("⚙️ Actions Editor: " + keyArea.getName());
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
		// Special panel for "Gehe zu" action
		if ("Gehe zu".equals(actionName)) {
			return createGeheZuPanel(actionName);
		}

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Info label
		JLabel infoLabel = new JLabel("<html><b>Configure conditions and results for: " + actionName + "</b><br>" +
				"Click the button below to open the new Actions Editor with checkbox-based interface.</html>");
		infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		panel.add(infoLabel, BorderLayout.NORTH);

		// Center panel with button to open new editor
		JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 50));
		JButton openEditorBtn = new JButton("Open Actions Editor for " + actionName);
		openEditorBtn.setFont(new Font("Arial", Font.BOLD, 16));
		openEditorBtn.setPreferredSize(new Dimension(400, 50));
		openEditorBtn.addActionListener(e -> {
			NewActionsEditorDialog dialog = new NewActionsEditorDialog(parent, keyArea, actionName);
			dialog.setVisible(true);
		});
		centerPanel.add(openEditorBtn);
		panel.add(centerPanel, BorderLayout.CENTER);

		return panel;
	}

	/**
	 * Creates special panel for "Gehe zu" action
	 * This action can have 0 or exactly 1 entry (condition -> scene)
	 */
	private JPanel createGeheZuPanel(String actionName) {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Info label
		JLabel infoLabel = new JLabel("<html><b>Gehe zu: Scene-Wechsel</b><br>" +
				"Wenn die Condition erfüllt ist, wechselt die Scene.</html>");
		infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		panel.add(infoLabel, BorderLayout.NORTH);

		// Center panel with condition and scene selection
		JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

		// Condition selection
		centerPanel.add(new JLabel("Condition:"));
		JComboBox<String> conditionCombo = new JComboBox<>();
		conditionCombo.addItem("none");
		for (String conditionName : Conditions.getAllConditionNames()) {
			conditionCombo.addItem(conditionName + " = true");
			conditionCombo.addItem(conditionName + " = false");
		}
		conditionCombo.setPreferredSize(new Dimension(200, 30));
		centerPanel.add(conditionCombo);

		centerPanel.add(new JLabel("  →  "));

		// Scene selection
		centerPanel.add(new JLabel("Ziel-Scene:"));
		JComboBox<String> sceneCombo = new JComboBox<>();
		java.util.List<String> scenes = SceneReferenceManager.getAllSceneNames();
		for (String sceneName : scenes) {
			sceneCombo.addItem(sceneName);
		}
		sceneCombo.setPreferredSize(new Dimension(200, 30));
		centerPanel.add(sceneCombo);

		panel.add(centerPanel, BorderLayout.CENTER);

		// Load existing data
		KeyArea.ActionHandler handler = keyArea.getActions().get(actionName);
		if (handler != null && handler.getConditionalResults() != null && !handler.getConditionalResults().isEmpty()) {
			// Load the first (and only) entry
			Map.Entry<String, String> entry = handler.getConditionalResults().entrySet().iterator().next();
			String condition = entry.getKey();
			String result = entry.getValue();

			conditionCombo.setSelectedItem(condition);

			// Extract scene name from result (format: ##loadSceneName or #GeheZu:SceneName)
			if (result.startsWith("##load")) {
				sceneCombo.setSelectedItem(result.substring(6).trim());
			} else if (result.startsWith("#GeheZu:")) {
				sceneCombo.setSelectedItem(result.substring(8).trim());
			}
		}

		// Store components for later retrieval during save
		panel.putClientProperty("actionName", actionName);
		panel.putClientProperty("conditionCombo", conditionCombo);
		panel.putClientProperty("sceneCombo", sceneCombo);

		return panel;
	}

	private void saveAllActions() {
		parent.log("Saving actions for " + keyArea.getName() + "...");

		// Clear existing actions
		keyArea.getActions().clear();

		// Save each tab
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			String actionName = tabbedPane.getTitleAt(i);

			if (comp instanceof JPanel) {
				JPanel panel = (JPanel) comp;

				// Special handling for "Gehe zu" action
				if ("Gehe zu".equals(actionName)) {
					@SuppressWarnings("unchecked")
					JComboBox<String> conditionCombo = (JComboBox<String>) panel.getClientProperty("conditionCombo");
					@SuppressWarnings("unchecked")
					JComboBox<String> sceneCombo = (JComboBox<String>) panel.getClientProperty("sceneCombo");

					if (conditionCombo != null && sceneCombo != null) {
						String condition = (String) conditionCombo.getSelectedItem();
						String sceneName = (String) sceneCombo.getSelectedItem();

						if (sceneName != null && !sceneName.trim().isEmpty()) {
							KeyArea.ActionHandler handler = new KeyArea.ActionHandler();
							String result = "##load" + sceneName.trim();
							handler.addConditionalResult(condition, result);
							keyArea.addAction(actionName, handler);
							parent.log("  " + actionName + ": " + condition + " → " + result);
						}
					}
				}
				// Note: Other actions are now handled by NewActionsEditorDialog
				// which saves directly to resources/actions/<keyAreaName>.txt
			}
		}

		// Auto-save to file
		parent.autoSaveCurrentScene();

		parent.log("✓ Actions saved!");

		JOptionPane.showMessageDialog(this, "Actions saved successfully!\n\n" + "Saved to scene file.", "Success",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void openConditionsManager() {
		parent.log("Opening Conditions Manager from Actions Editor...");
		ConditionsManagerDialog dialog = new ConditionsManagerDialog(parent);
		dialog.setVisible(true);
		parent.log("Conditions Manager closed");
	}

	private void openHoverEditor() {
		parent.log("Opening Hover Editor from Actions Editor...");
		HoverEditorDialog dialog = new HoverEditorDialog(parent, keyArea);
		dialog.setVisible(true);
		parent.log("Hover Editor closed");
	}

	private void openDialogManager() {
		parent.log("Opening Dialog Manager from Actions Editor...");
		DialogManagerDialog dialog = new DialogManagerDialog(parent);
		dialog.setVisible(true);
		parent.log("Dialog Manager opened");
	}
}
