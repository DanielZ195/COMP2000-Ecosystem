import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public PopulationRecorder(World world) {
        for (String species : world.speciesColours().keySet()) {
            history.put(species, new ArrayList<>());
        }
    }

    @Override
    public void onTick(World world) {
        history.get("Hawks").add(world.getHawks().size());
        history.get("Foxes").add(world.getFoxes().size());
        history.get("Rabbits").add(world.getRabbits().size());
        history.get("Mice").add(world.getMice().size());
        history.get("Food").add(world.getFood().size());
    }

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
