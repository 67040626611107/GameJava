package editor;

import map.MapData;
import map.MapIO;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * เปิด Map Editor ตาม world ที่เลือกจาก resources/config/worlds.json
 * - ผูก centerWater overlay ให้ World 2
 * - บังคับ path ต่อโลก เพื่อไม่สลับไปไฟล์ World 2 เมื่อเลือก World 1
 */
public class MapEditorMain {

    private static final String MANIFEST_PATH = "src/assets/Cute_Fantasy/manifest.files.json";
    private static final String DEFAULT_WORLD1_MAP = "src/assets/maps/map.json";
    private static final String DEFAULT_WORLD2_MAP = "src/assets/maps/map_world2.json";

    public static void main(String[] args) {
        List<WorldEntry> worlds = loadWorldsFromConfig();
        if (worlds.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "ไม่พบไฟล์ worlds.json — เปิด World 1 เริ่มต้น",
                    "Map Editor", JOptionPane.WARNING_MESSAGE);
            openEditorWithPaths(MANIFEST_PATH, DEFAULT_WORLD1_MAP, null);
            return;
        }

        int tmpWorldId = parseWorldId(args);
        if (tmpWorldId <= 0) {
            tmpWorldId = promptWorldId(worlds);
            if (tmpWorldId < 0) return;
        }
        final int parsedWorldId = tmpWorldId;

        WorldEntry selected = worlds.stream()
                .filter(w -> w.id == parsedWorldId)
                .findFirst()
                .orElseGet(() -> worlds.get(0));

        String resolvedMapPath = resolveMapPath(selected);
        ensureMapFileExists(resolvedMapPath, selected);

