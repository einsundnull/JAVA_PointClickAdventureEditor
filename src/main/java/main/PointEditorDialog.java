package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Universal Point Editor Dialog Allows editing points with x, y, z coordinates
 * Auto-saves on every change
 */
public class PointEditorDialog extends JDialog {
	private EditorMainSimple parent;
	private AdventureGame game;
	private JPanel pointsListContainer;
	private List<Point> points;
	private List<PointPanel> pointPanels;
	private PointEditorCallback callback;
	private String itemName; // Track item name for title
	private String pointType; // "CustomClickArea", "MovingRange", "Path"
	private int selectedPointIndex = -1;

	// Track minimized windows
	private List<java.awt.Window> minimizedWindows;

	/**
	 * Callback interface for point changes
	 */
	public interface PointEditorCallback {
		void onPointsChanged(List<Point> points);
	}

	// Legacy constructors for EditorMain
	public PointEditorDialog(EditorMain parent, List<Point> initialPoints, PointEditorCallback callback) {
		this(parent, initialPoints, callback, null);
	}

	public PointEditorDialog(EditorMain parent, List<Point> initialPoints, PointEditorCallback callback,
			String itemName) {
		super(parent, itemName != null ? "Point Editor: " + itemName : "Point Editor", false);
		this.parent = null; // EditorMain not supported in Simple mode
		this.game = parent.getGame();
		this.points = new ArrayList<>(initialPoints);
		this.pointPanels = new ArrayList<>();
		this.callback = callback;
		this.itemName = itemName;
		this.pointType = "Item";
		this.minimizedWindows = new ArrayList<>();

		setSize(600, 700);
		setLocationRelativeTo(parent);
		setAlwaysOnTop(true);

		initUI();
		loadPoints();
		minimizeOtherWindows();

		// IMPORTANT: Make window visible!
		setVisible(true);
		toFront();
		requestFocus();
	}

	// New constructors for EditorMainSimple
	public PointEditorDialog(EditorMainSimple parent, List<Point> initialPoints, PointEditorCallback callback) {
		this(parent, initialPoints, callback, null, "Item");
	}

	public PointEditorDialog(EditorMainSimple parent, List<Point> initialPoints, PointEditorCallback callback,
			String itemName) {
		this(parent, initialPoints, callback, itemName, "Item");
	}

	public PointEditorDialog(EditorMainSimple parent, List<Point> initialPoints, PointEditorCallback callback,
			String itemName, String pointType) {
		super(parent, itemName != null ? "Point Editor: " + itemName + " - " + pointType : "Point Editor", false); // Non-modal

		System.out.println("=== PointEditorDialog Constructor START ===");
		System.out.println("  Item: " + itemName);
		System.out.println("  Type: " + pointType);
		System.out.println("  Initial points: " + initialPoints.size());

		this.parent = parent;
		this.game = parent.getGame();
		this.points = new ArrayList<>(initialPoints);
		this.pointPanels = new ArrayList<>();
		this.callback = callback;
		this.itemName = itemName;
		this.pointType = pointType;
		this.minimizedWindows = new ArrayList<>();

		System.out.println("  Setting size...");
		setSize(600, 700); // Increased size for better visibility

		System.out.println("  Setting location...");
		setLocationRelativeTo(parent);

		System.out.println("  Setting always on top...");
		setAlwaysOnTop(true); // Always on top!

		System.out.println("  Initializing UI...");
		initUI();

		System.out.println("  Loading points...");
		loadPoints();

		System.out.println("  Minimizing other windows...");
		minimizeOtherWindows();

		System.out.println("  Enabling Add by Click mode...");
		enableAddByClickMode();

		System.out.println("  Making window VISIBLE using invokeLater...");

		// Use invokeLater to ensure window is shown on EDT after full initialization
		javax.swing.SwingUtilities.invokeLater(() -> {
			System.out.println("  >>> invokeLater: Making window visible NOW");
			setVisible(true);
			System.out.println("  >>> invokeLater: Is visible: " + isVisible());
			System.out.println("  >>> invokeLater: Is showing: " + isShowing());

			toFront();
			requestFocus();

			// Force to front
			setAlwaysOnTop(true);
			setAlwaysOnTop(true); // Set twice to force window manager attention

			System.out.println("  >>> invokeLater: Window should be visible now!");
		});

		System.out.println("=== PointEditorDialog Constructor END ===");
		System.out.println("  Scheduled visibility for EDT");
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		String titleText = itemName != null ? "Point Editor: " + itemName : "Point Editor";
		JLabel titleLabel = new JLabel(titleText);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		add(titleLabel, BorderLayout.NORTH);

		// Main panel with points list
		pointsListContainer = new JPanel();
		pointsListContainer.setLayout(new BoxLayout(pointsListContainer, BoxLayout.Y_AXIS));
		pointsListContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JScrollPane scrollPane = new JScrollPane(pointsListContainer);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane, BorderLayout.CENTER);

