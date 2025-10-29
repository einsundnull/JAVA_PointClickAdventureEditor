package main2;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyArea {
    public enum Type {
        TRANSITION,
        INTERACTION,
        MOVEMENT_BOUNDS,  // Defines where the player can move
        CHARACTER_RANGE   // Defines movement area for NPCs/characters
    }
    
    private Type type;
    private String name;
    private List<Point> points; // Polygon points
    private Polygon polygon;
    private Map<String, String> imageConditions; // condition -> image path
    private Map<String, ActionHandler> actions; // action name -> handler
    private Map<String, String> hoverDisplayConditions; // condition -> display text
    
    public KeyArea(Type type, String name) {
        this.type = type;
        this.name = name;
        this.points = new ArrayList<>();
        this.imageConditions = new HashMap<>();
        this.actions = new HashMap<>();
        this.hoverDisplayConditions = new HashMap<>();
        updatePolygon();
    }
    
    // Legacy constructor for compatibility
    public KeyArea(Type type, String name, int x1, int y1, int x2, int y2) {
        this(type, name);
        addPoint(new Point(x1, y1));
        addPoint(new Point(x2, y1));
        addPoint(new Point(x2, y2));
        addPoint(new Point(x1, y2));
    }
    
    public void addPoint(Point p) {
        points.add(p);
        updatePolygon();
    }
    
    public void addPoint(int x, int y) {
        addPoint(new Point(x, y));
    }
    
    public List<Point> getPoints() {
        return points;
    }
    
    public void updatePolygon() {
        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];
        
        for (int i = 0; i < points.size(); i++) {
            xPoints[i] = points.get(i).x;
            yPoints[i] = points.get(i).y;
        }
        
        polygon = new Polygon(xPoints, yPoints, points.size());
    }
    
    public Type getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rectangle getBounds() {
        if (polygon != null) {
            return polygon.getBounds();
        }
        return new Rectangle(0, 0, 0, 0);
    }
    
    public Polygon getPolygon() {
        return polygon;
    }
    
    public boolean contains(Point point) {
        return polygon != null && polygon.contains(point);
    }
    
    public void addImageCondition(String condition, String imagePath) {
        imageConditions.put(condition, imagePath);
    }

    public Map<String, String> getImageConditions() {
        return imageConditions;
    }

    public String getImageForConditions(GameProgress progress) {
        for (Map.Entry<String, String> entry : imageConditions.entrySet()) {
            String condition = entry.getKey();
            
            if (condition.equals("none") || evaluateCondition(condition, progress)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    public void addHoverDisplayCondition(String condition, String displayText) {
        hoverDisplayConditions.put(condition, displayText);
    }
    
    public Map<String, String> getHoverDisplayConditions() {
        return hoverDisplayConditions;
    }
    
    /**
     * Gets the hover display text based on conditions
     */
    public String getHoverDisplayText() {
        for (Map.Entry<String, String> entry : hoverDisplayConditions.entrySet()) {
            String condition = entry.getKey();
            String displayText = entry.getValue();
            
            if (condition.equals("none") || evaluateCondition(condition, null)) {
                return displayText;
            }
        }
        return null;
    }
    
    public void addAction(String actionName, ActionHandler handler) {
        actions.put(actionName, handler);
    }
    
    public Map<String, ActionHandler> getActions() {
        return actions;
    }
    
    public String performAction(String actionName, GameProgress progress) {
        ActionHandler handler = actions.get(actionName);
        if (handler != null) {
            return handler.execute(progress);
        }
        return null;
    }
    
    private boolean evaluateCondition(String condition, GameProgress progress) {
        if (condition == null || condition.equals("none")) {
            return true;
        }
        
        // Parse condition like "hasLighter = true"
        String[] parts = condition.split("=");
        if (parts.length == 2) {
            String variable = parts[0].trim();
            boolean expectedValue = Boolean.parseBoolean(parts[1].trim());
            return Conditions.getCondition(variable) == expectedValue;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "KeyArea{type=" + type + ", name='" + name + "', points=" + points.size() + "}";
    }
    
    /**
     * Inner class to handle actions with conditions
     */
    public static class ActionHandler {
        private Map<String, String> conditionalResults; // condition -> result
        
        public ActionHandler() {
            this.conditionalResults = new HashMap<>();
        }
        
        public void addConditionalResult(String condition, String result) {
            conditionalResults.put(condition, result);
        }
        
        public Map<String, String> getConditionalResults() {
            return conditionalResults;
        }
        
        public String execute(GameProgress progress) {
            // Check conditions in order
            for (Map.Entry<String, String> entry : conditionalResults.entrySet()) {
                String condition = entry.getKey();
                
                if (condition.equals("none") || evaluateCondition(condition, progress)) {
                    return entry.getValue();
                }
            }
            return null;
        }
        
        private boolean evaluateCondition(String condition, GameProgress progress) {
            if (condition == null || condition.equals("none")) {
                return true;
            }
            
            String[] parts = condition.split("=");
            if (parts.length == 2) {
                String variable = parts[0].trim();
                boolean expectedValue = Boolean.parseBoolean(parts[1].trim());
                return Conditions.getCondition(variable) == expectedValue;
            }
            
            return false;
        }
    }
}