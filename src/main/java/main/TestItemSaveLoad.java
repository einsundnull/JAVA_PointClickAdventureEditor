package main;

import java.awt.Point;

/**
 * Test program to verify Item save/load with MovingRange and Path
 */
public class TestItemSaveLoad {

    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Item Save/Load with MovingRange and Path ===\n");

            // Test 1: Load existing item
            System.out.println("Test 1: Loading cup.txt...");
            Item cup = ItemLoader.loadItemByName("cup");
            System.out.println("✓ Loaded: " + cup.getName());
            System.out.println("  Position: (" + cup.getPosition().x + ", " + cup.getPosition().y + ")");
            System.out.println("  Size: " + cup.getWidth() + "x" + cup.getHeight());
            System.out.println();

            // Test 2: Add MovingRange
            System.out.println("Test 2: Adding MovingRange with 3 points...");
            MovingRange range = new MovingRange();
            range.addPoint(new Point(100, 100));
            range.addPoint(new Point(200, 100));
            range.addPoint(new Point(150, 200));
            cup.addMovingRange(range);
            System.out.println("✓ Added MovingRange with " + range.getPoints().size() + " points");
            System.out.println();

            // Test 3: Add Path
            System.out.println("Test 3: Adding Path with 4 points...");
            Path path = new Path();
            path.addPoint(new Point(300, 300));
            path.addPoint(new Point(400, 350));
            path.addPoint(new Point(500, 400));
            path.addPoint(new Point(600, 450));
            cup.addPath(path);
            System.out.println("✓ Added Path with " + path.getPoints().size() + " points");
            System.out.println();

            // Test 4: Save to test file
            String testFilename = "resources/items/cup_test.txt";
            System.out.println("Test 4: Saving to " + testFilename + "...");
            ItemSaver.saveItem(cup, testFilename);
            System.out.println("✓ Saved successfully");
            System.out.println();

            // Test 5: Load from test file
            System.out.println("Test 5: Loading from " + testFilename + "...");
            Item cupLoaded = ItemLoader.loadItem(testFilename);
            System.out.println("✓ Loaded: " + cupLoaded.getName());
            System.out.println("  MovingRanges: " + cupLoaded.getMovingRanges().size());
            if (!cupLoaded.getMovingRanges().isEmpty()) {
                MovingRange loadedRange = cupLoaded.getMovingRanges().get(0);
                System.out.println("    - MovingRange has " + loadedRange.getPoints().size() + " points:");
                for (Point p : loadedRange.getPoints()) {
                    System.out.println("      (" + p.x + ", " + p.y + ")");
                }
            }
            System.out.println("  Paths: " + cupLoaded.getPaths().size());
            if (!cupLoaded.getPaths().isEmpty()) {
                Path loadedPath = cupLoaded.getPaths().get(0);
                System.out.println("    - Path has " + loadedPath.getPoints().size() + " points:");
                for (Point p : loadedPath.getPoints()) {
                    System.out.println("      (" + p.x + ", " + p.y + ")");
                }
            }
            System.out.println();

            // Test 6: Verify data integrity
            System.out.println("Test 6: Verifying data integrity...");
            boolean success = true;

            if (cupLoaded.getMovingRanges().size() != 1) {
                System.out.println("✗ FAIL: Expected 1 MovingRange, got " + cupLoaded.getMovingRanges().size());
                success = false;
            } else {
                MovingRange loadedRange = cupLoaded.getMovingRanges().get(0);
                if (loadedRange.getPoints().size() != 3) {
                    System.out.println("✗ FAIL: Expected 3 points in MovingRange, got " + loadedRange.getPoints().size());
                    success = false;
                } else {
                    System.out.println("✓ MovingRange has correct number of points");
                }
            }

            if (cupLoaded.getPaths().size() != 1) {
                System.out.println("✗ FAIL: Expected 1 Path, got " + cupLoaded.getPaths().size());
                success = false;
            } else {
                Path loadedPath = cupLoaded.getPaths().get(0);
                if (loadedPath.getPoints().size() != 4) {
                    System.out.println("✗ FAIL: Expected 4 points in Path, got " + loadedPath.getPoints().size());
                    success = false;
                } else {
                    System.out.println("✓ Path has correct number of points");
                }
            }

            if (success) {
                System.out.println("\n=== ALL TESTS PASSED ✓ ===");
            } else {
                System.out.println("\n=== SOME TESTS FAILED ✗ ===");
            }

            // Keep test file for inspection
            System.out.println("\nTest file saved at: " + testFilename + " (not deleted for inspection)");

        } catch (Exception e) {
            System.err.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
