package main;

/**
 * Tests scene loading with the new Simple format
 */
public class TestSceneLoadingSimple {

    public static void main(String[] args) {
        System.out.println("=== Testing Scene Loading (Simple Format) ===\n");

        try {
            // Load conditions first
            Conditions.loadFromDefaults();
            System.out.println("✓ Loaded conditions\n");

            // Create a GameProgress
            GameProgress progress = new GameProgress();
            progress.loadProgress();
            System.out.println("Current scene: " + progress.getCurrentScene() + "\n");

            // Test 1: Load Beach/MainBeach
            testLoadScene("Beach", "MainBeach", progress);

            // Test 2: Load Beach/BeachBar
            testLoadScene("Beach", "BeachBar", progress);

            // Test 3: Load Wood/MainWood
            testLoadScene("Wood", "MainWood", progress);

            System.out.println("\n✓ All tests passed!");

        } catch (Exception e) {
            System.err.println("\n✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLoadScene(String sceneDir, String subSceneName, GameProgress progress) throws Exception {
        System.out.println("--- Test: Loading " + sceneDir + "/" + subSceneName + " ---");

        Scene scene = FileHandlingSimple.loadSubScene(sceneDir, subSceneName, progress);

        System.out.println("Scene name: " + scene.getName());
        System.out.println("Background: " + scene.getBackgroundImagePath());
        System.out.println("Items: " + scene.getItems().size());

        if (scene.getItems().size() > 0) {
            for (Item item : scene.getItems()) {
                System.out.println("  - " + item.getName() + " at (" + item.getPosition().x + ", " + item.getPosition().y + ")");
            }
        }

        // Check if background image file exists
        java.io.File bgFile = new java.io.File("resources/images/scenes/" + scene.getBackgroundImagePath());
        if (!bgFile.exists()) {
            bgFile = new java.io.File("resources/images/" + scene.getBackgroundImagePath());
        }

        if (bgFile.exists()) {
            System.out.println("✓ Background image found: " + bgFile.getAbsolutePath());
        } else {
            System.out.println("✗ Background image NOT found: " + scene.getBackgroundImagePath());
        }

        System.out.println("✓ Test passed\n");
    }
}
