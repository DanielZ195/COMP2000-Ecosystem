import java.util.ArrayList;
import java.util.List;

/**
 * Times the MoodCensus collector on a sequential vs a parallel stream for
 * several population sizes, to find where parallel starts to pay off.
 *
 * Run: java CensusBenchmark
 */
public class CensusBenchmark {
    public static void main(String[] args) {
        System.out.println("cores: " + Runtime.getRuntime().availableProcessors());
        for (int n : new int[]{100, 1_000, 10_000, 100_000, 1_000_000}) {
            List<Animal> animals = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                animals.add(i % 4 == 0 ? new Fox(0, 0) : i % 4 == 1 ? new Hawk(0, 0)
                          : i % 4 == 2 ? new Rabbit(0, 0) : new Mouse(0, 0));
            }
            double seq = time(animals, false), par = time(animals, true);
            System.out.printf("%,9d animals   sequential %9.1f us   parallel %9.1f us   speedup %.2fx%n",
                n, seq, par, seq / par);
        }
    }

    /** Median of 31 timed runs after a warm-up, in microseconds. */
    static double time(List<Animal> animals, boolean parallel) {
        for (int i = 0; i < 50; i++) run(animals, parallel); // let the JIT compile it first
        long[] t = new long[31];
        for (int i = 0; i < t.length; i++) {
            long start = System.nanoTime();
            run(animals, parallel);
            t[i] = System.nanoTime() - start;
        }
        java.util.Arrays.sort(t);
        return t[t.length / 2] / 1000.0;
    }

    static MoodCensus run(List<Animal> animals, boolean parallel) {
        return (parallel ? animals.parallelStream() : animals.stream()).collect(MoodCensus.collector());
    }
}
