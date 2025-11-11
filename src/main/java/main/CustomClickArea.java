package main;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom click area for an item with polygon points and conditions.
 */
public class CustomClickArea {
    private List<Point> points; // Polygon points (x, y, z)
    private Map<String, Boolean> conditions; // condition name -> required value
    private String hoverText; // Mouse hover text
    private Polygon polygon; // Cached polygon for hit testing

    public CustomClickArea() {
        this.points = new ArrayList<>();
        this.conditions = new LinkedHashMap<>();
        this.hoverText = "";
        this.polygon = null;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public void addPoint(int x, int y) {
        this.points.add(new Point(x, y));
        updatePolygon();
    }

    public void addPoint(Point point) {
        this.points.add(point);
        updatePolygon();
    }

    public void removePoint(int index) {
        if (index >= 0 && index < points.size()) {
            points.remove(index);
            updatePolygon();
        }
    }

    public void removePoint(Point point) {
        points.remove(point);
        updatePolygon();
    }

    /**
     * Updates the cached polygon from the points list.
     * Call this after modifying points externally.
     */
    public void updatePolygon() {
        if (points.isEmpty()) {
            polygon = null;
            return;
        }

        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xPoints[i] = points.get(i).x;
            yPoints[i] = points.get(i).y;
        }

        polygon = new Polygon(xPoints, yPoints, points.size());
    }

    /**
     * Gets the cached polygon. Updates it if necessary.
     */
    public Polygon getPolygon() {
        if (polygon == null) {
            updatePolygon();
        }
        return polygon;
    }

    /**
     * Checks if a point is inside this custom click area's polygon.
     */
    public boolean containsPoint(Point point) {
        if (polygon == null) {
            updatePolygon();
        }
        return polygon != null && polygon.contains(point);
    }

    public Map<String, Boolean> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Boolean> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(String conditionName, boolean requiredValue) {
        this.conditions.put(conditionName, requiredValue);
    }

    public void removeCondition(String conditionName) {
        this.conditions.remove(conditionName);
    }

    public String getHoverText() {
        return hoverText;
    }

    public void setHoverText(String hoverText) {
        this.hoverText = hoverText;
    }

    /**
     * Evaluates whether this custom click area should be active based on current game conditions.
     */
    public boolean shouldBeActive(GameProgress gameProgress) {
        // If no conditions are set, always active
        if (conditions.isEmpty()) {
            return true;
        }

        // Check all conditions
        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            String conditionName = entry.getKey();
            boolean requiredValue = entry.getValue();

            boolean currentValue = Conditions.getCondition(conditionName);

            if (currentValue != requiredValue) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a deep copy of this CustomClickArea.
     */
    public CustomClickArea copy() {
        CustomClickArea copy = new CustomClickArea();
        copy.setHoverText(this.hoverText);
        copy.setConditions(new LinkedHashMap<>(this.conditions));

        // Deep copy points
        for (Point p : this.points) {
            copy.addPoint(p.x, p.y);
        }

        return copy;
    }

    @Override
    public String toString() {
        return "CustomClickArea [" + points.size() + " points, hover: \"" + hoverText + "\"]";
    }
}
