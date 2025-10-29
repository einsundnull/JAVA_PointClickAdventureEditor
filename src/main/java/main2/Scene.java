package main2;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    private String name;
    private String backgroundImagePath;
    private List<KeyArea> keyAreas;
    private List<Path> paths;
    private Map<String, String> dialogs; // dialogName -> dialogText
    private List<Item> items; // Items placed in this scene

    public Scene(String name) {
        this.name = name;
        this.keyAreas = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.dialogs = new HashMap<>();
        this.items = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getBackgroundImagePath() {
        return backgroundImagePath;
    }
    
    public void setBackgroundImagePath(String path) {
        this.backgroundImagePath = path;
    }
    
    public void addKeyArea(KeyArea area) {
        keyAreas.add(area);
    }
    
    public void addPath(Path path) {
        paths.add(path);
    }
    
    public void addDialog(String name, String text) {
        dialogs.put(name, text);
    }
    
    public String getDialog(String name) {
        return dialogs.get(name);
    }
    
    public Map<String, String> getDialogs() {
        return dialogs;
    }

    public List<KeyArea> getKeyAreas() {
        return keyAreas;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    /**
     * Get item at specific point using polygon-based click detection
     */
    public Item getItemAt(Point point) {
        for (Item item : items) {
            if (item.isVisible() && item.containsPoint(point)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Get KeyArea at specific point
     */
    public KeyArea getKeyAreaAt(Point point) {
        for (KeyArea area : keyAreas) {
            if (area.contains(point)) {
                return area;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "Scene{name='" + name + "', keyAreas=" + keyAreas.size() + ", paths=" + paths.size() + ", dialogs=" + dialogs.size() + ", items=" + items.size() + "}";
    }
}