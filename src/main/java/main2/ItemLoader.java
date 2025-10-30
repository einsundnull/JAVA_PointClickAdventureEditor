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
                System.out.println("ItemLoader: Found #ClickArea: section");
                // Clear default points and mark as custom
                if (item != null) {
                    item.getClickAreaPoints().clear();
                    item.setHasCustomClickArea(true);
                    System.out.println("ItemLoader: Set hasCustomClickArea=true, cleared points");
                }
            } else if (line.startsWith("#Actions:")) {
                currentSection = "ACTIONS";
            }
            // ClickArea coordinates - MUST come BEFORE single-dash check!
            else if (currentSection.equals("CLICKAREA") && line.startsWith("--x")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    clickAreaX = Integer.parseInt(parts[1].trim().replace(";", ""));
                    System.out.println("ItemLoader: Read clickAreaX = " + clickAreaX);
                }
            }
            else if (currentSection.equals("CLICKAREA") && line.startsWith("--y")) {
                // Parse coordinate like SceneLoader does
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    clickAreaY = Integer.parseInt(parts[1].trim().replace(";", ""));
                    // Add point after reading both x and y - exactly like KeyArea
                    if (item != null) {
                        item.getClickAreaPoints().add(new Point(clickAreaX, clickAreaY));
                        System.out.println("ItemLoader: Added point (" + clickAreaX + ", " + clickAreaY + ") to item");
                    }
                }
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
            // ClickArea marker ### - reset coordinates
            else if (currentSection.equals("CLICKAREA") && line.startsWith("###")) {
                // Marker for new click area point - reset coordinates
                clickAreaX = 0;
                clickAreaY = 0;
                System.out.println("ItemLoader: Found ### marker, reset coordinates");
                continue;
            }
            // Sub-sections for MouseHover, ImageConditions, Actions
            // NOTE: This must come AFTER --x and --y checks!
            else if (line.startsWith("--conditions") || line.startsWith("--conditions:")) {
                currentSubSection = "CONDITIONS";
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
                    System.out.println("DEBUG ItemLoader: Loading hover text for item");
                    System.out.println("  item: " + (item != null ? item.getName() : "null"));
                    System.out.println("  pendingCondition: '" + pendingCondition + "'");
                    System.out.println("  displayText: '" + displayText + "'");
                    if (item != null && pendingCondition != null) {
                        item.addHoverDisplayCondition(pendingCondition, displayText);
                        System.out.println("  -> Added hover condition!");
                    } else {
                        System.out.println("  -> NOT added (item or condition is null)");
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

        // Update polygon if custom points were loaded
        if (item.hasCustomClickArea()) {
            item.updateClickAreaPolygon();
            System.out.println("Loaded Custom Click Area for Item: " + item.getName());
            for (int i = 0; i < item.getClickAreaPoints().size(); i++) {
                Point p = item.getClickAreaPoints().get(i);
                System.out.println("  Added point: (" + p.x + ", " + p.y + ")");
            }
        } else {
            System.out.println("Loaded item: " + item.getName());
        }

        return item;
    }

    /**
     * Load item by name from resources/items/
     */
    public static Item loadItemByName(String itemName) throws IOException {
        System.out.println("==========================================");
        System.out.println("DEBUG ItemLoader.loadItemByName() CALLED!");
        System.out.println("  itemName: " + itemName);
        System.out.println("==========================================");
        String filename = "resources/items/" + itemName + ".txt";
        return loadItem(filename);
    }
}
