package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import main.ui.components.button.AppButton;
import main.ui.components.panel.CollapsiblePanel;
import main.ui.theme.Spacing;
// New UI imports
import main.ui.theme.ThemeManager;

/**
 * Dialog zum Verwalten aller Items
 */
public class ItemEditorDialog extends JDialog {
	private EditorMain parent;
	private JList<String> itemList;
	private DefaultListModel<String> itemListModel;
	private Item selectedItem = null;

	// Edit fields
	private JTextField nameField;
	private JCheckBox followingMouseCheckBox;
	private JCheckBox followingOnClickCheckBox;

	// Conditional Images
	private JPanel conditionalImagesContainer;
	private java.util.List<ConditionalImagePanel> conditionalImagePanels;

	// Custom Click Areas
	private JPanel customClickAreasContainer;
	private java.util.List<CustomClickAreaPanel> customClickAreaPanels;

	// Moving Ranges
	private JPanel movingRangesContainer;
	private java.util.List<MovingRangePanel> movingRangePanels;

	// Split panes for resizable sections
	private javax.swing.JSplitPane splitPane1; // Images vs Rest
	private javax.swing.JSplitPane splitPane2; // CustomClickAreas vs MovingRanges

	// Section panels for collapse functionality
	private JPanel imagesSection;
	private JPanel customClickAreaSection;
	private JPanel movingRangeSection;

	public ItemEditorDialog(EditorMain parent) {
		this(parent, null);
	}

