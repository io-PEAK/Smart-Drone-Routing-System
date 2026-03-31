/**
 * UnionFind.java  (Disjoint Set Union with Path Compression + Rank)
 *
 * Used by Kruskal's MST algorithm to detect cycles in O(α(n)) time.
 * Each Location's id maps directly to an index in the parent/rank arrays.
 */
public class UnionFind {
    private final int[] parent;
    private final int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank   = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    /** Find with path compression. */
    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    /**
     * Union by rank.
     * @return false if x and y are already in the same set (would form a cycle).
     */
    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        if (rank[px] < rank[py]) { int tmp = px; px = py; py = tmp; }
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }
}
