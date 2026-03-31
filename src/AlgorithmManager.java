import java.util.*;

/**
 * Manages core routing and geofencing algorithms.
 */
public class AlgorithmManager {

    /**
     * Computes the Convex Hull using Graham Scan.
     * Returns vertices in counter-clockwise order.
     */
    public List<Location> convexHull(List<Location> points) {
        int n = points.size();
        if (n < 3) return new ArrayList<>(points);

        Location pivot = points.get(0);
        for (Location p : points) {
            if (p.y > pivot.y || (p.y == pivot.y && p.x < pivot.x)) pivot = p;
        }

        final Location anchor = pivot;
        List<Location> sorted = new ArrayList<>(points);
        sorted.remove(anchor);
        sorted.sort((a, b) -> {
            double angleA = Math.atan2(a.y - anchor.y, a.x - anchor.x);
            double angleB = Math.atan2(b.y - anchor.y, b.x - anchor.x);
            if (Math.abs(angleA - angleB) > 1e-9) return Double.compare(angleA, angleB);
            return Double.compare(anchor.distanceTo(a), anchor.distanceTo(b));
        });

        Deque<Location> stack = new ArrayDeque<>();
        stack.push(anchor);
        for (Location p : sorted) {
            while (stack.size() >= 2) {
                Location top = stack.peek();
                Iterator<Location> it = stack.iterator();
                it.next();
                Location second = it.next();
                if (cross(second, top, p) < 0) stack.pop(); // Keep collinear points
                else break;
            }
            stack.push(p);
        }

        return new ArrayList<>(stack);
    }

    private double cross(Location O, Location A, Location B) {
        return (long)(A.x - O.x) * (B.y - O.y) - (long)(A.y - O.y) * (B.x - O.x);
    }

    /**
     * Finds shortest Hamiltonian cycle via backtracking with pruning.
     */
    public List<Location> tspBacktracking(List<Location> locations) {
        int n = locations.size();
        if (n <= 1) return new ArrayList<>(locations);
        if (n == 2) return new ArrayList<>(locations);

        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        List<Integer> best = new ArrayList<>();
        double[] bestDist = {Double.MAX_VALUE};

        visited[0] = true;
        path.add(0);

        tspHelper(locations, visited, path, best, bestDist, 0, n);

        List<Location> result = new ArrayList<>();
        for (int idx : best) result.add(locations.get(idx));
        return result;
    }

    private void tspHelper(List<Location> locs, boolean[] visited,
                            List<Integer> path, List<Integer> best,
                            double[] bestDist, double currentDist, int n) {
        if (path.size() == n) {
            double total = currentDist + locs.get(path.get(path.size() - 1))
                                             .distanceTo(locs.get(path.get(0)));
            if (total < bestDist[0]) {
                bestDist[0] = total;
                best.clear();
                best.addAll(path);
            }
            return;
        }

        int last = path.get(path.size() - 1);
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                double added = locs.get(last).distanceTo(locs.get(i));
                if (currentDist + added >= bestDist[0]) continue;

                visited[i] = true;
                path.add(i);
                tspHelper(locs, visited, path, best, bestDist, currentDist + added, n);
                path.remove(path.size() - 1);
                visited[i] = false;
            }
        }
    }

    /**
     * Builds MST using Kruskal's algorithm.
     */
    public List<Edge> kruskalMST(List<Location> locations) {
        int n = locations.size();
        List<Edge> mst = new ArrayList<>();
        if (n < 2) return mst;

        List<Edge> allEdges = new ArrayList<>();
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                allEdges.add(new Edge(locations.get(i), locations.get(j)));

        Collections.sort(allEdges);

        UnionFind uf = new UnionFind(n);
        Map<Integer, Integer> idxMap = new HashMap<>();
        for (int i = 0; i < n; i++) idxMap.put(locations.get(i).id, i);

        for (Edge e : allEdges) {
            int ui = idxMap.get(e.u.id);
            int vi = idxMap.get(e.v.id);
            if (uf.union(ui, vi)) {
                mst.add(e);
                if (mst.size() == n - 1) break;
            }
        }
        return mst;
    }
}
