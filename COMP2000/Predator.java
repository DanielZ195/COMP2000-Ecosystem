import java.util.List;

/**
 * Predator is-a Animal that can eat Prey. Hawk and Fox both extend this so
 * they share hunting and eating logic but move differently.
 *
 * tryEat() takes a Prey (not just any Animal) because only Prey is Edible -
 * the health gained comes from the prey's own getNutritionValue().
 *
 * Behaviour per tick comes from the current Mood: Hunting, or Digesting
 * for a few ticks after a kill.
 */
public abstract class Predator extends Animal {
    protected int eatDistance = 0;
    private Mood<Predator> mood = Hunting.INSTANCE;

    public Predator(int x, int y, double startHealth, int speed, int visionRadius) {
        super(x, y, startHealth, speed, visionRadius);
    }

    @Override
    protected double breedThreshold() { return maxHealth * 0.75; }

    /** Predators pay far more than prey to breed, so their numbers lag prey numbers. */
    @Override
    protected double breedCost() { return maxHealth * 0.40; }

    /**
     * The refuge check matters: World ejects predators from the safe zone only
     * after they have already acted, so without this a predator could step in,
     * eat, and be pushed back out in the same tick.
     */
    protected boolean tryEat(Prey prey, World world) {
        if (world.isInSafeZone(prey == null ? -1 : prey.getX(),
                               prey == null ? -1 : prey.getY())) return false;
        if (prey != null && prey.isAlive() && distanceTo(prey) <= eatDistance) {
            prey.kill(DeathCause.EATEN);
            feed(prey.getNutritionValue());
            return true;
        }
        return false;
    }

    /** Closest visible prey that is outside the refuge, or null. */
    protected Prey nearestPrey(World world) {
        List<Prey> visible =
            world.getGrid().occupantsWithin(getX(), getY(), visionRadius, Prey.class);
        visible.removeIf(p -> world.isInSafeZone(p.getX(), p.getY()));
        return findNearest(visible);
    }

    public Mood<Predator> getMood() { return mood; }

    /** Hunting or Digesting decides what this tick looks like. */
    @Override
    protected void act(World world) {
        mood = mood.act(this, world);
    }
}
