package main2;

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

        // Image File
        writer.write("#ImageFile:\n");
        writer.write("-" + item.getImageFileName() + "\n\n");

        // Image Path
        writer.write("#ImagePath:\n");
        writer.write("-" + item.getImageFilePath() + "\n\n");

        // Position
        writer.write("#Position:\n");
        writer.write("-x = " + item.getPosition().x + ";\n");
        writer.write("-y = " + item.getPosition().y + ";\n\n");

        // Size
        writer.write("#Size:\n");
        writer.write("-width = " + item.getWidth() + ";\n");
        writer.write("-height = " + item.getHeight() + ";\n\n");

        // Conditions
        writer.write("#Conditions:\n");
        Map<String, Boolean> conditions = item.getConditions();
        if (conditions.isEmpty()) {
            writer.write("-none\n");
        } else {
            for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
                writer.write("-" + entry.getKey() + " = " + entry.getValue() + ";\n");
            }
        }
        writer.write("\n");

        // IsInInventory
        writer.write("#IsInInventory:\n");
        writer.write("-" + item.isInInventory() + "\n");

        writer.close();
        System.out.println("Saved item: " + item.getName() + " to " + filename);
    }

    /**
     * Save item to resources/items/[ItemName].txt
     */
    public static void saveItemByName(Item item) throws IOException {
        String filename = "resources/items/" + item.getName() + ".txt";
        saveItem(item, filename);
    }
}
