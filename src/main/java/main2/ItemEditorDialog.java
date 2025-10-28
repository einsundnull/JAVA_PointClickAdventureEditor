package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
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
		super(parent, "Item Editor", false); // Non-modal
		this.parent = parent;

		setSize(900, 700);
		setLocationRelativeTo(parent);

		initUI();
		loadAllItems();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("🎒 Item Editor");
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

		// Image File
		JPanel imageFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imageFilePanel.add(new JLabel("Image File:"));
		imageFileField = new JTextField(20);
		imageFilePanel.add(imageFileField);
		JButton browseImageBtn = new JButton("📂");
		browseImageBtn.addActionListener(e -> browseImageFile());
		imageFilePanel.add(browseImageBtn);
		rightPanel.add(imageFilePanel);

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
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column != 0 || true; // All editable
			}
		};

		conditionsTable = new JTable(conditionsTableModel);
		conditionsTable.setRowHeight(25);

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

	private void onItemSelected() {
		int selectedIndex = itemList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedItem = null;
			clearFields();
			return;
		}

		String itemName = itemListModel.getElementAt(selectedIndex);

		try {
			selectedItem = ItemLoader.loadItemByName(itemName);
			loadItemToFields();
			parent.log("Selected item: " + itemName);
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
			conditionsTableModel.addRow(new Object[] { entry.getKey(), entry.getValue(), "[X]" });
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
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String itemName = selectedItem.getName();
		int confirm = JOptionPane.showConfirmDialog(this,
				"Delete item '" + itemName + "'?\n\nThis will delete the .txt file!", "Confirm Delete",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			File itemFile = new File("resources/items/" + itemName + ".txt");
			if (itemFile.delete()) {
				itemListModel.removeElement(itemName);
				selectedItem = null;
				clearFields();
				parent.log("Deleted item: " + itemName);
			} else {
				JOptionPane.showMessageDialog(this, "Could not delete item file!", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
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

			JOptionPane.showMessageDialog(this, "Item saved successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			parent.log("ERROR saving item: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error saving item: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addCondition() {
		// Get all available conditions
		try {
			java.lang.reflect.Field[] fields = Conditions.class.getDeclaredFields();
			String[] conditionNames = new String[fields.length];
			int count = 0;

			for (java.lang.reflect.Field field : fields) {
				if (field.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					conditionNames[count++] = field.getName();
				}
			}

			String[] validConditions = new String[count];
			System.arraycopy(conditionNames, 0, validConditions, 0, count);

			String selected = (String) JOptionPane.showInputDialog(this, "Select condition:", "Add Condition",
					JOptionPane.PLAIN_MESSAGE, null, validConditions, validConditions.length > 0 ? validConditions[0] : null);

			if (selected != null) {
				conditionsTableModel.addRow(new Object[] { selected, false, "[X]" });
			}
		} catch (Exception e) {
			parent.log("ERROR: " + e.getMessage());
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
}
