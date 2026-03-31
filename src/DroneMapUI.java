import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class DroneMapUI extends JFrame {

    private final DroneMapPanel    mapPanel = new DroneMapPanel();
    private final AlgorithmManager algMgr   = new AlgorithmManager();
    private final AlgorithmSidebar sidebar  = new AlgorithmSidebar();
    private final JLabel           statusBar;
    private final RadiatingDotIcon pulsingDot;

    private JLabel brandName;
    private JLabel legendTitle;
    private JLabel[] legendLabels = new JLabel[3];
    private JPanel navbar;
    private JPanel legendBar;

    public DroneMapUI() {
        super("Smart Drone Routing & Geofencing");
        
        try {
            Icon appIcon = new DroneLogoIcon();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(appIcon.getIconWidth(), appIcon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2Img = img.createGraphics();
            appIcon.paintIcon(null, g2Img, 0, 0);
            g2Img.dispose();
            setIconImage(img);
            try { java.awt.Taskbar.getTaskbar().setIconImage(img); } catch (Exception ignore) {}
        } catch (Exception ignore) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.bg);
        
        getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.border));

        statusBar = new JLabel("  Ready — Drop Base Station. (Scroll to Zoom, Shift+Drag to Pan)");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusBar.setForeground(Theme.textMuted);
        statusBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        statusBar.setBackground(Theme.bg);
        statusBar.setOpaque(true);
        add(statusBar, BorderLayout.SOUTH);

        pulsingDot = new RadiatingDotIcon(statusBar);

        mapPanel.setStatusCallback(msg -> SwingUtilities.invokeLater(() -> {
            boolean isAirborne = msg.contains("Drone airborne");
            if (isAirborne) {
                pulsingDot.setActive(true);
                statusBar.setIcon(pulsingDot); // Radiant Yellow Dot
                statusBar.setText("  " + msg);
                
                String telemetry = String.format(
                    "while (missionActive) {\n" +
                    "  drone.updatePosition(%.1f, %.1f);\n" +
                    "  drone.calculateAngle(%.2f°);\n" +
                    "  if (drone.checkArrival(target)) {\n" +
                    "     currentLeg++;\n" +
                    "     log(\"Node target reached\");\n" +
                    "  }\n" +
                    "  status = \"NAVIGATING_ACTIVE\";\n" +
                    "}", mapPanel.getDroneX(), mapPanel.getDroneY(), Math.toDegrees(mapPanel.getDroneAngle()));
                
                sidebar.updateForAlgo("Drone Mission Telemetry", msg, telemetry);
                if (!sidebar.isVisible()) {
                    sidebar.setVisible(true);
                    revalidate();
                }
            } else if (msg.contains("Executing Leg") || msg.contains("Navigating: Segment") || msg.contains("Final Approach")) {
                pulsingDot.setActive(true);
                statusBar.setIcon(pulsingDot); // Continuous Radiant Yellow
                statusBar.setText("  " + msg);
                sidebar.addTask("Current Flight Operation", msg);
            } else if (msg.contains("Mission complete") || msg.contains("computed") || msg.contains("established")) {
                pulsingDot.setActive(false);
                statusBar.setIcon(pulsingDot); // Radiant Success Dot
                statusBar.setText("  System: " + msg.replace("System: ", ""));
                sidebar.addTask("Mission Objective", "Successfully neutralized all tasks.");
            } else {
                statusBar.setIcon(null); 
                statusBar.setText("  System: " + msg);
            }
        }));

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        
        navbar = buildNavbar();
        legendBar = buildLegendBar();
        
        topContainer.add(navbar);
        topContainer.add(legendBar);

        add(topContainer, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        
        mapPanel.setSidebar(sidebar);
        sidebar.setVisible(false); // Hide by default
        sidebar.setOnClose(() -> {
            sidebar.setVisible(false);
            revalidate();
            repaint();
        });
        add(sidebar, BorderLayout.EAST);

        // --- Theme Switcher Logic ---
        Theme.addListener(() -> {
            getContentPane().setBackground(Theme.bg);
            statusBar.setBackground(Theme.bg);
            statusBar.setForeground(Theme.textMuted);
            getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.border));
            if (navbar != null) navbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border));
            if (legendBar != null) legendBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border));
            if (brandName != null) brandName.setForeground(Theme.textMuted);
            if (legendTitle != null) legendTitle.setForeground(Theme.textMuted);
            for (JLabel l : legendLabels) if (l != null) l.setForeground(Theme.text);
            sidebar.updateThemeUI();
            repaint();
        });

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 720));
        setVisible(true);
    }

    private JPanel buildNavbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border));

        // --- Brand Row (NORTH) ---
        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        brandRow.setOpaque(false);
        
        JLabel brandLogo = new JLabel(new DroneLogoIcon());
        brandName = new JLabel("SMART DRONE ROUTING");
        brandName.setFont(new Font("SansSerif", Font.BOLD, 14));
        brandName.setForeground(Theme.textMuted);
        brandName.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        brandRow.add(brandLogo);
        brandRow.add(brandName);
        bar.add(brandRow, BorderLayout.NORTH);

        // --- Action Row (CENTER) ---
        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);
        actionRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        leftPanel.setOpaque(false);

        NeumorphicButton btnHull = new NeumorphicButton("Draw Geofence", new Color(108, 99, 255), Color.WHITE);
        btnHull.addActionListener(e -> runConvexHull());
        leftPanel.add(btnHull);

        NeumorphicButton btnTSP = new NeumorphicButton("Optimal Route", new Color(255, 120, 100), Color.WHITE);
        btnTSP.addActionListener(e -> runTSP());
        leftPanel.add(btnTSP);

        NeumorphicButton btnMST = new NeumorphicButton("Connect Hubs", new Color(56, 178, 172), Color.WHITE);
        btnMST.addActionListener(e -> runMST());
        leftPanel.add(btnMST);

        NeumorphicButton btnFly = new NeumorphicButton("Fly Mission", new Color(255, 180, 0), new Color(61, 72, 82));
        btnFly.addActionListener(e -> mapPanel.startDroneAnimation());
        leftPanel.add(btnFly);

        leftPanel.add(new JLabel(" "));
        leftPanel.add(new NeumorphicSeparator());
        leftPanel.add(new JLabel(" "));

        // Soft Slate Gray for clearing lines
        NeumorphicButton btnClearResults = new NeumorphicButton("Clear Overlays", new Color(148, 163, 184), Color.WHITE);
        btnClearResults.addActionListener(e -> {
            mapPanel.clearHull();
            mapPanel.clearTSP();
            mapPanel.clearMST();
            sidebar.clear();
            sidebar.setVisible(false);
            revalidate();
            repaint();
        });
        leftPanel.add(btnClearResults);

        // Soft Crimson Red for deleting all coordinates
        NeumorphicButton btnClearMap = new NeumorphicButton("Reset Map", new Color(239, 68, 68), Color.WHITE);
        btnClearMap.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this,
                "Erase all coordinates?", "Wipe Map", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                mapPanel.clearAll();
                sidebar.clear();
                sidebar.setVisible(false);
                revalidate();
                repaint();
            }
        });
        leftPanel.add(btnClearMap);

        actionRow.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightPanel.setOpaque(false);

        // --- Now using the custom Vector Icon for the theme toggle ---
        NeumorphicCircleButton btnTheme = new NeumorphicCircleButton(new ThemeToggleIcon());
        btnTheme.setToolTipText("Toggle Dark Mode");
        btnTheme.addActionListener(e -> Theme.toggle());
        rightPanel.add(btnTheme);

        // Circular 'i' Info Button
        NeumorphicCircleButton btnInfo = new NeumorphicCircleButton("i");
        btnInfo.setToolTipText("System Info");
        btnInfo.addActionListener(e -> mapPanel.showIntroOverlay());
        rightPanel.add(btnInfo);
        
        actionRow.add(rightPanel, BorderLayout.EAST);
        bar.add(actionRow, BorderLayout.CENTER);

        return bar;
    }

    private JPanel buildLegendBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border));

        legendTitle = new JLabel("Map Legend:");
        legendTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        legendTitle.setForeground(Theme.textMuted);
        bar.add(legendTitle);

        bar.add(createLegendItem(new Color(56, 178, 172), "Base Depot Hub (H)", 0));
        bar.add(createLegendItem(new Color(108, 99, 255), "Delivery Target Point (L)", 1));
        bar.add(createLegendItem(new Color(255, 120, 100), "TSP Drone Route", 2));

        return bar;
    }

    private JPanel createLegendItem(Color c, String text, int index) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        
        JPanel colorCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(0, 2, 12, 12);
            }
        };
        colorCircle.setPreferredSize(new Dimension(14, 16));
        colorCircle.setOpaque(false);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(Theme.text);
        legendLabels[index] = lbl;

        p.add(colorCircle);
        p.add(lbl);
        return p;
    }

    private void runConvexHull() {
        List<Location> locs = mapPanel.getLocations();
        int n = locs.size();
        if (n < 3) {
            JOptionPane.showMessageDialog(this, "Place at least 3 points first.");
            return;
        }
        
        statusBar.setIcon(null);
        // Sidebar Update with Task Cards + Code
        String description = "Computes the minimal convex airspace perimeter for safety containment.";
        String code = "// 1. Find the lowest pivot point\n" +
                      "Location pivot = points.get(0);\n" +
                      "for (Location p : points) {\n" +
                      "  if (p.y > pivot.y || (p.y == pivot.y && p.x < pivot.x))\n" +
                      "      pivot = p;\n" +
                      "}\n\n" +
                      "// 2. Sort remaining by polar angle vs anchor\n" +
                      "sorted.sort((a, b) -> {\n" +
                      "  double angleA = Math.atan2(a.y - anchor.y, a.x - anchor.x);\n" +
                      "  double angleB = Math.atan2(b.y - anchor.y, b.x - anchor.x);\n" +
                      "  return Double.compare(angleA, angleB);\n" +
                      "});\n\n" +
                      "// 3. Graham Scan using Stack\n" +
                      "for (Location p : sorted) {\n" +
                      "  while (stack.size() >= 2) {\n" +
                      "    Location top = stack.peek();\n" +
                      "    Location second = getSecond(stack);\n" +
                      "    // Prune inner points (cross product < 0)\n" +
                      "    if (cross(second, top, p) < 0) stack.pop();\n" +
                      "    else break;\n" +
                      "  }\n" +
                      "  stack.push(p);\n" +
                      "}";
        
        sidebar.startMission("Geofencing Analysis", description);
        sidebar.addTask("Analyzing Space", "Scanning " + n + " coordinate vectors...");
        sidebar.addTask("Sorting Workspace", "Ordering targets by polar displacement.");
        sidebar.addTask("Constructing Perimeter", "Pruning internal nodes from geometry.");
        sidebar.addCodeBlock(code);
        
        List<Location> hull = algMgr.convexHull(new java.util.ArrayList<>(locs));
        mapPanel.setHullPoints(hull);

        sidebar.setVisible(true);
        revalidate();

        statusBar.setIcon(pulsingDot);
        statusBar.setText("  Geofence established — " + hull.size() + " vertices found.");
        
        mapPanel.showExplanation(
            DroneMapPanel.AlgoType.HULL,
            "Geofence Established",
            "The Graham Scan Algorithm was utilized to formulate the outermost containment boundary governing the " + n + " deployed points."
        );
    }

    private void runTSP() {
        List<Location> locs = mapPanel.getLocations();
        int n = locs.size();
        if (n < 2) return;
        
        statusBar.setIcon(null);
        statusBar.setText("  System: Calculating TSP Backtracking...");
        SwingWorker<List<Location>, Void> worker = new SwingWorker<>() {
            @Override protected List<Location> doInBackground() {
                return algMgr.tspBacktracking(new java.util.ArrayList<>(locs));
            }
            @Override protected void done() {
                try {
                    List<Location> route = get();
                    mapPanel.setTSPRoute(route);
                    
                    // Sidebar Update with Task Cards + Code
                    String description = "Globally minimizes energy consumption using exhaustive search.";
                    String code = "// Exhaustive Backtracking with Pruning\n" +
                                  "private void tspHelper(..., double currentDist, int n) {\n" +
                                  "  // Base Case: Full path constructed\n" +
                                  "  if (path.size() == n) {\n" +
                                  "    double total = currentDist + getDistToStart();\n" +
                                  "    if (total < bestDist[0]) {\n" +
                                  "      bestDist[0] = total;\n" +
                                  "      best.clear(); best.addAll(path);\n" +
                                  "    }\n" +
                                  "    return;\n" +
                                  "  }\n\n" +
                                  "  // Branch & Bound Exploration\n" +
                                  "  for (int i = 0; i < n; i++) {\n" +
                                  "    if (!visited[i]) {\n" +
                                  "      double added = getDistToNext(i);\n" +
                                  "      // Prune sub-optimal routes early\n" +
                                  "      if (currentDist + added >= bestDist[0]) continue;\n\n" +
                                  "      visited[i] = true;\n" +
                                  "      path.add(i);\n" +
                                  "      tspHelper(..., currentDist + added, n);\n" +
                                  "      path.remove(path.size() - 1);\n" +
                                  "      visited[i] = false;\n" +
                                  "    }\n" +
                                  "  }\n" +
                                  "}";
                    
                    sidebar.startMission("Route Optimization", description);
                    sidebar.addTask("Init Cost Matrix", "Mapped " + (n*n) + " battery weight vectors.");
                    sidebar.addTask("Exhaustive Search", "Started Branch-and-Bound backtracking.");
                    sidebar.addTask("Cycle Locking", "Locked optimal Hamiltonian cycle.");
                    sidebar.addCodeBlock(code);
                    
                    sidebar.setVisible(true);
                    revalidate();

                    double total = 0;
                    for (int i = 0; i < route.size(); i++)
                        total += route.get(i).distanceTo(route.get((i+1) % route.size()));
                    statusBar.setIcon(pulsingDot);
                    statusBar.setText(String.format("  Optimal TSP route found — Tour length: %.1f px", total));
                    
                    mapPanel.showExplanation(
                        DroneMapPanel.AlgoType.TSP,
                        "Optimum Route Calculated",
                        "Exhaustive Backtracking logic with branch-and-bound pruning was applied to identify the absolute minimal Hamiltonian cycle."
                    );
                } catch (Exception ex) {}
            }
        };
        worker.execute();
    }

    private void runMST() {
        List<Location> locs = mapPanel.getLocations();
        int n = locs.size();
        if (n < 2) return;
        
        statusBar.setIcon(null);
        statusBar.setText("  System: Building Minimum Spanning Tree (Kruskal's)...");
        List<Edge> mst = algMgr.kruskalMST(new java.util.ArrayList<>(locs));
        mapPanel.setMSTEdges(mst);
        
        // Sidebar Update with Task Cards + Code
        String description = "Establishes a resilient communication skeleton using greedy weight logic.";
        String code = "// Kruskal's MST algorithm\n" +
                      "public List<Edge> kruskalMST(List<Location> locs) {\n" +
                      "  List<Edge> mst = new ArrayList<>();\n\n" +
                      "  // 1. Generate O(V^2) complete graph edges\n" +
                      "  List<Edge> allEdges = new ArrayList<>();\n" +
                      "  for (int i = 0; i < n; i++)\n" +
                      "    for (int j = i + 1; j < n; j++)\n" +
                      "      allEdges.add(new Edge(locs.get(i), locs.get(j)));\n\n" +
                      "  // 2. Sort edges greedily by weight O(E log E)\n" +
                      "  Collections.sort(allEdges);\n\n" +
                      "  // 3. Disjoint-Set cycle detection\n" +
                      "  UnionFind uf = new UnionFind(n);\n" +
                      "  for (Edge e : allEdges) {\n" +
                      "    if (uf.union(e.u.id, e.v.id)) {\n" +
                      "      mst.add(e);\n" +
                      "      // Tree complete when edges = V - 1\n" +
                      "      if (mst.size() == n - 1) break;\n" +
                      "    }\n" +
                      "  }\n" +
                      "  return mst;\n" +
                      "}";
        
        sidebar.startMission("Network Connectivity", description);
        sidebar.addTask("Sorting Edges", "Prioritizing " + (n*(n-1)/2) + " link weights.");
        sidebar.addTask("Union-Find Init", "Establishing disjoint-set forest.");
        sidebar.addTask("Merging Cluster", "Eliminating communication bottlenecks.");
        sidebar.addCodeBlock(code);
        
        sidebar.setVisible(true);
        revalidate();

        double totalWeight = mst.stream().mapToDouble(e -> e.weight).sum();
        statusBar.setIcon(pulsingDot);
        statusBar.setText(String.format("  Kruskal's MST computed — Total weight: %.1f px", totalWeight));
        
        mapPanel.showExplanation(
            DroneMapPanel.AlgoType.MST,
            "Hubs Safely Connected",
            "Kruskal's Algorithm was applied to construct a Minimum Spanning Tree across the network."
        );
    }
}

