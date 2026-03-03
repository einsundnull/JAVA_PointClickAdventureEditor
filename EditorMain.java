package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import main.KeyArea.ActionHandler;

public class EditorMain extends JFrame {
	private AdventureGame game;
	private JTextArea outputArea;
	private JPanel controlPanel;
	private JList<String> keyAreaList;
	private DefaultListModel<String> keyAreaListModel;
	private KeyArea selectedKeyArea;
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

	public EditorMain(AdventureGame game) {
		this.game = game;
		System.out.println("EditorWindow: Constructor started");

		// CRITICAL: Disable auto-save IMMEDIATELY before doing anything else
		AutoSaveManager.setEnabled(false);
		System.out.println("EditorWindow: Auto-save DISABLED");

		try {
			setTitle("Main Editor - ALT+E to toggle");
			setSize(600, 800);
			setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			// CRITICAL: Set window state BEFORE anything else
			setExtendedState(JFrame.NORMAL); // Not minimized
			setResizable(true);
			System.out.println("EditorWindow: Basic setup complete");

			// Add window listener to reload progress when editor closes
			addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowOpened(java.awt.event.WindowEvent e) {
					// Auto-save already disabled in constructor
					log("✓ Editor opened - AUTO-SAVE DISABLED (Editor Mode)");
				}

				@Override
				public void windowClosing(java.awt.event.WindowEvent e) {
					// Switch to Gaming Mode when editor closes (via X button)
					game.switchToGamingMode();
					log("✓ Editor closed - Switched to GAMING MODE");
				}
			});
			System.out.println("EditorWindow: Window listeners added");

			System.out.println("EditorWindow: Calling initUI()...");
			initUI();
			System.out.println("EditorWindow: initUI() completed");

			// Ensure critical directories exist (AFTER initUI so outputArea is initialized)
			ensureDirectoriesExist();

			// Position window: Center on screen (safer than relative positioning)
			setLocationRelativeTo(null);
			System.out.println("EditorWindow: Positioned window at screen center");

			// Load defaults when editor opens (default mode is "Load Defaults")
			System.out.println("EditorWindow: Loading defaults...");
			Conditions.loadFromDefaults();

			// Only reload scene if a valid scene is loaded
			String currentSceneName = game.getCurrentSceneName();
			System.out.println("EditorWindow: Current scene name: " + currentSceneName);
			if (currentSceneName != null && !currentSceneName.equals("none")) {
				System.out.println("EditorWindow: Loading scene from DEFAULTS: " + currentSceneName);
				game.loadSceneFromDefault(currentSceneName);
			}

			// Load current scene info
			System.out.println("EditorWindow: Updating scene info...");
			updateSceneInfo();
			System.out.println("EditorWindow: Refreshing key area list...");
			refreshKeyAreaList();
			System.out.println("EditorWindow: Refreshing item list...");
			refreshItemList();
			System.out.println("EditorWindow: Loading all scenes...");
			loadAllScenes();

