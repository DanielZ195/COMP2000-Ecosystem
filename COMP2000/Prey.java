import java.util.Random;

/**
 * Prey is-a Animal that eats Food and flees Predators, and also implements
 * Edible - since Predator eats Prey. Rabbit and Mouse extend this so they
 * share fleeing, foraging and evasion.
 *
 * Speed follows health directly: an animal above half its maximum moves at
 * FED_SPEED, below it at HUNGRY_SPEED. One number drives speed, evasion and
 * breeding, so a starving animal is slow, cannot dodge, and cannot reproduce.
 */
public abstract class Prey extends Animal implements Edible {
    protected static final int HUNGRY_SPEED = 1;
    protected static final int FED_SPEED = 2;
    protected static final double HOP_COST = 8;


    protected int eatDistance = 1;
    protected double nutritionValue; // how much health a Predator gains from eating this

    public Prey(int x, int y, double startHealth, int visionRadius, double nutritionValue) {
        super(x, y, startHealth, FED_SPEED, visionRadius);
        this.nutritionValue = nutritionValue;
    }

    @Override
    public double getNutritionValue() {
        return nutritionValue;
    }

    /** Call once per tick before moving: sets this tick's speed from health. */
    protected void updateSpeed() {
        speed = isWellFed() ? FED_SPEED : HUNGRY_SPEED;
    }

    /**
     * Knight-hop clear of a predator: two cells directly away, one to the side.
     * A slider cannot follow that in a single tick. Costs HOP_COST, and an
     * animal below a quarter health cannot afford it at all.
     */
    protected void evade(Predator threat, World world) {
        if (health < maxHealth * 0.25) {
            moveAwayFrom(threat);
            return;
        }
        Random rng = world.getRandom();
        int dx = getX() - threat.getX();
        int dy = getY() - threat.getY();
        int sideways = rng.nextBoolean() ? 1 : -1;
        if (Math.abs(dx) >= Math.abs(dy)) {
            int away = (dx == 0) ? sideways : Integer.signum(dx);
            setPosition(getX() + 2 * away, getY() + sideways);
        } else {
            int away = (dy == 0) ? sideways : Integer.signum(dy);
            setPosition(getX() + sideways, getY() + 2 * away);
        }
        health -= HOP_COST;
        world.recordHop();
    }

    /** How far out this animal will still break for the refuge. */
    protected int refugeRange() { return 12; }

    /** How far this animal is from the refuge predators cannot enter. */
    protected int distanceToRefuge(World world) {
        return Math.max(Math.abs(getX() - world.getZoneCentreX()),
                        Math.abs(getY() - world.getZoneCentreY()));
    }

    protected boolean tryEatFood(Food food) {
        if (food != null && food.isAlive() && distanceTo(food) <= eatDistance) {
            food.kill();
            feed(food.getNutritionValue());
            return true;
        }
        return false;
    }

    /** Closest visible predator, or null. */
    protected Predator nearestThreat(World world) {
        return findNearest(
            world.getGrid().occupantsWithin(getX(), getY(), visionRadius, Predator.class));
    }

    /** Closest visible food, or null. */
    protected Food nearestFood(World world) {
        return findNearest(
            world.getGrid().occupantsWithin(getX(), getY(), visionRadius, Food.class));
    }

    /** Flee, then breed, then forage, then look for a mate, then wander. */
    @Override
    protected void act(World world) {
        updateSpeed();

        Predator threat = nearestThreat(world);
        if (threat != null) {
            if (world.isInSafeZone(getX(), getY())) {
                wander(world);
            } else if (distanceToRefuge(world) <= refugeRange()) {
                moveToward(world.getZoneCentreX(), world.getZoneCentreY());
            } else {
                evade(threat, world);
            }
            return;
        }

        tryBreed(world);

        Food food = nearestFood(world);
        if (food != null && !isWellFed()) {
            moveToward(food);
            tryEatFood(food);
            return;
        }

        if (canBreed()) {
            Animal mate = nearestMate(world);
            if (mate != null) {
                moveToward(mate);
                return;
            }
        }

        if (food != null) {
            moveToward(food);
            tryEatFood(food);
        } else {
            wander(world);
        }
    }
}
