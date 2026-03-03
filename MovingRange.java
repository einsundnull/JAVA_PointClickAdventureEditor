package main;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a moving range for an item with Points and conditions.
 * Extends CustomClickArea to inherit polygon functionality.
 * According to Schema: MovingRange uses Points (x, y, z) not KeyArea names.
 */
public class MovingRange extends CustomClickArea {

    private String name;

    public MovingRange() {
        super();
    }

    public MovingRange(String name) {
        super();
        this.name = name;
    }

    public MovingRange(String name, List<Point> points) {
        super();
        this.name = name;
        setPoints(points);
        updatePolygon();
    }

    public MovingRange(List<Point> points) {
        super();
        setPoints(points);
        updatePolygon();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Legacy support - deprecated
    @Deprecated
    public String getKeyAreaName() {
        return ""; // Empty string for backward compatibility
    }

    @Deprecated
    public void setKeyAreaName(String keyAreaName) {
        // No-op for backward compatibility
    }

    // shouldBeActive() is inherited from CustomClickArea

    /**
     * Creates a deep copy of this MovingRange.
     */
    public MovingRange copy() {
        MovingRange copy = new MovingRange();
        // Deep copy points
        for (Point p : this.getPoints()) {
            copy.addPoint(p.x, p.y);
        }
        // Copy conditions
        copy.setConditions(new LinkedHashMap<>(this.getConditions()));
        return copy;
    }

    @Override
    public String toString() {
        return "MovingRange [points: " + getPoints().size() + " points]";
    }
}
