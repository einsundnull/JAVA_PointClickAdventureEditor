package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Saves items to .txt files in resources/items/
 */
public class ItemSaver {

    public static void saveItem(Item item, String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs(); // Create directories if needed

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        // Name
        writer.write("#Name:\n");
        writer.write("-" + item.getName() + "\n\n");

        // Image Path
        writer.write("#ImagePath:\n");
        writer.write("-" + item.getImageFilePath() + "\n\n");

        // Orientation Image Paths
        writer.write("#ImagePathTopLeft:\n");
        writer.write("-" + (item.getImagePathTopLeft() != null ? item.getImagePathTopLeft() : "") + "\n\n");

        writer.write("#ImagePathTop:\n");
        writer.write("-" + (item.getImagePathTop() != null ? item.getImagePathTop() : "") + "\n\n");

        writer.write("#ImagePathTopRight:\n");
        writer.write("-" + (item.getImagePathTopRight() != null ? item.getImagePathTopRight() : "") + "\n\n");

        writer.write("#ImagePathLeft:\n");
        writer.write("-" + (item.getImagePathLeft() != null ? item.getImagePathLeft() : "") + "\n\n");

        writer.write("#ImagePathMiddle:\n");
        writer.write("-" + (item.getImagePathMiddle() != null ? item.getImagePathMiddle() : "") + "\n\n");

        writer.write("#ImagePathRight:\n");
        writer.write("-" + (item.getImagePathRight() != null ? item.getImagePathRight() : "") + "\n\n");

        writer.write("#ImagePathBottomLeft:\n");
        writer.write("-" + (item.getImagePathBottomLeft() != null ? item.getImagePathBottomLeft() : "") + "\n\n");

        writer.write("#ImagePathBottom:\n");
        writer.write("-" + (item.getImagePathBottom() != null ? item.getImagePathBottom() : "") + "\n\n");

        writer.write("#ImagePathBottomRight:\n");
        writer.write("-" + (item.getImagePathBottomRight() != null ? item.getImagePathBottomRight() : "") + "\n\n");

        // Position
        writer.write("#Position:\n");
        writer.write("-x = " + item.getPosition().x + ";\n");
        writer.write("-y = " + item.getPosition().y + ";\n");
        writer.write("-z = 1;\n\n");

        // Size
        writer.write("#Size:\n");
        writer.write("-width = " + item.getWidth() + ";\n");
        writer.write("-height = " + item.getHeight() + ";\n\n");

        // IsInInventory - item.isInInventory() now reads from Condition automatically
        boolean isInInventory = item.isInInventory();
        System.out.println("ItemSaver: Saving isInInventory for " + item.getName() + " = " + isInInventory);
        writer.write("#IsInInventory:\n");
        writer.write("-" + isInInventory + "\n\n");

        // IsFollowingMouse
        writer.write("#IsFollowingMouse:\n");
        writer.write("-" + item.isFollowingMouse() + "\n\n");

        // IsFollowingOnMouseClick
        writer.write("#IsFollowingOnMouseClick:\n");
        writer.write("-" + item.isFollowingOnMouseClick() + "\n\n");

        // MouseHover - Simple format (NEW SCHEMA)
        writer.write("#MouseHover:\n");
        // Get hover text from first display condition, or use item name as fallback
        String hoverText = item.getName(); // Default
        Map<String, String> hoverConditions = item.getHoverDisplayConditions();
        if (hoverConditions != null && !hoverConditions.isEmpty()) {
            // Take first display text as the hover text
            hoverText = hoverConditions.values().iterator().next();
        }
        writer.write("-\"" + hoverText + "\"\n\n");

		// Ensure we only have one container per type
		item.consolidatePointContainers();

		// CustomClickArea - main click area points (from primary CustomClickArea)
		CustomClickArea area = item.getPrimaryCustomClickArea();
		System.out.println("ItemSaver: Saving CustomClickArea for " + item.getName());

		// Migrate legacy clickAreaPoints if necessary
		if ((area == null || area.getPoints().isEmpty()) && item.hasCustomClickArea()
				&& item.getClickAreaPoints() != null && !item.getClickAreaPoints().isEmpty()) {
			area = item.ensurePrimaryCustomClickArea();
			area.getPoints().clear();
			for (java.awt.Point p : item.getClickAreaPoints()) {
				area.getPoints().add(new java.awt.Point(p.x, p.y));
			}
			area.updatePolygon();
			System.out.println("  Migrated legacy clickAreaPoints into primary CustomClickArea");
		}

		writer.write("#CustomClickArea:\n");
		if (area != null && area.getPoints() != null && !area.getPoints().isEmpty()) {
			int pointIndex = 1;
			for (java.awt.Point p : area.getPoints()) {
				writer.write("-point" + pointIndex + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
				System.out.println("  Saved CustomClickArea point " + pointIndex + ": (" + p.x + ", " + p.y + ")");
				pointIndex++;
			}
			System.out.println("  ✓ Saved " + (pointIndex - 1) + " CustomClickArea point(s)");
		} else {
			System.out.println("  No CustomClickArea points to save (empty section written)");
		}
		writer.write("\n");

        // Actions - NEW SCHEMA with IF/THEN/Processes
        writer.write("#Actions:\n");
        Map<String, KeyArea.ActionHandler> actions = item.getActions();

        // Filter out corrupted actions
        boolean hasValidActions = false;
        if (actions != null && !actions.isEmpty()) {
            for (Map.Entry<String, KeyArea.ActionHandler> actionEntry : actions.entrySet()) {
                String actionName = actionEntry.getKey();
                if (actionName != null && !actionName.trim().isEmpty() &&
                    !actionName.contains("dialog-") &&
                    !actionName.contains("conditions") &&
                    !actionName.contains("#Dialog") &&
                    !actionName.startsWith("-")) {
                    hasValidActions = true;
                    break;
                }
            }
        }

        if (hasValidActions) {
            for (Map.Entry<String, KeyArea.ActionHandler> actionEntry : actions.entrySet()) {
                String actionName = actionEntry.getKey();
                KeyArea.ActionHandler handler = actionEntry.getValue();

                // Skip corrupted action names
                if (actionName == null || actionName.trim().isEmpty() ||
                    actionName.contains("dialog-") ||
                    actionName.contains("conditions") ||
                    actionName.contains("#Dialog") ||
                    actionName.startsWith("-")) {
                    continue;
                }

                writer.write("-" + actionName + "\n");

                // Get conditional results
                Map<String, String> actionConditions = handler.getConditionalResults();
                boolean hasConditions = actionConditions != null && !actionConditions.isEmpty();

                // Write Conditions IF (preconditions to execute action)
                writer.write("--Conditions IF\n");
                if (hasConditions) {
                    // Use first condition as IF condition
                    String firstCondition = actionConditions.keySet().iterator().next();
                    if (firstCondition.equals("none")) {
                        writer.write("---none\n");
                    } else {
                        String[] conditionParts = firstCondition.split(" AND ");
                        for (String condPart : conditionParts) {
                            writer.write("---" + condPart.trim() + "\n");
                        }
                    }
                } else {
                    writer.write("---none\n");
                }

                // Write Conditions THEN (state changes after execution)
                writer.write("--Conditions THEN\n");
                writer.write("---none\n"); // Placeholder - would need to parse #SetBoolean results

                // Write Processes (dialog/scene loading)
                writer.write("--Processes:\n");
                if (hasConditions) {
                    String result = actionConditions.values().iterator().next();
                    if (result.startsWith("#Dialog:")) {
                        String dialogId = result.substring(8).trim();
                        writer.write("---" + dialogId + " (Processes need to be implemented yet)\n");
                    } else if (result.startsWith("##load")) {
                        writer.write("---" + result + " (Processes need to be implemented yet)\n");
                    } else {
                        writer.write("---process" + actionName + " (Processes need to be implemented yet)\n");
                    }
                } else {
                    writer.write("---process" + actionName + " (Processes need to be implemented yet)\n");
                }
            }
        } else {
            // Default action
            writer.write("-LookAt\n");
            writer.write("--Conditions IF\n");
            writer.write("---none\n");
            writer.write("--Conditions THEN\n");
            writer.write("---none\n");
            writer.write("--Processes:\n");
            writer.write("---processLookAt (Processes need to be implemented yet)\n");
        }

        // NOTE: #CustomClickAreas (plural) removed in new schema - only #CustomClickArea (singular) exists

		// MovingRange (singular per schema - no conditions in new schema)
		MovingRange primaryRange = item.getPrimaryMovingRange();
		System.out.println("ItemSaver: Saving MovingRange for " + item.getName());
		writer.write("\n#MovingRange:\n");
		if (primaryRange != null && primaryRange.getPoints() != null && !primaryRange.getPoints().isEmpty()) {
			for (int i = 0; i < primaryRange.getPoints().size(); i++) {
				java.awt.Point p = primaryRange.getPoints().get(i);
				writer.write("-point" + (i + 1) + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
				System.out.println("  Saved MovingRange point " + (i + 1) + ": (" + p.x + ", " + p.y + ")");
			}
			System.out.println("  ✓ Saved MovingRange with " + primaryRange.getPoints().size() + " point(s)");
		} else {
			System.out.println("  No MovingRange points to save (empty section written)");
		}

		// Path - singular list of points
		Path primaryPath = item.getPrimaryPath();
		System.out.println("ItemSaver: Saving Path for " + item.getName());
		writer.write("\n#Path:\n");
		if (primaryPath != null && primaryPath.getPoints() != null && !primaryPath.getPoints().isEmpty()) {
			int pointCounter = 1;
			for (java.awt.Point p : primaryPath.getPoints()) {
				writer.write("-point" + pointCounter + ": x=" + p.x + ", y=" + p.y + ", z=1\n");
				System.out.println("  Saved Path point " + pointCounter + ": (" + p.x + ", " + p.y + ")");
				pointCounter++;
			}
			System.out.println("  ✓ Saved Path with " + primaryPath.getPoints().size() + " point(s)");
		} else {
			System.out.println("  No Path points to save (empty section written)");
		}

        writer.close();
        System.out.println("Saved item: " + item.getName() + " to " + filename);
    }

    /**
     * Save item to DEFAULT file: resources/items/[ItemName].txt
     * Use this in EDITOR MODE when saving item definitions/templates
     * This is the DEFAULT version that will be loaded by the editor
     */
    public static void saveItemByName(Item item) throws IOException {
        String filename = "resources/items/" + item.getName() + ".txt";
        saveItem(item, filename);
        System.out.println("✓ Saved to DEFAULT: " + filename);
    }

    /**
     * @deprecated Use saveItemByName() instead - <name>.txt is now the DEFAULT
     */
    @Deprecated
    public static void saveItemToDefault(Item item) throws IOException {
        saveItemByName(item);
    }

    /**
     * @deprecated Progress is now managed differently - _progress.txt files are no longer used
     */
    @Deprecated
    public static void saveItemToProgress(Item item) throws IOException {
        System.out.println("⚠️  WARNING: saveItemToProgress() is deprecated - Progress is managed differently now");
        // Do nothing - progress files are no longer used
    }
}
