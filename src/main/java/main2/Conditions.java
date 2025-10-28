package main2;

import java.io.*;
import java.util.*;

/**
 * Zentrale Klasse für alle Spielbedingungen.
 * Alle Booleans werden hier als static Variablen gespeichert.
 */
public class Conditions {
    // Pfad-Bedingungen
    public static boolean pathToWoodIsClear = false;
    
    // Item-Bedingungen
    public static boolean hasLighter = false;
    public static boolean hasKey = false;
    
    // Zustandsbedingungen
    public static boolean shelfIsBurned = false;
    public static boolean doorIsLocked = true;
    public static boolean doorIsOpen = false;
    
    /**
     * Lädt alle Conditions aus progress.txt
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
            
            writer.write("pathToWoodIsClear=" + pathToWoodIsClear + "\n");
            writer.write("hasLighter=" + hasLighter + "\n");
            writer.write("hasKey=" + hasKey + "\n");
            writer.write("shelfIsBurned=" + shelfIsBurned + "\n");
            writer.write("doorIsLocked=" + doorIsLocked + "\n");
            writer.write("doorIsOpen=" + doorIsOpen + "\n");
            
            writer.close();
            System.out.println("Conditions gespeichert");
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern der Conditions: " + e.getMessage());
        }
    }
    
    /**
     * Setzt eine Condition per Namen
     */
    public static void setCondition(String name, boolean value) {
        switch (name) {
            case "pathToWoodIsClear":
                pathToWoodIsClear = value;
                break;
            case "hasLighter":
                hasLighter = value;
                break;
            case "hasKey":
                hasKey = value;
                break;
            case "shelfIsBurned":
                shelfIsBurned = value;
                break;
            case "doorIsLocked":
                doorIsLocked = value;
                break;
            case "doorIsOpen":
                doorIsOpen = value;
                break;
            default:
                System.err.println("Unbekannte Condition: " + name);
        }
        System.out.println("Condition gesetzt: " + name + " = " + value);
    }
    
    /**
     * Gibt eine Condition per Namen zurück
     */
    public static boolean getCondition(String name) {
        switch (name) {
            case "pathToWoodIsClear":
                return pathToWoodIsClear;
            case "hasLighter":
                return hasLighter;
            case "hasKey":
                return hasKey;
            case "shelfIsBurned":
                return shelfIsBurned;
            case "doorIsLocked":
                return doorIsLocked;
            case "doorIsOpen":
                return doorIsOpen;
            default:
                System.err.println("Unbekannte Condition: " + name);
                return false;
        }
    }
    
    /**
     * Setzt alle Conditions auf Default-Werte zurück
     */
    public static void resetToDefault() {
        // Try to load from defaults file first
        File defaultsFile = new File("resources/conditions-defaults.txt");
        if (defaultsFile.exists()) {
            System.out.println("Loading defaults from: " + defaultsFile.getAbsolutePath());
            loadFromProgress("resources/conditions-defaults.txt");
        } else {
            // Fallback to hardcoded defaults
            pathToWoodIsClear = false;
            hasLighter = false;
            hasKey = false;
            shelfIsBurned = false;
            doorIsLocked = true;
            doorIsOpen = false;
        }
        System.out.println("Conditions zurückgesetzt");
    }
}