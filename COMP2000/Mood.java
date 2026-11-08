import java.awt.Color;

/**
 * State pattern: the behaviour mode an Animal is in right now.
 *
 * The animal is the context. It holds one Mood and hands each tick to it.
 * The mood does that tick's work and returns the mood for the next tick,
 * which is usually itself. Returning the next state, instead of calling
 * animal.setMood(...) from inside the state, keeps the context's field
 * private and makes every transition visible as a return statement.
 *
 * A is bounded to Animal so a prey mood can be a Mood<Prey> and call
 * prey-only methods (evade, tryEatFood) without casting, and the compiler
 * stops a prey mood from ever being handed to a predator.
 */
public interface Mood<A extends Animal> {

    /** Do this tick's behaviour and return the mood for the next tick. */
    Mood<A> act(A animal, World world);

    /** Multiplier on the animal's energy cost per tick while in this mood. */
    default double metabolismRate() { return 1.0; }

    /** Shown in the census and the console. */
    String name();

    /** Colour of the small marker drawn beside the animal, or null for none. */
    default Color marker() { return null; }
}
