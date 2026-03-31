/**
 * Location.java
 * Represents a delivery node / point on the 2D map.
 * Used by all algorithms: Convex Hull, TSP, Kruskal's MST.
 */
public class Location {
    public int id;
    public int x, y; // pixel coordinates on the canvas

    public Location(int id, int x, int y) {
        this.id   = id;
        this.x    = x;
        this.y    = y;
    }

    /**
     * Euclidean distance to another Location.
     * Used by Edge and TSP backtracking.
     */
    public double distanceTo(Location other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    @Override
    public String toString() {
        return "L" + id + "(" + x + "," + y + ")";
    }
}