class NeumorphicButton extends JButton {
    private boolean softPressed = false;
    private Color bgColor;
    private Color fgColor;

    public NeumorphicButton(String text, Color bg, Color fg) {
        super(text);
        this.bgColor = bg;
        this.fgColor = fg;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { softPressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { softPressed = false; repaint(); }
        });
    }

    public NeumorphicButton(String text) {
        this(text, null, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        setForeground(fgColor != null ? fgColor : Theme.text);
        
        int w = getWidth() - 12;
        int h = getHeight() - 12;
        int x = 6, y = 6;
        int radius = 24;

        if (!softPressed) {
            for (int i=0; i<3; i++) {
                g2.setColor(Theme.shadLight);
                g2.fillRoundRect(x - i - 2, y - i - 2, w, h, radius, radius);
            }
            for (int i=0; i<4; i++) {
                g2.setColor(Theme.shadDark);
                g2.fillRoundRect(x + i + 1, y + i + 1, w, h, radius, radius);
            }
        }
        
        g2.setColor(bgColor != null ? bgColor : Theme.bg);
        g2.fillRoundRect(x, y, w, h, radius, radius);

        if (softPressed) {
            g2.setColor(Theme.shadDark);
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(3f));
            Shape inner = new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, radius, radius);
            g2.setClip(inner);
            g2.drawRoundRect(x+1, y+1, w, h, radius, radius);
            
            g2.setColor(Theme.shadLight);
            g2.drawRoundRect(x-2, y-2, w, h, radius, radius);
            g2.setClip(null);
            g2.setStroke(old);
            
            g2.translate(1, 1);
        }

        super.paintComponent(g);
        
        if (softPressed) g2.translate(-1, -1);
    }
}

