package main2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Utility class to manage condition references across all .txt files
 * Handles cascade deletion when a condition is removed
 */
public class ConditionReferenceManager {

	/**
	 * Deletes all references to a condition from .txt files in resources directory
	 * @param conditionName The name of the condition to remove
	 * @return List of files that were modified
	 */
	public static List<String> removeConditionReferences(String conditionName) throws IOException {
		List<String> modifiedFiles = new ArrayList<>();
		File resourcesDir = new File("resources");

		if (!resourcesDir.exists()) {
			throw new IOException("Resources directory not found");
		}

		// Find all .txt files recursively
		List<File> txtFiles = findAllTxtFiles(resourcesDir);

		for (File file : txtFiles) {
			if (removeConditionFromFile(file, conditionName)) {
				modifiedFiles.add(file.getPath());
			}
		}

		return modifiedFiles;
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
	 * Remove condition references from a single file
	 * @return true if file was modified
	 */
	private static boolean removeConditionFromFile(File file, String conditionName) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		List<String> newLines = new ArrayList<>();
		boolean modified = false;

		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);

			// Check if this line references the condition
			if (isConditionReference(line, conditionName)) {
				modified = true;

				// Get the indentation level of this condition line
				int conditionLevel = getIndentationLevel(line);

				// Skip this condition line
				i++;

				// Skip all following lines that are result lines (deeper indentation)
				while (i < lines.size()) {
					String nextLine = lines.get(i);
					int nextLevel = getIndentationLevel(nextLine);

					// Stop if we reach a line at the same or lower indentation level
					if (!nextLine.trim().isEmpty() && nextLevel <= conditionLevel) {
						break;
					}

					// Skip result lines (deeper indentation)
					i++;
				}
			} else if (isSetBooleanReference(line, conditionName)) {
				// Remove SetBoolean references
				modified = true;
				i++;
			} else {
				// Keep this line
				newLines.add(line);
				i++;
			}
		}

		// Write back if modified
		if (modified) {
			// Clean up empty sections
			newLines = cleanupEmptySections(newLines);
			Files.write(file.toPath(), newLines);
		}

		return modified;
	}

	/**
	 * Check if a line is a condition reference (e.g., "---hasKey = true;")
	 */
	private static boolean isConditionReference(String line, String conditionName) {
		String trimmed = line.trim();

		// Pattern: conditionName = true/false;
		Pattern pattern = Pattern.compile("^" + Pattern.quote(conditionName) + "\\s*=\\s*(true|false)\\s*;?$");
		return pattern.matcher(trimmed).matches();
	}

	/**
	 * Check if a line is a SetBoolean reference (e.g., "----#SetBoolean:hasKey=false")
	 */
	private static boolean isSetBooleanReference(String line, String conditionName) {
		String trimmed = line.trim();

		// Pattern: #SetBoolean:conditionName=true/false
		Pattern pattern = Pattern.compile("^#SetBoolean:\\s*" + Pattern.quote(conditionName) + "\\s*=\\s*(true|false)$");
		return pattern.matcher(trimmed).matches();
	}

	/**
	 * Get the indentation level of a line (number of leading dashes)
	 */
	private static int getIndentationLevel(String line) {
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c == '-') {
				count++;
			} else {
				break;
			}
		}
		return count;
	}

	/**
	 * Clean up empty sections (e.g., --conditions with no entries)
	 */
	private static List<String> cleanupEmptySections(List<String> lines) {
		List<String> cleaned = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String trimmed = line.trim();

			// Check if this is a "conditions" header
			if (trimmed.equals("conditions")) {
				int level = getIndentationLevel(line);

				// Look ahead to see if there are any condition entries
				boolean hasEntries = false;
				int j = i + 1;
				while (j < lines.size()) {
					String nextLine = lines.get(j);
					int nextLevel = getIndentationLevel(nextLine);

					if (!nextLine.trim().isEmpty() && nextLevel <= level) {
						break;
					}

					// If there's a non-empty line with deeper indentation, there are entries
					if (!nextLine.trim().isEmpty() && nextLevel > level) {
						hasEntries = true;
						break;
					}

					j++;
				}

				// Only keep the "conditions" header if it has entries
				if (hasEntries) {
					cleaned.add(line);
				}
			} else {
				cleaned.add(line);
			}
		}

		return cleaned;
	}

	/**
	 * Remove a condition from conditions-defaults.txt
	 * NOTE: This is kept for backward compatibility with old conditions-defaults.txt files
	 */
	public static boolean removeFromDefaults(String conditionName) throws IOException {
		// Check if old conditions-defaults.txt exists
		File oldDefaultsFile = new File("resources/conditions-defaults.txt");
		if (oldDefaultsFile.exists()) {
			List<String> lines = Files.readAllLines(oldDefaultsFile.toPath());
			List<String> newLines = new ArrayList<>();
			boolean modified = false;

			for (String line : lines) {
				String trimmed = line.trim();

				// Skip lines that define this condition
				if (trimmed.startsWith(conditionName + " =") || trimmed.startsWith(conditionName + "=")) {
					modified = true;
					continue;
				}

				newLines.add(line);
			}

			if (modified) {
				Files.write(oldDefaultsFile.toPath(), newLines);
				return true;
			}
		}

		return false;
	}

	/**
	 * Remove a condition from progress files
	 */
	public static void removeFromProgressFiles(String conditionName) throws IOException {
		String[] progressFiles = {
			"resources/progress.txt",
			"resources/progress-default.txt"
		};

		for (String filename : progressFiles) {
			File file = new File(filename);
			if (!file.exists()) {
				continue;
			}

			List<String> lines = Files.readAllLines(file.toPath());
			List<String> newLines = new ArrayList<>();

			for (String line : lines) {
				String trimmed = line.trim();

				// Skip lines that define this condition
				if (trimmed.startsWith(conditionName + "=") || trimmed.startsWith(conditionName + " =")) {
					continue;
				}

				newLines.add(line);
			}

			Files.write(file.toPath(), newLines);
		}
	}

	/**
	 * NOTE: This method is now obsolete as Conditions are managed dynamically.
	 * Conditions are stored in resources/conditions/conditions.txt - no source code changes needed!
	 *
	 * @deprecated Conditions are now dynamic and don't require source code changes
	 */
	@Deprecated
	public static String generateConditionsJavaRemovalInstructions(String conditionName) {
		return "ℹ️ No manual steps required!\n\n" +
		       "Conditions are now managed dynamically from:\n" +
		       "resources/conditions/conditions.txt\n\n" +
		       "The condition '" + conditionName + "' has been automatically removed.\n" +
		       "No source code changes are necessary!";
	}

	/**
	 * NOTE: This method is now obsolete as Conditions are managed dynamically.
	 * Use Conditions.removeCondition() instead.
	 *
	 * @deprecated Use Conditions.removeCondition() instead
	 */
	@Deprecated
	public static boolean removeFromConditionsJava(String conditionName) throws IOException {
		System.out.println("⚠️ removeFromConditionsJava() is deprecated - conditions are now dynamic!");
		System.out.println("   Use Conditions.removeCondition() instead.");
		return false;
	}

	/**
	 * Find all references to a condition (for reporting purposes)
	 */
	public static Map<String, List<Integer>> findConditionReferences(String conditionName) throws IOException {
		Map<String, List<Integer>> references = new LinkedHashMap<>();
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
				if (isConditionReference(line, conditionName) || isSetBooleanReference(line, conditionName)) {
					lineNumbers.add(i + 1); // 1-based line numbers
				}
			}

			if (!lineNumbers.isEmpty()) {
				references.put(file.getPath(), lineNumbers);
			}
		}

		return references;
	}
}
