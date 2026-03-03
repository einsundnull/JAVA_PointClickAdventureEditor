package main;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages MovingRange objects - stores in resources/movingranges/
 * Each MovingRange is stored in a separate file: movingranges/[name].txt
 */
public class MovingRangeManager {
    private static final String FOLDER = "resources/movingranges/";
    private static Map<String, MovingRange> cache = new LinkedHashMap<>();

    static {
        ensureFolderExists();
        loadAll();
    }

    /**
     * Ensures the movingranges folder exists
     */
    private static void ensureFolderExists() {
        File folder = new File(FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("MovingRangeManager: Created folder: " + FOLDER);
        }
    }

    /**
     * Loads all MovingRanges from the folder
     */
    public static void loadAll() {
        cache.clear();

        File folder = new File(FOLDER);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("MovingRangeManager: Folder does not exist: " + FOLDER);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                String name = file.getName().replace(".txt", "");
                MovingRange range = load(name);
                if (range != null) {
                    cache.put(name, range);
                }
            } catch (Exception e) {
                System.err.println("MovingRangeManager: Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("MovingRangeManager: Loaded " + cache.size() + " MovingRanges");
    }

    /**
     * Loads a single MovingRange by name
     */
    public static MovingRange load(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        File file = new File(FOLDER + name + ".txt");
        if (!file.exists()) {
            System.err.println("MovingRangeManager: File not found: " + file.getPath());
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            MovingRange range = null;
            String line;
            String currentSection = "";
            List<Point> points = new ArrayList<>();
            Map<String, Boolean> conditions = new LinkedHashMap<>();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    if (trimmed.startsWith("#Name:")) {
                        // Already have name
                    } else if (trimmed.equals("#Points:")) {
                        currentSection = "POINTS";
                    } else if (trimmed.equals("#Conditions:")) {
                        currentSection = "CONDITIONS";
                    }
                    continue;
                }

                if (currentSection.equals("POINTS") && trimmed.startsWith("-point")) {
                    // Parse: -point1: x=100, y=200, z=1
                    String coordPart = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                    String[] coords = coordPart.split(",");
                    int x = 0, y = 0;

                    for (String coord : coords) {
                        coord = coord.trim();
                        if (coord.startsWith("x=")) {
                            x = Integer.parseInt(coord.substring(2).trim());
                        } else if (coord.startsWith("y=")) {
                            y = Integer.parseInt(coord.substring(2).trim());
                        }
                    }

                    points.add(new Point(x, y));
                }

                if (currentSection.equals("CONDITIONS") && trimmed.startsWith("-")) {
                    // Parse: -hasLighter = true
                    String conditionLine = trimmed.substring(1).trim();
                    if (conditionLine.contains("=")) {
                        String[] parts = conditionLine.split("=");
                        if (parts.length == 2) {
                            String condName = parts[0].trim();
                            boolean condValue = Boolean.parseBoolean(parts[1].trim());
                            conditions.put(condName, condValue);
                        }
                    }
                }
            }

            // Create MovingRange
            range = new MovingRange(name, points);
            range.setConditions(conditions);
            range.updatePolygon();

            System.out.println("MovingRangeManager: Loaded " + name + " with " + points.size() + " points");
            return range;

        } catch (Exception e) {
            System.err.println("MovingRangeManager: Error loading " + name + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves a MovingRange to its file
     */
    public static void save(MovingRange range) {
        if (range == null || range.getName() == null || range.getName().trim().isEmpty()) {
            System.err.println("MovingRangeManager: Cannot save MovingRange - no name");
            return;
        }

        String filename = FOLDER + range.getName() + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("#Name:\n");
            writer.write("-" + range.getName() + "\n\n");

            writer.write("#Points:\n");
            List<Point> points = range.getPoints();
            if (points != null) {
                for (int i = 0; i < points.size(); i++) {
                    Point p = points.get(i);
                    writer.write("-point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
                }
            }
            writer.write("\n");

            // Write conditions if any
            Map<String, Boolean> conditions = range.getConditions();
            if (conditions != null && !conditions.isEmpty()) {
                writer.write("#Conditions:\n");
                for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                    writer.write("-" + entry.getKey() + " = " + entry.getValue() + "\n");
                }
                writer.write("\n");
            }

            System.out.println("MovingRangeManager: Saved " + range.getName() + " to " + filename);

        } catch (IOException e) {
            System.err.println("MovingRangeManager: Error saving " + range.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a new MovingRange
     */
    public static MovingRange create(String name) {
        MovingRange range = new MovingRange(name);
        cache.put(name, range);
        save(range);
        return range;
    }

    /**
     * Gets a MovingRange by name (from cache or loads from file)
     */
    public static MovingRange get(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        // Try loading from file
        MovingRange range = load(name);
        if (range != null) {
            cache.put(name, range);
        }
        return range;
    }

    /**
     * Gets all available MovingRange names
     */
    public static List<String> getAvailableNames() {
        return new ArrayList<>(cache.keySet());
    }

    /**
     * Gets all MovingRanges
     */
    public static Map<String, MovingRange> getAll() {
        return new LinkedHashMap<>(cache);
    }

    /**
     * Deletes a MovingRange
     */
    public static boolean delete(String name) {
        if (cache.containsKey(name)) {
            cache.remove(name);
        }

        File file = new File(FOLDER + name + ".txt");
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("MovingRangeManager: Deleted " + name);
            }
            return deleted;
        }

        return false;
    }

    /**
     * Checks if a MovingRange exists
     */
    public static boolean exists(String name) {
        return cache.containsKey(name) || new File(FOLDER + name + ".txt").exists();
    }

    /**
     * Renames a MovingRange
     */
    public static boolean rename(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }

        if (!cache.containsKey(oldName)) {
            return false;
        }

        MovingRange range = cache.get(oldName);
        range.setName(newName);

        // Delete old file
        delete(oldName);

        // Save with new name
        save(range);

        // Update cache
        cache.remove(oldName);
        cache.put(newName, range);

        return true;
    }
}
