import java.awt.Color;


public class Food extends Entity implements Edible {
    private static final double NUTRITION_VALUE = 40;

    public Food(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(World world) {
       
    }

    @Override
    public double getNutritionValue() {
        return NUTRITION_VALUE;
    }


    @Override
    public String getSpeciesName() { return "Food"; }

    @Override
    public String getLabel() { return "*"; }

    @Override
    public Color getColor() {
        return Color.GREEN;
    }
}
