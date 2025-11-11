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

    public MovingRange() {
        super();
    }

    public MovingRange(List<Point> points) {
        super();
        setPoints(points);
        updatePolygon();
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
