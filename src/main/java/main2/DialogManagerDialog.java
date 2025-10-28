package main2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

/**
 * Dialog zum Verwalten aller Dialoge der aktuellen Scene
 */
public class DialogManagerDialog extends JDialog {
	private EditorWindow parent;
	private JList<String> dialogList;
	private DefaultListModel<String> dialogListModel;
	private JTextArea dialogTextArea;
	private String selectedDialogKey = null;

	public DialogManagerDialog(EditorWindow parent) {
		super(parent, "Dialog Manager", false); // Non-modal
		this.parent = parent;

		setSize(900, 600);
		setLocationRelativeTo(parent);

		initUI();
		loadDialogs();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("💬 Dialog Manager");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Main panel with split view
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		// Left side - Dialog list
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setPreferredSize(new Dimension(300, 0));
		leftPanel.setBorder(BorderFactory.createTitledBorder("All Dialogs"));

		dialogListModel = new DefaultListModel<>();
		dialogList = new JList<>(dialogListModel);
		dialogList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dialogList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onDialogSelected();
			}
		});

		JScrollPane listScroll = new JScrollPane(dialogList);
		leftPanel.add(listScroll, BorderLayout.CENTER);

		// List buttons
		JPanel listButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton newDialogBtn = new JButton("➕ New");
		newDialogBtn.addActionListener(e -> createNewDialog());
		listButtonPanel.add(newDialogBtn);

		JButton deleteDialogBtn = new JButton("🗑️ Delete");
		deleteDialogBtn.addActionListener(e -> deleteDialog());
		listButtonPanel.add(deleteDialogBtn);

		JButton renameDialogBtn = new JButton("✏️ Rename");
		renameDialogBtn.addActionListener(e -> renameDialog());
		listButtonPanel.add(renameDialogBtn);

		leftPanel.add(listButtonPanel, BorderLayout.SOUTH);

		mainPanel.add(leftPanel, BorderLayout.WEST);

		// Right side - Dialog editor
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBorder(BorderFactory.createTitledBorder("Dialog Text"));

		JLabel infoLabel = new JLabel("Enter dialog text (each line will be shown separately):");
		infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		rightPanel.add(infoLabel, BorderLayout.NORTH);

		dialogTextArea = new JTextArea();
		dialogTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		dialogTextArea.setLineWrap(true);
		dialogTextArea.setWrapStyleWord(true);

		JScrollPane textScroll = new JScrollPane(dialogTextArea);
		rightPanel.add(textScroll, BorderLayout.CENTER);

		// Editor buttons
		JPanel editorButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveTextBtn = new JButton("💾 Save Text");
		saveTextBtn.addActionListener(e -> saveDialogText());
		editorButtonPanel.add(saveTextBtn);

		JButton previewBtn = new JButton("👁️ Preview");
		previewBtn.addActionListener(e -> previewDialog());
		editorButtonPanel.add(previewBtn);

		rightPanel.add(editorButtonPanel, BorderLayout.SOUTH);

		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);

		// Bottom buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton saveAllBtn = new JButton("💾 Save All & Close");
		saveAllBtn.addActionListener(e -> saveAndClose());
		bottomPanel.add(saveAllBtn);

		JButton closeBtn = new JButton("✓ Close");
		closeBtn.addActionListener(e -> dispose());
		bottomPanel.add(closeBtn);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadDialogs() {
		dialogListModel.clear();

		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null) {
			parent.log("No scene loaded");
			return;
		}

		Map<String, String> dialogs = currentScene.getDialogs();
		for (String key : dialogs.keySet()) {
			dialogListModel.addElement(key);
		}

		parent.log("Loaded " + dialogListModel.size() + " dialogs");
	}

	private void onDialogSelected() {
		int selectedIndex = dialogList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedDialogKey = null;
			dialogTextArea.setText("");
			dialogTextArea.setEnabled(false);
			return;
		}

		selectedDialogKey = dialogListModel.getElementAt(selectedIndex);
		dialogTextArea.setEnabled(true);

		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null)
			return;

		String dialogText = currentScene.getDialogs().get(selectedDialogKey);
		dialogTextArea.setText(dialogText != null ? dialogText : "");

		parent.log("Selected dialog: " + selectedDialogKey);
	}

	private void createNewDialog() {
		String dialogKey = JOptionPane.showInputDialog(this,
				"Enter dialog key (e.g., 'dialog-door-anschauen'):\n\n"
						+ "Tip: Use format 'dialog-[object]-[action]'",
				"Create New Dialog", JOptionPane.PLAIN_MESSAGE);

		if (dialogKey != null && !dialogKey.trim().isEmpty()) {
			dialogKey = dialogKey.trim();

			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene == null)
				return;

			// Check if already exists
			if (currentScene.getDialogs().containsKey(dialogKey)) {
				JOptionPane.showMessageDialog(this, "Dialog '" + dialogKey + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Create empty dialog
			currentScene.getDialogs().put(dialogKey, "");
			dialogListModel.addElement(dialogKey);

			// Select the new dialog
			dialogList.setSelectedValue(dialogKey, true);

			parent.log("Created new dialog: " + dialogKey);
		}
	}

	private void deleteDialog() {
		if (selectedDialogKey == null) {
			JOptionPane.showMessageDialog(this, "Please select a dialog first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete dialog '" + selectedDialogKey + "'?\n\nThis cannot be undone!", "Confirm Delete",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene == null)
				return;

			currentScene.getDialogs().remove(selectedDialogKey);
			dialogListModel.removeElement(selectedDialogKey);

			parent.log("Deleted dialog: " + selectedDialogKey);

			selectedDialogKey = null;
			dialogTextArea.setText("");
			dialogTextArea.setEnabled(false);
		}
	}

	private void renameDialog() {
		if (selectedDialogKey == null) {
			JOptionPane.showMessageDialog(this, "Please select a dialog first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String newKey = JOptionPane.showInputDialog(this, "Enter new key for dialog:", selectedDialogKey);

		if (newKey != null && !newKey.trim().isEmpty()) {
			newKey = newKey.trim();

			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene == null)
				return;

			// Check if already exists
			if (currentScene.getDialogs().containsKey(newKey) && !newKey.equals(selectedDialogKey)) {
				JOptionPane.showMessageDialog(this, "Dialog '" + newKey + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Rename
			String text = currentScene.getDialogs().get(selectedDialogKey);
			currentScene.getDialogs().remove(selectedDialogKey);
			currentScene.getDialogs().put(newKey, text);

			// Update list
			int index = dialogList.getSelectedIndex();
			dialogListModel.setElementAt(newKey, index);
			selectedDialogKey = newKey;

			parent.log("Renamed dialog to: " + newKey);
		}
	}

	private void saveDialogText() {
		if (selectedDialogKey == null) {
			JOptionPane.showMessageDialog(this, "Please select a dialog first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null)
			return;

		String text = dialogTextArea.getText();
		currentScene.getDialogs().put(selectedDialogKey, text);

		parent.log("Saved text for dialog: " + selectedDialogKey);
		JOptionPane.showMessageDialog(this, "Dialog text saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
	}

	private void previewDialog() {
		if (selectedDialogKey == null) {
			JOptionPane.showMessageDialog(this, "Please select a dialog first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String text = dialogTextArea.getText();
		if (text == null || text.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Dialog is empty!", "Preview", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Show preview
		JOptionPane.showMessageDialog(this, text, "Preview: " + selectedDialogKey, JOptionPane.PLAIN_MESSAGE);
	}

	private void saveAndClose() {
		parent.autoSaveCurrentScene();
		parent.log("✓ All dialogs saved!");
		dispose();
	}
}