class NeumorphicCircleButton extends JButton {
    private boolean softPressed = false;

    public NeumorphicCircleButton(String text) {
        super(text);
        initButton();
    }

    public NeumorphicCircleButton(Icon icon) {
        super(icon);
        initButton();
    }

    private void initButton() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 18));
        setPreferredSize(new Dimension(42, 42)); 
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { softPressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { softPressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        setForeground(Theme.textMuted);
        
        int d = Math.min(getWidth(), getHeight()) - 10;
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;

        if (!softPressed) {
            g2.setColor(Theme.shadDark);
            g2.fillOval(x + 3, y + 3, d, d);
            g2.setColor(Theme.shadLight);
            g2.fillOval(x - 3, y - 3, d, d);
        }
        
        g2.setColor(Theme.bg);
        g2.fillOval(x, y, d, d);

        if (softPressed) {
            g2.setColor(Theme.shadDark);
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 1, y + 1, d, d);
            g2.setStroke(old);
            g2.translate(1, 1); 
        }

        super.paintComponent(g);
        
        if (softPressed) g2.translate(-1, -1);
    }
}

class NeumorphicSeparator extends JComponent {
    public NeumorphicSeparator() {
        setPreferredSize(new Dimension(8, 30));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.shadLight);
        g2.drawLine(2, 0, 2, getHeight());
        g2.setColor(Theme.shadDark);
        g2.drawLine(3, 0, 3, getHeight());
    }
}

