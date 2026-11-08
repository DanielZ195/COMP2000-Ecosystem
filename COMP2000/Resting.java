import java.awt.Color;

/**
 * Well-fed prey with no mate in range sits still and burns energy at half
 * the normal rate.
 *
 * Grazing enters this mood above 80% health, and the animal only leaves it
 * once health falls below 50%. The gap between those two numbers stops an
 * animal flickering between the two moods every tick.
 */
public final class Resting implements Mood<Prey> {
    public static final Resting INSTANCE = new Resting();
    static final double RESTING_METABOLISM = 0.5;

    private Resting() {}

    @Override
    public Mood<Prey> act(Prey prey, World world) {
        if (prey.nearestThreat(world) != null) {
            return new Fleeing().act(prey, world);
        }
        if (!prey.isWellFed()) {
            return Grazing.INSTANCE.act(prey, world);
        }
        prey.tryBreed(world);
        if (prey.canBreed() && prey.nearestMate(world) != null) {
            return Grazing.INSTANCE.act(prey, world);
        }
        return this;
    }

    @Override
    public double metabolismRate() { return RESTING_METABOLISM; }

    @Override
    public String name() { return "Resting"; }

    @Override
    public Color marker() { return Color.CYAN; }
}
