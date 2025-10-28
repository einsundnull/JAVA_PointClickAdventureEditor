package main2;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<Point> points;
    
    public Path() {
        this.points = new ArrayList<>();
    }
    
    public void addPoint(Point point) {
        points.add(point);
    }
    
    public void addPoint(int x, int y) {
        points.add(new Point(x, y));
    }
    
    public List<Point> getPoints() {
        return points;
    }
    
    public boolean isPointOnPath(Point point, int tolerance) {
        for (Point pathPoint : points) {
            double distance = pathPoint.distance(point);
            if (distance <= tolerance) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Path{points=" + points.size() + "}";
    }
}