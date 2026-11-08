import java.awt.Color;

/**
 * After a kill a predator stops where it is for a few ticks, at half the
 * normal energy cost. It can still breed with a neighbour.
 *
 * Each predator that eats gets its own Digesting object, because the
 * countdown belongs to that one predator.
 */
public final class Digesting implements Mood<Predator> {
    static final int DIGEST_TICKS = 4;
    static final double DIGEST_METABOLISM = 0.5;

    private int ticksLeft = DIGEST_TICKS;

    @Override
    public Mood<Predator> act(Predator predator, World world) {
        predator.tryBreed(world);
        if (--ticksLeft <= 0) {
            return Hunting.INSTANCE;
        }
        return this;
    }

    @Override
    public double metabolismRate() { return DIGEST_METABOLISM; }

    @Override
    public String name() { return "Digesting"; }

    @Override
    public Color marker() { return Color.MAGENTA; }
}
