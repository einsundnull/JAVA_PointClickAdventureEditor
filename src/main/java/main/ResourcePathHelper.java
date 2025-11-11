package main;

import java.io.File;
import java.net.URL;

/**
 * Helper class to resolve resource paths correctly regardless of working directory.
 *
 * This class handles three scenarios:
 * 1. Running from project root (e.g., eclipse-workspace/PointClick2/)
 * 2. Running from parent directory (e.g., eclipse-workspace/)
 * 3. Running from classpath (JAR file or IDE)
 */
public class ResourcePathHelper {

    private static File projectRoot = null;

    /**
     * Find and return the project root directory.
     * Searches for the directory containing "resources" folder.
     */
    private static File findProjectRoot() {
        if (projectRoot != null) {
            return projectRoot;
        }

        // Check current directory
        File current = new File(".");
        if (hasResourcesFolder(current)) {
            projectRoot = current;
            return projectRoot;
        }

        // Check if we're in a subdirectory of the project
        File parent = current.getParentFile();
        if (parent != null && hasResourcesFolder(parent)) {
            projectRoot = parent;
            return projectRoot;
        }

        // Check common project locations
        String[] possibleRoots = {
            "eclipse-workspace/PointClick2",
            "../eclipse-workspace/PointClick2",
            "../../eclipse-workspace/PointClick2",
            "PointClick2"
        };

        for (String path : possibleRoots) {
            File candidate = new File(path);
            if (hasResourcesFolder(candidate)) {
                projectRoot = candidate;
                return projectRoot;
            }
        }

        // Fallback to current directory
        projectRoot = current;
        return projectRoot;
    }

    private static boolean hasResourcesFolder(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File resourcesDir = new File(dir, "resources");
        return resourcesDir.exists() && resourcesDir.isDirectory();
    }

    /**
     * Find an image file by trying multiple paths.
     *
     * @param relativePath Path relative to resources/images/ (e.g., "scenes/sceneBeach.png")
     * @return File object if found, or null if not found
     */
    public static File findImageFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }

        // Try relative to project root
        File root = findProjectRoot();

        // Try: resources/images/{relativePath}
        File imageFile = new File(root, "resources/images/" + relativePath);
        if (imageFile.exists()) {
            return imageFile;
        }

        // Try: resources/images/scenes/{relativePath} (in case path doesn't include scenes/)
        imageFile = new File(root, "resources/images/scenes/" + relativePath);
        if (imageFile.exists()) {
            return imageFile;
        }

        // Try: resources/images/items/{relativePath}
        imageFile = new File(root, "resources/images/items/" + relativePath);
        if (imageFile.exists()) {
            return imageFile;
        }

        return null;
    }

    /**
     * Load an image from file system or classpath.
     *
     * @param relativePath Path relative to resources/images/ (e.g., "scenes/sceneBeach.png")
     * @return java.net.URL to the image, or null if not found
     */
    public static URL findImageURL(String relativePath) {
        // First try file system
        File imageFile = findImageFile(relativePath);
        if (imageFile != null) {
            try {
                return imageFile.toURI().toURL();
            } catch (Exception e) {
                // Continue to classpath attempt
            }
        }

        // Try classpath resources
        URL url = ResourcePathHelper.class.getResource("/resources/images/" + relativePath);
        if (url != null) {
            return url;
        }

        url = ResourcePathHelper.class.getResource("/resources/images/scenes/" + relativePath);
        if (url != null) {
            return url;
        }

        url = ResourcePathHelper.class.getResource("/resources/images/items/" + relativePath);
        if (url != null) {
            return url;
        }

        return null;
    }

    /**
     * Get the absolute path to the project root.
     */
    public static String getProjectRootPath() {
        return findProjectRoot().getAbsolutePath();
    }

    /**
     * Debug method to print search paths.
     */
    public static void debugPrintPaths(String relativePath) {
        System.out.println("=== ResourcePathHelper Debug ===");
        System.out.println("Looking for: " + relativePath);
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        System.out.println("Project root: " + getProjectRootPath());

        File imageFile = findImageFile(relativePath);
        if (imageFile != null) {
            System.out.println("✓ Found at: " + imageFile.getAbsolutePath());
        } else {
            System.err.println("✗ File not found");
        }

        URL imageURL = findImageURL(relativePath);
        if (imageURL != null) {
            System.out.println("✓ URL: " + imageURL);
        }
        System.out.println("=================================");
    }
}
