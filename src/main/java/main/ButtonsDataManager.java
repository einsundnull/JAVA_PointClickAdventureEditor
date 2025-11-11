package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages ButtonData objects - full button configurations with images, text, etc.
 * Stores in resources/buttons/buttons.txt
 */
public class ButtonsDataManager {
    private static final String BUTTONS_FILE = "resources/buttons/buttons.txt";
    private static List<ButtonData> buttons = new ArrayList<>();

    static {
        loadButtons();
    }

    /**
     * Loads buttons from resources/buttons/buttons.txt
     */
    public static void loadButtons() {
        buttons.clear();

        File file = new File(BUTTONS_FILE);
        if (!file.exists()) {
            // Create default buttons
            createDefaultButtons();
            saveButtons();
            System.out.println("ButtonsDataManager: Created default buttons.txt");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            ButtonData currentButton = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                if (trimmed.startsWith("-Name:")) {
                    // Start of a new button
                    String name = trimmed.substring(6).trim();
                    currentButton = new ButtonData(name);
                    buttons.add(currentButton);
                } else if (currentButton != null) {
                    // Parse properties
                    if (trimmed.startsWith("--ImagePath:")) {
                        currentButton.setImagePath(trimmed.substring(12).trim());
                    } else if (trimmed.startsWith("--TextOnButton:")) {
                        currentButton.setTextOnButton(trimmed.substring(15).trim());
                    } else if (trimmed.startsWith("--HoverText:")) {
                        currentButton.setHoverText(trimmed.substring(12).trim());
                    } else if (trimmed.startsWith("--UseText:")) {
                        currentButton.setUseText(Boolean.parseBoolean(trimmed.substring(10).trim()));
                    } else if (trimmed.startsWith("--UseHoverText:")) {
                        currentButton.setUseHoverText(Boolean.parseBoolean(trimmed.substring(15).trim()));
                    } else if (trimmed.startsWith("--UseImage:")) {
                        currentButton.setUseImage(Boolean.parseBoolean(trimmed.substring(11).trim()));
                    } else if (trimmed.startsWith("--Width:")) {
                        currentButton.setWidth(Integer.parseInt(trimmed.substring(8).trim()));
                    } else if (trimmed.startsWith("--Height:")) {
                        currentButton.setHeight(Integer.parseInt(trimmed.substring(9).trim()));
                    }
                }
            }

            System.out.println("ButtonsDataManager: Loaded " + buttons.size() + " buttons");
        } catch (IOException e) {
            System.err.println("Error loading buttons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves buttons to resources/buttons/buttons.txt
     */
    public static void saveButtons() {
        File file = new File(BUTTONS_FILE);

        // Ensure directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# Button Definitions\n");
            writer.write("# Each button has: Name, Image, Text, Hover Text, Flags, Size\n\n");

            for (ButtonData button : buttons) {
                writer.write("-Name: " + button.getName() + "\n");
                writer.write("--ImagePath: " + button.getImagePath() + "\n");
                writer.write("--TextOnButton: " + button.getTextOnButton() + "\n");
                writer.write("--HoverText: " + button.getHoverText() + "\n");
                writer.write("--UseText: " + button.isUseText() + "\n");
                writer.write("--UseHoverText: " + button.isUseHoverText() + "\n");
                writer.write("--UseImage: " + button.isUseImage() + "\n");
                writer.write("--Width: " + button.getWidth() + "\n");
                writer.write("--Height: " + button.getHeight() + "\n");
                writer.write("\n");
            }

            System.out.println("ButtonsDataManager: Saved " + buttons.size() + " buttons");
        } catch (IOException e) {
            System.err.println("Error saving buttons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates default buttons
     */
    private static void createDefaultButtons() {
        buttons.add(createButton("Anschauen", "Anschauen", "Objekt anschauen"));
        buttons.add(createButton("Nehmen", "Nehmen", "Objekt nehmen"));
        buttons.add(createButton("Benutzen", "Benutzen", "Objekt benutzen"));
        buttons.add(createButton("Sprechen", "Sprechen", "Mit Objekt sprechen"));
        buttons.add(createButton("Öffnen", "Öffnen", "Objekt öffnen"));
        buttons.add(createButton("Schließen", "Schließen", "Objekt schließen"));
    }

    private static ButtonData createButton(String name, String text, String hoverText) {
        ButtonData button = new ButtonData(name);
        button.setTextOnButton(text);
        button.setHoverText(hoverText);
        return button;
    }

    /**
     * Gets all buttons
     */
    public static List<ButtonData> getButtons() {
        return new ArrayList<>(buttons);
    }

    /**
     * Gets button by name
     */
    public static ButtonData getButton(String name) {
        for (ButtonData button : buttons) {
            if (button.getName().equals(name)) {
                return button;
            }
        }
        return null;
    }

    /**
     * Adds a new button
     */
    public static boolean addButton(ButtonData button) {
        if (button == null || button.getName() == null || button.getName().trim().isEmpty()) {
            return false;
        }

        // Check for duplicates
        if (getButton(button.getName()) != null) {
            return false;
        }

        buttons.add(button);
        saveButtons();
        return true;
    }

    /**
     * Removes a button
     */
    public static boolean removeButton(String name) {
        ButtonData button = getButton(name);
        if (button != null) {
            buttons.remove(button);
            saveButtons();
            return true;
        }
        return false;
    }

    /**
     * Updates a button (replaces it)
     */
    public static boolean updateButton(ButtonData updatedButton) {
        if (updatedButton == null || updatedButton.getName() == null) {
            return false;
        }

        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).getName().equals(updatedButton.getName())) {
                buttons.set(i, updatedButton);
                saveButtons();
                return true;
            }
        }

        return false;
    }

    /**
     * Moves button up in the list
     */
    public static boolean moveButtonUp(String name) {
        int index = findButtonIndex(name);
        if (index > 0) {
            ButtonData temp = buttons.get(index);
            buttons.set(index, buttons.get(index - 1));
            buttons.set(index - 1, temp);
            saveButtons();
            return true;
        }
        return false;
    }

    /**
     * Moves button down in the list
     */
    public static boolean moveButtonDown(String name) {
        int index = findButtonIndex(name);
        if (index >= 0 && index < buttons.size() - 1) {
            ButtonData temp = buttons.get(index);
            buttons.set(index, buttons.get(index + 1));
            buttons.set(index + 1, temp);
            saveButtons();
            return true;
        }
        return false;
    }

    private static int findButtonIndex(String name) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets all button names (for backwards compatibility with old ButtonsManager)
     */
    public static List<String> getButtonNames() {
        List<String> names = new ArrayList<>();
        for (ButtonData button : buttons) {
            names.add(button.getName());
        }
        return names;
    }
}
