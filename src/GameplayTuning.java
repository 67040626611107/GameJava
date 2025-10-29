import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameplayTuning {

    public static final class MapSpec {
        public final int width;
        public final int height;
        public final boolean centerWater;
        public final int waterRadius;
        public final String groundTile;
        public final String waterTile;

        public MapSpec(int width, int height, boolean centerWater, int waterRadius, String groundTile, String waterTile) {
            this.width = width;
            this.height = height;
            this.centerWater = centerWater;
            this.waterRadius = 6;
            this.groundTile = groundTile;
            this.waterTile = waterTile;
        }
    }

    public static final class WorldParams {
        public final int id;
        public final String name;
        public final double reelProgressRate;
        public final double fishWiggleStrength;
        public final int biteTimeBaseMs;
        public final int biteTimeVarianceMs;
        public final double reelBarScale;
        public final MapSpec map; 

        public WorldParams(int id, String name,
                           double reelProgressRate,
                           double fishWiggleStrength,
                           int biteTimeBaseMs,
                           int biteTimeVarianceMs,
                           double reelBarScale,
                           MapSpec map) {
            this.id = id;
            this.name = name;
            this.reelProgressRate = reelProgressRate;
            this.fishWiggleStrength = fishWiggleStrength;
            this.biteTimeBaseMs = biteTimeBaseMs;
            this.biteTimeVarianceMs = biteTimeVarianceMs;
            this.reelBarScale = reelBarScale;
            this.map = map;
        }
    }

    public static final class FishParams {
        public final String id;
        public final String displayName;
        public final int basePrice;
        public final String imagePath; 
        public final double reelRateMul;
        public final double wiggleMul;
        public final double biteSpeedMul;

        public FishParams(String id, String displayName, int basePrice, String imagePath,
                          double reelRateMul, double wiggleMul, double biteSpeedMul) {
            this.id = id;
            this.displayName = displayName;
            this.basePrice = basePrice;
            this.imagePath = imagePath;
            this.reelRateMul = reelRateMul;
            this.wiggleMul = wiggleMul;
            this.biteSpeedMul = biteSpeedMul;
        }
    }

    public static final class CharStats {
        public final double luck;
        public final double reelBarScaleBonus;
        public final double biteSpeedBonus;

        public CharStats(double luck, double reelBarScaleBonus, double biteSpeedBonus) {
            this.luck = luck;
            this.reelBarScaleBonus = reelBarScaleBonus;
            this.biteSpeedBonus = biteSpeedBonus;
        }
    }

    public static final class RodParams {
        public final String id;
        public final String displayName;
        public final int price;
        public final double reelBarScaleBonus;
        public final double luckBonus;
        public final double biteSpeedBonus;
        public final double goldenChanceBonus;

        public RodParams(String id, String displayName, int price,
                         double reelBarScaleBonus, double luckBonus,
                         double biteSpeedBonus, double goldenChanceBonus) {
            this.id = id;
            this.displayName = displayName;
            this.price = price;
            this.reelBarScaleBonus = reelBarScaleBonus;
            this.luckBonus = luckBonus;
            this.biteSpeedBonus = biteSpeedBonus;
            this.goldenChanceBonus = goldenChanceBonus;
        }
    }

    private static final Map<Integer, WorldParams> WORLDS = new HashMap<>();
    private static final Map<String, FishParams> FISH = new HashMap<>();
    private static final Map<String, CharStats> CHARS = new HashMap<>();
    private static final Map<String, RodParams> RODS = new HashMap<>();

    public static void loadAll() {
        WORLDS.clear();
        FISH.clear();
        CHARS.clear();
        RODS.clear();

        loadWorldsJson(firstExisting("resources/config/worlds.json", "resources/config/world.js"));
        loadFishJson(firstExisting("resources/config/fish.json", "resources/config/fish.js"));
        loadCharsJson(firstExisting("resources/config/characters.json", "resources/config/characters.js"));
        loadRodsJson(firstExisting("resources/config/rods.json", "resources/config/rods.js"));

        if (WORLDS.isEmpty()) loadWorldDefaults();
        if (FISH.isEmpty()) loadFishDefaults();
        if (CHARS.isEmpty()) loadCharDefaults();
        if (RODS.isEmpty()) loadRodDefaults();
    }

    private static String firstExisting(String... paths) {
        for (String p : paths) {
            if (p != null && Files.exists(Paths.get(p))) return p;
        }
        return null;
    }

    public static WorldParams world(int id) {
        WorldParams w = WORLDS.get(id);
        if (w == null) w = WORLDS.getOrDefault(1, worldDefault1());
        return w;
    }

    public static Collection<WorldParams> worlds() {
        return WORLDS.values();
    }

    public static FishParams fish(String id) {
        return FISH.get(id);
    }

    public static Collection<FishParams> fishes() {
        return FISH.values();
    }

    public static CharStats charStats(String characterIdOrName) {
        return CHARS.getOrDefault(characterIdOrName, new CharStats(0,0,0));
    }

    public static Collection<RodParams> rods() {
        return RODS.values();
    }

    public static RodParams rod(String id) {
        return RODS.get(id);
    }

    // ================= JSON helpers (brace-safe) =================

    private static String read(String path) {
        if (path == null) return null;
        try {
            String raw = new String(Files.readAllBytes(Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8);
            if (path.endsWith(".js")) {
                int i = raw.indexOf('{');
                int j = findMatching(raw, i, '{', '}');
                if (i >= 0 && j > i) return raw.substring(i, j + 1);
            }
            return raw;
        } catch (IOException e) {
            return null;
        }
    }

    private static int findMatching(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String extractArray(String json, String key) {
        int k = indexOfKey(json, key);
        if (k < 0) return null;
        int lb = json.indexOf('[', k);
        if (lb < 0) return null;
        int rb = findMatching(json, lb, '[', ']');
        if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }

    private static String extractObject(String json, String key) {
        int k = indexOfKey(json, key);
        if (k < 0) return null;
        int lb = json.indexOf('{', k);
        if (lb < 0) return null;
        int rb = findMatching(json, lb, '{', '}');
        if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }

    private static int indexOfKey(String json, String key) {
        if (json == null) return -1;
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + needle.length());
        return colon >= 0 ? idx : -1;
    }

    private static Iterable<String> iterateObjectsInArray(String arrayLiteral) {
        List<String> out = new ArrayList<>();
        if (arrayLiteral == null) return out;
        int i = 0, n = arrayLiteral.length();
        while (i < n) {
            char c = arrayLiteral.charAt(i);
            if (c == '{') {
                int end = findMatching(arrayLiteral, i, '{', '}');
                if (end < 0) break;
                out.add(arrayLiteral.substring(i, end + 1));
                i = end + 1;
            } else {
                i++;
            }
        }
        return out;
    }

    private static String readString(String obj, String key, String def) {
        if (obj == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL);
        Matcher m = p.matcher(obj);
        return m.find() ? m.group(1) : def;
    }

    private static String readNullableString(String obj, String key) {
        if (obj == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(null|\"([^\"]*)\")", Pattern.DOTALL);
        Matcher m = p.matcher(obj);
        if (!m.find()) return null;
        if ("null".equals(m.group(1))) return null;
        return m.group(2);
    }

    private static int readInt(String obj, String key, int def) {
        if (obj == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)", Pattern.DOTALL);
        Matcher m = p.matcher(obj);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private static double readDouble(String obj, String key, double def) {
        if (obj == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL);
        Matcher m = p.matcher(obj);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }

    // ================= Loaders =================

    private static void loadWorldsJson(String path) {
        String s = read(path);
        if (s == null) return;
        String arr = extractArray(s, "worlds");
        if (arr == null) return;

        for (String block : iterateObjectsInArray(arr)) {
            int id = readInt(block, "id", -1);
            if (id < 0) continue;

            String name = readString(block, "name", "World " + id);
            double reelProgressRate = readDouble(block, "reelProgressRate", 1.0);
            double fishWiggleStrength = readDouble(block, "fishWiggleStrength", 1.0);
            int biteBase = readInt(block, "biteTimeBaseMs", 2000);
            int biteVar = readInt(block, "biteTimeVarianceMs", 1000);
            double reelBarScale = readDouble(block, "reelBarScale", 1.0);

            // map
            String mapObj = extractObject(block, "map");
            MapSpec map = null;
            if (mapObj != null) {
                int w = readInt(mapObj, "width", 32);
                int h = readInt(mapObj, "height", 18);
                boolean centerWater = "true".equalsIgnoreCase(readString(mapObj, "centerWater", "false"));
                int waterRadius = readInt(mapObj, "waterRadius", 10);
                String groundTile = readString(mapObj, "groundTile", "GROUND");
                String waterTile = readString(mapObj, "waterTile", "WATER");
                map = new MapSpec(w, h, centerWater, waterRadius, groundTile, waterTile);
            }

            WORLDS.put(id, new WorldParams(
                    id, name, reelProgressRate, fishWiggleStrength,
                    biteBase, biteVar, reelBarScale, map
            ));
        }
    }

    private static void loadFishJson(String path) {
        String s = read(path);
        if (s == null) return;
        String arr = extractArray(s, "fish");
        if (arr == null) return;

        for (String block : iterateObjectsInArray(arr)) {
            String id = readString(block, "id", null);
            if (id == null || id.isEmpty()) continue;

            String name = readString(block, "displayName", id);
            int price = readInt(block, "basePrice", 10);
            String imagePath = readNullableString(block, "imagePath");

            String diff = extractObject(block, "difficulty");
            double reelRateMul = readDouble(diff, "reelRateMul", 1.0);
            double wiggleMul = readDouble(diff, "wiggleMul", 1.0);
            double biteSpeedMul = readDouble(diff, "biteSpeedMul", 1.0);

            FISH.put(id, new FishParams(id, name, price, imagePath, reelRateMul, wiggleMul, biteSpeedMul));
        }
    }

    private static void loadCharsJson(String path) {
        String s = read(path);
        if (s == null) return;
        String arr = extractArray(s, "characters");
        if (arr == null) return;

        for (String block : iterateObjectsInArray(arr)) {
            String id = readString(block, "id", null);
            if (id == null || id.isEmpty()) continue;

            String statsObj = extractObject(block, "stats");
            double luck = readDouble(statsObj, "luck", 0.0);
            double reelBarScaleBonus = readDouble(statsObj, "reelBarScaleBonus", 0.0);
            double biteSpeedBonus = readDouble(statsObj, "biteSpeedBonus", 0.0);

            CHARS.put(id, new CharStats(luck, reelBarScaleBonus, biteSpeedBonus));
        }
    }

    private static void loadRodsJson(String path) {
        String s = read(path);
        if (s == null) return;
        String arr = extractArray(s, "rods");
        if (arr == null) return;

        for (String block : iterateObjectsInArray(arr)) {
            String id = readString(block, "id", null);
            if (id == null || id.isEmpty()) continue;

            String name = readString(block, "displayName", id);
            int price = readInt(block, "price", 0);
            double rScale = readDouble(block, "reelBarScaleBonus", 0.0);
            double luck = readDouble(block, "luckBonus", 0.0);
            double bite = readDouble(block, "biteSpeedBonus", 0.0);
            double golden = readDouble(block, "goldenChanceBonus", 0.0);

            RODS.put(id, new RodParams(id, name, price, rScale, luck, bite, golden));
        }
    }

    // ================= Defaults =================

    private static void loadWorldDefaults() {
        WORLDS.put(1, worldDefault1());
        WORLDS.put(2, worldDefault2());
    }

    private static WorldParams worldDefault1() {
        return new WorldParams(1, "World 1",
                1.0, 1.0,
                2000, 1000,
                1.0,
                new MapSpec(32, 18, false, 6, "GROUND", "WATER"));
    }

    private static WorldParams worldDefault2() {
        return new WorldParams(2, "World 2",
                0.6, 1.6,
                1400, 600,
                1.1,
                new MapSpec(32, 18, true, 6, "GROUND", "WATER"));
    }

    private static void loadFishDefaults() {
        FISH.put("common_carp", new FishParams("common_carp", "Common Carp", 20, null, 1.0, 1.0, 1.0));
        FISH.put("electric_eel", new FishParams("electric_eel", "Electric Eel", 120, null, 0.8, 1.4, 1.2));
        FISH.put("trout", new FishParams("trout", "Trout", 25, null, 1.0, 1.1, 1.0));
    }

    private static void loadCharDefaults() {
        CHARS.put("starter", new CharStats(0.0, 0.0, 0.0));
        CHARS.put("Fisherman_Fin", new CharStats(0.15, 0.10, 0.20));
    }

    private static void loadRodDefaults() {
        RODS.put("starter_rod", new RodParams("starter_rod", "Starter Rod", 0, 0.0, 0.0, 0.0, 0.0));
        RODS.put("bamboo_rod", new RodParams("bamboo_rod", "Bamboo Rod", 150, 0.10, 0.05, 0.10, 0.05));
        RODS.put("pro_rod", new RodParams("pro_rod", "Pro Rod", 400, 0.20, 0.10, 0.20, 0.12));
        RODS.put("legend_rod", new RodParams("legend_rod", "Legend Rod", 1200, 0.35, 0.18, 0.30, 0.25));
    }
}