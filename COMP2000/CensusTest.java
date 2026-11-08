import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that the parallel MoodCensus agrees with a plain sequential count
 * on every tick of a run. If the collector's combiner were wrong, or a
 * container were shared between threads, counts would go missing here.
 *
 * Run: java CensusTest
 */
public class CensusTest {
    public static void main(String[] args) throws Exception {
        int ticksChecked = 0, failures = 0;
        for (long seed : new long[]{918273645L, 555000111L, 777222333L}) {
            World w = new World(SimulationConfig.fromArgs(new String[]{"seed=" + seed}));
            Populate.defaults(w);
            while (w.getState() == SimulationState.RUNNING && w.getTickCount() < 400) {
                w.update();
                List<Animal> animals = w.allAnimals();

                Map<String, Integer> expected = new HashMap<>();
                for (Animal a : animals) expected.merge(a.getMood().name(), 1, Integer::sum);

                MoodCensus parallel = animals.parallelStream().collect(MoodCensus.collector());
                MoodCensus sequential = animals.stream().collect(MoodCensus.collector());

                if (!parallel.counts().equals(expected) || !sequential.counts().equals(expected)) {
                    failures++;
                    System.out.println("  FAIL seed " + seed + " tick " + w.getTickCount()
                        + ": expected " + expected + " parallel " + parallel.counts());
                }
                ticksChecked++;
            }
        }
        System.out.println("Checked " + ticksChecked + " ticks, " + failures + " mismatches");
        System.out.println(failures == 0 ? "ALL PASS" : "FAILED");
        showSharedMapRace();
    }

    /**
     * The version the collector replaces: every thread writes into one shared
     * HashMap. merge() is a read-then-write, so two threads can read the same
     * old count and both write old+1, losing an animal. Shown here, not used.
     */
    static void showSharedMapRace() {
        List<Animal> many = new java.util.ArrayList<>();
        for (int i = 0; i < 200_000; i++) many.add(i % 2 == 0 ? new Rabbit(0, 0) : new Fox(0, 0));
        int trials = 20, lossy = 0, threw = 0, worstLoss = 0;
        for (int trial = 0; trial < trials; trial++) {
            Map<String, Integer> shared = new HashMap<>();
            try {
                many.parallelStream().forEach(a -> shared.merge(a.getMood().name(), 1, Integer::sum));
            } catch (RuntimeException e) {
                // A HashMap changed by several threads at once can also corrupt itself and throw.
                threw++;
                continue;
            }
            int lost = many.size() - shared.values().stream().mapToInt(Integer::intValue).sum();
            if (lost != 0) lossy++;
            worstLoss = Math.max(worstLoss, lost);
        }
        System.out.println("Shared HashMap + parallel forEach, " + trials + " runs of " + many.size()
            + " animals: " + lossy + " lost counts (worst " + worstLoss + "), " + threw + " threw an exception");
    }
}
