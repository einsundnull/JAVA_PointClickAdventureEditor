package main;

import java.util.List;

/**
 * Tests the new Simple format to verify it works correctly
 */
public class TestSimpleFormat {

    public static void main(String[] args) {
        System.out.println("=== Testing Simple Format ===\n");

        try {
            // Test 1: Load all scenes
            testLoadScenes();

            // Test 2: Load SubScenes
            testLoadSubScenes();

            // Test 3: Load Items
            testLoadItems();

            System.out.println("\n✓ All tests passed!");

        } catch (Exception e) {
            System.err.println("\n✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLoadScenes() {
        System.out.println("--- Test 1: Loading Scenes ---");

        List<String> scenes = FileHandlingSimple.getAllScenes();

        System.out.println("Found " + scenes.size() + " scenes:");
        for (String sceneName : scenes) {
            System.out.println("  • " + sceneName);
        }

        if (scenes.size() != 3) {
            throw new RuntimeException("Expected 3 scenes, found " + scenes.size());
        }

        System.out.println("✓ Test 1 passed\n");
    }

    private static void testLoadSubScenes() throws Exception {
        System.out.println("--- Test 2: Loading SubScenes ---");

        // Test Beach scene
        List<String> beachSubScenes = FileHandlingSimple.getSubScenes("Beach");
        System.out.println("Beach SubScenes (" + beachSubScenes.size() + "):");
        for (String subScene : beachSubScenes) {
            System.out.println("  • " + subScene);

            // Try to load each subscene
            Scene scene = FileHandlingSimple.loadSubScene("Beach", subScene, null);
            System.out.println("    - Background: " + scene.getBackgroundImagePath());
            System.out.println("    - Items: " + scene.getItems().size());
        }

        if (beachSubScenes.size() != 2) {
            throw new RuntimeException("Expected 2 Beach SubScenes, found " + beachSubScenes.size());
        }

        // Test Wood scene
        List<String> woodSubScenes = FileHandlingSimple.getSubScenes("Wood");
        System.out.println("\nWood SubScenes (" + woodSubScenes.size() + "):");
        for (String subScene : woodSubScenes) {
            System.out.println("  • " + subScene);
        }

        // Test Town scene
        List<String> townSubScenes = FileHandlingSimple.getSubScenes("Town");
        System.out.println("\nTown SubScenes (" + townSubScenes.size() + "):");
        for (String subScene : townSubScenes) {
            System.out.println("  • " + subScene);
        }

        System.out.println("✓ Test 2 passed\n");
    }

    private static void testLoadItems() throws Exception {
        System.out.println("--- Test 3: Loading Items ---");

        // Test loading items
        String[] itemNames = {"cup", "book", "key"};

        for (String itemName : itemNames) {
            Item item = ItemLoader.loadItemByName(itemName);
            System.out.println("Loaded item: " + item.getName());
            System.out.println("  - Position: (" + item.getPosition().x + ", " + item.getPosition().y + ")");
            System.out.println("  - Size: " + item.getWidth() + "x" + item.getHeight());

            if (item.getConditionalImages() != null && !item.getConditionalImages().isEmpty()) {
                String imagePath = item.getConditionalImages().get(0).getImagePath();
                System.out.println("  - Image: " + imagePath);
            }
        }

        System.out.println("✓ Test 3 passed\n");
    }
}
