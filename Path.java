package main;

import java.awt.Point;
import java.util.List;

/**
 * Represents a path with points for movement/navigation.
 * Extends CustomClickArea to inherit point management and polygon functionality.
 */
public class Path extends CustomClickArea {

    public Path() {
        super();
    }

    public Path(List<Point> points) {
        super();
        setPoints(points);
        updatePolygon();
    }

    // Legacy support - deprecated
    @Deprecated
    public void setKeyAreaName(String keyAreaName) {
        // No-op for backward compatibility
    }

    /**
     * Checks if a point is on or near the path within a given tolerance.
     * @param point The point to check
     * @param tolerance Distance tolerance in pixels
     * @return true if the point is within tolerance of any path point
     */
    public boolean isPointOnPath(Point point, int tolerance) {
        for (Point pathPoint : getPoints()) {
            double distance = pathPoint.distance(point);
            if (distance <= tolerance) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Path{points=" + getPoints().size() + "}";
    }
}