class ThemeToggleIcon implements Icon {
    @Override public int getIconWidth() { return 20; }
    @Override public int getIconHeight() { return 20; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        
        g2.setColor(Theme.textMuted);

        if (Theme.isDark) {
            g2.fillOval(5, 5, 10, 10);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(10, 1, 10, 3);
            g2.drawLine(10, 17, 10, 19);
            g2.drawLine(1, 10, 3, 10);
            g2.drawLine(17, 10, 19, 10);
            g2.drawLine(4, 4, 5, 5);
            g2.drawLine(16, 16, 15, 15);
            g2.drawLine(4, 16, 5, 15);
            g2.drawLine(16, 4, 15, 5);
        } else {
            java.awt.geom.Area moon = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(3, 2, 14, 14));
            java.awt.geom.Area shadow = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(7, 0, 12, 12));
            moon.subtract(shadow);
            g2.fill(moon);
        }
        
        g2.dispose();
    }
}

class DroneLogoIcon implements Icon {
    @Override public int getIconWidth() { return 28; }
    @Override public int getIconHeight() { return 28; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);

        GradientPaint gp = new GradientPaint(0, 0, new Color(108, 99, 255), 28, 28, new Color(66, 153, 225));
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, 28, 28, 12, 12);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(8, 14, 14, 8);
        g2.drawLine(14, 8, 20, 14);
        g2.drawLine(20, 14, 14, 20);
        g2.drawLine(14, 20, 8, 14);
        
        g2.setColor(Color.WHITE);
        g2.fillOval(6, 12, 4, 4);
        g2.fillOval(12, 6, 4, 4);
        g2.fillOval(18, 12, 4, 4);
        g2.fillOval(12, 18, 4, 4);
        
        g2.setColor(new Color(255, 223, 0)); 
        g2.fillOval(12, 12, 4, 4);

        g2.dispose();
    }
}

