public class Series {
    public static void main(String[] a) {
        long seed = Long.parseLong(a[0]);
        int ticks = a.length > 1 ? Integer.parseInt(a[1]) : 800;
        World w = new World(40,30,seed);
        java.util.Random r = w.getRandom();
        for (int i=0;i<2;i++)  w.addHawk(new Hawk(r.nextInt(40), r.nextInt(30)));
        for (int i=0;i<3;i++)  w.addFox(new Fox(r.nextInt(40), r.nextInt(30)));
        for (int i=0;i<25;i++) w.addRabbit(new Rabbit(r.nextInt(40), r.nextInt(30)));
        for (int i=0;i<30;i++) w.addMouse(new Mouse(r.nextInt(40), r.nextInt(30)));
        for (int i=0;i<40;i++) w.addFood(new Food(r.nextInt(40), r.nextInt(30)));
        PopulationRecorder s = new PopulationRecorder(w);
        w.addListener(s);
        for (int t=0;t<ticks && w.getState()==SimulationState.RUNNING;t++) {
            if (t % 80 == 0)
                System.out.printf("t=%-4d H=%-2d F=%-2d R=%-3d M=%-3d Food=%-3d | births=%-4d starved=%-4d eaten=%-4d hops=%d%n",
                    t, w.getHawks().size(), w.getFoxes().size(), w.getRabbits().size(),
                    w.getMice().size(), w.getFood().size(), s.getBirths(), s.getStarved(), s.getEaten(), s.getHops());
            w.update();
        }
        System.out.printf("END t=%d %s | births=%d starved=%d eaten=%d hops=%d%n",
            w.getTickCount(), w.getState(), s.getBirths(), s.getStarved(), s.getEaten(), s.getHops());
    }
}
