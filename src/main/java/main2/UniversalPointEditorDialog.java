package main2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Universal Point Editor for KeyAreas, Items, and Characters
 * Works with any object that has a list of points defining a polygon
 */
public class UniversalPointEditorDialog extends JDialog {
	private EditorWindow parent;
	private AdventureGame game;
	private String objectName;
	private List<Point> points;
	private Runnable onSave;
	private Item editingItem = null; // Reference to item being edited (if any)

	private JList<String> pointList;
	private DefaultListModel<String> pointListModel;
	private JTextField xField;
	private JTextField yField;
	private int selectedPointIndex = -1;

	/**
	 * Constructor for KeyArea point editing
	 */
	public UniversalPointEditorDialog(EditorWindow parent, KeyArea keyArea) {
		this(parent, keyArea.getName(), keyArea.getPoints(), () -> {
			keyArea.updatePolygon();
			parent.autoSaveCurrentScene();
			parent.getGame().repaintGamePanel();
		});
	}

	/**
	 * Constructor for Item point editing
	 */
	public UniversalPointEditorDialog(EditorWindow parent, Item item) {
		this(parent, item.getName(), ensureItemHasPoints(item), () -> {
			// Mark as custom when saving
			item.setHasCustomClickArea(true);
			item.updateClickAreaPolygon();
			try {
				ItemSaver.saveItemByName(item);
			} catch (Exception e) {
				parent.log("ERROR saving item: " + e.getMessage());
			}
			parent.autoSaveCurrentScene();
			parent.getGame().repaintGamePanel();
		});

		this.editingItem = item;
		System.out.println("UniversalPointEditorDialog created for Item: " + item.getName());
		System.out.println("  Click area points: " + item.getClickAreaPoints().size());
		System.out.println("  Has custom click area: " + item.hasCustomClickArea());
		for (int i = 0; i < item.getClickAreaPoints().size(); i++) {
			Point p = item.getClickAreaPoints().get(i);
			System.out.println("    Point " + i + ": (" + p.x + ", " + p.y + ")");
		}
	}

	/**
	 * Helper method to ensure item has click area points before passing to constructor
	 */
	private static List<Point> ensureItemHasPoints(Item item) {
		if (item.getClickAreaPoints().isEmpty()) {
			// If hasCustomClickArea is set but points are empty, something went wrong
			// Reset the flag and create default points
			if (item.hasCustomClickArea()) {
				System.out.println("WARNING: Item has hasCustomClickArea=true but no points! Resetting flag.");
				item.setHasCustomClickArea(false);
			}
			item.createClickAreaFromImage();
		}
		return item.getClickAreaPoints();
	}

