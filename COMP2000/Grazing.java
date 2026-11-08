/**
 * Prey default mood: eat when hungry, breed when able, otherwise drift.
 *
 * Grazing holds no per-animal data, so a single shared instance serves
 * every prey animal in this mood.
 */
public final class Grazing implements Mood<Prey> {
    public static final Grazing INSTANCE = new Grazing();

    /** Health fraction above which a prey with nothing to do settles down. */
    static final double SATED = 0.8;

    private Grazing() {}

    @Override
    public Mood<Prey> act(Prey prey, World world) {
        Predator threat = prey.nearestThreat(world);
        if (threat != null) {
            // Switch and react in the same tick: losing a tick to the
            // transition would hand every predator a free step.
            return new Fleeing().act(prey, world);
        }

        prey.tryBreed(world);

        Food food = prey.nearestFood(world);
        if (food != null && !prey.isWellFed()) {
            prey.moveToward(food);
            prey.tryEatFood(food);
            return this;
        }

        if (prey.canBreed()) {
            Animal mate = prey.nearestMate(world);
            if (mate != null) {
                prey.moveToward(mate);
                return this;
            }
        }

        if (prey.getHealth() >= prey.getMaxHealth() * SATED) {
            return Resting.INSTANCE;
        }

        if (food != null) {
            prey.moveToward(food);
            prey.tryEatFood(food);
        } else {
            prey.wander(world);
        }
        return this;
    }

    @Override
    public String name() { return "Grazing"; }
}
