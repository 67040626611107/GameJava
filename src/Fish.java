import java.util.Random;

public class Fish {
    public final String id;          // เพิ่ม id
    public final String name;
    public final int price;
    public final boolean golden;
    public final String imagePath;    

    public final double reelRateMul;
    public final double wiggleMul;
    public final double biteSpeedMul;

    private static final String[] NAMES = {
    };
    private static final int[] PRICES = {
        10, 15, 25, 20, 18, 30
    };
    private static final Random RND = new Random();

    public Fish(String id, String name, int price, boolean golden, String imagePath,
                double reelRateMul, double wiggleMul, double biteSpeedMul) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.golden = golden;
        this.imagePath = imagePath;
        this.reelRateMul = reelRateMul;
        this.wiggleMul = wiggleMul;
        this.biteSpeedMul = biteSpeedMul;
    }

    private static Fish fallbackRandom() {
        int i = RND.nextInt(NAMES.length);
        String n = NAMES[i];
        int p = PRICES[i];

        boolean golden = RND.nextDouble() < 0.15;
        if (golden) {
            n = "Golden " + n;
            p = (int) Math.round(p * 2.0);
        }
        return new Fish("fallback_"+n.toLowerCase(), n, p, golden, null, 1.0, 1.0, 1.0);
    }

    public static Fish getRandomFish() {
        try {
            GameplayTuning.loadAll();
            java.util.Collection<GameplayTuning.FishParams> list = GameplayTuning.fishes();
            if (list != null && !list.isEmpty()) {
                int idx = RND.nextInt(list.size());
                GameplayTuning.FishParams fp = list.toArray(new GameplayTuning.FishParams[0])[idx];
                boolean golden = RND.nextDouble() < 0.15;
                String n = fp.displayName;
                int p = fp.basePrice;
                if (golden) {
                    n = "Golden " + n;
                    p = (int)Math.round(p * 2.0);
                }
                return new Fish(fp.id, n, p, golden, fp.imagePath, fp.reelRateMul, fp.wiggleMul, fp.biteSpeedMul);
            }
        } catch (Exception ignored) {}
        return fallbackRandom();
    }
}