	public ItemEditorDialog(EditorMain parent, Item preSelectedItem) {
		super(parent, "Item Editor - Manage All Items", false); // Non-modal
		this.parent = parent;
		this.conditionalImagePanels = new java.util.ArrayList<>();
		this.customClickAreaPanels = new java.util.ArrayList<>();
		this.movingRangePanels = new java.util.ArrayList<>();

		setSize(1100, 850); // Increased size for better accessibility
		setLocationRelativeTo(parent);

		initUI();
		loadAllItems();

		// Pre-select item if provided
		if (preSelectedItem != null) {
			selectItemInList(preSelectedItem);
		}

		// Add F3 key listener for refresh
		addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F3) {
					refreshItemList();
				}
			}
		});
		setFocusable(true);

		// Add window focus listener to auto-refresh when window gains focus
		addWindowFocusListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowGainedFocus(java.awt.event.WindowEvent e) {
				// Auto-refresh item list when window gains focus
				// This ensures that items created outside this dialog are visible
				refreshItemList();
			}
		});

		// Initialize JSplitPane dividers after window is shown
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent e) {
				javax.swing.SwingUtilities.invokeLater(() -> {
					// Set initial divider locations (proportional to window size)
					splitPane1.setDividerLocation(0.33); // Images get 1/3
					splitPane2.setDividerLocation(0.5);  // CustomClickAreas and MovingRanges split 50/50
				});
			}
		});
	}

	public ItemEditorDialog(SceneEditorDialogHybrid sceneEditorDialogHybrid, Item item, Scene scene) {
		// TODO Auto-generated constructor stub
	}

	private void initUI() {
		var c = ThemeManager.colors();
		var t = ThemeManager.typography();

		setLayout(new BorderLayout(Spacing.SM, Spacing.SM));

		// Title
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(c.getBackgroundPanel());
		titlePanel.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.XS, Spacing.SM));

		JLabel titleLabel = new JLabel("Item Editor");
		titleLabel.setFont(t.semiboldLg());
		titleLabel.setForeground(c.getTextPrimary());
		titlePanel.add(titleLabel, BorderLayout.WEST);

		// Theme toggle button
		AppButton themeToggleBtn = new AppButton("🌓", AppButton.Variant.GHOST, AppButton.Size.SMALL);
		themeToggleBtn.setToolTipText("Toggle Light/Dark Theme");
		themeToggleBtn.addActionListener(e -> {
			ThemeManager.toggleTheme();
			ThemeManager.updateAllWindows();
		});
		titlePanel.add(themeToggleBtn, BorderLayout.EAST);

		add(titlePanel, BorderLayout.NORTH);

		// Main panel with split view
		JPanel mainPanel = new JPanel(new BorderLayout(Spacing.SM, Spacing.SM));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, Spacing.SM, Spacing.SM, Spacing.SM));
		mainPanel.setBackground(c.getBackgroundRoot());

		// Left side - Item list with collapse functionality
		JPanel leftPanelWrapper = new JPanel(new BorderLayout());

		// Collapse button for left panel
		JButton leftCollapseBtn = new JButton("◀");
		leftCollapseBtn.setFont(t.sm());
		leftCollapseBtn.setToolTipText("Collapse/Expand Items List");
		leftCollapseBtn.setPreferredSize(new Dimension(Spacing.LG, 0));

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setPreferredSize(new Dimension(250, 0));
		leftPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(c.getBorderDefault(), 1),
			"All Items"
		));
		leftPanel.setBackground(c.getBackgroundPanel());

		itemListModel = new DefaultListModel<>();
		itemList = new JList<>(itemListModel);
		itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		itemList.setCellRenderer(new ItemTileRenderer());
		itemList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onItemSelected();
			}
		});
		itemList.setBackground(c.getBackgroundPanel());
		itemList.setForeground(c.getTextPrimary());

		JScrollPane listScroll = new JScrollPane(itemList);
		leftPanel.add(listScroll, BorderLayout.CENTER);

		// List buttons - Use AppButton
		JPanel listButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.XS));

		AppButton newItemBtn = new AppButton("➕ New", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		newItemBtn.addActionListener(e -> createNewItem());
		listButtonPanel.add(newItemBtn);

		AppButton deleteItemBtn = new AppButton("🗑️ Delete", AppButton.Variant.DANGER, AppButton.Size.SMALL);
		deleteItemBtn.addActionListener(e -> deleteItem());
		listButtonPanel.add(deleteItemBtn);

		AppButton addToSceneBtn = new AppButton("➕ Add to Scene", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		addToSceneBtn.addActionListener(e -> addItemToScene());
		listButtonPanel.add(addToSceneBtn);

		leftPanel.add(listButtonPanel, BorderLayout.SOUTH);

		// Add left panel and collapse button to wrapper
		leftPanelWrapper.add(leftPanel, BorderLayout.CENTER);
		leftPanelWrapper.add(leftCollapseBtn, BorderLayout.EAST);

		mainPanel.add(leftPanelWrapper, BorderLayout.WEST);

		// Collapse functionality for left panel
		leftCollapseBtn.addActionListener(e -> {
			if (leftPanel.isVisible()) {
				leftPanel.setVisible(false);
				leftCollapseBtn.setText("▶");
				leftCollapseBtn.setToolTipText("Expand Items List");
			} else {
				leftPanel.setVisible(true);
				leftCollapseBtn.setText("◀");
				leftCollapseBtn.setToolTipText("Collapse Items List");
			}
			mainPanel.revalidate();
			mainPanel.repaint();
		});

		// Right side - Item editor with sections
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(c.getBorderDefault(), 1),
			"Item Properties"
		));
		rightPanel.setBackground(c.getBackgroundPanel());

		// Top panel with Name and Mouse Following options
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(c.getBackgroundPanel());

		// Name panel
		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		namePanel.setBackground(c.getBackgroundPanel());
		JLabel nameLabel = new JLabel("Name:");
		nameLabel.setFont(t.sm());
		nameLabel.setForeground(c.getTextSecondary());
		namePanel.add(nameLabel);
		nameField = new JTextField(20);
		nameField.setEnabled(false);
		namePanel.add(nameField);
		topPanel.add(namePanel);

		// Mouse Following Panel
		JPanel mouseFollowingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		mouseFollowingPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(c.getBorderDefault(), 1),
			"Mouse Following"
		));
		mouseFollowingPanel.setBackground(c.getBackgroundPanel());

		followingMouseCheckBox = new JCheckBox("Following Mouse");
		followingMouseCheckBox.setFont(t.sm());
		followingMouseCheckBox.setForeground(c.getTextPrimary());
		followingMouseCheckBox.setBackground(c.getBackgroundPanel());
		followingMouseCheckBox.setToolTipText("Item follows mouse cursor continuously");
		mouseFollowingPanel.add(followingMouseCheckBox);

		followingOnClickCheckBox = new JCheckBox("Following On Click");
		followingOnClickCheckBox.setFont(t.sm());
		followingOnClickCheckBox.setForeground(c.getTextPrimary());
		followingOnClickCheckBox.setBackground(c.getBackgroundPanel());
		followingOnClickCheckBox.setToolTipText("Item follows mouse only when clicked");
		mouseFollowingPanel.add(followingOnClickCheckBox);

		topPanel.add(mouseFollowingPanel);

		rightPanel.add(topPanel, BorderLayout.NORTH);

		// === Create the three sections ===
		imagesSection = createImagesSection();
		customClickAreaSection = createCustomClickAreaSection();
		movingRangeSection = createMovingRangeSection();

		// === Create nested JSplitPanes for resizable sections ===
		// SplitPane 2: CustomClickAreas (top) vs MovingRanges (bottom)
		splitPane2 = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT,
			customClickAreaSection, movingRangeSection);
		splitPane2.setResizeWeight(0.5); // Equal distribution
		splitPane2.setDividerSize(Spacing.RADIUS_LG);
		splitPane2.setOneTouchExpandable(true);

		// SplitPane 1: Images (top) vs Rest (bottom)
		splitPane1 = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT,
			imagesSection, splitPane2);
		splitPane1.setResizeWeight(0.33); // Images get 1/3, rest gets 2/3
		splitPane1.setDividerSize(Spacing.RADIUS_LG);
		splitPane1.setOneTouchExpandable(true);

		rightPanel.add(splitPane1, BorderLayout.CENTER);
		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);

		// Bottom buttons - nach Schema - Use AppButton
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Spacing.SM, Spacing.SM));
		bottomPanel.setBackground(c.getBackgroundElevated());
		bottomPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, c.getBorderDefault()),
			BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
		));

		AppButton openActionEditorBtn = new AppButton("⚡ Open Action Editor", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		openActionEditorBtn.addActionListener(e -> openActionsDialog());
		bottomPanel.add(openActionEditorBtn);

		AppButton manageConditionsBtn = new AppButton("📋 Manage Conditions", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		manageConditionsBtn.addActionListener(e -> openConditionManager());
		bottomPanel.add(manageConditionsBtn);

		AppButton openResourceBtn = new AppButton("📁 Open Resource", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
		openResourceBtn.addActionListener(e -> openItemsFolder());
		bottomPanel.add(openResourceBtn);

		AppButton deleteBtn = new AppButton("🗑️ Delete", AppButton.Variant.DANGER, AppButton.Size.SMALL);
		deleteBtn.setToolTipText("Delete the selected item");
		deleteBtn.addActionListener(e -> deleteItem());
		bottomPanel.add(deleteBtn);

		AppButton saveBtn = new AppButton("💾 Save", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		saveBtn.addActionListener(e -> saveItem());
		bottomPanel.add(saveBtn);

		AppButton cancelBtn = new AppButton("❌ Cancel", AppButton.Variant.GHOST, AppButton.Size.SMALL);
		cancelBtn.addActionListener(e -> dispose());
		bottomPanel.add(cancelBtn);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	/**
	 * Creates the Images Section with list of image panels (Schema: ListViewImages)
	 * UPDATED: Uses modern CollapsiblePanel
	 */
	private JPanel createImagesSection() {
		var c = ThemeManager.colors();
		var t = ThemeManager.typography();

		CollapsiblePanel section = new CollapsiblePanel("Images", false);

		// Images container with scroll
		conditionalImagesContainer = new JPanel();
		conditionalImagesContainer.setLayout(new BoxLayout(conditionalImagesContainer, BoxLayout.Y_AXIS));
		conditionalImagesContainer.setBackground(c.getBackgroundPanel());

		JScrollPane scrollPane = new JScrollPane(conditionalImagesContainer);
		scrollPane.setPreferredSize(new Dimension(600, 250));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBorder(null);

		// Content panel (scrollPane + control buttons)
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// Control button
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.XS));
		controlPanel.setBackground(c.getBackgroundPanel());

		AppButton addImageBtn = new AppButton("Add Image Field", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		addImageBtn.addActionListener(e -> addNewConditionalImageField());
		controlPanel.add(addImageBtn);
		contentPanel.add(controlPanel, BorderLayout.SOUTH);

		section.setContent(contentPanel);

		return section;
	}

	/**
	 * Creates the Custom Click Area Section (Schema: ListViewCustomClickArea)
	 */
	private JPanel createCustomClickAreaSection() {
		var c = ThemeManager.colors();

		CollapsiblePanel section = new CollapsiblePanel("Custom Click Areas", false);

		// Custom Click Areas container with scroll
		customClickAreasContainer = new JPanel();
		customClickAreasContainer.setLayout(new BoxLayout(customClickAreasContainer, BoxLayout.Y_AXIS));
		customClickAreasContainer.setBackground(c.getBackgroundPanel());

		JScrollPane scrollPane = new JScrollPane(customClickAreasContainer);
		scrollPane.setPreferredSize(new Dimension(600, 250));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBorder(null);

		// Content panel (scrollPane + control buttons)
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// Control button
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.XS));
		controlPanel.setBackground(c.getBackgroundPanel());

		AppButton addBtn = new AppButton("Add Custom Click Area Field", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		addBtn.addActionListener(e -> addNewCustomClickAreaField());
		controlPanel.add(addBtn);
		contentPanel.add(controlPanel, BorderLayout.SOUTH);

		section.setContent(contentPanel);

		return section;
	}

	/**
	 * Creates the Moving Range Section (Schema: ListViewMovingRange)
	 */
	private JPanel createMovingRangeSection() {
		var c = ThemeManager.colors();

		CollapsiblePanel section = new CollapsiblePanel("Moving Ranges", false);

		// Moving Ranges container with scroll
		movingRangesContainer = new JPanel();
		movingRangesContainer.setLayout(new BoxLayout(movingRangesContainer, BoxLayout.Y_AXIS));
		movingRangesContainer.setBackground(c.getBackgroundPanel());

		JScrollPane scrollPane = new JScrollPane(movingRangesContainer);
		scrollPane.setPreferredSize(new Dimension(600, 250));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBorder(null);

		// Content panel (scrollPane + control buttons)
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// Control button
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.XS));
		controlPanel.setBackground(c.getBackgroundPanel());

		AppButton addBtn = new AppButton("Add Moving Range Field", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		addBtn.addActionListener(e -> addNewMovingRangeField());
		controlPanel.add(addBtn);
		contentPanel.add(controlPanel, BorderLayout.SOUTH);

		section.setContent(contentPanel);

		return section;
	}

	/**
	 * Opens the Condition Manager Dialog
	 */
	private void openConditionManager() {
		ConditionsManagerDialog dialog = new ConditionsManagerDialog(
				(javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this));
		dialog.setVisible(true);

		// Refresh all image panels if conditions changed
		if (dialog.changesMade()) {
			for (ConditionalImagePanel panel : conditionalImagePanels) {
				panel.refreshConditions();
			}
		}
	}

	private void loadAllItems() {
		itemListModel.clear();

		File itemsDir = ResourcePathHelper.resolve("items");
		if (!itemsDir.exists()) {
			itemsDir.mkdirs();
			parent.log("Created items directory");
			return;
		}

		// Filter: Load only .txt files that don't end with "_progress.txt" or "_default.txt"
		// <name>.txt = DEFAULT (shown in editor)
		// <name>_progress.txt = Game progress (hidden from editor)
		File[] itemFiles = itemsDir.listFiles((dir, name) ->
			name.endsWith(".txt") &&
			!name.endsWith("_progress.txt") &&
			!name.endsWith("_default.txt"));

		if (itemFiles != null) {
			for (File file : itemFiles) {
				String itemName = file.getName().replace(".txt", "");
				itemListModel.addElement(itemName);
			}
		}

		parent.log("Loaded " + itemListModel.size() + " items (DEFAULT files only)");
	}

	/**
	 * Refresh the item list - reloads all items from disk
	 * Can be called with F3 or programmatically when items are created/modified outside this dialog
	 */
	public void refreshItemList() {
		// Save current selection
		String currentSelection = itemList.getSelectedValue();

		// Reload all items from disk
		loadAllItems();

		// Restore selection if item still exists
		if (currentSelection != null) {
			for (int i = 0; i < itemListModel.size(); i++) {
				if (itemListModel.getElementAt(i).equals(currentSelection)) {
					itemList.setSelectedIndex(i);
					itemList.ensureIndexIsVisible(i);
					break;
				}
			}
		}

		parent.log("✓ Item list refreshed (F3) - " + itemListModel.size() + " items loaded");
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

		// Load mouse following checkboxes
		followingMouseCheckBox.setSelected(selectedItem.isFollowingMouse());
		followingOnClickCheckBox.setSelected(selectedItem.isFollowingOnMouseClick());

		// Load conditional images
		loadConditionalImagesForItem(selectedItem);

		// Load custom click areas
		loadCustomClickAreasForItem(selectedItem);

		// Load moving ranges
		loadMovingRangesForItem(selectedItem);
	}

	private void clearFields() {
		nameField.setText("");
		followingMouseCheckBox.setSelected(false);
		followingOnClickCheckBox.setSelected(false);
	}

	private void createNewItem() {
		String itemName = JOptionPane.showInputDialog(this, "Enter item name (e.g., 'GlassAtBeach'):",
				"Create New Item", JOptionPane.PLAIN_MESSAGE);

		if (itemName != null && !itemName.trim().isEmpty()) {
			itemName = itemName.trim();

			// Check if already exists
			File itemFile = ResourcePathHelper.resolve("items/" + itemName + ".txt");
			if (itemFile.exists()) {
				JOptionPane.showMessageDialog(this, "Item '" + itemName + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Create new item
			Item newItem = new Item(itemName);
			newItem.setImageFileName("default.png");
			newItem.setImageFilePath(ResourcePathHelper.resolvePath("images/items/default.png"));

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
		File itemFile = ResourcePathHelper.resolve("items/" + itemName + ".txt");
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
			// Update mouse following properties
			selectedItem.setFollowingMouse(followingMouseCheckBox.isSelected());
			selectedItem.setFollowingOnMouseClick(followingOnClickCheckBox.isSelected());

			// Update conditional images
			java.util.List<ConditionalImage> conditionalImages = new java.util.ArrayList<>();
			for (ConditionalImagePanel panel : conditionalImagePanels) {
				ConditionalImage img = panel.getConditionalImage();
				if (!img.getImagePath().trim().isEmpty()) {
					conditionalImages.add(img);
				}
			}
			selectedItem.setConditionalImages(conditionalImages);

			// Update custom click areas
			java.util.List<CustomClickArea> customClickAreas = new java.util.ArrayList<>();
			for (CustomClickAreaPanel panel : customClickAreaPanels) {
				CustomClickArea area = panel.getData();
				if (area != null && !area.getPoints().isEmpty()) {
					customClickAreas.add(area);
				}
			}
			selectedItem.setCustomClickAreas(customClickAreas);

			// Update moving ranges
			java.util.List<MovingRange> movingRanges = new java.util.ArrayList<>();
			for (MovingRangePanel panel : movingRangePanels) {
				MovingRange range = panel.getData();
				if (range != null && !range.getKeyAreaName().trim().isEmpty()) {
					movingRanges.add(range);
				}
			}
			selectedItem.setMovingRanges(movingRanges);

			// Save to file (DEFAULT)
			ItemSaver.saveItemToDefault(selectedItem);
			parent.log("Saved item: " + selectedItem.getName() + " to DEFAULT");

			// IMPORTANT: Reload the item from file to sync RAM with disk
			try {
				String itemName = selectedItem.getName();
				Item reloadedItem = ItemLoader.loadItemFromDefault(itemName);

				// Update the item reference in the scene
				Scene currentScene = parent.getGame().getCurrentScene();
				List<Item> items = currentScene.getItems();
				for (int i = 0; i < items.size(); i++) {
					if (items.get(i).getName().equals(itemName)) {
						items.set(i, reloadedItem);
						selectedItem = reloadedItem; // Update our reference too
						parent.log("Item reloaded from file");
						break;
					}
				}
			} catch (Exception reloadEx) {
				parent.log("Warning: Could not reload item: " + reloadEx.getMessage());
			}

			// Update all ListViews immediately
			updateAllItemLists();

			// Auto-save scene to persist changes
			parent.autoSaveCurrentScene();

//			JOptionPane.showMessageDialog(this, "Item saved successfully!\n\nItem reloaded from file.", "Success",
//					JOptionPane.INFORMATION_MESSAGE);
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

	private void openItemsFolder() {
		File itemsFolder = ResourcePathHelper.resolve("items");
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
	 * UPDATED: Uses theme colors
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
			setFont(ThemeManager.typography().sm());

			// Try to find item by name in the current scene
			Scene currentScene = parent.getGame().getCurrentScene();
			if (currentScene != null) {
				for (Item item : currentScene.getItems()) {
					if (item.getName().equals(value)) {
						String imagePath = item.getCurrentImagePath();
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

			// Selection colors - use theme colors
			var c = ThemeManager.colors();
			if (isSelected) {
				setBackground(c.getBackgroundSelected());
				setForeground(c.getPrimary());
			} else {
				setBackground(c.getBackgroundPanel());
				setForeground(c.getTextPrimary());
			}

			setBorder(BorderFactory.createEmptyBorder(Spacing.XS, Spacing.SM, Spacing.XS, Spacing.SM));
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

	private void addNewConditionalImageField() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		ConditionalImage newImage = new ConditionalImage("", "Image " + (conditionalImagePanels.size() + 1));
		addConditionalImagePanel(newImage);
		revalidateConditionalImagesContainer();
	}

	private void addConditionalImagePanel(ConditionalImage image) {
		ConditionalImagePanel panel = new ConditionalImagePanel(image, this);
		conditionalImagePanels.add(panel);
		conditionalImagesContainer.add(panel);
		conditionalImagesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
	}

	private void removeConditionalImagePanel(ConditionalImagePanel panel) {
		conditionalImagePanels.remove(panel);

		// Clear all components and rebuild to avoid orphaned spacers
		conditionalImagesContainer.removeAll();
		for (ConditionalImagePanel p : conditionalImagePanels) {
			conditionalImagesContainer.add(p);
			conditionalImagesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		revalidateConditionalImagesContainer();
	}

	private void revalidateConditionalImagesContainer() {
		conditionalImagesContainer.revalidate();
		conditionalImagesContainer.repaint();
	}

	private void loadConditionalImagesForItem(Item item) {
		// Clear existing panels
		conditionalImagePanels.clear();
		conditionalImagesContainer.removeAll();

		// Load conditional images from item
		java.util.List<ConditionalImage> images = item.getConditionalImages();
		if (images.isEmpty()) {
			// Add one empty panel if no images exist
			ConditionalImage defaultImg = new ConditionalImage("", "Default");
			addConditionalImagePanel(defaultImg);
		} else {
			for (ConditionalImage img : images) {
				addConditionalImagePanel(img);
			}
		}

		revalidateConditionalImagesContainer();
	}

	// === Custom Click Area Methods ===

	private void addNewCustomClickAreaField() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		CustomClickAreaPanel panel = new CustomClickAreaPanel(this);
		customClickAreaPanels.add(panel);
		customClickAreasContainer.add(panel);
		customClickAreasContainer.add(Box.createRigidArea(new Dimension(0, 10)));
		revalidateCustomClickAreasContainer();
	}

	private void removeCustomClickAreaPanel(CustomClickAreaPanel panel) {
		customClickAreaPanels.remove(panel);

		// Clear all components and rebuild to avoid orphaned spacers
		customClickAreasContainer.removeAll();
		for (CustomClickAreaPanel p : customClickAreaPanels) {
			customClickAreasContainer.add(p);
			customClickAreasContainer.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		revalidateCustomClickAreasContainer();
	}

	private void revalidateCustomClickAreasContainer() {
		customClickAreasContainer.revalidate();
		customClickAreasContainer.repaint();
	}

	// === Moving Range Methods ===

	private void addNewMovingRangeField() {
		if (selectedItem == null) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Item Selected",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		MovingRangePanel panel = new MovingRangePanel(this);
		movingRangePanels.add(panel);
		movingRangesContainer.add(panel);
		movingRangesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
		revalidateMovingRangesContainer();
	}

	private void removeMovingRangePanel(MovingRangePanel panel) {
		movingRangePanels.remove(panel);

		// Clear all components and rebuild to avoid orphaned spacers
		movingRangesContainer.removeAll();
		for (MovingRangePanel p : movingRangePanels) {
			movingRangesContainer.add(p);
			movingRangesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		revalidateMovingRangesContainer();
	}

	private void revalidateMovingRangesContainer() {
		movingRangesContainer.revalidate();
		movingRangesContainer.repaint();
	}

	private void loadCustomClickAreasForItem(Item item) {
		// Clear existing panels
		customClickAreaPanels.clear();
		customClickAreasContainer.removeAll();

		// Load custom click areas from item
		java.util.List<CustomClickArea> areas = item.getCustomClickAreas();
		if (!areas.isEmpty()) {
			for (CustomClickArea area : areas) {
				CustomClickAreaPanel panel = new CustomClickAreaPanel(this, area);
				customClickAreaPanels.add(panel);
				customClickAreasContainer.add(panel);
				customClickAreasContainer.add(Box.createRigidArea(new Dimension(0, 10)));
			}
		}

		revalidateCustomClickAreasContainer();
	}

	private void loadMovingRangesForItem(Item item) {
		// Clear existing panels
		movingRangePanels.clear();
		movingRangesContainer.removeAll();

		// Load moving ranges from item
		java.util.List<MovingRange> ranges = item.getMovingRanges();
		if (!ranges.isEmpty()) {
			for (MovingRange range : ranges) {
				MovingRangePanel panel = new MovingRangePanel(this, range);
				movingRangePanels.add(panel);
				movingRangesContainer.add(panel);
				movingRangesContainer.add(Box.createRigidArea(new Dimension(0, 10)));
			}
		}

		revalidateMovingRangesContainer();
	}

	/**
	 * Panel for a single conditional image with all its settings
	 * Similar to SceneEditorDialog but adapted for Items
	 */
	private class ConditionalImagePanel extends JPanel {
		private ConditionalImage image;
		private ItemEditorDialog parentDialog;
		private JLabel imagePreviewLabel;
		private JTextField imagePathField;
		private JTextField nameField;
		private JPanel conditionsListPanel;
		private Map<String, JCheckBox> conditionCheckboxes;

		public ConditionalImagePanel(ConditionalImage image, ItemEditorDialog parentDialog) {
			this.image = image;
			this.parentDialog = parentDialog;
			this.conditionCheckboxes = new java.util.LinkedHashMap<>();

			initUI();
		}

		private void initUI() {
			setLayout(new BorderLayout(10, 10));

			// Create titled border with image name
			String panelTitle = "Item Editor Image: " + image.getName();
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY, 2),
							panelTitle,
							javax.swing.border.TitledBorder.LEFT,
							javax.swing.border.TitledBorder.TOP,
							new Font("Arial", Font.BOLD, 12)
					),
					new javax.swing.border.EmptyBorder(10, 10, 10, 10)));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

			// === LEFT SIDE: Image preview and drop zone ===
			JPanel imagePanel = new JPanel(new BorderLayout());
			imagePanel.setBorder(BorderFactory.createTitledBorder("Image Preview"));

			imagePreviewLabel = new JLabel("<html><center>Drag & Drop<br>Image Here</center></html>");
			imagePreviewLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
			imagePreviewLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
			imagePreviewLabel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(100, 100, 255), 2),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			imagePreviewLabel.setBackground(new Color(240, 240, 255));
			imagePreviewLabel.setOpaque(true);
			imagePreviewLabel.setPreferredSize(new Dimension(180, 120));

			// Enable drag & drop
			imagePreviewLabel.setDropTarget(new DropTarget() {
				public synchronized void drop(DropTargetDropEvent evt) {
					handleImageDrop(evt);
				}
			});

			imagePanel.add(imagePreviewLabel, BorderLayout.CENTER);

			// === RIGHT SIDE: Conditions panel ===
			JPanel conditionsPanel = new JPanel(new BorderLayout(5, 5));
			conditionsPanel.setBorder(BorderFactory.createTitledBorder("Conditions"));

			// Conditions list
			conditionsListPanel = new JPanel();
			conditionsListPanel.setLayout(new BoxLayout(conditionsListPanel, BoxLayout.Y_AXIS));
			JScrollPane conditionsScroll = new JScrollPane(conditionsListPanel);
			conditionsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			conditionsPanel.add(conditionsScroll, BorderLayout.CENTER);

			// Condition control buttons
			JPanel conditionControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			JButton addConditionBtn = new JButton("Add");
			addConditionBtn.addActionListener(e -> addCondition());
			conditionControlPanel.add(addConditionBtn);

			JButton removeConditionBtn = new JButton("Remove");
			removeConditionBtn.addActionListener(e -> removeSelectedConditions());
			conditionControlPanel.add(removeConditionBtn);

			conditionsPanel.add(conditionControlPanel, BorderLayout.SOUTH);

			// === SPLIT PANE: Image vs Conditions ===
			javax.swing.JSplitPane imageSplitPane = new javax.swing.JSplitPane(
				javax.swing.JSplitPane.HORIZONTAL_SPLIT,
				imagePanel,
				conditionsPanel
			);
			imageSplitPane.setResizeWeight(0.4); // Image gets 40%, Conditions get 60%
			imageSplitPane.setDividerSize(6);
			imageSplitPane.setOneTouchExpandable(true);

			add(imageSplitPane, BorderLayout.CENTER);

			// Bottom: name and path
			JPanel bottomPanel = new JPanel();
			bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

			JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			namePanel.add(new JLabel("Name:"));
			nameField = new JTextField(image.getName(), 15);
			namePanel.add(nameField);
			bottomPanel.add(namePanel);

			JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			pathPanel.add(new JLabel("Image Path:"));
			imagePathField = new JTextField(image.getImagePath(), 25);
			imagePathField.setEditable(false);
			pathPanel.add(imagePathField);
			bottomPanel.add(pathPanel);

			// Size fields (Schema: Size: ↔:{<horizontally>}dp ↕:{<vertically>}dp)
			JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			sizePanel.add(new JLabel("Size:"));
			sizePanel.add(new JLabel("↔:"));
			JTextField widthField = new JTextField("100", 5);
			sizePanel.add(widthField);
			sizePanel.add(new JLabel("dp"));
			sizePanel.add(new JLabel("↕:"));
			JTextField heightField = new JTextField("100", 5);
			sizePanel.add(heightField);
			sizePanel.add(new JLabel("dp"));
			bottomPanel.add(sizePanel);

			// Image control buttons at bottom
			JPanel buttonControlPanel = new JPanel();
			buttonControlPanel.setLayout(new BoxLayout(buttonControlPanel, BoxLayout.Y_AXIS));

			// Buttons row 1: Rename and Remove
			JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
			JButton renameBtn = new JButton("Rename");
			renameBtn.addActionListener(e -> renameImage());
			buttonsPanel.add(renameBtn);

			JButton removeBtn = new JButton("Remove this Image field");
			removeBtn.addActionListener(e -> removeThisImage());
			buttonsPanel.add(removeBtn);
			buttonControlPanel.add(buttonsPanel);

			// Buttons row 2: Flip buttons
			JPanel flipButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
			JButton flipHBtn = new JButton("⇄ Flip H");
			flipHBtn.setToolTipText("Flip image horizontally");
			flipHBtn.addActionListener(e -> flipImage(true));
			flipButtonsPanel.add(flipHBtn);

			JButton flipVBtn = new JButton("⇅ Flip V");
			flipVBtn.setToolTipText("Flip image vertically");
			flipVBtn.addActionListener(e -> flipImage(false));
			flipButtonsPanel.add(flipVBtn);
			buttonControlPanel.add(flipButtonsPanel);

			bottomPanel.add(buttonControlPanel);

			add(bottomPanel, BorderLayout.SOUTH);

			// Load initial conditions
			refreshConditions();
			updateImagePreview();
		}

		private void handleImageDrop(DropTargetDropEvent evt) {
			try {
				evt.acceptDrop(DnDConstants.ACTION_COPY);
				@SuppressWarnings("unchecked")
				List<File> droppedFiles = (List<File>) evt.getTransferable()
						.getTransferData(DataFlavor.javaFileListFlavor);
				if (!droppedFiles.isEmpty()) {
					File file = droppedFiles.get(0);
					if (isImageFile(file)) {
						// Copy file to resources/images/items/
						File itemsImagesDir = ResourcePathHelper.resolve("images/items");
						if (!itemsImagesDir.exists()) {
							itemsImagesDir.mkdirs();
							parent.log("Created directory: resources/images/items/");
						}

						File targetFile = new File(itemsImagesDir, file.getName());
						if (!targetFile.exists() || !file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
							try {
								java.nio.file.Files.copy(file.toPath(), targetFile.toPath(),
										java.nio.file.StandardCopyOption.REPLACE_EXISTING);
								parent.log("✓ Copied image to: " + targetFile.getPath());
							} catch (Exception copyEx) {
								parent.log("ERROR copying image: " + copyEx.getMessage());
							}
						}

						// Store only filename, not full path
						image.setImagePath(file.getName());
						imagePathField.setText(file.getName());

						// Update preview immediately
						updateImagePreview();

						// Save item immediately for cache/RAM update
						try {
							parentDialog.saveItem();
							parent.log("✓ Item saved with new image reference");
						} catch (Exception saveEx) {
							parent.log("ERROR auto-saving item: " + saveEx.getMessage());
						}

						parent.log("✓ Image selected: " + file.getName());
					} else {
						JOptionPane.showMessageDialog(ConditionalImagePanel.this,
								"Please drop an image file (PNG, JPG, GIF)", "Invalid File",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				parent.log("ERROR handling image drop: " + ex.getMessage());
			}
		}

		private void renameImage() {
			String newName = JOptionPane.showInputDialog(this, "Enter new name for this image:", nameField.getText());

			if (newName != null && !newName.trim().isEmpty()) {
				image.setName(newName.trim());
				nameField.setText(newName.trim());

				// Update panel title
				updatePanelTitle();
			}
		}

		private void updatePanelTitle() {
			String panelTitle = "Item Editor Image: " + image.getName();
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY, 2),
							panelTitle,
							javax.swing.border.TitledBorder.LEFT,
							javax.swing.border.TitledBorder.TOP,
							new Font("Arial", Font.BOLD, 12)
					),
					new javax.swing.border.EmptyBorder(10, 10, 10, 10)));
			revalidate();
			repaint();
		}

		private void removeThisImage() {
			int confirm = JOptionPane.showConfirmDialog(this, "Remove this image field?", "Confirm Removal",
					JOptionPane.YES_NO_OPTION);

			if (confirm == JOptionPane.YES_OPTION) {
				parentDialog.removeConditionalImagePanel(this);
			}
		}

		private void flipImage(boolean horizontal) {
			String imagePath = image.getImagePath();
			if (imagePath == null || imagePath.trim().isEmpty()) {
				JOptionPane.showMessageDialog(this, "No image selected!", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			try {
				// Load image from resources/images/items/
				File imageFile = ResourcePathHelper.resolve("images/items/" + imagePath);

				// Fallback to resources/images/ for backward compatibility
				if (!imageFile.exists()) {
					imageFile = ResourcePathHelper.resolve("images/" + imagePath);
				}

				if (!imageFile.exists()) {
					JOptionPane.showMessageDialog(this, "Image file not found: " + imageFile.getPath(), "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(imageFile);
				int width = original.getWidth();
				int height = original.getHeight();

				java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(width, height,
						original.getType());

				Graphics2D g2d = flipped.createGraphics();

				if (horizontal) {
					// Flip horizontally
					g2d.drawImage(original, width, 0, -width, height, null);
				} else {
					// Flip vertically
					g2d.drawImage(original, 0, height, width, -height, null);
				}

				g2d.dispose();

				// Save back to same file
				String format = imagePath.substring(imagePath.lastIndexOf('.') + 1);
				javax.imageio.ImageIO.write(flipped, format, imageFile);

				// Update preview
				updateImagePreview();

				String direction = horizontal ? "horizontally" : "vertically";
				parentDialog.parent.log("Image flipped " + direction + ": " + imagePath);

			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error flipping image: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		private void addCondition() {
			Map<String, Boolean> allConditions = Conditions.getAllConditions();
			java.util.List<String> availableConditions = new java.util.ArrayList<>();

			for (String condName : allConditions.keySet()) {
				if (!condName.startsWith("isInInventory_") && !conditionCheckboxes.containsKey(condName)) {
					availableConditions.add(condName);
				}
			}

			if (availableConditions.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"No more conditions available. Use 'Manage Conditions' to create new ones.", "No Conditions",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			String selected = (String) JOptionPane.showInputDialog(this, "Select condition to add:", "Add Condition",
					JOptionPane.PLAIN_MESSAGE, null, availableConditions.toArray(), availableConditions.get(0));

			if (selected != null) {
				image.addCondition(selected, true);
				refreshConditions();
			}
		}

		private void removeSelectedConditions() {
			java.util.List<String> toRemove = new java.util.ArrayList<>();
			for (Map.Entry<String, JCheckBox> entry : conditionCheckboxes.entrySet()) {
				if (entry.getValue().isSelected()) {
					toRemove.add(entry.getKey());
				}
			}

			for (String condName : toRemove) {
				image.removeCondition(condName);
			}

			refreshConditions();
		}

		public void refreshConditions() {
			conditionsListPanel.removeAll();
			conditionCheckboxes.clear();

			Map<String, Boolean> imageConditions = image.getConditions();

			if (imageConditions.isEmpty()) {
				JLabel noCondLabel = new JLabel("No conditions set (always show)");
				noCondLabel.setForeground(Color.GRAY);
				conditionsListPanel.add(noCondLabel);
			} else {
				for (Map.Entry<String, Boolean> entry : imageConditions.entrySet()) {
					String condName = entry.getKey();
					boolean requiredValue = entry.getValue();

					JPanel condPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

					// Checkbox for selection (for removal)
					JCheckBox checkbox = new JCheckBox(condName);
					conditionCheckboxes.put(condName, checkbox);
					condPanel.add(checkbox);

					// Label "="
					condPanel.add(new JLabel("="));

					// Radio buttons for true/false
					javax.swing.JRadioButton trueRadio = new javax.swing.JRadioButton("true", requiredValue);
					javax.swing.JRadioButton falseRadio = new javax.swing.JRadioButton("false", !requiredValue);

					javax.swing.ButtonGroup radioGroup = new javax.swing.ButtonGroup();
					radioGroup.add(trueRadio);
					radioGroup.add(falseRadio);

					trueRadio.addActionListener(e -> {
						image.addCondition(condName, true);
					});

					falseRadio.addActionListener(e -> {
						image.addCondition(condName, false);
					});

					condPanel.add(trueRadio);
					condPanel.add(falseRadio);

					conditionsListPanel.add(condPanel);
				}
			}

			conditionsListPanel.revalidate();
			conditionsListPanel.repaint();
		}

		private void updateImagePreview() {
			try {
				String imagePath = image.getImagePath();
				if (imagePath != null && !imagePath.trim().isEmpty()) {
					// Try to load from resources/images/items/
					File imageFile = ResourcePathHelper.resolve("images/items/" + imagePath);

					// Fallback to resources/images/ for backward compatibility
					if (!imageFile.exists()) {
						imageFile = ResourcePathHelper.resolve("images/" + imagePath);
					}

					if (imageFile.exists()) {
						ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
						Image img = icon.getImage().getScaledInstance(160, 80, Image.SCALE_SMOOTH);
						imagePreviewLabel.setIcon(new ImageIcon(img));
						imagePreviewLabel.setText("");
						parent.log("✓ Loaded image preview: " + imagePath);
					} else {
						imagePreviewLabel.setIcon(null);
						imagePreviewLabel.setText("<html><center>Not found:<br>" + imagePath + "</center></html>");
						parent.log("⚠️  Image not found: " + imagePath);
					}
				} else {
					imagePreviewLabel.setIcon(null);
					imagePreviewLabel.setText("<html><center>Drag & Drop<br>Image Here</center></html>");
				}
			} catch (Exception e) {
				imagePreviewLabel.setIcon(null);
				imagePreviewLabel.setText("<html><center>Error loading<br>image</center></html>");
				parent.log("ERROR loading image preview: " + e.getMessage());
			}
		}

		public ConditionalImage getConditionalImage() {
			// Update image from UI fields
			image.setName(nameField.getText().trim());
			// Condition values are already updated via radio button listeners
			return image;
		}
	}

	/**
	 * Helper method: Save item to DEFAULT and reload from file to sync RAM
	 */
	private void saveAndReloadItem(Item item) throws Exception {
		String itemName = item.getName();

		// Save to DEFAULT file
		ItemSaver.saveItemToDefault(item);
		parent.log("Saved item: " + itemName + " to DEFAULT");

		// Reload from file
		Item reloadedItem = ItemLoader.loadItemFromDefault(itemName);

		// Update the item reference in the scene
		Scene currentScene = parent.getGame().getCurrentScene();
		List<Item> items = currentScene.getItems();
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).getName().equals(itemName)) {
				items.set(i, reloadedItem);
				if (item == selectedItem) {
					selectedItem = reloadedItem; // Update selected item reference
				}
				parent.log("Item reloaded from file");
				break;
			}
		}
	}

	/**
	 * Panel for a single Custom Click Area (Schema: ListItemCustomClickArea)
	 */
	private class CustomClickAreaPanel extends JPanel {
		private ItemEditorDialog parentDialog;
		private JTable pointsTable;
		private DefaultTableModel pointsTableModel;
		private JPanel conditionsListPanel;
		private Map<String, JCheckBox> conditionCheckboxes;
		private JTextField hoverTextField;
		private CustomClickArea customClickArea; // Store reference to the data

		public CustomClickAreaPanel(ItemEditorDialog parentDialog) {
			this(parentDialog, null);
		}

		public CustomClickAreaPanel(ItemEditorDialog parentDialog, CustomClickArea area) {
			this.parentDialog = parentDialog;
			this.customClickArea = area;
			this.conditionCheckboxes = new java.util.LinkedHashMap<>();

			initUI();

			// Load data if provided
			if (area != null) {
				loadData(area);
			}
		}

		private void initUI() {
			setLayout(new BorderLayout(10, 10));

			// Create titled border
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY, 2),
							"Custom Click Area",
							javax.swing.border.TitledBorder.LEFT,
							javax.swing.border.TitledBorder.TOP,
							new Font("Arial", Font.BOLD, 12)
					),
					new javax.swing.border.EmptyBorder(10, 10, 10, 10)));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));

			// === TOP: Points Table ===
			JPanel pointsPanel = new JPanel(new BorderLayout());
			pointsPanel.setBorder(BorderFactory.createTitledBorder("Points"));

			String[] columns = {"Point", "X", "Y", "Z"};
			pointsTableModel = new DefaultTableModel(columns, 0) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return column > 0; // Only X, Y, Z editable
				}
			};
			pointsTable = new JTable(pointsTableModel);
			pointsTable.setRowHeight(25);
			JScrollPane pointsScroll = new JScrollPane(pointsTable);
			pointsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			pointsPanel.add(pointsScroll, BorderLayout.CENTER);

			// Points control buttons
			JPanel pointsControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			JButton addPointBtn = new JButton("Add");
			addPointBtn.addActionListener(e -> addPoint());
			pointsControlPanel.add(addPointBtn);

			JButton addByClickBtn = new JButton("Add by Click");
			addByClickBtn.setToolTipText("Click on the game screen to add points");
			addByClickBtn.addActionListener(e -> enableClickToAddMode());
			pointsControlPanel.add(addByClickBtn);

			JButton removePointBtn = new JButton("Remove");
			removePointBtn.addActionListener(e -> removeSelectedPoint());
			pointsControlPanel.add(removePointBtn);

			pointsPanel.add(pointsControlPanel, BorderLayout.SOUTH);

			// === BOTTOM: Conditions Section ===
			JPanel conditionsMainPanel = new JPanel(new BorderLayout(5, 5));
			conditionsMainPanel.setBorder(BorderFactory.createTitledBorder("Conditions"));

			// Conditions list
			conditionsListPanel = new JPanel();
			conditionsListPanel.setLayout(new BoxLayout(conditionsListPanel, BoxLayout.Y_AXIS));
			JScrollPane conditionsScroll = new JScrollPane(conditionsListPanel);
			conditionsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			conditionsMainPanel.add(conditionsScroll, BorderLayout.CENTER);

			// Condition control buttons
			JPanel conditionControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			JButton addConditionBtn = new JButton("Add");
			addConditionBtn.addActionListener(e -> addCondition());
			conditionControlPanel.add(addConditionBtn);

			JButton removeConditionBtn = new JButton("Remove");
			removeConditionBtn.addActionListener(e -> removeSelectedConditions());
			conditionControlPanel.add(removeConditionBtn);

			conditionsMainPanel.add(conditionControlPanel, BorderLayout.SOUTH);

			// === SPLIT PANE: Points vs Conditions ===
			javax.swing.JSplitPane splitPane = new javax.swing.JSplitPane(
				javax.swing.JSplitPane.VERTICAL_SPLIT,
				pointsPanel,
				conditionsMainPanel
			);
			splitPane.setResizeWeight(0.6); // Points get 60%, Conditions get 40%
			splitPane.setDividerSize(6);
			splitPane.setOneTouchExpandable(true);

			add(splitPane, BorderLayout.CENTER);

			// Bottom section: Hover Text + Remove Button
			JPanel bottomSection = new JPanel();
			bottomSection.setLayout(new BoxLayout(bottomSection, BoxLayout.Y_AXIS));

			// Hover Text
			JPanel hoverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			hoverPanel.add(new JLabel("Hover Text:"));
			hoverTextField = new JTextField(35);
			hoverPanel.add(hoverTextField);
			bottomSection.add(hoverPanel);

			// Remove button
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
			JButton removeBtn = new JButton("Remove this Custom Click Area");
			removeBtn.addActionListener(e -> removeThis());
			buttonPanel.add(removeBtn);
			bottomSection.add(buttonPanel);

			add(bottomSection, BorderLayout.SOUTH);

			// Load initial data
			refreshConditions();
		}

		private void addPoint() {
			int pointNum = pointsTableModel.getRowCount() + 1;
			pointsTableModel.addRow(new Object[]{"point" + pointNum, 0, 0, 0});
		}

		/**
		 * Update the points table with new points from Point Editor
		 */
		private void updatePointsTable(List<java.awt.Point> updatedPoints) {
			// Clear current table
			pointsTableModel.setRowCount(0);

			// Add updated points
			for (int i = 0; i < updatedPoints.size(); i++) {
				java.awt.Point p = updatedPoints.get(i);
				pointsTableModel.addRow(new Object[]{"point" + (i + 1), p.x, p.y, 0});
			}

			parentDialog.parent.log("Updated " + updatedPoints.size() + " points in Custom Click Area");
		}

		private void removeSelectedPoint() {
			int selectedRow = pointsTable.getSelectedRow();
			if (selectedRow >= 0) {
				pointsTableModel.removeRow(selectedRow);
			}
		}

		private void addCondition() {
			Map<String, Boolean> allConditions = Conditions.getAllConditions();
			java.util.List<String> availableConditions = new java.util.ArrayList<>();

			for (String condName : allConditions.keySet()) {
				if (!condName.startsWith("isInInventory_") && !conditionCheckboxes.containsKey(condName)) {
					availableConditions.add(condName);
				}
			}

			if (availableConditions.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"No more conditions available. Use 'Manage Conditions' to create new ones.",
						"No Conditions",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			String selected = (String) JOptionPane.showInputDialog(this, "Select condition to add:",
					"Add Condition", JOptionPane.PLAIN_MESSAGE, null,
					availableConditions.toArray(), availableConditions.get(0));

			if (selected != null) {
				refreshConditions();
			}
		}

		private void removeSelectedConditions() {
			java.util.List<String> toRemove = new java.util.ArrayList<>();
			for (Map.Entry<String, JCheckBox> entry : conditionCheckboxes.entrySet()) {
				if (entry.getValue().isSelected()) {
					toRemove.add(entry.getKey());
				}
			}

			// Remove conditions
			refreshConditions();
		}

		private void refreshConditions() {
			conditionsListPanel.removeAll();
			conditionCheckboxes.clear();

			// Placeholder - empty for now
			JLabel noCondLabel = new JLabel("No conditions set");
			noCondLabel.setForeground(Color.GRAY);
			conditionsListPanel.add(noCondLabel);

			conditionsListPanel.revalidate();
			conditionsListPanel.repaint();
		}

		private void enableClickToAddMode() {
			// Get current points from table
			List<java.awt.Point> currentPoints = new java.util.ArrayList<>();
			for (int i = 0; i < pointsTableModel.getRowCount(); i++) {
				try {
					int x = Integer.parseInt(pointsTableModel.getValueAt(i, 1).toString());
					int y = Integer.parseInt(pointsTableModel.getValueAt(i, 2).toString());
					currentPoints.add(new java.awt.Point(x, y));
				} catch (Exception e) {
					parentDialog.parent.log("ERROR parsing point: " + e.getMessage());
				}
			}

			// Get item name for Point Editor title
			String itemName = parentDialog.selectedItem != null ? parentDialog.selectedItem.getName() : null;

			// Open Point Editor Dialog
			PointEditorDialog pointEditor = new PointEditorDialog(
				parentDialog.parent,
				currentPoints,
				updatedPoints -> {
					// Callback: Update this panel's table with the new points
					updatePointsTable(updatedPoints);

					// Auto-save the item
					try {
						parentDialog.saveItem();
					} catch (Exception ex) {
						parentDialog.parent.log("ERROR auto-saving item: " + ex.getMessage());
					}
				},
				itemName
			);

			pointEditor.setVisible(true);
			parentDialog.parent.log("Opened Point Editor for Custom Click Area");
		}

		private void disableClickToAddMode() {
			parentDialog.parent.getGame().setAddPointMode(false, null);
			parentDialog.parent.getGame().setCustomClickAreaPanelForAddMode(null);
			parentDialog.parent.log("Click to Add mode disabled");
		}

		/**
		 * Called by AdventureGame when a point is clicked in add mode
		 */
		public void addPointAtPosition(int x, int y) {
			int pointNum = pointsTableModel.getRowCount() + 1;
			pointsTableModel.addRow(new Object[]{"point" + pointNum, x, y, 0});
			parentDialog.parent.log("Added point " + pointNum + " at (" + x + ", " + y + ")");

			// Auto-save after adding point
			try {
				parentDialog.saveItem();
			} catch (Exception ex) {
				parentDialog.parent.log("ERROR auto-saving after adding point: " + ex.getMessage());
			}
		}

		private void loadData(CustomClickArea area) {
			// Load points
			java.util.List<java.awt.Point> points = area.getPoints();
			for (int i = 0; i < points.size(); i++) {
				java.awt.Point p = points.get(i);
				pointsTableModel.addRow(new Object[]{"point" + (i + 1), p.x, p.y, 0});
			}

			// Load hover text
			hoverTextField.setText(area.getHoverText());

			// Load conditions (TODO: implement when condition UI is ready)
		}

		public CustomClickArea getData() {
			// Create CustomClickArea from panel data
			CustomClickArea area = new CustomClickArea();

			// Get points from table
			for (int i = 0; i < pointsTableModel.getRowCount(); i++) {
				try {
					int x = Integer.parseInt(pointsTableModel.getValueAt(i, 1).toString());
					int y = Integer.parseInt(pointsTableModel.getValueAt(i, 2).toString());
					area.addPoint(x, y);
				} catch (Exception e) {
					System.err.println("Error parsing point from table: " + e.getMessage());
				}
			}

			// Get hover text
			area.setHoverText(hoverTextField.getText());

			// Get conditions (TODO: implement when condition UI is ready)

			return area;
		}

		private void removeThis() {
			disableClickToAddMode(); // Disable click mode if active

			int confirm = JOptionPane.showConfirmDialog(this,
					"Remove this Custom Click Area?",
					"Confirm Removal",
					JOptionPane.YES_NO_OPTION);

			if (confirm == JOptionPane.YES_OPTION) {
				parentDialog.removeCustomClickAreaPanel(this);
			}
		}
	}

	/**
	 * Panel for a single Moving Range (Schema: ListItemMovingRange)
	 */
	private class MovingRangePanel extends JPanel {
		private ItemEditorDialog parentDialog;
		private MovingRange movingRange;
		private JTextField keyAreaNameField;
		private JPanel conditionsListPanel;
		private Map<String, JCheckBox> conditionCheckboxes;

		public MovingRangePanel(ItemEditorDialog parentDialog) {
			this.parentDialog = parentDialog;
			this.movingRange = null;
			this.conditionCheckboxes = new java.util.LinkedHashMap<>();

			initUI();
		}

		public MovingRangePanel(ItemEditorDialog parentDialog, MovingRange range) {
			this.parentDialog = parentDialog;
			this.movingRange = range;
			this.conditionCheckboxes = new java.util.LinkedHashMap<>();

			initUI();
			if (range != null) {
				loadData(range);
			}
		}

		private void initUI() {
			setLayout(new BorderLayout(10, 10));

			// Create titled border
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY, 2),
							"Moving Range",
							javax.swing.border.TitledBorder.LEFT,
							javax.swing.border.TitledBorder.TOP,
							new Font("Arial", Font.BOLD, 12)
					),
					new javax.swing.border.EmptyBorder(10, 10, 10, 10)));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

			// === TOP: KeyArea Name ===
			JPanel keyAreaPanel = new JPanel(new BorderLayout());
			keyAreaPanel.setBorder(BorderFactory.createTitledBorder("KeyArea Name"));
			keyAreaNameField = new JTextField();
			keyAreaNameField.setPreferredSize(new Dimension(300, 30));
			JPanel keyAreaFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			keyAreaFieldPanel.add(keyAreaNameField);
			keyAreaPanel.add(keyAreaFieldPanel, BorderLayout.CENTER);

			// === BOTTOM: Conditions Section ===
			JPanel conditionsMainPanel = new JPanel(new BorderLayout(5, 5));
			conditionsMainPanel.setBorder(BorderFactory.createTitledBorder("Conditions"));

			// Conditions list
			conditionsListPanel = new JPanel();
			conditionsListPanel.setLayout(new BoxLayout(conditionsListPanel, BoxLayout.Y_AXIS));
			JScrollPane conditionsScroll = new JScrollPane(conditionsListPanel);
			conditionsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			conditionsMainPanel.add(conditionsScroll, BorderLayout.CENTER);

			// Condition control buttons
			JPanel conditionControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
			JButton addConditionBtn = new JButton("Add");
			addConditionBtn.addActionListener(e -> addCondition());
			conditionControlPanel.add(addConditionBtn);

			JButton removeConditionBtn = new JButton("Remove");
			removeConditionBtn.addActionListener(e -> removeSelectedConditions());
			conditionControlPanel.add(removeConditionBtn);

			conditionsMainPanel.add(conditionControlPanel, BorderLayout.SOUTH);

			// === SPLIT PANE: KeyArea vs Conditions ===
			javax.swing.JSplitPane splitPane = new javax.swing.JSplitPane(
				javax.swing.JSplitPane.VERTICAL_SPLIT,
				keyAreaPanel,
				conditionsMainPanel
			);
			splitPane.setResizeWeight(0.3); // KeyArea gets 30%, Conditions get 70%
			splitPane.setDividerSize(6);
			splitPane.setOneTouchExpandable(true);

			add(splitPane, BorderLayout.CENTER);

			// Bottom buttons
			JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
			JButton deleteBtn = new JButton("Delete");
			deleteBtn.addActionListener(e -> deleteThis());
			bottomPanel.add(deleteBtn);

			add(bottomPanel, BorderLayout.SOUTH);

			// Load initial data
			refreshConditions();
		}

		private void addCondition() {
			Map<String, Boolean> allConditions = Conditions.getAllConditions();
			java.util.List<String> availableConditions = new java.util.ArrayList<>();

			for (String condName : allConditions.keySet()) {
				if (!condName.startsWith("isInInventory_") && !conditionCheckboxes.containsKey(condName)) {
					availableConditions.add(condName);
				}
			}

			if (availableConditions.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"No more conditions available. Use 'Manage Conditions' to create new ones.",
						"No Conditions",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			String selected = (String) JOptionPane.showInputDialog(this, "Select condition to add:",
					"Add Condition", JOptionPane.PLAIN_MESSAGE, null,
					availableConditions.toArray(), availableConditions.get(0));

			if (selected != null) {
				refreshConditions();
			}
		}

		private void removeSelectedConditions() {
			java.util.List<String> toRemove = new java.util.ArrayList<>();
			for (Map.Entry<String, JCheckBox> entry : conditionCheckboxes.entrySet()) {
				if (entry.getValue().isSelected()) {
					toRemove.add(entry.getKey());
				}
			}

			// Remove conditions
			refreshConditions();
		}

		private void refreshConditions() {
			conditionsListPanel.removeAll();
			conditionCheckboxes.clear();

			// Placeholder - empty for now
			JLabel noCondLabel = new JLabel("No conditions set");
			noCondLabel.setForeground(Color.GRAY);
			conditionsListPanel.add(noCondLabel);

			conditionsListPanel.revalidate();
			conditionsListPanel.repaint();
		}

		private void deleteThis() {
			int confirm = JOptionPane.showConfirmDialog(this,
					"Delete this Moving Range?",
					"Confirm Deletion",
					JOptionPane.YES_NO_OPTION);

			if (confirm == JOptionPane.YES_OPTION) {
				parentDialog.removeMovingRangePanel(this);
			}
		}

		/**
		 * Loads data from a MovingRange object into this panel
		 */
		private void loadData(MovingRange range) {
			// Load KeyArea name
			keyAreaNameField.setText(range.getKeyAreaName());

			// Load conditions
			Map<String, Boolean> conditions = range.getConditions();
			conditionsListPanel.removeAll();
			conditionCheckboxes.clear();

			if (conditions != null && !conditions.isEmpty()) {
				for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
					String condName = entry.getKey();
					boolean requiredValue = entry.getValue();

					JPanel condPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
					JCheckBox checkbox = new JCheckBox(condName + " = " + requiredValue);
					checkbox.setSelected(false); // For removal selection
					condPanel.add(checkbox);

					conditionCheckboxes.put(condName, checkbox);
					conditionsListPanel.add(condPanel);
				}
			} else {
				JLabel noCondLabel = new JLabel("No conditions set");
				noCondLabel.setForeground(Color.GRAY);
				conditionsListPanel.add(noCondLabel);
			}

			conditionsListPanel.revalidate();
			conditionsListPanel.repaint();
		}

		/**
		 * Extracts data from this panel into a MovingRange object
		 */
		public MovingRange getData() {
			MovingRange range = new MovingRange();

			// Get KeyArea name
			range.setKeyAreaName(keyAreaNameField.getText());

			// Get conditions from checkboxes
			for (Map.Entry<String, JCheckBox> entry : conditionCheckboxes.entrySet()) {
				String condName = entry.getKey();
				// Parse the required value from checkbox text
				String checkboxText = entry.getValue().getText();
				boolean requiredValue = checkboxText.contains("= true");
				range.addCondition(condName, requiredValue);
			}

			return range;
		}
	}
}