class RadiatingDotIcon implements Icon {
    private final int coreRadius = 5;
    private final int maxPulseRadius = 14;
    private float pulseState = 0f;
    private final Component parent;
    private Color dotColor = new Color(16, 185, 129); 
    
    public void setActive(boolean active) {
        this.dotColor = active ? new Color(255, 180, 0) : new Color(16, 185, 129);
    }
    
    public RadiatingDotIcon(Component parent) {
        this.parent = parent;
        new Timer(40, e -> {
            pulseState += 0.05f;
            if (pulseState > 1.0f) pulseState = 0f;
            if (this.parent != null && this.parent.isShowing()) {
                this.parent.repaint();
            }
        }).start();
    }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int cx = x + maxPulseRadius;
        int cy = y + maxPulseRadius;
        
        float pulseOffset = pulseState * maxPulseRadius;
        int currentRad = (int) (coreRadius + pulseOffset);
        int alpha = (int) (200 * (1.0f - pulseState));
        if (alpha < 0) alpha = 0;
        
        g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), alpha));
        g2.fillOval(cx - currentRad, cy - currentRad, currentRad * 2, currentRad * 2);
        
        g2.setColor(dotColor);
        g2.fillOval(cx - coreRadius, cy - coreRadius, coreRadius * 2, coreRadius * 2);
        
        g2.dispose();
    }

    @Override public int getIconWidth() { return maxPulseRadius * 2; }
    @Override public int getIconHeight() { return maxPulseRadius * 2; }
}

