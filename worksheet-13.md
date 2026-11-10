# COMP2000 Worksheet 2 — Final Submission

**Student name:** Daniel Zohar

**Student ID:** 47572221

**GitHub repo URL:** https://github.com/DanielZ195/COMP2000-Ecosystem
(a GitHub fork of the team repository `AnumTaliya/COMP2000Assignment1`)

---

## 1. Design Patterns

Don't forget to submit a pdf file which shows a diagram of your pattern implemenation to supplement this text description.

> Submitted as `patterns.pdf` (also in the repository at `design/patterns.pdf`).

**1.1.** Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

Two: State and Observer.

**State: animal moods** (PR #1, branch `mood-states`)

| Role | Class |
|---|---|
| State interface | `Mood<A extends Animal>` |
| Concrete states for prey | `Grazing`, `Fleeing`, `Resting` (each a `Mood<Prey>`) |
| Concrete states for predators | `Hunting`, `Digesting` (each a `Mood<Predator>`) |
| Contexts | `Prey` and `Predator`, each holding a private `mood` field |

`Animal.update()` also asks the current mood for its `metabolismRate()`, so the mood decides
both what the animal does and what that tick costs it. `Animal.draw()` asks for its
`marker()` colour, so the board shows which animals are fleeing, resting or digesting.

**Observer: world events** (PR #2, branch `world-events`)

| Role | Class |
|---|---|
| Observer interface | `WorldListener` (`onTick`, `onBirth`, `onDeath`, `onEvade`, `onEnded`) |
| Subject | `World`: holds `List<WorldListener>`, `addListener()` / `removeListener()`, and fires events from `update()`, `spawnNear()`, `removeDead()` and `recordEvade()` |
| Concrete observers | `PopulationRecorder`, `ConsoleReporter`, plus a lambda listener and an anonymous listener in `Main` |

**1.2.** For each pattern, paste a code snippet showing the key interaction between participants.

**State.** The context hands the tick to its mood and keeps whatever mood comes back:

```java
// Prey.java (the context)
private Mood<Prey> mood = Grazing.INSTANCE;

@Override
protected void act(World world) {
    updateSpeed();
    mood = mood.act(this, world);
}
```

The mood does the work and decides the transition itself:

```java
// Grazing.java (a concrete state)
public Mood<Prey> act(Prey prey, World world) {
    Predator threat = prey.nearestThreat(world);
    if (threat != null) {
        // Switch and react in the same tick: losing a tick to the
        // transition would hand every predator a free step.
        return new Fleeing().act(prey, world);
    }
    ...
    if (prey.getHealth() >= prey.getMaxHealth() * SATED) {
        return Resting.INSTANCE;
    }
    ...
    return this;
}

// Fleeing.java: carries data belonging to one animal
if (--calmIn <= 0) {
    return Grazing.INSTANCE;
}
prey.moveAwayFrom(lastThreatX, lastThreatY);
return this;
```

**Observer.** The subject fires events without knowing who is listening:

```java
// World.java (the subject), end of update()
state = evaluateState();

for (WorldListener l : listeners) l.onTick(this);
if (state != SimulationState.RUNNING) {
    for (WorldListener l : listeners) l.onEnded(this, state);
}
```

The observers subscribe in `Main`, which now only drives the clock:

```java
// Main.java
PopulationRecorder recorder = new PopulationRecorder(world);
world.addListener(recorder);
...
Timer timer = new Timer(config.getTickMs(), e -> world.update());

world.addListener(WorldListener.onTick(w -> {
    panel.repaint();
    graph.repaint();
}));
world.addListener(new ConsoleReporter(recorder));
world.addListener(new WorldListener() {
    @Override
    public void onEnded(World w, SimulationState state) {
        timer.stop();
    }
});
```

**1.3.** For each pattern, explain why you chose it over an alternative. What trade-off did you make?

**State.**

The problem: `Prey.act()` and `Predator.act()` were long if/else priority chains (flee, else
breed, else eat if hungry, else find a mate, else eat anyway, else wander). I wanted animals
to have moods that last across ticks, like staying scared for a few ticks after a predator
disappears, or sitting still once full. Adding that to the chains would have meant more
flags on `Animal` and more branches checking them.

Alternatives I considered:

1. **An enum and a switch.** `enum Mood { GRAZING, FLEEING, RESTING }` with a switch in
   `act()`. This fails on data. `Fleeing` has to remember where the predator was last seen
   and how long until it calms down, and `Digesting` has to count down. With an enum those
   would become fields on every `Animal`, meaningless most of the time. The switch would
   also need repeating in three places: what to do, how much energy it costs, and which
   marker to draw. Those three switches could drift out of step.
2. **Strategy.** The class structure is almost identical (an interface, several
   implementations, a field holding one), so this was the closest call. The difference is
   who chooses. With Strategy, something outside the object picks the behaviour, usually
   once. Our species' movement rules are like that, and they were already handled by
   overriding `stepToward()`. A mood changes itself based on what happens inside the tick:
   `Hunting` decides to become `Digesting` when `tryEat()` succeeds. Transitions owned by
   the states themselves are what State is for.

Two deliberate differences from the Head First version:

- **`act()` returns the next state** instead of the state calling `context.setState(...)`.
  The `mood` field stays private to `Prey`/`Predator` with no public setter, and every
  transition is a `return` statement, so reading one class shows every way out of it.
- **Stateless moods are shared singletons** (`Grazing.INSTANCE`, `Resting.INSTANCE`,
  `Hunting.INSTANCE`), while `Fleeing` and `Digesting` are created fresh per animal because
  they carry per-animal data. A hundred grazing rabbits share one object.

The generic bound does real work too. `Mood<Prey>` can call `prey.evade()` and
`prey.tryEatFood()` without casting, and the compiler rejects handing a prey mood to a
predator.

The trade-off: behaviour that used to be in one method is now spread across five classes,
and the transitions are spread across them as well. No single file shows the whole state
machine, which is why the diagram includes the transition graphs. It also cost five new
files for what was about 60 lines of `if`.

**Observer.**

The problem: `World` was running the simulation and also keeping all the statistics (the
history map, births, deaths, hops). `Main`'s timer called `world.update()`, then repainted
both panels, then checked whether the run had ended, then stopped itself and printed a
summary. Every new display or logger meant editing `World` or `Main`.

Alternatives I considered:

1. **Keep polling.** Each consumer asks `World` for what it needs after every tick. That
   works for repainting, but not for births and deaths. Those happen inside the tick, so a
   poller would need `World` to keep the counters for it, which is the problem I started
   with.
2. **`java.beans.PropertyChangeSupport`.** It is in the JRE, but events are identified by
   strings and carry `Object` values, so every listener would cast. `WorldListener` has one
   typed method per event, so `onDeath(Animal, DeathCause)` can't be misspelled or handed
   the wrong type.
3. **World calling the panels directly.** That would tie the model to Swing. Headless
   harnesses like `Experiment` and `CensusTest` attach a `PopulationRecorder` and no panels,
   which only works because `World` doesn't know what is listening.

Every `WorldListener` method has an empty default, so a listener overrides only the events
it cares about. The static `WorldListener.onTick(Consumer<World>)` lets a tick-only
listener be a lambda.

The trade-off: the flow of control is now indirect. Reading `World.update()` no longer
tells you that the graph repaints. Notification order matters as well: the recorder must
update before the graph reads it, so `listeners` is a `List` (registration order) rather
than a `Set`, with a comment saying why. Listeners also run synchronously inside
`update()`, so a slow or throwing listener slows or breaks the tick. For a single-threaded
Swing app that is acceptable, and it keeps the events in a predictable order.

---

## 2. Streams / Lambdas / Parallelism

**2.1.** List every file in your project that uses lambda expressions or the Streams API.

| File | Use |
|---|---|
| `Animal.java` | `findNearest()` and `nearestMate()`: `stream().filter(...).min(Comparator.comparingInt(this::distanceTo)).orElse(null)` |
| `World.java` | `speciesColours()` builds the legend with `Stream.of(...).collect(toMap(...))`; `removeIf(h -> !h.isAlive())` for the dead |
| `PopulationRecorder.java` | `groupingBy(Entity::getSpeciesName, counting())` for population counts; `history.forEach((species, series) -> ...)`; sequential or parallel `collect(MoodCensus.collector())` |
| `MoodCensus.java` | custom `Collector.of(Tally::new, Tally::add, Tally::combine, Tally::finish, UNORDERED)`; `Map.merge(..., Integer::sum)` |
| `Experiment.java` | parallel `LongStream` of seeds, `mapToObj(Experiment::run)`, `groupingBy` into an `EnumMap`, `summaryStatistics()` |
| `CensusTest.java` | parallel vs sequential census; deliberate shared-`HashMap` race for comparison |
| `CensusBenchmark.java` | times `stream()` vs `parallelStream()` with the same collector |
| `Grid.java` | `occupantsWithin(..., Predicate<? super T> filter)` and an internal `Consumer<T>` visitor, both passed as lambdas |
| `Predator.java` | `visible.removeIf(p -> world.isInSafeZone(p.getX(), p.getY()))` |
| `Main.java` | `Timer` action `e -> world.update()`; `WorldListener.onTick(w -> { ... })` |
| `WorldListener.java` | `static onTick(Consumer<World>)` turns a lambda into a listener |
| `GridTest.java` | `e -> e instanceof Food` as a grid filter |

**2.2.** For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The mood census: a custom `Collector` that counts how many animals are in each mood and
their average health, run over a parallel stream once the population is large.

```java
// MoodCensus.java
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

    MoodCensus finish() { ... } // averages, sorted, frozen into an immutable result
}

public static Collector<Animal, ?, MoodCensus> collector() {
    return Collector.of(Tally::new, Tally::add, Tally::combine, Tally::finish,
                        Collector.Characteristics.UNORDERED);
}

// PopulationRecorder.java
List<Animal> animals = world.allAnimals();
Stream<Animal> source = animals.size() >= PARALLEL_THRESHOLD
    ? animals.parallelStream()
    : animals.stream();
census = source.collect(MoodCensus.collector());
```

What it does: each tick it produces something like
`Digesting 1 (hp 162)  Fleeing 10 (hp 24)  Grazing 21 (hp 56)  Hunting 12 (hp 134)  Resting 1 (hp 161)`,
shown under the population graph. It is also where the two patterns meet: an Observer
(`PopulationRecorder`) summarising the State objects.

Why a collector rather than a loop: a loop that counts into one map is correct only while it
runs on one thread. The collector splits the job into four parts: make an empty tally
(supplier), add one animal (accumulator), merge two tallies (combiner), and freeze the
result (finisher). Java can then run the same code on one thread or many, because each
thread fills its own tally and the combiner only merges finished ones. I tested the loop's
failure directly. In `CensusTest`, a parallel `forEach` merging into one shared `HashMap`
lost counts in **20 of 20** runs of 200,000 animals, the worst losing 178,456. Over 1,200
ticks of real simulation, the collector agreed with a plain sequential count every time.

A simpler case of the same idea is `Animal.findNearest()`. A dozen lines of "track the best
distance so far" became `filter → min → filter → orElse`. `Stream.min` keeps the first of
equal candidates, which is the same tie-break as the old strict `<`, so every seeded run
replays identically. I checked this against the six `Ablation` runs and `ChaseTest`.

**2.3.** Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

Yes: a custom collector, and parallel streams in two places (both run on the common
Fork/Join pool).

**1. The mood census** (`PopulationRecorder`, `MoodCensus`)

Thread safety comes from confinement. No container is ever written by two threads: each
worker has its own `Tally`, and `combine` runs only after both halves are finished. Nothing
is `synchronized`, because nothing is shared. `MoodCensus` itself is immutable once built.

But being safe is not the same as being worth it. I measured it (`CensusBenchmark`, median
of 31 runs after warm-up, 22 cores):

| Animals | Sequential | Parallel | Speedup |
|---:|---:|---:|---:|
| 100 | 24.7 µs | 308.6 µs | 0.08× |
| 1,000 | 36.6 µs | 180.9 µs | 0.20× |
| 10,000 | 186.3 µs | 226.3 µs | 0.82× |
| 100,000 | 2,319.9 µs | 410.9 µs | 5.65× |
| 1,000,000 | 18,103 µs | 3,007 µs | 6.02× |

On the default board (about 100 animals) the parallel version is twelve times *slower*:
splitting the work and merging tallies costs more than counting a hundred animals. So
`PopulationRecorder` only goes parallel at 50,000 animals or more. The result is the same
either way, only the speed changes.

**2. Many simulations at once** (`Experiment`)

This is where parallelism pays off on a normal board. `Experiment` runs the simulation on
many seeds and reports how runs end:

```java
LongStream seeds = LongStream.range(0, runs).map(i -> 1_000_003L * i + 77);
return (parallel ? seeds.parallel() : seeds)
    .mapToObj(Experiment::run)
    .collect(Collectors.toList()); // keeps seed order even when parallel
```

Each run is a whole separate `World`, with its own entities, `Grid`, listeners and seeded
`Random`, so the threads share no mutable state. The one thing two worlds share is the mood
singletons (`Grazing.INSTANCE` and so on), and they are safe only because they have no
fields. That gave the singleton decision from 1.3 a second justification: if `Grazing` had
kept any per-animal data, it would have been a race between worlds as well as a bug within
one. The harness checks itself too. It runs every seed sequentially and in parallel and
compares the two lists of `Outcome` records with `equals`. 100 runs: **6.5× faster,
outcomes identical**.

`run()` also shows a lambda and exception interaction. `SimulationConfig.fromArgs` throws
the checked `SimulationConfigException`, which a lambda inside `mapToObj` cannot throw, so
`run()` wraps it in an `IllegalStateException`. The arguments are fixed, so reaching that
would be a bug, not bad input.

**Where more could go:** the expensive part of a tick is each animal scanning the grid
around it. That scan only reads the grid, so it could run for every animal in parallel as a
first phase, followed by a sequential phase where animals act and use the shared `Random`.
I didn't do it because perceiving at the start of the tick, rather than as other animals
move, would change behaviour, and the board is too small to gain. Splitting the grid into
regions on separate threads, as the Week 12 handout suggests, has the harder problem of
animals crossing region borders mid-tick.

---

## 3. Uniqueness and Creativity

**3.1.** What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

Animal moods, with costs. Prey graze, flee and rest; predators hunt and digest. These are
more than labels, because each mood changes how fast the animal burns energy:

- **Fleeing costs 1.5×.** Panic is expensive, so a predator drains prey just by being seen.
  A frightened prey also stays frightened for three ticks after losing sight of the
  predator, running away from where it was last seen.
- **Resting costs 0.5×.** A prey above 80% health with no mate nearby sits still, and only
  gets up once it falls below 50%. The gap between those numbers stops it flickering
  between moods every tick.
- **Digesting costs 0.5×.** After a kill a predator stays put for four ticks.

The moods are visible: a yellow dot on fleeing animals, cyan on resting ones and magenta on
digesting ones, plus the live census line under the graph.

Two things led to it. The first was my own Week 7 result. In that version the prey died out
in 19 of 20 runs, and part of the reason was that animals had no memory between ticks. A
rabbit that lost sight of a fox went straight back to eating, and a fox that had just eaten
went straight back to hunting. Every tick was decided from scratch. The second was the Week 8
material on the State pattern. The vending machine and traffic light exercises were about an
object that behaves differently depending on what state it is in, and that was exactly what
the animals were missing. Once each mood was an object, giving it a cost and a memory came
naturally.

Alongside the feature I wrote `Experiment` (1.3 and 2.3) to measure what the moods did,
instead of judging by watching a few runs.

**3.2.** Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

The surprise came from `Digesting`:

```java
// Digesting.java
static final int DIGEST_TICKS = 4;
static final double DIGEST_METABOLISM = 0.5;

public Mood<Predator> act(Predator predator, World world) {
    predator.tryBreed(world);
    if (--ticksLeft <= 0) {
        return Hunting.INSTANCE;
    }
    return this;
}
```

I added it to help the prey: a predator that stops after every kill gives the animals around
it a window to get away. Then I ran each combination of the two new costs across the same 20
seeds (3,000-tick limit). "Panic cost off" means fleeing at 1.0× instead of 1.5×. "Short
pause" means the digest pause cut from 4 ticks to 1.

| Version | Avg ticks | Prey extinct | Predators extinct | Hit limit |
|---|---:|---:|---:|---:|
| Week 7 code, no moods | 616 | 19 | 0 | 1 |
| Moods, panic cost off, short pause | 1,103 | 12 | 6 | 2 |
| Moods, panic cost on, short pause | 761 | 8 | 12 | 0 |
| Moods, panic cost off, 4-tick pause | 934 | 12 | 8 | 0 |
| **Moods, panic cost on, 4-tick pause (submitted)** | **892** | **13** | **6** | **1** |

Cutting the digest pause to one tick (row 3 against row 5) doubled *predator* extinctions,
from 6 to 12. The pause was meant to protect prey, but its bigger effect was protecting predators. Half
the energy burn after a meal is what carries them through the lean stretch after they have
thinned the prey out. Nothing in `Digesting` mentions population or survival. That came out
of the interaction.

The second outcome is the overall balance. In the Week 7 version the prey died out in 19 of
20 runs, so the predators always won. With moods, over 100 seeds the runs split 47 prey
extinct, 51 predators extinct and 2 still going at 3,000 ticks. Which side collapses first
now depends on the run.

A third, quieter effect is the panic cost (the "landscape of fear"). Compared with the
Week 7 version, fewer prey are eaten on average (163 against 181) but far more starve (310
against 176). Predators kill a lot of prey without catching them, by making them burn
energy running. Not all of that starvation is fear, since the other mood changes also alter
the energy budget. But the fleeing animals on the census line average far lower health
than grazing ones (for example hp 24 against hp 56 on seed 918273645 at tick 300).

**3.3.** If you had another 6 weeks, what would you add or change?

Two things, both following directly from what I found.

1. **Prey that can read a predator's mood.** At the moment a rabbit flees a digesting fox
   exactly as it flees a hunting one, so it pays the 1.5x panic cost for nothing. I would
   let prey see a predator's mood and ignore one that is digesting. That turns the
   unanswered question from 3.2 into an experiment: if starvation drops a lot, the fear
   cost was doing most of the killing; if it barely moves, the real danger was. The State
   pattern makes this a small change, because `Prey.nearestThreat()` only needs to skip
   predators whose `getMood()` is a `Digesting`.
2. **A two-phase parallel tick.** As described at the end of 2.3: every animal scans the
   grid in parallel first (which only reads shared state), then all of them act
   sequentially, because acting uses the shared `Random` and moves things. I would build it
   behind a switch and run `Experiment` both ways, to measure two things: the speedup on a
   large board, and how much it changes outcomes when animals perceive the start of the
   tick instead of the world as it is mid-tick. I left it out this time because I could not
   answer the second question in the time I had, and on a 40x30 board there is nothing to
   gain on the first.

---

## 4. Log Book and Final Reflection

**4.1.** Attach or link your log book entries for Weeks 7–13.

My log book is submitted alongside this worksheet as `COMP2000-Logbook.docx` (also in the
repository root). It is the same log book as the Week 7 submission, with entries for Weeks 3
to 7. I did not keep entries for Weeks 8 to 13, so the Week 7 showcase entry is the only one
in the range this question asks for. The record of what I did in the second half of
semester is in the repository instead: the five pull requests and their commit messages,
which describe each change and what it did to the measured results.

**4.2.** Reflect on the semester:

- What was the most important concept you learned?

  Designing for change. In the first half of semester I thought of design mostly as
  getting the class hierarchy right, meaning what is an `Animal` and what overrides what.
  The patterns in the second half changed what I look at. State and Observer are both
  about where the *next* change will land. Before this submission, adding a mood would have
  meant another branch in `Prey.act()`, and adding a display or a logger would have meant
  editing `World` and `Main`'s timer. Now a new mood is one new class implementing `Mood`,
  and a new display is one `addListener()` call. Nothing that already works needs to be
  opened up. That is the Open/Closed Principle from the Week 8 reading, and I understand it
  now because I felt the difference. When I added `ConsoleReporter` and then `Experiment`,
  neither one required touching `World` at all.

- What design trade-off did you face, and how did you resolve it?

  Whether to run the mood census in parallel. I built `MoodCensus` as a custom collector
  specifically so it could run on a parallel stream safely, and `CensusTest` showed that it
  was safe: 1,200 ticks with no mismatches, against a shared-map version that lost counts
  in every run. I expected the obvious next step to be "so make it parallel". But when I
  measured it, the parallel version was twelve times slower on our board. With about 100
  animals, splitting the work across threads and merging the tallies costs far more than
  just counting them.

  So the trade-off was between the version that demonstrates parallelism and the version
  that is actually faster for the simulation as it runs. I resolved it with the benchmark
  rather than by picking one. `CensusBenchmark` showed parallel only wins somewhere between
  10,000 and 100,000 animals, so `PopulationRecorder` uses a sequential stream below 50,000
  and a parallel one above. Both paths run the same collector, so the result never changes,
  only the speed. The lesson I took is that being thread safe and being worth
  parallelising are separate questions, and only the first can be answered by reasoning.
  The second needs a measurement.

- If you started over, what would you do differently?

  Commit steadily across the whole semester. Both submissions were built in bursts close to
  the deadline. The Week 7 feedback said so, and it is still true of this one. The work
  itself benefited from small steps: every commit in this submission compiles and was
  checked against a seeded run before the next one started, which is how I could say
  exactly which change moved which number. But doing that in a single day rather than over a few
  weeks meant I never had time to sit with a result before building on it. The digest-pause
  finding in 3.2 is the best example. I found it after the feature was finished, when it
  should have shaped the design. Starting over, I would treat the weekly project guidance
  as weekly deadlines for my own fork, so each week's pattern or stream feature landed in
  the week it was taught.

- What feedback from the Week 7 submission did you act on?

  Two pieces.

  - *"The link isn't a fork of the team repo."* The Week 7 URL was a separate repository
    with the team's history pushed into it, not a fork. This submission's repository is a
    real GitHub fork of `AnumTaliya/COMP2000Assignment1`, so the link back to the team
    repository is visible on its page.
  - *"Commits could be more consistent across the semester rather than concentrated on a
    few days."* I acted on the part I could still change, which was how the commits are
    made. Each change was pushed as soon as it compiled and had been checked, one small
    commit at a time (18 commits across five pull requests: moods, events, streams,
    experiment and diagram). Each commit message says what changed and what it did to the
    measured results. The part I could not fix by then was timing. All 18 commits landed
    on a single day near the deadline, which is worse than last time on spread, and is why
    spreading the work out is my answer to the previous question.
