package main;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import main.ui.components.button.AppButton;
import main.ui.theme.Spacing;
// New UI imports
import main.ui.theme.ThemeManager;

public class AdventureGame extends JFrame {
	// Item corner enum for drag-resize
	private enum ItemCorner {
		NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	private Scene currentScene;
	private GameProgress progress;
	private JPanel gamePanel;
	private JPanel menuPanel;
	private JPanel inventoryPanel;
	private ProcessExecutor processExecutor;
	private Image backgroundImage;
	private JLabel hoverTextLabel;
	private String selectedAction = null;
	private String selectedItem = null;
	private Item selectedInventoryItem = null; // For item-as-cursor mode
	private Point playerPosition;

	// Editor features
	private EditorMain editorWindow;
	private EditorMainSimple editorWindowSimple;
	private DebugWindow debugWindow;
	private boolean showPaths = false; // Default OFF, only ON when editor is visible
	private boolean pathEditMode = false;
	private Point selectedPathPoint = null;
	private int selectedPathPointIndex = -1;
	private Item selectedItemForPointDrag = null; // Item whose point is being dragged
	private CustomClickArea selectedCustomClickAreaForPointDrag = null; // CustomClickArea whose point is being dragged
	private MovingRange selectedMovingRangeForPointDrag = null; // MovingRange whose point is being dragged
	private Path selectedPathForPointDrag = null; // Path whose point is being dragged
	private boolean pointWasDragged = false; // Track if point was actually dragged (moved)
	private boolean addPointMode = false;
	private EditorMain addPointModeEditor = null;
	private UniversalPointEditorDialog pointEditorDialog = null;
	private Object customClickAreaPanel = null; // ItemEditorDialog.CustomClickAreaPanel

	// Simple Editor Add by Click mode
	private boolean addPointModeSimple = false;
	private String addPointModeTypeSimple = ""; // "CustomClickArea", "MovingRange", "Path"
	private Item addPointModeItemSimple = null;
	private PointEditorDialog simplePointEditorDialog = null;
	private ScenePointEditor scenePointEditor = null;

	// Hidden items in editor (checkbox from ItemManagerDialog)
	private java.util.Set<String> hiddenItemsInEditor = new java.util.HashSet<>();

	// Point highlighting in Edit Scene
	private Item highlightedPointItem = null;
	private String highlightedPointType = ""; // "CustomClickArea", "MovingRange", "Path"
	private int highlightedPointIndex = -1;

	// Item selection in Editor mode (when clicking on items in GameView)
	private Item selectedItemInEditor = null; // Currently selected item in editor

	private Item draggedItem = null;
	private ItemCorner draggedCorner = ItemCorner.NONE;
	private Point initialDragPoint = null;

	// Character Movement (for isFollowingOnMouseClick)
	private Item movingCharacter = null;
	private Point characterTargetPosition = null;
	private javax.swing.Timer characterMovementTimer = null;
	private static final int CHARACTER_SPEED = 5; // Pixels per frame

	// Cursor blinking
	private javax.swing.Timer cursorBlinkTimer;
	private boolean cursorVisible = true;
	private KeyArea hoveredKeyArea = null;

	// Menu actions - Loaded dynamically via ButtonsDataManager from
	// resources/buttons/buttons.txt
	// Old hardcoded array removed - replaced with configurable button system

	public AdventureGame() {
		setTitle("Point & Click Adventure");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1024, 768);
		setLocationRelativeTo(null);

		// Ensure progress files exist (creates them from defaults if missing)
		GameStateManager.ensureProgressFilesExist();

		progress = new GameProgress();
		progress.loadProgress();

		playerPosition = new Point(400, 400);

		// DON'T initialize editor here - it will be created on demand when ALT+E is
		// pressed
		// editorWindow = new EditorWindow(this); // REMOVED!

		// Initialize debug window
		debugWindow = new DebugWindow();

		// Initialize process executor
		processExecutor = new ProcessExecutor(this);

		// Set up condition change listener for inventory updates
		setupConditionListener();

		initUI();
		setupHotkeys();
		setupCursorBlinking();

		// Load initial scene (auto-save disabled during load)
		loadScene(progress.getCurrentScene());

		// Enable auto-save for GAME mode (will be disabled when editor opens)
		AutoSaveManager.setEnabled(true);
		System.out.println("✓ Game started - Auto-save ENABLED");

		setVisible(true);
	}

	/**
	 * Sets up a listener to automatically update inventory and scene when
	 * isInInventory conditions change
	 */
	private void setupConditionListener() {
		Conditions.setChangeListener((conditionName, oldValue, newValue) -> {
			// Log to debug window
			debugWindow.logConditionChange(conditionName, oldValue, newValue);

			// Check if this is an isInInventory condition
			if (conditionName.startsWith("isInInventory_")) {
				System.out.println("Inventory condition changed: " + conditionName + " = " + newValue);
				// Update inventory display
				updateInventory();
				// Repaint scene to show/hide items
				if (gamePanel != null) {
					gamePanel.repaint();
				}
			}
		});
	}