		// Bottom buttons
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		JButton newBtn = new JButton("New");
		newBtn.setToolTipText("Add a new point");
		newBtn.addActionListener(e -> addNewPoint());
		bottomPanel.add(newBtn);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setToolTipText("Remove selected point");
		removeBtn.addActionListener(e -> removeSelectedPoint());
		bottomPanel.add(removeBtn);

		JButton moveUpBtn = new JButton("↑");
		moveUpBtn.setToolTipText("Move selected point up");
		moveUpBtn.addActionListener(e -> movePointUp());
		bottomPanel.add(moveUpBtn);

		JButton moveDownBtn = new JButton("↓");
		moveDownBtn.setToolTipText("Move selected point down");
		moveDownBtn.addActionListener(e -> movePointDown());
		bottomPanel.add(moveDownBtn);

		// Add spacer to push Close button to the right
		bottomPanel.add(Box.createHorizontalGlue());

		JButton closeBtn = new JButton("Close");
		closeBtn.setToolTipText("Close Point Editor");
		closeBtn.addActionListener(e -> closeEditor());
		bottomPanel.add(closeBtn);

		add(bottomPanel, BorderLayout.SOUTH);

		// Window close listener
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				closeEditor();
			}
		});

		// Key listener for DELETE key
		addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
					removeSelectedPoint();
				}
			}
		});

		// Make sure the dialog can receive key events
		setFocusable(true);
		requestFocusInWindow();
	}

	private void loadPoints() {
		// Clear existing panels
		pointsListContainer.removeAll();
		pointPanels.clear();

		// Create a panel for each point
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			PointPanel panel = new PointPanel(i, p);
			pointPanels.add(panel);
			pointsListContainer.add(panel);
			pointsListContainer.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
		}

		pointsListContainer.revalidate();
		pointsListContainer.repaint();

		if (parent != null) {
			parent.log("Loaded " + points.size() + " points into Point Editor");
		}
	}

	private void addNewPoint() {
		// Default coordinates as per schema: (100, 100)
		Point newPoint = new Point(100, 100);

		int insertIndex;

		if (selectedPointIndex >= 0) {
			// Insert BELOW the selected point
			insertIndex = selectedPointIndex + 1;
			points.add(insertIndex, newPoint);
			if (parent != null)
				parent.log("Inserted new point at index " + insertIndex + " (below selected): (100, 100)");
		} else {
			// No selection, add at end
			insertIndex = points.size();
			points.add(newPoint);
			if (parent != null)
				parent.log("Added new point at end: (100, 100)");
		}

		loadPoints();

		// Select the newly added point
		if (insertIndex < pointPanels.size()) {
			selectPoint(insertIndex);
		}

		saveChanges();
	}

	private void removeSelectedPoint() {
		if (selectedPointIndex < 0) {
			JOptionPane.showMessageDialog(this, "Please select a point to remove!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		points.remove(selectedPointIndex);
		selectedPointIndex = -1;

		loadPoints();
		if (parent != null)
			parent.log("Removed point");
		saveChanges();
	}

	private void movePointUp() {
		if (selectedPointIndex <= 0) {
			return; // Can't move first point up
		}

		// Swap in data list
		Point temp = points.get(selectedPointIndex);
		points.set(selectedPointIndex, points.get(selectedPointIndex - 1));
		points.set(selectedPointIndex - 1, temp);

		selectedPointIndex--;

		loadPoints();
		selectPoint(selectedPointIndex);

		if (parent != null)
			parent.log("Moved point up");
		saveChanges();
	}

	private void movePointDown() {
		if (selectedPointIndex < 0 || selectedPointIndex >= points.size() - 1) {
			return; // Can't move last point down
		}

		// Swap in data list
		Point temp = points.get(selectedPointIndex);
		points.set(selectedPointIndex, points.get(selectedPointIndex + 1));
		points.set(selectedPointIndex + 1, temp);

		selectedPointIndex++;

		loadPoints();
		selectPoint(selectedPointIndex);

		if (parent != null)
			parent.log("Moved point down");
		saveChanges();
	}

	private void saveChanges() {
		// Trigger callback with updated points
		if (callback != null) {
			callback.onPointsChanged(new ArrayList<>(points));
		}

		// CRITICAL: Force immediate repaint of game canvas to show new points
		if (game != null) {
			game.repaint();
			game.revalidate();
		}

		if (parent != null)
			parent.log("Auto-saved " + points.size() + " points");
	}

	private void enableAddByClickMode() {
		if (game != null) {
			game.setAddPointModeSimple(true, pointType, null);
			game.setShowPaths(true);
			game.setPointEditorDialog(this);
			if (parent != null)
				parent.log("✓ Add by Click enabled for " + pointType);
		}
	}

	private void disableAddByClickMode() {
		if (game != null) {
			game.setAddPointModeSimple(false, "", null);
			game.setPointEditorDialog(null);
			if (parent != null)
				parent.log("✓ Add by Click disabled");
		}
	}

	private void minimizeOtherWindows() {
		System.out.println("  >>> minimizeOtherWindows() called");
		// Get all windows
		java.awt.Window[] windows = java.awt.Window.getWindows();
		System.out.println("  >>> Total windows found: " + windows.length);

		for (java.awt.Window window : windows) {
			System.out.println("  >>> Checking window: " + window.getClass().getSimpleName() + " (this? "
					+ (window == this) + ", parent? " + (window == parent) + ", game? " + (window == game)
					+ ", visible? " + window.isVisible() + ")");

			// CRITICAL: Never minimize this dialog itself or the game window
			if (window == this) {
				System.out.println("  >>> SKIPPING: This is the PointEditorDialog itself!");
				continue;
			}
			if (window == game) {
				System.out.println("  >>> SKIPPING: This is the game window!");
				continue;
			}

			// Minimize all other windows INCLUDING the parent EditorMainSimple
			if (window.isVisible() && (window instanceof javax.swing.JFrame || window instanceof javax.swing.JDialog)) {
				System.out.println("  >>> MINIMIZING: " + window.getClass().getSimpleName());
				minimizedWindows.add(window);
				window.setVisible(false);
				if (parent != null)
					parent.log("Minimized window: " + window.getClass().getSimpleName());
			} else {
				System.out.println("  >>> SKIPPING: " + window.getClass().getSimpleName());
			}
		}
		System.out.println("  >>> minimizeOtherWindows() complete - minimized " + minimizedWindows.size() + " windows");
	}

	private void restoreMinimizedWindows() {
		for (java.awt.Window window : minimizedWindows) {
			window.setVisible(true);
			if (parent != null)
				parent.log("Restored window: " + window.getClass().getSimpleName());
		}
		minimizedWindows.clear();
	}

	private void closeEditor() {
		disableAddByClickMode();
		saveChanges();
		restoreMinimizedWindows();
		dispose();
		if (parent != null)
			parent.log("Point Editor closed");
	}

	/**
	 * Add a point from click (called externally via reflection) Inserts the point
	 * BELOW the currently selected point, or at the end if no selection
	 */
	public void addPointAtPosition(int x, int y) {
		Point newPoint = new Point(x, y);

		int insertIndex;

		if (selectedPointIndex >= 0) {
			// Insert BELOW the selected point
			insertIndex = selectedPointIndex + 1;
			points.add(insertIndex, newPoint);
			if (parent != null)
				parent.log("Inserted point from click at index " + insertIndex + " (below selected): (" + x + ", " + y
						+ ")");
		} else {
			// No selection, add at end
			insertIndex = points.size();
			points.add(newPoint);
			if (parent != null)
				parent.log("Added point from click at end: (" + x + ", " + y + ")");
		}

		// Reload panels
		loadPoints();

		// Select the newly added point
		if (insertIndex < pointPanels.size()) {
			selectPoint(insertIndex);
		}

		saveChanges();

		// CRITICAL: Force immediate visual update in game canvas
		if (game != null) {
			// Ensure paths are still shown
			game.setShowPaths(true);
			// Force repaint
			game.repaint();
			game.revalidate();
			if (parent != null)
				parent.log("   Rendered new point at (" + x + ", " + y + ")");
		}
	}

	/**
	 * Check if add point mode is active
	 */
	public boolean isAddPointModeActive() {
		return game != null && game.isAddPointMode();
	}

	/**
	 * Inner class for a single Point Panel
	 */
	private class PointPanel extends JPanel {
		private int index;
		private Point point;
		private JTextField xField;
		private JTextField yField;
		private JTextField zField;

		public PointPanel(int index, Point point) {
			this.index = index;
			this.point = point;

			setLayout(new BorderLayout());
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
					BorderFactory.createEmptyBorder(5, 10, 5, 10)));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

			// Click to select
			addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					selectPoint(PointPanel.this.index);
					// Request focus so DELETE key works
					requestFocusInWindow();
				}
			});

			// Key listener for DELETE key
			addKeyListener(new java.awt.event.KeyAdapter() {
				@Override
				public void keyPressed(java.awt.event.KeyEvent e) {
					if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
						removeSelectedPoint();
					}
				}
			});

			// Make panel focusable so it can receive key events
			setFocusable(true);

			initPanel();
		}

		private void initPanel() {
			// Left: Point number label
			JLabel numberLabel = new JLabel("Point " + (index + 1) + ":");
			numberLabel.setFont(new Font("Arial", Font.BOLD, 12));
			numberLabel.setPreferredSize(new Dimension(80, 25));
			add(numberLabel, BorderLayout.WEST);

			// Center: X, Y, Z fields
			JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

			fieldsPanel.add(new JLabel("x:"));
			xField = new JTextField(String.valueOf(point.x), 6);
			xField.addFocusListener(new java.awt.event.FocusAdapter() {
				@Override
				public void focusLost(java.awt.event.FocusEvent e) {
					updatePoint();
				}
			});
			xField.addActionListener(e -> updatePoint());
			fieldsPanel.add(xField);

			fieldsPanel.add(new JLabel("y:"));
			yField = new JTextField(String.valueOf(point.y), 6);
			yField.addFocusListener(new java.awt.event.FocusAdapter() {
				@Override
				public void focusLost(java.awt.event.FocusEvent e) {
					updatePoint();
				}
			});
			yField.addActionListener(e -> updatePoint());
			fieldsPanel.add(yField);

			fieldsPanel.add(new JLabel("z:"));
			zField = new JTextField("1", 6); // Default z = 1 as per schema
			zField.setEnabled(false); // Z not used yet
			fieldsPanel.add(zField);

			add(fieldsPanel, BorderLayout.CENTER);
		}

		private void updatePoint() {
			try {
				point.x = Integer.parseInt(xField.getText());
				point.y = Integer.parseInt(yField.getText());
				saveChanges();
			} catch (NumberFormatException ex) {
				// Restore previous values
				xField.setText(String.valueOf(point.x));
				yField.setText(String.valueOf(point.y));
			}
		}

		public void setSelected(boolean selected) {
			if (selected) {
				setBackground(new Color(200, 220, 255));
			} else {
				setBackground(null);
			}
		}

		public void updateIndex(int newIndex) {
			this.index = newIndex;
			// Update label
			Component[] components = getComponents();
			for (Component c : components) {
				if (c instanceof JLabel) {
					((JLabel) c).setText("Point " + (newIndex + 1) + ":");
					break;
				}
			}
		}
	}

	private void selectPoint(int index) {
		selectedPointIndex = index;

		// Update visual selection
		for (int i = 0; i < pointPanels.size(); i++) {
			pointPanels.get(i).setSelected(i == index);
		}

		// Notify game to highlight this point in the scene
		if (game != null) {
			// Get the currently selected item from the scene
			Scene currentScene = game.getCurrentScene();
			if (currentScene != null) {
				Item selectedItem = currentScene.getSelectedItem();
				if (selectedItem != null) {
					game.setHighlightedPoint(selectedItem, pointType, index);
					game.repaint();
				}
			}
		}

		if (parent != null)
			parent.log("Selected point " + (index + 1));
	}

	/**
	 * Update a specific point's coordinates in the ListView (called during drag).
	 * Highlights the point being dragged and updates the text fields live.
	 */
	public void updatePointInList(int index, int newX, int newY) {
		if (index < 0 || index >= pointPanels.size()) {
			return;
		}

		// Update the point in the list
		Point p = points.get(index);
		p.x = newX;
		p.y = newY;

		// Update the text fields in the PointPanel
		PointPanel panel = pointPanels.get(index);
		panel.xField.setText(String.valueOf(newX));
		panel.yField.setText(String.valueOf(newY));

		// Highlight this point (select it)
		selectPoint(index);

		// Scroll to the panel if needed
		SwingUtilities.invokeLater(() -> {
			panel.scrollRectToVisible(panel.getBounds());
		});
	}
}
