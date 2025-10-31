package main2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

public class EditorWindow extends JFrame {
	private AdventureGame game;
	private JTextArea outputArea;
	private JPanel controlPanel;
	private JList<String> keyAreaList;
	private DefaultListModel<String> keyAreaListModel;
	private JComboBox<String> pointDropdown;
	private DefaultComboBoxModel<String> pointDropdownModel;
	private KeyArea selectedKeyArea;
	private JTextField xField;
	private JTextField yField;
	private JButton updatePointButton;
	private JButton deletePointButton;
	private JButton addPointButton;
	private JCheckBox alwaysOnTopCheckbox;
	private boolean addPointMode = false;
	private JLabel sceneLabel;
	private JList<String> sceneList;
	private DefaultListModel<String> sceneListModel;

	// Item management
	private JList<String> itemList;
	private DefaultListModel<String> itemListModel;
	private Item selectedItem;

	// Collapseable panels content
	private JPanel sceneListContentPanel;
	private JPanel keyAreaListContentPanel;
	private JPanel itemListContentPanel;

	// Reference to open PointsManager
	private PointsManagerDialog pointsManager;

	public EditorWindow(AdventureGame game) {
		this.game = game;

		setTitle("Scene Editor - ALT+E to toggle");
		setSize(600, 800);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		initUI();

		// Position next to main window
		Point mainLocation = game.getLocation();
		setLocation(mainLocation.x + game.getWidth(), mainLocation.y);

		// Load current scene info
		updateSceneInfo();
		refreshKeyAreaList();
		loadAllScenes();
	}

	private void initUI() {
		setLayout(new BorderLayout());

		// Control Panel
		controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Title
		JLabel titleLabel = new JLabel("🎮 Scene Editor");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(titleLabel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Always On Top Checkbox and Current Scene on same line
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		alwaysOnTopCheckbox = new JCheckBox("Always On Top");
		alwaysOnTopCheckbox.setSelected(true); // Default = true
		alwaysOnTopCheckbox.addActionListener(e -> {
			setAlwaysOnTop(alwaysOnTopCheckbox.isSelected());
			log("Always On Top: " + alwaysOnTopCheckbox.isSelected());
		});
		setAlwaysOnTop(true); // Set it on startup
		topPanel.add(alwaysOnTopCheckbox);

		sceneLabel = new JLabel("  |  Current Scene: " + game.getCurrentSceneName());
		sceneLabel.setFont(new Font("Arial", Font.BOLD, 12));
		topPanel.add(sceneLabel);

		controlPanel.add(topPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Scene ListView (Collapseable)
		JPanel sceneListPanel = new JPanel(new BorderLayout());
		sceneListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sceneListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
		sceneListPanel.setBorder(BorderFactory.createTitledBorder("All Scenes"));

		// Toggle button
		JPanel sceneHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton sceneToggleBtn = new JButton("▼");
		sceneToggleBtn.setPreferredSize(new Dimension(40, 25));
		sceneHeaderPanel.add(sceneToggleBtn);

		sceneListContentPanel = new JPanel(new BorderLayout());

		sceneListModel = new DefaultListModel<>();
		sceneList = new JList<>(sceneListModel);
		sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sceneList.setCellRenderer(new SceneTileRenderer());
		sceneList.setFixedCellHeight(60);
		sceneList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onSceneSelected();
			}
		});

		JScrollPane sceneListScroll = new JScrollPane(sceneList);
		sceneListScroll.setPreferredSize(new Dimension(0, 120));
		sceneListContentPanel.add(sceneListScroll, BorderLayout.CENTER);

		JPanel sceneButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton refreshSceneBtn = new JButton("Refresh");
		refreshSceneBtn.addActionListener(e -> loadAllScenes());
		sceneButtonPanel.add(refreshSceneBtn);

		JButton openSceneBtn = new JButton("Open Selected");
		openSceneBtn.addActionListener(e -> openSelectedScene());
		sceneButtonPanel.add(openSceneBtn);

		JButton createSceneBtn = new JButton("Create New");
		createSceneBtn.addActionListener(e -> createNewScene());
		sceneButtonPanel.add(createSceneBtn);

		JButton editSceneBtn = new JButton("✏️ Edit");
		editSceneBtn.addActionListener(e -> editSelectedScene());
		sceneButtonPanel.add(editSceneBtn);

		JButton deleteSceneBtn = new JButton("🗑️ Delete");
		deleteSceneBtn.addActionListener(e -> deleteSelectedScene());
		sceneButtonPanel.add(deleteSceneBtn);

		sceneListContentPanel.add(sceneButtonPanel, BorderLayout.SOUTH);

		sceneListPanel.add(sceneHeaderPanel, BorderLayout.NORTH);
		sceneListPanel.add(sceneListContentPanel, BorderLayout.CENTER);