			System.out.println("EditorWindow: Constructor completed successfully");
		} catch (Exception e) {
			System.err.println("ERROR in EditorWindow constructor: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to create EditorWindow", e);
		}
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

		JPanel sceneButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

		// [Load] - Loads the default values from /resources/scenes/<name>.txt
		JButton loadSceneBtn = new JButton("Load");
		loadSceneBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		loadSceneBtn.setPreferredSize(new Dimension(70, 28));
		loadSceneBtn.addActionListener(e -> openSelectedScene());
		sceneButtonPanel.add(loadSceneBtn);

		// [New] - Creates new Scene file
		JButton newSceneBtn = new JButton("New");
		newSceneBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		newSceneBtn.setPreferredSize(new Dimension(70, 28));
		newSceneBtn.addActionListener(e -> createNewScene());
		sceneButtonPanel.add(newSceneBtn);

		// [Edit] - Opens Scene Editor
		JButton editSceneBtn = new JButton("Edit");
		editSceneBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editSceneBtn.setPreferredSize(new Dimension(70, 28));
		editSceneBtn.addActionListener(e -> editSelectedScene());
		sceneButtonPanel.add(editSceneBtn);

		// [Copy] - Copies selected Scene file
		JButton copySceneBtn = new JButton("Copy");
		copySceneBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		copySceneBtn.setPreferredSize(new Dimension(70, 28));
		copySceneBtn.addActionListener(e -> copySelectedScene());
		sceneButtonPanel.add(copySceneBtn);

		// [Delete] - Deletes the selected Scene file
		JButton deleteSceneBtn = new JButton("Delete");
		deleteSceneBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteSceneBtn.setPreferredSize(new Dimension(75, 28));
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

		// [New] - Creates new KeyArea file and writes reference to scenes/<nameOfScene>.txt
		JButton newKeyAreaBtn = new JButton("New");
		newKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		newKeyAreaBtn.setPreferredSize(new Dimension(70, 28));
		newKeyAreaBtn.addActionListener(e -> addNewKeyArea());
		listButtonPanel.add(newKeyAreaBtn);

		// [Edit] - Opens KeyArea Editor (consolidated: Hover, Points, Rename, Type)
		JButton editKeyAreaBtn = new JButton("Edit");
		editKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editKeyAreaBtn.setPreferredSize(new Dimension(70, 28));
		editKeyAreaBtn.addActionListener(e -> openKeyAreaEditor());
		listButtonPanel.add(editKeyAreaBtn);

		// [Actions] - Opens KeyArea Action Editor
		JButton actionsKeyAreaBtn = new JButton("Actions");
		actionsKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		actionsKeyAreaBtn.setPreferredSize(new Dimension(75, 28));
		actionsKeyAreaBtn.addActionListener(e -> openActionsEditor());
		listButtonPanel.add(actionsKeyAreaBtn);

		// [Copy] - Copies selected KeyArea file and writes reference
		JButton copyKeyAreaBtn = new JButton("Copy");
		copyKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		copyKeyAreaBtn.setPreferredSize(new Dimension(70, 28));
		copyKeyAreaBtn.addActionListener(e -> copySelectedKeyArea());
		listButtonPanel.add(copyKeyAreaBtn);

		// [Delete] - Deletes the selected KeyArea file
		JButton deleteKeyAreaBtn = new JButton("Delete");
		deleteKeyAreaBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteKeyAreaBtn.setPreferredSize(new Dimension(75, 28));
		deleteKeyAreaBtn.addActionListener(e -> deleteKeyArea());
		listButtonPanel.add(deleteKeyAreaBtn);

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

		// Don't add KeyArea panel here yet - will add after Items panel

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

		// [New] - Creates new Item file and writes reference to scenes/<nameOfScene>.txt
		JButton newItemBtn = new JButton("New");
		newItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		newItemBtn.setPreferredSize(new Dimension(70, 28));
		newItemBtn.addActionListener(e -> addNewItem());
		itemButtonPanel.add(newItemBtn);

		// [Edit] - Opens Item Editor
		JButton editItemBtn = new JButton("Edit");
		editItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editItemBtn.setPreferredSize(new Dimension(70, 28));
		editItemBtn.addActionListener(e -> openItemEditorForItem());
		itemButtonPanel.add(editItemBtn);

		// [Actions] - Opens Item Action Editor
		JButton actionsItemBtn = new JButton("Actions");
		actionsItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		actionsItemBtn.setPreferredSize(new Dimension(75, 28));
		actionsItemBtn.addActionListener(e -> openItemActionsEditor());
		itemButtonPanel.add(actionsItemBtn);

		// [Copy] - Copies selected Item file and writes reference
		JButton copyItemBtn = new JButton("Copy");
		copyItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		copyItemBtn.setPreferredSize(new Dimension(70, 28));
		copyItemBtn.addActionListener(e -> copySelectedItem());
		itemButtonPanel.add(copyItemBtn);

		// [Delete] - Deletes the selected Item file
		JButton deleteItemBtn = new JButton("Delete");
		deleteItemBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteItemBtn.setPreferredSize(new Dimension(75, 28));
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

		// Add Items panel first, then KeyAreas panel
		controlPanel.add(itemListPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		controlPanel.add(listPanel); // KeyArea panel added here
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Dialogs ListView (Collapseable) - NEW per Schema
		JPanel dialogListPanel = new JPanel(new BorderLayout());
		dialogListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		dialogListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
		dialogListPanel.setBorder(BorderFactory.createTitledBorder("Dialogs"));

		// Toggle button
		JPanel dialogHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton dialogToggleBtn = new JButton("▼");
		dialogToggleBtn.setPreferredSize(new Dimension(40, 25));
		dialogHeaderPanel.add(dialogToggleBtn);

		JPanel dialogListContentPanel = new JPanel(new BorderLayout());

		DefaultListModel<String> dialogListModel = new DefaultListModel<>();
		JList<String> dialogList = new JList<>(dialogListModel);
		dialogList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane dialogListScroll = new JScrollPane(dialogList);
		dialogListScroll.setPreferredSize(new Dimension(0, 100));
		dialogListContentPanel.add(dialogListScroll, BorderLayout.CENTER);

		JPanel dialogButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));

		// [New] - Creates new Dialog file
		JButton newDialogBtn = new JButton("New");
		newDialogBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		newDialogBtn.setPreferredSize(new Dimension(70, 28));
		newDialogBtn.addActionListener(e -> log("TODO: New Dialog"));
		dialogButtonPanel.add(newDialogBtn);

		// [Edit] - Opens Dialog Editor
		JButton editDialogBtn = new JButton("Edit");
		editDialogBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		editDialogBtn.setPreferredSize(new Dimension(70, 28));
		editDialogBtn.addActionListener(e -> log("TODO: Edit Dialog"));
		dialogButtonPanel.add(editDialogBtn);

		// [Copy] - Copies selected Dialog
		JButton copyDialogBtn = new JButton("Copy");
		copyDialogBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		copyDialogBtn.setPreferredSize(new Dimension(70, 28));
		copyDialogBtn.addActionListener(e -> log("TODO: Copy Dialog"));
		dialogButtonPanel.add(copyDialogBtn);

		// [Delete] - Deletes the selected Dialog
		JButton deleteDialogBtn = new JButton("Delete");
		deleteDialogBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		deleteDialogBtn.setPreferredSize(new Dimension(75, 28));
		deleteDialogBtn.addActionListener(e -> log("TODO: Delete Dialog"));
		dialogButtonPanel.add(deleteDialogBtn);

		dialogListContentPanel.add(dialogButtonPanel, BorderLayout.SOUTH);

		dialogListPanel.add(dialogHeaderPanel, BorderLayout.NORTH);
		dialogListPanel.add(dialogListContentPanel, BorderLayout.CENTER);

		// Toggle collapse/expand
		dialogToggleBtn.addActionListener(e -> {
			boolean isVisible = dialogListContentPanel.isVisible();
			dialogListContentPanel.setVisible(!isVisible);
			dialogToggleBtn.setText(isVisible ? "►" : "▼");
			dialogListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, isVisible ? 40 : 200));
			dialogListPanel.revalidate();
			dialogListPanel.repaint();
		});

		controlPanel.add(dialogListPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		// Manager Buttons (per Schema)
		JPanel managerButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		managerButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		managerButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		JButton conditionsManagerBtn = new JButton("Conditions Manager");
		conditionsManagerBtn.addActionListener(e -> openConditionsManager());
		managerButtonsPanel.add(conditionsManagerBtn);

		JButton buttonManagerBtn = new JButton("Button Manager");
		buttonManagerBtn.addActionListener(e -> openActionButtonEditor());
		managerButtonsPanel.add(buttonManagerBtn);

		JButton imageManagerBtn = new JButton("Image Manager");
		imageManagerBtn.addActionListener(e -> log("TODO: Image Manager"));
		managerButtonsPanel.add(imageManagerBtn);

		JButton openResourcesBtn = new JButton("Open Resources");
		openResourcesBtn.addActionListener(e -> openResourcesFolder());
		managerButtonsPanel.add(openResourcesBtn);

		controlPanel.add(managerButtonsPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

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
//	public void selectPoint(int pointIndex) {
//		if (pointIndex >= 0 && pointIndex < pointDropdownModel.getSize()) {
//			pointDropdown.setSelectedIndex(pointIndex);
//			// This triggers onPointSelected automatically
//		}
//	}

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

	private void openActionButtonEditor() {
		log("Opening Action Button Editor...");
		ActionButtonEditorDialog dialog = new ActionButtonEditorDialog(this);
		dialog.setVisible(true);
		log("Action Button Editor opened");
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
		log("Scene info updated: " + sceneName);
	}

	private void loadAllScenes() {
		sceneListModel.clear();
		File scenesDir = new File("resources/scenes");

		if (!scenesDir.exists()) {
			log("⚠️  Scenes directory not found, creating it: " + scenesDir.getAbsolutePath());
			boolean created = scenesDir.mkdirs();
			if (!created) {
				log("ERROR: Failed to create scenes directory");
				return;
			}
		}

		if (!scenesDir.isDirectory()) {
			log("ERROR: resources/scenes exists but is not a directory!");
			return;
		}

		File[] sceneFiles = scenesDir.listFiles((dir, name) -> name.endsWith(".txt"));
		if (sceneFiles != null) {
			for (File file : sceneFiles) {
				try {
					String sceneName = file.getName().replace(".txt", "");
					sceneListModel.addElement(sceneName);
				} catch (Exception e) {
					log("⚠️  Error loading scene file: " + file.getName() + " - " + e.getMessage());
				}
			}
		} else {
			log("⚠️  No scene files found or directory cannot be read");
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

		String selectedSceneName = sceneListModel.getElementAt(selectedIndex);
		log("Opening Hybrid Scene Editor for: " + selectedSceneName);

		try {
			// Load scene
			Scene scene = SceneLoader.loadSceneFromDefault(selectedSceneName, game.getGameProgress());

			// Open new Hybrid Scene Editor
			SceneEditorDialogHybrid dialog = new SceneEditorDialogHybrid(this, scene, game, this);
			dialog.setVisible(true);

			// Refresh scene list after editing (in case it was renamed)
			loadAllScenes();
			refreshKeyAreaList();
			refreshItemList();
		} catch (Exception e) {
			log("ERROR: Failed to open Scene Editor - " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error loading scene:\n" + e.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
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

	// ========================================
	// COPY METHODS (Schema-compliant)
	// ========================================

	/**
	 * [Copy] - Copies selected Scene file to scenes/<nameOfScene_copy>.txt
	 * Note: Scenes do NOT write references when copied (unique to Scenes)
	 */
	private void copySelectedScene() {
		int selectedIndex = sceneList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a scene first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		String originalSceneName = sceneListModel.getElementAt(selectedIndex);

		// Check if original scene file exists
		File originalFile = new File("resources/scenes/" + originalSceneName + ".txt");
		if (!originalFile.exists()) {
			log("ERROR: Original scene file not found: " + originalFile.getAbsolutePath());
			JOptionPane.showMessageDialog(this,
				"Original scene file not found!\n\nFile: " + originalSceneName + ".txt",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String copySceneName = originalSceneName + "_copy";

		// Check for name conflict
		File copyFile = new File("resources/scenes/" + copySceneName + ".txt");
		int counter = 1;
		while (copyFile.exists()) {
			copySceneName = originalSceneName + "_copy" + counter;
			copyFile = new File("resources/scenes/" + copySceneName + ".txt");
			counter++;
		}

		try {
			// Load original scene
			Scene originalScene = SceneLoader.loadSceneFromDefault(originalSceneName, game.getGameProgress());

			if (originalScene == null) {
				throw new IOException("Failed to load original scene");
			}

			// Create copy with new name
			Scene copiedScene = new Scene(copySceneName);
			copiedScene.setBackgroundImagePath(originalScene.getBackgroundImagePath());

			// Copy all items, keyareas, etc. from original scene
			for (Item item : originalScene.getItems()) {
				copiedScene.addItem(item);
			}
			for (KeyArea keyArea : originalScene.getKeyAreas()) {
				copiedScene.addKeyArea(keyArea);
			}

			// Save copied scene to DEFAULT file
			SceneSaver.saveSceneToDefault(copiedScene);

			// Refresh scene list
			loadAllScenes();
			log("✓ Scene copied: " + originalSceneName + " → " + copySceneName);

			JOptionPane.showMessageDialog(this,
				"Scene copied successfully!\n\n" +
				"Original: " + originalSceneName + "\n" +
				"Copy: " + copySceneName,
				"Scene Copied", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			log("ERROR copying scene (IO): " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error copying scene:\n\n" + e.getMessage() +
				"\n\nCheck that the scene file exists and is readable.",
				"Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			log("ERROR copying scene: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Unexpected error copying scene:\n\n" + e.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * [Copy] - Copies selected Item file to items/<nameOfItem_copy>.txt
	 * AND writes reference to scenes/<nameOfScene>.txt
	 */
	private void copySelectedItem() {
		int selectedIndex = itemList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select an item first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (selectedIndex >= currentScene.getItems().size()) {
			JOptionPane.showMessageDialog(this, "Invalid item selection!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Item originalItem = currentScene.getItems().get(selectedIndex);
		String originalItemName = originalItem.getName();

		// Check if original item file exists
		File originalFile = new File("resources/items/" + originalItemName + ".txt");
		if (!originalFile.exists()) {
			log("⚠️  Original item file not found: " + originalFile.getAbsolutePath());
			log("⚠️  Will copy from in-memory item data");
		}

		String copyItemName = originalItemName + "_copy";

		// Ensure items directory exists
		File itemsDir = new File("resources/items");
		if (!itemsDir.exists()) {
			boolean created = itemsDir.mkdirs();
			if (!created) {
				log("ERROR: Failed to create items directory");
				JOptionPane.showMessageDialog(this,
					"Error: Could not create items directory!",
					"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// Check for name conflict
		File copyFile = new File("resources/items/" + copyItemName + ".txt");
		int counter = 1;
		while (copyFile.exists()) {
			copyItemName = originalItemName + "_copy" + counter;
			copyFile = new File("resources/items/" + copyItemName + ".txt");
			counter++;
		}

		try {
			// Load original item (with fallback to in-memory copy)
			Item copiedItem;
			if (originalFile.exists()) {
				copiedItem = ItemLoader.loadItemFromDefault(originalItemName);
			} else {
				// Fallback: create copy from in-memory item
				log("⚠️  Using in-memory item data for copy");
				copiedItem = originalItem; // This references the same object, should deep copy
			}

			if (copiedItem == null) {
				throw new IOException("Failed to load original item");
			}

			// Create new item with copied name
			copiedItem.setName(copyItemName);

			// Save copied item to DEFAULT file
			ItemSaver.saveItemToDefault(copiedItem);

			// Write reference to current scene file
			currentScene.addItem(copiedItem);
			SceneSaver.saveSceneToDefault(currentScene);

			// Refresh item list
			refreshItemList();
			log("✓ Item copied: " + originalItemName + " → " + copyItemName);
			log("  Reference written to: scenes/" + currentScene.getName() + ".txt");

			JOptionPane.showMessageDialog(this,
				"Item copied successfully!\n\n" +
				"Original: " + originalItemName + "\n" +
				"Copy: " + copyItemName + "\n\n" +
				"Reference added to scene: " + currentScene.getName(),
				"Item Copied", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			log("ERROR copying item (IO): " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error copying item:\n\n" + e.getMessage() +
				"\n\nCheck that the item file exists and is readable.",
				"Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			log("ERROR copying item: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Unexpected error copying item:\n\n" + e.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * [Copy] - Copies selected KeyArea file to keyareas/<nameOfKeyArea_copy>.txt
	 * AND writes reference to scenes/<nameOfScene>.txt
	 */
	private void copySelectedKeyArea() {
		int selectedIndex = keyAreaList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a keyarea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) {
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (selectedIndex >= currentScene.getKeyAreas().size()) {
			JOptionPane.showMessageDialog(this, "Invalid keyarea selection!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		KeyArea originalKeyArea = currentScene.getKeyAreas().get(selectedIndex);
		String originalKeyAreaName = originalKeyArea.getName();
		String copyKeyAreaName = originalKeyAreaName + "_copy";

		// Check for name conflict
		int counter = 1;
		while (keyAreaExists(copyKeyAreaName)) {
			copyKeyAreaName = originalKeyAreaName + "_copy" + counter;
			counter++;
		}

		try {
			// Create deep copy of KeyArea
			KeyArea copiedKeyArea = new KeyArea(originalKeyArea.getType(), copyKeyAreaName);

			// Copy all points
			if (originalKeyArea.getPoints() != null) {
				for (Point p : originalKeyArea.getPoints()) {
					if (p != null) {
						copiedKeyArea.addPoint(new Point(p.x, p.y));
					}
				}
			}

			// Copy actions
			if (originalKeyArea.getActions() != null) {
				for (Map.Entry<String, ActionHandler> entry : originalKeyArea.getActions().entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						copiedKeyArea.addAction(entry.getKey(), entry.getValue());
					}
				}
			}

			// Copy hover display conditions
			if (originalKeyArea.getHoverDisplayConditions() != null) {
				for (Map.Entry<String, String> entry : originalKeyArea.getHoverDisplayConditions().entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						copiedKeyArea.addHoverDisplayCondition(entry.getKey(), entry.getValue());
					}
				}
			}

			// Add to current scene
			currentScene.addKeyArea(copiedKeyArea);

			// Save scene to DEFAULT file (this saves the reference)
			SceneSaver.saveSceneToDefault(currentScene);

			// Refresh keyarea list
			refreshKeyAreaList();
			log("✓ KeyArea copied: " + originalKeyAreaName + " → " + copyKeyAreaName);
			log("  Reference written to: scenes/" + currentScene.getName() + ".txt");

			JOptionPane.showMessageDialog(this,
				"KeyArea copied successfully!\n\n" +
				"Original: " + originalKeyAreaName + "\n" +
				"Copy: " + copyKeyAreaName + "\n\n" +
				"Reference added to scene: " + currentScene.getName(),
				"KeyArea Copied", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			log("ERROR copying keyarea (IO): " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error copying keyarea:\n\n" + e.getMessage() +
				"\n\nFailed to save the scene file.",
				"Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			log("ERROR copying keyarea: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Unexpected error copying keyarea:\n\n" + e.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean keyAreaExists(String name) {
		Scene currentScene = game.getCurrentScene();
		if (currentScene == null) return false;

		for (KeyArea ka : currentScene.getKeyAreas()) {
			if (ka.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * [Edit] - Opens KeyArea Editor (consolidates Hover, Points, Rename, Type functionality)
	 */
	private void openKeyAreaEditor() {
		int selectedIndex = keyAreaList.getSelectedIndex();
		if (selectedIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a keyarea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Scene currentScene = game.getCurrentScene();
		if (currentScene == null || selectedIndex >= currentScene.getKeyAreas().size()) {
			JOptionPane.showMessageDialog(this, "Invalid selection!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		KeyArea selectedKeyArea = currentScene.getKeyAreas().get(selectedIndex);

		// Open consolidated KeyArea Editor Dialog
		KeyAreaEditorDialog editor = new KeyAreaEditorDialog(this, selectedKeyArea, currentScene);
		editor.setVisible(true);

		// Refresh after editing
		refreshKeyAreaList();
		game.repaintGamePanel();
	}

	/**
	 * Ensure all critical directories exist, create them if missing
	 */
	private void ensureDirectoriesExist() {
		String[] directories = {
			"resources",
			"resources/scenes",
			"resources/scenes-progress",
			"resources/items",
			"resources/items-progress",
			"resources/actions",
			"resources/actions-progress",
			"resources/images",
			"resources/dialogs",
			"resources/manual2"
		};

		for (String dirPath : directories) {
			File dir = new File(dirPath);
			if (!dir.exists()) {
				boolean created = dir.mkdirs();
				if (created) {
					log("✓ Created missing directory: " + dirPath);
					System.out.println("EditorWindow: Created directory: " + dirPath);
				} else {
					log("⚠️  Failed to create directory: " + dirPath);
					System.err.println("EditorWindow: Failed to create directory: " + dirPath);
				}
			}
		}
	}
}