class DroneStatusIcon implements Icon {
    @Override public int getIconWidth() { return 24; }
    @Override public int getIconHeight() { return 24; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        g2.scale(0.8, 0.8);

        g2.setColor(Theme.text);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(4, 4, 20, 20);
        g2.drawLine(20, 4, 4, 20);

        g2.setColor(new Color(255, 180, 0));
        g2.drawOval(2, 2, 6, 6);
        g2.drawOval(16, 2, 6, 6);
        g2.drawOval(2, 16, 6, 6);
        g2.drawOval(16, 16, 6, 6);

        g2.setColor(new Color(108, 99, 255));
        g2.fillOval(8, 8, 8, 8);

        g2.dispose();
    }
}

class TargetNavigationIcon implements Icon {
    @Override public int getIconWidth() { return 18; }
    @Override public int getIconHeight() { return 18; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x + 1, y + 1);
        
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(108, 99, 255, 60));
        g2.drawOval(0, 0, 16, 16);
        g2.setColor(new Color(108, 99, 255, 120));
        g2.drawOval(3, 3, 10, 10);
        
        g2.setColor(new Color(108, 99, 255));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(8, -1, 8, 2);
        g2.drawLine(8, 14, 8, 17);
        g2.drawLine(-1, 8, 2, 8);
        g2.drawLine(14, 8, 17, 8);
        g2.fillOval(7, 7, 3, 3);
        
        g2.dispose();
    }
}