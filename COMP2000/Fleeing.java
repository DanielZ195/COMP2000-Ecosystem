import java.awt.Color;

/**
 * Prey panic mood. Runs for the refuge or knight-hops clear of the nearest
 * predator, and keeps running for a few ticks after losing sight of it.
 *
 * Unlike Grazing, a Fleeing object carries data that belongs to one animal
 * (where the threat was last seen, how long until it calms down), so each
 * prey that panics gets its own instance.
 *
 * Panic costs extra energy. A predator therefore drains prey that it never
 * catches, just by being seen.
 */
public final class Fleeing implements Mood<Prey> {
    static final int CALM_TICKS = 3;
    static final double PANIC_METABOLISM = 1.5;

    private int lastThreatX, lastThreatY;
    private int calmIn = CALM_TICKS;

    @Override
    public Mood<Prey> act(Prey prey, World world) {
        Predator threat = prey.nearestThreat(world);

        if (threat != null) {
            lastThreatX = threat.getX();
            lastThreatY = threat.getY();
            calmIn = CALM_TICKS;
            if (world.isInSafeZone(prey.getX(), prey.getY())) {
                prey.wander(world);
            } else if (prey.distanceToRefuge(world) <= prey.refugeRange()) {
                prey.moveToward(world.getZoneCentreX(), world.getZoneCentreY());
            } else {
                prey.evade(threat, world);
            }
            return this;
        }

        if (--calmIn <= 0) {
            return Grazing.INSTANCE;
        }
        // Out of sight is not out of danger: keep putting distance between
        // this animal and the last place the predator was seen.
        prey.moveAwayFrom(lastThreatX, lastThreatY);
        return this;
    }

    @Override
    public double metabolismRate() { return PANIC_METABOLISM; }

    @Override
    public String name() { return "Fleeing"; }

    @Override
    public Color marker() { return Color.YELLOW; }
}
