/**
 * Edge.java
 * Represents a weighted, undirected edge between two Locations.
 * Implements Comparable so Collections.sort() works for Kruskal's algorithm.
 */
public class Edge implements Comparable<Edge> {
    public Location u, v;
    public double weight;

    public Edge(Location u, Location v) {
        this.u      = u;
        this.v      = v;
        this.weight = u.distanceTo(v);
    }

    /**
     * Natural ordering by weight (ascending) — required by Kruskal's MST.
     */
    @Override
    public int compareTo(Edge other) {
        return Double.compare(this.weight, other.weight);
    }

    @Override
    public String toString() {
        return u + " -- " + v + " [" + String.format("%.1f", weight) + "]";
    }
}
