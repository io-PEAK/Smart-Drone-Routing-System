import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * AlgorithmSidebar.java
 * Optimized with BoxLayout and dynamic Theme support.
 */
public class AlgorithmSidebar extends JPanel {

    private final JPanel content;
    private final JScrollPane scroll;
    private final JLabel titleLabel;
    private final JPanel codeContainer;
    private final JButton closeButton;
    private final JPanel headerPanel;
    private Runnable closeAction;
    
    private float  sideAlpha = 0f;
    private Timer  animTimer;
    private String currentMissionTitle = "Mission";
    
    // Code block stays dark in both themes for a "terminal" look
    private static final Color COL_CODE_BG = new Color(30, 32, 40);
    private static final Color COL_CODE_TEXT = new Color(210, 215, 225);

    public AlgorithmSidebar() {
        setPreferredSize(new Dimension(380, 0));
        setMinimumSize(new Dimension(300, 0)); 
        setBackground(Theme.bg);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.border));

        // Header
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.bg);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 10));
        
        titleLabel = new JLabel("MISSION STATUS");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(Theme.textMuted);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        closeButton = new JButton("✕");
        closeButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        closeButton.setForeground(Theme.textMuted);
        closeButton.setBorder(null);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeButton.setForeground(new Color(255, 100, 100)); }
            @Override public void mouseExited(MouseEvent e) { closeButton.setForeground(Theme.textMuted); }
        });
        closeButton.addActionListener(e -> {
            if (closeAction != null) closeAction.run();
        });
        headerPanel.add(closeButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Content panel with BoxLayout
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.bg);
        content.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        // Phantom Scrollbar Logic (Invisible but active)
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {}
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            private JButton createZeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });
        add(scroll, BorderLayout.CENTER);

        codeContainer = new JPanel();
        codeContainer.setLayout(new BoxLayout(codeContainer, BoxLayout.Y_AXIS));
        codeContainer.setOpaque(false);
        codeContainer.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        add(codeContainer, BorderLayout.SOUTH);

        animTimer = new Timer(16, e -> {
            if (sideAlpha < 1.0f) {
                sideAlpha += 0.08f;
                content.getVisibleRect(); // Force layout check
                repaint();
            } else {
                animTimer.stop();
            }
        });
    }

    /**
     * Called by the main UI when the theme toggles to dynamically update colors.
     */
    public void updateThemeUI() {
        setBackground(Theme.bg);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.border));
        
        headerPanel.setBackground(Theme.bg);
        content.setBackground(Theme.bg);
        
        titleLabel.setForeground(Theme.textMuted);
        closeButton.setForeground(Theme.textMuted);
        
        // Update any existing dynamic cards in the sidebar
        for (Component c : content.getComponents()) {
            if (c instanceof JPanel) {
                c.repaint(); // Force neumorphic shadows to redraw with new theme colors
                for (Component child : ((JPanel)c).getComponents()) {
                    if (child instanceof JLabel) {
                        JLabel lbl = (JLabel)child;
                        // Update text color for body text (bold/accent text stays purple)
                        if (!lbl.getText().contains("<b>") && lbl.getForeground().getGreen() != 99) {
                            lbl.setForeground(Theme.textMuted);
                        }
                    }
                }
            }
        }
        
        revalidate();
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        float safeAlpha = Math.max(0f, Math.min(1f, sideAlpha));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeAlpha));
        
        int offset = (int)((1.0f - sideAlpha) * 40);
        g2.translate(offset, 0);
        super.paintChildren(g2);
        g2.dispose();
    }

    public void setOnClose(Runnable action) { this.closeAction = action; }
    public void clear() { content.removeAll(); content.revalidate(); content.repaint(); }

    public void startMission(String title, String desc) {
        this.currentMissionTitle = title;
        clear();
        sideAlpha = 0f;
        animTimer.start();
        titleLabel.setText(title.toUpperCase());
        addInfoCard("Mission Overview", desc);
    }

    public void addCodeBlock(String code) {
        JLabel l = new JLabel("SOURCE CODE (Click to Expand)");
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(Theme.textMuted);
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        codeContainer.removeAll();
        codeContainer.add(l);

        // Sidebar embedded code preview with dots
        JPanel term = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COL_CODE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 95, 86)); g2.fillOval(12, 12, 6, 6);
                g2.setColor(new Color(255, 189, 46)); g2.fillOval(20, 12, 6, 6);
                g2.setColor(new Color(39, 201, 63)); g2.fillOval(28, 12, 6, 6);
                g2.dispose();
            }
        };
        term.setOpaque(false);
        term.setBorder(BorderFactory.createEmptyBorder(32, 15, 15, 15));
        term.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextArea area = new JTextArea(code);
        area.setFont(new Font("Monospaced", Font.PLAIN, 11));
        area.setForeground(COL_CODE_TEXT);
        area.setBackground(COL_CODE_BG);
        area.setEditable(false);
        area.setFocusable(false);
        
        JScrollPane codeScroll = new JScrollPane(area);
        codeScroll.setBorder(null);
        codeScroll.setOpaque(false);
        codeScroll.getViewport().setOpaque(false);
        codeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        // Hide horizontal scrollbar inside the sidebar embedded preview
        codeScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        codeScroll.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {}
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });

        MouseAdapter popupListener = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                showCodePopup(code, currentMissionTitle + " Source Code"); 
            }
            @Override public void mouseEntered(MouseEvent e) {
                term.setCursor(new Cursor(Cursor.HAND_CURSOR));
                area.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        };
        area.addMouseListener(popupListener);
        term.addMouseListener(popupListener);
        
        term.add(codeScroll, BorderLayout.CENTER);
        codeContainer.add(term);
        codeContainer.revalidate();
        codeContainer.repaint();
    }

    private void showCodePopup(String code, String title) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(650, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(COL_CODE_BG);
        
        JTextArea fullArea = new JTextArea(code);
        fullArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        fullArea.setForeground(COL_CODE_TEXT);
        fullArea.setBackground(COL_CODE_BG);
        fullArea.setEditable(false);
        fullArea.setMargin(new Insets(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(fullArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Hide vertical scrollbar completely
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {}
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
        
        // Hide horizontal scrollbar completely
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {}
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
        
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        dialog.setContentPane(contentPane);
        dialog.setVisible(true);
    }

    public void addTask(String taskName, String status) {
        JPanel task = createBaseCard(8, 12);
        
        JLabel name = new JLabel("<html><div style='width: 325px;'><b>" + taskName + "</b></div></html>");
        name.setFont(new Font("SansSerif", Font.PLAIN, 12));
        name.setForeground(new Color(108, 99, 255));
        
        JLabel stat = new JLabel("<html><div style='width: 325px;'>" + status + "</div></html>");
        stat.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stat.setForeground(Theme.textMuted);
        stat.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        task.add(name);
        task.add(stat);
        
        content.add(task);
        content.add(Box.createVerticalStrut(6));
        
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
        
        content.revalidate();
        content.repaint();
    }

    public void updateForAlgo(String title, String desc, String code) {
        startMission(title, desc);
        addCodeBlock(code);
    }

    private void addInfoCard(String title, String text) {
        JPanel card = createBaseCard(10, 15);
        
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 13));
        t.setForeground(new Color(108, 99, 255));
        card.add(t);

        JLabel b = new JLabel("<html><div style='width: 325px;'>" + text + "</div></html>");
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setForeground(Theme.text);
        b.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        card.add(b);

        content.add(card);
        content.add(Box.createVerticalStrut(10));
    }

    private JPanel createBaseCard(int vPad, int hPad) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.shadLight); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Theme.shadDark);  g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(vPad, hPad, vPad, hPad));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(340, 200));
        return p;
    }
}