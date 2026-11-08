import java.util.function.Consumer;

/**
 * Observer pattern: anything that wants to know what happens in the World
 * implements this and registers with world.addListener(...).
 *
 * World is the subject. It calls these methods as things happen and does
 * not know or care who is listening: the panels, the statistics recorder
 * and the console reporter all subscribe the same way.
 *
 * Every method has an empty default body, so a listener overrides only the
 * events it cares about.
 */
public interface WorldListener {

    /** Called once at the end of every tick, after the dead are removed. */
    default void onTick(World world) {}

    /** A new animal was born and placed in the world. */
    default void onBirth(Animal child) {}

    /** An animal died this tick, with the reason. */
    default void onDeath(Animal animal, DeathCause cause) {}

    /** A prey animal spent energy on a knight-hop away from a predator. */
    default void onEvade(Prey prey) {}

    /** The run has stopped. Fired once, on the tick the state leaves RUNNING. */
    default void onEnded(World world, SimulationState state) {}

    /** Shorthand for a listener that only cares about ticks: World.addListener(onTick(w -> ...)). */
    static WorldListener onTick(Consumer<World> action) {
        return new WorldListener() {
            @Override
            public void onTick(World world) { action.accept(world); }
        };
    }
}
