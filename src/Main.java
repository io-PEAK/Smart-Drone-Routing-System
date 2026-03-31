public class Main {
    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(
                javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        javax.swing.SwingUtilities.invokeLater(DroneMapUI::new);
    }
}
