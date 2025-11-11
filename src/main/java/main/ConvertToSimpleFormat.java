package main;

import java.io.File;
import java.util.ArrayList;

/**
 * Converts old scene/item structure to new Simple format
 *
 * Old: resources/scenes/sceneBeach/sceneBeach.txt
 * New: resources/scenes/Beach/MainBeach.txt
 */
public class ConvertToSimpleFormat {

    public static void main(String[] args) {
        try {
            System.out.println("=== Converting to Simple Format ===");

            // Delete old scene files (they're backed up)
            deleteOldSceneFiles();

            // Create new Scene directories and SubScenes
            createNewScenes();

            // Create new Item files
            createNewItems();

            System.out.println("\n✓ Conversion complete!");
            System.out.println("Old files backed up in: resources/backup_*");

        } catch (Exception e) {
            System.err.println("ERROR during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteOldSceneFiles() {
        System.out.println("\n--- Deleting old scene files ---");

        String[] oldSceneDirs = {"sceneBeach", "sceneWood", "sceneTown"};

        for (String dirName : oldSceneDirs) {
            File dir = new File("resources/scenes/" + dirName);
            if (dir.exists() && dir.isDirectory()) {
                // Delete all files in directory
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            System.out.println("  Deleted: " + file.getName());
                        }
                    }
                }
                // Delete directory itself
                if (dir.delete()) {
                    System.out.println("  Deleted directory: " + dirName);
                }
            }
        }
    }

    private static void createNewScenes() throws Exception {
        System.out.println("\n--- Creating new Scene structure ---");

        // Create Beach Scene with SubScenes
        createBeachScene();

        // Create Wood Scene with SubScenes
        createWoodScene();

        // Create Town Scene with SubScenes
        createTownScene();
    }

    private static void createBeachScene() throws Exception {
        System.out.println("\n→ Creating Beach Scene");

        // Create Scene directory
        FileHandlingSimple.createScene("Beach");

        // Create SubScene: MainBeach
        Scene mainBeach = new Scene("MainBeach");
        mainBeach.setBackgroundImagePath("sceneBeach.png");

        // Add a sample item reference
        Item cupItem = new Item("cup");
        cupItem.setPosition(621, 357);
        cupItem.setSize(93, 97);
        mainBeach.addItem(cupItem);

        FileHandlingSimple.saveSubSceneToDefault(mainBeach, "Beach");
        System.out.println("  ✓ Created: Beach/MainBeach.txt");

        // Create SubScene: BeachBar
        Scene beachBar = new Scene("BeachBar");
        beachBar.setBackgroundImagePath("sceneBeach2.png");
        FileHandlingSimple.saveSubSceneToDefault(beachBar, "Beach");
        System.out.println("  ✓ Created: Beach/BeachBar.txt");
    }

    private static void createWoodScene() throws Exception {
        System.out.println("\n→ Creating Wood Scene");

        // Create Scene directory
        FileHandlingSimple.createScene("Wood");

        // Create SubScene: MainWood
        Scene mainWood = new Scene("MainWood");
        mainWood.setBackgroundImagePath("sceneWood.png");
        FileHandlingSimple.saveSubSceneToDefault(mainWood, "Wood");
        System.out.println("  ✓ Created: Wood/MainWood.txt");

        // Create SubScene: ForestPath
        Scene forestPath = new Scene("ForestPath");
        forestPath.setBackgroundImagePath("sceneWood.png");
        FileHandlingSimple.saveSubSceneToDefault(forestPath, "Wood");
        System.out.println("  ✓ Created: Wood/ForestPath.txt");
    }

    private static void createTownScene() throws Exception {
        System.out.println("\n→ Creating Town Scene");

        // Create Scene directory
        FileHandlingSimple.createScene("Town");

        // Create SubScene: MainTown
        Scene mainTown = new Scene("MainTown");
        mainTown.setBackgroundImagePath("sceneTown.png");
        FileHandlingSimple.saveSubSceneToDefault(mainTown, "Town");
        System.out.println("  ✓ Created: Town/MainTown.txt");

        // Create SubScene: TownSquare
        Scene townSquare = new Scene("TownSquare");
        townSquare.setBackgroundImagePath("sceneTown.png");
        FileHandlingSimple.saveSubSceneToDefault(townSquare, "Town");
        System.out.println("  ✓ Created: Town/TownSquare.txt");
    }

    private static void createNewItems() throws Exception {
        System.out.println("\n--- Creating new Item files ---");

        // Delete old item files (backed up)
        File oldCup = new File("resources/items/cup.txt");
        if (oldCup.exists()) {
            oldCup.delete();
            System.out.println("  Deleted old: cup.txt");
        }

        File oldCupProgress = new File("resources/items/cup_progress.txt");
        if (oldCupProgress.exists()) {
            oldCupProgress.delete();
            System.out.println("  Deleted old: cup_progress.txt");
        }

        // Create new cup item
        Item cup = new Item("cup");
        cup.setPosition(621, 357);
        cup.setSize(93, 97);

        // Add conditional image
        ConditionalImage cupImage = new ConditionalImage();
        cupImage.setName("Default");
        cupImage.setImagePath("white_cup.png");
        cupImage.setShowIfTrue(true);

        ArrayList<ConditionalImage> cupImages = new ArrayList<>();
        cupImages.add(cupImage);
        cup.setConditionalImages(cupImages);

        // Initialize empty collections
        cup.setCustomClickAreas(new ArrayList<>());
        cup.setMovingRanges(new ArrayList<>());

        // Save item
        ItemSaver.saveItemByName(cup);
        System.out.println("  ✓ Created: cup.txt (new format)");

        // Create sample items
        createSampleItem("book", "book.png", 300, 200);
        createSampleItem("key", "key.png", 450, 350);
    }

    private static void createSampleItem(String name, String imageName, int x, int y) throws Exception {
        Item item = new Item(name);
        item.setPosition(x, y);
        item.setSize(64, 64);

        // Add conditional image
        ConditionalImage image = new ConditionalImage();
        image.setName("Default");
        image.setImagePath(imageName);
        image.setShowIfTrue(true);

        ArrayList<ConditionalImage> images = new ArrayList<>();
        images.add(image);
        item.setConditionalImages(images);

        // Initialize empty collections
        item.setCustomClickAreas(new ArrayList<>());
        item.setMovingRanges(new ArrayList<>());

        // Save item
        ItemSaver.saveItemByName(item);
        System.out.println("  ✓ Created: " + name + ".txt");
    }
}