	/**
	 * Generic constructor
	 */
	private UniversalPointEditorDialog(EditorWindow parent, String objectName, List<Point> points, Runnable onSave) {
		super(parent, "Point Editor - " + objectName, false);
		this.parent = parent;
		this.game = parent.getGame();
		this.objectName = objectName;
		this.points = points;
		this.onSave = onSave;

		setSize(500, 600);
		setLocationRelativeTo(parent);

		// Add window listener to disable "Click to add" mode when dialog closes
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				disableClickToAddMode();
			}
		});

		initUI();
		loadPoints();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("📍 Point Editor: " + objectName);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Center: Point list
		JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
		centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		JLabel listLabel = new JLabel("Points (Polygon vertices):");
		centerPanel.add(listLabel, BorderLayout.NORTH);

		pointListModel = new DefaultListModel<>();
		pointList = new JList<>(pointListModel);
		pointList.setFont(new Font("Monospaced", Font.PLAIN, 12));
		pointList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				selectPoint();
			}
		});

		JScrollPane scrollPane = new JScrollPane(pointList);
		centerPanel.add(scrollPane, BorderLayout.CENTER);

		// Edit panel
		JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		editPanel.add(new JLabel("X:"));
		xField = new JTextField(6);
		editPanel.add(xField);
		editPanel.add(new JLabel("Y:"));
		yField = new JTextField(6);
		editPanel.add(yField);

		JButton updateBtn = new JButton("Update");
		updateBtn.addActionListener(e -> updatePoint());
		editPanel.add(updateBtn);

		centerPanel.add(editPanel, BorderLayout.SOUTH);
		add(centerPanel, BorderLayout.CENTER);

		// Bottom: Buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton addBtn = new JButton("➕ Add Point");
		addBtn.addActionListener(e -> addPoint());
		bottomPanel.add(addBtn);

		JButton deleteBtn = new JButton("🗑️ Delete Point");
		deleteBtn.addActionListener(e -> deletePoint());
		bottomPanel.add(deleteBtn);

		JButton addModeBtn = new JButton("🖱️ Click to Add (Autosave)");
		addModeBtn.setToolTipText("Click on the game screen to add points");
		addModeBtn.addActionListener(e -> enableClickToAddMode());
		bottomPanel.add(addModeBtn);

		JButton saveBtn = new JButton("💾 Save");
		saveBtn.addActionListener(e -> save());
		bottomPanel.add(saveBtn);

		JButton closeBtn = new JButton("✓ Close");
		closeBtn.addActionListener(e -> {
			disableClickToAddMode();
			dispose();
		});
		bottomPanel.add(closeBtn);

		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadPoints() {
		pointListModel.clear();
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			pointListModel.addElement(String.format("Point %d: (%d, %d)", i, p.x, p.y));
		}
		parent.log("Loaded " + points.size() + " points for " + objectName);
	}

	private void selectPoint() {
		selectedPointIndex = pointList.getSelectedIndex();
		if (selectedPointIndex >= 0 && selectedPointIndex < points.size()) {
			Point p = points.get(selectedPointIndex);
			xField.setText(String.valueOf(p.x));
			yField.setText(String.valueOf(p.y));
		}
	}

	private void updatePoint() {
		if (selectedPointIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a point first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			int x = Integer.parseInt(xField.getText());
			int y = Integer.parseInt(yField.getText());

			points.get(selectedPointIndex).setLocation(x, y);
			loadPoints();
			pointList.setSelectedIndex(selectedPointIndex);

			// Mark item as having custom click area and update polygon
			if (editingItem != null) {
				editingItem.setHasCustomClickArea(true);
				editingItem.updateClickAreaPolygon();

				// CRITICAL: Also update the scene item
				Scene currentScene = game.getCurrentScene();
				if (currentScene != null) {
					for (Item sceneItem : currentScene.getItems()) {
						if (sceneItem.getName().equals(editingItem.getName())) {
							sceneItem.getClickAreaPoints().clear();
							for (Point p : editingItem.getClickAreaPoints()) {
								sceneItem.getClickAreaPoints().add(new Point(p.x, p.y));
							}
							sceneItem.setHasCustomClickArea(true);
							sceneItem.updateClickAreaPolygon();
							break;
						}
					}
				}
			}

			parent.log("Updated point " + selectedPointIndex + " to (" + x + ", " + y + ")");
			game.repaintGamePanel();

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Invalid coordinates!", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addPoint() {
		String input = JOptionPane.showInputDialog(this, "Enter coordinates (x,y):", "100,100");
		if (input != null) {
			try {
				String[] parts = input.split(",");
				int x = Integer.parseInt(parts[0].trim());
				int y = Integer.parseInt(parts[1].trim());

				points.add(new Point(x, y));
				loadPoints();

				// Mark item as having custom click area and update polygon
				if (editingItem != null) {
					editingItem.setHasCustomClickArea(true);
					editingItem.updateClickAreaPolygon();

					// CRITICAL: Also update the scene item
					Scene currentScene = game.getCurrentScene();
					if (currentScene != null) {
						for (Item sceneItem : currentScene.getItems()) {
							if (sceneItem.getName().equals(editingItem.getName())) {
								sceneItem.getClickAreaPoints().clear();
								for (Point p : editingItem.getClickAreaPoints()) {
									sceneItem.getClickAreaPoints().add(new Point(p.x, p.y));
								}
								sceneItem.setHasCustomClickArea(true);
								sceneItem.updateClickAreaPolygon();
								break;
							}
						}
					}
				}

				parent.log("Added point: (" + x + ", " + y + ")");
				game.repaintGamePanel();

			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Invalid format! Use: x,y", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void deletePoint() {
		if (selectedPointIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a point first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (points.size() <= 3) {
			JOptionPane.showMessageDialog(this, "Cannot delete - minimum 3 points required!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "Delete point " + selectedPointIndex + "?",
				"Confirm Delete", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			points.remove(selectedPointIndex);
			loadPoints();

			// Mark item as having custom click area and update polygon
			if (editingItem != null) {
				editingItem.setHasCustomClickArea(true);
				editingItem.updateClickAreaPolygon();

				// CRITICAL: Also update the scene item
				Scene currentScene = game.getCurrentScene();
				if (currentScene != null) {
					for (Item sceneItem : currentScene.getItems()) {
						if (sceneItem.getName().equals(editingItem.getName())) {
							sceneItem.getClickAreaPoints().clear();
							for (Point p : editingItem.getClickAreaPoints()) {
								sceneItem.getClickAreaPoints().add(new Point(p.x, p.y));
							}
							sceneItem.setHasCustomClickArea(true);
							sceneItem.updateClickAreaPolygon();
							break;
						}
					}
				}
			}

			parent.log("Deleted point " + selectedPointIndex);
			selectedPointIndex = -1;
			xField.setText("");
			yField.setText("");
			game.repaintGamePanel();
		}
	}

	private void enableClickToAddMode() {
		parent.log("Click to Add mode enabled - click on the game screen to add points");
		game.setAddPointMode(true, parent);

		// Store reference to this dialog so we can add points
		game.setPointEditorForAddMode(this);

		JOptionPane.showMessageDialog(this,
				"Click on the game screen to add points.\nClick 'Save' when done.",
				"Click to Add Mode", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Disable Click to Add mode when dialog closes
	 */
	private void disableClickToAddMode() {
		game.setAddPointMode(false, null);
		game.setPointEditorForAddMode(null);
		parent.log("Click to Add mode disabled");
	}

	/**
	 * Called by AdventureGame when a point is clicked in add mode
	 */
	public void addPointAtPosition(int x, int y) {
		points.add(new Point(x, y));
		System.out.println("UniversalPointEditorDialog.addPointAtPosition: Added point (" + x + ", " + y + ")");
		System.out.println("  editingItem: " + (editingItem != null ? editingItem.getName() : "null"));
		System.out.println("  points.size(): " + points.size());

		// Mark item as having custom click area and update polygon immediately
		if (editingItem != null) {
			editingItem.setHasCustomClickArea(true);
			editingItem.updateClickAreaPolygon();
			System.out.println("  Updated item polygon, npoints: " + editingItem.getClickAreaPolygon().npoints);

			// IMPORTANT: Also update the item in the current scene!
			// The editingItem might be a different object than the one in the scene
			Scene currentScene = game.getCurrentScene();
			if (currentScene != null) {
				for (Item sceneItem : currentScene.getItems()) {
					if (sceneItem.getName().equals(editingItem.getName())) {
						// Copy the points to the scene item
						sceneItem.getClickAreaPoints().clear();
						for (Point p : editingItem.getClickAreaPoints()) {
							sceneItem.getClickAreaPoints().add(new Point(p.x, p.y));
						}
						sceneItem.setHasCustomClickArea(true);
						sceneItem.updateClickAreaPolygon();
						System.out.println("  Updated scene item polygon!");
						break;
					}
				}
			}
		}

		loadPoints();
		parent.repaintGamePanel();
		parent.log("Added point at (" + x + ", " + y + ")");
	}

	private void save() {
		// Before calling onSave, ensure scene item is updated
		if (editingItem != null) {
			Scene currentScene = game.getCurrentScene();
			if (currentScene != null) {
				for (Item sceneItem : currentScene.getItems()) {
					if (sceneItem.getName().equals(editingItem.getName())) {
						// Copy all points to scene item
						sceneItem.getClickAreaPoints().clear();
						for (Point p : editingItem.getClickAreaPoints()) {
							sceneItem.getClickAreaPoints().add(new Point(p.x, p.y));
						}
						sceneItem.setHasCustomClickArea(true);
						sceneItem.updateClickAreaPolygon();
						parent.log("Synchronized points with scene item: " + sceneItem.getName());
						break;
					}
				}
			}
		}

		if (onSave != null) {
			onSave.run();
		}
		parent.log("✓ Points saved for " + objectName);
		JOptionPane.showMessageDialog(this, "Points saved successfully!", "Success",
				JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void dispose() {
		// Disable click to add mode when dialog closes
		game.setAddPointMode(false, null);
		game.setPointEditorForAddMode(null);
		super.dispose();
	}
}
