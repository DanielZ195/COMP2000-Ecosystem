import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.io.File;

/** Paints the two panels offscreen after N ticks, so the display can be checked without a window. */
public class Render {
    public static void main(String[] a) throws Exception {
        SimulationConfig c = SimulationConfig.fromArgs(new String[]{"seed=" + a[0]});
        World w = new World(c);
        java.util.Random r = w.getRandom();
        for (int i=0;i<c.getHawks();i++)   w.addHawk(new Hawk(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getFoxes();i++)   w.addFox(new Fox(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getRabbits();i++) w.addRabbit(new Rabbit(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getMice();i++)    w.addMouse(new Mouse(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getFood();i++)    w.addFood(new Food(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        PopulationRecorder stats = new PopulationRecorder(w);
        w.addListener(stats);
        int ticks = Integer.parseInt(a[1]);
        for (int t=0; t<ticks && w.getState()==SimulationState.RUNNING; t++) w.update();

        int gw = c.getCols()*SimPanel.CELL_SIZE, gh = c.getRows()*SimPanel.CELL_SIZE, graphH = 120;
        SimPanel sp = new SimPanel(w);   sp.setSize(gw, gh);
        GraphPanel gp = new GraphPanel(w, stats); gp.setSize(gw, graphH);

        BufferedImage img = new BufferedImage(gw, gh + graphH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        sp.paint(g);
        g.translate(0, gh);
        gp.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File(a[2]));
        System.out.println("rendered tick " + w.getTickCount() + " " + w.getState()
            + "  H=" + w.getHawks().size() + " F=" + w.getFoxes().size()
            + " R=" + w.getRabbits().size() + " M=" + w.getMice().size()
            + " Food=" + w.getFood().size() + " -> " + a[2]);
    }
}
