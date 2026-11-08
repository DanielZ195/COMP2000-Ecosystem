import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * Draws the population history as a line per species, most recent WINDOW
 * ticks. Line colours come from the species themselves, so the graph can
 * never disagree with what is drawn on the grid.
 */
public class GraphPanel extends JPanel {
    private static final int WINDOW = 400;
    private static final int HEIGHT = 120;

    private final World world;
    private final PopulationRecorder recorder;
    private final Map<String, Color> colours;

    public GraphPanel(World world, PopulationRecorder recorder) {
        this.world = world;
        this.recorder = recorder;
        this.colours = world.speciesColours();
        setBackground(new Color(20, 20, 20));
        setPreferredSize(new Dimension(100, HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        Map<String, List<Integer>> history = recorder.getHistory();

        // Scaled to the animal counts only. Food runs an order of magnitude
        // higher, and including it flattens every species line to the floor.
        int peak = 1;
        for (Map.Entry<String, List<Integer>> e : history.entrySet()) {
            if (e.getKey().equals("Food")) continue;
            List<Integer> series = e.getValue();
            for (int i = Math.max(0, series.size() - WINDOW); i < series.size(); i++) {
                peak = Math.max(peak, series.get(i));
            }
        }

        for (Map.Entry<String, List<Integer>> entry : history.entrySet()) {
            List<Integer> series = entry.getValue();
            int from = Math.max(0, series.size() - WINDOW);
            int n = series.size() - from;
            if (n < 2) continue;
            g.setColor(colours.get(entry.getKey()));
            for (int i = 1; i < n; i++) {
                int x1 = (i - 1) * w / WINDOW, x2 = i * w / WINDOW;
                int y1 = plot(series.get(from + i - 1), peak, h);
                int y2 = plot(series.get(from + i), peak, h);
                g.drawLine(x1, y1, x2, y2);
            }
        }

        g.setColor(Color.GRAY);
        g.drawString("max " + peak, 4, 12);

        int x = 60;
        for (Map.Entry<String, Color> c : colours.entrySet()) {
            g.setColor(c.getValue());
            g.drawString(c.getKey(), x, 12);
            x += 58;
        }

        g.setColor(Color.LIGHT_GRAY);
        g.drawString(summary(), 4, h - 4);
    }

    /** Clamped to the panel, so the food line pins to the top instead of flying off it. */
    private int plot(int value, int peak, int h) {
        int y = h - 16 - value * (h - 30) / peak;
        return Math.max(14, y);
    }

    private String summary() {
        String ending = world.getState() == SimulationState.RUNNING
            ? "" : "   ENDED: " + world.getState();
        return "tick " + world.getTickCount() + "   " + recorder.totals() + ending;
    }
}
