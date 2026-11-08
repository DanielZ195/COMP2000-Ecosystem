public class Ablation {
    static void run(String label, String... args) throws Exception {
        SimulationConfig c = SimulationConfig.fromArgs(args);
        World w = new World(c);
        java.util.Random r = w.getRandom();
        for (int i=0;i<c.getHawks();i++)   w.addHawk(new Hawk(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getFoxes();i++)   w.addFox(new Fox(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getRabbits();i++) w.addRabbit(new Rabbit(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getMice();i++)    w.addMouse(new Mouse(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        for (int i=0;i<c.getFood();i++)    w.addFood(new Food(r.nextInt(c.getCols()), r.nextInt(c.getRows())));
        PopulationRecorder stats = new PopulationRecorder(w);
        w.addListener(stats);
        int peak=0, troughAfterPeak=Integer.MAX_VALUE, rebounds=0, prev=c.getRabbits(); boolean falling=false;
        while (w.getState()==SimulationState.RUNNING && w.getTickCount()<3000) {
            w.update();
            int R = w.getRabbits().size() + w.getMice().size();
            peak = Math.max(peak, R);
            if (R < prev) falling = true;
            if (falling && R > prev + 4) { rebounds++; falling = false; }
            prev = R;
        }
        System.out.printf("%-26s survived %4d ticks  peakPrey=%-3d rebounds=%d  births=%d%n",
            label, w.getTickCount(), peak, rebounds, stats.getBirths());
    }
    public static void main(String[] a) throws Exception {
        for (String seed : new String[]{"918273645","555000111","777222333"}) {
            run("refuge ON  seed "+seed,  "seed="+seed, "safeZone=1");
            run("refuge OFF seed "+seed,  "seed="+seed, "safeZone=0");
        }
    }
}
