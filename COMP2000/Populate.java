import java.util.Random;

/**
 * Places the starting population a config asks for, for the headless
 * test harnesses. Uses the world's own seeded Random, so a seed always
 * gives the same opening board.
 */
public final class Populate {
    private Populate() {}

    public static void defaults(World w) {
        SimulationConfig c = w.getConfig();
        Random r = w.getRandom();
        int cols = c.getCols(), rows = c.getRows();
        for (int i = 0; i < c.getHawks(); i++)   w.addHawk(new Hawk(r.nextInt(cols), r.nextInt(rows)));
        for (int i = 0; i < c.getFoxes(); i++)   w.addFox(new Fox(r.nextInt(cols), r.nextInt(rows)));
        for (int i = 0; i < c.getRabbits(); i++) w.addRabbit(new Rabbit(r.nextInt(cols), r.nextInt(rows)));
        for (int i = 0; i < c.getMice(); i++)    w.addMouse(new Mouse(r.nextInt(cols), r.nextInt(rows)));
        for (int i = 0; i < c.getFood(); i++)    w.addFood(new Food(r.nextInt(cols), r.nextInt(rows)));
    }
}
