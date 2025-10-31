package main2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class GameProgress {
    private String currentScene;
    private static final String PROGRESS_FILE = "resources/progress.txt";
    private static final String DEFAULT_FILE = "resources/progress_default.txt";
    
    public GameProgress() {
        this.currentScene = "sceneBeach";
    }
    
    public String getCurrentScene() {
        return currentScene;
    }
    
    public void setCurrentScene(String sceneName) {
        this.currentScene = sceneName;
    }
    
    public void loadProgress() {
        File progressFile = new File(PROGRESS_FILE);
        
        if (!progressFile.exists()) {
            System.out.println("progress.txt nicht gefunden, lade progress_default.txt");
            loadFromFile(DEFAULT_FILE);
            saveProgress(); // Create progress.txt from default
        } else {
            loadFromFile(PROGRESS_FILE);
        }
    }
    
    private void loadFromFile(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                System.err.println("Datei nicht gefunden: " + filename);
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.startsWith("currentScene=")) {
                    currentScene = line.substring(13).trim();
                }
            }
            
            reader.close();
            
            // Lade Conditions
            Conditions.loadFromProgress(filename);
            
            System.out.println("Progress geladen aus: " + filename);
        } catch (Exception e) {
            System.err.println("Fehler beim Laden des Progress: " + e.getMessage());
        }
    }
    
    public void saveProgress() {
        Conditions.saveToProgress(PROGRESS_FILE, currentScene);
    }
    
    public void resetToDefault() {
        Conditions.resetToDefault();
        loadFromFile(DEFAULT_FILE);
        System.out.println("Progress zurückgesetzt");
    }
    
    @Override
    public String toString() {
        return "GameProgress{currentScene='" + currentScene + "'}";
    }
}