package main2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Zentrale Klasse für alle Spielbedingungen.
 * Conditions werden dynamisch aus conditions.txt geladen und verwaltet.
 * KEINE Quellcode-Änderungen mehr notwendig!
 */
public class Conditions {
    private static final String CONDITIONS_FILE = "resources/conditions/conditions.txt";
    private static final String CONDITIONS_DEFAULTS_FILE = "resources/conditions-defaults.txt";

    // Dynamische Map für alle Conditions
    private static Map<String, Boolean> conditions = new LinkedHashMap<>();

    // Listener interface
    public interface ConditionChangeListener {
        void onConditionChanged(String conditionName, boolean oldValue, boolean newValue);
    }

    // Listener for condition changes
    private static ConditionChangeListener changeListener = null;

    // Static initializer - lädt Conditions beim ersten Zugriff
    static {
        loadConditionsFromFile();
    }

    /**
     * Sets a listener that will be notified when conditions change
     */
    public static void setChangeListener(ConditionChangeListener listener) {
        changeListener = listener;
    }

    /**
     * Lädt alle verfügbaren Conditions aus conditions.txt
     */
    private static void loadConditionsFromFile() {
        File file = new File(CONDITIONS_FILE);

        // Falls Datei nicht existiert, erstelle sie mit Defaults
        if (!file.exists()) {
            System.out.println("Conditions-Datei nicht gefunden, erstelle neue: " + CONDITIONS_FILE);
            file.getParentFile().mkdirs();
            createDefaultConditionsFile();
        }

        conditions.clear();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Überspringe Kommentare und leere Zeilen
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Format: conditionName = defaultValue
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String name = parts[0].trim();
                    boolean defaultValue = Boolean.parseBoolean(parts[1].trim());

                    conditions.put(name, defaultValue);
                    System.out.println("Loaded condition: " + name + " = " + defaultValue);
                }
            }

            reader.close();
            System.out.println("✓ Loaded " + conditions.size() + " conditions from: " + CONDITIONS_FILE);
        } catch (Exception e) {
            System.err.println("ERROR loading conditions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Erstellt Standard-Conditions-Datei
     */
    private static void createDefaultConditionsFile() {
        try {
            File file = new File(CONDITIONS_FILE);
            file.getParentFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("# Conditions Definition File\n");
            writer.write("# Format: conditionName = defaultValue\n");
            writer.write("# Add, modify, or remove conditions here - no source code changes needed!\n\n");

            // Migration: Übernehme vorhandene Conditions aus conditions-defaults.txt falls vorhanden
            File defaultsFile = new File(CONDITIONS_DEFAULTS_FILE);
            if (defaultsFile.exists()) {
                writer.write("# Migrated from conditions-defaults.txt:\n");
                BufferedReader reader = new BufferedReader(new FileReader(defaultsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                        writer.write(line + "\n");
                    }
                }
                reader.close();
            } else {
                // Default conditions
                writer.write("pathToWoodIsClear = false\n");
                writer.write("hasLighter = false\n");
                writer.write("hasKey = false\n");
                writer.write("shelfIsBurned = false\n");
                writer.write("doorIsLocked = true\n");
                writer.write("doorIsOpen = false\n");
            }

            writer.close();
            System.out.println("Created default conditions file: " + CONDITIONS_FILE);
        } catch (Exception e) {
            System.err.println("ERROR creating default conditions file: " + e.getMessage());
        }
    }

    /**
     * Setzt eine Condition per Namen
     */
    public static void setCondition(String name, boolean value) {
        boolean oldValue = conditions.getOrDefault(name, false);
        boolean changed = oldValue != value;

        if (conditions.containsKey(name)) {
            conditions.put(name, value);
            System.out.println("Condition gesetzt: " + name + " = " + value);
        } else {
            System.err.println("⚠️ Unbekannte Condition (wird trotzdem gesetzt): " + name);
            conditions.put(name, value);
        }

        // Notify listener if value actually changed
        if (changed && changeListener != null) {
            changeListener.onConditionChanged(name, oldValue, value);
        }
    }

    /**
     * Gibt eine Condition per Namen zurück
     */
    public static boolean getCondition(String name) {
        Boolean value = conditions.get(name);
        if (value == null) {
            System.err.println("⚠️ Unbekannte Condition: " + name + " (returning false)");
            return false;
        }
        return value;
    }

    /**
     * Fügt eine neue Condition hinzu und speichert in conditions.txt
     */
    public static void addCondition(String name, boolean defaultValue) {
        conditions.put(name, defaultValue);
        saveConditionsToFile();
        System.out.println("✓ Neue Condition hinzugefügt: " + name + " = " + defaultValue);
    }

    /**
     * Fügt eine Condition nur zur Laufzeit hinzu, OHNE in conditions.txt zu speichern
     * Wird für isInInventory_* Conditions verwendet, die nur in Item-Dateien gespeichert werden
     */
    public static void addConditionRuntimeOnly(String name, boolean defaultValue) {
        conditions.put(name, defaultValue);
        System.out.println("✓ Runtime Condition hinzugefügt: " + name + " = " + defaultValue);
    }

    /**
     * Löscht eine Condition
     */
    public static void removeCondition(String name) {
        if (conditions.remove(name) != null) {
            saveConditionsToFile();
            System.out.println("✓ Condition gelöscht: " + name);
        } else {
            System.err.println("⚠️ Condition nicht gefunden: " + name);
        }
    }

    /**
     * Gibt alle Condition-Namen zurück
     */
    public static Set<String> getAllConditionNames() {
        return new LinkedHashSet<>(conditions.keySet());
    }

    /**
     * Gibt alle Conditions als Map zurück
     */
    public static Map<String, Boolean> getAllConditions() {
        return new LinkedHashMap<>(conditions);
    }

    /**
     * Prüft ob eine Condition existiert
     */
    public static boolean conditionExists(String name) {
        return conditions.containsKey(name);
    }

    /**
     * Speichert alle Conditions in conditions.txt
     */
    public static void saveConditionsToFile() {
        try {
            File file = new File(CONDITIONS_FILE);
            file.getParentFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("# Conditions Definition File\n");
            writer.write("# Format: conditionName = defaultValue\n");
            writer.write("# Add, modify, or remove conditions here - no source code changes needed!\n\n");

            for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                // Skip isInInventory_* conditions - they are stored in item files only
                if (!entry.getKey().startsWith("isInInventory_")) {
                    writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
                }
            }

            writer.close();
            System.out.println("✓ Conditions saved to: " + CONDITIONS_FILE);
        } catch (Exception e) {
            System.err.println("ERROR saving conditions: " + e.getMessage());
        }
    }

    /**
     * Lädt Conditions aus progress.txt (aktuelle Spielstände)
     */
    public static void loadFromProgress(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("Progress-Datei nicht gefunden: " + filename);
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#") || line.startsWith("currentScene=")) {
                    continue;
                }

                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    boolean value = Boolean.parseBoolean(parts[1].trim());

                    setCondition(key, value);
                }
            }

            reader.close();
            System.out.println("Conditions geladen aus: " + filename);
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Conditions: " + e.getMessage());
        }
    }

    /**
     * Speichert alle Conditions in progress.txt
     */
    public static void saveToProgress(String filename, String currentScene) {
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write("# Game Progress\n");
            writer.write("currentScene=" + currentScene + "\n");
            writer.write("\n# Boolean Variables\n");

            // Dynamisch alle Conditions speichern
            for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }

            writer.close();
            System.out.println("Conditions gespeichert");
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern der Conditions: " + e.getMessage());
        }
    }

    /**
     * Setzt alle Conditions auf Default-Werte zurück
     */
    public static void resetToDefault() {
        System.out.println("Resetting conditions to defaults...");
        loadConditionsFromFile();
        System.out.println("Conditions zurückgesetzt");
    }

    /**
     * Lädt Conditions neu aus der Datei
     */
    public static void reloadConditions() {
        loadConditionsFromFile();
    }
}
