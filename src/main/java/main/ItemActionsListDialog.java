package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Dialog that shows all actions for an item and allows editing each action's conditions/results.
 * This is the intermediary dialog between "Edit Actions" button and NewItemActionsEditorDialog.
 */
public class ItemActionsListDialog extends JDialog {
    private Item item;
    private Frame owner;
    private JList<String> actionsList;
    private DefaultListModel<String> actionsListModel;

    public ItemActionsListDialog(Frame owner, Item item) {
        super(owner, "Actions for Item: " + item.getName(), true);
        this.owner = owner;
        this.item = item;

        initUI();
        loadActions();

        setSize(600, 500);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Manage Actions for: " + item.getName());
        titleLabel.setFont(titleLabel.getFont().deriveFont(14f).deriveFont(java.awt.Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><i>Actions are button behaviors (e.g., 'Anschauen', 'Nehmen').<br>" +
            "Select an action to edit its conditions and results.</i></html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        infoPanel.add(infoLabel, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.NORTH);

        // Center: List of actions
        actionsListModel = new DefaultListModel<>();
        actionsList = new JList<>(actionsListModel);
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(actionsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonsPanel = new JPanel(new BorderLayout());

        // Left side: Action management buttons
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));

        JButton addBtn = new JButton("Add Action");
        addBtn.addActionListener(e -> addAction());
        actionButtonsPanel.add(addBtn);

        JButton editBtn = new JButton("Edit Selected Action");
        editBtn.addActionListener(e -> editSelectedAction());
        actionButtonsPanel.add(editBtn);

        JButton deleteBtn = new JButton("Delete Action");
        deleteBtn.addActionListener(e -> deleteAction());
        actionButtonsPanel.add(deleteBtn);

        buttonsPanel.add(actionButtonsPanel, BorderLayout.WEST);

        // Right side: Close and Button Manager
        JPanel rightButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));

        JButton manageButtonsBtn = new JButton("Manage Buttons");
        manageButtonsBtn.setToolTipText("Open Button Manager to create/edit button names");
        manageButtonsBtn.addActionListener(e -> openButtonManager());
        rightButtonsPanel.add(manageButtonsBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        rightButtonsPanel.add(closeBtn);

        buttonsPanel.add(rightButtonsPanel, BorderLayout.EAST);

        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void loadActions() {
        actionsListModel.clear();

        Map<String, KeyArea.ActionHandler> actions = item.getActions();
        if (actions != null && !actions.isEmpty()) {
            for (String actionName : actions.keySet()) {
                actionsListModel.addElement(actionName);
            }
        }

        System.out.println("Loaded " + actionsListModel.size() + " actions for item: " + item.getName());
    }

    private void addAction() {
        // Show dialog with available buttons
        List<String> availableButtons = ButtonsDataManager.getButtonNames();

        if (availableButtons.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No buttons available!\nPlease create buttons first using 'Manage Buttons'.",
                "No Buttons",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Filter out buttons that already have actions
        List<String> unusedButtons = new ArrayList<>();
        Map<String, KeyArea.ActionHandler> existingActions = item.getActions();
        for (String button : availableButtons) {
            if (existingActions == null || !existingActions.containsKey(button)) {
                unusedButtons.add(button);
            }
        }

        if (unusedButtons.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "All available buttons already have actions for this item!\n" +
                "You can create new buttons using 'Manage Buttons'.",
                "All Buttons Used",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show selection dialog
        String[] buttonArray = unusedButtons.toArray(new String[0]);
        String selectedButton = (String) JOptionPane.showInputDialog(
            this,
            "Select a button to add as an action:",
            "Add Action",
            JOptionPane.PLAIN_MESSAGE,
            null,
            buttonArray,
            buttonArray[0]
        );

        if (selectedButton == null) {
            return; // Cancelled
        }

        // Create a default action handler with "none" condition
        KeyArea.ActionHandler handler = new KeyArea.ActionHandler();
        handler.addConditionalResult("none", "#Dialog:dialog-" + item.getName().toLowerCase() + "-" + selectedButton.toLowerCase());

        // Add to item
        item.addAction(selectedButton, handler);

        // Save item
        try {
            ItemSaver.saveItemByName(item);
            loadActions();
            JOptionPane.showMessageDialog(this,
                "Action '" + selectedButton + "' added successfully!\n\n" +
                "You can now edit its conditions and results.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving item: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedAction() {
        String selectedAction = actionsList.getSelectedValue();
        if (selectedAction == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an action to edit",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open NewItemActionsEditorDialog for this specific action
        try {
            NewItemActionsEditorDialog actionDialog = new NewItemActionsEditorDialog(
                owner,
                item,
                selectedAction  // Pass the ACTION NAME, not item name!
            );
            actionDialog.setVisible(true);

            // Reload item from file after editing
            Item reloadedItem = ItemLoader.loadItemByName(item.getName());
            this.item = reloadedItem;
            loadActions();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error opening action editor: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void deleteAction() {
        String selectedAction = actionsList.getSelectedValue();
        if (selectedAction == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an action to delete",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete action '" + selectedAction + "' from item '" + item.getName() + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove action from item
        item.getActions().remove(selectedAction);

        // Save item
        try {
            ItemSaver.saveItemByName(item);
            loadActions();
            JOptionPane.showMessageDialog(this,
                "Action '" + selectedAction + "' deleted successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving item: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openButtonManager() {
        ButtonManagerDialog dialog = new ButtonManagerDialog(owner);
        dialog.setVisible(true);

        // Reload actions list in case button names changed
        loadActions();
    }

    public Item getItem() {
        return item;
    }
}
