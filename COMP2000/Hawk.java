import java.awt.Color;

/** Hawks are fast, long-sighted hunters, but they soar every fourth tick instead of acting. */
public class Hawk extends Predator {
    private int restCounter = 0;

    public Hawk(int x, int y) {
        super(x, y, 150, 3, 5);
    }

    @Override
    protected Animal newOffspring(int x, int y) { return new Hawk(x, y); }

    /** Bishop: always diagonal, never straight, so it cannot track an axis-aligned target exactly. */
    @Override
    protected int[] stepToward(int dx, int dy) {
        int sx = (dx == 0) ? 1 : Integer.signum(dx);
        int sy = (dy == 0) ? 1 : Integer.signum(dy);
        int m = Math.min(speed, Math.max(1, Math.max(Math.abs(dx), Math.abs(dy))));
        return new int[]{ sx * m, sy * m };
    }

    @Override
    protected void act(World world) {
        if (++restCounter % 4 == 0) return;
        super.act(world);
    }


    @Override
    public String getSpeciesName() { return "Hawks"; }

    @Override
    public String getLabel() { return "H"; }

    @Override
    public Color getColor() { return Color.RED; }
}
