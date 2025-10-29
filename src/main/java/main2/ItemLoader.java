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
        String currentSubSection = "";
        String pendingCondition = null;
        String currentAction = null;
        KeyArea.ActionHandler currentActionHandler = null;
        int clickAreaX = 0;
        int clickAreaY = 0;

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
            } else if (line.startsWith("#MouseHover:")) {
                currentSection = "MOUSEHOVER";
            } else if (line.startsWith("#ImageConditions:")) {
                currentSection = "IMAGECONDITIONS";
            } else if (line.startsWith("#ClickArea:")) {
                currentSection = "CLICKAREA";
                // Clear default click area points when loading custom ones
                if (item != null) {
                    item.getClickAreaPoints().clear();
                    System.out.println("Loading click area for: " + item.getName());
                }
            } else if (line.startsWith("#Actions:")) {
                currentSection = "ACTIONS";
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

                    case "ACTIONS":
                        // Action name (starts with single -)
                        currentAction = value;
                        currentActionHandler = new KeyArea.ActionHandler();
                        if (item != null) {
                            item.addAction(currentAction, currentActionHandler);
                        }
                        break;
                }
            }
            // ClickArea coordinates (###)
            else if (line.startsWith("###")) {
                // Marker for new click area point - do nothing, just continue
                continue;
            }
            // Sub-sections for MouseHover, ImageConditions, Actions, ClickArea
            else if (line.startsWith("--conditions") || line.startsWith("--conditions:")) {
                currentSubSection = "CONDITIONS";
            }
            else if (line.startsWith("--x") && currentSection.equals("CLICKAREA")) {
                String value = line.substring(3).trim();
                if (value.contains("=")) {
                    String[] parts = value.split("=");
                    if (parts.length == 2) {
                        clickAreaX = Integer.parseInt(parts[1].trim().replace(";", ""));
                    }
                }
            }
            else if (line.startsWith("--y") && currentSection.equals("CLICKAREA")) {
                String value = line.substring(3).trim();
                if (value.contains("=")) {
                    String[] parts = value.split("=");
                    if (parts.length == 2) {
                        clickAreaY = Integer.parseInt(parts[1].trim().replace(";", ""));
                        // Add point after reading both x and y
                        if (item != null) {
                            item.addClickAreaPoint(clickAreaX, clickAreaY);
                            System.out.println("  Added click point: (" + clickAreaX + ", " + clickAreaY + ")");
                        }
                    }
                }
            }
            else if (line.startsWith("---") && !line.startsWith("----")) {
                // Condition line
                pendingCondition = line.substring(3).trim().replace(";", "");
            }
            else if (line.startsWith("----Display:")) {
                currentSubSection = "DISPLAY";
            }
            else if (line.startsWith("----Image:")) {
                String imagePath = line.substring(10).trim().replace(";", "");
                if (item != null && pendingCondition != null && currentSection.equals("IMAGECONDITIONS")) {
                    item.addImageCondition(pendingCondition, imagePath);
                }
                pendingCondition = null;
            }
            else if (line.startsWith("----#Dialog:")) {
                currentSubSection = "DIALOG";
            }
            else if (line.startsWith("----##load")) {
                String result = line.substring(4).trim();
                if (currentActionHandler != null && pendingCondition != null) {
                    currentActionHandler.addConditionalResult(pendingCondition, result);
                }
                pendingCondition = null;
            }
            else if (line.startsWith("------")) {
                String value = line.substring(6).trim();

                if (currentSection.equals("MOUSEHOVER") && currentSubSection.equals("DISPLAY")) {
                    // Hover text
                    String displayText = value.replace("\"", "");
                    if (item != null && pendingCondition != null) {
                        item.addHoverDisplayCondition(pendingCondition, displayText);
                    }
                    pendingCondition = null;
                } else if (currentSection.equals("ACTIONS") && currentSubSection.equals("DIALOG")) {
                    // Action dialog result
                    if (currentActionHandler != null && pendingCondition != null) {
                        currentActionHandler.addConditionalResult(pendingCondition, "#Dialog:" + value);
                    }
                    pendingCondition = null;
                }
            }
        }

        reader.close();

        if (item == null) {
            throw new IOException("Invalid item file format - no name found");
        }

        // If no click area was loaded from file, create one based on image
        if (item.getClickAreaPoints().isEmpty()) {
            item.createClickAreaFromImage();
            System.out.println("Created default click area for: " + item.getName());
        } else {
            System.out.println("Loaded item: " + item.getName() + " with " + item.getClickAreaPoints().size() + " click points");
        }

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
