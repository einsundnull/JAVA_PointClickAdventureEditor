package main;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Utility class for character movement with MovingRange constraints.
 * Handles calculation of target positions when clicking outside allowed movement area.
 */
public class CharacterMovement {

    /**
     * Calculates the target position for a character item based on mouse click.
     * Respects MovingRange constraints - if click is outside range, finds nearest point inside.
     *
     * @param characterItem The character item to move
     * @param clickPoint The point where the user clicked
     * @param gameProgress The game progress for checking conditions
     * @return The target position (either clickPoint or nearest valid point)
     */
    public static Point calculateTargetPosition(Item characterItem, Point clickPoint, GameProgress gameProgress) {
        // Get the first active MovingRange
        MovingRange activeRange = getActiveMovingRange(characterItem, gameProgress);

        if (activeRange == null || activeRange.getPoints().isEmpty()) {
            // No MovingRange → Item can move anywhere
            return clickPoint;
        }

        // Check if click is inside MovingRange
        if (activeRange.containsPoint(clickPoint)) {
            return clickPoint; // Move directly there
        }

        // Click is outside → Find nearest point inside MovingRange
        return findNearestPointInside(characterItem.getPosition(), clickPoint, activeRange);
    }

    /**
     * Finds the nearest point inside the MovingRange.
     * Calculates intersection of line (current position → click) with polygon border.
     *
     * @param currentPos Current position of the character
     * @param clickPoint Where the user clicked
     * @param range The MovingRange constraint
     * @return The intersection point on the polygon border, or currentPos if no intersection found
     */
    private static Point findNearestPointInside(Point currentPos, Point clickPoint, MovingRange range) {
        Polygon polygon = range.getPolygon();
        if (polygon == null) {
            return clickPoint;
        }

        // Line from current position to click point
        Line2D.Double line = new Line2D.Double(currentPos, clickPoint);

        // Find intersection with polygon border
        Point intersection = findPolygonIntersection(line, range.getPoints());

        return intersection != null ? intersection : currentPos;
    }

    /**
     * Finds the intersection of a line with a polygon.
     * Tests the line against all edges of the polygon and returns the closest intersection.
     *
     * @param line The line to test
     * @param polygonPoints The polygon vertices
     * @return The closest intersection point, or null if no intersection found
     */
    private static Point findPolygonIntersection(Line2D.Double line, List<Point> polygonPoints) {
        Point closestIntersection = null;
        double closestDistance = Double.MAX_VALUE;
        Point lineStart = new Point((int)line.x1, (int)line.y1);

        // Check each edge of the polygon
        for (int i = 0; i < polygonPoints.size(); i++) {
            Point p1 = polygonPoints.get(i);
            Point p2 = polygonPoints.get((i + 1) % polygonPoints.size());

            Line2D.Double edge = new Line2D.Double(p1, p2);

            // Calculate intersection
            Point intersection = getLineIntersection(line, edge);

            if (intersection != null) {
                double distance = lineStart.distance(intersection);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestIntersection = intersection;
                }
            }
        }

        return closestIntersection;
    }

    /**
     * Calculates the intersection point of two lines.
     * Uses parametric line equation to find intersection.
     *
     * @param line1 First line
     * @param line2 Second line
     * @return The intersection point, or null if lines don't intersect or are parallel
     */
    private static Point getLineIntersection(Line2D.Double line1, Line2D.Double line2) {
        double x1 = line1.x1, y1 = line1.y1, x2 = line1.x2, y2 = line1.y2;
        double x3 = line2.x1, y3 = line2.y1, x4 = line2.x2, y4 = line2.y2;

        double denom = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
        if (Math.abs(denom) < 0.0001) return null; // Parallel lines

        double t = ((x1-x3)*(y3-y4) - (y1-y3)*(x3-x4)) / denom;
        double u = -((x1-x2)*(y1-y3) - (y1-y2)*(x1-x3)) / denom;

        // Check if intersection is within both line segments
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            int x = (int)(x1 + t*(x2-x1));
            int y = (int)(y1 + t*(y2-y1));
            return new Point(x, y);
        }

        return null;
    }

    /**
     * Gets the first active MovingRange of the item based on Conditions.
     *
     * @param item The item to check
     * @param gameProgress The game progress for checking conditions
     * @return The first active MovingRange, or null if none found
     */
    private static MovingRange getActiveMovingRange(Item item, GameProgress gameProgress) {
        List<MovingRange> ranges = item.getMovingRanges();
        if (ranges == null || ranges.isEmpty()) {
            return null;
        }

        // Find first active range (with fulfilled Conditions)
        for (MovingRange range : ranges) {
            if (range.shouldBeActive(gameProgress)) {
                return range;
            }
        }

        // Fallback: First range without conditions
        return ranges.get(0);
    }

    /**
     * Calculates the distance between two points.
     *
     * @param p1 First point
     * @param p2 Second point
     * @return The Euclidean distance
     */
    public static double getDistance(Point p1, Point p2) {
        return p1.distance(p2);
    }

    /**
     * Calculates the angle from one point to another in radians.
     *
     * @param from Source point
     * @param to Target point
     * @return Angle in radians
     */
    public static double getAngle(Point from, Point to) {
        return Math.atan2(to.y - from.y, to.x - from.x);
    }
}
