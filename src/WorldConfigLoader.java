import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * WorldConfigLoader - อ่าน config/world2.json แบบง่ายโดยไม่พึ่ง Gson
 *
 * ข้อสังเกต:
 * - โค้ดนี้เป็น parser แบบย่อสำหรับโครง JSON ที่เราคาดว่าไฟล์ world2.json จะมี (baseProgressSpeed, maps, fish array ฯลฯ)
 * - หากต้องการ parsing ที่สมบูรณ์และปลอดภัยกว่า ให้เพิ่ม Gson ลงใน classpath และเขียน loader ด้วย Gson (ปลดคอมเมนต์/เปลี่ยนตามต้องการ)
 */
public class WorldConfigLoader {

    public static World2Config loadWorld2Config() throws IOException {
        File f = new File("config/world2.json");
        if (!f.exists()) {
            throw new IllegalStateException("config/world2.json not found (expected at project root config/world2.json)");
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        String json = sb.toString();

        World2Config cfg = new World2Config();
        cfg.worldId = extractString(json, "\"worldId\"\\s*:\\s*\"([^\"]*)\"");
        cfg.displayName = extractString(json, "\"displayName\"\\s*:\\s*\"([^\"]*)\"");

        // spawnRules.waterTileTypes (as list)
        String waterArray = extractArrayBlock(json, "\"waterTileTypes\"\\s*:\\s*\\[", "]");
        if (waterArray != null) {
            List<String> items = extractStringList(waterArray);
            cfg.spawnRules = new World2Config.SpawnRules();
            cfg.spawnRules.waterTileTypes = items.toArray(new String[0]);
        } else {
            cfg.spawnRules = new World2Config.SpawnRules();
            cfg.spawnRules.waterTileTypes = new String[0];
        }
        cfg.spawnRules.fishSpawnRate = extractDouble(json, "\"fishSpawnRate\"\\s*:\\s*([0-9\\.]+)", 0.6);

        // fishing settings
        cfg.fishing = new World2Config.FishingSettings();
        cfg.fishing.baseProgressSpeed = extractDouble(json, "\"baseProgressSpeed\"\\s*:\\s*([0-9\\.]+)", 0.6);
        cfg.fishing.barScaleMultiplier = extractDouble(json, "\"barScaleMultiplier\"\\s*:\\s*([0-9\\.]+)", 1.0);

        cfg.fishing.progressSpeedModifierByDifficulty = extractDoubleMap(json, "\"progressSpeedModifierByDifficulty\"\\s*:\\s*\\{");
        cfg.fishing.biteSpeedMultiplier = extractDoubleMap(json, "\"biteSpeedMultiplier\"\\s*:\\s*\\{");

        // fish array: grab block between "fish": [ ... ]
        String fishArrayBlock = extractArrayBlock(json, "\"fish\"\\s*:\\s*\\[", "]");
        if (fishArrayBlock != null) {
            List<String> fishObjects = splitObjects(fishArrayBlock);
            List<World2Config.FishEntry> fishEntries = new ArrayList<>();
            for (String obj : fishObjects) {
                World2Config.FishEntry fe = new World2Config.FishEntry();
                fe.id = extractString(obj, "\"id\"\\s*:\\s*\"([^\"]*)\"");
                fe.name = extractString(obj, "\"name\"\\s*:\\s*\"([^\"]*)\"");
                fe.difficulty = extractString(obj, "\"difficulty\"\\s*:\\s*\"([^\"]*)\"");
                fe.struggleStrength = extractDouble(obj, "\"struggleStrength\"\\s*:\\s*([0-9\\.]+)", 1.0);
                fe.biteTimeSeconds = (int) extractDouble(obj, "\"biteTimeSeconds\"\\s*:\\s*([0-9\\.]+)", 5);
                fe.price = (int) extractDouble(obj, "\"price\"\\s*:\\s*([0-9\\.]+)", 10);
                fe.image = extractString(obj, "\"image\"\\s*:\\s*\"([^\"]*)\"");
                fishEntries.add(fe);
            }
            cfg.fish = fishEntries.toArray(new World2Config.FishEntry[0]);
        } else {
            cfg.fish = new World2Config.FishEntry[0];
        }

        return cfg;
    }

    // --- helpers: very small parsing utilities (regex-like using indexOf)
    private static String extractString(String text, String pattern) {
        // simple implementation using regex
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return "";
    }

    private static double extractDouble(String text, String pattern, double def) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception ignored) {}
        return def;
    }

    private static String extractArrayBlock(String text, String startPattern, String endToken) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(startPattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                int start = m.end();
                int depth = 0;
                int i = start;
                for (; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '[') depth++;
                    else if (c == ']') {
                        if (depth == 0) {
                            return text.substring(start, i);
                        } else depth--;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static List<String> extractStringList(String arrayBlock) {
        List<String> out = new ArrayList<>();
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(arrayBlock);
            while (m.find()) {
                out.add(m.group(1));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static Map<String, Double> extractDoubleMap(String text, String startPattern) {
        Map<String, Double> map = new HashMap<>();
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(startPattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                int start = m.end();
                int braceDepth = 0;
                int i = start;
                for (; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == '{') braceDepth++;
                    else if (c == '}') {
                        if (braceDepth == 0) break;
                        else braceDepth--;
                    }
                }
                int end = i;
                String block = text.substring(start, end);
                // find "key": value pairs
                java.util.regex.Pattern pair = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9\\.]+)");
                java.util.regex.Matcher mm = pair.matcher(block);
                while (mm.find()) {
                    map.put(mm.group(1), Double.parseDouble(mm.group(2)));
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private static List<String> splitObjects(String arrayBlock) {
        List<String> objs = new ArrayList<>();
        int i = 0;
        while (i < arrayBlock.length()) {
            // find next '{'
            int s = arrayBlock.indexOf('{', i);
            if (s < 0) break;
            int depth = 0;
            int j = s;
            for (; j < arrayBlock.length(); j++) {
                char c = arrayBlock.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        objs.add(arrayBlock.substring(s, j + 1));
                        i = j + 1;
                        break;
                    }
                }
            }
            if (j >= arrayBlock.length()) break;
        }
        return objs;
    }

    // --- Data classes
    public static class World2Config {
        public String worldId;
        public String displayName;
        public SpawnRules spawnRules;
        public FishingSettings fishing;
        public FishEntry[] fish;

        public static class SpawnRules {
            public String[] waterTileTypes;
            public double fishSpawnRate;
        }

        public static class FishingSettings {
            public double baseProgressSpeed;
            public Map<String, Double> progressSpeedModifierByDifficulty;
            public double barScaleMultiplier;
            public Map<String, Double> biteSpeedMultiplier;
        }

        public static class FishEntry {
            public String id;
            public String name;
            public String difficulty;
            public double struggleStrength;
            public int biteTimeSeconds;
            public int price;
            public String image;
        }
    }
}