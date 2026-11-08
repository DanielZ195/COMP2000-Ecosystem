import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        SimulationConfig config;
        try {
            config = SimulationConfig.fromArgs(args);
        } catch (SimulationConfigException e) {
            System.err.println("Cannot start: " + e.getMessage());
            if (e.getCause() != null) System.err.println("  caused by: " + e.getCause());
            System.err.println("Usage: java Main [key=value ...]");
            return;
        }

        World world = new World(config);
        System.out.println("Simulation seed: " + world.getSeed()
            + "  (re-run with: java Main seed=" + world.getSeed() + ")");
        System.out.println("Settings: " + config);

        Random rng = world.getRandom();
        int cols = config.getCols(), rows = config.getRows();

        for (int i = 0; i < config.getHawks(); i++) {
            int[] p = randomPointOutsideZone(world);
            world.addHawk(new Hawk(p[0], p[1]));
        }
        for (int i = 0; i < config.getFoxes(); i++) {
            int[] p = randomPointOutsideZone(world);
            world.addFox(new Fox(p[0], p[1]));
        }
        for (int i = 0; i < config.getRabbits(); i++) world.addRabbit(new Rabbit(rng.nextInt(cols), rng.nextInt(rows)));
        for (int i = 0; i < config.getMice(); i++)    world.addMouse(new Mouse(rng.nextInt(cols), rng.nextInt(rows)));
        for (int i = 0; i < config.getFood(); i++)    world.addFood(new Food(rng.nextInt(cols), rng.nextInt(rows)));

        SimPanel panel = new SimPanel(world);
        panel.setPreferredSize(new Dimension(cols * SimPanel.CELL_SIZE, rows * SimPanel.CELL_SIZE));
        PopulationRecorder recorder = new PopulationRecorder(world);
        world.addListener(recorder);
        GraphPanel graph = new GraphPanel(world, recorder);

        JFrame frame = new JFrame("Predator and Prey Simulation - COMP2000");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel, BorderLayout.CENTER);
        frame.add(graph, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // The timer only drives the clock. Everything that reacts to a tick
        // subscribes to the world, so Main no longer has to know who that is.
        Timer timer = new Timer(config.getTickMs(), e -> world.update());

        world.addListener(WorldListener.onTick(w -> {
            panel.repaint();
            graph.repaint();
        }));
        world.addListener(new ConsoleReporter(recorder));
        world.addListener(new WorldListener() {
            @Override
            public void onEnded(World w, SimulationState state) {
                timer.stop();
            }
        });
        timer.start();
    }

    /** Keeps re-rolling until the point lands outside the refuge predators cannot enter. */
    private static int[] randomPointOutsideZone(World world) {
        Random rng = world.getRandom();
        int x, y;
        do {
            x = rng.nextInt(world.getWidth());
            y = rng.nextInt(world.getHeight());
        } while (world.isInSafeZone(x, y));
        return new int[]{x, y};
    }
}