		// Toggle collapse/expand
		sceneToggleBtn.addActionListener(e -> {
			boolean isVisible = sceneListContentPanel.isVisible();
			sceneListContentPanel.setVisible(!isVisible);
			sceneToggleBtn.setText(isVisible ? "►" : "▼");
			sceneListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 180));
			sceneListPanel.revalidate();
			sceneListPanel.repaint();
		});

		controlPanel.add(sceneListPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// KeyArea ListView (Collapseable)
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		listPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
		listPanel.setBorder(BorderFactory.createTitledBorder("KeyAreas"));

		// Toggle button
		JPanel keyAreaHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton keyAreaToggleBtn = new JButton("▼");
		keyAreaToggleBtn.setPreferredSize(new Dimension(40, 25));
		keyAreaHeaderPanel.add(keyAreaToggleBtn);

		keyAreaListContentPanel = new JPanel(new BorderLayout());

		keyAreaListModel = new DefaultListModel<>();
		keyAreaList = new JList<>(keyAreaListModel);
		keyAreaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		keyAreaList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onKeyAreaSelected();
			}
		});

		// Add DELETE key support for KeyAreas
		keyAreaList.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
					deleteKeyArea();
				}
			}
		});

		JScrollPane listScroll = new JScrollPane(keyAreaList);
		listScroll.setPreferredSize(new Dimension(0, 100));
		keyAreaListContentPanel.add(listScroll, BorderLayout.CENTER);

		JPanel listButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

		JButton refreshListBtn = new JButton("Refresh");
		refreshListBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		refreshListBtn.setPreferredSize(new Dimension(80, 28));
		refreshListBtn.addActionListener(e -> refreshKeyAreaList());
		listButtonPanel.add(refreshListBtn);

		JButton addKeyAreaBtn = new JButton("Add New");
		addKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		addKeyAreaBtn.setPreferredSize(new Dimension(85, 28));
		addKeyAreaBtn.addActionListener(e -> addNewKeyArea());
		listButtonPanel.add(addKeyAreaBtn);

		JButton editActionsBtn = new JButton("⚙️ Actions");
		editActionsBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editActionsBtn.setPreferredSize(new Dimension(95, 28));
		editActionsBtn.addActionListener(e -> openActionsEditor());
		listButtonPanel.add(editActionsBtn);

		JButton editHoverBtn = new JButton("💬 Hover");
		editHoverBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editHoverBtn.setPreferredSize(new Dimension(90, 28));
		editHoverBtn.addActionListener(e -> openHoverEditor());
		listButtonPanel.add(editHoverBtn);

		JButton managePointsBtn = new JButton("📍 Points");
		managePointsBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		managePointsBtn.setPreferredSize(new Dimension(90, 28));
		managePointsBtn.addActionListener(e -> openPointsManager());
		listButtonPanel.add(managePointsBtn);

		JButton renameBtn = new JButton("✏️ Rename");
		renameBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		renameBtn.setPreferredSize(new Dimension(95, 28));
		renameBtn.addActionListener(e -> renameKeyArea());
		listButtonPanel.add(renameBtn);

		JButton changeTypeBtn = new JButton("🔄 Type");
		changeTypeBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		changeTypeBtn.setPreferredSize(new Dimension(85, 28));
		changeTypeBtn.addActionListener(e -> changeKeyAreaType());
		listButtonPanel.add(changeTypeBtn);

		JButton deleteBtn = new JButton("🗑️ Delete");
		deleteBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteBtn.setPreferredSize(new Dimension(90, 28));
		deleteBtn.addActionListener(e -> deleteKeyArea());
		listButtonPanel.add(deleteBtn);

		keyAreaListContentPanel.add(listButtonPanel, BorderLayout.SOUTH);

		listPanel.add(keyAreaHeaderPanel, BorderLayout.NORTH);
		listPanel.add(keyAreaListContentPanel, BorderLayout.CENTER);

		// Toggle collapse/expand
		keyAreaToggleBtn.addActionListener(e -> {
			boolean isVisible = keyAreaListContentPanel.isVisible();
			keyAreaListContentPanel.setVisible(!isVisible);
			keyAreaToggleBtn.setText(isVisible ? "►" : "▼");
			listPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 200));
			listPanel.revalidate();
			listPanel.repaint();
		});

		controlPanel.add(listPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Item ListView (Collapseable)
		JPanel itemListPanel = new JPanel(new BorderLayout());
		itemListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		itemListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
		itemListPanel.setBorder(BorderFactory.createTitledBorder("Items"));

		// Toggle button
		JPanel itemHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton itemToggleBtn = new JButton("▼");
		itemToggleBtn.setPreferredSize(new Dimension(40, 25));
		itemHeaderPanel.add(itemToggleBtn);

		itemListContentPanel = new JPanel(new BorderLayout());

		itemListModel = new DefaultListModel<>();
		itemList = new JList<>(itemListModel);
		itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		itemList.setCellRenderer(new ItemCellRenderer());
		itemList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				onItemSelected();
			}
		});

		// Add DELETE key support for Items
		itemList.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
					deleteItem();
				}
			}
		});

		JScrollPane itemListScroll = new JScrollPane(itemList);
		itemListScroll.setPreferredSize(new Dimension(0, 100));
		itemListContentPanel.add(itemListScroll, BorderLayout.CENTER);

		JPanel itemButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

		JButton refreshItemBtn = new JButton("Refresh");
		refreshItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		refreshItemBtn.setPreferredSize(new Dimension(80, 28));
		refreshItemBtn.addActionListener(e -> refreshItemList());
		itemButtonPanel.add(refreshItemBtn);

		JButton addItemBtn = new JButton("Add New");
		addItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		addItemBtn.setPreferredSize(new Dimension(85, 28));
		addItemBtn.addActionListener(e -> addNewItem());
		itemButtonPanel.add(addItemBtn);

		JButton editItemBtn = new JButton("⚙️ Edit Item");
		editItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editItemBtn.setPreferredSize(new Dimension(100, 28));
		editItemBtn.addActionListener(e -> openItemEditorForItem());
		itemButtonPanel.add(editItemBtn);

		JButton actionsItemBtn = new JButton("🎬 Actions");
		actionsItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		actionsItemBtn.setPreferredSize(new Dimension(95, 28));
		actionsItemBtn.addActionListener(e -> openItemActionsEditor());
		itemButtonPanel.add(actionsItemBtn);

		JButton renameItemBtn = new JButton("✏️ Rename");
		renameItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		renameItemBtn.setPreferredSize(new Dimension(95, 28));
		renameItemBtn.addActionListener(e -> renameItem());
		itemButtonPanel.add(renameItemBtn);

		JButton deleteItemBtn = new JButton("🗑️ Delete");
		deleteItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteItemBtn.setPreferredSize(new Dimension(90, 28));
		deleteItemBtn.addActionListener(e -> deleteItem());
		itemButtonPanel.add(deleteItemBtn);

		itemListContentPanel.add(itemButtonPanel, BorderLayout.SOUTH);

		itemListPanel.add(itemHeaderPanel, BorderLayout.NORTH);
		itemListPanel.add(itemListContentPanel, BorderLayout.CENTER);

		// Toggle collapse/expand
		itemToggleBtn.addActionListener(e -> {
			boolean isVisible = itemListContentPanel.isVisible();
			itemListContentPanel.setVisible(!isVisible);
			itemToggleBtn.setText(isVisible ? "►" : "▼");
			itemListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 250));
			itemListPanel.revalidate();
			itemListPanel.repaint();
		});

		controlPanel.add(itemListPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Point Editor
		JPanel pointPanel = new JPanel();
		pointPanel.setLayout(new BoxLayout(pointPanel, BoxLayout.Y_AXIS));
		pointPanel.setBorder(BorderFactory.createTitledBorder("Point Editor"));
		pointPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Point Dropdown with X/Y on same line
		JPanel dropdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		dropdownPanel.add(new JLabel("Select Point:"));
		pointDropdownModel = new DefaultComboBoxModel<>();
		pointDropdown = new JComboBox<>(pointDropdownModel);
		pointDropdown.addActionListener(e -> onPointSelected());
		dropdownPanel.add(pointDropdown);

		// X and Y Coordinates on same line as dropdown
		dropdownPanel.add(new JLabel("  X:"));
		xField = new JTextField(6);
		dropdownPanel.add(xField);

		dropdownPanel.add(new JLabel("Y:"));
		yField = new JTextField(6);
		dropdownPanel.add(yField);

		pointPanel.add(dropdownPanel);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		updatePointButton = new JButton("Update Point");
		updatePointButton.addActionListener(e -> updateSelectedPoint());
		buttonPanel.add(updatePointButton);

		addPointButton = new JButton("Add Point (Click on Game)");
		addPointButton.addActionListener(e -> startAddPointMode());
		buttonPanel.add(addPointButton);

		deletePointButton = new JButton("Delete Point");
		deletePointButton.addActionListener(e -> deleteSelectedPoint());
		buttonPanel.add(deletePointButton);

		pointPanel.add(buttonPanel);

		// Initially disabled
		pointDropdown.setEnabled(false);
		xField.setEnabled(false);
		yField.setEnabled(false);
		updatePointButton.setEnabled(false);
		addPointButton.setEnabled(false);
		deletePointButton.setEnabled(false);

		controlPanel.add(pointPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// Scene Controls
		JPanel scenePanel = new JPanel();
		scenePanel.setLayout(new BoxLayout(scenePanel, BoxLayout.Y_AXIS));
		scenePanel.setBorder(BorderFactory.createTitledBorder("Scene Controls"));
		scenePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton visualizeBtn = new JButton("Toggle Visualization (ALT+P)");
		visualizeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		visualizeBtn.addActionListener(e -> game.togglePathVisualization());
		scenePanel.add(visualizeBtn);
		scenePanel.add(Box.createRigidArea(new Dimension(0, 5)));

		// Image rotation and flip buttons
		JPanel imageControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		imageControlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton rotateLeftBtn = new JButton("↶ -90°");
		rotateLeftBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		rotateLeftBtn.setPreferredSize(new Dimension(65, 25));
		rotateLeftBtn.addActionListener(e -> rotateImage(-90));
		imageControlsPanel.add(rotateLeftBtn);

		JButton rotateRightBtn = new JButton("↷ +90°");
		rotateRightBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		rotateRightBtn.setPreferredSize(new Dimension(65, 25));
		rotateRightBtn.addActionListener(e -> rotateImage(90));
		imageControlsPanel.add(rotateRightBtn);

		JButton flipHBtn = new JButton("⇄ Flip H");
		flipHBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		flipHBtn.setPreferredSize(new Dimension(75, 25));
		flipHBtn.addActionListener(e -> flipImage(true));
		imageControlsPanel.add(flipHBtn);

		JButton flipVBtn = new JButton("⇅ Flip V");
		flipVBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		flipVBtn.setPreferredSize(new Dimension(75, 25));
		flipVBtn.addActionListener(e -> flipImage(false));
		imageControlsPanel.add(flipVBtn);

		scenePanel.add(imageControlsPanel);
		scenePanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JButton reloadBtn = new JButton("Reload Scene (F5)");
		reloadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		reloadBtn.addActionListener(e -> {
			game.reloadCurrentScene();
			refreshKeyAreaList();
		});
		scenePanel.add(reloadBtn);
		scenePanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JButton saveBtn = new JButton("Save Scene to File");
		saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		saveBtn.addActionListener(e -> saveSceneToFile());
		scenePanel.add(saveBtn);
		scenePanel.add(Box.createRigidArea(new Dimension(0, 10)));

		JButton conditionsBtn = new JButton("🎛️ Manage Conditions");
		conditionsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		conditionsBtn.addActionListener(e -> openConditionsManager());
		scenePanel.add(conditionsBtn);

		controlPanel.add(scenePanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// Hotkeys Info
		JPanel hotkeysPanel = new JPanel();
		hotkeysPanel.setLayout(new BoxLayout(hotkeysPanel, BoxLayout.Y_AXIS));
		hotkeysPanel.setBorder(BorderFactory.createTitledBorder("Hotkeys"));
		hotkeysPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		hotkeysPanel.add(new JLabel("ALT + E: Toggle Editor"));
		hotkeysPanel.add(new JLabel("ALT + P: Toggle Visualization"));
		hotkeysPanel.add(new JLabel("F5: Reload Scene"));

		controlPanel.add(hotkeysPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// Scene Manager
		JPanel sceneManagerPanel = new JPanel();
		sceneManagerPanel.setLayout(new BoxLayout(sceneManagerPanel, BoxLayout.Y_AXIS));
		sceneManagerPanel.setBorder(BorderFactory.createTitledBorder("Scene Manager"));
		sceneManagerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton manageBoolsBtn = new JButton("🎛️ Manage Booleans");
		manageBoolsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		manageBoolsBtn.addActionListener(e -> openConditionsManager());
		sceneManagerPanel.add(manageBoolsBtn);

		JButton manageDialogsBtn = new JButton("💬 Manage Dialogs");
		manageDialogsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		manageDialogsBtn.addActionListener(e -> openDialogManager());
		sceneManagerPanel.add(manageDialogsBtn);

		JButton manageItemsBtn = new JButton("🎒 Manage Items");
		manageItemsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		manageItemsBtn.addActionListener(e -> openItemEditor());
		sceneManagerPanel.add(manageItemsBtn);

		JButton openSceneFileBtn = new JButton("📂 Open Scene File");
		openSceneFileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		openSceneFileBtn.addActionListener(e -> openSceneFileInExplorer());
		sceneManagerPanel.add(openSceneFileBtn);

		JButton openResourcesFolderBtn = new JButton("📁 Open Resources Folder");
		openResourcesFolderBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		openResourcesFolderBtn.addActionListener(e -> openResourcesFolder());
		sceneManagerPanel.add(openResourcesFolderBtn);

		controlPanel.add(sceneManagerPanel);

		add(new JScrollPane(controlPanel), BorderLayout.CENTER);

		// Output Area (Collapseable, default collapsed)
		JPanel outputPanel = new JPanel(new BorderLayout());
		outputPanel.setBorder(BorderFactory.createTitledBorder("Output Log"));

		// Toggle button
		JPanel outputHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton outputToggleBtn = new JButton("►");
		outputToggleBtn.setPreferredSize(new Dimension(40, 25));
		outputHeaderPanel.add(outputToggleBtn);

		JPanel outputContentPanel = new JPanel(new BorderLayout());

		outputArea = new JTextArea(10, 50);
		outputArea.setEditable(false);
		outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane outputScrollPane = new JScrollPane(outputArea);
		outputScrollPane.setPreferredSize(new Dimension(0, 150));

		outputContentPanel.add(outputScrollPane, BorderLayout.CENTER);

		outputPanel.add(outputHeaderPanel, BorderLayout.NORTH);
		outputPanel.add(outputContentPanel, BorderLayout.CENTER);

		// Start collapsed
		outputContentPanel.setVisible(false);

		// Toggle collapse/expand
		outputToggleBtn.addActionListener(e -> {
			boolean isVisible = outputContentPanel.isVisible();
			outputContentPanel.setVisible(!isVisible);
			outputToggleBtn.setText(isVisible ? "►" : "▼");
			outputPanel.revalidate();
			outputPanel.repaint();
		});

		add(outputPanel, BorderLayout.SOUTH);

		log("Editor initialized. Press ALT+E to toggle.");
	}

	public void log(String message) {
		outputArea.append(message + "\n");
		outputArea.setCaretPosition(outputArea.getDocument().getLength());
	}

	public void refreshKeyAreaList() {
		keyAreaListModel.clear();
		Scene currentScene = game.getCurrentScene();

		if (currentScene == null) {
			log("No scene loaded");
			return;
		}

		for (KeyArea area : currentScene.getKeyAreas()) {
			keyAreaListModel
					.addElement(area.getName() + " (" + area.getType() + ") - " + area.getPoints().size() + " points");
		}

		log("KeyArea list refreshed: " + keyAreaListModel.size() + " areas");

		// Also refresh item list
		refreshItemList();
	}

	public void refreshItemList() {
		// Clear image cache first
		clearItemImageCache();

		itemListModel.clear();
		Scene currentScene = game.getCurrentScene();

		if (currentScene == null) {
			log("No items to load");
			return;
		}

		for (Item item : currentScene.getItems()) {
			String visibility = item.isVisible() ? "✓" : "✗";
			String inventory = item.isInInventory() ? "[INV]" : "";
			itemListModel.addElement(visibility + " " + item.getName() + " " + inventory + " @ ("
					+ item.getPosition().x + "," + item.getPosition().y + ")");
		}

		// Force repaint
		itemList.repaint();
		itemList.revalidate();
	}

	/**
	 * Clear the item image cache to force reload
	 */
	public void clearItemImageCache() {
		if (itemList.getCellRenderer() instanceof ItemCellRenderer) {
			ItemCellRenderer renderer = (ItemCellRenderer) itemList.getCellRenderer();
			renderer.clearCache();
		}

		log("Item list refreshed: " + itemListModel.size() + " items");
	}

	private void onKeyAreaSelected() {
		int selectedIndex = keyAreaList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedKeyArea = null;
			clearPointEditor();
			return;
		}

		Scene currentScene = game.getCurrentScene();
		if (currentScene == null)
			return;

		List<KeyArea> areas = currentScene.getKeyAreas();
		if (selectedIndex >= areas.size())
			return;

		selectedKeyArea = areas.get(selectedIndex);
		log("Selected: " + selectedKeyArea.getName());

		// Update point dropdown
		updatePointDropdown();

		// Enable editor
		pointDropdown.setEnabled(true);
		addPointButton.setEnabled(true);
	}

	private void updatePointDropdown() {
		pointDropdownModel.removeAllElements();

		if (selectedKeyArea == null)
			return;

		List<Point> points = selectedKeyArea.getPoints();
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			pointDropdownModel.addElement("Point " + i + " (" + p.x + ", " + p.y + ")");
		}

		if (pointDropdownModel.getSize() > 0) {
			pointDropdown.setSelectedIndex(0);
		}
	}

	private void onPointSelected() {
		if (selectedKeyArea == null)
			return;

		int pointIndex = pointDropdown.getSelectedIndex();
		if (pointIndex < 0) {
			clearPointFields();
			return;
		}

		List<Point> points = selectedKeyArea.getPoints();
		if (pointIndex >= points.size())
			return;

		Point p = points.get(pointIndex);
		xField.setText(String.valueOf(p.x));
		yField.setText(String.valueOf(p.y));

		xField.setEnabled(true);
		yField.setEnabled(true);
		updatePointButton.setEnabled(true);
		deletePointButton.setEnabled(points.size() > 3); // Need at least 3 points
	}

	private void updateSelectedPoint() {
		if (selectedKeyArea == null)
			return;

		int pointIndex = pointDropdown.getSelectedIndex();
		if (pointIndex < 0)
			return;

		try {
			int x = Integer.parseInt(xField.getText());
			int y = Integer.parseInt(yField.getText());

			List<Point> points = selectedKeyArea.getPoints();
			if (pointIndex < points.size()) {
				Point p = points.get(pointIndex);
				p.x = x;
				p.y = y;

				// Trigger polygon update
				selectedKeyArea.getPolygon();

				log("Updated point " + pointIndex + " to (" + x + ", " + y + ")");
				updatePointDropdown();
				game.repaintGamePanel();
			}
		} catch (NumberFormatException e) {
			log("ERROR: Invalid coordinates");
		}
	}

	private void addPointToKeyArea() {
		if (selectedKeyArea == null)
			return;

		try {
			int x = Integer.parseInt(xField.getText());
			int y = Integer.parseInt(yField.getText());

			selectedKeyArea.addPoint(x, y);
			log("Added point at (" + x + ", " + y + ")");
			updatePointDropdown();
			game.repaintGamePanel();

			// Auto-save to file
			autoSaveScene();
		} catch (NumberFormatException e) {
			log("ERROR: Invalid coordinates");
		}
	}

	private void startAddPointMode() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			return;
		}

		addPointMode = true;
		game.setAddPointMode(true, this);
		addPointButton.setText("Adding Point... (Click on Game)");
		addPointButton.setEnabled(false);
		log("Click on the game screen to add point to " + selectedKeyArea.getName());
	}

	public void addPointAtPosition(int x, int y) {
		if (selectedKeyArea == null || !addPointMode)
			return;

		selectedKeyArea.addPoint(x, y);
		log("Added point at (" + x + ", " + y + ")");
		updatePointDropdown();

		// Select the new point
		int newIndex = selectedKeyArea.getPoints().size() - 1;
		if (newIndex >= 0) {
			pointDropdown.setSelectedIndex(newIndex);
		}

		game.repaintGamePanel();

		// Auto-save to file
		autoSaveScene();

		// Reset mode
		addPointMode = false;
		game.setAddPointMode(false, null);
		addPointButton.setText("Add Point (Click on Game)");
		addPointButton.setEnabled(true);
	}

	private void autoSaveScene() {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null)
			return;

		try {
			String filename = "resources/scenes/" + currentScene.getName() + ".txt";
			SceneSaver.saveScene(currentScene, filename);
			log("Auto-saved to " + filename);
		} catch (Exception e) {
			log("ERROR: Auto-save failed - " + e.getMessage());
		}
	}

	private void deleteSelectedPoint() {
		if (selectedKeyArea == null)
			return;

		int pointIndex = pointDropdown.getSelectedIndex();
		if (pointIndex < 0)
			return;

		List<Point> points = selectedKeyArea.getPoints();
		if (points.size() <= 3) {
			log("ERROR: Cannot delete - need at least 3 points");
			return;
		}

		if (pointIndex < points.size()) {
			points.remove(pointIndex);
			selectedKeyArea.getPolygon(); // Trigger update
			log("Deleted point " + pointIndex);
			updatePointDropdown();
			game.repaintGamePanel();
		}
	}

	private void clearPointEditor() {
		pointDropdownModel.removeAllElements();
		clearPointFields();

		pointDropdown.setEnabled(false);
		xField.setEnabled(false);
		yField.setEnabled(false);
		updatePointButton.setEnabled(false);
		addPointButton.setEnabled(false);
		deletePointButton.setEnabled(false);
	}

	private void clearPointFields() {
		xField.setText("");
		yField.setText("");
	}

	private void addNewKeyArea() {
		String name = JOptionPane.showInputDialog(this, "Enter KeyArea name:");
		if (name != null && !name.trim().isEmpty()) {
			String typeStr = (String) JOptionPane.showInputDialog(this, "Select KeyArea type:", "Type",
					JOptionPane.QUESTION_MESSAGE, null,
					new String[] { "Interaction", "Transition", "Movement_Bounds", "Character_Range" },
					"Interaction");

			if (typeStr != null) {
				log("Creating new " + typeStr + " KeyArea: " + name);
				game.createNewKeyArea(name, typeStr);
				refreshKeyAreaList();
			}
		}
	}

	private void saveSceneToFile() {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			log("ERROR: No scene loaded");
			return;
		}

		try {
			String filename = "resources/scenes/" + currentScene.getName() + ".txt";
			log("Saving scene to: " + filename);

			SceneSaver.saveScene(currentScene, filename);

			log("SUCCESS: Scene saved!");
		} catch (Exception e) {
			log("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Select a KeyArea programmatically (called from game when dragging)
	 */
	public void selectKeyArea(KeyArea area) {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null)
			return;

		List<KeyArea> areas = currentScene.getKeyAreas();
		int index = areas.indexOf(area);

		if (index >= 0 && index < keyAreaListModel.size()) {
			keyAreaList.setSelectedIndex(index);
			// This triggers onKeyAreaSelected automatically
		}
	}

	/**
	 * Select an Item programmatically (called from game when dragging Item points)
	 */
	public void selectItem(Item item) {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null)
			return;

		List<Item> items = currentScene.getItems();
		int index = items.indexOf(item);

		if (index >= 0 && index < itemListModel.size()) {
			itemList.setSelectedIndex(index);
			// This triggers onItemSelected automatically
		}
	}

	/**
	 * Select a point programmatically (called from game when dragging)
	 */
	public void selectPoint(int pointIndex) {
		if (pointIndex >= 0 && pointIndex < pointDropdownModel.getSize()) {
			pointDropdown.setSelectedIndex(pointIndex);
			// This triggers onPointSelected automatically
		}
	}

	/**
	 * Update point coordinates in UI during drag (live update)
	 */
	public void updatePointCoordinates(KeyArea area, int pointIndex, int x, int y) {
		// Only update if this is the currently selected KeyArea
		if (selectedKeyArea == area) {
			// Update text fields if this point is selected
			if (pointDropdown.getSelectedIndex() == pointIndex) {
				xField.setText(String.valueOf(x));
				yField.setText(String.valueOf(y));
			}

			// Update dropdown text
			if (pointIndex >= 0 && pointIndex < pointDropdownModel.getSize()) {
				pointDropdownModel.removeElementAt(pointIndex);
				pointDropdownModel.insertElementAt("Point " + pointIndex + " (" + x + ", " + y + ")", pointIndex);
				pointDropdown.setSelectedIndex(pointIndex);
			}
		}
	}

	private void openConditionsManager() {
		log("Opening Conditions Manager...");
		ConditionsManagerDialog dialog = new ConditionsManagerDialog(this);
		dialog.setVisible(true);
		log("Conditions Manager closed");
	}

	private void openDialogManager() {
		log("Opening Dialog Manager...");
		DialogManagerDialog dialog = new DialogManagerDialog(this);
		dialog.setVisible(true);
		log("Dialog Manager opened");
	}

	private void openItemEditor() {
		log("Opening Item Editor...");
		ItemEditorDialog dialog = new ItemEditorDialog(this, selectedItem);
		dialog.setVisible(true);
		log("Item Editor opened");
	}

	private void openSceneFileInExplorer() {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			log("ERROR: No scene loaded");
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String sceneName = currentScene.getName();
		File sceneFile = new File("resources/scenes/" + sceneName + ".txt");

		if (!sceneFile.exists()) {
			log("ERROR: Scene file not found: " + sceneFile.getAbsolutePath());
			JOptionPane.showMessageDialog(this, "Scene file not found!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			// Open file in default text editor
			java.awt.Desktop.getDesktop().open(sceneFile);
			log("Opened scene file: " + sceneFile.getAbsolutePath());
		} catch (Exception e) {
			log("ERROR opening file: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void openResourcesFolder() {
		File resourcesFolder = new File("resources");

		if (!resourcesFolder.exists()) {
			log("ERROR: Resources folder not found");
			JOptionPane.showMessageDialog(this, "Resources folder not found!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			// Open folder in file explorer
			java.awt.Desktop.getDesktop().open(resourcesFolder);
			log("Opened resources folder");
		} catch (Exception e) {
			log("ERROR opening folder: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error opening folder: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void openActionsEditor() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		log("Opening Actions Editor for " + selectedKeyArea.getName() + "...");
		ActionsEditorDialog dialog = new ActionsEditorDialog(this, selectedKeyArea);
		dialog.setVisible(true);
		log("Actions Editor closed");
	}

	private void openHoverEditor() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		log("Opening Hover Editor for " + selectedKeyArea.getName() + "...");
		HoverEditorDialog dialog = new HoverEditorDialog(this, selectedKeyArea);
		dialog.setVisible(true);
		log("Hover Editor closed");
	}

	private void openPointsManager() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		log("Opening Point Editor for " + selectedKeyArea.getName() + "...");
		UniversalPointEditorDialog pointEditor = new UniversalPointEditorDialog(this, selectedKeyArea);
		pointEditor.setVisible(true);
		log("Point Editor closed");
	}

	public PointsManagerDialog getPointsManager() {
		return pointsManager;
	}

	private void renameKeyArea() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String oldName = selectedKeyArea.getName();
		String newName = JOptionPane.showInputDialog(this, "Enter new name for KeyArea:", oldName);

		if (newName != null && !newName.trim().isEmpty()) {
			newName = newName.trim();

			// Check if name already exists
			Scene currentScene = game.getCurrentScene();
			for (KeyArea area : currentScene.getKeyAreas()) {
				if (area != selectedKeyArea && area.getName().equals(newName)) {
					JOptionPane.showMessageDialog(this, "A KeyArea with name '" + newName + "' already exists!",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			// Rename KeyArea
			selectedKeyArea.setName(newName);

			log("Renamed KeyArea from '" + oldName + "' to '" + newName + "'");
			refreshKeyAreaList();
			autoSaveCurrentScene();

			JOptionPane.showMessageDialog(this, "KeyArea renamed successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void changeKeyAreaType() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		KeyArea.Type currentType = selectedKeyArea.getType();
		KeyArea.Type[] types = KeyArea.Type.values();
		String[] typeNames = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			typeNames[i] = types[i].toString();
		}

		String selected = (String) JOptionPane.showInputDialog(
				this,
				"Select type for KeyArea '" + selectedKeyArea.getName() + "':",
				"Change KeyArea Type",
				JOptionPane.PLAIN_MESSAGE,
				null,
				typeNames,
				currentType.toString());

		if (selected != null) {
			KeyArea.Type newType = KeyArea.Type.valueOf(selected);
			selectedKeyArea.setType(newType);
			refreshKeyAreaList();
			autoSaveCurrentScene();
			log("✓ Changed KeyArea type to: " + newType);

			// Show info about the type
			String info = "";
			switch (newType) {
				case TRANSITION:
					info = "TRANSITION: Use Actions → LoadScene to define which scene to load when clicked";
					break;
				case INTERACTION:
					info = "INTERACTION: Use Actions to define dialogs or other interactions";
					break;
				case MOVEMENT_BOUNDS:
					info = "MOVEMENT_BOUNDS: Defines where the player can move";
					break;
				case CHARACTER_RANGE:
					info = "CHARACTER_RANGE: Defines movement area for NPCs/characters";
					break;
			}
			if (!info.isEmpty()) {
				JOptionPane.showMessageDialog(this, info, "Type Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void deleteKeyArea() {
		if (selectedKeyArea == null) {
			log("ERROR: Select a KeyArea first!");
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String name = selectedKeyArea.getName();
		int confirm = JOptionPane.showConfirmDialog(this, "Delete KeyArea '" + name + "'?\n\nThis cannot be undone!",
				"Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			Scene currentScene = game.getCurrentScene();
			currentScene.getKeyAreas().remove(selectedKeyArea);

			log("Deleted KeyArea: " + name);
			selectedKeyArea = null;

			// Update all UI components
			refreshKeyAreaList();
			clearPointEditor();
			game.repaintGamePanel();
			autoSaveCurrentScene();

			JOptionPane.showMessageDialog(this, "KeyArea deleted successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public void autoSaveCurrentScene() {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			log("ERROR: No scene to save");
			return;
		}

		try {
			String filename = "resources/scenes/" + currentScene.getName() + ".txt";
			SceneSaver.saveScene(currentScene, filename);
			log("✓ Auto-saved: " + filename);
		} catch (Exception e) {
			log("ERROR saving: " + e.getMessage());
		}
	}

	public AdventureGame getGame() {
		return game;
	}

	public void repaintGamePanel() {
		game.repaintGamePanel();
	}

	public void updateSceneInfo() {
		String sceneName = game.getCurrentSceneName();
		sceneLabel.setText("  |  Current Scene: " + sceneName);
		log("Scene info updated: " + sceneName);
	}

	private void loadAllScenes() {
		sceneListModel.clear();
		File scenesDir = new File("resources/scenes");

		if (!scenesDir.exists() || !scenesDir.isDirectory()) {
			log("ERROR: Scenes directory not found: " + scenesDir.getAbsolutePath());
			return;
		}

		File[] sceneFiles = scenesDir.listFiles((dir, name) -> name.endsWith(".txt"));
		if (sceneFiles != null) {
			for (File file : sceneFiles) {
				String sceneName = file.getName().replace(".txt", "");
				sceneListModel.addElement(sceneName);
			}
		}

		log("Loaded " + sceneListModel.size() + " scenes");
	}

	private void onSceneSelected() {
		int selectedIndex = sceneList.getSelectedIndex();
		if (selectedIndex >= 0) {
			String selectedScene = sceneListModel.getElementAt(selectedIndex);
			log("Selected scene: " + selectedScene);
		}
	}

	private void openSelectedScene() {
		int selectedIndex = sceneList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a scene first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String selectedScene = sceneListModel.getElementAt(selectedIndex);
		log("Opening scene: " + selectedScene);

		game.loadScene(selectedScene);
		updateSceneInfo();
		refreshKeyAreaList();
	}

	private void createNewScene() {
		String sceneName = JOptionPane.showInputDialog(this, "Enter new scene name (e.g., sceneForest):",
				"Create New Scene", JOptionPane.PLAIN_MESSAGE);

		if (sceneName != null && !sceneName.trim().isEmpty()) {
			sceneName = sceneName.trim();

			// Check if already exists
			File sceneFile = new File("resources/scenes/" + sceneName + ".txt");
			if (sceneFile.exists()) {
				JOptionPane.showMessageDialog(this, "Scene '" + sceneName + "' already exists!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Create new scene with default background
			Scene newScene = new Scene(sceneName);
			newScene.setBackgroundImagePath("default.jpg");

			try {
				SceneSaver.saveScene(newScene, sceneFile.getAbsolutePath());
				log("Created new scene: " + sceneName);

				// Reload scene list and open new scene
				loadAllScenes();
				game.loadScene(sceneName);
				updateSceneInfo();
				refreshKeyAreaList();

				JOptionPane.showMessageDialog(this, "Scene created successfully!\n\nFile: " + sceneFile.getName(),
						"Success", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				log("ERROR creating scene: " + e.getMessage());
				JOptionPane.showMessageDialog(this, "Error creating scene: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void editSelectedScene() {
		int selectedIndex = sceneList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a scene first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String selectedScene = sceneListModel.getElementAt(selectedIndex);
		log("Opening Scene Editor for: " + selectedScene);

		SceneEditorDialog dialog = new SceneEditorDialog(this, selectedScene);
		dialog.setVisible(true);

		// Refresh scene list after editing (in case it was renamed)
		loadAllScenes();
	}

	private void deleteSelectedScene() {
		int selectedIndex = sceneList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a scene first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String selectedScene = sceneListModel.getElementAt(selectedIndex);

		try {
			// Find all references first
			log("Searching for references to scene: " + selectedScene);
			java.util.Map<String, java.util.List<Integer>> references = SceneReferenceManager.findSceneReferences(selectedScene);

			// Build confirmation message
			StringBuilder message = new StringBuilder();
			message.append("Delete scene '").append(selectedScene).append("'?\n\n");

			if (references.isEmpty()) {
				message.append("✓ No references found in .txt files.\n\n");
			} else {
				message.append("⚠️ Found references in ").append(references.size()).append(" file(s):\n\n");
				int count = 0;
				for (java.util.Map.Entry<String, java.util.List<Integer>> entry : references.entrySet()) {
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
			message.append("✓ Delete the scene file\n");
			message.append("✓ Remove all references from .txt files\n");
			message.append("\n⚠️ This action cannot be undone!");

			int confirm = JOptionPane.showConfirmDialog(this, message.toString(), "Confirm Scene Deletion",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}

			// Perform deletion
			log("Starting scene deletion for: " + selectedScene);

			// 1. Remove references from .txt files
			java.util.List<String> modifiedFiles = SceneReferenceManager.removeSceneReferences(selectedScene);
			log("Removed references from " + modifiedFiles.size() + " file(s)");

			// 2. Delete the scene file
			if (SceneReferenceManager.deleteSceneFile(selectedScene)) {
				log("Scene file deleted successfully");
			} else {
				log("⚠️ Could not delete scene file");
			}

			// 3. Refresh scene list
			loadAllScenes();

			// 4. Load a different scene if this was the current scene
			Scene currentScene = game.getCurrentScene();
			if (currentScene != null && currentScene.getName().equals(selectedScene)) {
				// Load the first available scene or create a blank one
				if (sceneListModel.size() > 0) {
					String firstScene = sceneListModel.getElementAt(0);
					game.loadScene(firstScene);
					log("Loaded scene: " + firstScene);
				}
			}

			log("✓ Scene deletion complete");

			JOptionPane.showMessageDialog(this,
					"✓ Scene deleted successfully!\n\n" +
					"Deleted:\n" +
					"• Scene file: " + selectedScene + ".txt\n" +
					"• All references in .txt files",
					"Success", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			log("ERROR during scene deletion: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error deleting scene: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void onItemSelected() {
		int selectedIndex = itemList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedItem = null;
			return;
		}

		Scene currentScene = game.getCurrentScene();
		if (currentScene == null)
			return;

		List<Item> items = currentScene.getItems();
		if (selectedIndex >= items.size())
			return;

		selectedItem = items.get(selectedIndex);
		log("Selected Item: " + selectedItem.getName());
	}

	private void addNewItem() {
		String name = JOptionPane.showInputDialog(this, "Enter Item name:");
		if (name != null && !name.trim().isEmpty()) {
			Scene currentScene = game.getCurrentScene();
			if (currentScene == null) {
				log("ERROR: No scene loaded");
				return;
			}

			// Check if name already exists
			for (Item item : currentScene.getItems()) {
				if (item.getName().equals(name.trim())) {
					JOptionPane.showMessageDialog(this, "An item with name '" + name + "' already exists!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			Item newItem = new Item(name.trim());
			newItem.setPosition(100, 100); // Default position
			currentScene.addItem(newItem);

			log("Created new item: " + name);
			refreshItemList();
			autoSaveCurrentScene();

			JOptionPane.showMessageDialog(this, "Item '" + name + "' created!\n\nUse Edit Item to configure it.",
					"Success", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void openItemEditorForItem() {
		if (selectedItem == null) {
			log("ERROR: Select an item first!");
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		log("Opening detailed editor for item: " + selectedItem.getName());
		// Open the existing ItemEditorDialog that's already in the Scene Manager
		openItemEditor();
	}

	private void openItemActionsEditor() {
		if (selectedItem == null) {
			log("ERROR: Select an item first!");
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		log("Opening Actions Editor for item: " + selectedItem.getName());
		ItemActionsEditorDialog dialog = new ItemActionsEditorDialog(this, selectedItem);
		dialog.setVisible(true);
		log("Actions Editor opened for item");
	}


	private void renameItem() {
		if (selectedItem == null) {
			log("ERROR: Select an item first!");
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String oldName = selectedItem.getName();
		String newName = JOptionPane.showInputDialog(this, "Enter new name for Item:", oldName);

		if (newName != null && !newName.trim().isEmpty()) {
			newName = newName.trim();

			// Check if name already exists
			Scene currentScene = game.getCurrentScene();
			for (Item item : currentScene.getItems()) {
				if (item != selectedItem && item.getName().equals(newName)) {
					JOptionPane.showMessageDialog(this, "An item with name '" + newName + "' already exists!",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			// Rename Item
			selectedItem.setName(newName);

			log("Renamed Item from '" + oldName + "' to '" + newName + "'");
			refreshItemList();
			autoSaveCurrentScene();

			JOptionPane.showMessageDialog(this, "Item renamed successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void deleteItem() {
		if (selectedItem == null) {
			log("ERROR: Select an item first!");
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String name = selectedItem.getName();
		int confirm = JOptionPane.showConfirmDialog(this, "Delete Item '" + name + "'?\n\nThis cannot be undone!",
				"Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirm == JOptionPane.YES_OPTION) {
			Scene currentScene = game.getCurrentScene();
			currentScene.getItems().remove(selectedItem);

			log("Deleted Item: " + name);
			selectedItem = null;

			// Update all UI components
			refreshItemList();
			game.repaintGamePanel();
			autoSaveCurrentScene();

			// Force list repaint to update thumbnails
			itemList.repaint();

			JOptionPane.showMessageDialog(this, "Item deleted successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Rotate the background image
	 */
	private void rotateImage(int degrees) {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			log("ERROR: No scene loaded");
			return;
		}

		log("Rotating image by " + degrees + " degrees...");
		game.rotateBackgroundImage(degrees);
		autoSaveCurrentScene();
	}

	/**
	 * Flip the background image
	 */
	private void flipImage(boolean horizontal) {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			log("ERROR: No scene loaded");
			return;
		}

		log("Flipping image " + (horizontal ? "horizontally" : "vertically") + "...");
		game.flipBackgroundImage(horizontal);
		autoSaveCurrentScene();
	}

	/**
	 * Custom cell renderer for Items with image tiles
	 */
	private class ItemCellRenderer extends JLabel implements ListCellRenderer<String> {
		private Map<String, ImageIcon> imageCache = new HashMap<>();
		private static final int TILE_SIZE = 32;

		public ItemCellRenderer() {
			setOpaque(true);
		}

		public void clearCache() {
			imageCache.clear();
			log("EditorWindow image cache cleared");
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {

			setText(value);
			setFont(new Font("Arial", Font.PLAIN, 11));

			// Get the item from the scene
			Scene currentScene = game.getCurrentScene();
			if (currentScene != null && index >= 0 && index < currentScene.getItems().size()) {
				Item item = currentScene.getItems().get(index);

				// Try to load and display item image
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
			} else {
				setIcon(null);
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
	 * Custom renderer for scene list that shows a thumbnail preview
	 */
	class SceneTileRenderer extends JPanel implements javax.swing.ListCellRenderer<String> {
		private JLabel iconLabel;
		private JLabel textLabel;
		private java.util.Map<String, ImageIcon> iconCache = new java.util.HashMap<>();

		public SceneTileRenderer() {
			setLayout(new BorderLayout(5, 0));
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

			iconLabel = new JLabel();
			iconLabel.setPreferredSize(new Dimension(80, 50));
			iconLabel.setHorizontalAlignment(JLabel.CENTER);
			iconLabel.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY, 1));

			textLabel = new JLabel();
			textLabel.setFont(new Font("Arial", Font.PLAIN, 12));

			add(iconLabel, BorderLayout.WEST);
			add(textLabel, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String sceneName, int index,
				boolean isSelected, boolean cellHasFocus) {

			textLabel.setText(sceneName);

			// Load and cache thumbnail
			if (!iconCache.containsKey(sceneName)) {
				try {
					String bgImage = SceneReferenceManager.getSceneBackgroundImage(sceneName);
					if (bgImage != null && !bgImage.isEmpty()) {
						File imageFile = new File("resources/images/" + bgImage);
						if (imageFile.exists()) {
							ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
							Image img = icon.getImage().getScaledInstance(75, 45, Image.SCALE_SMOOTH);
							iconCache.put(sceneName, new ImageIcon(img));
						}
					}
				} catch (Exception e) {
					// Image load failed
				}
			}

			ImageIcon cachedIcon = iconCache.get(sceneName);
			if (cachedIcon != null) {
				iconLabel.setIcon(cachedIcon);
			} else {
				iconLabel.setIcon(null);
				iconLabel.setText("No Image");
			}

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				textLabel.setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
				textLabel.setForeground(list.getForeground());
			}

			setEnabled(list.isEnabled());
			setOpaque(true);

			return this;
		}
	}
}