package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import main.ui.components.button.AppButton;
import main.ui.theme.Spacing;
import main.ui.theme.ThemeManager;

/**
 * Debug Sidebar Panel - Shows action executions and condition changes
 * Now integrated as a sidebar instead of a separate window
 */
public class DebugWindow extends JPanel {

    private JTextArea logArea;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private boolean autoScroll = true;
    private boolean visible = false;

    // Reference to the frame containing this sidebar (for dispose)
    private JFrame frame;

    public DebugWindow() {
        super(new BorderLayout());
        initUI();
    }

    private void initUI() {
        var c = ThemeManager.colors();
        var t = ThemeManager.typography();

        setPreferredSize(new Dimension(350, 0));
        setMinimumSize(new Dimension(250, 0));

        // Header Panel - Collapsible with title and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(c.getBackgroundElevated());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, c.getBorderDefault()),
            BorderFactory.createEmptyBorder(Spacing.SM, Spacing.SM, Spacing.SM, Spacing.SM)
        ));

        JLabel titleLabel = new JLabel("🐛 Debug Console");
        titleLabel.setFont(t.semiboldBase());
        titleLabel.setForeground(c.getTextPrimary());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        AppButton closeBtn = new AppButton("×", AppButton.Variant.GHOST, AppButton.Size.SMALL);
        closeBtn.setToolTipText("Hide Debug Sidebar");
        closeBtn.addActionListener(e -> hide());
        headerPanel.add(closeBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Content area with buttons and log
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(c.getBackgroundRoot());

        // Top button row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.XS, Spacing.XS));
        buttonPanel.setBackground(c.getBackgroundRoot());

        AppButton clearBtn = new AppButton("Clear", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
        clearBtn.addActionListener(e -> clearLog());
        buttonPanel.add(clearBtn);

        AppButton autoScrollBtn = new AppButton("Auto-Scroll: ON", AppButton.Variant.SECONDARY, AppButton.Size.SMALL);
        autoScrollBtn.addActionListener(e -> {
            autoScroll = !autoScroll;
            autoScrollBtn.setText("Auto-Scroll: " + (autoScroll ? "ON" : "OFF"));
        });
        buttonPanel.add(autoScrollBtn);

        contentPanel.add(buttonPanel, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(t.monoSm());
        logArea.setBackground(c.getBackgroundInput());
        logArea.setForeground(c.getTextPrimary());
        logArea.setBorder(BorderFactory.createEmptyBorder(Spacing.XS, Spacing.XS, Spacing.XS, Spacing.XS));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        scrollPane.setBackground(c.getBackgroundRoot());
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Show the debug sidebar in a frame
     */
    public void show() {
        if (frame == null) {
            frame = new JFrame("Debug Console");
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            frame.add(this);
            frame.setSize(400, 600);
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        visible = true;
    }

    /**
     * Hide the debug sidebar frame
     */
    public void hide() {
        if (frame != null) {
            frame.setVisible(false);
        }
        visible = false;
    }

    /**
     * Check if currently visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set the parent frame (when embedded in a sidebar)
     */
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    /**
     * Logs an action execution
     */
    public void logAction(String actionName, String targetName, String result) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            StringBuilder sb = new StringBuilder();

            sb.append("[").append(timestamp).append("] ");
            sb.append("ACTION: ").append(actionName);
            sb.append(" on ").append(targetName);

            if (result != null && !result.isEmpty()) {
                sb.append("\n    └─ Result: ").append(result);
            }

            appendLog(sb.toString(), new Color(100, 200, 255));
        });
    }

    /**
     * Logs a condition change
     */
    public void logConditionChange(String conditionName, boolean oldValue, boolean newValue) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            StringBuilder sb = new StringBuilder();

            sb.append("[").append(timestamp).append("] ");
            sb.append("CONDITION: ").append(conditionName);
            sb.append(" changed from ").append(oldValue).append(" to ").append(newValue);

            // Highlight inventory changes
            Color color = conditionName.startsWith("isInInventory_") ?
                new Color(100, 255, 100) : new Color(255, 255, 100);

            appendLog(sb.toString(), color);
        });
    }

    /**
     * Logs a general message
     */
    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            String logEntry = "[" + timestamp + "] " + message;
            appendLog(logEntry, new Color(200, 200, 200));
        });
    }

    /**
     * Logs a scene change
     */
    public void logSceneChange(String sceneName) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            String logEntry = "[" + timestamp + "] SCENE CHANGED: " + sceneName;
			appendLog("\n" + repeatChar('=', 80), new Color(128, 128, 128));
            appendLog(logEntry, new Color(255, 150, 100));
			appendLog(repeatChar('=', 80) + "\n", new Color(128, 128, 128));
        });
    }

    /**
     * Appends a log entry with color (simulated via text since JTextArea doesn't support colors easily)
     */
    private void appendLog(String message, Color color) {
        logArea.append(message + "\n");

        if (autoScroll) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    /**
     * Clears the log
     */
    public void clearLog() {
        logArea.setText("");
        logMessage("Log cleared");
    }

    /**
     * Toggles visibility of the debug window
     */
    public void toggle() {
        setVisible(!isVisible());
    }

	/**
	 * Java 8-compatible replacement for String.repeat in Java 11+
	 */
	private static String repeatChar(char ch, int count) {
		if (count <= 0) return "";
		StringBuilder builder = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			builder.append(ch);
		}
		return builder.toString();
	}
}
