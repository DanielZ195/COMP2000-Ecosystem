/**
 * Predator default mood: chase prey when hungry, look for a mate when able,
 * otherwise patrol. A kill moves the predator into Digesting.
 */
public final class Hunting implements Mood<Predator> {
    public static final Hunting INSTANCE = new Hunting();

    private Hunting() {}

    @Override
    public Mood<Predator> act(Predator predator, World world) {
        predator.tryBreed(world);

        Prey target = predator.nearestPrey(world);
        if (target != null && !predator.isWellFed()) {
            return chase(predator, target, world);
        }

        if (predator.canBreed()) {
            Animal mate = predator.nearestMate(world);
            if (mate != null) {
                predator.moveToward(mate);
                return this;
            }
        }

        if (target != null) {
            return chase(predator, target, world);
        }
        predator.wander(world);
        return this;
    }

    private Mood<Predator> chase(Predator predator, Prey target, World world) {
        predator.moveToward(target);
        if (predator.tryEat(target, world)) {
            return new Digesting();
        }
        return this;
    }

    @Override
    public String name() { return "Hunting"; }
}
