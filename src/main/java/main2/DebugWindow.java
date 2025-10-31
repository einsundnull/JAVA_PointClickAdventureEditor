package main2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Debug window to show action executions and condition changes
 * Toggle with ALT+D
 */
public class DebugWindow extends JFrame {
    private JTextArea logArea;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private boolean autoScroll = true;

    public DebugWindow() {
        setTitle("Debug Console");
        setSize(800, 600);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        // Top panel with info and buttons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Debug Console - Actions & Condition Changes"));
        topPanel.add(infoPanel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearLog());
        buttonPanel.add(clearBtn);

        JButton autoScrollBtn = new JButton("Auto-Scroll: ON");
        autoScrollBtn.addActionListener(e -> {
            autoScroll = !autoScroll;
            autoScrollBtn.setText("Auto-Scroll: " + (autoScroll ? "ON" : "OFF"));
        });
        buttonPanel.add(autoScrollBtn);

        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with stats
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(new JLabel("Press ALT+D to toggle this window"));
        add(bottomPanel, BorderLayout.SOUTH);
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
            appendLog("\n" + "=".repeat(80), new Color(128, 128, 128));
            appendLog(logEntry, new Color(255, 150, 100));
            appendLog("=".repeat(80) + "\n", new Color(128, 128, 128));
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
}
