package main2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Dialog zum Verwalten aller Punkte aller KeyAreas
 */
public class PointsManagerDialog extends JDialog {
	private KeyArea selectedKeyArea;
	private EditorWindow parent;
	private JTable pointsTable;
	private DefaultTableModel tableModel;
	private JList<String> keyAreaList;
	private DefaultListModel<String> keyAreaListModel;
	private JPanel keyAreaListContentPanel;
	private boolean isAddingPoint = false;

	public PointsManagerDialog(EditorWindow parent) {
		super(parent, "Points Manager - All KeyAreas", false); // Non-modal
		this.parent = parent;

		setSize(700, 700);
		setLocationRelativeTo(parent);

		// Enable path visualization when opening
		if (!parent.getGame().isShowingPaths()) {
			parent.getGame().togglePathVisualization();
		}

		// Disable path visualization when closing
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (parent.getGame().isShowingPaths()) {
					parent.getGame().togglePathVisualization();
				}
			}
		});

		initUI();
		loadKeyAreas();
		setupHotkeys();
	}

	private void setupHotkeys() {
		// ESC to cancel adding point mode
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKey, "cancelAddPoint");
		getRootPane().getActionMap().put("cancelAddPoint", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isAddingPoint) {
					isAddingPoint = false;
					parent.log("Adding point cancelled (ESC)");
				}
			}
		});
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// Title
		JLabel titleLabel = new JLabel("📍 Points Manager - All KeyAreas");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

		// Info
		JLabel infoLabel = new JLabel("Select a KeyArea to manage its polygon points");
		infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(titleLabel, BorderLayout.NORTH);
		northPanel.add(infoLabel, BorderLayout.SOUTH);
		add(northPanel, BorderLayout.NORTH);

		// Main content panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		// KeyArea ListView (Collapseable)
		JPanel keyAreaPanel = new JPanel(new BorderLayout());
		keyAreaPanel.setBorder(BorderFactory.createTitledBorder("KeyAreas"));
		keyAreaPanel.setPreferredSize(new Dimension(0, 180));

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

		JScrollPane keyAreaScroll = new JScrollPane(keyAreaList);
		keyAreaScroll.setPreferredSize(new Dimension(0, 120));
		keyAreaListContentPanel.add(keyAreaScroll, BorderLayout.CENTER);

		keyAreaPanel.add(keyAreaHeaderPanel, BorderLayout.NORTH);
		keyAreaPanel.add(keyAreaListContentPanel, BorderLayout.CENTER);

		// Toggle collapse/expand
		keyAreaToggleBtn.addActionListener(e -> {
			boolean isVisible = keyAreaListContentPanel.isVisible();
			keyAreaListContentPanel.setVisible(!isVisible);
			keyAreaToggleBtn.setText(isVisible ? "►" : "▼");
			keyAreaPanel.setPreferredSize(new Dimension(0, isVisible ? 40 : 180));
			keyAreaPanel.revalidate();
			keyAreaPanel.repaint();
		});

		mainPanel.add(keyAreaPanel, BorderLayout.NORTH);

		// Table
		String[] columns = { "Index", "X", "Y", "" };
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public Class<?> getColumnClass(int column) {
				if (column == 0) return Integer.class;
				if (column == 1 || column == 2) return Integer.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1 || column == 2 || column == 3; // X, Y, and Delete button editable
			}
		};

		pointsTable = new JTable(tableModel);
		pointsTable.setRowHeight(30);
		pointsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
		pointsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		pointsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		pointsTable.getColumnModel().getColumn(3).setPreferredWidth(80);

		// Delete button column
		pointsTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
		pointsTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));

		// Listen for X/Y changes
		tableModel.addTableModelListener(e -> {
			if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				int column = e.getColumn();

				if (column == 1 || column == 2) {
					// X or Y changed
					updatePointFromTable(row);
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(pointsTable);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);

		// Bottom panel
		JPanel bottomPanel = new JPanel(new BorderLayout());

		JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton addPointBtn = new JButton("➕ Add Point");
		addPointBtn.addActionListener(e -> addNewPoint());
		addPanel.add(addPointBtn);

		JButton addPointClickBtn = new JButton("➕ Add Point (Click on Game)");
		addPointClickBtn.addActionListener(e -> startAddPointMode());
		addPanel.add(addPointClickBtn);

		bottomPanel.add(addPanel, BorderLayout.WEST);

		JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveButton = new JButton("💾 Save & Close");
		saveButton.addActionListener(e -> saveAndClose());
		savePanel.add(saveButton);

		JButton closeButton = new JButton("✓ Close");
		closeButton.addActionListener(e -> dispose());
		savePanel.add(closeButton);

		bottomPanel.add(savePanel, BorderLayout.EAST);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void loadKeyAreas() {
		keyAreaListModel.clear();
		Scene currentScene = parent.getGame().getCurrentScene();

		if (currentScene == null) {
			parent.log("No scene loaded");
			return;
		}

		for (KeyArea area : currentScene.getKeyAreas()) {
			keyAreaListModel.addElement(area.getName() + " (" + area.getType() + ") - " + area.getPoints().size() + " points");
		}

		parent.log("Loaded " + keyAreaListModel.size() + " KeyAreas in Points Manager");
	}

	private void onKeyAreaSelected() {
		int selectedIndex = keyAreaList.getSelectedIndex();
		if (selectedIndex < 0) {
			selectedKeyArea = null;
			clearPointsTable();
			return;
		}

		Scene currentScene = parent.getGame().getCurrentScene();
		if (currentScene == null)
			return;

		java.util.List<KeyArea> areas = currentScene.getKeyAreas();
		if (selectedIndex >= areas.size())
			return;

		selectedKeyArea = areas.get(selectedIndex);
		parent.log("Selected KeyArea: " + selectedKeyArea.getName());

		loadPoints();
	}

	private void loadPoints() {
		tableModel.setRowCount(0);

		if (selectedKeyArea == null) {
			return;
		}

		java.util.List<Point> points = selectedKeyArea.getPoints();
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			tableModel.addRow(new Object[] { i, p.x, p.y, "🗑️" });
		}
	}

	private void clearPointsTable() {
		tableModel.setRowCount(0);
	}

	private void updatePointFromTable(int row) {
		if (selectedKeyArea == null) {
			return;
		}

		try {
			int x = (Integer) tableModel.getValueAt(row, 1);
			int y = (Integer) tableModel.getValueAt(row, 2);

			java.util.List<Point> points = selectedKeyArea.getPoints();
			if (row < points.size()) {
				Point p = points.get(row);
				p.x = x;
				p.y = y;
				selectedKeyArea.updatePolygon();
				parent.repaintGamePanel();
				parent.log("Updated point " + row + " to (" + x + ", " + y + ")");
			}
		} catch (Exception e) {
			parent.log("ERROR: Invalid coordinates - " + e.getMessage());
		}
	}

	private void addNewPoint() {
		if (selectedKeyArea == null) {
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Dialog for coordinates
		JTextField xField = new JTextField("100");
		JTextField yField = new JTextField("100");

		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel("X:"));
		panel.add(xField);
		panel.add(new JLabel("Y:"));
		panel.add(yField);

		int result = JOptionPane.showConfirmDialog(this, panel, "Enter Point Coordinates",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			try {
				int x = Integer.parseInt(xField.getText());
				int y = Integer.parseInt(yField.getText());

				selectedKeyArea.addPoint(x, y);
				loadPoints();
				loadKeyAreas(); // Update point count
				parent.repaintGamePanel();
				parent.log("Added point at (" + x + ", " + y + ")");
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid coordinates!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void startAddPointMode() {
		if (selectedKeyArea == null) {
			JOptionPane.showMessageDialog(this, "Please select a KeyArea first!", "No Selection",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		isAddingPoint = true;
		parent.log("Click on game screen to add point to " + selectedKeyArea.getName());
	}

	/**
	 * Called from EditorWindow when a point is clicked in add mode
	 */
	public void addPointAtPosition(int x, int y) {
		if (!isAddingPoint || selectedKeyArea == null) {
			return;
		}

		selectedKeyArea.addPoint(x, y);
		loadPoints();
		loadKeyAreas(); // Update point count
		parent.repaintGamePanel();
		parent.log("Added point at (" + x + ", " + y + ") to " + selectedKeyArea.getName());

		isAddingPoint = false;
	}

	public boolean isAddingPoint() {
		return isAddingPoint;
	}

	private void deletePoint(int row) {
		if (selectedKeyArea == null) {
			return;
		}

		java.util.List<Point> points = selectedKeyArea.getPoints();

		if (points.size() <= 3) {
			JOptionPane.showMessageDialog(this, "Cannot delete - need at least 3 points for a polygon!", "Error",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (row >= 0 && row < points.size()) {
			Point p = points.get(row);
			points.remove(row);
			selectedKeyArea.updatePolygon();
			loadPoints();
			loadKeyAreas(); // Update point count
			parent.repaintGamePanel();
			parent.log("Deleted point " + row + " (" + p.x + ", " + p.y + ")");
		}
	}

	private void saveAndClose() {
		parent.autoSaveCurrentScene();
		parent.log("✓ Points saved!");

		// Disable path visualization when closing
		if (parent.getGame().isShowingPaths()) {
			parent.getGame().togglePathVisualization();
		}

		dispose();
	}

	// Button Renderer
	class ButtonRenderer extends JButton implements TableCellRenderer {
		public ButtonRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText("🗑️");
			return this;
		}
	}

	// Button Editor
	class ButtonEditor extends DefaultCellEditor {
		private JButton button;
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
			button.setText("🗑️");
			isPushed = true;
			currentRow = row;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				deletePoint(currentRow);
			}
			isPushed = false;
			return "🗑️";
		}

		@Override
		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}
}
