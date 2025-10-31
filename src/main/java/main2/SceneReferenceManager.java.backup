package main2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Utility class to manage scene references across all files
 * Handles scene renaming and deletion with automatic reference updates
 */
public class SceneReferenceManager {

	/**
	 * Finds all references to a scene in .txt files
	 * @param sceneName The name of the scene to find
	 * @return Map of file paths to line numbers where the scene is referenced
	 */
	public static Map<String, List<Integer>> findSceneReferences(String sceneName) throws IOException {
		Map<String, List<Integer>> references = new LinkedHashMap<>();

		// Search in all .txt files in resources directory
		File resourcesDir = new File("resources");
		if (!resourcesDir.exists()) {
			return references;
		}

		List<File> txtFiles = findAllTxtFiles(resourcesDir);

		for (File file : txtFiles) {
			List<String> lines = Files.readAllLines(file.toPath());
			List<Integer> lineNumbers = new ArrayList<>();

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);

				// Check for scene references:
				// 1. ##loadsceneName or ##scene:sceneName
				// 2. Result type LoadScene with scene name
				// 3. currentScene=sceneName in progress files
				if (lineContainsSceneReference(line, sceneName)) {
					lineNumbers.add(i + 1); // 1-based line numbers
				}
			}

			if (!lineNumbers.isEmpty()) {
				references.put(file.getPath(), lineNumbers);
			}
		}

		return references;
	}

	/**
	 * Checks if a line contains a reference to a scene
	 */
	private static boolean lineContainsSceneReference(String line, String sceneName) {
		String trimmed = line.trim();

		// Pattern 1: ##loadsceneName or ##scene:sceneName
		if (trimmed.equals("##load" + sceneName) || trimmed.equals("##scene:" + sceneName)) {
			return true;
		}

		// Pattern 2: ##sceneName (simple format)
		if (trimmed.equals("##" + sceneName)) {
			return true;
		}

		// Pattern 3: currentScene=sceneName
		if (trimmed.equals("currentScene=" + sceneName)) {
			return true;
		}

		return false;
	}

	/**
	 * Renames all references to a scene across all .txt files
	 * @param oldName The old scene name
	 * @param newName The new scene name
	 * @return List of files that were modified
	 */
	public static List<String> renameSceneReferences(String oldName, String newName) throws IOException {
		List<String> modifiedFiles = new ArrayList<>();
		File resourcesDir = new File("resources");

		if (!resourcesDir.exists()) {
			throw new IOException("Resources directory not found");
		}

		List<File> txtFiles = findAllTxtFiles(resourcesDir);

		for (File file : txtFiles) {
			if (renameSceneInFile(file, oldName, newName)) {
				modifiedFiles.add(file.getPath());
			}
		}

		return modifiedFiles;
	}

	/**
	 * Renames scene references in a single file
	 * @return true if file was modified
	 */
	private static boolean renameSceneInFile(File file, String oldName, String newName) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		List<String> newLines = new ArrayList<>();
		boolean modified = false;

		for (String line : lines) {
			String newLine = line;
			String trimmed = line.trim();

			// Replace scene references
			if (trimmed.equals("##load" + oldName)) {
				newLine = line.replace("##load" + oldName, "##load" + newName);
				modified = true;
			} else if (trimmed.equals("##scene:" + oldName)) {
				newLine = line.replace("##scene:" + oldName, "##scene:" + newName);
				modified = true;
			} else if (trimmed.equals("##" + oldName)) {
				newLine = line.replace("##" + oldName, "##" + newName);
				modified = true;
			} else if (trimmed.equals("currentScene=" + oldName)) {
				newLine = line.replace("currentScene=" + oldName, "currentScene=" + newName);
				modified = true;
			}

			newLines.add(newLine);
		}

		if (modified) {
			Files.write(file.toPath(), newLines);
		}

		return modified;
	}

	/**
	 * Removes all references to a scene from .txt files
	 * @param sceneName The name of the scene to remove
	 * @return List of files that were modified
	 */
	public static List<String> removeSceneReferences(String sceneName) throws IOException {
		List<String> modifiedFiles = new ArrayList<>();
		File resourcesDir = new File("resources");

		if (!resourcesDir.exists()) {
			throw new IOException("Resources directory not found");
		}

		List<File> txtFiles = findAllTxtFiles(resourcesDir);

		for (File file : txtFiles) {
			if (removeSceneFromFile(file, sceneName)) {
				modifiedFiles.add(file.getPath());
			}
		}

		return modifiedFiles;
	}

	/**
	 * Removes scene references from a single file
	 * @return true if file was modified
	 */
	private static boolean removeSceneFromFile(File file, String sceneName) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		List<String> newLines = new ArrayList<>();
		boolean modified = false;

		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);
			String trimmed = line.trim();

			// Check if this line references the scene
			if (lineContainsSceneReference(line, sceneName)) {
				modified = true;
				// Get indentation level
				int level = getIndentationLevel(line);

				// Skip this line
				i++;

				// Skip all following lines with deeper indentation (they belong to this scene reference)
				while (i < lines.size()) {
					String nextLine = lines.get(i);
					int nextLevel = getIndentationLevel(nextLine);

					if (!nextLine.trim().isEmpty() && nextLevel <= level) {
						break;
					}

					i++;
				}
			} else {
				newLines.add(line);
				i++;
			}
		}

		if (modified) {
			Files.write(file.toPath(), newLines);
		}

		return modified;
	}

	/**
	 * Get the indentation level of a line (number of leading dashes or spaces)
	 */
	private static int getIndentationLevel(String line) {
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c == '-' || c == ' ' || c == '\t') {
				count++;
			} else {
				break;
			}
		}
		return count;
	}

	/**
	 * Find all .txt files in a directory recursively
	 */
	private static List<File> findAllTxtFiles(File directory) {
		List<File> txtFiles = new ArrayList<>();

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					txtFiles.addAll(findAllTxtFiles(file));
				} else if (file.getName().endsWith(".txt")) {
					txtFiles.add(file);
				}
			}
		}

		return txtFiles;
	}

	/**
	 * Gets all available scene names
	 */
	public static List<String> getAllSceneNames() {
		List<String> sceneNames = new ArrayList<>();
		File scenesDir = new File("resources/scenes");

		if (!scenesDir.exists()) {
			return sceneNames;
		}

		File[] sceneFiles = scenesDir.listFiles((dir, name) -> name.endsWith(".txt"));
		if (sceneFiles != null) {
			for (File file : sceneFiles) {
				String sceneName = file.getName().replace(".txt", "");
				sceneNames.add(sceneName);
			}
		}

		Collections.sort(sceneNames);
		return sceneNames;
	}

	/**
	 * Deletes a scene file
	 */
	public static boolean deleteSceneFile(String sceneName) {
		File sceneFile = new File("resources/scenes/" + sceneName + ".txt");
		return sceneFile.delete();
	}

	/**
	 * Renames a scene file
	 */
	public static boolean renameSceneFile(String oldName, String newName) {
		File oldFile = new File("resources/scenes/" + oldName + ".txt");
		File newFile = new File("resources/scenes/" + newName + ".txt");

		if (newFile.exists()) {
			return false; // New name already exists
		}

		return oldFile.renameTo(newFile);
	}

	/**
	 * Gets the background image path from a scene file
	 */
	public static String getSceneBackgroundImage(String sceneName) throws IOException {
		File sceneFile = new File("resources/scenes/" + sceneName + ".txt");
		if (!sceneFile.exists()) {
			return null;
		}

		List<String> lines = Files.readAllLines(sceneFile.toPath());
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("backgroundImage=")) {
				return trimmed.substring("backgroundImage=".length()).trim();
			}
		}

		return null;
	}

	/**
	 * Sets the background image path in a scene file
	 */
	public static void setSceneBackgroundImage(String sceneName, String imagePath) throws IOException {
		File sceneFile = new File("resources/scenes/" + sceneName + ".txt");
		if (!sceneFile.exists()) {
			return;
		}

		List<String> lines = Files.readAllLines(sceneFile.toPath());
		List<String> newLines = new ArrayList<>();
		boolean found = false;

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("backgroundImage=")) {
				newLines.add("backgroundImage=" + imagePath);
				found = true;
			} else {
				newLines.add(line);
			}
		}

		// If backgroundImage line doesn't exist, add it at the beginning
		if (!found) {
			newLines.add(0, "backgroundImage=" + imagePath);
		}

		Files.write(sceneFile.toPath(), newLines);
	}
}
