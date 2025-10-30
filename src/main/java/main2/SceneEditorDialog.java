package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog zum Editieren einer Scene
 */
public class SceneEditorDialog extends JDialog {
	private EditorWindow parent;
	private String sceneName;
	private JTextField sceneNameField;
	private JTextField backgroundImageField;
	private JLabel imagePreviewLabel;

	public SceneEditorDialog(EditorWindow parent, String sceneName) {
		super(parent, "Scene Editor - " + sceneName, true);
		this.parent = parent;
		this.sceneName = sceneName;

		setSize(600, 500);
		setLocationRelativeTo(parent);

		initUI();
		loadSceneData();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("Edit Scene: " + sceneName);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Main panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Scene Name
		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(new JLabel("Scene Name:"));
		sceneNameField = new JTextField(sceneName, 25);
		namePanel.add(sceneNameField);
		namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		mainPanel.add(namePanel);

		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Background Image with Drag & Drop
		JPanel imagePanel = new JPanel(new BorderLayout());
		imagePanel.setBorder(BorderFactory.createTitledBorder("Background Image"));
		imagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

		// Drop zone
		JLabel dropLabel = new JLabel("<html><center>📁<br>Drag & Drop Background Image Here<br>(PNG, JPG)</center></html>");
		dropLabel.setHorizontalAlignment(JLabel.CENTER);
		dropLabel.setVerticalAlignment(JLabel.CENTER);
		dropLabel.setFont(new Font("Arial", Font.BOLD, 14));
		dropLabel.setPreferredSize(new Dimension(400, 150));
		dropLabel.setBorder(BorderFactory.createDashedBorder(null, 2, 5, 5, true));

		// Enable drag & drop
		dropLabel.setDropTarget(new DropTarget() {
			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					@SuppressWarnings("unchecked")
					List<File> droppedFiles = (List<File>) evt.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					if (!droppedFiles.isEmpty()) {
						File file = droppedFiles.get(0);
						if (isImageFile(file)) {
							backgroundImageField.setText(file.getName());
							updateImagePreview(file.getAbsolutePath());
							parent.log("Image selected: " + file.getName());
						} else {
							JOptionPane.showMessageDialog(SceneEditorDialog.this,
									"Please drop an image file (PNG, JPG)", "Invalid File",
									JOptionPane.WARNING_MESSAGE);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		imagePanel.add(dropLabel, BorderLayout.NORTH);

		// Image field
		JPanel imageFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imageFieldPanel.add(new JLabel("Image File:"));
		backgroundImageField = new JTextField(20);
		imageFieldPanel.add(backgroundImageField);

		JButton browseBtn = new JButton("Browse...");
		browseBtn.addActionListener(e -> browseForImage());
		imageFieldPanel.add(browseBtn);
		imagePanel.add(imageFieldPanel, BorderLayout.CENTER);

		// Image preview
		imagePreviewLabel = new JLabel();
		imagePreviewLabel.setHorizontalAlignment(JLabel.CENTER);
		imagePreviewLabel.setPreferredSize(new Dimension(400, 100));
		imagePanel.add(imagePreviewLabel, BorderLayout.SOUTH);

		mainPanel.add(imagePanel);

		add(mainPanel, BorderLayout.CENTER);

		// Bottom panel with buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton saveBtn = new JButton("💾 Save Changes");
		saveBtn.addActionListener(e -> saveChanges());
		bottomPanel.add(saveBtn);

		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(e -> dispose());
		bottomPanel.add(cancelBtn);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadSceneData() {
		try {
			String bgImage = SceneReferenceManager.getSceneBackgroundImage(sceneName);
			if (bgImage != null) {
				backgroundImageField.setText(bgImage);
				updateImagePreview("resources/images/" + bgImage);
			}
		} catch (Exception e) {
			parent.log("Error loading scene data: " + e.getMessage());
		}
	}

	private void saveChanges() {
		String newName = sceneNameField.getText().trim();
		String newBackgroundImage = backgroundImageField.getText().trim();

		if (newName.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Scene name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			boolean renamed = false;

			// Check if name changed
			if (!newName.equals(sceneName)) {
				// Check if new name already exists
				File newFile = new File("resources/scenes/" + newName + ".txt");
				if (newFile.exists()) {
					JOptionPane.showMessageDialog(this, "A scene with name '" + newName + "' already exists!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Confirm rename
				int confirm = JOptionPane.showConfirmDialog(this,
						"Rename scene from '" + sceneName + "' to '" + newName + "'?\n\n" +
						"This will update all references in .txt files.",
						"Confirm Rename", JOptionPane.YES_NO_OPTION);

				if (confirm != JOptionPane.YES_OPTION) {
					return;
				}

				// Rename references in all .txt files
				parent.log("Renaming scene references from '" + sceneName + "' to '" + newName + "'...");
				List<String> modifiedFiles = SceneReferenceManager.renameSceneReferences(sceneName, newName);
				parent.log("Updated " + modifiedFiles.size() + " file(s)");

				// Rename the scene file itself
				if (SceneReferenceManager.renameSceneFile(sceneName, newName)) {
					parent.log("Scene file renamed successfully");
					sceneName = newName;
					renamed = true;
				} else {
					JOptionPane.showMessageDialog(this, "Failed to rename scene file!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			// Update background image
			if (!newBackgroundImage.isEmpty()) {
				SceneReferenceManager.setSceneBackgroundImage(sceneName, newBackgroundImage);
				parent.log("Background image updated: " + newBackgroundImage);
			}

			// Reload scene in game
			parent.getGame().loadScene(sceneName);

			parent.log("✓ Scene saved successfully!");

			String message = "✓ Scene saved successfully!";
			if (renamed) {
				message += "\n\nScene renamed and all references updated.";
			}

			JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);

			dispose();

		} catch (Exception e) {
			parent.log("Error saving scene: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error saving scene: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void browseForImage() {
		File imagesDir = new File("resources/images");
		if (!imagesDir.exists()) {
			imagesDir.mkdirs();
		}

		// Simple file browser
		String filename = JOptionPane.showInputDialog(this,
				"Enter image filename (e.g., 'background.png'):",
				backgroundImageField.getText());

		if (filename != null && !filename.trim().isEmpty()) {
			backgroundImageField.setText(filename.trim());
			updateImagePreview("resources/images/" + filename.trim());
		}
	}

	private void updateImagePreview(String imagePath) {
		try {
			File imageFile = new File(imagePath);
			if (imageFile.exists()) {
				ImageIcon icon = new ImageIcon(imagePath);
				Image img = icon.getImage().getScaledInstance(300, 80, Image.SCALE_SMOOTH);
				imagePreviewLabel.setIcon(new ImageIcon(img));
				imagePreviewLabel.setText("");
			} else {
				imagePreviewLabel.setIcon(null);
				imagePreviewLabel.setText("Image not found: " + imagePath);
			}
		} catch (Exception e) {
			imagePreviewLabel.setIcon(null);
			imagePreviewLabel.setText("Error loading image");
		}
	}

	private boolean isImageFile(File file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
	}
}
