package main2;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Loads items from .txt files in resources/items/
 */
public class ItemLoader {

    public static Item loadItem(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Item file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        Item item = null;
        String currentSection = "";

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("//")) {
                continue; // Skip empty lines and comments
            }

            // Section headers
            if (line.startsWith("#Name:")) {
                currentSection = "NAME";
            } else if (line.startsWith("#ImageFile:")) {
                currentSection = "IMAGEFILE";
            } else if (line.startsWith("#ImagePath:")) {
                currentSection = "IMAGEPATH";
            } else if (line.startsWith("#Position:")) {
                currentSection = "POSITION";
            } else if (line.startsWith("#Size:")) {
                currentSection = "SIZE";
            } else if (line.startsWith("#Conditions:")) {
                currentSection = "CONDITIONS";
            } else if (line.startsWith("#IsInInventory:")) {
                currentSection = "INVENTORY";
            }
            // Data lines
            else if (line.startsWith("-")) {
                String value = line.substring(1).trim();

                switch (currentSection) {
                    case "NAME":
                        item = new Item(value);
                        break;

                    case "IMAGEFILE":
                        if (item != null) {
                            item.setImageFileName(value);
                        }
                        break;

                    case "IMAGEPATH":
                        if (item != null) {
                            item.setImageFilePath(value);
                        }
                        break;

                    case "POSITION":
                        if (item != null && value.contains("=")) {
                            String[] parts = value.split("=");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                String val = parts[1].trim().replace(";", "");

                                if (key.equals("x")) {
                                    Point pos = item.getPosition();
                                    item.setPosition(Integer.parseInt(val), pos.y);
                                } else if (key.equals("y")) {
                                    Point pos = item.getPosition();
                                    item.setPosition(pos.x, Integer.parseInt(val));
                                }
                            }
                        }
                        break;

                    case "SIZE":
                        if (item != null && value.contains("=")) {
                            String[] parts = value.split("=");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                String val = parts[1].trim().replace(";", "");

                                if (key.equals("width")) {
                                    item.setWidth(Integer.parseInt(val));
                                } else if (key.equals("height")) {
                                    item.setHeight(Integer.parseInt(val));
                                }
                            }
                        }
                        break;

                    case "CONDITIONS":
                        if (item != null && value.contains("=")) {
                            String conditionLine = value.replace(";", "");
                            String[] parts = conditionLine.split("=");
                            if (parts.length == 2) {
                                String conditionName = parts[0].trim();
                                boolean conditionValue = Boolean.parseBoolean(parts[1].trim());
                                item.addCondition(conditionName, conditionValue);
                            }
                        }
                        break;

                    case "INVENTORY":
                        if (item != null) {
                            item.setInInventory(Boolean.parseBoolean(value));
                        }
                        break;
                }
            }
        }

        reader.close();

        if (item == null) {
            throw new IOException("Invalid item file format - no name found");
        }

        System.out.println("Loaded item: " + item.getName());
        return item;
    }

    /**
     * Load item by name from resources/items/
     */
    public static Item loadItemByName(String itemName) throws IOException {
        String filename = "resources/items/" + itemName + ".txt";
        return loadItem(filename);
    }
}
