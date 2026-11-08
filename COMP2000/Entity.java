import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Root of the hierarchy. Anything in the world (an animal OR a piece of food)
 * is-a Entity: it has a grid position and can be alive or dead.
 *
 * x and y are cell coordinates, not pixels. px()/py() convert for drawing.
 */
public abstract class Entity {
    private int x, y;
    private boolean alive = true;
    private DeathCause deathCause = DeathCause.UNKNOWN;

    public Entity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return alive; }
    public void kill() { kill(DeathCause.UNKNOWN); }

    public void kill(DeathCause cause) {
        alive = false;
        deathCause = cause;
    }

    public DeathCause getDeathCause() { return deathCause; }

    /** The only way to move an entity - subclasses use this instead of touching fields. */
    protected void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Chebyshev (king-move) distance: how many grid steps apart. */
    public int distanceTo(Entity other) {
        return Math.max(Math.abs(this.x - other.x), Math.abs(this.y - other.y));
    }

    /** Pixel centre of this entity's cell. */
    protected int px() { return x * SimPanel.CELL_SIZE + SimPanel.CELL_SIZE / 2; }
    protected int py() { return y * SimPanel.CELL_SIZE + SimPanel.CELL_SIZE / 2; }

    public abstract void update(World world);
    public abstract Color getColor();

    /** The letter shown on the grid: H hawk, F fox, R rabbit, m mouse, * food. */
    public abstract String getLabel();

    /** Plural group name used for counts and the graph legend: "Hawks", "Mice", "Food". */
    public abstract String getSpeciesName();

    private static final Font LABEL_FONT = new Font(Font.MONOSPACED, Font.BOLD, 15);

    /** Every entity draws the same way; only the letter and colour differ. */
    public void draw(Graphics g) {
        g.setFont(LABEL_FONT);
        g.setColor(getColor());
        FontMetrics fm = g.getFontMetrics();
        String label = getLabel();
        g.drawString(label, px() - fm.stringWidth(label) / 2, py() + fm.getAscent() / 2 - 1);
    }

    @Override
    public String toString() {
        return String.format("%s at (%d, %d)", getClass().getSimpleName(), x, y);
    }
}
