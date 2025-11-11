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
 * Manages available button names (action names) globally.
 * Buttons are stored in resources/buttons.txt
 */
public class ButtonsManager {
    private static final String BUTTONS_FILE = "resources/buttons.txt";
    private static List<String> availableButtons = new ArrayList<>();

    static {
        loadButtons();
    }

    /**
     * Loads buttons from resources/buttons.txt
     */
    public static void loadButtons() {
        availableButtons.clear();

        File file = new File(BUTTONS_FILE);
        if (!file.exists()) {
            // Create default buttons
            availableButtons.add("Anschauen");
            availableButtons.add("Nehmen");
            availableButtons.add("Benutzen");
            availableButtons.add("Sprechen");
            availableButtons.add("Öffnen");
            availableButtons.add("Schließen");
            saveButtons();
            System.out.println("ButtonsManager: Created default buttons.txt");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    availableButtons.add(trimmed);
                }
            }
            System.out.println("ButtonsManager: Loaded " + availableButtons.size() + " buttons");
        } catch (IOException e) {
            System.err.println("Error loading buttons: " + e.getMessage());
        }
    }

    /**
     * Saves buttons to resources/buttons.txt
     */
    public static void saveButtons() {
        File file = new File(BUTTONS_FILE);

        // Ensure directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# Available Button Names (Action Names)\n");
            writer.write("# One button name per line\n\n");

            for (String button : availableButtons) {
                writer.write(button + "\n");
            }

            System.out.println("ButtonsManager: Saved " + availableButtons.size() + " buttons");
        } catch (IOException e) {
            System.err.println("Error saving buttons: " + e.getMessage());
        }
    }

    /**
     * Gets all available button names
     */
    public static List<String> getAvailableButtons() {
        return new ArrayList<>(availableButtons);
    }

    /**
     * Adds a new button name
     */
    public static boolean addButton(String buttonName) {
        if (buttonName == null || buttonName.trim().isEmpty()) {
            return false;
        }

        String trimmed = buttonName.trim();
        if (availableButtons.contains(trimmed)) {
            return false; // Already exists
        }

        availableButtons.add(trimmed);
        saveButtons();
        return true;
    }

    /**
     * Removes a button name
     */
    public static boolean removeButton(String buttonName) {
        boolean removed = availableButtons.remove(buttonName);
        if (removed) {
            saveButtons();
        }
        return removed;
    }

    /**
     * Renames a button (updates the name in the list)
     */
    public static boolean renameButton(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }

        String trimmedNew = newName.trim();
        if (availableButtons.contains(trimmedNew)) {
            return false; // New name already exists
        }

        int index = availableButtons.indexOf(oldName);
        if (index >= 0) {
            availableButtons.set(index, trimmedNew);
            saveButtons();
            return true;
        }

        return false;
    }

    /**
     * Checks if a button name exists
     */
    public static boolean buttonExists(String buttonName) {
        return availableButtons.contains(buttonName);
    }
}
