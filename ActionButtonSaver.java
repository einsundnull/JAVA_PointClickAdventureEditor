package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Saves ActionButtons to action_buttons.txt
 */
public class ActionButtonSaver {
    private static final String ACTION_BUTTONS_FILE = ResourcePathHelper.resolvePath("action_buttons/action_buttons.txt");

    public static void saveActionButtons(List<ActionButton> buttons) throws IOException {
        File file = new File(ACTION_BUTTONS_FILE);
        file.getParentFile().mkdirs(); // Create directories if they don't exist

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("// Action Buttons Configuration\n");
            writer.write("// This file defines the action buttons in the game menu\n");
            writer.write("//\n");
            writer.write("// Available CursorTypes: HAND, CROSSHAIR, DEFAULT, TEXT, MOVE, WAIT, CUSTOM\n");
            writer.write("// If CUSTOM is used, set CustomCursorPath to the image path\n");
            writer.write("//\n");
            writer.write("// HoverFormat placeholders:\n");
            writer.write("//   {action} - The action name (button text)\n");
            writer.write("//   {target} - The target object name\n");
            writer.write("//   {item}   - The selected item (for item-as-cursor mode)\n");
            writer.write("//\n\n");

            // Sort by order before saving
            buttons.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

            for (ActionButton button : buttons) {
                writer.write("#Button:\n");
                writer.write("Text: " + button.getText() + "\n");
                writer.write("HoverText: " + button.getHoverText() + "\n");
                writer.write("ShowHoverText: " + button.isShowHoverText() + "\n");
                writer.write("ImagePath: " + button.getImagePath() + "\n");
                writer.write("UseImage: " + button.isUseImage() + "\n");
                writer.write("HoverFormat: " + button.getHoverFormat() + "\n");
                writer.write("CursorType: " + button.getCursorType() + "\n");
                writer.write("CustomCursorPath: " + button.getCustomCursorPath() + "\n");
                writer.write("Order: " + button.getOrder() + "\n");
                writer.write("\n");
            }

            System.out.println("Saved " + buttons.size() + " action buttons to " + ACTION_BUTTONS_FILE);
        }
    }

    /**
     * Creates and saves the default action_buttons.txt file
     */
    public static void createDefaultFile() throws IOException {
        List<ActionButton> defaultButtons = createDefaultButtons();
        saveActionButtons(defaultButtons);
    }

    private static List<ActionButton> createDefaultButtons() {
        java.util.List<ActionButton> buttons = new java.util.ArrayList<>();

        String[] defaultActions = { "Nimm", "Gib", "Ziehe", "Drücke", "Drehe", "Hebe", "Anschauen", "Sprich", "Benutze", "Gehe zu" };
        String[] cursors = { "HAND", "HAND", "MOVE", "HAND", "HAND", "HAND", "DEFAULT", "TEXT", "HAND", "CROSSHAIR" };

        for (int i = 0; i < defaultActions.length; i++) {
            ActionButton btn = new ActionButton(defaultActions[i]);
            btn.setCursorType(cursors[i]);
            btn.setOrder(i);
            btn.setHoverFormat("{action}");
            btn.setShowHoverText(false);
            buttons.add(btn);
        }

        return buttons;
    }
}
