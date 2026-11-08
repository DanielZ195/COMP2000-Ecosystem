import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;

/**
 * How many animals are in each mood, and their average health, for one tick.
 *
 * Built by a custom Collector so it can run over a parallel stream. A
 * parallel stream splits the animals across worker threads. Each thread
 * gets its own Tally from the supplier and only ever adds to that one, and
 * the combiner merges two finished tallies after their threads are done.
 * No Tally is touched by two threads at once, so no locking is needed.
 * Sharing one HashMap across threads would lose counts.
 */
public final class MoodCensus {
    private final Map<String, Integer> counts;
    private final Map<String, Double> meanHealth;

    private MoodCensus(Map<String, Integer> counts, Map<String, Double> meanHealth) {
        this.counts = Collections.unmodifiableMap(counts);
        this.meanHealth = Collections.unmodifiableMap(meanHealth);
    }

    public int count(String mood) { return counts.getOrDefault(mood, 0); }
    public double meanHealth(String mood) { return meanHealth.getOrDefault(mood, 0.0); }

    /** Mood names in alphabetical order, so the display does not jump about. */
    public Map<String, Integer> counts() { return counts; }

    /** e.g. "Digesting 2 (hp 210)  Fleeing 9 (hp 61) ..." */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        counts.forEach((mood, n) -> sb.append(String.format("%s %d (hp %.0f)   ", mood, n, meanHealth(mood))));
        return sb.toString().trim();
    }

    /** The mutable container each thread fills in. Never seen outside the collector. */
    private static final class Tally {
        final Map<String, Integer> counts = new HashMap<>();
        final Map<String, Double> health = new HashMap<>();

        void add(Animal a) {
            String mood = a.getMood().name();
            counts.merge(mood, 1, Integer::sum);
            health.merge(mood, a.getHealth(), Double::sum);
        }

        Tally combine(Tally other) {
            other.counts.forEach((mood, n) -> counts.merge(mood, n, Integer::sum));
            other.health.forEach((mood, h) -> health.merge(mood, h, Double::sum));
            return this;
        }

        MoodCensus finish() {
            Map<String, Integer> sortedCounts = new TreeMap<>(counts);
            Map<String, Double> means = new TreeMap<>();
            counts.forEach((mood, n) -> means.put(mood, health.get(mood) / n));
            return new MoodCensus(sortedCounts, means);
        }
    }

    /**
     * supplier: a fresh Tally per thread; accumulator: add one animal;
     * combiner: merge two partial tallies; finisher: freeze into a census.
     * UNORDERED because the result does not depend on which animal came first.
     */
    public static Collector<Animal, ?, MoodCensus> collector() {
        return Collector.of(Tally::new, Tally::add, Tally::combine, Tally::finish,
                            Collector.Characteristics.UNORDERED);
    }
}
