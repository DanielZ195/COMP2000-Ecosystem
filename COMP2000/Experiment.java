import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Runs the default simulation on many seeds and reports how runs end.
 *
 * Each seed is a whole independent World, so the runs are spread across
 * cores with a parallel stream. That is safe because a World shares no
 * mutable state with any other: it owns its entities, its Grid, its
 * listeners and its own seeded Random. The only objects two worlds share
 * are the stateless mood singletons (Grazing, Resting, Hunting), which have
 * no fields to race on. Fleeing and Digesting carry data, and each of those
 * belongs to one animal in one world.
 *
 * The parallel results are checked against a sequential run: same seed,
 * same outcome, whichever thread ran it.
 *
 * Run: java Experiment [runs]
 */
public class Experiment {

    /** How one seeded run ended. A record, so two outcomes compare by value. */
    record Outcome(long seed, int ticks, SimulationState end, int eaten, int starved, int hops) {}

    static Outcome run(long seed) {
        SimulationConfig config;
        try {
            config = SimulationConfig.fromArgs(new String[]{"seed=" + seed, "maxTicks=3000"});
        } catch (SimulationConfigException e) {
            // A lambda passed to mapToObj cannot throw a checked exception, so wrap it.
            // These arguments are fixed, so reaching this is a bug, not bad input.
            throw new IllegalStateException(e);
        }
        World world = new World(config);
        Populate.defaults(world);
        PopulationRecorder stats = new PopulationRecorder(world);
        world.addListener(stats);
        while (world.getState() == SimulationState.RUNNING) world.update();
        return new Outcome(seed, world.getTickCount(), world.getState(),
                           stats.getEaten(), stats.getStarved(), stats.getHops());
    }

    static List<Outcome> runAll(int runs, boolean parallel) {
        LongStream seeds = LongStream.range(0, runs).map(i -> 1_000_003L * i + 77);
        return (parallel ? seeds.parallel() : seeds)
            .mapToObj(Experiment::run)
            .collect(Collectors.toList()); // keeps seed order even when parallel
    }

    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 20;

        long t0 = System.nanoTime();
        List<Outcome> sequential = runAll(runs, false);
        long t1 = System.nanoTime();
        List<Outcome> parallel = runAll(runs, true);
        long t2 = System.nanoTime();

        Map<SimulationState, Long> endings = parallel.stream()
            .collect(Collectors.groupingBy(Outcome::end,
                     () -> new EnumMap<>(SimulationState.class), Collectors.counting()));
        IntSummaryStatistics ticks = parallel.stream().mapToInt(Outcome::ticks).summaryStatistics();
        double eaten = parallel.stream().mapToInt(Outcome::eaten).average().orElse(0);
        double starved = parallel.stream().mapToInt(Outcome::starved).average().orElse(0);

        System.out.printf("%d runs   ticks avg %.0f (min %d, max %d)   eaten avg %.0f   starved avg %.0f%n",
            runs, ticks.getAverage(), ticks.getMin(), ticks.getMax(), eaten, starved);
        System.out.println("endings: " + endings);
        System.out.printf("sequential %.2fs   parallel %.2fs   speedup %.1fx on %d cores%n",
            (t1 - t0) / 1e9, (t2 - t1) / 1e9, (double) (t1 - t0) / (t2 - t1),
            Runtime.getRuntime().availableProcessors());
        System.out.println(sequential.equals(parallel)
            ? "parallel outcomes identical to sequential: PASS"
            : "parallel outcomes DIFFER from sequential: FAIL");
    }
}