        openEditorWithPaths(MANIFEST_PATH, resolvedMapPath, selected);
    }

    private static void openEditorWithPaths(String manifestPath, String mapPath, WorldEntry w) {
        final String manifestPathFinal = manifestPath;
        final String mapPathFinal = mapPath;
        SwingUtilities.invokeLater(() -> {
            MapEditorFrame f = new MapEditorFrame(manifestPathFinal, mapPathFinal);
            if (w != null && w.centerWater) {
                int radius = (w.waterRadius != null ? w.waterRadius : 6);
                f.getCanvas().setCenterWaterPreview(true, radius, w.tileWidth, w.tileHeight);
            } else if (w != null) {
                f.getCanvas().setCenterWaterPreview(false, 0, null, null);
            }
            f.setVisible(true);
        });
    }

    // ===== worlds.json loader =====
    private static class WorldEntry {
        final int id; final String name; final boolean centerWater;
        final Integer tileWidth, tileHeight, waterRadius;
        final String groundTile, waterTile, mapPath;
        WorldEntry(int id, String name, boolean centerWater,
                   Integer tileWidth, Integer tileHeight, Integer waterRadius,
                   String groundTile, String waterTile, String mapPath) {
            this.id = id; this.name = name; this.centerWater = centerWater;
            this.tileWidth = tileWidth; this.tileHeight = tileHeight; this.waterRadius = waterRadius;
            this.groundTile = groundTile; this.waterTile = waterTile; this.mapPath = mapPath;
        }
        @Override public String toString() { return "#" + id + " - " + name + (centerWater ? " (centerWater)" : ""); }
    }

    private static List<WorldEntry> loadWorldsFromConfig() {
        List<WorldEntry> list = new ArrayList<>();
        String path = firstExisting("resources/config/world.json","resource/config/worlds.json","resources/config/world.js");
        if (path == null) return list;
        String json = readJsonOrJs(path);
        if (json == null) return list;

        String arr = extractArray(json, "worlds");
        if (arr == null) return list;

        for (String obj : splitObjects(arr)) {
            int id = readInt(obj, "id", -1);
            if (id < 0) continue;
            String name = readString(obj, "name", "World " + id);
            String mapObj = extractObject(obj, "map");
            boolean centerWater = false;
            Integer tileW = null, tileH = null, waterRad = null;
            String groundTile = null, waterTile = null, mapPathKey = null;
            if (mapObj != null) {
                centerWater = readBool(mapObj, "centerWater", false);
                tileW = readNullableInt(mapObj, "width");
                tileH = readNullableInt(mapObj, "height");
                waterRad = readNullableInt(mapObj, "waterRadius");
                groundTile = readNullableString(mapObj, "groundTile");
                waterTile = readNullableString(mapObj, "waterTile");
                mapPathKey = readNullableString(mapObj, "mapPath");
                if (mapPathKey == null) mapPathKey = readNullableString(mapObj, "path");
            }
            list.add(new WorldEntry(id, name, centerWater, tileW, tileH, waterRad, groundTile, waterTile, mapPathKey));
        }
        list.sort(Comparator.comparingInt(w -> w.id));
        return list;
    }

    // ===== utilities (JSON-lite) =====
    private static String firstExisting(String... paths) {
        for (String p : paths) if (p != null && Files.exists(Path.of(p))) return p;
        return null;
    }
    private static String readJsonOrJs(String path) {
        try {
            String raw = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            if (path.endsWith(".js")) {
                int i = raw.indexOf('{'); int j = findMatching(raw, i, '{', '}');
                if (i >= 0 && j > i) return raw.substring(i, j + 1);
            }
            return raw;
        } catch (IOException e) { return null; }
    }
    private static int findMatching(String s, int start, char open, char close) {
        if (start < 0) return -1; int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++; else if (c == close) { depth--; if (depth == 0) return i; }
        } return -1;
    }
    private static String extractArray(String json, String key) {
        int k = indexOfKey(json, key); if (k < 0) return null;
        int lb = json.indexOf('[', k); if (lb < 0) return null;
        int rb = findMatching(json, lb, '[', ']'); if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }
    private static String extractObject(String json, String key) {
        int k = indexOfKey(json, key); if (k < 0) return null;
        int lb = json.indexOf('{', k); if (lb < 0) return null;
        int rb = findMatching(json, lb, '{', '}'); if (rb < 0) return null;
        return json.substring(lb, rb + 1);
    }
    private static int indexOfKey(String json, String key) {
        if (json == null) return -1;
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle); if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + needle.length());
        return colon >= 0 ? idx : -1;
    }
    private static List<String> splitObjects(String arrayBlock) {
        ArrayList<String> list = new ArrayList<>();
        int i = 0;
        while (i < arrayBlock.length()) {
            int s = arrayBlock.indexOf('{', i); if (s < 0) break;
            int e = findMatching(arrayBlock, s, '{', '}'); if (e < 0) break;
            list.add(arrayBlock.substring(s, e + 1)); i = e + 1;
        } return list;
    }
    private static String readString(String obj, String key, String def) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(obj);
        return m.find() ? m.group(1) : def;
    }
    private static String readNullableString(String obj, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(null|\"([^\"]*)\")");
        java.util.regex.Matcher m = p.matcher(obj);
        if (!m.find()) return null; return "null".equals(m.group(1)) ? null : m.group(2);
    }
    private static int readInt(String obj, String key, int def) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        java.util.regex.Matcher m = p.matcher(obj);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }
    private static Integer readNullableInt(String obj, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        java.util.regex.Matcher m = p.matcher(obj);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
    private static boolean readBool(String obj, String key, boolean def) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        java.util.regex.Matcher m = p.matcher(obj);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : def;
    }

    private static int promptWorldId(List<WorldEntry> worlds) {
        Object[] opts = worlds.stream().map(w -> (Object) (w.toString())).toArray(Object[]::new);
        int choice = JOptionPane.showOptionDialog(
                null, "Select world to edit (อ่านจาก resources/config/worlds.json):",
                "Map Editor", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opts, opts.length > 0 ? opts[0] : null
        );
        if (choice < 0 || choice >= worlds.size()) return -1;
        return worlds.get(choice).id;
    }

    private static int parseWorldId(String[] args) {
        if (args == null || args.length == 0) return 0;
        for (String a : args) {
            if (a == null) continue;
            String s = a.trim();
            if (s.matches("^--?world=\\d+$")) {
                try { return Integer.parseInt(s.substring(s.indexOf('=') + 1)); } catch (Exception ignored) {}
            } else if (s.matches("^\\d+$")) {
                try { return Integer.parseInt(s); } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private static String resolveMapPath(WorldEntry w) {
        if (w.mapPath != null && !w.mapPath.isEmpty()) return w.mapPath;
        // Always return per-world default (let ensureMapFileExists create if missing)
        if (w.id == 2) return DEFAULT_WORLD2_MAP;
        return DEFAULT_WORLD1_MAP;
    }

    private static void ensureMapFileExists(String mapPath, WorldEntry w) {
        try {
            Path p = Path.of(mapPath);
            if (Files.exists(p)) return;
            if (p.getParent() != null && !Files.exists(p.getParent())) Files.createDirectories(p.getParent());

            MapData data = new MapData();
            data.tileSize = 64;

            int tilesW = (w.tileWidth != null ? w.tileWidth : 22);
            int tilesH = (w.tileHeight != null ? w.tileHeight : 12);
            data.width  = tilesW * data.tileSize;
            data.height = tilesH * data.tileSize;

            if (w.centerWater) {
                data.waterTopY = data.height + 100; // move off-screen in editor; overlay handles visual
            } else {
                int wr = (w.waterRadius != null ? w.waterRadius : 6);
                int approx = (int) Math.round(wr * data.tileSize * 0.8);
                data.waterTopY = Math.max(0, data.height - approx);
                if (data.waterTopY < data.height / 2) data.waterTopY = Math.max(0, data.height - 220);
            }

            String json = MapIO.toJson(data);
            Files.writeString(p, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "สร้างไฟล์แมพเริ่มต้นไม่สำเร็จ:\n" + mapPath + "\n" + e.getMessage(),
                    "Map Editor", JOptionPane.ERROR_MESSAGE);
        }
    }
}