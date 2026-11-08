import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps the run's statistics: population per species per tick, plus totals
 * for births, deaths by cause and evasive hops.
 *
 * This used to be fields inside World. As a WorldListener it learns
 * everything from events, so World only runs the simulation, and a
 * headless experiment can attach a recorder without any Swing panels.
 */
public class PopulationRecorder implements WorldListener {
    private final Map<String, List<Integer>> history = new LinkedHashMap<>();
    private int births, starved, eaten, hops;
    private MoodCensus census;

    /**
     * Below this many animals the census runs sequentially. CensusBenchmark
     * found parallel 12x slower at 100 animals, still slower at 10,000, and
     * 5.6x faster at 100,000. The collector is thread safe either way, so
     * this only chooses the faster path; it does not change the result.
     */
    static final int PARALLEL_THRESHOLD = 50_000;

    public PopulationRecorder(World world) {
        for (String species : world.speciesColours().keySet()) {
            history.put(species, new ArrayList<>());
        }
    }

    /**
     * One pass over every entity, grouped by species. groupingBy leaves out
     * a species with no members, so getOrDefault records an extinct
     * species as 0 instead of skipping its point on the graph.
     */
    @Override
    public void onTick(World world) {
        Map<String, Long> counts = world.allEntities().stream()
            .collect(Collectors.groupingBy(Entity::getSpeciesName, Collectors.counting()));
        history.forEach((species, series) ->
            series.add(counts.getOrDefault(species, 0L).intValue()));

        List<Animal> animals = world.allAnimals();
        Stream<Animal> source = animals.size() >= PARALLEL_THRESHOLD
            ? animals.parallelStream()
            : animals.stream();
        census = source.collect(MoodCensus.collector());
    }

    /** Moods at the end of the latest tick, or null before the first tick. */
    public MoodCensus getCensus() { return census; }

    @Override
    public void onBirth(Animal child) { births++; }

    @Override
    public void onDeath(Animal animal, DeathCause cause) {
        if (cause == DeathCause.STARVED) starved++;
        else if (cause == DeathCause.EATEN) eaten++;
    }

    @Override
    public void onEvade(Prey prey) { hops++; }

    public Map<String, List<Integer>> getHistory() { return history; }
    public int getBirths() { return births; }
    public int getStarved() { return starved; }
    public int getEaten() { return eaten; }
    public int getHops() { return hops; }

    /** One line of totals, shared by the graph panel and the console. */
    public String totals() {
        return "births " + births + "   starved " + starved
             + "   eaten " + eaten + "   hops " + hops;
    }
}
