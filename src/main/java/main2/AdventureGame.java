package main2;

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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
	private Point playerPosition;

	// Editor features
	private EditorWindow editorWindow;
	private DebugWindow debugWindow;
	private boolean showPaths = false; // Default OFF, only ON when editor is visible
	private boolean pathEditMode = false;
	private Point selectedPathPoint = null;
	private int selectedPathPointIndex = -1;
	private Item selectedItemForPointDrag = null; // Item whose point is being dragged
	private boolean addPointMode = false;
	private EditorWindow addPointModeEditor = null;
	private UniversalPointEditorDialog pointEditorDialog = null;
	private Item draggedItem = null;
	private ItemCorner draggedCorner = ItemCorner.NONE;
	private Point initialDragPoint = null;

	// Cursor blinking
	private javax.swing.Timer cursorBlinkTimer;
	private boolean cursorVisible = true;
	private KeyArea hoveredKeyArea = null;

	// Menu actions
	private static final String[] MENU_ACTIONS = { "Nimm", "Gib", "Ziehe", "Drücke", "Drehe", "Hebe", "Anschauen",
			"Sprich", "Benutze", "Gehe zu" };

	public AdventureGame() {
		setTitle("Point & Click Adventure");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1024, 768);
		setLocationRelativeTo(null);

		progress = new GameProgress();
		progress.loadProgress();

		playerPosition = new Point(400, 400);

		// Initialize editor
		editorWindow = new EditorWindow(this);

		// Initialize debug window
		debugWindow = new DebugWindow();

		// Set up condition change listener for inventory updates
		setupConditionListener();

		initUI();
		setupHotkeys();
		setupCursorBlinking();
		loadScene(progress.getCurrentScene());

		setVisible(true);
	}

	/**
	 * Sets up a listener to automatically update inventory and scene when isInInventory conditions change
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
		// ALT + E: Toggle Editor
		KeyStroke altE = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altE, "toggleEditor");
		getRootPane().getActionMap().put("toggleEditor", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleEditor();
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

		// ALT + D: Toggle Debug Window
		KeyStroke altD = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altD, "toggleDebug");
		getRootPane().getActionMap().put("toggleDebug", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				debugWindow.toggle();
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
		if(editorWindow == null) {
			editorWindow = new EditorWindow(this);
		}
		editorWindow.setVisible(!editorWindow.isVisible());
		if (editorWindow.isVisible()) {
			editorWindow.updateSceneInfo();
			editorWindow.refreshKeyAreaList();
			// Enable path visualization when editor opens
			showPaths = true;
		} else {
			// Disable path visualization when editor closes
			showPaths = false;
		}
		gamePanel.repaint();
		editorWindow.log("Editor toggled: " + editorWindow.isVisible());
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

	public void setPathEditMode(boolean enabled) {
		this.pathEditMode = enabled;
		if (editorWindow != null) {
			editorWindow.log("Path edit mode: " + (enabled ? "ON - Click to add points" : "OFF"));
		}
	}

	public void setAddPointMode(boolean enabled, EditorWindow editor) {
		this.addPointMode = enabled;
		this.addPointModeEditor = editor;

		// Always keep crosshair cursor
		gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void setPointEditorForAddMode(UniversalPointEditorDialog dialog) {
		this.pointEditorDialog = dialog;
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
						if (item != selectedSceneItem && item.isVisible()) {
							// Load item image (with condition-based path)
							String imagePath = item.getCurrentImagePath();
							File imageFile = new File(imagePath);

							if (imageFile.exists()) {
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
									int handleSize = 8;
									g2d.setColor(Color.GREEN);
									// Top-left
									g2d.fillRect(x - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
									// Top-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
									// Bottom-left
									g2d.fillRect(x - handleSize / 2, y + imgHeight - handleSize / 2, handleSize, handleSize);
									// Bottom-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y + imgHeight - handleSize / 2, handleSize, handleSize);
								}
								} catch (Exception e) {
									// Failed to load image
									System.err.println("Failed to load item image: " + imagePath + " - " + e.getMessage());
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
						String imagePath = selectedSceneItem.getCurrentImagePath();
						File imageFile = new File(imagePath);

						if (imageFile.exists()) {
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

								// Draw item boundary and drag points in editor mode with ORANGE color for selected
								if (showPaths) {
									g2d.setColor(new Color(255, 165, 0)); // Orange for selected
									g2d.setStroke(new BasicStroke(3)); // Thicker border
									g2d.drawRect(x, y, imgWidth, imgHeight);
									g2d.setFont(new Font("Arial", Font.BOLD, 12));
									g2d.drawString(selectedSceneItem.getName() + " [SELECTED]", x, y - 5);

									// Draw 4 corner drag points in ORANGE
									int handleSize = 10; // Bigger for selected
									g2d.setColor(new Color(255, 140, 0)); // Dark orange
									// Top-left
									g2d.fillRect(x - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
									// Top-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y - handleSize / 2, handleSize, handleSize);
									// Bottom-left
									g2d.fillRect(x - handleSize / 2, y + imgHeight - handleSize / 2, handleSize, handleSize);
									// Bottom-right
									g2d.fillRect(x + imgWidth - handleSize / 2, y + imgHeight - handleSize / 2, handleSize, handleSize);
								}
							} catch (Exception e) {
								// Failed to load image
								System.err.println("Failed to load selected item image: " + imagePath + " - " + e.getMessage());
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
					// Draw Item click area polygons OVER images
					// This allows dragging polygon points without moving the item
					g2d.setColor(new Color(0, 255, 255, 100)); // Cyan for items
					g2d.setStroke(new BasicStroke(2));
					Item selectedSceneItem = currentScene.getSelectedItem();
					for (Item item : currentScene.getItems()) {
						if (item.isVisible()) {
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
								int handleSize = isSelected ? 8 : 6; // Bigger points for selected
								List<Point> points = item.getClickAreaPoints();
								for (int i = 0; i < points.size(); i++) {
									Point p = points.get(i);
									g2d.fillRect(p.x - handleSize / 2, p.y - handleSize / 2, handleSize, handleSize);
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
							int handleSize = 8;
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
			}

			@Override
			public void mousePressed(MouseEvent e) {
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

		// Setup Drag & Drop for images
		gamePanel.setTransferHandler(new TransferHandler() {
			@Override
			public boolean canImport(TransferSupport support) {
				// Only accept file drops when editor is visible
				if (!editorWindow.isVisible()) {
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
					if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
					    fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
					    fileName.endsWith(".svg")) {

						// Get drop location
						Point dropPoint = support.getDropLocation().getDropPoint();

						// Create new item from dropped image
						handleImageDrop(imageFile, dropPoint);
						return true;
					}
				} catch (Exception e) {
					if (editorWindow != null) {
						editorWindow.log("ERROR dropping file: " + e.getMessage());
					}
					e.printStackTrace();
				}
				return false;
			}
		});

		add(gamePanel, BorderLayout.CENTER);

		// Menu panel
		menuPanel = new JPanel();
		menuPanel.setLayout(new FlowLayout());
		menuPanel.setPreferredSize(new Dimension(1024, 100));
		menuPanel.setBackground(new Color(50, 50, 50));

		for (String action : MENU_ACTIONS) {
			JButton btn = new JButton(action);
			btn.addActionListener(e -> selectAction(action));
			menuPanel.add(btn);
		}

		JButton saveBtn = new JButton("Speichern");
		saveBtn.addActionListener(e -> progress.saveProgress());
		menuPanel.add(saveBtn);

		JButton resetBtn = new JButton("Reset");
		resetBtn.addActionListener(e -> resetGame());
		menuPanel.add(resetBtn);

		// Inventory panel
		inventoryPanel = new JPanel();
		inventoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		inventoryPanel.setPreferredSize(new Dimension(1024, 60));
		inventoryPanel.setBackground(new Color(40, 40, 40));
		inventoryPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.GRAY),
				"Inventar",
				javax.swing.border.TitledBorder.LEFT,
				javax.swing.border.TitledBorder.TOP,
				new Font("Arial", Font.BOLD, 12),
				Color.WHITE));

		updateInventory();

		// Combined south panel
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(inventoryPanel, BorderLayout.NORTH);
		southPanel.add(menuPanel, BorderLayout.SOUTH);

		add(southPanel, BorderLayout.SOUTH);
	}

	private void handleMouseHover(Point point) {
		if (currentScene == null) {
			hoverTextLabel.setText(" ");
			hoveredKeyArea = null;
			gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			return;
		}

		// Check if hovering over an Item first
		Item hoveredItem = currentScene.getItemAt(point);
		if (hoveredItem != null && hoveredItem.isVisible()) {
			String displayText = hoveredItem.getHoverDisplayText();
			if (displayText != null && !displayText.isEmpty()) {
				hoverTextLabel.setText(displayText);
			} else {
				hoverTextLabel.setText(hoveredItem.getName());
			}
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
				gamePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
		}

		if (hoveredArea != null) {
			// Get hover display text based on conditions
			String displayText = hoveredArea.getHoverDisplayText();

			if (displayText != null && !displayText.isEmpty()) {
				hoverTextLabel.setText(displayText);
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

		// Check if clicking on Item click area points first
		for (Item item : currentScene.getItems()) {
			if (!item.isVisible()) continue;

			List<Point> points = item.getClickAreaPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 10) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = item;

					// Auto-select in editor window
					if (editorWindow != null) {
						editorWindow.selectItem(item);
						editorWindow.log("Selected Item point " + i + " from " + item.getName() + " at (" + p.x + "," + p.y + ")");
					}
					return;
				}
			}
		}

		// Check if clicking on KeyArea points
		for (KeyArea area : currentScene.getKeyAreas()) {
			List<Point> points = area.getPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 10) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = null;

					// Auto-select in editor window
					if (editorWindow != null) {
						editorWindow.selectKeyArea(area);
						editorWindow.selectPoint(i);
						editorWindow.log("Selected KeyArea point " + i + " from " + area.getName() + " at (" + p.x + ","
								+ p.y + ")");
					}
					return;
				}
			}
		}

		// Check if clicking on Path points
		for (Path path : currentScene.getPaths()) {
			List<Point> points = path.getPoints();
			for (int i = 0; i < points.size(); i++) {
				Point p = points.get(i);
				if (p.distance(clickPoint) < 10) {
					selectedPathPoint = p;
					selectedPathPointIndex = i;
					selectedItemForPointDrag = null;
					if (editorWindow != null) {
						editorWindow.log("Selected path point " + i + " at (" + p.x + "," + p.y + ")");
					}
					return;
				}
			}
		}
	}

	private void handlePathPointDrag(Point dragPoint) {
		if (selectedPathPoint != null) {
			selectedPathPoint.x = dragPoint.x;
			selectedPathPoint.y = dragPoint.y;

			// Update Item click area polygon if dragging Item point
			if (selectedItemForPointDrag != null) {
				selectedItemForPointDrag.updateClickAreaPolygon();
				selectedItemForPointDrag.setHasCustomClickArea(true);

				if (editorWindow != null) {
					editorWindow.log("Dragging Item point " + selectedPathPointIndex + " to (" + dragPoint.x + "," + dragPoint.y + ")");
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
							editorWindow.updatePointCoordinates(area, selectedPathPointIndex, selectedPathPoint.x,
									selectedPathPoint.y);
						}
						break;
					}
				}
			}

			gamePanel.repaint();
		}
	}

	private void handlePathPointRelease() {
		if (selectedPathPoint != null && editorWindow != null) {
			editorWindow.log("Moved point to (" + selectedPathPoint.x + "," + selectedPathPoint.y + ")");

			// Auto-save Item when releasing Item point
			if (selectedItemForPointDrag != null) {
				try {
					ItemSaver.saveItemByName(selectedItemForPointDrag);
					editorWindow.log("✓ Auto-saved Item: " + selectedItemForPointDrag.getName());
				} catch (Exception e) {
					editorWindow.log("ERROR saving Item: " + e.getMessage());
				}
				editorWindow.autoSaveCurrentScene();
			}
			// Auto-save scene when releasing KeyArea point
			else {
				editorWindow.autoSaveCurrentScene();
			}
		}
		selectedPathPoint = null;
		selectedPathPointIndex = -1;
		selectedItemForPointDrag = null;
	}

	private void handleItemPress(Point clickPoint) {
		if (currentScene == null)
			return;

		int handleSize = 8;
		int handleTolerance = 6; // Extra pixels for easier clicking

		// Check if clicking on an item corner or body
		for (Item item : currentScene.getItems()) {
			if (item.isVisible()) {
				Point pos = item.getPosition();
				int imgWidth = item.getWidth();
				int imgHeight = item.getHeight();

				// Calculate corners
				int x = pos.x - imgWidth / 2;
				int y = pos.y - imgHeight / 2;

				// Check corners first (in editor mode)
				if (showPaths) {
					// Top-left
					if (Math.abs(clickPoint.x - x) <= handleSize / 2 + handleTolerance &&
					    Math.abs(clickPoint.y - y) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.TOP_LEFT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging top-left corner of " + item.getName());
						}
						return;
					}
					// Top-right
					if (Math.abs(clickPoint.x - (x + imgWidth)) <= handleSize / 2 + handleTolerance &&
					    Math.abs(clickPoint.y - y) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.TOP_RIGHT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging top-right corner of " + item.getName());
						}
						return;
					}
					// Bottom-left
					if (Math.abs(clickPoint.x - x) <= handleSize / 2 + handleTolerance &&
					    Math.abs(clickPoint.y - (y + imgHeight)) <= handleSize / 2 + handleTolerance) {
						draggedItem = item;
						draggedCorner = ItemCorner.BOTTOM_LEFT;
						initialDragPoint = new Point(clickPoint.x, clickPoint.y);
						if (editorWindow != null) {
							editorWindow.log("Dragging bottom-left corner of " + item.getName());
						}
						return;
					}
					// Bottom-right
					if (Math.abs(clickPoint.x - (x + imgWidth)) <= handleSize / 2 + handleTolerance &&
					    Math.abs(clickPoint.y - (y + imgHeight)) <= handleSize / 2 + handleTolerance) {
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
				if (clickPoint.x >= x && clickPoint.x <= x + imgWidth &&
				    clickPoint.y >= y && clickPoint.y <= y + imgHeight) {
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
		if (draggedItem == null) return;

		if (draggedCorner == ItemCorner.NONE) {
			// Move entire item
			draggedItem.setPosition(newPosition.x, newPosition.y);
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

		gamePanel.repaint();
	}

	private void handleItemRelease() {
		if (draggedItem != null && editorWindow != null) {
			Point pos = draggedItem.getPosition();
			if (draggedCorner == ItemCorner.NONE) {
				editorWindow.log("Moved item " + draggedItem.getName() + " to (" + pos.x + ", " + pos.y + ")");
			} else {
				editorWindow.log("Resized item " + draggedItem.getName() + " to " + draggedItem.getWidth() + "x" + draggedItem.getHeight());
			}

			// Auto-save both the item and the scene
			try {
				ItemSaver.saveItemByName(draggedItem);
				editorWindow.autoSaveCurrentScene();
			} catch (Exception e) {
				editorWindow.log("ERROR saving item: " + e.getMessage());
			}
		}
		draggedItem = null;
		draggedCorner = ItemCorner.NONE;
		initialDragPoint = null;
	}

	private void handleImageDrop(File imageFile, Point dropPoint) {
		if (currentScene == null) {
			JOptionPane.showMessageDialog(this, "No scene loaded!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Ask for item name
		String itemName = JOptionPane.showInputDialog(this,
			"Enter item name for image:\n" + imageFile.getName(),
			"Create Item from Image",
			JOptionPane.PLAIN_MESSAGE);

		if (itemName == null || itemName.trim().isEmpty()) {
			return; // User cancelled
		}

		itemName = itemName.trim();

		// Check if item already exists
		File itemFile = new File("resources/items/" + itemName + ".txt");
		if (itemFile.exists()) {
			int overwrite = JOptionPane.showConfirmDialog(this,
				"Item '" + itemName + "' already exists!\nOverwrite?",
				"Item Exists",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if (overwrite != JOptionPane.YES_OPTION) {
				return;
			}
		}

		try {
			// Copy image to resources/images/items/ directory
			File itemsImagesDir = new File("resources/images/items");
			if (!itemsImagesDir.exists()) {
				itemsImagesDir.mkdirs();
			}

			String imageFileName = imageFile.getName();
			File targetImageFile = new File(itemsImagesDir, imageFileName);

			// Copy file
			java.nio.file.Files.copy(
				imageFile.toPath(),
				targetImageFile.toPath(),
				java.nio.file.StandardCopyOption.REPLACE_EXISTING
			);

			// Create new item
			Item newItem = new Item(itemName);
			newItem.setImageFileName(imageFileName);
			newItem.setImageFilePath("resources/images/items/" + imageFileName);
			newItem.setPosition(dropPoint.x, dropPoint.y);
			newItem.setInInventory(false);

			// Get original image size
			ImageIcon icon = new ImageIcon(targetImageFile.getAbsolutePath());
			int originalWidth = icon.getIconWidth();
			int originalHeight = icon.getIconHeight();

			// Set size (use original size or default if invalid)
			if (originalWidth > 0 && originalHeight > 0) {
				newItem.setSize(originalWidth, originalHeight);
			} else {
				newItem.setSize(100, 100); // Default size
			}

			// Save item
			ItemSaver.saveItemByName(newItem);

			// Add to current scene
			currentScene.addItem(newItem);
			editorWindow.autoSaveCurrentScene();

			// Repaint to show the item
			gamePanel.repaint();

			editorWindow.log("Created item '" + itemName + "' from dropped image at (" + dropPoint.x + ", " + dropPoint.y + ")");

			// Open ItemEditor with the new item selected
			openItemEditorWithItem(itemName);

		} catch (Exception e) {
			editorWindow.log("ERROR creating item from image: " + e.getMessage());
			JOptionPane.showMessageDialog(this,
				"Error creating item:\n" + e.getMessage(),
				"Error",
				JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private void openItemEditorWithItem(String itemName) {
		// Open ItemEditor (non-modal so it won't block)
		SwingUtilities.invokeLater(() -> {
			ItemEditorDialog dialog = new ItemEditorDialog(editorWindow);
			dialog.setVisible(true);
			// Auto-select the newly created item
			dialog.selectItemByName(itemName);
		});
	}

	private void selectAction(String action) {
		selectedAction = action;
		System.out.println("Aktion gewählt: " + action);

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
		// Check if Universal Point Editor is in add point mode
		if (pointEditorDialog != null && addPointMode) {
			pointEditorDialog.addPointAtPosition(clickPoint.x, clickPoint.y);
			return;
		}

		// Check if PointsManager is in add point mode
		if (editorWindow != null && editorWindow.getPointsManager() != null) {
			PointsManagerDialog pm = editorWindow.getPointsManager();
			if (pm.isAddingPoint()) {
				pm.addPointAtPosition(clickPoint.x, clickPoint.y);
				return;
			}
		}

		// Add point mode takes priority (legacy for old editor)
		if (addPointMode && addPointModeEditor != null) {
			addPointModeEditor.addPointAtPosition(clickPoint.x, clickPoint.y);
			return;
		}

		if (currentScene == null)
			return;

		// Check if clicked on a KeyArea
		KeyArea clickedArea = currentScene.getKeyAreaAt(clickPoint);

		if (clickedArea != null && selectedAction != null) {
			// Perform action on KeyArea
			String result = clickedArea.performAction(selectedAction, progress);

			// Log action to debug window
			debugWindow.logAction(selectedAction, "KeyArea: " + clickedArea.getName(), result);

			if (result != null) {
				if (result != null) {
					if (result.startsWith("##load")) {
						// Load new scene
						String sceneName = result.substring(6).trim();
						loadScene(sceneName);
					} else if (result.startsWith("#Dialog:")) {
						// Show dialog by name - extract dialog name after the 6 dashes
						String dialogLine = result.substring(8).trim();
						// Remove leading dashes (------dialogname.txt)
						while (dialogLine.startsWith("-")) {
							dialogLine = dialogLine.substring(1);
						}
						// Remove .txt if present
						if (dialogLine.endsWith(".txt")) {
							dialogLine = dialogLine.substring(0, dialogLine.length() - 4);
						}
						showDialog(dialogLine.trim());
					} else if (result.startsWith("#SetBoolean:")) {
						// Set boolean variable in Conditions
						String[] parts = result.substring(12).split("=");
						if (parts.length == 2) {
							Conditions.setCondition(parts[0].trim(), Boolean.parseBoolean(parts[1].trim()));
							updateInventory(); // Update inventory when conditions change
						}
					} else if (result.startsWith("#AddItem:")) {
						// Add item to inventory
						String itemName = result.substring(9).trim();
						addItemToInventoryByName(itemName);
					} else if (result.startsWith("#Process:")) {
						// Execute process
						String processName = result.substring(9).trim();
						System.out.println("Starting process: " + processName);
						processExecutor.executeProcess(processName);
					}
				}
			}

			selectedAction = null;
			// Reset cursor to default
			updateCursorForAction(null);
		} else if (selectedAction == null) {
			// Move player (simple implementation)
			playerPosition = clickPoint;
			gamePanel.repaint();
		}
	}

	public void loadScene(String sceneName) {
		try {
			currentScene = SceneLoader.loadScene(sceneName, progress);
			progress.setCurrentScene(sceneName);

			// Log scene change to debug window
			debugWindow.logSceneChange(sceneName);

			// Load background image
			String bgPath = currentScene.getBackgroundImagePath();
			ImageIcon bgImage = null;

			// Try loading from file system first
			File imageFile = new File("resources/images/" + bgPath);
			if (imageFile.exists()) {
				bgImage = new ImageIcon(imageFile.getAbsolutePath());
				System.out.println("Bild geladen von: " + imageFile.getAbsolutePath());
			} else {
				// Try loading from classpath
				java.net.URL imageUrl = getClass().getResource("/resources/images/" + bgPath);
				if (imageUrl != null) {
					bgImage = new ImageIcon(imageUrl);
					System.out.println("Bild geladen von Classpath");
				} else {
					System.err.println("Hintergrundbild nicht gefunden: " + bgPath);
					System.err.println("Gesucht in: " + imageFile.getAbsolutePath());
					System.err.println("Aktuelles Verzeichnis: " + new File(".").getAbsolutePath());
				}
			}

			if (bgImage != null) {
				// Scale image to fit and store it
				backgroundImage = bgImage.getImage().getScaledInstance(1024, 668, Image.SCALE_SMOOTH);
			} else {
				// Clear background
				backgroundImage = null;
				System.err.println("Bild nicht gefunden: " + bgPath);
			}

			// Trigger repaint to show new background
			gamePanel.repaint();

			// Update inventory
			updateInventory();

			System.out.println("Scene geladen: " + sceneName);
		} catch (Exception e) {
			System.err.println("Fehler beim Laden der Scene: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Reloads all scenes (actually just reloads the current scene since scenes are loaded on-demand)
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
			errorMsg.append("❌ Dialog nicht gefunden: ").append(dialogName).append("\n\n");
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

		// Show all items that are in inventory
		if (currentScene != null) {
			for (Item item : currentScene.getItems()) {
				if (item.isInInventory()) {
					addItemToInventory(item);
				}
			}
		}

		// If empty, show message
		if (inventoryPanel.getComponentCount() == 0) {
			JLabel emptyLabel = new JLabel("(Keine Items)");
			emptyLabel.setForeground(Color.GRAY);
			inventoryPanel.add(emptyLabel);
		}

		inventoryPanel.revalidate();
		inventoryPanel.repaint();
	}

	/**
	 * Adds an item to inventory by name
	 * Sets isInInventory to true and makes item/clickArea invisible
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
	 * Adds an item to the inventory display with tile image
	 */
	private void addItemToInventory(Item item) {
		JButton itemBtn = new JButton();
		itemBtn.setPreferredSize(new Dimension(60, 60));
		itemBtn.setBackground(new Color(80, 80, 80));
		itemBtn.setFocusPainted(false);
		itemBtn.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
		
		// Try to load and display item image as tile
		if (item.getImageFilePath() != null && !item.getImageFilePath().isEmpty()) {
			try {
				java.io.File imageFile = new java.io.File(item.getImageFilePath());
				if (imageFile.exists()) {
					ImageIcon icon = new ImageIcon(item.getImageFilePath());
					Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
					itemBtn.setIcon(new ImageIcon(img));
				} else {
					// Fallback to text if image not found
					itemBtn.setText(item.getName());
					itemBtn.setForeground(Color.WHITE);
				}
			} catch (Exception e) {
				// Fallback to text if image loading fails
				itemBtn.setText(item.getName());
				itemBtn.setForeground(Color.WHITE);
			}
		} else {
			// No image, show text
			itemBtn.setText(item.getName());
			itemBtn.setForeground(Color.WHITE);
		}
		
		itemBtn.setToolTipText(item.getName());

		itemBtn.addActionListener(e -> {
			if (selectedItem != null && selectedItem.equals(item.getName())) {
				// Deselect
				selectedItem = null;
				itemBtn.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
				System.out.println("Item abgewählt: " + item.getName());
			} else {
				// Select
				selectedItem = item.getName();
				// Reset all borders
				for (Component c : inventoryPanel.getComponents()) {
					if (c instanceof JButton) {
						((JButton) c).setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
					}
				}
				// Highlight selected
				itemBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
				System.out.println("Item gewählt: " + item.getName());
			}
		});

		inventoryPanel.add(itemBtn);
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

			java.awt.image.BufferedImage rotated = new java.awt.image.BufferedImage(
					newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = rotated.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.translate((newWidth - buffered.getWidth()) / 2, (newHeight - buffered.getHeight()) / 2);
			g2d.rotate(radians, buffered.getWidth() / 2.0, buffered.getHeight() / 2.0);
			g2d.drawImage(buffered, 0, 0, null);
			g2d.dispose();

			// Save rotated image back to file
			String bgPath = currentScene.getBackgroundImagePath();
			File imageFile = new File("resources/images/" + bgPath);
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

			java.awt.image.BufferedImage flipped = new java.awt.image.BufferedImage(
					width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);

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
			String bgPath = currentScene.getBackgroundImagePath();
			File imageFile = new File("resources/images/" + bgPath);
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

				System.out.println("Image flipped " + (horizontal ? "horizontally" : "vertically") + " and display updated");
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
		java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
				img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = buffered.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();

		return buffered;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new AdventureGame());
	}
}