	private void setupCursorBlinking() {
		// Set crosshair cursor by default
		gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

		// Timer for blinking over KeyAreas (every 500ms)
		cursorBlinkTimer = new javax.swing.Timer(500, e -> {
			if (hoveredKeyArea != null) {
				cursorVisible = !cursorVisible;
				if (cursorVisible) {
					gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} else {
					gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
		cursorBlinkTimer.start();
	}

	private void setupHotkeys() {
		// ALT + E: Toggle Editor (old)
		KeyStroke altE = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altE, "toggleEditor");
		getRootPane().getActionMap().put("toggleEditor", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleEditor();
			}
		});

		// ALT + S: Toggle Simple Editor (new simplified version)
		KeyStroke altS = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altS, "toggleSimpleEditor");
		getRootPane().getActionMap().put("toggleSimpleEditor", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleSimpleEditor();
			}
		});

		// ALT + P: Toggle Path Visualization
		KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altP, "togglePaths");
		getRootPane().getActionMap().put("togglePaths", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				togglePathVisualization();
			}
		});

		// F5: Reload Scene
		KeyStroke f5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f5, "reloadScene");
		getRootPane().getActionMap().put("reloadScene", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reloadCurrentScene();
			}
		});
	}

	public void toggleEditor() {
		try {
			if (editorWindow == null || !editorWindow.isVisible()) {
				// Create NEW editor window each time (fresh data from .txt files)
				if (editorWindow != null) {
					editorWindow.dispose(); // Clean up old window
				}
				System.out.println("Creating new EditorWindow...");
				editorWindow = new EditorMain(this);
				System.out.println("EditorWindow created successfully");

				// IMPORTANT: Use invokeLater to ensure window is fully initialized
				// before showing it
				javax.swing.SwingUtilities.invokeLater(() -> {
					try {
						System.out.println("Making EditorWindow visible...");

						// Debug: Check window properties
						System.out
								.println("  Window size: " + editorWindow.getWidth() + "x" + editorWindow.getHeight());
						System.out.println("  Window location: " + editorWindow.getLocation());
						System.out.println("  Window state: " + editorWindow.getExtendedState());
						System.out.println("  Is displayable: " + editorWindow.isDisplayable());

						// Set window state
						editorWindow.setExtendedState(JFrame.NORMAL);

						// Pack components (ensures proper sizing)
						editorWindow.validate();

						// Make visible
						editorWindow.setVisible(true);

						// Bring to front (multiple methods for compatibility)
						editorWindow.toFront();
						editorWindow.requestFocus();
						editorWindow.setState(JFrame.NORMAL);

						// Force to front
						editorWindow.setAlwaysOnTop(true);
						editorWindow.setAlwaysOnTop(false);

						// Final debug info
						System.out.println("EditorWindow visible and in front");
						System.out.println("  Is visible: " + editorWindow.isVisible());
						System.out.println("  Is showing: " + editorWindow.isShowing());
						editorWindow.log("Editor opened (fresh .txt data loaded)");
					} catch (Exception e) {
						System.err.println("ERROR showing editor: " + e.getMessage());
						e.printStackTrace();
					}
				});

				// Note: updateSceneInfo(), refreshKeyAreaList(), refreshItemList()
				// are already called in EditorWindow constructor

				// Enable path visualization when editor opens
				showPaths = true;
				gamePanel.repaint();
			} else {
				// Close editor (via Alt+E)
				editorWindow.setVisible(false);
				editorWindow.dispose();
				editorWindow = null; // Clear cache

				// Switch to Gaming Mode
				switchToGamingMode();

				// Disable path visualization when editor closes
				showPaths = false;
				gamePanel.repaint();
			}
		} catch (Exception e) {
			System.err.println("ERROR opening editor: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error opening editor:\n" + e.getMessage(), "Editor Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void toggleSimpleEditor() {
		try {
			if (editorWindowSimple == null || !editorWindowSimple.isVisible()) {
				// Create NEW simple editor window each time (fresh data from .txt files)
				if (editorWindowSimple != null) {
					editorWindowSimple.dispose(); // Clean up old window
				}
				System.out.println("Creating new EditorMainSimple...");
				editorWindowSimple = new EditorMainSimple(this);
				System.out.println("EditorMainSimple created successfully");

				// IMPORTANT: Use invokeLater to ensure window is fully initialized
				javax.swing.SwingUtilities.invokeLater(() -> {
					try {
						System.out.println("Making EditorMainSimple visible...");

						// Set window state
						editorWindowSimple.setExtendedState(JFrame.NORMAL);

						// Make visible
						editorWindowSimple.setVisible(true);

						// Bring to front
						editorWindowSimple.toFront();
						editorWindowSimple.requestFocus();
						editorWindowSimple.setState(JFrame.NORMAL);

						// Force to front
						editorWindowSimple.setAlwaysOnTop(true);
						editorWindowSimple.setAlwaysOnTop(false);

						System.out.println("EditorMainSimple visible and in front");
						editorWindowSimple.log("Simple Editor opened (ALT+S)");
					} catch (Exception e) {
						System.err.println("ERROR showing simple editor: " + e.getMessage());
						e.printStackTrace();
					}
				});

				// Enable path visualization when editor opens
				showPaths = true;
				gamePanel.repaint();
			} else {
				// Close editor (via Alt+S)
				editorWindowSimple.setVisible(false);
				editorWindowSimple.dispose();
				editorWindowSimple = null; // Clear cache

				// Switch to Gaming Mode
				switchToGamingMode();

				// Disable path visualization when editor closes
				showPaths = false;
				gamePanel.repaint();
			}
		} catch (Exception e) {
			System.err.println("ERROR opening simple editor: " + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error opening simple editor:\n" + e.getMessage(), "Editor Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void togglePathVisualization() {
		showPaths = !showPaths;
		gamePanel.repaint();
		if (editorWindow != null) {
			editorWindow.log("Path visualization: " + (showPaths ? "ON" : "OFF"));
		}
	}

	public boolean isShowingPaths() {
		return showPaths;
	}

	public void reloadCurrentScene() {
		String sceneName = currentScene.getName();
		if (editorWindow != null) {
			editorWindow.log("Reloading scene: " + sceneName);
		}
		loadScene(sceneName);
	}

	public String getCurrentSceneName() {
		return currentScene != null ? currentScene.getName() : "none";
	}

	public Scene getCurrentScene() {
		return currentScene;
	}

	public GameProgress getGameProgress() {
		return progress;
	}

	public void setPathEditMode(boolean enabled) {
		this.pathEditMode = enabled;
		if (editorWindow != null) {
			editorWindow.log("Path edit mode: " + (enabled ? "ON - Click to add points" : "OFF"));
		}
	}

	public void setAddPointMode(boolean enabled, EditorMain editor) {
		this.addPointMode = enabled;
		this.addPointModeEditor = editor;

		// Always keep crosshair cursor
		gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void setPointEditorForAddMode(UniversalPointEditorDialog dialog) {
		this.pointEditorDialog = dialog;
	}

	public void setCustomClickAreaPanelForAddMode(Object panel) {
		this.customClickAreaPanel = panel;
	}

	public boolean isAddPointMode() {
		return this.addPointMode;
	}

	public void setAddPointModeSimple(boolean enabled, String pointType, Item item) {
		this.addPointModeSimple = enabled;
		this.addPointModeTypeSimple = pointType;
		this.addPointModeItemSimple = item;

		if (enabled) {
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			System.out.println("Add by Click mode enabled for " + pointType);
		} else {
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			System.out.println("Add by Click mode disabled");
		}
	}

	public void setPointEditorDialog(PointEditorDialog dialog) {
		this.simplePointEditorDialog = dialog;
	}

	public void setScenePointEditor(ScenePointEditor editor) {
		this.scenePointEditor = editor;
	}

	/**
	 * Helper method to check if item should be visible in current mode When
	 * ScenePointEditor is active, respects the visibleInEditor flag
	 */
	private boolean isItemVisibleInCurrentMode(Item item) {
		if (!item.isVisible()) {
			return false;
		}

		if (showPaths) {
			// Respect central editor visibility flag
			if (!item.isVisibleInEditor()) {
				return false;
			}
			// Maintain compatibility with legacy hidden items tracking
			if (hiddenItemsInEditor.contains(item.getName())) {
				return false;
			}
		}
		return true;
	}

	private void setItemEditorVisibilityInternal(Item item, boolean visible) {
		if (item == null) {
			return;
		}

		if (visible) {
			hiddenItemsInEditor.remove(item.getName());
		} else {
			hiddenItemsInEditor.add(item.getName());
		}

		item.setVisibleInEditor(visible);
		gamePanel.repaint();
	}

	public void setItemVisibleInEditor(Item item, boolean visible) {
		setItemEditorVisibilityInternal(item, visible);
	}

	public void setItemVisibleInEditor(String itemName, boolean visible) {
		if (itemName == null) {
			return;
		}

		if (currentScene != null) {
			Item sceneItem = currentScene.getItemByName(itemName);
			setItemEditorVisibilityInternal(sceneItem, visible);
		} else if (!visible) {
			hiddenItemsInEditor.add(itemName);
		} else {
			hiddenItemsInEditor.remove(itemName);
		}
	}

	/**
	 * Hide item in editor (called from ItemManagerDialog checkbox)
	 */
	public void hideItemInEditor(String itemName) {
		setItemVisibleInEditor(itemName, false);
	}

	/**
	 * Show item in editor (called from ItemManagerDialog checkbox)
	 */
	public void showItemInEditor(String itemName) {
		setItemVisibleInEditor(itemName, true);
	}

	public void setShowPaths(boolean show) {
		this.showPaths = show;
		gamePanel.repaint();
	}

	public boolean isShowPaths() {
		return this.showPaths;
	}

	/**
	 * Highlight a specific point in the Edit Scene
	 */
	public void setHighlightedPoint(Item item, String pointType, int pointIndex) {
		this.highlightedPointItem = item;
		this.highlightedPointType = pointType;
		this.highlightedPointIndex = pointIndex;
		gamePanel.repaint();
	}

	/**
	 * Clear point highlighting
	 */
	public void clearHighlightedPoint() {
		this.highlightedPointItem = null;
		this.highlightedPointType = "";
		this.highlightedPointIndex = -1;
		gamePanel.repaint();
	}

	/**
	 * Set item image visibility in editor mode
	 */
	public void setItemImageVisibleInEditor(Item item, boolean visible) {
		if (item != null) {
			item.setVisibleInEditor(visible);
			gamePanel.repaint();
		}
	}

	/**
	 * Set custom click area visibility in editor mode
	 */
	public void setCustomClickAreaVisibleInEditor(Item item, boolean visible) {
		if (item != null) {
			item.setCustomClickAreaVisibleInEditor(visible);
			gamePanel.repaint();
		}
	}

	/**
	 * Set moving range visibility in editor mode
	 */
	public void setMovingRangeVisibleInEditor(Item item, boolean visible) {
		if (item != null) {
			item.setMovingRangeVisibleInEditor(visible);
			gamePanel.repaint();
		}
	}

	/**
	 * Set path visibility in editor mode
	 */
	public void setPathVisibleInEditor(Item item, boolean visible) {
		if (item != null) {
			item.setPathVisibleInEditor(visible);
			gamePanel.repaint();
		}
	}

	// ==================== BIDIRECTIONAL CHECKBOX SYNCHRONIZATION
	// ====================

	/**
	 * Synchronize isFollowingMouse checkbox state between EditorMainSimple and
	 * ScenePointEditor. Called when checkbox changes in either editor.
	 *
	 * @param item      The item whose checkbox state changed
	 * @param following The new checkbox state
	 */
	public void syncFollowingMouseCheckbox(Item item, boolean following) {
		if (item == null)
			return;

		System.out.println("🟢 DEBUG syncFollowingMouseCheckbox: Setting item '" + item.getName()
				+ "' isFollowingMouse=" + following);

		// Update the item itself
		item.setFollowingMouse(following);

		// Verify it was set
		System.out.println(
				"🟢 DEBUG syncFollowingMouseCheckbox: Verified item.isFollowingMouse()=" + item.isFollowingMouse());

		// CRITICAL: Update the item in the current scene as well!
		if (currentScene != null) {
			for (Item sceneItem : currentScene.getItems()) {
				if (sceneItem.getName().equals(item.getName())) {
					sceneItem.setFollowingMouse(following);
					System.out.println("🟢 DEBUG syncFollowingMouseCheckbox: Updated scene item instance");
					break;
				}
			}
		}

		// Auto-save item
		try {
			ItemSaver.saveItemByName(item);
			System.out.println("🟢 DEBUG syncFollowingMouseCheckbox: Item saved successfully");
		} catch (java.io.IOException e) {
			System.err.println("ERROR: Failed to save item during checkbox sync: " + e.getMessage());
		}

		// Sync to EditorMainSimple
		if (editorWindowSimple != null) {
			editorWindowSimple.refreshFollowingMouseCheckbox(item, following);
		}

		// Sync to ScenePointEditor
		if (scenePointEditor != null) {
			scenePointEditor.refreshFollowingMouseCheckbox(item, following);
		}

		gamePanel.repaint();
	}

	/**
	 * Synchronize isFollowingOnMouseClick checkbox state between EditorMainSimple
	 * and ScenePointEditor. Called when checkbox changes in either editor.
	 *
	 * @param item             The item whose checkbox state changed
	 * @param followingOnClick The new checkbox state
	 */
	public void syncFollowingOnClickCheckbox(Item item, boolean followingOnClick) {
		if (item == null)
			return;

		System.out.println("🟡 DEBUG syncFollowingOnClickCheckbox: Setting item '" + item.getName()
				+ "' isFollowingOnMouseClick=" + followingOnClick);

		// Update the item itself
		item.setFollowingOnMouseClick(followingOnClick);

		// Verify it was set
		System.out.println("🟡 DEBUG syncFollowingOnClickCheckbox: Verified item.isFollowingOnMouseClick()="
				+ item.isFollowingOnMouseClick());

		// CRITICAL: Update the item in the current scene as well!
		if (currentScene != null) {
			for (Item sceneItem : currentScene.getItems()) {
				if (sceneItem.getName().equals(item.getName())) {
					sceneItem.setFollowingOnMouseClick(followingOnClick);
					System.out.println("🟡 DEBUG syncFollowingOnClickCheckbox: Updated scene item instance");
					break;
				}
			}
		}

		// Auto-save item
		try {
			ItemSaver.saveItemByName(item);
			System.out.println("🟡 DEBUG syncFollowingOnClickCheckbox: Item saved successfully");
		} catch (java.io.IOException e) {
			System.err.println("ERROR: Failed to save item during checkbox sync: " + e.getMessage());
		}

		// Sync to EditorMainSimple
		if (editorWindowSimple != null) {
			editorWindowSimple.refreshFollowingOnClickCheckbox(item, followingOnClick);
		}

		// Sync to ScenePointEditor
		if (scenePointEditor != null) {
			scenePointEditor.refreshFollowingOnClickCheckbox(item, followingOnClick);
		}

		gamePanel.repaint();
	}

	/**
	 * Synchronize visibleInEditor checkbox state between EditorMainSimple and
	 * ScenePointEditor. Called when "Show" checkbox changes in either editor.
	 *
	 * @param item    The item whose checkbox state changed
	 * @param visible The new checkbox state
	 */
	public void syncVisibleInEditorCheckbox(Item item, boolean visible) {
		if (item == null)
			return;

		System.out.println("🔵 DEBUG syncVisibleInEditorCheckbox: Setting item '" + item.getName()
				+ "' visibleInEditor=" + visible);

		// Update the item itself
		item.setVisibleInEditor(visible);

		// CRITICAL: Update the item in the current scene as well!
		if (currentScene != null) {
			for (Item sceneItem : currentScene.getItems()) {
				if (sceneItem.getName().equals(item.getName())) {
					sceneItem.setVisibleInEditor(visible);
					System.out.println("🔵 DEBUG syncVisibleInEditorCheckbox: Updated scene item instance");
					break;
				}
			}
		}

		// Sync to EditorMainSimple
		if (editorWindowSimple != null) {
			editorWindowSimple.refreshShowCheckbox(item, visible);
		}

		// Sync to ScenePointEditor
		if (scenePointEditor != null) {
			scenePointEditor.refreshShowCheckbox(item, visible);
		}

		gamePanel.repaint();
	}

	public void createNewKeyArea(String name, String type) {
		if (currentScene == null)
			return;

		// Create at center of screen
		int x = 400;
		int y = 300;
		int width = 100;
		int height = 100;

		KeyArea.Type areaType;
		switch (type) {
		case "Transition":
			areaType = KeyArea.Type.TRANSITION;
			break;
		case "Movement_Bounds":
			areaType = KeyArea.Type.MOVEMENT_BOUNDS;
			break;
		case "Character_Range":
			areaType = KeyArea.Type.CHARACTER_RANGE;
			break;
		case "Interaction":
		default:
			areaType = KeyArea.Type.INTERACTION;
			break;
		}

		KeyArea newArea = new KeyArea(areaType, name, x, y, x + width, y + height);
		currentScene.addKeyArea(newArea);
		gamePanel.repaint();

		if (editorWindow != null) {
			editorWindow.log("Created KeyArea: " + name + " at (" + x + "," + y + ")");
		}
	}

	public void repaintGamePanel() {
		if (gamePanel != null) {
			gamePanel.repaint();
		}
	}

	private void initUI() {
		setLayout(new BorderLayout());

		// Game panel with background
		gamePanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				System.out.println("🎨🎨 paintComponent() CALLED - showPaths=" + showPaths + ", scenePointEditor="
						+ (scenePointEditor != null ? "REGISTERED" : "NULL"));

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Draw background image FIRST (bottom layer)
				if (backgroundImage != null) {
					g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
				}

				// Draw items (after background, before player)
				// Draw non-selected items first, then selected item on top
				if (currentScene != null) {
					Item selectedSceneItem = currentScene.getSelectedItem();

					// Draw all non-selected items first
					for (Item item : currentScene.getItems()) {
						if (item != selectedSceneItem && isItemVisibleInCurrentMode(item)) {
							// Load item image (with orientation-based or condition-based path)
							// Priority: Orientation image > Conditional image > Default image
							String imagePath = item.getOrientationImage();
							if (imagePath == null) {
								imagePath = item.getCurrentImagePath();
							}

							// Use ResourcePathHelper to resolve path (handles both full paths and
							// filenames)
							File imageFile = ResourcePathHelper.findImageFile(imagePath);
							if (imageFile == null) {
								// Fallback: try direct path
								imageFile = new File(imagePath);
							}

							if (imageFile != null && imageFile.exists()) {
								try {
									// Use ImageIO.read to avoid caching issues
									java.awt.image.BufferedImage buffered = javax.imageio.ImageIO.read(imageFile);
									Image img = buffered;
									Point pos = item.getPosition();

									// Use stored width/height from item
									int imgWidth = item.getWidth();
									int imgHeight = item.getHeight();

									// Calculate top-left corner
									int x = pos.x - imgWidth / 2;
									int y = pos.y - imgHeight / 2;

									// Draw image scaled to item size
									// In editor mode, draw with transparency so polygon points are visible
									if (showPaths) {
										Composite originalComposite = g2d.getComposite();
										g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
										g2d.drawImage(img, x, y, imgWidth, imgHeight, null);
										g2d.setComposite(originalComposite);
									} else {
										g2d.drawImage(img, x, y, imgWidth, imgHeight, null);
									}

									// Draw item boundary and drag points in editor mode
									if (showPaths) {
										g2d.setColor(Color.CYAN);
										g2d.setStroke(new BasicStroke(2));
										g2d.drawRect(x, y, imgWidth, imgHeight);
										g2d.drawString(item.getName(), x, y - 5);

										// Draw 4 corner drag points in GREEN
										int handleSize = 12;
										g2d.setColor(Color.GREEN);
										// Top-left
										g2d.fillRect(x - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
										// Top-right
										g2d.fillRect(x + imgWidth - handleSize / 2, y - handleSize / 2, handleSize,
												handleSize);
										// Bottom-left
										g2d.fillRect(x - handleSize / 2, y + imgHeight - handleSize / 2, handleSize,
												handleSize);
										// Bottom-right
										g2d.fillRect(x + imgWidth - handleSize / 2, y + imgHeight - handleSize / 2,
												handleSize, handleSize);
									}
								} catch (Exception e) {
									// Failed to load image
									System.err.println(
											"Failed to load item image: " + imagePath + " - " + e.getMessage());
								}
							} else {
								// Draw placeholder if image not found
								if (showPaths) {
									Point pos = item.getPosition();
									int imgWidth = item.getWidth();
									int imgHeight = item.getHeight();
									int x = pos.x - imgWidth / 2;
									int y = pos.y - imgHeight / 2;

									g2d.setColor(Color.RED);
									g2d.setStroke(new BasicStroke(2));
									g2d.drawRect(x, y, imgWidth, imgHeight);
									g2d.drawLine(x, y, x + imgWidth, y + imgHeight);
									g2d.drawLine(x + imgWidth, y, x, y + imgHeight);
									g2d.setColor(Color.WHITE);
									g2d.drawString(item.getName() + " (IMAGE NOT FOUND)", x, y - 5);
									g2d.drawString("Path: " + imagePath, x, y + imgHeight + 15);
								}
							}
						} else if (item != selectedSceneItem && showPaths && !item.isVisible()) {
							// Show invisible non-selected items in editor mode
							Point pos = item.getPosition();
							int imgWidth = item.getWidth();
							int imgHeight = item.getHeight();
							int x = pos.x - imgWidth / 2;
							int y = pos.y - imgHeight / 2;

							g2d.setColor(new Color(128, 128, 128, 100));
							g2d.fillRect(x, y, imgWidth, imgHeight);
							g2d.setColor(Color.GRAY);
							g2d.drawString(item.getName() + " (INVISIBLE)", x, y - 5);
						}
					}

					// Draw selected item last (on top of other items)
					if (selectedSceneItem != null && selectedSceneItem.isVisible()) {
						// Priority: Orientation image > Conditional image > Default image
						String imagePath = selectedSceneItem.getOrientationImage();
						if (imagePath == null) {
							imagePath = selectedSceneItem.getCurrentImagePath();
						}

						// Use ResourcePathHelper to resolve path (handles both full paths and
						// filenames)
						File imageFile = ResourcePathHelper.findImageFile(imagePath);
						if (imageFile == null) {
							// Fallback: try direct path
							imageFile = new File(imagePath);
						}

						if (imageFile != null && imageFile.exists()) {
							try {
								// Use ImageIO.read to avoid caching issues
								java.awt.image.BufferedImage buffered = javax.imageio.ImageIO.read(imageFile);
								Image img = buffered;
								Point pos = selectedSceneItem.getPosition();

								// Use stored width/height from item
								int imgWidth = selectedSceneItem.getWidth();
								int imgHeight = selectedSceneItem.getHeight();

								// Calculate top-left corner
								int x = pos.x - imgWidth / 2;
								int y = pos.y - imgHeight / 2;

								// Draw image scaled to item size
								// In editor mode, draw with transparency so polygon points are visible
								if (showPaths) {
									Composite originalComposite = g2d.getComposite();
									g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
									g2d.drawImage(img, x, y, imgWidth, imgHeight, null);
									g2d.setComposite(originalComposite);
								} else {
									g2d.drawImage(img, x, y, imgWidth, imgHeight, null);
								}

								// Draw item boundary and drag points in editor mode with ORANGE color for
								// selected
								if (showPaths) {
									g2d.setColor(new Color(255, 165, 0)); // Orange for selected
									g2d.setStroke(new BasicStroke(3)); // Thicker border
									g2d.drawRect(x, y, imgWidth, imgHeight);
									g2d.setFont(new Font("Arial", Font.BOLD, 12));
									g2d.drawString(selectedSceneItem.getName() + " [SELECTED]", x, y - 5);

									// Draw 4 corner drag points in ORANGE
									int handleSize = 14; // Bigger for selected
									g2d.setColor(new Color(255, 140, 0)); // Dark orange
									// Top-left
									g2d.fillRect(x - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
									// Top-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y - handleSize / 2, handleSize,
											handleSize);
									// Bottom-left
									g2d.fillRect(x - handleSize / 2, y + imgHeight - handleSize / 2, handleSize,
											handleSize);
									// Bottom-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y + imgHeight - handleSize / 2,
											handleSize, handleSize);
								}
							} catch (Exception e) {
								// Failed to load image
								System.err.println(
										"Failed to load selected item image: " + imagePath + " - " + e.getMessage());
							}
						} else {
							// Draw placeholder if image not found
							if (showPaths) {
								Point pos = selectedSceneItem.getPosition();
								int imgWidth = selectedSceneItem.getWidth();
								int imgHeight = selectedSceneItem.getHeight();
								int x = pos.x - imgWidth / 2;
								int y = pos.y - imgHeight / 2;

								g2d.setColor(new Color(255, 100, 0)); // Orange-red for selected missing image
								g2d.setStroke(new BasicStroke(3));
								g2d.drawRect(x, y, imgWidth, imgHeight);
								g2d.drawLine(x, y, x + imgWidth, y + imgHeight);
								g2d.drawLine(x + imgWidth, y, x, y + imgHeight);
								g2d.setColor(Color.WHITE);
								g2d.setFont(new Font("Arial", Font.BOLD, 12));
								g2d.drawString(selectedSceneItem.getName() + " [SELECTED] (IMAGE NOT FOUND)", x, y - 5);
								g2d.drawString("Path: " + imagePath, x, y + imgHeight + 15);
							}
						}
					}
				}

				// Draw player character (on top of items)
				g.setColor(Color.RED);
				g.fillOval(playerPosition.x - 10, playerPosition.y - 10, 20, 20);

				// Draw editor visualizations ON TOP
				if (showPaths && currentScene != null) {
					// DEBUG: Log rendering start
					if (scenePointEditor != null) {
						System.out.println(
								"🎨 RENDERING: showPaths=" + showPaths + ", currentScene=" + currentScene.getName());
					}
					// Draw Item click area polygons OVER images
					// This allows dragging polygon points without moving the item
					g2d.setColor(new Color(0, 255, 255, 100)); // Cyan for items
					g2d.setStroke(new BasicStroke(2));
					Item selectedSceneItem = currentScene.getSelectedItem();
					for (Item item : currentScene.getItems()) {
						if (isItemVisibleInCurrentMode(item)) {
							Polygon poly = item.getClickAreaPolygon();
							if (poly != null && poly.npoints > 0) {
								// Highlight selected item with thicker orange border
								boolean isSelected = (item == selectedSceneItem);
								if (isSelected) {
									g2d.setColor(new Color(255, 165, 0)); // Orange for selected
									g2d.setStroke(new BasicStroke(4)); // Thicker stroke
								} else {
									g2d.setColor(new Color(0, 255, 255, 100)); // Cyan for normal
									g2d.setStroke(new BasicStroke(2));
								}
								g2d.drawPolygon(poly);

								// Fill semi-transparent
								if (isSelected) {
									g2d.setColor(new Color(255, 165, 0, 50)); // Orange fill for selected
								} else {
									g2d.setColor(new Color(0, 255, 255, 30));
								}
								g2d.fillPolygon(poly);

								// Draw name at centroid
								Rectangle bounds = poly.getBounds();
								if (isSelected) {
									g2d.setColor(new Color(255, 140, 0)); // Dark orange for selected
									g2d.setFont(new Font("Arial", Font.BOLD, 12));
								} else {
									g2d.setColor(Color.CYAN);
									g2d.setFont(new Font("Arial", Font.PLAIN, 11));
								}
								g2d.drawString("ITEM: " + item.getName(), bounds.x + 5, bounds.y + 15);

								// Draw points OVER everything
								if (isSelected) {
									g2d.setColor(new Color(255, 140, 0)); // Orange points for selected
								} else {
									g2d.setColor(new Color(255, 255, 0)); // Yellow points
								}
								int handleSize = isSelected ? 12 : 10; // Bigger points for selected
								List<Point> points = item.getClickAreaPoints();
								for (int i = 0; i < points.size(); i++) {
									Point p = points.get(i);

									// Check if this is the highlighted point
									boolean isHighlighted = (item == highlightedPointItem
											&& "CustomClickArea".equals(highlightedPointType)
											&& i == highlightedPointIndex);

									if (isHighlighted) {
										// Draw highlighted point larger and in bright red
										g2d.setColor(Color.RED);
										int highlightSize = 12;
										g2d.fillRect(p.x - highlightSize / 2, p.y - highlightSize / 2, highlightSize,
												highlightSize);
										// Draw white border around highlighted point
										g2d.setColor(Color.WHITE);
										g2d.setStroke(new BasicStroke(2));
										g2d.drawRect(p.x - highlightSize / 2, p.y - highlightSize / 2, highlightSize,
												highlightSize);
										g2d.setStroke(new BasicStroke(2));
									} else {
										g2d.fillRect(p.x - handleSize / 2, p.y - handleSize / 2, handleSize,
												handleSize);
									}

									// Draw point index
									g2d.setColor(Color.WHITE);
									g2d.drawString(String.valueOf(i), p.x + 5, p.y - 5);
									if (isSelected) {
										g2d.setColor(new Color(255, 140, 0));
									} else {
										g2d.setColor(new Color(255, 255, 0));
									}
								}

								g2d.setColor(new Color(0, 255, 255, 100));
								g2d.setStroke(new BasicStroke(2));
							}
						}
					}

					// Draw NEW CustomClickArea objects from items
					for (Item item : currentScene.getItems()) {
						if (isItemVisibleInCurrentMode(item)) {
							boolean isSelected = (item == selectedSceneItem);
							List<CustomClickArea> customAreas = item.getCustomClickAreas();

							if (scenePointEditor != null && !customAreas.isEmpty()) {
								System.out.println("🟣 Rendering CustomClickArea for item: " + item.getName() + " - "
										+ customAreas.size() + " areas, " + customAreas.get(0).getPoints().size()
										+ " points");
							}

							for (CustomClickArea area : customAreas) {
								// Only show if conditions are met
								if (!area.shouldBeActive(null)) {
									continue;
								}

								// Get polygon from area
								area.updatePolygon();
								Polygon poly = area.getPolygon();

								if (poly != null && poly.npoints > 0) {
									// Draw polygon border
									if (isSelected) {
										g2d.setColor(new Color(255, 0, 255)); // Magenta for selected item's areas
										g2d.setStroke(new BasicStroke(3));
									} else {
										g2d.setColor(new Color(255, 0, 255, 150)); // Magenta for normal
										g2d.setStroke(new BasicStroke(2));
									}
									g2d.drawPolygon(poly);

									// Fill semi-transparent
									g2d.setColor(new Color(255, 0, 255, 30)); // Magenta fill
									g2d.fillPolygon(poly);

									// Draw hover text at centroid
									Rectangle bounds = poly.getBounds();
									g2d.setColor(new Color(255, 0, 255));
									g2d.setFont(new Font("Arial", Font.PLAIN, 10));
									String hoverText = area.getHoverText();
									if (hoverText != null && !hoverText.isEmpty()) {
										g2d.drawString("HOVER: " + hoverText, bounds.x + 5, bounds.y + 12);
									}

									// Draw points
									g2d.setColor(new Color(255, 0, 255)); // Magenta points
									int handleSize = isSelected ? 12 : 10;
									List<Point> points = area.getPoints();
									for (int i = 0; i < points.size(); i++) {
										Point p = points.get(i);

										// Check if this is the highlighted point
										boolean isHighlighted = (item == highlightedPointItem
												&& "CustomClickArea".equals(highlightedPointType)
												&& i == highlightedPointIndex);

										if (isHighlighted) {
											// Draw highlighted point larger and in bright yellow
											g2d.setColor(Color.YELLOW);
											int highlightSize = 18;
											g2d.fillOval(p.x - highlightSize / 2, p.y - highlightSize / 2,
													highlightSize, highlightSize);
											// Draw white border around highlighted point
											g2d.setColor(Color.WHITE);
											g2d.setStroke(new BasicStroke(2));
											g2d.drawOval(p.x - highlightSize / 2, p.y - highlightSize / 2,
													highlightSize, highlightSize);
											g2d.setStroke(new BasicStroke(2));
											g2d.setColor(new Color(255, 0, 255));
										} else {
											g2d.fillOval(p.x - handleSize / 2, p.y - handleSize / 2, handleSize,
													handleSize);
										}

										// Draw point index
										g2d.setColor(Color.WHITE);
										g2d.setFont(new Font("Arial", Font.PLAIN, 9));
										g2d.drawString(String.valueOf(i), p.x + 5, p.y - 5);
										g2d.setColor(new Color(255, 0, 255));
									}
								}
							}
						}
					}

					// Draw MovingRange points from items (GREEN - according to schema)
					for (Item item : currentScene.getItems()) {
						if (isItemVisibleInCurrentMode(item)) {
							boolean isSelected = (item == selectedSceneItem);
							List<MovingRange> movingRanges = item.getMovingRanges();

							if (movingRanges != null && !movingRanges.isEmpty() && scenePointEditor != null) {
								System.out.println("🟢 Rendering MovingRange for item: " + item.getName() + " - "
										+ movingRanges.size() + " ranges, " + movingRanges.get(0).getPoints().size()
										+ " points");
							}

							if (movingRanges != null) {
								for (MovingRange range : movingRanges) {
									// Only show if conditions are met
									if (!range.shouldBeActive(null)) {
										continue;
									}

									// Get polygon from range
									range.updatePolygon();
									Polygon poly = range.getPolygon();

									if (poly != null && poly.npoints > 0) {
										// Draw polygon border
										if (isSelected) {
											g2d.setColor(new Color(0, 200, 0)); // Bright green for selected
											g2d.setStroke(new BasicStroke(3));
										} else {
											g2d.setColor(new Color(0, 255, 0, 150)); // Green for normal
											g2d.setStroke(new BasicStroke(2));
										}
										g2d.drawPolygon(poly);

										// Fill semi-transparent
										g2d.setColor(new Color(0, 255, 0, 30)); // Green fill
										g2d.fillPolygon(poly);

										// Draw label at centroid
										Rectangle bounds = poly.getBounds();
										g2d.setColor(new Color(0, 200, 0));
										g2d.setFont(new Font("Arial", Font.PLAIN, 10));
										g2d.drawString("MOVING RANGE: " + item.getName(), bounds.x + 5, bounds.y + 12);

										// Draw points (GREEN - 'm' in schema)
										g2d.setColor(new Color(0, 255, 0)); // Green points
										int handleSize = isSelected ? 12 : 10;
										List<Point> points = range.getPoints();
										for (int i = 0; i < points.size(); i++) {
											Point p = points.get(i);

											// Check if this is the highlighted point
											boolean isHighlighted = (item == highlightedPointItem
													&& "MovingRange".equals(highlightedPointType)
													&& i == highlightedPointIndex);

											if (isHighlighted) {
												// Draw highlighted point larger and in bright yellow
												g2d.setColor(Color.YELLOW);
												int highlightSize = 18;
												g2d.fillRect(p.x - highlightSize / 2, p.y - highlightSize / 2,
														highlightSize, highlightSize);
												// Draw white border around highlighted point
												g2d.setColor(Color.WHITE);
												g2d.setStroke(new BasicStroke(2));
												g2d.drawRect(p.x - highlightSize / 2, p.y - highlightSize / 2,
														highlightSize, highlightSize);
												g2d.setStroke(new BasicStroke(2));
												g2d.setColor(new Color(0, 255, 0));
											} else {
												g2d.fillRect(p.x - handleSize / 2, p.y - handleSize / 2, handleSize,
														handleSize);
											}

											// Draw point index
											g2d.setColor(Color.WHITE);
											g2d.setFont(new Font("Arial", Font.PLAIN, 9));
											g2d.drawString(String.valueOf(i), p.x + 5, p.y - 5);
											g2d.setColor(new Color(0, 255, 0));
										}
									}
								}
							}
						}
					}

					// Draw Path points from items (RED - according to schema)
					for (Item item : currentScene.getItems()) {
						if (isItemVisibleInCurrentMode(item)) {
							boolean isSelected = (item == selectedSceneItem);
							List<Path> paths = item.getPaths();

							if (paths != null && !paths.isEmpty() && scenePointEditor != null) {
								System.out.println("🔴 Rendering Path for item: " + item.getName() + " - "
										+ paths.size() + " paths, " + paths.get(0).getPoints().size() + " points");
							}

							if (paths != null) {
								for (Path path : paths) {
									// Only show if conditions are met
									if (!path.shouldBeActive(null)) {
										continue;
									}

									// Get polygon from path (same as MovingRange)
									path.updatePolygon();
									Polygon poly = path.getPolygon();

									if (poly != null && poly.npoints > 0) {
										// Draw polygon border
										if (isSelected) {
											g2d.setColor(new Color(200, 0, 0)); // Bright red for selected
											g2d.setStroke(new BasicStroke(3));
										} else {
											g2d.setColor(new Color(255, 0, 0, 150)); // Red for normal
											g2d.setStroke(new BasicStroke(2));
										}
										g2d.drawPolygon(poly);

										// Fill semi-transparent
										g2d.setColor(new Color(255, 0, 0, 30)); // Red fill
										g2d.fillPolygon(poly);

										// Draw label at centroid
										Rectangle bounds = poly.getBounds();
										g2d.setColor(new Color(200, 0, 0));
										g2d.setFont(new Font("Arial", Font.PLAIN, 10));
										g2d.drawString("PATH: " + item.getName(), bounds.x + 5, bounds.y + 12);

										// Draw path points (RED - 'p' in schema)
										g2d.setColor(new Color(255, 0, 0)); // Red points
										int handleSize = isSelected ? 12 : 10;
										List<Point> points = path.getPoints();
										for (int i = 0; i < points.size(); i++) {
											Point p = points.get(i);

											// Check if this is the highlighted point
											boolean isHighlighted = (item == highlightedPointItem
													&& "Path".equals(highlightedPointType)
													&& i == highlightedPointIndex);

											if (isHighlighted) {
												// Draw highlighted point larger and in bright yellow
												g2d.setColor(Color.YELLOW);
												int highlightSize = 18;
												g2d.fillOval(p.x - highlightSize / 2, p.y - highlightSize / 2,
														highlightSize, highlightSize);
												// Draw white border around highlighted point
												g2d.setColor(Color.WHITE);
												g2d.setStroke(new BasicStroke(2));
												g2d.drawOval(p.x - highlightSize / 2, p.y - highlightSize / 2,
														highlightSize, highlightSize);
												g2d.setStroke(new BasicStroke(2));
												g2d.setColor(new Color(255, 0, 0));
											} else {
												g2d.fillOval(p.x - handleSize / 2, p.y - handleSize / 2, handleSize,
														handleSize);
											}

											// Draw point index
											g2d.setColor(Color.WHITE);
											g2d.setFont(new Font("Arial", Font.PLAIN, 9));
											g2d.drawString(String.valueOf(i), p.x + 5, p.y - 5);
											g2d.setColor(new Color(255, 0, 0));
										}
									}
								}
							}
						}
					}

					// Draw KeyArea polygons
					g2d.setColor(new Color(0, 255, 0, 100));
					g2d.setStroke(new BasicStroke(2));
					for (KeyArea area : currentScene.getKeyAreas()) {
						Polygon poly = area.getPolygon();
						if (poly != null && poly.npoints > 0) {
							g2d.drawPolygon(poly);

							// Fill semi-transparent
							g2d.setColor(new Color(0, 255, 0, 30));
							g2d.fillPolygon(poly);

							// Draw name at centroid
							Rectangle bounds = poly.getBounds();
							g2d.setColor(Color.GREEN);
							g2d.drawString(area.getName(), bounds.x + 5, bounds.y + 15);

							// Draw points
							g2d.setColor(Color.YELLOW);
							int handleSize = 12;
							for (Point p : area.getPoints()) {
								g2d.fillRect(p.x - handleSize / 2, p.y - handleSize / 2, handleSize, handleSize);
								// Draw point index
								g2d.setColor(Color.WHITE);
								g2d.drawString(String.valueOf(area.getPoints().indexOf(p)), p.x + 5, p.y - 5);
								g2d.setColor(Color.YELLOW);
							}

							g2d.setColor(new Color(0, 255, 0, 100));
						}
					}

					// Draw paths
					g2d.setColor(new Color(255, 0, 255, 150));
					g2d.setStroke(new BasicStroke(3));
					for (Path path : currentScene.getPaths()) {
						List<Point> points = path.getPoints();
						for (int i = 0; i < points.size() - 1; i++) {
							Point p1 = points.get(i);
							Point p2 = points.get(i + 1);
							g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
						}

						// Draw path points
						g2d.setColor(Color.MAGENTA);
						for (int i = 0; i < points.size(); i++) {
							Point p = points.get(i);
							g2d.fillOval(p.x - 5, p.y - 5, 10, 10);
							// Draw point index
							g2d.setColor(Color.WHITE);
							g2d.drawString(String.valueOf(i), p.x + 8, p.y - 5);
							g2d.setColor(Color.MAGENTA);
						}
					}
				}
			}
		};
		gamePanel.setLayout(new BorderLayout());
		gamePanel.setPreferredSize(new Dimension(1024, 668));

		// Hover text label (top of game panel)
		hoverTextLabel = new JLabel(" ");
		hoverTextLabel.setFont(new Font("Arial", Font.BOLD, 16));
		hoverTextLabel.setForeground(Color.WHITE);
		hoverTextLabel.setBackground(new Color(0, 0, 0, 180));
		hoverTextLabel.setOpaque(true);
		hoverTextLabel.setHorizontalAlignment(SwingConstants.CENTER);
		hoverTextLabel.setPreferredSize(new Dimension(1024, 30));
		gamePanel.add(hoverTextLabel, BorderLayout.NORTH);

		// Mouse listener for clicking on screen
		gamePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handleGamePanelClick(e.getPoint());
				// Request focus so keyboard shortcuts work
				gamePanel.requestFocusInWindow();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// Request focus so keyboard shortcuts work
				gamePanel.requestFocusInWindow();

				if (showPaths) {
					// Check for path points FIRST (higher priority for precise clicking)
					handlePathPointPress(e.getPoint());

					// If no path point selected, check for item click
					if (selectedPathPoint == null) {
						handleItemPress(e.getPoint());
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (showPaths && selectedPathPoint != null) {
					handlePathPointRelease();
				}

				// Handle item drag release
				if (showPaths && draggedItem != null) {
					handleItemRelease();
				}
			}
		});

		// Mouse motion listener for hover text and dragging
		gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				handleMouseHover(e.getPoint());

				// Update orientation images for items following mouse or following on click
				// IMPORTANT: isFollowingMouse only changes orientation images, NOT position!
				// Position changes only happen with isFollowingOnMouseClick via
				// handleCharacterMovement
				if (currentScene != null) {
					boolean foundFollowingItem = false;
					for (Item item : currentScene.getItems()) {
						// DEBUG: Check if any item has following flags set
						if (item.isFollowingMouse() || item.isFollowingOnMouseClick()) {
							System.out.println("🔵 DEBUG mouseMoved: Item '" + item.getName() + "' isFollowingMouse="
									+ item.isFollowingMouse() + ", isFollowingOnMouseClick="
									+ item.isFollowingOnMouseClick());
							item.updateOrientationBasedOnCursor(e.getX(), e.getY());
							foundFollowingItem = true;
						}
					}
					if (foundFollowingItem) {
						gamePanel.repaint();
					}
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (showPaths && selectedPathPoint != null) {
					handlePathPointDrag(e.getPoint());
				}

				// Handle item dragging
				if (showPaths && draggedItem != null) {
					handleItemDrag(e.getPoint());
				}
			}
		});

		// Key listener for point editing shortcuts (+ and DELETE)
		gamePanel.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				System.out.println("KEY PRESSED: keyCode=" + e.getKeyCode() + ", char=" + e.getKeyChar() + ", focus="
						+ gamePanel.isFocusOwner());

				// Only handle when an editor is open and a point is selected
				if (editorWindow == null && editorWindowSimple == null) {
					return;
				}

				// Check if a point is selected
				if (selectedPathPoint == null || selectedItemForPointDrag == null) {
					System.out.println("  -> No point selected");
					return;
				}

				// Determine point type
				String pointType = null;
				if (selectedCustomClickAreaForPointDrag != null) {
					pointType = "CustomClickArea";
				} else if (selectedMovingRangeForPointDrag != null) {
					pointType = "MovingRange";
				} else if (selectedPathForPointDrag != null) {
					pointType = "Path";
				}

				if (pointType == null) {
					System.out.println("  -> No point type");
					return;
				}

				System.out.println("  -> pointType=" + pointType);

				// Handle + key (add point) - support both numpad + and regular +
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ADD
						|| e.getKeyCode() == java.awt.event.KeyEvent.VK_PLUS || e.getKeyChar() == '+') {
					System.out.println("  -> + key handled!");
					e.consume();
					handlePointInsertShortcut(pointType);
				}
				// Handle DELETE key (remove point)
				else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
					System.out.println("  -> DELETE key handled!");
					e.consume();
					handlePointDeleteShortcut(pointType);
				}
			}
		});

		// Setup Drag & Drop for images
		gamePanel.setTransferHandler(new TransferHandler() {
			@Override
			public boolean canImport(TransferSupport support) {
				// Accept file drops when ANY editor is visible (EditorMain or EditorMainSimple)
				boolean editorVisible = (editorWindow != null && editorWindow.isVisible())
						|| (editorWindowSimple != null && editorWindowSimple.isVisible());
				if (!editorVisible) {
					return false;
				}
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}

			@Override
			public boolean importData(TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}

				try {
					Transferable transferable = support.getTransferable();
					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

					if (files.isEmpty()) {
						return false;
					}

					File imageFile = files.get(0);
					String fileName = imageFile.getName().toLowerCase();

					// Check if it's an image file (including SVG)
					if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
							|| fileName.endsWith(".gif") || fileName.endsWith(".svg")) {

						// Get drop location
						Point dropPoint = support.getDropLocation().getDropPoint();

						// Create new item from dropped image
						handleImageDrop(imageFile, dropPoint);
						return true;
					}
				} catch (Exception e) {
					logToActiveEditor("ERROR dropping file: " + e.getMessage());
					e.printStackTrace();
				}
				return false;
			}
		});

		// Make gamePanel focusable for keyboard shortcuts (+ and DELETE)
		gamePanel.setFocusable(true);
		gamePanel.requestFocusInWindow();

		add(gamePanel, BorderLayout.CENTER);

		// Menu panel - Modern with theme support
		menuPanel = new JPanel();
		menuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, Spacing.SM, Spacing.SM));
		menuPanel.setPreferredSize(new Dimension(1024, 80)); // Slightly reduced
		menuPanel.setBackground(ThemeManager.colors().getBackgroundElevated());
		menuPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.colors().getBorderDefault()),
				BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)));

		// Load buttons dynamically from resources/buttons/buttons.txt via
		// ButtonsDataManager
		for (String action : ButtonsDataManager.getButtonNames()) {
			AppButton btn = new AppButton(action, AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
			btn.addActionListener(e -> selectAction(action));
			menuPanel.add(btn);
		}

		// [Load] button - Load saved game
		AppButton loadBtn = new AppButton("Load", AppButton.Variant.PRIMARY, AppButton.Size.SMALL);
		loadBtn.setToolTipText("Load saved game from progress files");
		loadBtn.addActionListener(e -> {
			progress.loadProgress();
			switchToGamingMode(); // Reload everything from progress
			System.out.println("✓ Game loaded from progress files");
		});
		menuPanel.add(loadBtn);

		// [Save] button - Save game to progress
		JButton saveBtn = new JButton("Save");
		saveBtn.setToolTipText("Save current game to progress files");
		saveBtn.addActionListener(e -> {
			progress.saveProgress();
			System.out.println("✓ Game saved to progress files");
		});
		menuPanel.add(saveBtn);

		JButton resetBtn = new JButton("Reset");
		resetBtn.setToolTipText("Reset game to default state");
		resetBtn.addActionListener(e -> resetGame());
		menuPanel.add(resetBtn);

		// Inventory panel
		inventoryPanel = new JPanel();
		inventoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, Spacing.SM, Spacing.SM));
		inventoryPanel.setPreferredSize(new Dimension(1024, 100)); // Compacted
		inventoryPanel.setBackground(ThemeManager.colors().getBackgroundRoot());
		inventoryPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(ThemeManager.colors().getBorderDefault(), 1), "Inventar",
				javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
				ThemeManager.typography().semiboldBase(), ThemeManager.colors().getTextPrimary()));

		updateInventory();

		// Theme toggle button for game window
		AppButton themeToggleBtn = new AppButton("🌓", AppButton.Variant.GHOST, AppButton.Size.SMALL);
		themeToggleBtn.setToolTipText("Toggle Light/Dark Theme");
		themeToggleBtn.addActionListener(e -> {
			ThemeManager.toggleTheme();
			ThemeManager.updateAllWindows();
			// Re-apply inventory panel styling after theme change
			inventoryPanel.setBackground(ThemeManager.colors().getBackgroundRoot());
			inventoryPanel.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createLineBorder(ThemeManager.colors().getBorderDefault(), 1), "Inventar",
					javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
					ThemeManager.typography().semiboldBase(), ThemeManager.colors().getTextPrimary()));
			updateInventory(); // Refresh inventory to update colors
		});
		menuPanel.add(themeToggleBtn);

		// Combined south panel
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(inventoryPanel, BorderLayout.NORTH);
		southPanel.add(menuPanel, BorderLayout.SOUTH);

		add(southPanel, BorderLayout.SOUTH);

		// === DEBUG SIDEBAR (Right side, hideable) ===
		// Create a split pane with game panel in center and debug sidebar on right
		JComponent currentCenter = gamePanel;

		// Create split pane for game panel (center) + debug sidebar (right)
		final javax.swing.JSplitPane mainSplitPane = new javax.swing.JSplitPane(
				javax.swing.JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setResizeWeight(1.0); // Give all extra space to game panel
		mainSplitPane.setDividerSize(Spacing.RADIUS_SM);

		// Add game panel to left/center
		remove(gamePanel);
		mainSplitPane.setLeftComponent(currentCenter);

		// Add debug sidebar to right
		mainSplitPane.setRightComponent(debugWindow);
		debugWindow.setPreferredSize(new Dimension(0, 0)); // Initially collapsed
		mainSplitPane.setDividerLocation(1.0); // Collapse sidebar by default

		// Add split pane to center (replacing game panel)
		add(mainSplitPane, BorderLayout.CENTER);

		// Add hotkey to toggle debug sidebar
		javax.swing.KeyStroke altD = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_D,
				java.awt.event.KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altD, "toggleDebugSidebar");
		getRootPane().getActionMap().put("toggleDebugSidebar", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleDebugSidebar();
			}
		});
	}

	/**
	 * Toggle the debug sidebar visibility (expand/collapse)
	 */
	private void toggleDebugSidebar() {
		for (Component c : getComponents()) {
			if (c instanceof javax.swing.JSplitPane) {
				javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) c;
				if (splitPane.getRightComponent() == debugWindow) {
					boolean isExpanded = splitPane.getDividerLocation() < splitPane.getWidth() - 50;

					if (isExpanded) {
						// Collapse sidebar
						splitPane.setDividerLocation(1.0);
						debugWindow.setPreferredSize(new Dimension(0, 0));
					} else {
						// Expand sidebar
						splitPane.setDividerLocation(0.75); // Sidebar gets 25% width
						debugWindow.setPreferredSize(new Dimension(300, 0));
					}
					splitPane.revalidate();
					splitPane.repaint();
					break;
				}
			}
		}
	}

	/**
	 * Show the debug sidebar (expanded)
	 */
	public void showDebugSidebar() {
		for (Component c : getComponents()) {
			if (c instanceof javax.swing.JSplitPane) {
				javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) c;
				if (splitPane.getRightComponent() == debugWindow) {
					splitPane.setDividerLocation(0.75);
					debugWindow.setPreferredSize(new Dimension(300, 0));
					splitPane.revalidate();
					splitPane.repaint();
					break;
				}
			}
		}
	}

	/**
	 * Hide the debug sidebar (collapsed)
	 */
	public void hideDebugSidebar() {
		for (Component c : getComponents()) {
			if (c instanceof javax.swing.JSplitPane) {
				javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) c;
				if (splitPane.getRightComponent() == debugWindow) {
					splitPane.setDividerLocation(1.0);
					debugWindow.setPreferredSize(new Dimension(0, 0));
					splitPane.revalidate();
					splitPane.repaint();
					break;
				}
			}
		}
	}

	private void handleMouseHover(Point point) {
		if (currentScene == null) {
			hoverTextLabel.setText(" ");
			hoveredKeyArea = null;
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			return;
		}

		// Build dynamic hover text
		String dynamicHoverText = null;

		// Check if hovering over an Item first
		Item hoveredItem = currentScene.getItemAt(point);
		if (hoveredItem != null && hoveredItem.isVisible()) {
			// Check if item-as-cursor mode is active
			if (selectedInventoryItem != null && selectedAction != null) {
				// Dynamic hover: "{action} {item} mit {target}"
				dynamicHoverText = selectedAction + " " + selectedInventoryItem.getName() + " mit "
						+ hoveredItem.getName();
			} else if (selectedAction != null) {
				// Action selected but no item: "{action} {target}"
				dynamicHoverText = selectedAction + " " + hoveredItem.getName();
			} else {
				// No action selected, use item's display text
				// NEW: Use getHoverDisplayText(Point) to check CustomClickAreas with conditions
				String displayText = hoveredItem.getHoverDisplayText(point);
				if (displayText != null && !displayText.isEmpty()) {
					dynamicHoverText = displayText;
				} else {
					dynamicHoverText = hoveredItem.getName();
				}
			}

			hoverTextLabel.setText(dynamicHoverText);
			hoveredKeyArea = null;
			return;
		}

		// Check if hovering over a KeyArea
		KeyArea hoveredArea = currentScene.getKeyAreaAt(point);

		// Update hovered area for cursor blinking
		if (hoveredArea != hoveredKeyArea) {
			hoveredKeyArea = hoveredArea;
			cursorVisible = true; // Reset blink state
			if (hoveredArea == null) {
				// Reset cursor only if not in item-as-cursor mode
				if (selectedInventoryItem == null) {
					gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}
		}

		if (hoveredArea != null) {
			// Check if item-as-cursor mode is active
			if (selectedInventoryItem != null && selectedAction != null) {
				// Dynamic hover: "{action} {item} mit {target}"
				dynamicHoverText = selectedAction + " " + selectedInventoryItem.getName() + " mit "
						+ hoveredArea.getName();
			} else if (selectedAction != null) {
				// Action selected but no item: "{action} {target}"
				dynamicHoverText = selectedAction + " " + hoveredArea.getName();
			} else {
				// No action selected, use area's display text
				String displayText = hoveredArea.getHoverDisplayText();
				if (displayText != null && !displayText.isEmpty()) {
					dynamicHoverText = displayText;
				}
			}

			if (dynamicHoverText != null && !dynamicHoverText.isEmpty()) {
				hoverTextLabel.setText(dynamicHoverText);
			} else {
				hoverTextLabel.setText(" ");
			}
		} else {
			hoverTextLabel.setText(" ");
		}
	}

	private void handlePathPointPress(Point clickPoint) {
		if (currentScene == null)
			return;

		// Check if clicking on CustomClickArea points first (highest priority)
		for (Item item : currentScene.getItems()) {
			if (!isItemVisibleInCurrentMode(item))
				continue;

			List<CustomClickArea> customAreas = item.getCustomClickAreas();
			for (CustomClickArea area : customAreas) {
				// Only allow dragging if area is active
				if (!area.shouldBeActive(null))
					continue;

				List<Point> points = area.getPoints();
				for (int i = 0; i < points.size(); i++) {
					Point p = points.get(i);
					if (p.distance(clickPoint) < 15) {
						// Store point info
						selectedPathPoint = p;
						selectedPathPointIndex = i;
						selectedItemForPointDrag = item;
						selectedCustomClickAreaForPointDrag = area;
						pointWasDragged = false;

						// Set highlighted point IMMEDIATELY for visual feedback
						setHighlightedPoint(item, "CustomClickArea", i);

						// IMPORTANT: Return to prevent sprite/item from reacting
						return;
					}
				}
			}
		}

		// Check if clicking on Item click area points (old system)
		for (Item item : currentScene.getItems()) {
			if (!isItemVisibleInCurrentMode(item))
				continue;

			List<Point> points = item.getClickAreaPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 15) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = item;
					selectedCustomClickAreaForPointDrag = null;

					// Auto-select in editor window
					if (editorWindow != null) {
						editorWindow.selectItem(item);
						editorWindow.log("Selected Item point " + i + " from " + item.getName() + " at (" + p.x + ","
								+ p.y + ")");
					}
					return;
				}
			}
		}

		// Check if clicking on MovingRange points from Items
		for (Item item : currentScene.getItems()) {
			if (!isItemVisibleInCurrentMode(item) || !item.isMovingRangeVisibleInEditor())
				continue;

			List<MovingRange> movingRanges = item.getMovingRanges();
			if (movingRanges != null) {
				for (MovingRange range : movingRanges) {
					if (!range.shouldBeActive(null))
						continue;

					List<Point> points = range.getPoints();
					for (int i = 0; i < points.size(); i++) {
						Point p = points.get(i);
						if (p.distance(clickPoint) < 15) {
							selectedPathPoint = p;
							selectedPathPointIndex = i;
							selectedItemForPointDrag = item;
							selectedCustomClickAreaForPointDrag = null;
							selectedMovingRangeForPointDrag = range;
							selectedPathForPointDrag = null;
							pointWasDragged = false;

							// Set highlighted point IMMEDIATELY for visual feedback
							setHighlightedPoint(item, "MovingRange", i);

							// IMPORTANT: Return to prevent sprite/item from reacting
							return;
						}
					}
				}
			}
		}

		// Check if clicking on Path points from Items
		for (Item item : currentScene.getItems()) {
			if (!isItemVisibleInCurrentMode(item) || !item.isPathVisibleInEditor())
				continue;

			List<Path> paths = item.getPaths();
			if (paths != null) {
				for (Path path : paths) {
					if (!path.shouldBeActive(null))
						continue;

					List<Point> points = path.getPoints();
					for (int i = 0; i < points.size(); i++) {
						Point p = points.get(i);
						if (p.distance(clickPoint) < 15) {
							selectedPathPoint = p;
							selectedPathPointIndex = i;
							selectedItemForPointDrag = item;
							selectedCustomClickAreaForPointDrag = null;
							selectedMovingRangeForPointDrag = null;
							selectedPathForPointDrag = path;
							pointWasDragged = false;

							// Set highlighted point IMMEDIATELY for visual feedback
							setHighlightedPoint(item, "Path", i);

							// IMPORTANT: Return to prevent sprite/item from reacting
							return;
						}
					}
				}
			}
		}

		// Check if clicking on KeyArea points
		for (KeyArea area : currentScene.getKeyAreas()) {
			List<Point> points = area.getPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 15) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = null;
					selectedCustomClickAreaForPointDrag = null;
					selectedMovingRangeForPointDrag = null;
					selectedPathForPointDrag = null;

					// Auto-select in editor window
					if (editorWindow != null) {
						editorWindow.selectKeyArea(area);
//						editorWindow.selectPoint(i);
						editorWindow.log("Selected KeyArea point " + i + " from " + area.getName() + " at (" + p.x + ","
								+ p.y + ")");
					}
					return;
				}
			}
		}

		// Check if clicking on Path points (scene-level, not item-level)
		for (Path path : currentScene.getPaths()) {
			List<Point> points = path.getPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 15) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = null;
					selectedCustomClickAreaForPointDrag = null;
					selectedMovingRangeForPointDrag = null;
					selectedPathForPointDrag = null;
					if (editorWindow != null) {
						editorWindow.log("Selected scene path point " + i + " at (" + p.x + "," + p.y + ")");
					}
					return;
				}
			}
		}
	}

	private void handlePathPointDrag(Point dragPoint) {
		if (selectedPathPoint != null) {
			System.out.println("DRAG: Before=(" + selectedPathPoint.x + "," + selectedPathPoint.y + ") To=("
					+ dragPoint.x + "," + dragPoint.y + ")");

			// Only mark as dragged if position actually changed
			if (selectedPathPoint.x != dragPoint.x || selectedPathPoint.y != dragPoint.y) {
				selectedPathPoint.x = dragPoint.x;
				selectedPathPoint.y = dragPoint.y;
				pointWasDragged = true;
				System.out.println("DRAG: Position changed, pointWasDragged=true");
			} else {
				System.out.println("DRAG: Position same, pointWasDragged=" + pointWasDragged);
			}

			// Update CustomClickArea polygon if dragging CustomClickArea point
			if (selectedCustomClickAreaForPointDrag != null) {
				selectedCustomClickAreaForPointDrag.updatePolygon();

				String logMsg = "🔵 Dragging CustomClickArea point " + selectedPathPointIndex + " of Item '"
						+ (selectedItemForPointDrag != null ? selectedItemForPointDrag.getName() : "?") + "' to ("
						+ dragPoint.x + "," + dragPoint.y + ")";
				if (selectedItemForPointDrag != null) {
					logMsg += " -> resources/items/" + selectedItemForPointDrag.getName() + ".txt";
				}

				if (editorWindow != null) {
					editorWindow.log(logMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
				}
			}
			// Update MovingRange polygon if dragging MovingRange point
			else if (selectedMovingRangeForPointDrag != null) {
				selectedMovingRangeForPointDrag.updatePolygon();

				String logMsg = "🟢 Dragging MovingRange point " + selectedPathPointIndex + " of Item '"
						+ (selectedItemForPointDrag != null ? selectedItemForPointDrag.getName() : "?") + "' to ("
						+ dragPoint.x + "," + dragPoint.y + ")";
				if (selectedItemForPointDrag != null) {
					logMsg += " -> resources/items/" + selectedItemForPointDrag.getName() + ".txt";
				}

				if (editorWindow != null) {
					editorWindow.log(logMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
				}
			}
			// Update Path if dragging Path point (no polygon, just point list)
			else if (selectedPathForPointDrag != null) {
				String logMsg = "🔴 Dragging Path point " + selectedPathPointIndex + " of Item '"
						+ (selectedItemForPointDrag != null ? selectedItemForPointDrag.getName() : "?") + "' to ("
						+ dragPoint.x + "," + dragPoint.y + ")";
				if (selectedItemForPointDrag != null) {
					logMsg += " -> resources/items/" + selectedItemForPointDrag.getName() + ".txt";
				}

				if (editorWindow != null) {
					editorWindow.log(logMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
				}
			}
			// Update Item click area polygon if dragging Item point (old system)
			else if (selectedItemForPointDrag != null) {
				selectedItemForPointDrag.updateClickAreaPolygon();
				selectedItemForPointDrag.setHasCustomClickArea(true);

				String logMsg = "⚫ Dragging Item point " + selectedPathPointIndex + " of Item '"
						+ selectedItemForPointDrag.getName() + "' to (" + dragPoint.x + "," + dragPoint.y
						+ ") -> resources/items/" + selectedItemForPointDrag.getName() + ".txt";

				if (editorWindow != null) {
					editorWindow.log(logMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
				}
			}
			// Update KeyArea polygons if dragging KeyArea point
			else if (currentScene != null) {
				for (KeyArea area : currentScene.getKeyAreas()) {
					if (area.getPoints().contains(selectedPathPoint)) {
						// Update polygon with new coordinates
						area.updatePolygon();

						// Notify editor window for live update
						if (editorWindow != null) {
//							editorWindow.updatePointCoordinates(area, selectedPathPointIndex, selectedPathPoint.x,
//									selectedPathPoint.y);
						}
						break;
					}
				}
			}

			// Notify PointEditorDialog to update ListView
			if (simplePointEditorDialog != null) {
				simplePointEditorDialog.updatePointInList(selectedPathPointIndex, selectedPathPoint.x,
						selectedPathPoint.y);
			}

			// Notify ScenePointEditor to update ListView
			if (scenePointEditor != null && selectedItemForPointDrag != null) {
				String pointType = "";
				if (selectedCustomClickAreaForPointDrag != null) {
					pointType = "CustomClickArea";
				} else if (selectedMovingRangeForPointDrag != null) {
					pointType = "MovingRange";
				} else if (selectedPathForPointDrag != null) {
					pointType = "Path";
				}
				if (!pointType.isEmpty()) {
					scenePointEditor.updatePointInList(selectedPathPointIndex, selectedPathPoint.x, selectedPathPoint.y,
							pointType);
				}
			}

			gamePanel.repaint();
		}
	}

	private void handlePathPointRelease() {
		System.out.println("RELEASE: pointWasDragged=" + pointWasDragged + ", selectedPathPoint="
				+ (selectedPathPoint != null ? "SET" : "NULL"));

		if (selectedPathPoint != null) {
			// Determine which editor is active
			boolean hasEditor = (editorWindow != null || editorWindowSimple != null);

			if (hasEditor) {
				String logMsg = "✓ Moved point to (" + selectedPathPoint.x + "," + selectedPathPoint.y + ")";

				// Log to active editor
				if (editorWindow != null) {
					editorWindow.log(logMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
				}

				// Auto-save Item when releasing CustomClickArea point
				if (selectedCustomClickAreaForPointDrag != null && selectedItemForPointDrag != null) {
					try {
						ItemSaver.saveItemByName(selectedItemForPointDrag);
						String successMsg = "✓ Auto-saved CustomClickArea point " + selectedPathPointIndex
								+ " to resources/items/" + selectedItemForPointDrag.getName() + ".txt";

						if (editorWindow != null) {
							editorWindow.log(successMsg);
							editorWindow.autoSaveCurrentScene();
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(successMsg);
							SceneSaver.saveScene(currentScene);
							editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
						}
					} catch (Exception e) {
						String errorMsg = "ERROR saving CustomClickArea point: " + e.getMessage();
						if (editorWindow != null) {
							editorWindow.log(errorMsg);
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(errorMsg);
						}
					}
				}
				// Auto-save Item when releasing MovingRange point
				else if (selectedMovingRangeForPointDrag != null && selectedItemForPointDrag != null) {
					try {
						ItemSaver.saveItemByName(selectedItemForPointDrag);
						String successMsg = "✓ Auto-saved MovingRange point " + selectedPathPointIndex
								+ " to resources/items/" + selectedItemForPointDrag.getName() + ".txt";

						// Also save MovingRange to MovingRangeManager
						MovingRange range = selectedItemForPointDrag.getPrimaryMovingRange();
						if (range != null && range.getName() != null) {
							MovingRangeManager.save(range);
							successMsg += " | ✓ MovingRange saved to movingranges/" + range.getName() + ".txt";
						}

						if (editorWindow != null) {
							editorWindow.log(successMsg);
							editorWindow.autoSaveCurrentScene();
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(successMsg);
							SceneSaver.saveScene(currentScene);
							editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
						}
					} catch (Exception e) {
						String errorMsg = "ERROR saving MovingRange point: " + e.getMessage();
						if (editorWindow != null) {
							editorWindow.log(errorMsg);
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(errorMsg);
						}
					}
				}
				// Auto-save Item when releasing Path point
				else if (selectedPathForPointDrag != null && selectedItemForPointDrag != null) {
					try {
						ItemSaver.saveItemByName(selectedItemForPointDrag);
						String successMsg = "✓ Auto-saved Path point " + selectedPathPointIndex + " to resources/items/"
								+ selectedItemForPointDrag.getName() + ".txt";

						if (editorWindow != null) {
							editorWindow.log(successMsg);
							editorWindow.autoSaveCurrentScene();
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(successMsg);
							SceneSaver.saveScene(currentScene);
							editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
						}
					} catch (Exception e) {
						String errorMsg = "ERROR saving Path point: " + e.getMessage();
						if (editorWindow != null) {
							editorWindow.log(errorMsg);
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(errorMsg);
						}
					}
				}
				// Auto-save Item when releasing Item point (old system)
				else if (selectedItemForPointDrag != null) {
					try {
						ItemSaver.saveItemByName(selectedItemForPointDrag);
						String successMsg = "✓ Auto-saved Item point " + selectedPathPointIndex + " to resources/items/"
								+ selectedItemForPointDrag.getName() + ".txt";

						if (editorWindow != null) {
							editorWindow.log(successMsg);
							editorWindow.autoSaveCurrentScene();
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(successMsg);
							SceneSaver.saveScene(currentScene);
							editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
						}
					} catch (Exception e) {
						String errorMsg = "ERROR saving Item point: " + e.getMessage();
						if (editorWindow != null) {
							editorWindow.log(errorMsg);
						} else if (editorWindowSimple != null) {
							editorWindowSimple.log(errorMsg);
						}
					}
				}
				// Auto-save scene when releasing KeyArea point
				else {
					if (editorWindow != null) {
						editorWindow.autoSaveCurrentScene();
					} else if (editorWindowSimple != null && currentScene != null) {
						try {
							SceneSaver.saveScene(currentScene);
							editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
						} catch (Exception e) {
							editorWindowSimple.log("ERROR saving scene: " + e.getMessage());
						}
					}
				}
			}
		}

		// After all saving, handle selection based on whether point was dragged
		if (pointWasDragged) {
			// Point was dragged - deselect it and clear highlight
			selectedPathPoint = null;
			selectedPathPointIndex = -1;
			selectedItemForPointDrag = null;
			selectedCustomClickAreaForPointDrag = null;
			selectedMovingRangeForPointDrag = null;
			selectedPathForPointDrag = null;
			pointWasDragged = false;
			clearHighlightedPoint();
		}
		// If NOT dragged: Keep point selected (highlighting is already set in
		// mousePressed)
		// Don't clear selection - point stays selected for keyboard shortcuts
	}

	/**
	 * Handles the "+" keyboard shortcut to insert a new point. Opens
	 * ScenePointEditor and inserts a point at the correct position.
	 */
	private void handlePointInsertShortcut(String pointType) {
		if (selectedItemForPointDrag == null) {
			return;
		}

		// Open ScenePointEditor first (only works with Simple Editor)
		if (editorWindowSimple != null) {
			editorWindowSimple.openScenePointEditorForItem(selectedItemForPointDrag);
		}

		// Wait a bit for the editor to open, then add the point
		javax.swing.SwingUtilities.invokeLater(() -> {
			// Calculate insert index
			int pointCount = 0;
			if (pointType.equals("CustomClickArea")) {
				CustomClickArea area = selectedItemForPointDrag.getPrimaryCustomClickArea();
				if (area != null && area.getPoints() != null) {
					pointCount = area.getPoints().size();
				}
			} else if (pointType.equals("MovingRange")) {
				MovingRange range = selectedItemForPointDrag.getPrimaryMovingRange();
				if (range != null && range.getPoints() != null) {
					pointCount = range.getPoints().size();
				}
			} else if (pointType.equals("Path")) {
				Path path = selectedItemForPointDrag.getPrimaryPath();
				if (path != null && path.getPoints() != null) {
					pointCount = path.getPoints().size();
				}
			}

			// Same logic as getInsertIndex(): insert after selected, or at 0 if last is
			// selected
			int insertIndex;
			if (selectedPathPointIndex < 0 || pointCount == 0) {
				insertIndex = pointCount; // Append at end
			} else if (selectedPathPointIndex == pointCount - 1) {
				insertIndex = 0; // Last selected - insert at beginning
			} else {
				insertIndex = selectedPathPointIndex + 1; // Insert after selected
			}

			// Add point at calculated position
			Point newPoint = new Point(100, 100);
			if (pointType.equals("CustomClickArea")) {
				CustomClickArea area = selectedItemForPointDrag.ensurePrimaryCustomClickArea();
				area.addPoint(insertIndex, newPoint);
			} else if (pointType.equals("MovingRange")) {
				MovingRange range = selectedItemForPointDrag.ensurePrimaryMovingRange();
				range.addPoint(insertIndex, newPoint);
				MovingRangeManager.save(range);
			} else if (pointType.equals("Path")) {
				Path path = selectedItemForPointDrag.ensurePrimaryPath();
				path.addPoint(insertIndex, newPoint);
			}

			// Auto-save and log
			try {
				ItemSaver.saveItemByName(selectedItemForPointDrag);
				String logMsg = "✓ Inserted " + pointType + " point at index " + insertIndex + " (+ key)";
				if (editorWindow != null) {
					editorWindow.log(logMsg);
					editorWindow.autoSaveCurrentScene();
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
					if (currentScene != null) {
						SceneSaver.saveScene(currentScene);
					}
				}
			} catch (Exception e) {
				String errorMsg = "ERROR saving: " + e.getMessage();
				if (editorWindow != null) {
					editorWindow.log(errorMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(errorMsg);
				}
			}

			gamePanel.repaint();
		});
	}

	/**
	 * Handles the "DELETE" keyboard shortcut to remove the selected point. Opens
	 * ScenePointEditor and removes the selected point.
	 */
	private void handlePointDeleteShortcut(String pointType) {
		if (selectedItemForPointDrag == null || selectedPathPointIndex < 0) {
			return;
		}

		// Open ScenePointEditor first (only works with Simple Editor)
		if (editorWindowSimple != null) {
			editorWindowSimple.openScenePointEditorForItem(selectedItemForPointDrag);
		}

		// Wait a bit for the editor to open, then remove the point
		javax.swing.SwingUtilities.invokeLater(() -> {
			// Remove the point
			if (pointType.equals("CustomClickArea")) {
				CustomClickArea area = selectedItemForPointDrag.getPrimaryCustomClickArea();
				if (area != null) {
					area.removePoint(selectedPathPointIndex);
				}
			} else if (pointType.equals("MovingRange")) {
				MovingRange range = selectedItemForPointDrag.getPrimaryMovingRange();
				if (range != null) {
					range.removePoint(selectedPathPointIndex);
					MovingRangeManager.save(range);
				}
			} else if (pointType.equals("Path")) {
				Path path = selectedItemForPointDrag.getPrimaryPath();
				if (path != null) {
					path.removePoint(selectedPathPointIndex);
				}
			}

			// Clear selection
			int deletedIndex = selectedPathPointIndex;
			selectedPathPoint = null;
			selectedPathPointIndex = -1;

			// Auto-save and log
			try {
				ItemSaver.saveItemByName(selectedItemForPointDrag);
				String logMsg = "✓ Removed " + pointType + " point at index " + deletedIndex + " (DELETE key)";
				if (editorWindow != null) {
					editorWindow.log(logMsg);
					editorWindow.autoSaveCurrentScene();
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMsg);
					if (currentScene != null) {
						SceneSaver.saveScene(currentScene);
					}
				}
			} catch (Exception e) {
				String errorMsg = "ERROR saving: " + e.getMessage();
				if (editorWindow != null) {
					editorWindow.log(errorMsg);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(errorMsg);
				}
			}

			gamePanel.repaint();
		});
	}

	private void handleItemPress(Point clickPoint) {
		if (currentScene == null)
			return;

		int handleSize = 8;
		int handleTolerance = 6; // Extra pixels for easier clicking

		// Check if clicking on an item corner or body
		for (Item item : currentScene.getItems()) {
			if (isItemVisibleInCurrentMode(item)) {
				Point pos = item.getPosition();
				int imgWidth = item.getWidth();
				int imgHeight = item.getHeight();

				// Calculate corners
				int x = pos.x - imgWidth / 2;
				int y = pos.y - imgHeight / 2;

				// Check corners first (in editor mode)
				if (showPaths) {
					// Top-left
					if (Math.abs(clickPoint.x - x) <= handleSize / 2 + handleTolerance
							&& Math.abs(clickPoint.y - y) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.TOP_LEFT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging top-left corner of " + item.getName());
						}
						return;
					}
					// Top-right
					if (Math.abs(clickPoint.x - (x + imgWidth)) <= handleSize / 2 + handleTolerance
							&& Math.abs(clickPoint.y - y) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.TOP_RIGHT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging top-right corner of " + item.getName());
						}
						return;
					}
					// Bottom-left
					if (Math.abs(clickPoint.x - x) <= handleSize / 2 + handleTolerance
							&& Math.abs(clickPoint.y - (y + imgHeight)) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.BOTTOM_LEFT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging bottom-left corner of " + item.getName());
						}
						return;
					}
					// Bottom-right
					if (Math.abs(clickPoint.x - (x + imgWidth)) <= handleSize / 2 + handleTolerance
							&& Math.abs(clickPoint.y - (y + imgHeight)) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.BOTTOM_RIGHT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging bottom-right corner of " + item.getName());
						}
						return;
					}
				}

				// Check if clicking inside item body (for move)
				if (clickPoint.x >= x && clickPoint.x <= x + imgWidth && clickPoint.y >= y
						&& clickPoint.y <= y + imgHeight) {
					draggedItem = item;
					draggedCorner = ItemCorner.NONE;
					if (editorWindow != null) {
						editorWindow.log("Moving item: " + item.getName());
					}
					return;
				}
			}
		}
	}

	private void handleItemDrag(Point newPosition) {
		if (draggedItem == null)
			return;

		if (draggedCorner == ItemCorner.NONE) {
			// Move entire item
			Point oldPosition = draggedItem.getPosition();
			int deltaX = newPosition.x - oldPosition.x;
			int deltaY = newPosition.y - oldPosition.y;

			draggedItem.setPosition(newPosition.x, newPosition.y);

			// Move all custom click areas with the item
			if (draggedItem.getCustomClickAreas() != null && !draggedItem.getCustomClickAreas().isEmpty()) {
				for (CustomClickArea area : draggedItem.getCustomClickAreas()) {
					List<Point> points = area.getPoints();
					for (Point p : points) {
						p.x += deltaX;
						p.y += deltaY;
					}
					// Update polygon for this area
					area.updatePolygon();
				}
			}

			// Also move old-style custom click area (for backward compatibility)
			if (draggedItem.hasCustomClickArea() && draggedItem.getClickAreaPoints() != null) {
				List<Point> clickAreaPoints = draggedItem.getClickAreaPoints();
				for (Point p : clickAreaPoints) {
					p.x += deltaX;
					p.y += deltaY;
				}
				draggedItem.updateClickAreaPolygon();
			}
		} else {
			// Resize item by dragging corner
			Point pos = draggedItem.getPosition();
			int currentWidth = draggedItem.getWidth();
			int currentHeight = draggedItem.getHeight();

			// Calculate current bounds
			int left = pos.x - currentWidth / 2;
			int top = pos.y - currentHeight / 2;
			int right = pos.x + currentWidth / 2;
			int bottom = pos.y + currentHeight / 2;

			// Adjust bounds based on which corner is being dragged
			switch (draggedCorner) {
			case TOP_LEFT:
				left = newPosition.x;
				top = newPosition.y;
				break;
			case TOP_RIGHT:
				right = newPosition.x;
				top = newPosition.y;
				break;
			case BOTTOM_LEFT:
				left = newPosition.x;
				bottom = newPosition.y;
				break;
			case BOTTOM_RIGHT:
				right = newPosition.x;
				bottom = newPosition.y;
				break;
			}

			// Calculate new size and position
			int newWidth = Math.max(20, right - left); // Minimum 20px
			int newHeight = Math.max(20, bottom - top);
			int newCenterX = left + newWidth / 2;
			int newCenterY = top + newHeight / 2;

			// Update item
			draggedItem.setSize(newWidth, newHeight);
			draggedItem.setPosition(newCenterX, newCenterY);
		}

		// CRITICAL: Live update ScenePointEditor during drag
		if (scenePointEditor != null && draggedItem != null) {
			System.out.println("🔵 DEBUG: Calling refreshPointFieldsForItem from handleItemDrag for item: "
					+ draggedItem.getName());
			scenePointEditor.refreshPointFieldsForItem(draggedItem);
		} else {
			if (scenePointEditor == null) {
				System.out.println("⚠️ DEBUG: scenePointEditor is NULL in handleItemDrag");
			}
			if (draggedItem == null) {
				System.out.println("⚠️ DEBUG: draggedItem is NULL in handleItemDrag");
			}
		}

		gamePanel.repaint();
	}

	private void handleItemRelease() {
		if (draggedItem != null) {
			// Determine which editor is active
			boolean hasEditor = (editorWindow != null || editorWindowSimple != null);

			if (hasEditor) {
				Point pos = draggedItem.getPosition();
				String logMessage;

				if (draggedCorner == ItemCorner.NONE) {
					logMessage = "Moved item " + draggedItem.getName() + " to (" + pos.x + ", " + pos.y + ")";
				} else {
					logMessage = "Resized item " + draggedItem.getName() + " to " + draggedItem.getWidth() + "x"
							+ draggedItem.getHeight();
				}

				// Log to the active editor
				if (editorWindow != null) {
					editorWindow.log(logMessage);
				} else if (editorWindowSimple != null) {
					editorWindowSimple.log(logMessage);
				}

				// Auto-save both the item and the scene
				try {
					ItemSaver.saveItemByName(draggedItem);

					// Save scene using the active editor
					if (editorWindow != null) {
						editorWindow.autoSaveCurrentScene();
					} else if (editorWindowSimple != null && currentScene != null) {
						// EditorMainSimple doesn't have autoSaveCurrentScene(), so save directly
						SceneSaver.saveScene(currentScene);
						editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
					}
				} catch (Exception e) {
					String errorMsg = "ERROR saving item: " + e.getMessage();
					if (editorWindow != null) {
						editorWindow.log(errorMsg);
					} else if (editorWindowSimple != null) {
						editorWindowSimple.log(errorMsg);
					}
				}
			}
		}
		draggedItem = null;
		draggedCorner = ItemCorner.NONE;
		initialDragPoint = null;
	}

	// ==================== Character Movement ====================

	/**
	 * Handles character movement when user clicks on scene. Only applies to items
	 * with isFollowingOnMouseClick = true.
	 */
	private void handleCharacterMovement(Point clickPoint) {
		// Find item with isFollowingOnMouseClick = true
		Item characterItem = findCharacterItem();

		if (characterItem == null) {
			return; // No character item found
		}

		// Calculate target position (respects MovingRange)
		Point targetPosition = CharacterMovement.calculateTargetPosition(characterItem, clickPoint, progress);

		// Start animated movement to target
		startCharacterMovement(characterItem, targetPosition);
	}

	/**
	 * Finds the first item with isFollowingOnMouseClick = true in the current
	 * scene.
	 */
	private Item findCharacterItem() {
		if (currentScene == null)
			return null;

		for (Item item : currentScene.getItems()) {
			if (item.isFollowingOnMouseClick()) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Starts smooth animated movement of character to target position.
	 */
	private void startCharacterMovement(Item characterItem, Point targetPosition) {
		this.movingCharacter = characterItem;
		this.characterTargetPosition = targetPosition;

		// Stop existing movement
		if (characterMovementTimer != null && characterMovementTimer.isRunning()) {
			characterMovementTimer.stop();
		}

		// Create movement timer (~60 FPS)
		characterMovementTimer = new javax.swing.Timer(16, e -> {
			if (movingCharacter == null || characterTargetPosition == null) {
				((javax.swing.Timer) e.getSource()).stop();
				return;
			}

			Point currentPos = movingCharacter.getPosition();

			// Calculate distance to target
			double distance = currentPos.distance(characterTargetPosition);

			if (distance <= CHARACTER_SPEED) {
				// Target reached
				// Move item and its CustomClickArea
				moveItemWithCustomClickArea(movingCharacter, characterTargetPosition.x, characterTargetPosition.y);
				((javax.swing.Timer) e.getSource()).stop();

				// Auto-save final position (only in editor mode)
				if (editorWindow != null || editorWindowSimple != null) {
					try {
						ItemSaver.saveItemByName(movingCharacter);
						if (editorWindow != null) {
							editorWindow.autoSaveCurrentScene();
						} else if (editorWindowSimple != null) {
							SceneSaver.saveScene(currentScene);
						}
					} catch (Exception ex) {
						System.err.println("Failed to save character position: " + ex.getMessage());
					}
				}

				movingCharacter = null;
				characterTargetPosition = null;
			} else {
				// Move towards target
				double angle = Math.atan2(characterTargetPosition.y - currentPos.y,
						characterTargetPosition.x - currentPos.x);

				int newX = currentPos.x + (int) (Math.cos(angle) * CHARACTER_SPEED);
				int newY = currentPos.y + (int) (Math.sin(angle) * CHARACTER_SPEED);

				// Move item and its CustomClickArea
				moveItemWithCustomClickArea(movingCharacter, newX, newY);
			}

			gamePanel.repaint();
		});

		characterMovementTimer.start();
	}

	/**
	 * Stops current character movement if any.
	 */
	public void stopCharacterMovement() {
		if (characterMovementTimer != null && characterMovementTimer.isRunning()) {
			characterMovementTimer.stop();
		}
		movingCharacter = null;
		characterTargetPosition = null;
	}

	/**
	 * Starts character movement for processes (non-blocking from process
	 * perspective) Called by Process.MovementAction
	 */
	public void startCharacterMovementProcess(Item characterItem, Point targetPosition) {
		startCharacterMovement(characterItem, targetPosition);
	}

	/**
	 * Check if character is currently moving (for process blocking)
	 */
	public boolean isCharacterMoving() {
		return movingCharacter != null && characterMovementTimer != null && characterMovementTimer.isRunning();
	}

	/**
	 * Moves an item to a new position and updates all CustomClickAreas to move with
	 * it. This ensures that click areas stay synchronized with the item's visual
	 * position.
	 *
	 * @param item The item to move
	 * @param newX New X position
	 * @param newY New Y position
	 */
	private void moveItemWithCustomClickArea(Item item, int newX, int newY) {
		// Calculate delta (how much the item moved)
		Point oldPosition = item.getPosition();
		int deltaX = newX - oldPosition.x;
		int deltaY = newY - oldPosition.y;

		// Update item position
		item.setPosition(newX, newY);

		// Move all custom click areas with the item
		if (item.getCustomClickAreas() != null && !item.getCustomClickAreas().isEmpty()) {
			for (CustomClickArea area : item.getCustomClickAreas()) {
				List<Point> points = area.getPoints();
				for (Point p : points) {
					p.x += deltaX;
					p.y += deltaY;
				}
				// Update polygon for this area
				area.updatePolygon();
			}
		}

		// Also move old-style custom click area (for backward compatibility)
		if (item.hasCustomClickArea() && item.getClickAreaPoints() != null) {
			List<Point> clickAreaPoints = item.getClickAreaPoints();
			for (Point p : clickAreaPoints) {
				p.x += deltaX;
				p.y += deltaY;
			}
			item.updateClickAreaPolygon();
		}
	}

	// ==================== End Character Movement ====================

	/**
	 * Helper method to log to whichever editor is currently active
	 */
	private void logToActiveEditor(String message) {
		if (editorWindow != null && editorWindow.isVisible()) {
			editorWindow.log(message);
		} else if (editorWindowSimple != null && editorWindowSimple.isVisible()) {
			editorWindowSimple.log(message);
		}
	}

	/**
	 * Auto-save current scene to whichever editor is active
	 */
	private void autoSaveCurrentSceneToActiveEditor() {
		if (editorWindow != null && editorWindow.isVisible()) {
			editorWindow.autoSaveCurrentScene();
		} else if (editorWindowSimple != null && editorWindowSimple.isVisible() && currentScene != null) {
			try {
				SceneSaver.saveScene(currentScene);
				editorWindowSimple.log("✓ Auto-saved scene: " + currentScene.getName());
			} catch (Exception e) {
				editorWindowSimple.log("ERROR saving scene: " + e.getMessage());
			}
		}
	}

	private void handleImageDrop(File imageFile, Point dropPoint) {
		if (currentScene == null) {
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Ask for item name
		String itemName = JOptionPane.showInputDialog(this, "Enter item name for image:\n" + imageFile.getName(),
				"Create Item from Image", JOptionPane.PLAIN_MESSAGE);

		if (itemName == null || itemName.trim().isEmpty()) {
			return; // User cancelled
		}

		itemName = itemName.trim();

		// Check if item already exists
		File itemFile = ResourcePathHelper.resolve("items/" + itemName + ".txt");
		if (itemFile.exists()) {
			int overwrite = JOptionPane.showConfirmDialog(this, "Item '" + itemName + "' already exists!\nOverwrite?",
					"Item Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (overwrite != JOptionPane.YES_OPTION) {
				return;
			}
		}

		try {
			// Copy image to resources/images/items/ directory
			File itemsImagesDir = ResourcePathHelper.resolve("images/items");
			if (!itemsImagesDir.exists()) {
				itemsImagesDir.mkdirs();
			}

			String imageFileName = imageFile.getName();
			File targetImageFile = new File(itemsImagesDir, imageFileName);

			// Copy file
			java.nio.file.Files.copy(imageFile.toPath(), targetImageFile.toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);

			// Create new item
			Item newItem = new Item(itemName);
			newItem.setImageFileName(imageFileName);
			newItem.setImageFilePath(ResourcePathHelper.resolvePath("images/items/" + imageFileName));
			newItem.setPosition(dropPoint.x, dropPoint.y);
			newItem.setInInventory(false);

			// Get original image size
			ImageIcon icon = new ImageIcon(targetImageFile.getAbsolutePath());
			int originalWidth = icon.getIconWidth();
			int originalHeight = icon.getIconHeight();

			// Auto-scale if image is larger than 120x120
			int targetWidth = originalWidth;
			int targetHeight = originalHeight;

			if (originalWidth > 120 || originalHeight > 120) {
				// Scale down to fit in 120x120 while maintaining aspect ratio
				double scaleX = 120.0 / originalWidth;
				double scaleY = 120.0 / originalHeight;
				double scale = Math.min(scaleX, scaleY);

				targetWidth = (int) (originalWidth * scale);
				targetHeight = (int) (originalHeight * scale);

				logToActiveEditor("Auto-scaled image from " + originalWidth + "x" + originalHeight + " to "
						+ targetWidth + "x" + targetHeight);
			}

			// Set size (use scaled size or default if invalid)
			if (targetWidth > 0 && targetHeight > 0) {
				newItem.setSize(targetWidth, targetHeight);
			} else {
				newItem.setSize(100, 100); // Default size
			}

			// Set default custom click area to 140x140 if image was scaled
			if (originalWidth > 120 || originalHeight > 120) {
				newItem.getClickAreaPoints().clear();
				// Create rectangle 140x140 centered on item position
				int halfSize = 70; // 140/2
				int centerX = dropPoint.x;
				int centerY = dropPoint.y;
				newItem.addClickAreaPoint(centerX - halfSize, centerY - halfSize); // Top-left
				newItem.addClickAreaPoint(centerX + halfSize, centerY - halfSize); // Top-right
				newItem.addClickAreaPoint(centerX + halfSize, centerY + halfSize); // Bottom-right
				newItem.addClickAreaPoint(centerX - halfSize, centerY + halfSize); // Bottom-left
				newItem.setHasCustomClickArea(true);
				newItem.updateClickAreaPolygon();
				logToActiveEditor("Set custom click area to 140x140");
			}

			// Save item
			ItemSaver.saveItemByName(newItem);

			// Add to current scene
			currentScene.addItem(newItem);
			autoSaveCurrentSceneToActiveEditor();

			// Repaint to show the item
			gamePanel.repaint();

			logToActiveEditor(
					"Created item '" + itemName + "' from dropped image at (" + dropPoint.x + ", " + dropPoint.y + ")");

			// Open ItemEditor with the new item selected
			openItemEditorWithItem(itemName);

		} catch (Exception e) {
			logToActiveEditor("ERROR creating item from image: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error creating item:\n" + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private void openItemEditorWithItem(String itemName) {
		// Open ItemEditor (non-modal so it won't block)
		// Note: ItemEditorDialog requires EditorMain, so only open when EditorMain is
		// available
		SwingUtilities.invokeLater(() -> {
			if (editorWindow != null && editorWindow.isVisible()) {
				ItemEditorDialog dialog = new ItemEditorDialog(editorWindow);
				dialog.setVisible(true);
				// Auto-select the newly created item
				dialog.selectItemByName(itemName);
			} else {
				// If only EditorMainSimple is open, log a message
				logToActiveEditor("ItemEditor only available with EditorMain (ALT+E), not EditorMainSimple (ALT+S)");
			}
		});
	}

	private void selectAction(String action) {
		selectedAction = action;
		System.out.println("Aktion gewählt: " + action);

		// Reset item-as-cursor mode when changing action
		if (selectedInventoryItem != null) {
			selectedInventoryItem = null;
			selectedItem = null;
			// Reset inventory borders
			for (Component c : inventoryPanel.getComponents()) {
				if (c instanceof JButton) {
					((JButton) c).setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			}
		}

		// Update cursor based on selected action
		updateCursorForAction(action);
	}

	/**
	 * Updates the cursor based on the selected action
	 */
	private void updateCursorForAction(String action) {
		if (action == null) {
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			return;
		}

		switch (action) {
		case "Nimm":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case "Gib":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case "Ziehe":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			break;
		case "Drücke":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case "Drehe":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case "Hebe":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case "Anschauen":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			break;
		case "Sprich":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			break;
		case "Benutze":
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		default:
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		}
	}

	private void handleGamePanelClick(Point clickPoint) {
		System.out.println("🖱️ handleGamePanelClick: Click at (" + clickPoint.x + ", " + clickPoint.y + ")");
		System.out.println(
				"   addPointModeSimple=" + addPointModeSimple + ", addPointModeTypeSimple=" + addPointModeTypeSimple);
		System.out.println("   scenePointEditor=" + (scenePointEditor != null ? "REGISTERED" : "NULL"));

		// IMPORTANT: If a point is currently selected, don't process sprite/item clicks
		// Just deselect the point
		if (selectedPathPoint != null) {
			System.out.println("   -> Point is selected, deselecting and ignoring sprite/item click");
			selectedPathPoint = null;
			selectedPathPointIndex = -1;
			clearHighlightedPoint();
			gamePanel.repaint();
			return;
		}

		// Check if Scene Point Editor is in add point mode
		if (addPointModeSimple && scenePointEditor != null) {
			System.out.println("✅ Delegating to ScenePointEditor.addPointAtPosition()");
			scenePointEditor.addPointAtPosition(clickPoint.x, clickPoint.y, addPointModeTypeSimple);
			return;
		}

		// Check if Simple Editor PointEditorDialog is in add point mode
		if (addPointModeSimple && simplePointEditorDialog != null) {
			simplePointEditorDialog.addPointAtPosition(clickPoint.x, clickPoint.y);
			return;
		}

		// Check if Simple Editor is in add point mode (legacy)
		if (addPointModeSimple && addPointModeItemSimple != null && editorWindowSimple != null) {
			editorWindowSimple.addPointFromCanvas(clickPoint.x, clickPoint.y, addPointModeTypeSimple);
			return;
		}

		// Check if Universal Point Editor is in add point mode
		if (pointEditorDialog != null && addPointMode) {
			pointEditorDialog.addPointAtPosition(clickPoint.x, clickPoint.y);
			return;
		}

		// Check if CustomClickArea Panel is in add point mode
		if (customClickAreaPanel != null && addPointMode) {
			try {
				// Use reflection to call addPointAtPosition method
				java.lang.reflect.Method method = customClickAreaPanel.getClass().getMethod("addPointAtPosition",
						int.class, int.class);
				method.invoke(customClickAreaPanel, clickPoint.x, clickPoint.y);
				return;
			} catch (Exception e) {
				System.err.println("ERROR calling CustomClickAreaPanel.addPointAtPosition: " + e.getMessage());
				e.printStackTrace();
			}
		}

		// Check if PointsManager is in add point mode
//		if (editorWindow != null && editorWindow.getPointsManager() != null) {
//			PointsManagerDialog pm = editorWindow.getPointsManager();
//			if (pm.isAddingPoint()) {
//				pm.addPointAtPosition(clickPoint.x, clickPoint.y);
//				return;
//			}
//		}

		// Add point mode takes priority (legacy for old editor)
//		if (addPointMode && addPointModeEditor != null) {
////			addPointModeEditor.addPointAtPosition(clickPoint.x, clickPoint.y);
//			return;
//		}

		if (currentScene == null)
			return;

		// === CHARACTER MOVEMENT ===
		// Handle character movement (for items with isFollowingOnMouseClick = true)
		// This runs BEFORE action handling, so characters can move regardless of
		// selected action
		handleCharacterMovement(clickPoint);

		// Check if clicked on an Item FIRST (Items should have priority over KeyAreas)
		Item clickedItem = currentScene.getItemAt(clickPoint);

		// === EDITOR MODE: Item selection ===
		// When editor is open (showPaths), clicking on an item selects it for editing
		if (showPaths && clickedItem != null && clickedItem.isVisible() && isItemVisibleInCurrentMode(clickedItem)) {
			// Select or deselect the clicked item
			Item currentlySelected = currentScene.getSelectedItem();
			if (currentlySelected == clickedItem) {
				// Deselect if clicking the same item again
				currentScene.setSelectedItem(null);
				selectedItemInEditor = null;
				logToActiveEditor("Deselected item: " + clickedItem.getName());
			} else {
				// Select the clicked item
				currentScene.setSelectedItem(clickedItem);
				selectedItemInEditor = clickedItem;
				logToActiveEditor("Selected item: " + clickedItem.getName() + " at (" + clickedItem.getPosition().x + "," + clickedItem.getPosition().y + ")");

				// Also select in the editor window (only works with EditorMain)
				if (editorWindow != null && editorWindow.isVisible()) {
					editorWindow.selectItem(clickedItem);
				}
				// Note: EditorMainSimple doesn't have selectItem() - selection is still saved in scene
			}
			gamePanel.repaint();
			return; // Don't process game actions when in editor mode
		}

		// === GAME MODE: Action handling ===
		if (clickedItem != null && clickedItem.isVisible() && selectedAction != null) {
			// Perform action on Item
			String result = clickedItem.performAction(selectedAction, progress);

			// Log action to debug window
			debugWindow.logAction(selectedAction, clickedItem.getName() + " (Item)", result);

			if (result != null) {
				System.out.println("DEBUG [Item]: Processing result: " + result);
				// Support multiple results separated by |||
				String[] results = result.split("\\|\\|\\|");
				System.out.println("DEBUG [Item]: Split into " + results.length + " results");
				for (String singleResult : results) {
					singleResult = singleResult.trim();
					System.out.println("DEBUG [Item]: Processing single result: " + singleResult);

					if (singleResult.startsWith("##load")) {
						// Load new scene
						String sceneName = singleResult.substring(6).trim();
						loadScene(sceneName);
					} else if (singleResult.startsWith("#Dialog:")) {
						// Show dialog by name - extract dialog name after the 6 dashes
						String dialogLine = singleResult.substring(8).trim();
						// Remove leading dashes (------dialogname.txt)
						while (dialogLine.startsWith("-")) {
							dialogLine = dialogLine.substring(1);
						}
						// Remove .txt if present
						if (dialogLine.endsWith(".txt")) {
							dialogLine = dialogLine.substring(0, dialogLine.length() - 4);
						}
						showDialog(dialogLine.trim());
					} else if (singleResult.startsWith("#SetBoolean:")) {
						System.out.println("DEBUG: #SetBoolean detected in ITEM action!");
						// Set boolean variable in Conditions
						String[] parts = singleResult.substring(12).split("=");
						if (parts.length == 2) {
							String condName = parts[0].trim();
							boolean newValue = Boolean.parseBoolean(parts[1].trim());
							boolean oldValue = Conditions.getCondition(condName);
							System.out.println("DEBUG [ITEM]: ConditionName=" + condName + ", oldValue=" + oldValue
									+ ", newValue=" + newValue);

							Conditions.setCondition(condName, newValue);

							// Log to debug window
							debugWindow.logConditionChange(condName, oldValue, newValue);

							updateInventory(); // Update inventory when conditions change
							reloadBackgroundImageIfNeeded(); // Reload background if needed

							// Auto-save after condition change
							autoSave();
							System.out.println("✓ Auto-saved after condition change: " + condName);
						}
					} else if (singleResult.startsWith("#AddItem:")) {
						// Add item to inventory
						String itemName = singleResult.substring(9).trim();
						addItemToInventoryByName(itemName);
					} else if (singleResult.startsWith("#Process:")) {
						// Execute process
						String processName = singleResult.substring(9).trim();
						System.out.println("Loading process: " + processName);
						Process process = ProcessLoader.loadProcess(processName);
						if (process != null) {
							processExecutor.executeProcess(process);
						} else {
							System.err.println("Process not found: " + processName);
						}
					}
				}
			}

			// Reset action and item-as-cursor mode
			selectedAction = null;
			selectedInventoryItem = null;
			selectedItem = null;
			// Reset inventory borders
			for (Component c : inventoryPanel.getComponents()) {
				if (c instanceof JButton) {
					((JButton) c).setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			}
			// Reset cursor to default
			updateCursorForAction(null);
			gamePanel.repaint();
			return; // Don't check KeyAreas if Item was clicked
		}

		// Check if clicked on a KeyArea
		KeyArea clickedArea = currentScene.getKeyAreaAt(clickPoint);

		if (clickedArea != null && selectedAction != null) {
			// Perform action on KeyArea
			String result = clickedArea.performAction(selectedAction, progress);

			// Log action to debug window
			debugWindow.logAction(selectedAction, clickedArea.getName() + " (KeyArea)", result);

			if (result != null) {
				System.out.println("DEBUG [KeyArea]: Processing result: " + result);
				// Support multiple results separated by |||
				String[] results = result.split("\\|\\|\\|");
				System.out.println("DEBUG [KeyArea]: Split into " + results.length + " results");
				for (String singleResult : results) {
					singleResult = singleResult.trim();
					System.out.println("DEBUG [KeyArea]: Processing single result: " + singleResult);

					if (singleResult.startsWith("##load")) {
						// Load new scene
						String sceneName = singleResult.substring(6).trim();
						loadScene(sceneName);
					} else if (singleResult.startsWith("#Dialog:")) {
						// Show dialog by name - extract dialog name after the 6 dashes
						String dialogLine = singleResult.substring(8).trim();
						// Remove leading dashes (------dialogname.txt)
						while (dialogLine.startsWith("-")) {
							dialogLine = dialogLine.substring(1);
						}
						// Remove .txt if present
						if (dialogLine.endsWith(".txt")) {
							dialogLine = dialogLine.substring(0, dialogLine.length() - 4);
						}
						showDialog(dialogLine.trim());
					} else if (singleResult.startsWith("#SetBoolean:")) {
						System.out.println("DEBUG: #SetBoolean detected in KEYAREA action!");
						// Set boolean variable in Conditions
						String[] parts = singleResult.substring(12).split("=");
						if (parts.length == 2) {
							String condName = parts[0].trim();
							boolean newValue = Boolean.parseBoolean(parts[1].trim());
							boolean oldValue = Conditions.getCondition(condName);
							System.out.println("DEBUG [KEYAREA]: ConditionName=" + condName + ", oldValue=" + oldValue
									+ ", newValue=" + newValue);

							Conditions.setCondition(condName, newValue);

							// Log to debug window
							debugWindow.logConditionChange(condName, oldValue, newValue);

							updateInventory(); // Update inventory when conditions change
							reloadBackgroundImageIfNeeded(); // Reload background if needed

							// Auto-save after condition change
							autoSave();
							System.out.println("✓ Auto-saved after condition change: " + condName);
						}
					} else if (singleResult.startsWith("#AddItem:")) {
						// Add item to inventory
						String itemName = singleResult.substring(9).trim();
						addItemToInventoryByName(itemName);
					} else if (singleResult.startsWith("#Process:")) {
						// Execute process
						String processName = singleResult.substring(9).trim();
						System.out.println("Loading process: " + processName);
						Process process = ProcessLoader.loadProcess(processName);
						if (process != null) {
							processExecutor.executeProcess(process);
						} else {
							System.err.println("Process not found: " + processName);
						}
					}
				}
			}

			// Reset action and item-as-cursor mode
			selectedAction = null;
			selectedInventoryItem = null;
			selectedItem = null;
			// Reset inventory borders
			for (Component c : inventoryPanel.getComponents()) {
				if (c instanceof JButton) {
					((JButton) c).setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			}
			// Reset cursor to default
			updateCursorForAction(null);
		} else if (selectedAction == null) {
			// Move player (simple implementation)
			playerPosition = clickPoint;
			gamePanel.repaint();
		}
	}

	public void loadScene(String sceneName) {
		// Default behavior: Load from current state (Gaming Mode)
		loadSceneFromProgress(sceneName);
	}

	/**
	 * Load scene from DEFAULT/TEMPLATE files (Editor Mode)
	 *
	 * @param sceneName Format: "SceneName/SubSceneName" (e.g. "Beach/MainBeach")
	 */
	public void loadSceneFromDefault(String sceneName) {
		try {
			// Parse sceneName: "SceneName/SubSceneName"
			String[] parts = sceneName.split("/");
			if (parts.length != 2) {
				throw new IllegalArgumentException(
						"Invalid scene name format. Expected: SceneName/SubSceneName, got: " + sceneName);
			}

			String sceneDir = parts[0];
			String subSceneName = parts[1];

			// Load using FileHandlingSimple
			currentScene = FileHandlingSimple.loadSubScene(sceneDir, subSceneName, progress);
			progress.setCurrentScene(sceneName);

			// Log scene change to debug window
			debugWindow.logSceneChange(sceneName);

			// Load background image (with conditional support)
			String bgPath = currentScene.getCurrentBackgroundImagePath(progress);
			ImageIcon bgImage = null;

			// Use ResourcePathHelper for robust path resolution
			File imageFile = ResourcePathHelper.findImageFile(bgPath);
			if (imageFile != null) {
				bgImage = new ImageIcon(imageFile.getAbsolutePath());
				System.out.println("✓ Bild geladen von: " + imageFile.getAbsolutePath());
			} else {
				// Try URL (classpath or JAR)
				java.net.URL imageUrl = ResourcePathHelper.findImageURL(bgPath);
				if (imageUrl != null) {
					bgImage = new ImageIcon(imageUrl);
					System.out.println("✓ Bild geladen von Classpath/JAR");
				} else {
					System.err.println("✗ Hintergrundbild nicht gefunden: " + bgPath);
					System.err.println("  Projekt-Root: " + ResourcePathHelper.getProjectRootPath());
					System.err.println("  Aktuelles Verzeichnis: " + new File(".").getAbsolutePath());
					ResourcePathHelper.debugPrintPaths(bgPath);
				}
			}

			if (bgImage != null) {
				// Scale image to fit and store it
				backgroundImage = bgImage.getImage().getScaledInstance(1024, 668, Image.SCALE_SMOOTH);
			} else {
				// Clear background
				backgroundImage = null;
				System.err.println("✗ Bild konnte nicht geladen werden: " + bgPath);
			}

			// Trigger repaint to show new background
			gamePanel.repaint();

			// Update inventory
			updateInventory();

			// Register scene with AutoSaveManager for auto-saving
			AutoSaveManager.setCurrentScene(currentScene);

			System.out.println("Scene geladen (DEFAULT): " + sceneName);
		} catch (Exception e) {
			System.err.println("Fehler beim Laden der Scene: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Load scene from PROGRESS files
	 *
	 * @param sceneName Format: "SceneName/SubSceneName" (e.g. "Beach/MainBeach")
	 */
	public void loadSceneFromProgress(String sceneName) {
		try {
			// Parse sceneName: "SceneName/SubSceneName"
			String[] parts = sceneName.split("/");
			if (parts.length != 2) {
				throw new IllegalArgumentException(
						"Invalid scene name format. Expected: SceneName/SubSceneName, got: " + sceneName);
			}

			String sceneDir = parts[0];
			String subSceneName = parts[1];

			// Try to load from PROGRESS first
			String progressPath = ResourcePathHelper.resolvePath("scenes/" + sceneDir + "/" + subSceneName + "_progress.txt");
			java.io.File progressFile = new java.io.File(progressPath);

			if (progressFile.exists()) {
				// Load from PROGRESS using SceneLoader
				String relativeScenePath = sceneDir + "/" + subSceneName;
				currentScene = SceneLoader.loadSceneFromProgress(relativeScenePath, progress);
				System.out.println("✓ Scene loaded from PROGRESS: " + sceneName);
			} else {
				// Fallback to DEFAULT if PROGRESS doesn't exist
				currentScene = FileHandlingSimple.loadSubScene(sceneDir, subSceneName, progress);
				System.out.println("✓ Scene loaded from DEFAULT (no progress file): " + sceneName);
			}

			progress.setCurrentScene(sceneName);

			// Log scene change to debug window
			debugWindow.logSceneChange(sceneName);

			// Load background image (with conditional support)
			String bgPath = currentScene.getCurrentBackgroundImagePath(progress);
			ImageIcon bgImage = null;

			// Use ResourcePathHelper for robust path resolution
			File imageFile = ResourcePathHelper.findImageFile(bgPath);
			if (imageFile != null) {
				bgImage = new ImageIcon(imageFile.getAbsolutePath());
				System.out.println("✓ Bild geladen von: " + imageFile.getAbsolutePath());
			} else {
				// Try URL (classpath or JAR)
				java.net.URL imageUrl = ResourcePathHelper.findImageURL(bgPath);
				if (imageUrl != null) {
					bgImage = new ImageIcon(imageUrl);
					System.out.println("✓ Bild geladen von Classpath/JAR");
				} else {
					System.err.println("✗ Hintergrundbild nicht gefunden: " + bgPath);
					System.err.println("  Projekt-Root: " + ResourcePathHelper.getProjectRootPath());
					System.err.println("  Aktuelles Verzeichnis: " + new File(".").getAbsolutePath());
					ResourcePathHelper.debugPrintPaths(bgPath);
				}
			}

			if (bgImage != null) {
				// Scale image to fit and store it
				backgroundImage = bgImage.getImage().getScaledInstance(1024, 668, Image.SCALE_SMOOTH);
			} else {
				// Clear background
				backgroundImage = null;
				System.err.println("✗ Bild konnte nicht geladen werden: " + bgPath);
			}

			// Trigger repaint to show new background
			gamePanel.repaint();

			// Update inventory
			updateInventory();

			// Register scene with AutoSaveManager for auto-saving
			AutoSaveManager.setCurrentScene(currentScene);

			System.out.println("Scene geladen (PROGRESS): " + sceneName);
		} catch (Exception e) {
			System.err.println("Fehler beim Laden der Scene: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Switches to Gaming Mode: - Enables auto-save - Loads conditions from progress
	 * - Reloads current scene from progress files
	 *
	 * Called when editor window closes (via X button or Alt+E)
	 */
	public void switchToGamingMode() {
		System.out.println("======================================");
		System.out.println("SWITCHING TO GAMING MODE");
		System.out.println("======================================");

		// 1. Enable auto-save
		AutoSaveManager.setEnabled(true);
		System.out.println("✓ Auto-save ENABLED");

		// 2. Load conditions from current game state
		Conditions.loadFromProgress(ResourcePathHelper.resolvePath("conditions/conditions.txt"));
		System.out.println("✓ Conditions loaded from conditions/conditions.txt (CURRENT STATE)");

		// 3. Reload current scene from current state
		String currentSceneName = getCurrentSceneName();
		if (currentSceneName != null && !currentSceneName.equals("none")) {
			loadSceneFromProgress(currentSceneName);
			System.out.println("✓ Scene reloaded from current state: " + currentSceneName);
		}

		// 4. Update inventory
		updateInventory();
		System.out.println("✓ Inventory updated");

		// 5. Repaint game panel
		gamePanel.repaint();

		System.out.println("======================================");
		System.out.println("GAMING MODE ACTIVE");
		System.out.println("All reads/writes use current state files (.txt)");
		System.out.println("======================================");
	}

	/**
	 * Reloads all scenes (actually just reloads the current scene since scenes are
	 * loaded on-demand)
	 */
	public void reloadAllScenes() {
		if (currentScene != null) {
			String sceneName = currentScene.getName();
			loadScene(sceneName);
			System.out.println("Reloaded scene: " + sceneName);
		}
	}

	/**
	 * Reloads the inventory
	 */
	public void reloadInventory() {
		updateInventory();
		System.out.println("Inventory reloaded");
	}

	/**
	 * Public method to show dialog by name (for ProcessExecutor)
	 */
	public void showDialogByName(String dialogName) {
		showDialog(dialogName);
	}

	private void showDialog(String dialogName) {
		if (currentScene == null) {
			return;
		}

		String dialogText = currentScene.getDialog(dialogName);

		if (dialogText == null) {
			// Show detailed error with available dialogs
			StringBuilder errorMsg = new StringBuilder();
			errorMsg.append("ERROR: Dialog nicht gefunden: ").append(dialogName).append("\n\n");
			errorMsg.append("Verfügbare Dialoge in dieser Scene:\n");

			Map<String, String> availableDialogs = currentScene.getDialogs();
			if (availableDialogs.isEmpty()) {
				errorMsg.append("  (keine Dialoge definiert)\n");
			} else {
				for (String dialogKey : availableDialogs.keySet()) {
					errorMsg.append("  • ").append(dialogKey).append("\n");
				}
			}

			errorMsg.append("\nBitte prüfen Sie die Scene-Datei!");

			System.err.println("Dialog nicht gefunden: " + dialogName);
			System.err.println("Verfügbare Dialoge: " + availableDialogs.keySet());

			JOptionPane.showMessageDialog(this, errorMsg.toString(), "Dialog Fehler", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JOptionPane.showMessageDialog(this, dialogText, "Dialog", JOptionPane.INFORMATION_MESSAGE);
	}

	private void resetGame() {
		progress.resetToDefault();
		loadScene(progress.getCurrentScene());
	}

	private void updateInventory() {
		inventoryPanel.removeAll();

		var c = ThemeManager.colors();
		var t = ThemeManager.typography();

		// Show all items that are in inventory
		if (currentScene != null) {
			for (Item item : currentScene.getItems()) {
				if (item.isInInventory()) {
					addItemToInventory(item);
				}
			}
		}

		// If empty, show message with theme color
		if (inventoryPanel.getComponentCount() == 0) {
			JLabel emptyLabel = new JLabel("(Keine Items)");
			emptyLabel.setFont(t.sm());
			emptyLabel.setForeground(c.getTextTertiary());
			inventoryPanel.add(emptyLabel);
		}

		inventoryPanel.revalidate();
		inventoryPanel.repaint();
	}

	/**
	 * Adds an item to inventory by name Sets isInInventory to true and makes
	 * item/clickArea invisible
	 */
	private void addItemToInventoryByName(String itemName) {
		if (currentScene != null) {
			for (Item item : currentScene.getItems()) {
				if (item.getName().equals(itemName)) {
					// Set item to inventory
					item.setInInventory(true);

					// Make item mouseInvisible (handled in Item.shouldShowInScene())
					// The item is already hidden when isInInventory is true

					System.out.println("Added item to inventory: " + itemName);

					// Update inventory display
					updateInventory();
					gamePanel.repaint();
					return;
				}
			}
			System.err.println("Item not found in scene: " + itemName);
		}
	}

	/**
	 * Adds an item to the inventory display with tile image UPDATED: Uses theme
	 * colors
	 */
	private void addItemToInventory(Item item) {
		var c = ThemeManager.colors();

		JButton itemBtn = new JButton();
		itemBtn.setPreferredSize(new Dimension(60, 60));
		itemBtn.setBackground(c.getBackgroundInput());
		itemBtn.setFocusPainted(false);
		itemBtn.setBorder(BorderFactory.createLineBorder(c.getPrimary(), 2));
		itemBtn.setToolTipText(item.getName());

		// Try to load and display item image as tile
		if (item.getImageFilePath() != null && !item.getImageFilePath().isEmpty()) {
			try {
				// Use ResourcePathHelper to resolve path (handles both full paths and
				// filenames)
				java.io.File imageFile = ResourcePathHelper.findImageFile(item.getImageFilePath());
				if (imageFile == null) {
					// Fallback: try direct path
					imageFile = new java.io.File(item.getImageFilePath());
				}

				if (imageFile != null && imageFile.exists()) {
					ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
					Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
					itemBtn.setIcon(new ImageIcon(img));
				} else {
					// Fallback to text if image not found
					itemBtn.setText(item.getName());
					itemBtn.setForeground(c.getTextPrimary());
				}
			} catch (Exception e) {
				// Fallback to text if image loading fails
				itemBtn.setText(item.getName());
				itemBtn.setForeground(c.getTextPrimary());
			}
		} else {
			// No image, show text
			itemBtn.setText(item.getName());
			itemBtn.setForeground(c.getTextPrimary());
		}

		itemBtn.addActionListener(e -> {
			// Check if "Benutze" or "Gib" action is selected -> activate item-as-cursor
			// mode
			if (selectedAction != null && (selectedAction.equals("Benutze") || selectedAction.equals("Gib"))) {
				// Item-as-cursor mode
				if (selectedInventoryItem != null && selectedInventoryItem.equals(item)) {
					// Deselect
					selectedInventoryItem = null;
					selectedItem = null;
					itemBtn.setBorder(BorderFactory.createLineBorder(c.getPrimary(), 2));
					// Reset cursor
					updateCursorForAction(selectedAction);
					System.out.println("Item-as-cursor abgewählt: " + item.getName());
				} else {
					// Select for item-as-cursor
					selectedInventoryItem = item;
					selectedItem = item.getName();
					// Reset all borders
					for (Component comp : inventoryPanel.getComponents()) {
						if (comp instanceof JButton) {
							((JButton) comp).setBorder(BorderFactory.createLineBorder(c.getPrimary(), 2));
						}
					}
					// Highlight selected with different color for item-as-cursor mode
					itemBtn.setBorder(BorderFactory.createLineBorder(c.getSuccess(), 3));
					System.out.println("Item-as-cursor gewählt: " + item.getName());

					// Set custom cursor with item image
					setItemAsCursor(item);
				}
			} else {
				// Normal selection mode (legacy)
				if (selectedItem != null && selectedItem.equals(item.getName())) {
					// Deselect
					selectedItem = null;
					itemBtn.setBorder(BorderFactory.createLineBorder(c.getPrimary(), 2));
					System.out.println("Item abgewählt: " + item.getName());
				} else {
					// Select
					selectedItem = item.getName();
					// Reset all borders
					for (Component cc : inventoryPanel.getComponents()) {
						if (cc instanceof JButton) {
							((JButton) cc).setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
						}
					}
					// Highlight selected
					itemBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
					System.out.println("Item gewählt: " + item.getName());
				}
			}
		});

		inventoryPanel.add(itemBtn);
	}

	/**
	 * Sets a custom cursor using the item's image
	 */
	private void setItemAsCursor(Item item) {
		try {
			// Get item image
			String imagePath = item.getCurrentImagePath();
			if (imagePath == null || imagePath.isEmpty()) {
				System.out.println("No image for item-as-cursor, using default");
				updateCursorForAction(selectedAction);
				return;
			}

			File imageFile = new File(imagePath);
			if (!imageFile.exists()) {
				System.out.println("Item image not found: " + imagePath);
				updateCursorForAction(selectedAction);
				return;
			}

			// Load and scale image to cursor size (32x32 is typical)
			ImageIcon icon = new ImageIcon(imagePath);
			Image img = icon.getImage();
			Image scaledImg = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
			BufferedImage bufferedImg = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bufferedImg.createGraphics();
			g2d.drawImage(scaledImg, 0, 0, null);
			g2d.dispose();

			// Create custom cursor
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Cursor customCursor = toolkit.createCustomCursor(bufferedImg, new Point(16, 16), "ItemCursor");
			gamePanel.setCursor(customCursor);

			System.out.println("Set custom cursor for item: " + item.getName());

		} catch (Exception e) {
			System.err.println("Error creating custom cursor: " + e.getMessage());
			e.printStackTrace();
			updateCursorForAction(selectedAction);
		}
	}

	/**
	 * Rotate the background image by specified degrees
	 */
	public void rotateBackgroundImage(int degrees) {
		if (backgroundImage == null) {
			System.err.println("ERROR: No background image to rotate");
			return;
		}

		try {
			// Convert to BufferedImage
			java.awt.image.BufferedImage buffered = toBufferedImage(backgroundImage);

			// Calculate rotation
			double radians = Math.toRadians(degrees);
			double sin = Math.abs(Math.sin(radians));
			double cos = Math.abs(Math.cos(radians));

			int newWidth = (int) Math.floor(buffered.getWidth() * cos + buffered.getHeight() * sin);
			int newHeight = (int) Math.floor(buffered.getHeight() * cos + buffered.getWidth() * sin);

			java.awt.image.BufferedImage rotated = new java.awt.image.BufferedImage(newWidth, newHeight,
					java.awt.image.BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = rotated.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.translate((newWidth - buffered.getWidth()) / 2, (newHeight - buffered.getHeight()) / 2);
			g2d.rotate(radians, buffered.getWidth() / 2.0, buffered.getHeight() / 2.0);
			g2d.drawImage(buffered, 0, 0, null);
			g2d.dispose();

			// Save rotated image back to file
			String bgPath = currentScene.getCurrentBackgroundImagePath(progress);
			File imageFile = ResourcePathHelper.resolve("images/" + bgPath);
			if (imageFile.exists()) {
				String format = bgPath.substring(bgPath.lastIndexOf('.') + 1);
				javax.imageio.ImageIO.write(rotated, format, imageFile);
				System.out.println("Saved rotated image to: " + imageFile.getAbsolutePath());

				// Force flush any cached image data
				if (backgroundImage != null) {
					backgroundImage.flush();
				}

				// Small delay to ensure file system write completes
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					// Ignore
				}

				// Reload image to update display (this clears any cached versions)
				ImageIcon bgImage = new ImageIcon(imageFile.getAbsolutePath());
				backgroundImage = bgImage.getImage();
				gamePanel.repaint();
				gamePanel.revalidate();

				System.out.println("Image rotated by " + degrees + " degrees and display updated");
			}
		} catch (Exception e) {
			System.err.println("ERROR rotating image: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Flip the background image horizontally or vertically
	 */
	public void flipBackgroundImage(boolean horizontal) {
		if (backgroundImage == null) {
			System.err.println("ERROR: No background image to flip");
			return;
		}

		try {
			// Convert to BufferedImage
			java.awt.image.BufferedImage buffered = toBufferedImage(backgroundImage);

			int width = buffered.getWidth();
			int height = buffered.getHeight();

			java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(width, height,
					java.awt.image.BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = flipped.createGraphics();

			if (horizontal) {
				// Flip horizontally
				g2d.drawImage(buffered, width, 0, -width, height, null);
			} else {
				// Flip vertically
				g2d.drawImage(buffered, 0, height, width, -height, null);
			}

			g2d.dispose();

			// Save flipped image back to file
			String bgPath = currentScene.getCurrentBackgroundImagePath(progress);
			File imageFile = ResourcePathHelper.resolve("images/" + bgPath);
			if (imageFile.exists()) {
				String format = bgPath.substring(bgPath.lastIndexOf('.') + 1);
				javax.imageio.ImageIO.write(flipped, format, imageFile);
				System.out.println("Saved flipped image to: " + imageFile.getAbsolutePath());

				// Force flush any cached image data
				if (backgroundImage != null) {
					backgroundImage.flush();
				}

				// Small delay to ensure file system write completes
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					// Ignore
				}

				// Reload image to update display (this clears any cached versions)
				ImageIcon bgImage = new ImageIcon(imageFile.getAbsolutePath());
				backgroundImage = bgImage.getImage();
				gamePanel.repaint();
				gamePanel.revalidate();

				System.out.println(
						"Image flipped " + (horizontal ? "horizontally" : "vertically") + " and display updated");
			}
		} catch (Exception e) {
			System.err.println("ERROR flipping image: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Convert Image to BufferedImage
	 */
	private java.awt.image.BufferedImage toBufferedImage(Image img) {
		if (img instanceof java.awt.image.BufferedImage) {
			return (java.awt.image.BufferedImage) img;
		}

		// Create buffered image with transparency
		java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(img.getWidth(null),
				img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = buffered.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();

		return buffered;
	}

	/**
	 * Reload background image if conditions have changed and a different image
	 * should be shown
	 */
	private void reloadBackgroundImageIfNeeded() {
		if (currentScene == null) {
			return;
		}

		// Get the image path that should be displayed based on current conditions
		String newBgPath = currentScene.getCurrentBackgroundImagePath(progress);

		// Get the current background path (from first image or legacy)
		String currentBgPath = currentScene.getBackgroundImagePath();

		// If the path has changed, reload the background image
		if (newBgPath != null && !newBgPath.equals(currentBgPath)) {
			System.out.println("Reloading background image: " + newBgPath);
			ImageIcon bgImage = null;

			// Use ResourcePathHelper for robust path resolution
			File imageFile = ResourcePathHelper.findImageFile(newBgPath);
			if (imageFile != null) {
				bgImage = new ImageIcon(imageFile.getAbsolutePath());
			} else {
				java.net.URL imageUrl = ResourcePathHelper.findImageURL(newBgPath);
				if (imageUrl != null) {
					bgImage = new ImageIcon(imageUrl);
				}
			}

			if (bgImage != null) {
				// Scale image to fit and store it
				backgroundImage = bgImage.getImage().getScaledInstance(1024, 668, Image.SCALE_SMOOTH);
				gamePanel.repaint();
			}
		}
	}

	/**
	 * Auto-save that respects Editor mode - If EditorWindow is open and in "Load
	 * Defaults" mode → save to conditions.txt (default values) - Otherwise → save
	 * to progress.txt (normal gaming mode)
	 */
	private void autoSave() {
		if (editorWindow != null && editorWindow.isVisible() /* && editorWindow.isEditorModeLoadDefaults() */) {
			// Editor is open in "Load Defaults" mode → save to conditions.txt (default
			// values)
			Conditions.saveConditionsToFile();
			System.out.println("✓ Auto-saved to conditions.txt (Edit Mode)");
		} else {
			// Normal gaming mode OR editor in "Load Progress" mode → save to progress.txt
			progress.saveProgress();
			System.out.println("✓ Auto-saved to progress.txt (Game Mode)");
		}
	}

	public static void main(String[] args) {
		// Initialize ThemeManager before creating any Swing components
		main.ui.theme.ThemeManager.init();

		SwingUtilities.invokeLater(() -> new AdventureGame());
	}
}