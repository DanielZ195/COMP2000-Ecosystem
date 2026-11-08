/**
 * Prints a one-line summary to the console when the run ends.
 *
 * A listener rather than code in Main's timer, so the same report appears
 * however the world is being driven: the Swing window, or a headless test.
 */
public class ConsoleReporter implements WorldListener {
    private final PopulationRecorder recorder;

    public ConsoleReporter(PopulationRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onEnded(World world, SimulationState state) {
        System.out.println("Ended at tick " + world.getTickCount() + ": " + state
            + "  " + recorder.totals());
    }
}
