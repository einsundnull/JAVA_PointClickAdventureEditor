package main2;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Dialog zum Verwalten aller Items
 */
public class ItemEditorDialog extends JDialog {
	private EditorWindow parent;
	private JList<String> itemList;
	private DefaultListModel<String> itemListModel;
	private Item selectedItem = null;

	// Edit fields
	private JTextField nameField;
	private JTextField imageFileField;
	private JTextField imagePathField;
	private JTextField posXField;
	private JTextField posYField;
	private JCheckBox inInventoryCheckbox;
	private JTable conditionsTable;
	private DefaultTableModel conditionsTableModel;

	public ItemEditorDialog(EditorWindow parent) {
		this(parent, null);
	}

	public ItemEditorDialog(EditorWindow parent, Item preSelectedItem) {
		super(parent, "Item Editor", false); // Non-modal
		this.parent = parent;

		setSize(900, 700);
		setLocationRelativeTo(parent);

		initUI();
		loadAllItems();

		// Pre-select item if provided
		if (preSelectedItem != null) {
			selectItemInList(preSelectedItem);
		}
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("Item Editor");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Main panel with split view
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		// Left side - Item list
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setPreferredSize(new Dimension(250, 0));
		leftPanel.setBorder(BorderFactory.createTitledBorder("All Items"));

		itemListModel = new DefaultListModel<>();
		itemList = new JList<>(itemListModel);
		itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		itemList.setCellRenderer(new ItemTileRenderer());
		itemList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onItemSelected();
			}
		});

		JScrollPane listScroll = new JScrollPane(itemList);
		leftPanel.add(listScroll, BorderLayout.CENTER);

		// List buttons
		JPanel listButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton newItemBtn = new JButton("➕ New");
		newItemBtn.addActionListener(e -> createNewItem());
		listButtonPanel.add(newItemBtn);

		JButton deleteItemBtn = new JButton("🗑️ Delete");
		deleteItemBtn.addActionListener(e -> deleteItem());
		listButtonPanel.add(deleteItemBtn);

		JButton addToSceneBtn = new JButton("➕ Add to Scene");
		addToSceneBtn.addActionListener(e -> addItemToScene());
		listButtonPanel.add(addToSceneBtn);

		leftPanel.add(listButtonPanel, BorderLayout.SOUTH);

		mainPanel.add(leftPanel, BorderLayout.WEST);

		// Right side - Item editor
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setBorder(BorderFactory.createTitledBorder("Item Properties"));

		// Name
		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(new JLabel("Name:"));
		nameField = new JTextField(20);
		nameField.setEnabled(false);
		namePanel.add(nameField);
		rightPanel.add(namePanel);

		// Image Drop Zone - Visual drag & drop area
		JPanel dropZonePanel = new JPanel(new BorderLayout());
		dropZonePanel.setBorder(BorderFactory.createTitledBorder("Image Drop Zone"));
		dropZonePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

		JLabel dropLabel = new JLabel("<html><center>📁<br>Drag & Drop Image Here<br>(PNG, JPG, GIF)</center></html>");
		dropLabel.setHorizontalAlignment(JLabel.CENTER);
		dropLabel.setVerticalAlignment(JLabel.CENTER);
		dropLabel.setFont(new Font("Arial", Font.BOLD, 14));
		dropLabel.setForeground(new Color(100, 100, 100));
		dropLabel.setPreferredSize(new Dimension(400, 100));
		dropLabel.setBorder(BorderFactory.createDashedBorder(new Color(150, 150, 150), 2, 5, 5, true));
		dropLabel.setOpaque(true);
		dropLabel.setBackground(new Color(245, 245, 245));

		// Enable drag & drop on dropLabel
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
							imageFileField.setText(file.getName());
							imagePathField.setText(file.getAbsolutePath());
							dropLabel.setText("<html><center>✓<br>" + file.getName() + "<br>Image loaded!</center></html>");
							dropLabel.setForeground(new Color(0, 150, 0));
							dropLabel.setBackground(new Color(230, 255, 230));

							// Auto-save and update lists immediately
							if (selectedItem != null) {
								selectedItem.setImageFileName(file.getName());
								selectedItem.setImageFilePath(file.getAbsolutePath());
								try {
									ItemSaver.saveItemByName(selectedItem);
									updateAllItemLists();
									parent.autoSaveCurrentScene();
									parent.log("✓ Image updated: " + file.getName());
								} catch (Exception saveEx) {
									parent.log("ERROR saving after image drop: " + saveEx.getMessage());
								}
							}
						} else {
							JOptionPane.showMessageDialog(ItemEditorDialog.this,
									"Please drop an image file (PNG, JPG, GIF)", "Invalid File",
									JOptionPane.WARNING_MESSAGE);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		dropZonePanel.add(dropLabel, BorderLayout.CENTER);
		rightPanel.add(dropZonePanel);
		rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Image File with Drag & Drop
		JPanel imageFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imageFilePanel.add(new JLabel("Image File:"));
		imageFileField = new JTextField(25);
		imageFileField.setToolTipText("Drag & drop image file here or use browse button");

		// Enable drag & drop on imageFileField
		imageFileField.setDropTarget(new DropTarget() {
			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					@SuppressWarnings("unchecked")
					List<File> droppedFiles = (List<File>) evt.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					if (!droppedFiles.isEmpty()) {
						File file = droppedFiles.get(0);
						if (isImageFile(file)) {
							imageFileField.setText(file.getName());
							imagePathField.setText(file.getAbsolutePath());
						} else {
							JOptionPane.showMessageDialog(ItemEditorDialog.this,
									"Please drop an image file (PNG, JPG, GIF)", "Invalid File",
									JOptionPane.WARNING_MESSAGE);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		imageFilePanel.add(imageFileField);
		JButton browseImageBtn = new JButton("📂");
		browseImageBtn.addActionListener(e -> browseImageFile());
		imageFilePanel.add(browseImageBtn);
		rightPanel.add(imageFilePanel);

		// Image manipulation buttons
		JPanel imageControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imageControlPanel.setBorder(BorderFactory.createTitledBorder("Image Transform"));

		JButton rotateLeftBtn = new JButton("↶ -90°");
		rotateLeftBtn.setToolTipText("Rotate image 90° counter-clockwise");
		rotateLeftBtn.addActionListener(e -> rotateItemImage(-90));
		imageControlPanel.add(rotateLeftBtn);

		JButton rotateRightBtn = new JButton("↷ +90°");
		rotateRightBtn.setToolTipText("Rotate image 90° clockwise");
		rotateRightBtn.addActionListener(e -> rotateItemImage(90));
		imageControlPanel.add(rotateRightBtn);

		JButton flipHBtn = new JButton("⇄ Flip H");
		flipHBtn.setToolTipText("Flip image horizontally");
		flipHBtn.addActionListener(e -> flipItemImage(true));
		imageControlPanel.add(flipHBtn);

		JButton flipVBtn = new JButton("⇅ Flip V");
		flipVBtn.setToolTipText("Flip image vertically");
		flipVBtn.addActionListener(e -> flipItemImage(false));
		imageControlPanel.add(flipVBtn);

		rightPanel.add(imageControlPanel);

		// Image Path
		JPanel imagePathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imagePathPanel.add(new JLabel("Image Path:"));
		imagePathField = new JTextField(30);
		imagePathPanel.add(imagePathField);
		rightPanel.add(imagePathPanel);

		// Position
		JPanel posPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		posPanel.add(new JLabel("Position X:"));
		posXField = new JTextField(5);
		posPanel.add(posXField);
		posPanel.add(new JLabel("Y:"));
		posYField = new JTextField(5);
		posPanel.add(posYField);

		JButton editPointsBtn = new JButton("📍 Edit Points");
		editPointsBtn.setToolTipText("Edit click area polygon points");
		editPointsBtn.addActionListener(e -> openPointEditor());
		posPanel.add(editPointsBtn);

		JButton actionsBtn = new JButton("⚡ Actions");
		actionsBtn.setToolTipText("Edit item actions (like KeyArea actions)");
		actionsBtn.addActionListener(e -> openActionsDialog());
		posPanel.add(actionsBtn);

		JButton mouseHoverBtn = new JButton("🖱️ Mouse Hover");
		mouseHoverBtn.setToolTipText("Edit mouse hover text");
		mouseHoverBtn.addActionListener(e -> openMouseHoverDialog());
		posPanel.add(mouseHoverBtn);

		rightPanel.add(posPanel);

		// In Inventory
		JPanel inventoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		inInventoryCheckbox = new JCheckBox("Is in Inventory");
		inventoryPanel.add(inInventoryCheckbox);
		rightPanel.add(inventoryPanel);

		rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Conditions table
		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder("Conditions"));
		conditionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

		String[] columns = { "Condition", "Value", "Delete" };
		conditionsTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public Class<?> getColumnClass(int column) {
				if (column == 1)
					return Boolean.class;
				if (column == 2)
					return JButton.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				// Column 0 (Condition): editable
				// Column 1 (Value): editable
				// Column 2 (Delete): editable (button)
				return true;
			}
		};

		conditionsTable = new JTable(conditionsTableModel);
		conditionsTable.setRowHeight(25);

		// Setup delete button column
		conditionsTable.getColumn("Delete").setCellRenderer(new ButtonRenderer());
		conditionsTable.getColumn("Delete").setCellEditor(new ButtonEditor(new JCheckBox()));

		JScrollPane conditionsScroll = new JScrollPane(conditionsTable);
		conditionsPanel.add(conditionsScroll, BorderLayout.CENTER);

		JPanel condButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addCondBtn = new JButton("➕ Add Condition");
		addCondBtn.addActionListener(e -> addCondition());
		condButtonPanel.add(addCondBtn);

		conditionsPanel.add(condButtonPanel, BorderLayout.SOUTH);

		rightPanel.add(conditionsPanel);

		// Save button
		rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveBtn = new JButton("💾 Save Item");
		saveBtn.addActionListener(e -> saveItem());
		savePanel.add(saveBtn);
		rightPanel.add(savePanel);

		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);

		// Bottom buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton renameBtn = new JButton("✏️ Rename Item");
		renameBtn.setToolTipText("Rename the selected item");
		renameBtn.addActionListener(e -> renameItem());
		bottomPanel.add(renameBtn);

		JButton deleteBtn = new JButton("🗑️ Delete Item");
		deleteBtn.setToolTipText("Delete the selected item");
		deleteBtn.addActionListener(e -> deleteItem());
		bottomPanel.add(deleteBtn);

		JButton openFolderBtn = new JButton("📁 Open Items Folder");
		openFolderBtn.addActionListener(e -> openItemsFolder());
		bottomPanel.add(openFolderBtn);

		JButton closeBtn = new JButton("✓ Close");
		closeBtn.addActionListener(e -> dispose());
		bottomPanel.add(closeBtn);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadAllItems() {
		itemListModel.clear();

		File itemsDir = new File("resources/items");
		if (!itemsDir.exists()) {
			itemsDir.mkdirs();
			parent.log("Created items directory");
			return;
		}

		File[] itemFiles = itemsDir.listFiles((dir, name) -> name.endsWith(".txt"));
		if (itemFiles != null) {
			for (File file : itemFiles) {
				String itemName = file.getName().replace(".txt", "");
				itemListModel.addElement(itemName);
			}
		}

		parent.log("Loaded " + itemListModel.size() + " items");
	}

	private void selectItemInList(Item item) {
		if (item == null) return;

		String itemName = item.getName();
		for (int i = 0; i < itemListModel.size(); i++) {
			if (itemListModel.getElementAt(i).equals(itemName)) {
				itemList.setSelectedIndex(i);
				itemList.ensureIndexIsVisible(i);
				parent.log("Pre-selected item: " + itemName);
				return;
			}
		}
	}

	private void onItemSelected() {
		int selectedIndex = itemList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedItem = null;
			clearFields();
			// Clear selected item in scene
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene != null) {
				currentScene.setSelectedItem(null);
			}
			return;
		}

		String itemName = itemListModel.getElementAt(selectedIndex);

		try {
			selectedItem = ItemLoader.loadItemByName(itemName);
			loadItemToFields();
			parent.log("Selected item: " + itemName);

			// Set as selected item in scene for mouse priority
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene != null) {
				// Find the actual item instance in the scene
				for (Item sceneItem : currentScene.getItems()) {
					if (sceneItem.getName().equals(itemName)) {
						currentScene.setSelectedItem(sceneItem);
						parent.log("Set " + itemName + " as selected item in scene (mouse priority)");
						break;
					}
				}
			}

			// Repaint to highlight selected item
			parent.getGame().repaintGamePanel();
		} catch (Exception e) {
			parent.log("ERROR loading item: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error loading item: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadItemToFields() {
		if (selectedItem == null) {
			clearFields();
			return;
		}

		nameField.setText(selectedItem.getName());
		imageFileField.setText(selectedItem.getImageFileName());
		imagePathField.setText(selectedItem.getImageFilePath());
		posXField.setText(String.valueOf(selectedItem.getPosition().x));
		posYField.setText(String.valueOf(selectedItem.getPosition().y));
		inInventoryCheckbox.setSelected(selectedItem.isInInventory());

		// Load conditions
		conditionsTableModel.setRowCount(0);
		for (java.util.Map.Entry<String, Boolean> entry : selectedItem.getConditions().entrySet()) {
			conditionsTableModel.addRow(new Object[] { entry.getKey(), entry.getValue(), "🗑️" });
		}
	}

	private void clearFields() {
		nameField.setText("");
		imageFileField.setText("");
		imagePathField.setText("");
		posXField.setText("0");
		posYField.setText("0");
		inInventoryCheckbox.setSelected(false);
		conditionsTableModel.setRowCount(0);
	}

	private void createNewItem() {
		String itemName = JOptionPane.showInputDialog(this, "Enter item name (e.g., 'GlassAtBeach'):",
				"Create New Item", JOptionPane.PLAIN_MESSAGE);

		if (itemName != null && !itemName.trim().isEmpty()) {
			itemName = itemName.trim();

			// Check if already exists
			File itemFile = new File("resources/items/" + itemName + ".txt");
			if (itemFile.exists()) {
				JOptionPane.showMessageDialog(this, "Item '" + itemName + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Create new item
			Item newItem = new Item(itemName);
			newItem.setImageFileName("default.png");
			newItem.setImageFilePath("resources/images/items/default.png");

			try {
				ItemSaver.saveItemByName(newItem);
				itemListModel.addElement(itemName);
				itemList.setSelectedValue(itemName, true);
				parent.log("Created new item: " + itemName);
			} catch (Exception e) {
				parent.log("ERROR creating item: " + e.getMessage());
				JOptionPane.showMessageDialog(this, "Error creating item: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void deleteItem() {
		if (selectedItem == null) {
			return;
		}

		String itemName = selectedItem.getName();
		File itemFile = new File("resources/items/" + itemName + ".txt");
		if (itemFile.delete()) {
			itemListModel.removeElement(itemName);
			selectedItem = null;
			clearFields();
			parent.log("Deleted item: " + itemName);

			// Remove from current scene and update all UIs
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene != null) {
				currentScene.getItems().removeIf(item -> item.getName().equals(itemName));
				currentScene.setSelectedItem(null); // Clear selected item
				parent.autoSaveCurrentScene();
				parent.getGame().repaintGamePanel();
				parent.refreshItemList();
			}

			// Refresh this dialog's list
			itemList.repaint();
		}
	}

	private void saveItem() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			// Update item from fields
			selectedItem.setImageFileName(imageFileField.getText());
			selectedItem.setImageFilePath(imagePathField.getText());
			selectedItem.setPosition(Integer.parseInt(posXField.getText()), Integer.parseInt(posYField.getText()));
			selectedItem.setInInventory(inInventoryCheckbox.isSelected());

			// Update conditions
			selectedItem.getConditions().clear();
			for (int i = 0; i < conditionsTableModel.getRowCount(); i++) {
				String condName = (String) conditionsTableModel.getValueAt(i, 0);
				Boolean condValue = (Boolean) conditionsTableModel.getValueAt(i, 1);
				if (condName != null && !condName.trim().isEmpty()) {
					selectedItem.addCondition(condName, condValue);
				}
			}

			// Save to file
			ItemSaver.saveItemByName(selectedItem);
			parent.log("Saved item: " + selectedItem.getName());

			// Update all ListViews immediately
			updateAllItemLists();

			// Auto-save scene to persist changes
			parent.autoSaveCurrentScene();

			JOptionPane.showMessageDialog(this, "Item saved successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			parent.log("ERROR saving item: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error saving item: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Update all item lists to show changes immediately
	 */
	private void updateAllItemLists() {
		// Clear image caches to force reload
		clearImageCaches();

		// Update local list in dialog
		itemList.repaint();
		itemList.revalidate();

		// Update parent EditorWindow list
		parent.refreshItemList();

		// Update game panel
		parent.getGame().repaintGamePanel();

		parent.log("✓ All item lists updated");
	}

	/**
	 * Clear all image caches to force reload of images
	 */
	private void clearImageCaches() {
		// Clear cache in local renderer
		if (itemList.getCellRenderer() instanceof ItemTileRenderer) {
			ItemTileRenderer renderer = (ItemTileRenderer) itemList.getCellRenderer();
			renderer.clearCache();
		}
	}

	private void addCondition() {
		// Get all available conditions from Conditions system (dynamic!)
		Set<String> conditionNames = Conditions.getAllConditionNames();
		String[] validConditions = conditionNames.toArray(new String[0]);

		if (validConditions.length == 0) {
			JOptionPane.showMessageDialog(this, "No conditions available! Create conditions first in the Conditions Manager.",
					"No Conditions", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String selected = (String) JOptionPane.showInputDialog(this, "Select condition:", "Add Condition",
				JOptionPane.PLAIN_MESSAGE, null, validConditions, validConditions[0]);

		if (selected != null) {
			conditionsTableModel.addRow(new Object[] { selected, false, "🗑️" });
		}
	}

	private void addItemToScene() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null) {
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Check if item already in scene
		for (Item item : currentScene.getItems()) {
			if (item.getName().equals(selectedItem.getName())) {
				JOptionPane.showMessageDialog(this, "Item already in scene!", "Info",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
		}

		// Add item to scene
		currentScene.addItem(selectedItem);
		parent.autoSaveCurrentScene();
		parent.getGame().repaintGamePanel();
		parent.log("Added item to scene: " + selectedItem.getName());

		JOptionPane.showMessageDialog(this, "Item added to scene!\n\nYou can now position it in the game.", "Success",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void browseImageFile() {
		// Simple file browser (you can enhance this)
		String filename = JOptionPane.showInputDialog(this, "Enter image filename (e.g., 'glass.png'):",
				imageFileField.getText());
		if (filename != null && !filename.trim().isEmpty()) {
			imageFileField.setText(filename);
			imagePathField.setText("resources/images/items/" + filename);
		}
	}

	private void openItemsFolder() {
		File itemsFolder = new File("resources/items");
		if (!itemsFolder.exists()) {
			itemsFolder.mkdirs();
		}

		try {
			java.awt.Desktop.getDesktop().open(itemsFolder);
			parent.log("Opened items folder");
		} catch (Exception e) {
			parent.log("ERROR opening folder: " + e.getMessage());
		}
	}

	/**
	 * Select an item by name in the list
	 */
	public void selectItemByName(String itemName) {
		for (int i = 0; i < itemListModel.getSize(); i++) {
			if (itemListModel.getElementAt(i).equals(itemName)) {
				itemList.setSelectedIndex(i);
				itemList.ensureIndexIsVisible(i);
				parent.log("Auto-selected item: " + itemName);
				return;
			}
		}
		parent.log("Could not find item to select: " + itemName);
	}

	/**
	 * Open the universal point editor for the selected item
	 */
	private void openPointEditor() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		parent.log("Opening Point Editor for item: " + selectedItem.getName());
		UniversalPointEditorDialog pointEditor = new UniversalPointEditorDialog(parent, selectedItem);
		pointEditor.setVisible(true);
	}

	/**
	 * Check if a file is an image file
	 */
	private boolean isImageFile(File file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")
				|| name.endsWith(".bmp");
	}

	/**
	 * Rotate the current item's image
	 */
	private void rotateItemImage(int degrees) {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String imagePath = imagePathField.getText();
		if (imagePath == null || imagePath.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "No image path set for this item!", "No Image",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			File imageFile = new File(imagePath);
			if (!imageFile.exists()) {
				JOptionPane.showMessageDialog(this, "Image file not found: " + imagePath, "File Not Found",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Load and rotate image
			java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(imageFile);
			java.awt.image.BufferedImage rotated = rotateImage(original, degrees);

			// Save back to file
			String format = imagePath.substring(imagePath.lastIndexOf('.') + 1);
			javax.imageio.ImageIO.write(rotated, format, imageFile);

			parent.log("Rotated item image by " + degrees + " degrees");

			// Update all lists and views immediately
			updateAllItemLists();

			// Save item to persist changes
			try {
				ItemSaver.saveItemByName(selectedItem);
				parent.autoSaveCurrentScene();
			} catch (Exception saveEx) {
				parent.log("ERROR saving after rotation: " + saveEx.getMessage());
			}

			JOptionPane.showMessageDialog(this, "Image rotated successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			parent.log("ERROR rotating image: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error rotating image: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Flip the current item's image
	 */
	private void flipItemImage(boolean horizontal) {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String imagePath = imagePathField.getText();
		if (imagePath == null || imagePath.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "No image path set for this item!", "No Image",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			File imageFile = new File(imagePath);
			if (!imageFile.exists()) {
				JOptionPane.showMessageDialog(this, "Image file not found: " + imagePath, "File Not Found",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Load and flip image
			java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(imageFile);
			java.awt.image.BufferedImage flipped = flipImage(original, horizontal);

			// Save back to file
			String format = imagePath.substring(imagePath.lastIndexOf('.') + 1);
			javax.imageio.ImageIO.write(flipped, format, imageFile);

			parent.log("Flipped item image " + (horizontal ? "horizontally" : "vertically"));

			// Update all lists and views immediately
			updateAllItemLists();

			// Save item to persist changes
			try {
				ItemSaver.saveItemByName(selectedItem);
				parent.autoSaveCurrentScene();
			} catch (Exception saveEx) {
				parent.log("ERROR saving after flip: " + saveEx.getMessage());
			}

			JOptionPane.showMessageDialog(this, "Image flipped successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			parent.log("ERROR flipping image: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error flipping image: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Rotate a BufferedImage by specified degrees
	 */
	private java.awt.image.BufferedImage rotateImage(java.awt.image.BufferedImage img, int degrees) {
		double radians = Math.toRadians(degrees);
		double sin = Math.abs(Math.sin(radians));
		double cos = Math.abs(Math.cos(radians));

		int newWidth = (int) Math.floor(img.getWidth() * cos + img.getHeight() * sin);
		int newHeight = (int) Math.floor(img.getHeight() * cos + img.getWidth() * sin);

		java.awt.image.BufferedImage rotated = new java.awt.image.BufferedImage(newWidth, newHeight,
				java.awt.image.BufferedImage.TYPE_INT_ARGB);

		java.awt.Graphics2D g2d = rotated.createGraphics();
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
				java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.translate((newWidth - img.getWidth()) / 2, (newHeight - img.getHeight()) / 2);
		g2d.rotate(radians, img.getWidth() / 2.0, img.getHeight() / 2.0);
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();

		return rotated;
	}

	/**
	 * Flip a BufferedImage horizontally or vertically
	 */
	private java.awt.image.BufferedImage flipImage(java.awt.image.BufferedImage img, boolean horizontal) {
		int width = img.getWidth();
		int height = img.getHeight();

		java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(width, height,
				java.awt.image.BufferedImage.TYPE_INT_ARGB);

		java.awt.Graphics2D g2d = flipped.createGraphics();

		if (horizontal) {
			// Flip horizontally
			g2d.drawImage(img, width, 0, -width, height, null);
		} else {
			// Flip vertically
			g2d.drawImage(img, 0, height, width, -height, null);
		}

		g2d.dispose();
		return flipped;
	}

	private void renameItem() {
		if (selectedItem == null) {
			return;
		}

		String oldName = selectedItem.getName();
		String newName = JOptionPane.showInputDialog(this, "Enter new name for item:", oldName);

		if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
			newName = newName.trim();

			// Check if new name already exists
			File newFile = new File("resources/items/" + newName + ".txt");
			if (newFile.exists()) {
				parent.log("ERROR: Item '" + newName + "' already exists!");
				return;
			}

			// Rename the file
			File oldFile = new File("resources/items/" + oldName + ".txt");
			if (oldFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					// Update the item object
					selectedItem.setName(newName);

					// Save the item with new name
					try {
						ItemSaver.saveItemByName(selectedItem);
					} catch (Exception e) {
						parent.log("ERROR renaming item: " + e.getMessage());
					}

					// Update the list
					loadAllItems();
					selectItemInList(selectedItem);

					// Update item in scene if it exists
					Scene currentScene = parent.getGame().getCurrentScene();
					if (currentScene != null) {
						for (Item sceneItem : currentScene.getItems()) {
							if (sceneItem.getName().equals(oldName)) {
								sceneItem.setName(newName);
								parent.autoSaveCurrentScene();
								break;
							}
						}
					}

					parent.log("Renamed item from '" + oldName + "' to '" + newName + "'");
				} else {
					parent.log("ERROR: Failed to rename file!");
				}
			}
		}
	}

	private void openActionsDialog() {
		if (selectedItem == null) {
			return;
		}

		parent.log("Opening Actions Dialog for item: " + selectedItem.getName());
		ItemActionsEditorDialog dialog = new ItemActionsEditorDialog(parent, selectedItem);
		dialog.setVisible(true);
	}

	private void openMouseHoverDialog() {
		if (selectedItem == null) {
			return;
		}

		parent.log("Opening Mouse Hover Dialog for item: " + selectedItem.getName());
		HoverEditorDialog dialog = new HoverEditorDialog(parent, selectedItem);
		dialog.setVisible(true);
	}

	/**
	 * Custom cell renderer for Items with image tiles
	 */
	private class ItemTileRenderer extends JLabel implements ListCellRenderer<String> {
		private Map<String, ImageIcon> imageCache = new HashMap<>();
		private static final int TILE_SIZE = 32;

		public ItemTileRenderer() {
			setOpaque(true);
		}

		public void clearCache() {
			imageCache.clear();
			parent.log("Image cache cleared");
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {

			setText(value);
			setFont(new Font("Arial", Font.PLAIN, 11));

			// Try to find item by name in the current scene
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene != null) {
				for (Item item : currentScene.getItems()) {
					if (item.getName().equals(value)) {
						String imagePath = item.getImageFilePath();
						if (imagePath != null && !imagePath.isEmpty()) {
							ImageIcon icon = loadThumbnail(imagePath);
							if (icon != null) {
								setIcon(icon);
							} else {
								setIcon(null);
							}
						} else {
							setIcon(null);
						}
						break;
					}
				}
			}

			// Selection colors
			if (isSelected) {
				setBackground(new Color(100, 150, 255));
				setForeground(Color.WHITE);
			} else {
				setBackground(Color.WHITE);
				setForeground(Color.BLACK);
			}

			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			return this;
		}

		private ImageIcon loadThumbnail(String imagePath) {
			// Check cache first
			if (imageCache.containsKey(imagePath)) {
				return imageCache.get(imagePath);
			}

			try {
				File imageFile = new File(imagePath);
				if (imageFile.exists()) {
					ImageIcon originalIcon = new ImageIcon(imagePath);
					Image img = originalIcon.getImage();

					// Scale to tile size
					Image scaledImg = img.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
					ImageIcon thumbnail = new ImageIcon(scaledImg);

					// Cache it
					imageCache.put(imagePath, thumbnail);
					return thumbnail;
				}
			} catch (Exception e) {
				// Image load failed, return null
			}

			return null;
		}
	}

	/**
	 * Button renderer for Delete column
	 */
	private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
		public ButtonRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText("🗑️");
			setToolTipText("Delete this condition");
			return this;
		}
	}

	/**
	 * Button editor for Delete column
	 */
	private class ButtonEditor extends javax.swing.DefaultCellEditor {
		private JButton button;
		private String label;
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
			label = "🗑️";
			button.setText(label);
			button.setToolTipText("Delete this condition");
			isPushed = true;
			currentRow = row;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				// Delete the row
				conditionsTableModel.removeRow(currentRow);
				parent.log("Deleted condition from row " + currentRow);
			}
			isPushed = false;
			return label;
		}

		@Override
		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}
}
