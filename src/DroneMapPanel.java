import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.AffineTransform;

public class DroneMapPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    public enum AlgoType { HULL, TSP, MST }

    // Map Navigation State
    private double zoomLevel = 1.0;
    private double offsetX = 0, offsetY = 0;
    private Point  lastMousePos;
    private float  minimapAlpha = 0f;
    private Timer  minimapTimer;

    private final List<Location>      locations  = new ArrayList<>();
    private       List<Location>      hullPoints = new ArrayList<>();
    private       List<Location>      tspRoute   = new ArrayList<>();
    private       List<Edge>          mstEdges   = new ArrayList<>();

    private boolean showHull = false;
    private boolean showTSP  = false;
    private boolean showMST  = false;

    private boolean showWelcomeOverlay = true;
    private int scrollY = 0;
    private int maxScrollY = 0;
    private Rectangle closeButtonRect = new Rectangle();
    private Rectangle popupCloseRect = new Rectangle();
    
    // Accordion State
    private boolean showDetails = false;
    private boolean expandHull = false;
    private boolean expandTSP  = false;
    private boolean expandMST  = false;
    private Rectangle rectDetails = new Rectangle();
    private Rectangle rectHull = new Rectangle();
    private Rectangle rectTSP  = new Rectangle();
    private Rectangle rectMST  = new Rectangle();
    private Rectangle rectSearch = new Rectangle();

    private static final int    NODE_RADIUS   = 14;
    private static final int    BASE_RADIUS   = 18;
    
    // Drone Animation State
    private boolean animatingDrone = false;
    private double  droneX, droneY, droneAngle;
    private Timer   droneTimer;
    
    private static final Color  COL_NODE      = new Color(108, 99, 255);
    private static final Color  COL_BASE      = new Color(56, 178, 172);
    private static final Color  COL_HULL      = new Color(108, 99, 255);
    private static final Color  COL_TSP       = new Color(255, 120, 100);
    private static final Color  COL_MST       = new Color(56, 178, 172);

    private StatusCallback statusCallback;

    private AlgoType lastAlgoType = null;
    private String popupTitle = "";
    private String popupText  = "";
    private float  popupAlpha = 0f;
    private float  popupY     = 50f;
    private Timer  popupTimer;
    private Location hoveredLocation = null;
    
    private boolean showSearchUI = false;
    private String  searchX = "", searchY = "";
    private int     searchFocus = 0; // 0=X, 1=Y
    private Rectangle rectSearchX = new Rectangle();
    private Rectangle rectSearchY = new Rectangle();
    private Rectangle rectSearchGo = new Rectangle();
    
    private boolean showMissionSelector = false;
    private Rectangle rectSelTSP = new Rectangle();
    private Rectangle rectSelMST = new Rectangle();
    private Rectangle rectSelHull = new Rectangle();
    private String    activeTrajectory = "";
    private AlgorithmSidebar sidebar;
    private float     missionAlpha = 0f;
    private Timer     missionTimer;

    public interface StatusCallback { void update(String msg); }
    public void setSidebar(AlgorithmSidebar s) { this.sidebar = s; }
    public void setStatusCallback(StatusCallback cb) { this.statusCallback = cb; }

    public DroneMapPanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(1000, 700));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        popupTimer = new Timer(16, e -> {
            if (popupAlpha < 1.0f) {
                popupAlpha = Math.min(1.0f, popupAlpha + 0.05f);
                popupY = Math.max(20f, popupY - 1.5f);
                repaint();
            } else {
                popupTimer.stop();
            }
        });

        minimapTimer = new Timer(16, e -> {
            if (minimapAlpha > 0) {
                minimapAlpha -= 0.02f;
                if (minimapAlpha < 0) {
                    minimapAlpha = 0;
                    minimapTimer.stop();
                }
                repaint();
            }
        });

        missionTimer = new Timer(16, e -> {
            if (showMissionSelector) {
                if (missionAlpha < 1.0f) { missionAlpha += 0.08f; repaint(); }
                else missionTimer.stop();
            } else {
                if (missionAlpha > 0f) { missionAlpha -= 0.12f; repaint(); }
                else missionTimer.stop();
            }
        });
    }

    private void wakeMinimap() {
        minimapAlpha = 1.0f;
        if (!minimapTimer.isRunning()) minimapTimer.start();
        repaint();
    }

    public void showIntroOverlay() {
        showWelcomeOverlay = true;
        showDetails = false;
        scrollY = 0;
        expandHull = expandTSP = expandMST = false;
        repaint();
    }

    public void clearAll() {
        if (droneTimer != null) droneTimer.stop();
        animatingDrone = false;
        locations.clear(); hullPoints.clear(); tspRoute.clear(); mstEdges.clear();
        showHull = showTSP = showMST = false;
        popupAlpha = 0f;
        zoomLevel = 1.0;
        offsetX = 0;
        offsetY = 0;
        
        // Ensure sidebar completely vanishes on reset
        if (sidebar != null) {
            sidebar.clear();
            sidebar.setVisible(false);
        }
        
        postStatus("Map reset and cleared.");
        repaint();
    }

    public void showExplanation(AlgoType type, String title, String text) {
        this.lastAlgoType = type;
        this.popupTitle = title;
        this.popupText = text;
        this.popupAlpha = 0f;
        this.popupY = 50f;
        popupTimer.start();
        repaint();
    }

    public List<Location> getLocations() { return locations; }
    public double getDroneX() { return droneX; }
    public double getDroneY() { return droneY; }
    public double getDroneAngle() { return droneAngle; }
    public void setHullPoints(List<Location> hull) { hullPoints = hull; showHull = true; repaint(); }
    public void setTSPRoute(List<Location> route) { tspRoute = route; showTSP = true; repaint(); }
    public void setMSTEdges(List<Edge> edges) { mstEdges = edges; showMST = true; repaint(); }

    public void clearHull() { showHull = false; hullPoints.clear(); repaint(); }
    public void clearTSP()  { 
        if (droneTimer != null) droneTimer.stop();
        animatingDrone = false;
        showTSP  = false; 
        tspRoute.clear();   
        repaint(); 
    }
    public void clearMST()  { showMST  = false; mstEdges.clear();   repaint(); }

    public void startDroneAnimation() {
        int activeCount = 0;
        if (showTSP && tspRoute.size() >= 2) activeCount++;
        if (showMST && !mstEdges.isEmpty()) activeCount++;
        if (showHull && hullPoints.size() >= 3) activeCount++;

        if (activeCount > 1) {
            showMissionSelector = true;
            missionAlpha = 0f;
            missionTimer.start();
            repaint();
            return;
        }

        if (showTSP && tspRoute.size() >= 2) executeFlyMission("TSP");
        else if (showMST && !mstEdges.isEmpty()) executeFlyMission("MST");
        else if (showHull && hullPoints.size() >= 3) executeFlyMission("HULL");
        else postStatus("No flight plan found. Run an algorithm first.");
    }

    private void executeFlyMission(String type) {
        final List<Location> path = new ArrayList<>();
        String missionName = "";

        if (type.equals("TSP")) {
            path.addAll(tspRoute);
            path.add(path.get(0));
            missionName = "Optimal Delivery Route";
        } else if (type.equals("MST")) {
            path.addAll(generateMSTPatrolPath());
            missionName = "Network Backbone Patrol";
        } else if (type.equals("HULL")) {
            path.addAll(hullPoints);
            path.add(path.get(0));
            missionName = "Geofence Perimeter Sweep";
        }

        if (path.isEmpty()) return;

        showMissionSelector = false;
        activeTrajectory = type;
        
        if (sidebar != null) {
            updateSidebarForMission(type);
        }
        
        if (droneTimer != null && droneTimer.isRunning()) droneTimer.stop();
        animatingDrone = true;

        runDroneTimer(path, missionName);
    }

    private void runDroneTimer(List<Location> path, String missionName) {
        droneTimer = new Timer(20, new ActionListener() {
            int currentLeg = 0;
            double t = 0;
            public void actionPerformed(ActionEvent e) {
                if (currentLeg >= path.size() - 1) {
                    droneTimer.stop();
                    animatingDrone = false;
                    postStatus("Mission complete. Drone safely returned to Hub.");
                    repaint();
                    return;
                }
                Location start = path.get(currentLeg);
                Location end = path.get(currentLeg + 1);
                double dist = Math.max(1, start.distanceTo(end));
                double speed = 4.0;
                t += speed / dist;
                if (t >= 1.0) { 
                    t = 0; 
                    currentLeg++; 
                    if (currentLeg < path.size() - 1) {
                        postStatus("Navigating: Segment " + (currentLeg+1) + "/" + (path.size()-1));
                    } else {
                        postStatus("Final Approach: Returning to Base Depot Hub (H)...");
                    }
                }
                else {
                    droneX = start.x + (end.x - start.x) * t;
                    droneY = start.y + (end.y - start.y) * t;
                    droneAngle = Math.atan2(end.y - start.y, end.x - start.x);
                    if (t < 0.05) {
                         postStatus("Executing Leg: targeting node " + (currentLeg + 1));
                    }
                }
                repaint();
            }
        });
        droneTimer.start();
        postStatus("Drone airborne: Navigating " + missionName + "...");
    }

    private List<Location> generateMSTPatrolPath() {
        List<Location> patrol = new ArrayList<>();
        if (locations.isEmpty()) return patrol;
        
        Location hub = locations.get(0);
        java.util.Map<Location, List<Location>> adj = new java.util.HashMap<>();
        for (Edge e : mstEdges) {
            adj.computeIfAbsent(e.u, k -> new ArrayList<>()).add(e.v);
            adj.computeIfAbsent(e.v, k -> new ArrayList<>()).add(e.u);
        }
        
        java.util.Set<Location> visited = new java.util.HashSet<>();
        buildDFSPath(hub, adj, visited, patrol);
        
        if (!patrol.isEmpty() && patrol.get(patrol.size()-1) != hub) {
            patrol.add(hub);
        }
        return patrol;
    }

    private void buildDFSPath(Location curr, java.util.Map<Location, List<Location>> adj, 
                              java.util.Set<Location> visited, List<Location> path) {
        visited.add(curr);
        path.add(curr);
        List<Location> neighbors = adj.getOrDefault(curr, new ArrayList<>());
        for (Location next : neighbors) {
            if (!visited.contains(next)) {
                buildDFSPath(next, adj, visited, path);
                path.add(curr); 
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(Theme.bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        AffineTransform worldTransform = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(zoomLevel, zoomLevel);

        drawGrid(g2);

        if (!activeTrajectory.equals("MST")  && showMST  && !mstEdges.isEmpty())    drawMST(g2);
        if (!activeTrajectory.equals("TSP")  && showTSP  && tspRoute.size() > 1)    drawTSP(g2);
        if (!activeTrajectory.equals("HULL") && showHull && hullPoints.size() >= 3) drawHull(g2);
        
        if (activeTrajectory.equals("MST")  && showMST  && !mstEdges.isEmpty())    drawMST(g2);
        if (activeTrajectory.equals("TSP")  && showTSP  && tspRoute.size() > 1)    drawTSP(g2);
        if (activeTrajectory.equals("HULL") && showHull && hullPoints.size() >= 3) drawHull(g2);
        
        drawLocations(g2);
        if (showMST  && !mstEdges.isEmpty())    drawMSTWeights(g2);
        
        if (animatingDrone) drawDrone(g2);
        
        g2.setTransform(worldTransform); 

        drawMinimap(g2);
        drawPopup(g2);

        if (showWelcomeOverlay) {
            drawWelcomeOverlay(g2);
        }

        drawSearchIcon(g2);
        if (showSearchUI) drawSearchInputBox(g2);
        if (showMissionSelector) drawMissionSelector(g2);

        if (hoveredLocation != null) {
            drawHoverTooltip(g2);
        }
    }

    private void drawMinimap(Graphics2D g2) {
        if (minimapAlpha <= 0) return;
        
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, minimapAlpha));

        int mw = 180, mh = 120;
        int mx = 20, my = getHeight() - mh - 20;
        
        g2.setColor(Theme.tooltipBg);
        g2.fillRoundRect(mx, my, mw, mh, 12, 12);
        g2.setColor(Theme.border);
        g2.drawRoundRect(mx, my, mw, mh, 12, 12);
        
        int minX = 0, minY = 0, maxX = getWidth(), maxY = getHeight();
        for (Location l : locations) {
            minX = Math.min(minX, l.x); minY = Math.min(minY, l.y);
            maxX = Math.max(maxX, l.x); maxY = Math.max(maxY, l.y);
        }
        int pad = 200;
        minX -= pad; minY -= pad; maxX += pad; maxY += pad;
        
        double worldW = maxX - minX;
        double worldH = maxY - minY;
        double scale = Math.min((double)(mw-20)/worldW, (double)(mh-20)/worldH);
        
        Shape oldClip = g2.getClip();
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(mx, my, mw, mh, 12, 12));

        g2.setColor(new Color(108, 99, 255, 40));
        double viewX = (0 - offsetX) / zoomLevel;
        double viewY = (0 - offsetY) / zoomLevel;
        double viewW = getWidth() / zoomLevel;
        double viewH = getHeight() / zoomLevel;
        
        int vx = mx + 10 + (int)((viewX - minX) * scale);
        int vy = my + 10 + (int)((viewY - minY) * scale);
        int vw = (int)(viewW * scale);
        int vh = (int)(viewH * scale);
        g2.fillRect(vx, vy, vw, vh);
        g2.setColor(new Color(108, 99, 255, 120));
        g2.drawRect(vx, vy, vw, vh);

        for (int i=0; i<locations.size(); i++) {
            Location l = locations.get(i);
            int lx = mx + 10 + (int)((l.x - minX) * scale);
            int ly = my + 10 + (int)((l.y - minY) * scale);
            g2.setColor(i == 0 ? COL_BASE : COL_NODE);
            g2.fillOval(lx-2, ly-2, 4, 4);
        }
        
        g2.setClip(oldClip);
        g2.setComposite(oldComp);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(Theme.grid);
        g2.setStroke(new BasicStroke(0.5f));
        int gap = 50;
        for (int x = 0; x < getWidth();  x += gap) g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += gap) g2.drawLine(0, y, getWidth(), y);
    }

    private void drawMST(Graphics2D g2) {
        float[] dash = {12f, 8f};
        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f));
        g2.setColor(COL_MST);
        for (Edge e : mstEdges) {
            g2.drawLine(e.u.x, e.u.y, e.v.x, e.v.y);
        }
    }

    private void drawMSTWeights(Graphics2D g2) {
        for (Edge e : mstEdges) {
            int mx = (e.u.x + e.v.x) / 2;
            int my = (e.u.y + e.v.y) / 2;
            
            g2.setColor(Theme.bg); 
            g2.fillOval(mx - 14, my - 14, 28, 28);
            g2.setColor(Theme.shadDark); g2.drawOval(mx-14, my-14, 28, 28);
            
            String dist = String.format("%.0f", e.weight);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            int tw = g2.getFontMetrics().stringWidth(dist);
            g2.setColor(Theme.text);
            g2.drawString(dist, mx - tw/2, my + 3);
        }
    }

    private void drawTSP(Graphics2D g2) {
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COL_TSP);
        int sz = tspRoute.size();
        for (int i = 0; i < sz; i++) {
            Location a = tspRoute.get(i);
            Location b = tspRoute.get((i + 1) % sz);
            int destR = (tspRoute.indexOf(b) == 0 || (locations.indexOf(b) == 0)) ? BASE_RADIUS : NODE_RADIUS;
            drawArrow(g2, a.x, a.y, b.x, b.y, destR);
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, int destRadius) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int edgeGap = destRadius - 2; 
        int tipX = (int)(x2 - edgeGap * Math.cos(angle));
        int tipY = (int)(y2 - edgeGap * Math.sin(angle));
        g2.drawLine(x1, y1, tipX, tipY);

        int arrowLen = 14;
        double spread = Math.toRadians(28);
        int ax1 = (int)(tipX - arrowLen * Math.cos(angle - spread));
        int ay1 = (int)(tipY - arrowLen * Math.sin(angle - spread));
        int ax2 = (int)(tipX - arrowLen * Math.cos(angle + spread));
        int ay2 = (int)(tipY - arrowLen * Math.sin(angle + spread));
        g2.fillPolygon(new int[]{tipX, ax1, ax2}, new int[]{tipY, ay1, ay2}, 3);
    }

    private void drawHull(Graphics2D g2) {
        int[] xs = new int[hullPoints.size()], ys = new int[hullPoints.size()];
        for (int i = 0; i < hullPoints.size(); i++) { xs[i] = hullPoints.get(i).x; ys[i] = hullPoints.get(i).y; }
        
        g2.setColor(new Color(COL_HULL.getRed(), COL_HULL.getGreen(), COL_HULL.getBlue(), 10));
        g2.fillPolygon(xs, ys, hullPoints.size());
        g2.setColor(COL_HULL);
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(xs, ys, hullPoints.size());
    }

    private java.util.List<int[]> gatherActiveSegments() {
        java.util.List<int[]> segs = new java.util.ArrayList<>();
        if (showTSP && tspRoute.size() > 1) {
            for (int i = 0; i < tspRoute.size(); i++) {
                Location a = tspRoute.get(i);
                Location b = tspRoute.get((i + 1) % tspRoute.size());
                segs.add(new int[]{a.x, a.y, b.x, b.y});
            }
        }
        if (showMST && !mstEdges.isEmpty()) {
            for (Edge e : mstEdges) segs.add(new int[]{e.u.x, e.u.y, e.v.x, e.v.y});
        }
        if (showHull && hullPoints.size() >= 3) {
            for (int i = 0; i < hullPoints.size(); i++) {
                Location a = hullPoints.get(i);
                Location b = hullPoints.get((i + 1) % hullPoints.size());
                segs.add(new int[]{a.x, a.y, b.x, b.y});
            }
        }
        return segs;
    }

    private double pointToSegDist(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double lenSq = dx*dx + dy*dy;
        if (lenSq < 1e-6) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1)*dx + (py - y1)*dy) / lenSq));
        double projX = x1 + t*dx, projY = y1 + t*dy;
        return Math.hypot(px - projX, py - projY);
    }

    private void drawLocations(Graphics2D g2) {
        java.util.List<int[]> segs = gatherActiveSegments();

        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            boolean isBase = (i == 0);
            int r = isBase ? BASE_RADIUS : NODE_RADIUS;
            Color accent = isBase ? COL_BASE : COL_NODE;

            g2.setColor(Theme.shadLight); g2.fillOval(loc.x - r - 4, loc.y - r - 4, r*2, r*2);
            g2.setColor(Theme.shadDark);  g2.fillOval(loc.x - r + 4, loc.y - r + 4, r*2, r*2);

            g2.setColor(Theme.bg);
            g2.fillOval(loc.x - r, loc.y - r, r*2, r*2);

            g2.setColor(Theme.shadDark); g2.drawOval(loc.x - r + 2, loc.y - r + 2, r*2 - 4, r*2 - 4);
            g2.setColor(Theme.shadLight); g2.drawOval(loc.x - r + 0, loc.y - r + 0, r*2 - 4, r*2 - 4);
            
            g2.setColor(accent);
            g2.fillOval(loc.x - r + 6, loc.y - r + 6, r*2 - 12, r*2 - 12);

            g2.setColor(Theme.text);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String label = isBase ? "H" : "L" + loc.id;
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(label);
            int lh = fm.getAscent();
            int gap = r + 8;

            double[][] offsets = {
                { 0, -1}, { 0.71, -0.71}, { 1,  0}, { 0.71,  0.71}, 
                { 0,  1}, {-0.71,  0.71}, {-1,  0}, {-0.71, -0.71} 
            };

            double bestScore = -1;
            int bestLX = loc.x - lw / 2, bestLY = loc.y - r - 10;

            for (double[] off : offsets) {
                double cx = loc.x + off[0] * gap;
                double cy = loc.y + off[1] * gap;
                
                double minDist = Double.MAX_VALUE;
                for (int[] seg : segs) {
                    double d = pointToSegDist(cx, cy, seg[0], seg[1], seg[2], seg[3]);
                    if (d < minDist) minDist = d;
                }
                if (segs.isEmpty()) minDist = 100;

                double boundsPenalty = 0;
                if (cx - lw/2 < 10 || cx + lw/2 > getWidth() - 10) boundsPenalty += 500;
                if (cy - lh < 10    || cy + lh/2 > getHeight() - 10) boundsPenalty += 500;

                double score = minDist - boundsPenalty;
                if (score > bestScore) {
                    bestScore = score;
                    bestLX = (int)(cx - lw / 2.0);
                    bestLY = (int)(cy + lh / 2.0 - 2);
                }
            }

            int pad = 3;
            g2.setColor(Theme.tooltipBg);
            g2.fillRoundRect(bestLX - pad, bestLY - lh + 1, lw + pad*2, lh + pad, 6, 6);

            g2.setColor(Theme.text);
            g2.drawString(label, bestLX, bestLY);
        }
    }

    private void drawWelcomeOverlay(Graphics2D g2) {
        g2.setColor(Theme.overlayBg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int cw = Math.min(800, getWidth() - 60);
        
        int ch = 350; 
        if (cw < 600) ch += 60; 
        if (showDetails) {
            ch += 110; 
            if (expandHull) ch += 60;
            if (expandTSP) ch += 75;
            if (expandMST) ch += 75;
        }
        if (cw < 450) ch += 80; 

        maxScrollY = Math.max(0, ch - (getHeight() - 60));
        if (scrollY > maxScrollY) scrollY = maxScrollY;
        
        int cx = (getWidth() - cw) / 2;
        int cy = Math.max(30, (getHeight() - ch) / 2) - scrollY;
        int radius = 30;

        for (int i=0; i<3; i++) {
            g2.setColor(Theme.shadLight); 
            g2.fillRoundRect(cx - i - 2, cy - i - 2, cw, ch, radius, radius);
            g2.setColor(Theme.shadDark);
            g2.fillRoundRect(cx + i + 2, cy + i + 2, cw, ch, radius, radius);
        }
        g2.setColor(Theme.bg);
        g2.fillRoundRect(cx, cy, cw, ch, radius, radius);

        g2.setColor(Theme.text);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        String title = "Smart Drone Routing & Geofencing System";
        g2.drawString(title, cx + (cw - g2.getFontMetrics().stringWidth(title))/2, cy + 45);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(Theme.textMuted);
        String text = "This system is a real-time visualization of advanced drone routing strategies. " +
                      "It simulates the logistical challenges of deploying an autonomous drone fleet to deliver payloads across multiple coordinates. The platform implements core Design and Analysis of Algorithms (DAA) concepts to solve NP-Hard supply chain optimization problems dynamically in O(n log n) and O(n!) complexity bounds.\n\n" +
                      "Operational Workflow:\n" +
                      "1. Click anywhere on the map grid to establish the central Base Depot.\n" +
                      "2. Click additional points to drop Delivery Targets across the topology.\n" +
                      "3. Use the Mission Control panel to execute geospatial computations.";

        int margin = 40;
        int textY = cy + 80;
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) { textY += 12; continue; }
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (g2.getFontMetrics().stringWidth(line.toString() + w) < cw - 2*margin) {
                    line.append(w).append(" ");
                } else {
                    g2.drawString(line.toString(), cx + margin, textY);
                    textY += g2.getFontMetrics().getHeight() + 4;
                    line = new StringBuilder(w).append(" ");
                }
            }
            if(line.length() > 0) g2.drawString(line.toString(), cx + margin, textY);
            textY += g2.getFontMetrics().getHeight() + 4;
        }

        textY += 15;
        
        String btnText = showDetails ? "▲ Hide Technical Details" : "▼ Explore Technical Details";
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        int btnW = 240, btnH = 36;
        int btnX = cx + (cw - btnW) / 2;
        int btnY = textY;
        
        g2.setColor(Theme.shadLight);
        g2.fillRoundRect(btnX, btnY, btnW, btnH, 18, 18);
        g2.setColor(new Color(108, 99, 255));
        g2.drawString(btnText, btnX + (btnW - g2.getFontMetrics().stringWidth(btnText))/2, btnY + 23);
        rectDetails = new Rectangle(btnX, btnY, btnW, btnH);
        
        textY += 70; 

        if (showDetails) {
            int headerY = textY;
            textY = drawAccordion(g2, "1. Graham Scan - O(n log n) Geofencing", 
                "Establishes a secure operational Geofence. It identifies the outermost boundary points to construct a minimal convex polygon that contains all delivery targets. This guarantees the drones remain within restricted authorized airspace during their entire mission.", 
                expandHull, cx + margin, textY, cw - 2*margin);
            rectHull = new Rectangle(cx + margin, headerY - 14, cw - 2*margin, 24);
            
            headerY = textY;
            textY = drawAccordion(g2, "2. Traveling Salesman Problem - O(n!) Optimal Route", 
                "Calculates the absolute shortest Hamiltonian cycle returning to the Base Depot. Due to the mathematically intense complexity of the TSP, we use an exhaustive Backtracking strategy with Branch-and-Bound pruning. This cuts off inefficient exploratory paths early, finding the globally optimal flight route to maximize battery conservation.", 
                expandTSP, cx + margin, textY, cw - 2*margin);
            rectTSP = new Rectangle(cx + margin, headerY - 14, cw - 2*margin, 24);
            
            headerY = textY;
            textY = drawAccordion(g2, "3. Kruskal's MST - O(E log E) Connect Hubs", 
                "In scenarios prioritizing continuous ground-to-drone sensor connectivity, Kruskal's Algorithm builds a Minimum Spanning Tree (MST). Using a Disjoint-Set (Union-Find) data structure, it greedily selects the shortest distances to interlink all nodes with the absolute minimum total transmission range.", 
                expandMST, cx + margin, textY, cw - 2*margin);
            rectMST = new Rectangle(cx + margin, headerY - 14, cw - 2*margin, 24);
        } else {
            rectHull = new Rectangle();
            rectTSP = new Rectangle();
            rectMST = new Rectangle();
        }

        int closeSize = 36;
        int closeBtnX = cx + cw - closeSize - 20;
        int closeBtnY = cy + 20;
        
        g2.setColor(Theme.shadLight); g2.fillRoundRect(closeBtnX-1, closeBtnY-1, closeSize, closeSize, 12, 12);
        g2.setColor(Theme.shadDark);  g2.fillRoundRect(closeBtnX+2, closeBtnY+2, closeSize, closeSize, 12, 12);
        g2.setColor(Theme.bg);
        g2.fillRoundRect(closeBtnX, closeBtnY, closeSize, closeSize, 12, 12);

        g2.setColor(Theme.textMuted);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(closeBtnX + 10, closeBtnY + 10, closeBtnX + 26, closeBtnY + 26);
        g2.drawLine(closeBtnX + 26, closeBtnY + 10, closeBtnX + 10, closeBtnY + 26);

        closeButtonRect = new Rectangle(closeBtnX, closeBtnY, closeSize, closeSize);
    }

    private int drawAccordion(Graphics2D g2, String title, String body, boolean expanded, int x, int y, int width) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(Theme.text);
        String prefix = expanded ? "▼ " : "▶ ";
        g2.drawString(prefix + title, x, y);
        int nextY = y + 20;

        if (expanded) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Theme.textMuted);
            String[] words = body.split(" ");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (g2.getFontMetrics().stringWidth(line.toString() + w) < width - 20) {
                    line.append(w).append(" ");
                } else {
                    g2.drawString(line.toString(), x + 20, nextY);
                    nextY += g2.getFontMetrics().getHeight() + 4;
                    line = new StringBuilder(w).append(" ");
                }
            }
            if(line.length() > 0) {
                g2.drawString(line.toString(), x + 20, nextY);
                nextY += g2.getFontMetrics().getHeight() + 4;
            }
            nextY += 10;
        } else {
            nextY += 5;
        }
        
        g2.setColor(Theme.border);
        g2.drawLine(x, nextY - 5, x + width, nextY - 5);
        return nextY + 15;
    }
    
    private void drawPopup(Graphics2D g2) {
        if (popupAlpha <= 0f) return;
        
        int pw = 360;
        int margin = 26;
        int radius = 32;
        int titlePad = 54;
        int bottomPad = 24;
        
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight() + 4;
        int maxTextW = pw - 2 * margin;
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = popupText.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (fm.stringWidth(cur.toString() + word) < maxTextW) {
                cur.append(word).append(" ");
            } else {
                lines.add(cur.toString());
                cur = new StringBuilder(word).append(" ");
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        
        int ph = titlePad + lines.size() * lineHeight + bottomPad;
        int px = getWidth()  - pw - 20; 
        int py = 20 + (int)((1f - popupAlpha) * -30); 
        
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, popupAlpha));
        
        for (int i = 0; i < 3; i++) {
            g2.setColor(Theme.shadLight); g2.fillRoundRect(px - i - 2, py - i - 2, pw, ph, radius, radius);
            g2.setColor(Theme.shadDark);  g2.fillRoundRect(px + i + 2, py + i + 2, pw, ph, radius, radius);
        }
        g2.setColor(Theme.bg);
        g2.fillRoundRect(px, py, pw, ph, radius, radius);
        
        drawAlgoVectorIcon(g2, px + 24, py + 22, lastAlgoType);

        g2.setColor(Theme.text);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString(popupTitle, px + 56, py + 36);
        
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(Theme.textMuted);
        int textY = py + titlePad + fm.getAscent();
        for (String ln : lines) {
            g2.drawString(ln, px + margin, textY);
            textY += lineHeight;
        }

        int cs = 22;
        int cx = px + pw - cs - 14;
        int cy_ = py + 14;
        g2.setColor(Theme.textMuted);
        g2.drawOval(cx, cy_, cs, cs);
        g2.drawLine(cx + 6, cy_ + 6, cx + cs - 6, cy_ + cs - 6);
        g2.drawLine(cx + cs - 6, cy_ + 6, cx + 6, cy_ + cs - 6);
        popupCloseRect = new Rectangle(cx, cy_, cs, cs);
        
        g2.setComposite(originalComposite);
    }

    private void drawAlgoVectorIcon(Graphics2D g2, int x, int y, AlgoType type) {
        if (type == null) return;
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (type == AlgoType.HULL) {
            g2.setColor(COL_HULL);
            g2.drawPolygon(new int[]{x+4, x+16, x+12, x+2}, new int[]{y+2, y+8, y+16, y+12}, 4);
            g2.setColor(COL_HULL.darker());
            g2.fillOval(x+2, y, 4, 4); g2.fillOval(x+14, y+6, 4, 4); g2.fillOval(x+10, y+14, 4, 4);
        } else if (type == AlgoType.TSP) {
            g2.setColor(COL_TSP);
            g2.drawArc(x, y, 16, 10, 0, 180); 
            g2.drawLine(x, y+5, x+8, y+14); 
            g2.drawLine(x+8, y+14, x+16, y+5);
        } else if (type == AlgoType.MST) {
            g2.setColor(COL_MST);
            g2.drawLine(x+8, y+2, x+2, y+14);
            g2.drawLine(x+8, y+2, x+16, y+10);
            g2.setColor(COL_MST.darker());
            g2.fillOval(x+6, y, 5, 5);
            g2.fillOval(x, y+12, 5, 5);
            g2.fillOval(x+14, y+8, 5, 5);
        }
    }

    @Override public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        
        if (popupAlpha > 0.5f && popupCloseRect.contains(p)) {
            popupAlpha = 0f;
            repaint();
            return;
        }

        if (showWelcomeOverlay) {
            if (closeButtonRect.contains(p)) {
                showWelcomeOverlay = false;
                repaint();
            } else if (rectDetails.contains(p)) {
                showDetails = !showDetails;
                repaint();
            } else if (rectHull.contains(p)) {
                expandHull = !expandHull;
                repaint();
            } else if (rectTSP.contains(p)) {
                expandTSP = !expandTSP;
                repaint();
            } else if (rectMST.contains(p)) {
                expandMST = !expandMST;
                repaint();
            }
            return;
        }

        if (rectSearch.contains(p)) {
            showSearchUI = !showSearchUI;
            if (showSearchUI) requestFocusInWindow();
            repaint();
            return;
        }
        
        if (showSearchUI) {
            if (rectSearchX.contains(p)) { searchFocus = 0; repaint(); return; }
            if (rectSearchY.contains(p)) { searchFocus = 1; repaint(); return; }
            if (rectSearchGo.contains(p)) { executeSearch(); return; }
        }

        if (showMissionSelector) {
            if (rectSelTSP.contains(p)) { executeFlyMission("TSP"); return; }
            if (rectSelMST.contains(p)) { executeFlyMission("MST"); return; }
            if (rectSelHull.contains(p)) { executeFlyMission("HULL"); return; }
            showMissionSelector = false; repaint(); return; 
        }

        if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
            int cx = (int)((e.getX() - offsetX) / zoomLevel);
            int cy = (int)((e.getY() - offsetY) / zoomLevel);

            if (e.getY() < 35) {
                postStatus("⚠️ Restricted Airspace: Too close to Mission Control panel.");
                return;
            }

            for (Location loc : locations) {
                if (Math.hypot(loc.x - cx, loc.y - cy) < BASE_RADIUS * 2) {
                    postStatus("⚠️ Cannot place point: Too close to existing target " + (loc.id == 0 ? "H" : "L" + loc.id));
                    return;
                }
            }

            int id = locations.size();
            locations.add(new Location(id, cx, cy));
            String msg = id == 0
                ? "Base Station locked. Click map to define supply points."
                : "Target L" + id + " designated  (" + cx + ", " + cy + ").";
            postStatus(msg);
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e) && !locations.isEmpty()) {
            double wx = (double)(e.getX() - offsetX) / zoomLevel;
            double wy = (double)(e.getY() - offsetY) / zoomLevel;
            
            int hitIndex = -1;
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                double d = Math.hypot(loc.x - wx, loc.y - wy);
                if (d < (loc.id == 0 ? 30 : 20) / zoomLevel + 10) {
                    hitIndex = i;
                    break;
                }
            }

            if (hitIndex != -1) {
                Location target = locations.get(hitIndex);
                if (target.id == 0) {
                    locations.clear();
                    hullPoints.clear(); tspRoute.clear(); mstEdges.clear();
                    showHull = showTSP = showMST = false;
                    animatingDrone = false;
                    if (droneTimer != null) droneTimer.stop();
                    // Ensure sidebar hides on Hub deletion reset
                    if (sidebar != null) {
                        sidebar.clear();
                        sidebar.setVisible(false);
                    }
                    postStatus("⚠️ Hub Deletion: Neural Network Reset. All mission data purged.");
                } else {
                    locations.remove(hitIndex);
                    for (int i = 0; i < locations.size(); i++) {
                        locations.get(i).id = i;
                    }
                    hullPoints.clear(); tspRoute.clear(); mstEdges.clear();
                    showHull = showTSP = showMST = false;
                    postStatus("Target L" + target.id + " neutralized. Network re-indexed.");
                }
                popupAlpha = 0f;
                repaint();
            } else {
                locations.remove(locations.size() - 1);
                hullPoints.clear(); tspRoute.clear(); mstEdges.clear();
                showHull = showTSP = showMST = false;
                postStatus("Previous target withdrawn. Trajectories cleared.");
                repaint();
            }
        }
    }

    @Override 
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        boolean overUI = false;
        
        if (showWelcomeOverlay) {
            if (closeButtonRect.contains(p) || rectDetails.contains(p) || rectHull.contains(p) || rectTSP.contains(p) || rectMST.contains(p)) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                overUI = true;
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else if (popupAlpha > 0.5f && popupCloseRect.contains(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            overUI = true;
        } else if (rectSearch.contains(p) || (showSearchUI && rectSearchGo.contains(p)) || (showMissionSelector && (rectSelTSP.contains(p) || rectSelMST.contains(p) || rectSelHull.contains(p)))) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            overUI = true;
        } else if (showSearchUI && (rectSearchX.contains(p) || rectSearchY.contains(p))) {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            overUI = true;
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        if (!showWelcomeOverlay && !overUI) {
            double wx = (e.getX() - offsetX) / zoomLevel;
            double wy = (e.getY() - offsetY) / zoomLevel;
            
            Location found = null;
            for (Location loc : locations) {
                double dist = Math.hypot(loc.x - wx, loc.y - wy);
                if (dist < (loc.id == 0 ? BASE_RADIUS : NODE_RADIUS) / zoomLevel + 5) {
                    found = loc;
                    break;
                }
            }
            
            if (found != hoveredLocation) {
                hoveredLocation = found;
                repaint();
            }
        } else if (hoveredLocation != null) {
            hoveredLocation = null;
            repaint();
        }
    }

    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        if (showWelcomeOverlay) {
            scrollY += e.getWheelRotation() * 30;
            if (scrollY < 0) scrollY = 0;
            if (scrollY > maxScrollY) scrollY = maxScrollY;
            repaint();
            return;
        }
        
        wakeMinimap();
        double oldZoom = zoomLevel;
        double factor = Math.pow(1.15, -e.getPreciseWheelRotation());
        zoomLevel *= factor;
        zoomLevel = Math.max(0.1, Math.min(zoomLevel, 10.0));
        
        double pX = e.getX();
        double pY = e.getY();
        offsetX = pX - (pX - offsetX) * (zoomLevel / oldZoom);
        offsetY = pY - (pY - offsetY) * (zoomLevel / oldZoom);
        
        repaint();
    }

    @Override public void mousePressed(MouseEvent e) {
        lastMousePos = e.getPoint();
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (showWelcomeOverlay) return;
        
        if (SwingUtilities.isLeftMouseButton(e)) {
            wakeMinimap();
            Point p = e.getPoint();
            offsetX += (p.x - lastMousePos.x);
            offsetY += (p.y - lastMousePos.y);
            lastMousePos = p;
            repaint();
        }
    }
    
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {
        hoveredLocation = null;
        repaint();
    }
    
    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_Q) {
            showSearchUI = false;
            showMissionSelector = false;
            repaint();
            return;
        }
        if (!showSearchUI) return;
        if (key == KeyEvent.VK_ENTER) {
            executeSearch();
        } else if (key == KeyEvent.VK_TAB) {
            searchFocus = (searchFocus + 1) % 2;
            repaint();
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            if (searchFocus == 0 && searchX.length() > 0) searchX = searchX.substring(0, searchX.length()-1);
            if (searchFocus == 1 && searchY.length() > 0) searchY = searchY.substring(0, searchY.length()-1);
            repaint();
        } else {
            char c = e.getKeyChar();
            if (Character.isDigit(c)) {
                if (searchFocus == 0) searchX += c;
                else searchY += c;
                repaint();
            }
        }
    }
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    private void postStatus(String msg) {
        if (statusCallback != null) statusCallback.update(msg);
    }

    private void drawDrone(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform old = g2.getTransform();
        
        g2.translate(droneX, droneY);
        g2.rotate(droneAngle);
        
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.text);
        g2.drawLine(-12, -12, 12, 12);
        g2.drawLine(12, -12, -12, 12);
        
        g2.setColor(new Color(255, 180, 0));
        int r = 8;
        g2.drawOval(-12-r/2, -12-r/2, r, r);
        g2.drawOval(12-r/2, -12-r/2, r, r);
        g2.drawOval(-12-r/2, 12-r/2, r, r);
        g2.drawOval(12-r/2, 12-r/2, r, r);
        
        g2.setColor(new Color(108, 99, 255));
        g2.fillOval(-6, -6, 12, 12);
        g2.setColor(Color.WHITE);
        g2.fillOval(2, -2, 4, 4); 
        
        g2.setTransform(old);
    }

    private void drawHoverTooltip(Graphics2D g2) {
        String coordText = String.format("%dx, %dy", hoveredLocation.x, hoveredLocation.y);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(coordText);
        int th = fm.getHeight();
        
        int sx = (int)(hoveredLocation.x * zoomLevel + offsetX);
        int sy = (int)(hoveredLocation.y * zoomLevel + offsetY);
        
        int tx = sx - tw / 2;
        int ty = sy - (hoveredLocation.id == 0 ? BASE_RADIUS : NODE_RADIUS) - 25;
        
        int pad = 8;
        g2.setColor(Theme.tooltipBg);
        g2.fillRoundRect(tx - pad, ty - th, tw + pad * 2, th + pad, 10, 10);
        
        int[] px = {sx - 5, sx + 5, sx};
        int[] py = {ty + pad/2, ty + pad/2, ty + 12};
        g2.fillPolygon(px, py, 3);
        
        g2.setColor(Theme.text);
        g2.drawString(coordText, tx, ty);
    }

    private void drawSearchIcon(Graphics2D g2) {
        int x = 20, y = 20, size = 32;
        rectSearch = new Rectangle(x, y, size, size);
        
        g2.setColor(new Color(108, 99, 255));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval(x + 8, y + 8, 12, 12);
        g2.drawLine(x + 18, y + 18, x + 24, y + 24);
    }

    private void drawSearchInputBox(Graphics2D g2) {
        int x = 20, y = 62, w = 180, h = 34; 
        int radius = 12;
        
        g2.setColor(Theme.shadDark); g2.fillRoundRect(x+2, y+2, w, h, radius, radius);
        
        g2.setColor(Theme.bg);
        g2.fillRoundRect(x, y, w, h, radius, radius);
        g2.setColor(new Color(108, 99, 255, 60)); 
        g2.drawRoundRect(x, y, w, h, radius, radius);
        
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(108, 99, 255));
        
        rectSearchX = new Rectangle(x + 25, y + 6, 40, 22);
        g2.drawString("X:", x + 10, y + 22);
        g2.setColor(searchFocus == 0 ? new Color(108, 99, 255, 30) : Theme.shadDark);
        g2.fillRoundRect(rectSearchX.x, rectSearchX.y, rectSearchX.width, rectSearchX.height, 6, 6);
        g2.setColor(Theme.text);
        g2.drawString(searchX + (searchFocus == 0 ? "|" : ""), rectSearchX.x + 5, y + 22);
        
        g2.setColor(new Color(108, 99, 255));
        rectSearchY = new Rectangle(x + 85, y + 6, 40, 22);
        g2.drawString("Y:", x + 70, y + 22);
        g2.setColor(searchFocus == 1 ? new Color(108, 99, 255, 30) : Theme.shadDark);
        g2.fillRoundRect(rectSearchY.x, rectSearchY.y, rectSearchY.width, rectSearchY.height, 6, 6);
        g2.setColor(Theme.text);
        g2.drawString(searchY + (searchFocus == 1 ? "|" : ""), rectSearchY.x + 5, y + 22);
        
        rectSearchGo = new Rectangle(x + 135, y + 5, 40, 24);
        g2.setColor(new Color(108, 99, 255));
        g2.fillRoundRect(rectSearchGo.x, rectSearchGo.y, rectSearchGo.width, rectSearchGo.height, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("GO", rectSearchGo.x + 10, y + 22);
    }

    private void executeSearch() {
        if (searchX.isEmpty() || searchY.isEmpty()) {
            postStatus("⚠️ Enter both X and Y coordinates.");
            return;
        }
        try {
            int targetX = Integer.parseInt(searchX);
            int targetY = Integer.parseInt(searchY);
            offsetX = getWidth() / 2.0 - (targetX * zoomLevel);
            offsetY = getHeight() / 2.0 - (targetY * zoomLevel);
            showSearchUI = false;
            searchX = ""; searchY = ""; 
            postStatus("Navigated to (" + targetX + ", " + targetY + ")");
            wakeMinimap();
            repaint();
        } catch (NumberFormatException ex) {
            postStatus("⚠️ Invalid coordinates.");
        }
    }

    private void drawMissionSelector(Graphics2D g2) {
        if (missionAlpha <= 0f) return;
        
        Composite oldComp = g2.getComposite();
        float dimIntensity = Theme.isDark ? 0.75f : 0.45f;
        float safeDimAlpha = Math.max(0f, Math.min(1f, missionAlpha * dimIntensity));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeDimAlpha));
        g2.setColor(Theme.isDark ? Color.BLACK : new Color(15, 23, 42)); 
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        float safeBtnAlpha = Math.max(0f, Math.min(1f, missionAlpha));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeBtnAlpha));
        int slideY = (int)((1.0f - missionAlpha) * 20); 
        
        int btnW = 140;
        int btnH = 30; 
        int gap = 20;
        int totalW = (btnW * 3) + (gap * 2);
        int sx = (getWidth() - totalW) / 2;
        int sy = (getHeight() - btnH) / 2 + slideY;

        rectSelHull = new Rectangle(sx, sy, btnW, btnH);
        drawSelOption(g2, rectSelHull, "Geofence Scan", COL_HULL, showHull);
        
        rectSelTSP = new Rectangle(sx + btnW + gap, sy, btnW, btnH);
        drawSelOption(g2, rectSelTSP, "Optimal Path", COL_TSP, showTSP);
        
        rectSelMST = new Rectangle(sx + (btnW + gap) * 2, sy, btnW, btnH);
        drawSelOption(g2, rectSelMST, "Backbone Connect", COL_MST, showMST);
        
        g2.setComposite(oldComp);
    }

    private void drawSelOption(Graphics2D g2, Rectangle r, String text, Color col, boolean active) {
        int x = r.x, y = r.y, w = r.width, h = r.height;
        int radius = h; 
        
        if (!active) {
            g2.setColor(Theme.bg);
            g2.fillRoundRect(x, y, w, h, radius, radius);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(Theme.textMuted);
            g2.drawString(text, x + (w - g2.getFontMetrics().stringWidth(text))/2, y + 20);
            return;
        }
        
        for (int i=0; i<3; i++) {
            g2.setColor(Theme.shadLight);
            g2.fillRoundRect(x - i - 2, y - i - 2, w, h, radius, radius);
        }
        for (int i=0; i<4; i++) {
            g2.setColor(Theme.shadDark);
            g2.fillRoundRect(x + i + 1, y + i + 1, w, h, radius, radius);
        }
        
        g2.setColor(col);
        g2.fillRoundRect(x, y, w, h, radius, radius);
        
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + (w - g2.getFontMetrics().stringWidth(text))/2, y + 20);
    }

    private void updateSidebarForMission(String type) {
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.getParent().revalidate();
        }
        if (type.equals("TSP")) {
            sidebar.startMission("Optimal Delivery Mission", 
                "Executing real-time TSP route optimization using Exhaustive Backtracking. Drone is visiting 10 nodes for maximum efficiency.");
            sidebar.addTask("Skeleton Coverage", "Mapping the absolute minimal Hamiltonian cycle.");
            sidebar.addTask("Optimal Linkage", "Prioritizing node sequence to minimize battery drain.");
            sidebar.addTask("Home Return", "Closing the loop back to Base Depot (H).");
            sidebar.addCodeBlock("void flyTSP() {\n  for(Location p : tspRoute) {\n    moveTo(p);\n    logEnergy();\n  }\n}");
        } else if (type.equals("MST")) {
            sidebar.startMission("Network Backbone Patrol",
                "Navigating the Minimum Spanning Tree backbone. Ensuring connectivity across all Base Depot links using Kruskal's verified edges.");
            sidebar.addTask("Skeleton Coverage", "Performing a DFS (Depth-First) patrol along communication hubs.");
            sidebar.addTask("Shortest Connectivity", "Intentional travel along paths that preserve network integrity.");
            sidebar.addTask("Home Return", "Calculating direct re-docking route after last leaf node.");
            sidebar.addCodeBlock("void patrolMST() {\n  dfs(hub, edges);\n  checkSignals();\n}");
        } else if (type.equals("HULL")) {
            sidebar.startMission("Geofence Perimeter Sweep",
                "Drone performing a boundary validation mission along the calculated Convex Hull. Ensuring zone integrity across 1319.7px.");
            sidebar.addTask("Boundary Scan", "Actively scanning the outermost convex corner points.");
            sidebar.addTask("Enclosure Check", "Validating the exclusion zone perimeter.");
            sidebar.addTask("Home Return", "Returning to HQ after completing the geofence lap.");
            sidebar.addCodeBlock("void sweepHull() {\n  for(Location b : hullPoints) {\n    scanBoundary(b);\n  }\n}");
        }
    }
}