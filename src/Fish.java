import java.util.Random;

public class Fish {
    public final String name;
    public final int price;
    public final boolean golden;

    private static final String[] NAMES = {
        "Carp", "Trout", "Salmon", "Catfish", "Bass", "Tuna"
    };
    private static final int[] PRICES = {
        10, 15, 25, 20, 18, 30
    };
    private static final Random RND = new Random();

    public Fish(String name, int price, boolean golden) {
        this.name = name;
        this.price = price;
        this.golden = golden;
    }

    public static Fish getRandomFish() {
        int i = RND.nextInt(NAMES.length);
        String n = NAMES[i];
        int p = PRICES[i];

        boolean golden = RND.nextDouble() < 0.15;
        if (golden) {
            n = "Golden " + n;
            p = (int) Math.round(p * 2.0); 
        }
        return new Fish(n, p, golden);
    }
}