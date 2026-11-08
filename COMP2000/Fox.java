import java.awt.Color;

/** Foxes are slower and shorter-sighted than hawks, and can only run down one axis at a time. */
public class Fox extends Predator {
    public Fox(int x, int y) {
        super(x, y, 150, 2, 3);
    }

    @Override
    protected Animal newOffspring(int x, int y) { return new Fox(x, y); }

    /** Rook: moves on one axis at a time, so changing direction costs a tick. */
    @Override
    protected int[] stepToward(int dx, int dy) {
        if (Math.abs(dx) >= Math.abs(dy)) return new int[]{ clampStep(dx), 0 };
        return new int[]{ 0, clampStep(dy) };
    }


    @Override
    public String getSpeciesName() { return "Foxes"; }

    @Override
    public String getLabel() { return "F"; }

    @Override
    public Color getColor() { return Color.ORANGE; }
}
