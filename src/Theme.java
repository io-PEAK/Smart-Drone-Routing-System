import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Theme {
    public static boolean isDark = false;

    public static Color bg;
    public static Color text;
    public static Color textMuted;
    public static Color border;
    public static Color shadLight;
    public static Color shadDark;
    public static Color grid;
    public static Color tooltipBg;
    public static Color overlayBg;

    private static final Preferences prefs = Preferences.userNodeForPackage(Theme.class);
    private static final String THEME_KEY = "isDarkTheme";

    private static final List<Runnable> listeners = new ArrayList<>();

    static {
        // Load the saved preference. 
        // The 'true' at the end makes Dark Theme the default on the very first run!
        isDark = prefs.getBoolean(THEME_KEY, true); 
        updateColors();
    }

    public static void toggle() {
        isDark = !isDark;
        // Save the user's choice immediately when they click the toggle button
        prefs.putBoolean(THEME_KEY, isDark);
        
        updateColors();
        for (Runnable r : listeners) r.run();
    }


    public static void addListener(Runnable r) {
        listeners.add(r);
    }

    public static void updateColors() {
        if (isDark) {
            bg = new Color(28, 33, 40); // Sleek dark slate
            text = new Color(210, 215, 225); // Off-white text
            textMuted = new Color(140, 150, 165); // Muted slate text
            shadLight = new Color(40, 47, 57, 255); // Top-left highlight
            shadDark = new Color(16, 19, 23, 200); // Bottom-right shadow
            grid = new Color(255, 255, 255, 15);
            border = new Color(45, 52, 65);
            overlayBg = new Color(10, 12, 16, 160);
            tooltipBg = new Color(28, 33, 40, 220);
        } else {
            bg = new Color(224, 229, 236);
            text = new Color(61, 72, 82);
            textMuted = new Color(107, 114, 128);
            shadLight = new Color(255, 255, 255, 180);
            shadDark = new Color(163, 177, 198, 110);
            grid = new Color(200, 205, 215, 100);
            border = new Color(200, 205, 215);
            overlayBg = new Color(0, 0, 0, 80);
            tooltipBg = new Color(224, 229, 236, 200);
        }
        for (Runnable r : listeners)
            r.run();
    }
}