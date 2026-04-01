package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads ActionButtons from action_buttons.txt
 */
public class ActionButtonLoader {
    private static final String ACTION_BUTTONS_FILE = ResourcePathHelper.resolvePath("action_buttons/action_buttons.txt");

    public static List<ActionButton> loadActionButtons() {
        List<ActionButton> buttons = new ArrayList<>();
        File file = new File(ACTION_BUTTONS_FILE);

        if (!file.exists()) {
            System.out.println("ActionButtons file not found: " + ACTION_BUTTONS_FILE);
            return createDefaultButtons();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            ActionButton currentButton = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                    continue; // Skip empty lines and comments
                }

                if (trimmed.startsWith("#Button:")) {
                    // Save previous button if exists
                    if (currentButton != null) {
                        buttons.add(currentButton);
                    }
                    // Start new button
                    currentButton = new ActionButton();
                } else if (currentButton != null) {
                    // Parse button properties
                    if (trimmed.startsWith("Text:")) {
                        currentButton.setText(trimmed.substring(5).trim());
                    } else if (trimmed.startsWith("HoverText:")) {
                        currentButton.setHoverText(trimmed.substring(10).trim());
                    } else if (trimmed.startsWith("ShowHoverText:")) {
                        currentButton.setShowHoverText(Boolean.parseBoolean(trimmed.substring(14).trim()));
                    } else if (trimmed.startsWith("ImagePath:")) {
                        currentButton.setImagePath(trimmed.substring(10).trim());
                    } else if (trimmed.startsWith("UseImage:")) {
                        currentButton.setUseImage(Boolean.parseBoolean(trimmed.substring(9).trim()));
                    } else if (trimmed.startsWith("HoverFormat:")) {
                        currentButton.setHoverFormat(trimmed.substring(12).trim());
                    } else if (trimmed.startsWith("CursorType:")) {
                        currentButton.setCursorType(trimmed.substring(11).trim());
                    } else if (trimmed.startsWith("CustomCursorPath:")) {
                        currentButton.setCustomCursorPath(trimmed.substring(17).trim());
                    } else if (trimmed.startsWith("Order:")) {
                        currentButton.setOrder(Integer.parseInt(trimmed.substring(6).trim()));
                    }
                }
            }

            // Add last button
            if (currentButton != null) {
                buttons.add(currentButton);
            }

            System.out.println("Loaded " + buttons.size() + " action buttons from " + ACTION_BUTTONS_FILE);

        } catch (IOException e) {
            System.err.println("Error loading action buttons: " + e.getMessage());
            e.printStackTrace();
            return createDefaultButtons();
        }

        // Sort by order
        buttons.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

        return buttons;
    }

    /**
     * Creates default action buttons matching the hardcoded MENU_ACTIONS
     */
    private static List<ActionButton> createDefaultButtons() {
        System.out.println("Creating default action buttons");
        List<ActionButton> buttons = new ArrayList<>();

        String[] defaultActions = { "Nimm", "Gib", "Ziehe", "Drücke", "Drehe", "Hebe", "Anschauen", "Sprich", "Benutze", "Gehe zu" };
        String[] cursors = { "HAND", "HAND", "MOVE", "HAND", "HAND", "HAND", "DEFAULT", "TEXT", "HAND", "CROSSHAIR" };

        for (int i = 0; i < defaultActions.length; i++) {
            ActionButton btn = new ActionButton(defaultActions[i]);
            btn.setCursorType(cursors[i]);
            btn.setOrder(i);
            btn.setHoverFormat("{action}");
            buttons.add(btn);
        }

        return buttons;
    }
}
